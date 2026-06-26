package com.cloudbrainmed.ai.model;

import com.cloudbrainmed.ai.entity.ModelVersion;
import com.cloudbrainmed.ai.mapper.ModelVersionMapper;
import com.cloudbrainmed.ai.util.ConfigYamlBuilder;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 模型训练器：管理训练任务的生命周期
 *
 * v2.0: 真实调用 Python 训练脚本，不再模拟
 *
 * 职责：
 * - 创建训练任务
 * - 跟踪训练状态（PENDING → RUNNING → COMPLETED/FAILED）
 * - 通过 ProcessBuilder 启动 Python 训练脚本
 * - 训练完成后注册模型到 ModelLoader
 */
@Component
public class ModelTrainer {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainer.class);

    private final ModelVersionMapper modelVersionMapper;
    private final ModelLoader modelLoader;

    /** Python-ml 目录（相对于应用工作目录） */
    private static final String PYTHON_ML_DIR = "backEnd/services/ai-service/python-ml";

    /** 训练任务状态缓存 */
    private final ConcurrentHashMap<String, TrainingTask> tasks = new ConcurrentHashMap<>();

    public ModelTrainer(ModelVersionMapper modelVersionMapper, ModelLoader modelLoader) {
        this.modelVersionMapper = modelVersionMapper;
        this.modelLoader = modelLoader;
    }

    /**
     * 创建训练任务
     */
    public TrainingTask createTrainingTask(String modelKey, CnnModel.ModelType modelType,
                                            CnnModel.HyperParams params, String datasetPath) {
        String taskId = "TRN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        TrainingTask task = new TrainingTask();
        task.setTaskId(taskId);
        task.setModelKey(modelKey);
        task.setModelType(modelType.getKey());
        task.setHyperParams(params);
        task.setDatasetPath(datasetPath);
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());

        tasks.put(taskId, task);
        log.info("训练任务已创建: {} [{}] → 超参: lr={}, epochs={}, batch={}",
                taskId, modelType.getKey(),
                params.getLearningRate(), params.getEpochs(), params.getBatchSize());
        return task;
    }

    /**
     * 启动训练（异步，真实 Python 进程）
     */
    public void startTraining(String taskId) {
        TrainingTask task = tasks.get(taskId);
        if (task == null) throw new RuntimeException("训练任务不存在: " + taskId);

        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        log.info("训练已启动: {}", taskId);
        new Thread(() -> runPythonTraining(taskId)).start();
    }

    // ==================== Python 训练进程管理 ====================

    /**
     * 通过 ProcessBuilder 启动真实 Python 训练脚本
     */
    private void runPythonTraining(String taskId) {
        TrainingTask task = tasks.get(taskId);
        if (task == null) return;

        Path mlDir = Path.of(PYTHON_ML_DIR);
        Path configPath = mlDir.resolve("training/temp_" + taskId + ".yaml");
        Path expRoot = mlDir.resolve("experiments");

        try {
            // 1. 记录训练前已有的实验目录
            List<String> before = listExperimentDirs(expRoot);

            // 2. 写出临时配置文件
            String yaml = ConfigYamlBuilder.build(task.getHyperParams(), resolveModelType(task.getModelType()));
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, yaml);
            log.info("配置文件已写入: {}", configPath.toAbsolutePath());

            // 3. 启动 Python 进程
            ProcessBuilder pb = new ProcessBuilder(
                    "python", "-m", "training.train",
                    "--config", "training/" + configPath.getFileName().toString()
            );
            pb.directory(mlDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 4. 实时读取输出并写入日志
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[PYTHON] {}", line);
                }
            }
            int exitCode = process.waitFor();
            log.info("Python 进程退出，exitCode={}", exitCode);

            // 5. 删除临时配置文件
            Files.deleteIfExists(configPath);

            if (exitCode != 0) {
                throw new RuntimeException("Python 训练脚本异常退出，exitCode=" + exitCode);
            }

            // 6. 查找最新生成的实验目录
            List<String> after = listExperimentDirs(expRoot);
            after.removeAll(before);
            if (after.isEmpty()) {
                throw new RuntimeException("训练完成但未找到实验输出目录");
            }
            String latestExp = after.get(after.size() - 1);
            Path expDir = expRoot.resolve(latestExp);
            log.info("实验目录: {}", expDir.toAbsolutePath());

            // 7. 读取 metrics.json，获取最优 Dice
            Path metricsPath = expDir.resolve("metrics.json");
            String bestPthPath = expDir.resolve("best.pth").toAbsolutePath().toString();

            String version = "v" + (System.currentTimeMillis() / 1000);
            double bestDice = 0.0;
            if (Files.exists(metricsPath)) {
                String jsonStr = Files.readString(metricsPath);
                JSONObject metrics = JSONObject.parseObject(jsonStr);
                bestDice = metrics.getDouble("best_dice");
            } else {
                log.warn("metrics.json 未生成，使用 best.pth 路径注册");
            }

            // 8. 注册模型到 ModelLoader
            ModelVersion mv = modelLoader.registerModel(
                    task.getModelKey(), version, bestPthPath,
                    resolveModelType(task.getModelType()), task.getHyperParams()
            );
            modelLoader.activateModel(mv.getModelId());

            task.setStatus("COMPLETED");
            task.setCompletedTime(LocalDateTime.now());
            task.setModelId(mv.getModelId());
            log.info("训练完成: taskId={}, modelId={}, bestDice={}", taskId, mv.getModelId(), bestDice);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            log.error("训练失败: {} → {}", taskId, e.getMessage());
        } finally {
            try { Files.deleteIfExists(configPath); } catch (Exception ignored) {}
        }
    }

    // ==================== 工具方法 ====================

    /** 列出 expRoot 下的子目录名（按名称排序，即时间正序） */
    private List<String> listExperimentDirs(Path expRoot) throws IOException {
        if (!Files.isDirectory(expRoot)) return List.of();
        try (Stream<Path> stream = Files.list(expRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /** 根据类型 key 解析枚举 */
    private CnnModel.ModelType resolveModelType(String typeKey) {
        if ("attention".equals(typeKey)) return CnnModel.ModelType.ATTENTION_UNET;
        return CnnModel.ModelType.UNET;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取训练任务状态
     */
    public TrainingTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 查询所有训练任务
     */
    public Map<String, TrainingTask> listTasks() {
        return Map.copyOf(tasks);
    }

    /**
     * 获取模型训练统计
     */
    public Map<String, Object> getStats() {
        long pending = tasks.values().stream().filter(t -> "PENDING".equals(t.getStatus())).count();
        long running = tasks.values().stream().filter(t -> "RUNNING".equals(t.getStatus())).count();
        long completed = tasks.values().stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long failed = tasks.values().stream().filter(t -> "FAILED".equals(t.getStatus())).count();

        return Map.of(
            "pending", pending,
            "running", running,
            "completed", completed,
            "failed", failed,
            "totalModels", modelVersionMapper.countAll()
        );
    }

    // ==================== 内部类 ====================

    /**
     * 训练任务实体
     */
    public static class TrainingTask {
        private String taskId;
        private String modelKey;
        private String modelType;
        private CnnModel.HyperParams hyperParams;
        private String datasetPath;
        private String status;
        private String modelId;
        private String errorMessage;
        private LocalDateTime createTime;
        private LocalDateTime startTime;
        private LocalDateTime completedTime;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getModelKey() { return modelKey; }
        public void setModelKey(String modelKey) { this.modelKey = modelKey; }
        public String getModelType() { return modelType; }
        public void setModelType(String modelType) { this.modelType = modelType; }
        public CnnModel.HyperParams getHyperParams() { return hyperParams; }
        public void setHyperParams(CnnModel.HyperParams hyperParams) { this.hyperParams = hyperParams; }
        public String getDatasetPath() { return datasetPath; }
        public void setDatasetPath(String datasetPath) { this.datasetPath = datasetPath; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
    }
}

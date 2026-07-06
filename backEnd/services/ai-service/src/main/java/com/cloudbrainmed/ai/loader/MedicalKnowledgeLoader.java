package com.cloudbrainmed.ai.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
@Slf4j
public class MedicalKnowledgeLoader implements CommandLineRunner {

    private static final int BATCH_SIZE = 10;
    private final VectorStore vectorStore;

    public MedicalKnowledgeLoader(@Qualifier("consultVectorStore") VectorStore consultVectorStore) {
        this.vectorStore = consultVectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // ⭐ 1. 先检查向量库是否已有数据（仿照 MedicineLoader）
            List<Document> existing = vectorStore.similaritySearch("症状 疾病");
            if (!existing.isEmpty()) {
                log.info("📚 医学知识向量库数据已存在，无需加载");
                return;
            }

            log.info("📚 医学知识向量库为空，开始导入PDF...");

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:docs/*.pdf");

            if (resources.length == 0) {
                log.warn("⚠️ 未找到PDF文件，请检查 src/main/resources/docs/ 目录");
                return;
            }

            log.info("📁 找到 {} 个PDF文件", resources.length);
            List<Document> allDocuments = new ArrayList<>();

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                log.info("📄 正在处理: {}", fileName);

                try {
                    String text = extractTextFromPDF(resource.getInputStream());

                    if (text == null || text.trim().isEmpty()) {
                        log.warn("   ⚠️ 文件内容为空: {}", fileName);
                        continue;
                    }

                    List<String> chunks = splitIntoChunks(text, 300);
                    String symptomName = extractSymptomName(fileName);

                    log.info("   📊 文档长度: {} 字，分成 {} 个文档块", text.length(), chunks.size());

                    for (int i = 0; i < chunks.size(); i++) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("source_file", fileName);
                        metadata.put("chunk_index", i);
                        metadata.put("type", "SYMPTOM_KNOWLEDGE");
                        metadata.put("symptom", symptomName);
                        metadata.put("total_chunks", chunks.size());

                        Document doc = new Document(chunks.get(i), metadata);
                        allDocuments.add(doc);
                    }

                    log.info("   ✅ 成功解析，生成 {} 个文档块", chunks.size());

                } catch (Exception e) {
                    log.error("   ❌ 解析失败: {}", fileName, e);
                }
            }

            if (!allDocuments.isEmpty()) {
                log.info("💾 正在生成向量并存入数据库（共 {} 个文档块）...", allDocuments.size());

                // ⭐ 2. 批量插入逻辑（仿照 MedicineLoader）
                for (int i = 0; i < allDocuments.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, allDocuments.size());
                    List<Document> batch = allDocuments.subList(i, end);

                    try {
                        vectorStore.add(batch);
                        log.info("医学知识向量库批次 {}/{} 加载完成，本批 {} 条",
                                (i / BATCH_SIZE + 1),
                                (allDocuments.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                                batch.size());
                    } catch (Exception e) {
                        log.warn("批次 {}/{} 插入失败，尝试逐条插入: {}",
                                (i / BATCH_SIZE + 1),
                                (allDocuments.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                                e.getMessage());
                        // 逐条插入
                        for (Document doc : batch) {
                            try {
                                vectorStore.add(List.of(doc));
                            } catch (Exception singleError) {
                                log.warn("   ⚠️ 单条文档插入失败: {}", singleError.getMessage());
                            }
                        }
                    }
                }

                log.info("🎉 医学知识库导入完成！共 {} 个文档块", allDocuments.size());
            }

        } catch (Exception ex) {
            // ⭐ 3. 捕获异常，不影响服务启动（仿照 MedicineLoader）
            log.warn("医学知识向量库初始化失败，已跳过本次导入，不影响 ai-service 启动。原因：{}", ex.getMessage(), ex);
        }
    }

    private String extractTextFromPDF(InputStream inputStream) throws Exception {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * 分块策略：按段落和句子智能切分
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                String[] sentences = trimmed.split("。|！|？|；");
                for (String sentence : sentences) {
                    String s = sentence.trim();
                    if (!s.isEmpty()) {
                        if (s.length() > chunkSize) {
                            for (int j = 0; j < s.length(); j += chunkSize) {
                                int end = Math.min(j + chunkSize, s.length());
                                chunks.add(s.substring(j, end) + "。");
                            }
                        } else {
                            chunks.add(s + "。");
                        }
                    }
                }
                continue;
            }

            if (currentChunk.length() + trimmed.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(trimmed).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        // 二次切分：处理过长的块
        List<String> finalChunks = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() > 800) {
                String[] sentences = chunk.split("。|！|？");
                StringBuilder temp = new StringBuilder();
                for (String sentence : sentences) {
                    String s = sentence.trim();
                    if (!s.isEmpty()) {
                        if (temp.length() + s.length() > chunkSize && temp.length() > 0) {
                            finalChunks.add(temp.toString() + "。");
                            temp = new StringBuilder();
                        }
                        temp.append(s).append("。");
                    }
                }
                if (temp.length() > 0) {
                    finalChunks.add(temp.toString());
                }
            } else {
                finalChunks.add(chunk);
            }
        }

        return finalChunks.isEmpty() ? chunks : finalChunks;
    }

    private String extractSymptomName(String fileName) {
        String name = fileName.replace("_百度百科.pdf", "")
                .replace(".pdf", "");
        name = name.replaceAll("\\(.*?\\)", "").trim();
        return name;
    }
}
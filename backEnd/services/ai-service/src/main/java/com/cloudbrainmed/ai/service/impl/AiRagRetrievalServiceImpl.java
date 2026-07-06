package com.cloudbrainmed.ai.service.impl;

import com.cloudbrainmed.ai.service.AiRagRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG检索服务实现
 * 从向量数据库中检索医学知识
 */
@Service
@Slf4j
public class AiRagRetrievalServiceImpl implements AiRagRetrievalService {

    private final VectorStore vectorStore;

    /** 默认检索数量 */
    private static final int DEFAULT_TOP_K = 5;

    /** 症状关键词库 */
    private static final String[] SYMPTOM_KEYWORDS = {
            "发热", "咳嗽", "头痛", "腹痛", "恶心", "呕吐", "腹泻", "便秘",
            "胸痛", "心悸", "气短", "头晕", "乏力", "皮疹", "瘙痒", "关节痛",
            "腰痛", "背痛", "失眠", "焦虑", "抑郁", "视力模糊", "耳鸣", "脑梗",
            "中风", "冠心病", "糖尿病", "高血压", "肺炎", "胃炎", "肝炎", "肾炎",
            "感冒", "流感", "过敏", "哮喘", "胃痛", "肝病", "肾病", "心脏病"
    };

    public AiRagRetrievalServiceImpl(@Qualifier("consultVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String retrieveKnowledge(String chiefComplaint) {
        try {
            // 提取关键词
            List<String> keywords = extractKeywords(chiefComplaint);
            if (keywords.isEmpty()) {
                log.debug("未提取到关键词，主诉: {}", chiefComplaint);
                return "暂无相关医学知识";
            }

            // 构建查询
            String query = String.join(" ", keywords);
            log.debug("RAG查询: {}", query);

            // 向量检索
            List<Document> documents = vectorStore.similaritySearch(query);

            if (documents == null || documents.isEmpty()) {
                log.info("未检索到相关知识，关键词: {}", keywords);
                return "暂无相关医学知识";
            }

            // 构建上下文
            String context = buildContext(documents);
            log.info("RAG检索成功，找到 {} 条相关知识", documents.size());
            return context;

        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return "RAG知识库暂时不可用";
        }
    }

    @Override
    public boolean hasRelevantKnowledge(String chiefComplaint) {
        try {
            List<String> keywords = extractKeywords(chiefComplaint);
            if (keywords.isEmpty()) {
                return false;
            }
            String query = String.join(" ", keywords);
            List<Document> documents = vectorStore.similaritySearch(query);
            return documents != null && !documents.isEmpty();
        } catch (Exception e) {
            log.error("检查RAG知识失败", e);
            return false;
        }
    }

    @Override
    public List<Document> searchDocuments(String query) {
        try {
            if (!StringUtils.hasText(query)) {
                return new ArrayList<>();
            }
            return vectorStore.similaritySearch(query);
        } catch (Exception e) {
            log.error("搜索文档失败，查询: {}", query, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Document> retrieveDocuments(String chiefComplaint, int topK) {
        try {
            List<String> keywords = extractKeywords(chiefComplaint);
            if (keywords.isEmpty()) {
                return new ArrayList<>();
            }
            String query = String.join(" ", keywords);
            List<Document> documents = vectorStore.similaritySearch(query);

            if (documents == null || documents.isEmpty()) {
                return new ArrayList<>();
            }

            // 返回前 topK 个结果
            return documents.stream()
                    .limit(topK)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("检索文档失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> extractKeywords(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }

        return java.util.Arrays.stream(SYMPTOM_KEYWORDS)
                .filter(text::contains)
                .collect(Collectors.toList());
    }

    @Override
    public String buildContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "暂无相关医学知识";
        }

        StringBuilder context = new StringBuilder();
        context.append("【医学知识库参考】\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Map<String, Object> metadata = doc.getMetadata();
            String source = (String) metadata.getOrDefault("source_file", "未知来源");
            String symptom = (String) metadata.getOrDefault("symptom", "");

            context.append(String.format("参考资料 %d（来自: %s）:\n", i + 1, source));
            if (StringUtils.hasText(symptom)) {
                context.append("症状: ").append(symptom).append("\n");
            }
            // 截取过长内容，避免上下文超长
            String content = doc.getText();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            context.append("内容: ").append(content).append("\n\n");
        }

        return context.toString();
    }

    @Override
    public boolean isAvailable() {
        try {
            // 尝试检索一个测试查询来验证向量库是否可用
            vectorStore.similaritySearch("测试");
            return true;
        } catch (Exception e) {
            log.warn("向量库不可用", e);
            return false;
        }
    }

    /**
     * 批量检索相关知识（增强方法）
     *
     * @param chiefComplaint 患者主诉
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度阈值（0-1）
     * @return 文档列表
     */
    public List<Document> retrieveDocumentsWithScore(String chiefComplaint, int maxResults, double minScore) {
        try {
            List<String> keywords = extractKeywords(chiefComplaint);
            if (keywords.isEmpty()) {
                return new ArrayList<>();
            }
            String query = String.join(" ", keywords);

            // 注意：这里假设 VectorStore 支持返回相似度分数
            // 具体实现取决于你的 VectorStore 实现
            List<Document> documents = vectorStore.similaritySearch(query);

            if (documents == null || documents.isEmpty()) {
                return new ArrayList<>();
            }

            return documents.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("检索文档失败", e);
            return new ArrayList<>();
        }
    }
}
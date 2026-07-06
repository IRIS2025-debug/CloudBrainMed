package com.cloudbrainmed.ai.service;

import org.springframework.ai.document.Document;
import java.util.List;

/**
 * RAG检索服务接口
 * 用于从向量数据库中检索医学知识
 */
public interface AiRagRetrievalService {

    /**
     * 检索相关医学知识
     *
     * @param chiefComplaint 患者主诉
     * @return 检索到的知识上下文字符串
     */
    String retrieveKnowledge(String chiefComplaint);

    /**
     * 检查是否有相关知识
     *
     * @param chiefComplaint 患者主诉
     * @return true-有相关知识, false-无相关知识
     */
    boolean hasRelevantKnowledge(String chiefComplaint);

    /**
     * 根据关键词检索文档
     *
     * @param query 查询关键词
     * @return 文档列表
     */
    List<Document> searchDocuments(String query);

    /**
     * 根据主诉提取关键词
     *
     * @param chiefComplaint 患者主诉
     * @return 关键词列表
     */
    List<String> extractKeywords(String chiefComplaint);

    /**
     * 构建上下文字符串
     *
     * @param documents 文档列表
     * @return 格式化后的上下文
     */
    String buildContext(List<Document> documents);

    /**
     * 检索知识并返回原始文档列表
     *
     * @param chiefComplaint 患者主诉
     * @param topK 返回前K个结果
     * @return 文档列表
     */
    List<Document> retrieveDocuments(String chiefComplaint, int topK);

    /**
     * 检查向量库是否可用
     *
     * @return true-可用, false-不可用
     */
    boolean isAvailable();
}
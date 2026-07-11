package com.cloudbrainmed.ai.service;

/**
 * 医生端AI药品知识查询服务。
 *
 * <p>面向执业医生的无状态药品知识查询：只按问题做药品知识库（vector_store）
 * 检索并回答，不绑定具体患者、不保存对话历史，也不针对患者生成用药方案。
 * 与患者端 {@link MedicineAiChatService}（需患者上下文与历史记忆）和接诊
 * 处方草稿（需 registerId 及接诊权限）在权限、记忆和安全边界上完全隔离。</p>
 */
public interface DoctorMedicineQueryService {

    /**
     * 医生药品知识查询。
     *
     * @param question 医生输入的病症或药品问题
     * @return 面向医生的纯文本回答
     */
    String query(String question);
}

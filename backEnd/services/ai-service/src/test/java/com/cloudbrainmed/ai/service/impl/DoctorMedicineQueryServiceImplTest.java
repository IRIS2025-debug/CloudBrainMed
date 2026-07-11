package com.cloudbrainmed.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DoctorMedicineQueryServiceImplTest {

    private ChatClient chatClient;
    private VectorStore medicineVectorStore;
    private DoctorMedicineQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        medicineVectorStore = mock(VectorStore.class);
        service = new DoctorMedicineQueryServiceImpl(
                chatClient, medicineVectorStore);
    }

    @Test
    void blankQuestionReturnsPromptWithoutRetrievalOrModel() {
        String result = service.query("   ");

        assertThat(result).isEqualTo("请输入病症或药品名称");
        verifyNoInteractions(medicineVectorStore);
        verifyNoInteractions(chatClient);
    }

    @Test
    void normalQuestionRetrievesFromMedicineVectorStoreAndAnswers() {
        when(medicineVectorStore.similaritySearch(anyString()))
                .thenReturn(List.of(
                        new Document("布洛芬用于缓解轻至中度疼痛，注意胃肠道反应")));
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("布洛芬适用于疼痛与发热，消化道溃疡患者慎用。");

        String result = service.query("布洛芬适应症");

        assertThat(result)
                .isEqualTo("布洛芬适用于疼痛与发热，消化道溃疡患者慎用。");
        // 必须查询的是药品说明书向量库，而非其他向量库。
        verify(medicineVectorStore).similaritySearch("布洛芬适应症");
    }

    @Test
    void retrievalFailureSkipsModelAndReturnsDegradedMessage() {
        when(medicineVectorStore.similaritySearch(anyString()))
                .thenThrow(new RuntimeException("pgvector 连接失败"));

        String result = service.query("阿莫西林禁忌");

        assertThat(result).contains("药品知识库检索暂时不可用");
        verify(medicineVectorStore).similaritySearch("阿莫西林禁忌");
        // 安全边界：检索故障时不得调用模型让其脱离知识库自由回答。
        verify(chatClient, never()).prompt();
    }

    @Test
    void emptyRetrievalStillAnswersWithKnowledgeGapNotice() {
        when(medicineVectorStore.similaritySearch(anyString()))
                .thenReturn(List.of());
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("知识库暂无相关资料，建议咨询药师。");

        String result = service.query("某罕见药品");

        assertThat(result).isEqualTo("知识库暂无相关资料，建议咨询药师。");
        verify(medicineVectorStore).similaritySearch("某罕见药品");
    }
}

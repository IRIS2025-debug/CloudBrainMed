package com.cloudbrainmed.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PgVectorConfig {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.vectorstore.pgvector.medicine-table-name:vector_store}")
    private String medicineTableName;

    @Value("${spring.ai.vectorstore.pgvector.consult-table-name:ai_consult_vector}")
    private String consultTableName;

    @Value("${spring.ai.vectorstore.pgvector.index-type:HNSW}")
    private String indexType;

    @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
    private String schemaName;

    @Value("${spring.ai.embedding.dimensions:1024}")
    private int embeddingDimensions;

    public PgVectorConfig(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
    }

    // 药品向量库 vector_store
    @Bean("medicineVectorStore")
    public VectorStore medicineVectorStore() {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(medicineTableName)          // ✅ 正确的方法名
                .indexType(PgVectorStore.PgIndexType.valueOf(indexType.toUpperCase()))
                .dimensions(embeddingDimensions)
                .initializeSchema(true)
                .schemaName(schemaName)
                .build();
    }

    // 问诊知识库 ai_consult_vector
    @Bean("consultVectorStore")
    public VectorStore consultVectorStore() {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(consultTableName)           // ✅ 正确的方法名
                .indexType(PgVectorStore.PgIndexType.valueOf(indexType.toUpperCase()))
                .dimensions(embeddingDimensions)
                .initializeSchema(true)
                .schemaName(schemaName)
                .build();
    }
}

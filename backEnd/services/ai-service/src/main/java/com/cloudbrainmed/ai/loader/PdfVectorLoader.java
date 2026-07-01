package com.cloudbrainmed.ai.loader;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "spring.vector-loader.pdf", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PdfVectorLoader implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(PdfVectorLoader.class);
    private static final int CHUNK_SIZE = 500;
    private static final int BATCH_SIZE = 10;

    @Autowired
    private VectorStore vectorStore;

    @Override
    public void run(String... args) throws Exception {
        try {
            List<Document> existing = vectorStore.similaritySearch("药品说明书");
            if (!existing.isEmpty()) {
                log.info("已存在药品说明书向量");
                return;
            }

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] pdfs = resolver.getResources("classpath:pdf/*.pdf");
            List<Document> allDocs = new ArrayList<>();

            for (Resource pdf : pdfs) {
                try (InputStream is = pdf.getInputStream();
                     PDDocument pdDoc = Loader.loadPDF(is.readAllBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(pdDoc);
                    log.info("解析 PDF [{}]，共 {} 字", pdf.getFilename(), text.length());

                    for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
                        int end = Math.min(i + CHUNK_SIZE, text.length());
                        String chunk = text.substring(i, end);
                        Document doc = new Document(
                                chunk,
                                Map.of("source", pdf.getFilename(), "type", "pdf")
                        );
                        allDocs.add(doc);
                    }
                }
            }

            if (allDocs.isEmpty()) {
                log.info("未发现可导入的 PDF 文档块");
                return;
            }

            for (int i = 0; i < allDocs.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, allDocs.size());
                List<Document> batch = allDocs.subList(i, end);
                vectorStore.add(batch);
                log.info("PDF 向量库批次 {}/{} 导入完成，本批 {} 个文档块", (i / BATCH_SIZE + 1),
                        (allDocs.size() + BATCH_SIZE - 1) / BATCH_SIZE, batch.size());
            }
            log.info("PDF 向量库全部导入完成，共导入 {} 个文档块", allDocs.size());
        } catch (Exception ex) {
            log.warn("PDF 向量库初始化失败，已跳过本次导入，不影响 ai-service 启动。原因：{}", ex.getMessage(), ex);
        }
    }

}
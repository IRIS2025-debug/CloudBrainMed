package com.cloudbrainmed.ai.loader;

import com.cloudbrainmed.ai.entity.Medicine;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MedicineLoader implements CommandLineRunner {
    private static final Logger log= LoggerFactory.getLogger(MedicineLoader.class);
    private static final int BATCH_SIZE = 10;

    @Resource
    private MedicineMapper medicineMapper;

    @Resource
    private VectorStore vectorStore;

    @Override
    public void run(String... args) throws Exception {
        try {
            List<Document> existing=vectorStore.similaritySearch("药品");
            if(!existing.isEmpty()){
                log.info("向量库数据已存在，无需加载");
                return;
            }
            List<Medicine> medicines=medicineMapper.selectAll();
            List<Document> documents=new ArrayList<>();
            for(Medicine medicine:medicines){
                String text="药品名称:"+medicine.getName()+"\n"
                        +"用法用量:"+medicine.getUsage()+"\n"
                        +"药品适应症:"+medicine.getIndication()+"\n"
                        +"药品注意事项:"+medicine.getAttention()+"\n"
                        +"库存:"+medicine.getStock()+"\n";
                Document doc=new Document(text, Map.of("source",medicine.getName(),"type","medicine"));
                documents.add(doc);
            }

            for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, documents.size());
                List<Document> batch = documents.subList(i, end);
                vectorStore.add(batch);
                log.info("药品向量库批次 {}/{} 加载完成，本批 {} 条", (i / BATCH_SIZE + 1),
                        (documents.size() + BATCH_SIZE - 1) / BATCH_SIZE, batch.size());
            }
            log.info("药品向量库全部加载完成，共 {} 条", documents.size());
        } catch (Exception ex) {
            log.warn("药品向量库初始化失败，已跳过本次导入，不影响 ai-service 启动。原因：{}", ex.getMessage(), ex);
        }
    }
}
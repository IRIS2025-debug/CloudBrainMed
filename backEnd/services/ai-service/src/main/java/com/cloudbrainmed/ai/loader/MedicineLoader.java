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
            vectorStore.add(documents);
            log.info("向量库数据加载完成");
        } catch (Exception ex) {
            log.warn("药品向量库初始化失败，已跳过本次加载，不影响 ai-service 启动。原因：{}", ex.getMessage(), ex);
        }
    }
}

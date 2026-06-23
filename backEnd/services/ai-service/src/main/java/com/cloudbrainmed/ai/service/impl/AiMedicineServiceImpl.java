package com.cloudbrainmed.ai.service.impl;

import ch.qos.logback.classic.Logger;
import com.cloudbrainmed.ai.dto.MedicineQueryDto;
import com.cloudbrainmed.ai.entity.Medicine;
import com.cloudbrainmed.ai.mapper.MedicineMapper;
import com.cloudbrainmed.ai.service.AiMedicineService;
import com.cloudbrainmed.ai.vo.MedicineAnswerVo;
import jakarta.annotation.Resource;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiMedicineServiceImpl implements AiMedicineService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private VectorStore vectorStore;

    private static final String HISTORY_KEY = "ai:medicine:chat:";
    private static final int EXPIRE = 30;

    public void chatStream(MedicineQueryDto dto, SseEmitter emitter) {
        ChatClient chatClient = chatClientBuilder.build();
        List<Message> messages = loadHistory(dto.getSessionId());

        Medicine medicine = null;
        if (StringUtils.hasText(dto.getMedicineId())) {
            medicine = medicineMapper.selectById(dto.getMedicineId());
        } else {
            medicine = medicineMapper.findByKeyword(dto.getQuestion());
        }
        List<Document> documents = vectorStore.similaritySearch(dto.getQuestion());
        String context =
                documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));
        Logger log = (Logger) LoggerFactory.getLogger(AiMedicineServiceImpl.class);
        log.info("检索到 {} 条相关文档，内容预览：{}", documents.size(),
                context.length() > 100 ? context.substring(0, 100) + "..." : context);
        String promptContent = buildPrompt(medicine, dto.getQuestion(),context);
        messages.add(new SystemMessage(promptContent));
        messages.add(new UserMessage(dto.getQuestion()));

        Prompt prompt = new Prompt(messages);
        StringBuilder fullReply = new StringBuilder();

        // 直接输出 chunk，不再依赖 startsWith 判断
        chatClient.prompt(prompt).stream().content()
                .doOnNext(chunk -> {
                    try {
                        emitter.send(chunk);
                        fullReply.append(chunk);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(() -> {
                    messages.add(new AssistantMessage(fullReply.toString()));
                    saveHistory(dto.getSessionId(), messages);
                    emitter.complete();
                })
                .doOnError(emitter::completeWithError)
                .subscribe();
    }

    public MedicineAnswerVo chat(MedicineQueryDto dto) {
        ChatClient chatClient = chatClientBuilder.build();
        List<Message> messages = loadHistory(dto.getSessionId());

        Medicine medicine = null;
        List<Document> documents = vectorStore.similaritySearch(dto.getQuestion());
        String context =
                documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));

        if (StringUtils.hasText(dto.getMedicineId())) {
            medicine = medicineMapper.selectById(dto.getMedicineId());
        } else {
            medicine = medicineMapper.findByKeyword(dto.getQuestion());
        }

        String promptContent = buildPrompt(medicine, dto.getQuestion(),context);
        messages.add(new SystemMessage(promptContent));
        messages.add(new UserMessage(dto.getQuestion()));

        Prompt prompt = new Prompt(messages);
        String reply = chatClient.prompt(prompt).call().content();

        messages.add(new AssistantMessage(reply));
        saveHistory(dto.getSessionId(), messages);

        MedicineAnswerVo vo = new MedicineAnswerVo();
        vo.setAnswer(reply);
        if (medicine != null) {
            vo.setMedicineName(medicine.getName());
            vo.setUsage(medicine.getUsage());
            vo.setIndication(medicine.getIndication());
            vo.setAttention(medicine.getAttention());
        }
        return vo;
    }

    private String buildPrompt(Medicine med, String question,String context) {
        if (med == null) {
            if(context.isEmpty()){
                return """
                你是一位专业的临床药学顾问，正在为执业医生提供支持。

                请严格按以下格式回答，各部分之间用空行分隔：

                【核心回答】
                直接回答医生问题，2-3句话。

                【详细分析】
                • 要点一
                • 要点二
                • 要点三

                【安全提醒】
                ⚠️ 最重要警告
                • 注意事项一
                • 注意事项二

                【临床建议】
                • 建议一
                • 建议二

                > 总结提示

                要求：
                - 不要使用任何markdown语法
                - 基于循证医学回答
                - 如超出知识范围，建议查阅最新指南
                """;
            }
            else{
                return String.format("""
                你是一位专业的临床药学顾问，正在为执业医生提供支持。
                【参考资料】：%s
                请严格按以下格式回答，各部分之间用空行分隔：

                【核心回答】
                直接回答医生问题，2-3句话。

                【详细分析】
                • 要点一
                • 要点二
                • 要点三

                【安全提醒】
                ⚠️ 最重要警告
                • 注意事项一
                • 注意事项二

                【临床建议】
                • 建议一
                • 建议二

                > 总结提示

                要求：
                - 不要使用任何markdown语法
                - 基于循证医学回答
                - 如超出知识范围，建议查阅最新指南
                """,
                context
                );
            }

        }
        else{
            if(context.isEmpty()){
                return String.format("""
                你是一位专业的临床药学顾问，正在为执业医生提供支持。
    
                当前药品信息：
                药品名称：%s
                用法用量：%s
                适应症：%s
                注意事项：%s
    
                医生提问：%s
    
                请严格按以下格式回答，各部分之间用空行分隔：
    
                【核心回答】
                直接回答医生问题，2-3句话。
    
                【详细分析】
                • 要点一
                • 要点二
                • 要点三
    
                【安全提醒】
                ⚠️ 最重要警告
                • 注意事项一
                • 注意事项二
    
                【临床建议】
                • 建议一
                • 建议二
    
                > 总结提示
    
                要求：
                - 不要使用任何markdown语法
                - 基于药品说明书和临床指南回答
                - 如超出知识范围，建议查阅最新指南
                """,
                        med.getName(),
                        med.getUsage() != null ? med.getUsage() : "请参考药品说明书",
                        med.getIndication() != null ? med.getIndication() : "请参考药品说明书",
                        med.getAttention() != null ? med.getAttention() : "请参考药品说明书",
                        question
                );
            }else{
                return String.format("""
                你是一位专业的临床药学顾问，正在为执业医生提供支持。
                【参考资料】:%s
                当前药品信息：
                药品名称：%s
                用法用量：%s
                适应症：%s
                注意事项：%s
    
                医生提问：%s
    
                请严格按以下格式回答，各部分之间用空行分隔：
    
                【核心回答】
                直接回答医生问题，2-3句话。
    
                【详细分析】
                • 要点一
                • 要点二
                • 要点三
    
                【安全提醒】
                ⚠️ 最重要警告
                • 注意事项一
                • 注意事项二
    
                【临床建议】
                • 建议一
                • 建议二
    
                > 总结提示
    
                要求：
                - 不要使用任何markdown语法
                - 基于药品说明书和临床指南回答
                - 如超出知识范围，建议查阅最新指南
                """,
                        context,
                        med.getName(),
                        med.getUsage() != null ? med.getUsage() : "请参考药品说明书",
                        med.getIndication() != null ? med.getIndication() : "请参考药品说明书",
                        med.getAttention() != null ? med.getAttention() : "请参考药品说明书",
                        question
                );
            }
        }

    }

    private List<Message> loadHistory(String sessionId) {
        return new ArrayList<>();
    }

    private void saveHistory(String sessionId, List<Message> msgs) {
        redisTemplate.opsForValue()
                .set(HISTORY_KEY + sessionId, msgs.toString(), EXPIRE, TimeUnit.MINUTES);
    }
}
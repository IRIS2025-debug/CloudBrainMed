package com.cloudbrainmed.ai.service.impl;

import ch.qos.logback.classic.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
    private static final int MAX_HISTORY_ROUNDS = 5;
    private static final String ROLE_DOCTOR = "doctor";
    private static final String ROLE_PATIENT = "patient";
    private static final String PATIENT_SYSTEM_PROMPT =
            "你是一位专业的AI用药助手，专门为患者提供用药咨询。请用通俗易懂的语言回答患者关于药品用法、用量、注意事项、副作用等问题。你的回答要温暖、耐心、安全，并始终提醒患者如有严重不适请及时就医。";

    public void chatStream(MedicineQueryDto dto, SseEmitter emitter) {
        ChatClient chatClient = chatClientBuilder.build();
        String userRole = resolveUserRole(dto.getUserRole());
        List<Message> history = loadHistory(resolveHistoryKey(dto));
        List<Message> messages = new ArrayList<>(history);

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
        String promptContent = buildPrompt(userRole, medicine, dto.getQuestion(), context);
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
                    history.add(new UserMessage(dto.getQuestion()));
                    history.add(new AssistantMessage(fullReply.toString()));
                    saveHistory(resolveHistoryKey(dto), history);
                    emitter.complete();
                })
                .doOnError(emitter::completeWithError)
                .subscribe();
    }

    public MedicineAnswerVo chat(MedicineQueryDto dto) {
        ChatClient chatClient = chatClientBuilder.build();
        String userRole = resolveUserRole(dto.getUserRole());
        List<Message> history = loadHistory(resolveHistoryKey(dto));
        List<Message> messages = new ArrayList<>(history);

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

        String promptContent = buildPrompt(userRole, medicine, dto.getQuestion(), context);
        messages.add(new SystemMessage(promptContent));
        messages.add(new UserMessage(dto.getQuestion()));

        Prompt prompt = new Prompt(messages);
        String reply = chatClient.prompt(prompt).call().content();

        history.add(new UserMessage(dto.getQuestion()));
        history.add(new AssistantMessage(reply));
        saveHistory(resolveHistoryKey(dto), history);

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

    private String resolveUserRole(String userRole) {
        if (ROLE_PATIENT.equalsIgnoreCase(userRole)) {
            return ROLE_PATIENT;
        }
        return ROLE_DOCTOR;
    }

    private String resolveHistoryKey(MedicineQueryDto dto) {
        if (StringUtils.hasText(dto.getPatientId())) {
            return dto.getPatientId() + ":" + dto.getSessionId();
        }
        return dto.getSessionId();
    }

    private String buildPrompt(String userRole, Medicine med, String question, String context) {
        if (ROLE_PATIENT.equals(userRole)) {
            return buildPatientPrompt(med, question, context);
        }
        return buildDoctorPrompt(med, question, context);
    }

    private String buildPatientPrompt(Medicine med, String question, String context) {
        StringBuilder prompt = new StringBuilder(PATIENT_SYSTEM_PROMPT);
        if (StringUtils.hasText(context)) {
            prompt.append("\n\n【参考资料】\n").append(context);
        }
        if (med != null) {
            prompt.append("\n\n【当前药品信息】\n")
                    .append("药品名称：").append(med.getName()).append('\n')
                    .append("用法用量：").append(med.getUsage() != null ? med.getUsage() : "请参考药品说明书").append('\n')
                    .append("适应症：").append(med.getIndication() != null ? med.getIndication() : "请参考药品说明书").append('\n')
                    .append("注意事项：").append(med.getAttention() != null ? med.getAttention() : "请参考药品说明书");
        }
        prompt.append("\n\n患者提问：").append(question);
        return prompt.toString();
    }

    private String buildDoctorPrompt(Medicine med, String question, String context) {
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

    private List<Message> loadHistory(String sessionKey) {
        String json = redisTemplate.opsForValue().get(HISTORY_KEY + sessionKey);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // 使用更安全的解析方式处理复杂的Message对象
            List<Message> all = JSON.parseArray(json, Message.class);
            return trimHistory(all);
        } catch (Exception e) {
            // 如果解析失败，记录错误并尝试修复JSON
            Logger logger = (Logger) LoggerFactory.getLogger(AiMedicineServiceImpl.class);
            logger.error("Failed to parse message history from Redis: {}", e.getMessage());
            
            // 尝试修复JSON字符串
            String fixedJson = fixMalformedJson(json);
            if (!fixedJson.equals(json)) {
                try {
                    List<Message> all = JSON.parseArray(fixedJson, Message.class);
                    return trimHistory(all);
                } catch (Exception fixEx) {
                    logger.error("Failed to parse fixed message history: {}", fixEx.getMessage());
                }
            }
            
            return new ArrayList<>();
        }
    }
    
    private String fixMalformedJson(String json) {
        // 修复损坏的 $ref 引用格式
        // 原始错误格式: "...text":"..."{"$ref":"$[0][1].media"}}]
        // 正确格式应该是: "...text":"...","media":[]}]
        String fixed = json.replaceAll("(?<=\")\\s*\\{\\s*\\$ref\\s*:\\s*\"([^\"}]*)\"\\s*\\}\\s*(?=\\}\\s*])", "\",\"media\":[]");
        
        // 修复其他可能的格式问题
        fixed = fixed.replaceAll(",\\s*\\}\\s*,\\s*\\}", "},{");
        
        // 修复可能的尾部格式问题
        fixed = fixed.replaceAll("\\{\\s*\\$ref\\s*:\\s*\"([^\"}]*)\"\\s*\\}\\s*(\\]\\s*])", "\"media\":[],$1");
        
        return fixed;
    }

    private void saveHistory(String sessionKey, List<Message> msgs) {
        List<Message> trimmed = trimHistory(msgs);
        redisTemplate.opsForValue()
                .set(HISTORY_KEY + sessionKey, JSON.toJSONString(trimmed, SerializerFeature.WriteClassName), EXPIRE, TimeUnit.MINUTES);
    }

    /** 仅保留最近 MAX_HISTORY_ROUNDS 轮 User/Assistant 对话 */
    private List<Message> trimHistory(List<Message> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return new ArrayList<>();
        }
        List<Message> dialog = msgs.stream()
                .filter(m -> m instanceof UserMessage || m instanceof AssistantMessage)
                .collect(Collectors.toCollection(ArrayList::new));
        int maxMessages = MAX_HISTORY_ROUNDS * 2;
        if (dialog.size() <= maxMessages) {
            return dialog;
        }
        return new ArrayList<>(dialog.subList(dialog.size() - maxMessages, dialog.size()));
    }
}
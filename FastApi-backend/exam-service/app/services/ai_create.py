import logging
import json
import os
from typing import Dict, Any, List

logger = logging.getLogger(__name__)

# 延迟导入，避免无密钥启动时报错
AsyncOpenAI = None

class AIConclusionService:
    """AI生成检验结论服务，懒加载大模型客户端，无密钥不初始化OpenAI"""
    def __init__(self):
        # 仅读取环境变量，不初始化客户端
        self.api_key = os.getenv("DASHSCOPE_API_KEY", "").strip()
        self.base_url = os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1").strip()
        self.model_name = os.getenv("DASHSCOPE_MODEL", "deepseek-v4-flash").strip()
        self._client = None  # 懒加载客户端缓存

    def _get_client(self):
        """首次使用时才创建AsyncOpenAI实例"""
        global AsyncOpenAI
        if self._client is not None:
            return self._client

        # 无密钥直接返回None，不走openai初始化
        if not self.api_key:
            return None

        # 运行时才导入openai，启动阶段不触发
        if AsyncOpenAI is None:
            from openai import AsyncOpenAI as AOAI
            AsyncOpenAI = AOAI

        self._client = AsyncOpenAI(
            api_key=self.api_key,
            base_url=self.base_url
        )
        return self._client

    async def generate_conclusion(self, result_summary: str) -> Dict[str, Any]:
        try:
            client = self._get_client()
            # 无密钥/客户端初始化失败，降级兜底mock
            if client is None:
                logger.warning("未配置DASHSCOPE_API_KEY，使用本地兜底模板生成结论")
                return self._fallback_mock(result_summary)

            conclusion_data = await self._generate_ai_conclusion(client, result_summary)
            return conclusion_data
        except Exception as e:
            logger.error(f"大模型生成结论异常: {str(e)}")
            return self._fallback_mock(result_summary)

    async def _generate_ai_conclusion(self, client, result_summary: str) -> Dict[str, Any]:
        system_prompt = """
你是三甲医院检验科资深医师，根据患者检验摘要书写完整、详细、专业的检验报告结论，严格遵循要求：
1. 仅依据给出的检验数据解读，不得虚构指标、病史与确诊疾病；
2. 分层逐项分析白细胞、红细胞、血小板、炎症标志物等指标含义，内容饱满不简短；
3. 区分全部正常/细菌感染/病毒感染/贫血等不同场景做差异化解读；
4. 最后给出3条贴合本次检验结果的个性化临床随访或检查建议；
5. 行文严谨客观，必须注明仅为实验室参考，需临床医生结合症状体征综合判断；
6. 只输出纯JSON字符串，无任何多余文字、注释、markdown，固定格式：
{"conclusion":"完整检验结论文本","confidence":0~1之间浮点数,"suggestions":["建议1","建议2","建议3"]}
"""
        user_content = f"患者检验结果摘要：{result_summary}"

        response = await client.chat.completions.create(
            model=self.model_name,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content}
            ],
            temperature=0.4,
            max_tokens=900
        )
        raw_text = response.choices[0].message.content.strip()
        parse_res = json.loads(raw_text)

        conclusion = parse_res.get("conclusion", "暂无有效检验结论，请手动填写")
        confidence = float(parse_res.get("confidence", 0.85))
        suggestions = parse_res.get("suggestions", ["建议临床综合评估"])
        if len(suggestions) > 3:
            suggestions = suggestions[:3]

        return {
            "conclusion": conclusion,
            "confidence": confidence,
            "suggestions": suggestions
        }

    def _fallback_mock(self, result_summary: str) -> Dict[str, Any]:
        """密钥缺失/大模型异常时兜底旧关键词逻辑，保证页面可用"""
        conclusion = "检验结果分析："
        abnormalities = []
        if "WBC" in result_summary or "白细胞" in result_summary:
            if "升高" in result_summary or "↑" in result_summary:
                abnormalities.append("白细胞计数升高")
            elif "降低" in result_summary or "↓" in result_summary:
                abnormalities.append("白细胞计数降低")
        if "CRP" in result_summary and ("升高" in result_summary or "↑" in result_summary):
            abnormalities.append("CRP升高")
        if "NEUT" in result_summary or "中性粒" in result_summary:
            if "升高" in result_summary or "↑" in result_summary:
                abnormalities.append("中性粒细胞比例升高")
        if "HB" in result_summary or "血红蛋白" in result_summary:
            if "降低" in result_summary or "↓" in result_summary:
                abnormalities.append("血红蛋白降低")
        if "PLT" in result_summary or "血小板" in result_summary:
            if "降低" in result_summary or "↓" in result_summary:
                abnormalities.append("血小板减少")

        if abnormalities:
            conclusion += f"存在异常指标：{', '.join(abnormalities)}。"
            if "CRP升高" in abnormalities and "白细胞计数升高" in abnormalities:
                conclusion += "结合WBC和CRP均升高，提示可能存在急性细菌感染。"
            elif "白细胞计数升高" in abnormalities:
                conclusion += "提示可能存在感染或炎症反应。"
            elif "血红蛋白降低" in abnormalities:
                conclusion += "提示可能存在贫血，建议进一步检查明确贫血类型。"
            else:
                conclusion += "建议结合临床进一步分析。"
        else:
            conclusion += "各项检验指标均在正常参考范围内，未见明显异常。"
        conclusion += " 建议临床医生结合患者症状及体征综合判断。"

        suggestions = []
        if "CRP升高" in abnormalities or "白细胞计数升高" in abnormalities:
            suggestions.extend(["建议进一步检查明确感染部位", "建议进行血培养检查"])
        elif "血红蛋白降低" in abnormalities:
            suggestions.extend(["建议进行铁代谢、维生素B12和叶酸检查", "必要时完善骨髓相关检查"])
        elif not abnormalities:
            suggestions.extend(["保持健康生活作息", "定期复查血常规"])
        else:
            suggestions.extend(["结合临床症状综合判断", "不适时及时复查"])
        suggestions = suggestions[:3]

        return {
            "conclusion": conclusion,
            "confidence": 0.7,
            "suggestions": suggestions
        }


# 全局单例，启动时仅初始化空对象，不会触发openai校验
ai_conclusion_service = AIConclusionService()
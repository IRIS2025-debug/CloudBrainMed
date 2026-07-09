# ai_analysis.py
import json
import re
import logging
from typing import Dict, Any, List
from datetime import datetime

# LangChain 0.3.x 正确的导入方式
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.output_parsers import JsonOutputParser

from app.config.settings import settings

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CTAnalysisService:
    """CT影像AI分析服务，使用阿里云百炼DeepSeek模型"""

    def __init__(self):
        logger.info("=" * 50)
        logger.info("初始化CTAnalysisService...")
        logger.info(f"模型配置: {settings.DASHSCOPE_MODEL}")
        logger.info(f"API Base URL: {settings.DASHSCOPE_BASE_URL}")

        # 配置阿里云百炼的兼容OpenAI接口
        self.model = ChatOpenAI(
            model=settings.DASHSCOPE_MODEL,
            openai_api_key=settings.DASHSCOPE_API_KEY,
            openai_api_base=settings.DASHSCOPE_BASE_URL,
            temperature=0.3,
            timeout=120,
            max_tokens=2048,
        )
        logger.info("✓ ChatOpenAI模型初始化成功")

        self.system_prompt = self._build_system_prompt()
        logger.info("✓ 系统提示词构建完成")
        logger.info("=" * 50)

    def _build_system_prompt(self) -> str:
        """构建系统提示词"""
        prompt = """你是一位专业的医学影像诊断专家，擅长分析CT影像报告数据。

请根据提供的CT影像分析结果（包括伪影或病灶的像素统计、位置信息、模型输出等），
输出结构化的诊断分析结论。

请严格按照以下JSON格式输出，不要包含任何其他文字：

{
    "summary": "用中文总结影像学发现，简明扼要描述检测到的异常情况",
    "riskLevel": "LOW/MEDIUM/HIGH", 
    "suggestions": ["建议1", "建议2", "建议3"],
    "followUpAdvice": "随访观察建议"
}

风险等级判断标准：
- LOW: 未检测到明显异常，或异常占比<1%
- MEDIUM: 检测到异常，占比1-10%，或累及关键区域
- HIGH: 异常占比>10%，或检测到多发性病灶

请结合临床医学知识，给出专业、合理的建议。"""

        logger.debug("系统提示词长度: %d 字符", len(prompt))
        return prompt

    def _build_user_prompt(self, report_input: Dict[str, Any]) -> str:
        """构建用户提示词"""
        logger.info("开始构建用户提示词...")
        logger.debug(f"原始报告输入: {report_input}")

        # 提取关键信息
        task = report_input.get("task", "")
        positive_pixels = report_input.get("positive_pixels", 0)
        total_pixels = report_input.get("total_pixels", 0)
        ratio = report_input.get("ratio", 0)
        slice_indices = report_input.get("slice_indices", [])
        summary = report_input.get("summary", "")

        logger.info(f"任务类型: {task}")
        logger.info(f"阳性像素数: {positive_pixels:,}")
        logger.info(f"总像素数: {total_pixels:,}")
        logger.info(f"阳性占比: {ratio:.2f}%")
        logger.info(f"涉及切片数: {len(slice_indices)}")

        # 提取额外信息（从report_input的原始数据）
        extra_info = {}
        for key, value in report_input.items():
            if key not in ["task", "positive_pixels", "total_pixels", "ratio", "slice_indices", "summary"]:
                extra_info[key] = value

        # 构建详细的提示
        prompt = f"""请分析以下CT影像AI模型输出结果：

任务类型：{task}

检测结果统计：
- 阳性像素数：{positive_pixels:,}
- 总像素数：{total_pixels:,}
- 阳性占比：{ratio:.2f}%
- 涉及切片：{', '.join(map(str, slice_indices)) if slice_indices else '未知'}

模型输出摘要：
{summary}

其他信息：
{json.dumps(extra_info, ensure_ascii=False, indent=2) if extra_info else '无'}

请根据以上信息，输出结构化的诊断分析结论。"""

        logger.info("✓ 用户提示词构建完成，长度: %d 字符", len(prompt))
        logger.debug(f"完整用户提示词: {prompt}")
        return prompt

    async def analyze_ct_report(self, report_input: Dict[str, Any]) -> Dict[str, Any]:
        """分析CT报告数据"""
        logger.info("=" * 50)
        logger.info(f"开始CT报告分析 [时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}]")

        # 构建提示词
        user_prompt = self._build_user_prompt(report_input)

        messages = [
            SystemMessage(content=self.system_prompt),
            HumanMessage(content=user_prompt)
        ]

        logger.info(f"发送请求到AI模型...")
        logger.info(f"系统提示词长度: {len(self.system_prompt)} 字符")
        logger.info(f"用户提示词长度: {len(user_prompt)} 字符")

        try:
            # 调用模型
            logger.info("⏳ 等待AI模型响应...")
            start_time = datetime.now()

            response = await self.model.agenerate([messages])

            elapsed_time = (datetime.now() - start_time).total_seconds()
            logger.info(f"✓ AI模型响应完成 [耗时: {elapsed_time:.2f}秒]")

            content = response.generations[0][0].text
            logger.info(f"模型原始响应长度: {len(content)} 字符")
            logger.debug(f"模型原始响应内容: {content}")

            # 解析JSON响应
            logger.info("开始解析模型响应...")
            parsed_result = self._parse_response(content)

            logger.info("✓ 响应解析完成")
            logger.info(f"解析结果 - summary: {parsed_result.get('summary', 'N/A')[:50]}...")
            logger.info(f"解析结果 - riskLevel: {parsed_result.get('riskLevel', 'N/A')}")
            logger.info(f"解析结果 - suggestions: {len(parsed_result.get('suggestions', []))} 条建议")

            return parsed_result

        except Exception as e:
            logger.error(f"✗ AI分析失败: {str(e)}")
            logger.error(f"错误类型: {type(e).__name__}")
            import traceback
            logger.error(f"错误堆栈: {traceback.format_exc()}")

            # 如果解析失败，返回默认结构
            logger.info("返回备用结果...")
            fallback_result = self._get_fallback_result(str(e))
            logger.info("✓ 备用结果构建完成")
            return fallback_result

    def _parse_response(self, content: str) -> Dict[str, Any]:
        """解析模型响应，提取JSON"""
        logger.info("开始JSON解析...")
        logger.debug(f"待解析内容: {content[:200]}...")

        # 尝试直接解析JSON
        try:
            result = json.loads(content)
            logger.info("✓ 直接JSON解析成功")
            return result
        except json.JSONDecodeError as e:
            logger.warning(f"直接JSON解析失败: {e}，尝试其他方法...")

        # 尝试从文本中提取JSON
        json_pattern = r'\{[^{}]*"summary"[^{}]*"riskLevel"[^{}]*"suggestions"[^{}]*"followUpAdvice"[^{}]*\}'
        match = re.search(json_pattern, content, re.DOTALL)
        if match:
            try:
                result = json.loads(match.group())
                logger.info("✓ 模式匹配JSON解析成功")
                return result
            except json.JSONDecodeError as e:
                logger.warning(f"模式匹配JSON解析失败: {e}")

        # 尝试更宽松的提取
        json_pattern2 = r'\{.*?\}'
        matches = re.findall(json_pattern2, content, re.DOTALL)
        logger.info(f"找到 {len(matches)} 个可能的JSON块")

        for i, match in enumerate(matches):
            try:
                data = json.loads(match)
                if all(k in data for k in ["summary", "riskLevel", "suggestions", "followUpAdvice"]):
                    logger.info(f"✓ 第 {i + 1} 个JSON块解析成功，包含所有必需字段")
                    return data
                else:
                    logger.debug(f"第 {i + 1} 个JSON块缺少必需字段")
            except json.JSONDecodeError as e:
                logger.debug(f"第 {i + 1} 个JSON块解析失败: {e}")
                continue

        # 如果所有解析都失败，返回备用结果
        logger.error("✗ 所有JSON解析方法均失败")
        return self._get_fallback_result("无法解析AI响应")

    def _get_fallback_result(self, error_msg: str) -> Dict[str, Any]:
        """获取备用结果"""
        logger.warning(f"使用备用结果，原因: {error_msg[:100]}")
        return {
            "summary": f"AI分析处理异常，请人工复核。错误信息：{error_msg[:100]}",
            "riskLevel": "MEDIUM",
            "suggestions": [
                "建议由主治医生结合临床资料综合评估",
                "建议考虑进一步影像学检查（如增强CT或MRI）",
                "建议密切随访观察"
            ],
            "followUpAdvice": "建议1-3个月后复查，如有症状变化随时就诊"
        }


# 单例实例
logger.info("创建CTAnalysisService单例实例...")
ct_analysis_service = CTAnalysisService()
logger.info("✓ CTAnalysisService初始化完成")
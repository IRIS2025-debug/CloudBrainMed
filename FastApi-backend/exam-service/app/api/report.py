# report.py
from fastapi import APIRouter, HTTPException
from typing import Dict, Any
import logging

from app.models.report import AnalyzeReportRequest, AnalyzeReportResponse, GenerateConclusionRequest
from app.services.ai_analysis import ct_analysis_service
from app.models.response import ApiResponse  # 导入统一响应模型
from app.services.ai_create import ai_conclusion_service

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/exam-service", tags=["报告分析"])

@router.post("/report/analyze")
async def analyze_ct_report(request: AnalyzeReportRequest):
    """
    分析CT报告输入，使用AI生成结构化诊断结论

    Args:
        request: 包含挂号ID、报告类型和报告输入数据

    Returns:
        统一格式的响应
    """
    logger.info("=" * 50)
    logger.info("开始处理CT报告分析请求")
    logger.info(f"挂号ID: {request.registerId}")
    logger.info(f"报告类型: {request.reportType}")
    logger.info(f"报告数据: {request.reportInput}")

    try:
        # 验证reportInput
        if not request.reportInput:
            logger.error("报告输入数据为空")
            # ===== 使用统一响应格式返回错误 =====
            return ApiResponse.error(message="报告输入数据不能为空", code=400)

        logger.info("✓ 请求参数验证通过")
        logger.info("开始调用AI分析服务...")

        # 调用AI分析服务
        result = await ct_analysis_service.analyze_ct_report(request.reportInput)

        logger.info("✓ AI分析服务调用完成")
        logger.info(f"分析结果摘要: {result.get('summary', 'N/A')[:100]}...")
        logger.info(f"风险等级: {result.get('riskLevel', 'N/A')}")
        logger.info(f"建议数量: {len(result.get('suggestions', []))}")

        # 添加额外的元数据
        result["registerId"] = request.registerId
        result["reportType"] = request.reportType

        logger.info("✓ 返回结果构建完成")
        logger.info(f"最终响应: {result}")
        logger.info("=" * 50)

        # ===== 使用统一响应格式返回成功 =====
        return ApiResponse.success(data=result, message="分析成功")

    except HTTPException as e:
        logger.error(f"✗ HTTP异常: {e.status_code} - {e.detail}")
        logger.error("=" * 50)
        # ===== HTTP异常也统一格式 =====
        return ApiResponse.error(message=e.detail, code=e.status_code)
    except Exception as e:
        logger.error(f"✗ 未知异常: {str(e)}")
        logger.error("=" * 50)
        # ===== 未知异常统一格式 =====
        return ApiResponse.error(message=f"AI分析失败: {str(e)}", code=500)

@router.post("/report/create")
async def generate_conclusion(request: GenerateConclusionRequest):
    """
    使用AI生成检验结论

    Args:
        request: 包含检验结果摘要的请求

    Returns:
        统一格式的响应，包含AI生成的检验结论
    """
    logger.info("=" * 50)
    logger.info("开始处理AI生成检验结论请求")
    logger.info(f"检验结果摘要: {request.resultSummary[:200]}..." if len(
        request.resultSummary) > 200 else f"检验结果摘要: {request.resultSummary}")

    try:
        # 验证输入
        if not request.resultSummary or not request.resultSummary.strip():
            logger.error("检验结果摘要为空")
            return ApiResponse.error(message="检验结果摘要不能为空", code=400)

        logger.info("✓ 请求参数验证通过")
        logger.info("开始调用AI生成结论服务...")

        # 调用AI服务生成结论
        result = await ai_conclusion_service.generate_conclusion(request.resultSummary)

        logger.info("✓ AI生成结论服务调用完成")
        logger.info(f"生成结论: {result.get('conclusion', '')[:100]}...")
        logger.info(f"置信度: {result.get('confidence', 'N/A')}")

        # 构建响应数据
        response_data = {
            "conclusion": result.get("conclusion", "AI生成结论失败，请手动填写"),
            "confidence": result.get("confidence"),
            "suggestions": result.get("suggestions", [])
        }

        logger.info("✓ 返回结果构建完成")
        logger.info("=" * 50)

        # 使用统一响应格式返回成功
        return ApiResponse.success(data=response_data, message="AI结论生成成功")

    except HTTPException as e:
        logger.error(f"✗ HTTP异常: {e.status_code} - {e.detail}")
        logger.error("=" * 50)
        return ApiResponse.error(message=e.detail, code=e.status_code)
    except Exception as e:
        logger.error(f"✗ 未知异常: {str(e)}")
        logger.error("=" * 50)
        return ApiResponse.error(message=f"AI生成结论失败: {str(e)}", code=500)
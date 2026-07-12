# app/models/report.py
from pydantic import BaseModel
from typing import Optional, Dict, Any, List


class AnalyzeReportRequest(BaseModel):
    registerId: str
    reportType: str
    reportInput: Dict[str, Any]


class AnalyzeReportResponse(BaseModel):
    summary: str
    riskLevel: str  # LOW, MEDIUM, HIGH
    suggestions: List[str]
    followUpAdvice: str
    rawAnalysis: Optional[Dict[str, Any]] = None


class GenerateConclusionRequest(BaseModel):
    """AI生成检验结论请求模型"""
    resultSummary: str  # 检验结果摘要

    class Config:
        json_schema_extra = {
            "example": {
                "resultSummary": "白细胞计数: 12.5×10^9/L (参考范围: 4.0-10.0), 中性粒细胞百分比: 85% (参考范围: 50-70%), CRP: 45mg/L (参考范围: <10)"
            }
        }


class GenerateConclusionResponse(BaseModel):
    """AI生成检验结论响应模型"""
    conclusion: str  # 生成的检验结论
    confidence: Optional[float] = None  # 置信度（可选）
    suggestions: Optional[List[str]] = None  # 建议（可选）

    class Config:
        json_schema_extra = {
            "example": {
                "conclusion": "白细胞计数及中性粒细胞百分比明显升高，CRP显著升高，提示存在急性细菌感染，建议结合临床进一步明确感染部位。",
                "confidence": 0.92,
                "suggestions": ["建议进行血培养", "建议进行影像学检查明确感染部位"]
            }
        }
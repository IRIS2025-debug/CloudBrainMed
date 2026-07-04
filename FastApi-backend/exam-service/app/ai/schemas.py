# app/ai/schemas.py

from pydantic import BaseModel
from typing import Optional


class ExamReportInput(BaseModel):
    """检查报告生成输入（Pydantic V2 模型）"""
    patient_name: str
    patient_age: int
    patient_gender: str
    exam_type: str
    image_findings: str
    doctor_hint: Optional[str] = None


class ExamReportOutput(BaseModel):
    """检查报告生成输出（Pydantic V2 模型）"""
    findings: str
    impression: str
    recommendation: Optional[str] = None
    full_report: str

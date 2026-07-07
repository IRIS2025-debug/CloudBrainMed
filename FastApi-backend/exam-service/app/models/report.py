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
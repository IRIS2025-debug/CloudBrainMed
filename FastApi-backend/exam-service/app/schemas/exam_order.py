from pydantic import BaseModel
from typing import Optional, List


class ExamOrderQueryParams(BaseModel):
    doctor_id: str
    status: Optional[str] = None
    urgency_level: Optional[str] = None
    patient_id: Optional[str] = None
    page: int = 1
    page_size: int = 20


class ApiResponse(BaseModel):
    code: int
    message: str
    data: Optional[dict] = None
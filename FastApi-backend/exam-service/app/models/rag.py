from typing import Dict, Any, List, Optional
from datetime import datetime
from pydantic import BaseModel, Field
import uuid


class RAGCase(BaseModel):
    """RAG知识库案例 - 对应exam_report_vector_store表"""

    # 表字段映射
    id: str = Field(default_factory=lambda: str(uuid.uuid4()), description="UUID主键")
    content: str = Field(..., description="检索文本内容")
    embedding: Optional[List[float]] = Field(None, description="向量嵌入(1024维)")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="元数据JSON")

    # 业务字段（存在metadata中）
    @property
    def case_id(self) -> Optional[str]:
        return self.metadata.get('case_id')

    @property
    def model_features(self) -> Dict[str, Any]:
        return self.metadata.get('model_features', {})

    @property
    def ai_analysis(self) -> Dict[str, Any]:
        return self.metadata.get('ai_analysis', {})

    @property
    def doctor_confirmed(self) -> Dict[str, Any]:
        return self.metadata.get('doctor_confirmed', {})

    @property
    def validation(self) -> Dict[str, Any]:
        return self.metadata.get('validation', {})

    class Config:
        json_schema_extra = {
            "example": {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "content": "左肺上叶磨玻璃结节 8mm 边缘模糊 阳性占比1.25%",
                "embedding": [0.123, 0.456, ...],
                "metadata": {
                    "case_id": "CASE_20240001",
                    "model_features": {...},
                    "ai_analysis": {...},
                    "doctor_confirmed": {...},
                    "validation": {...}
                }
            }
        }


class RAGSearchResult(BaseModel):
    """RAG检索结果"""
    case: RAGCase
    similarity: float = Field(..., description="相似度分数")
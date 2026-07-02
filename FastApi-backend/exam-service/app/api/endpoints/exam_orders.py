from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from typing import Optional

from app.core.database import get_db
from app.core.auth import get_current_doctor_id
from app.services.exam_order_service import ExamOrderService
from app.schemas.exam_order import ExamOrderQueryParams, ApiResponse

router = APIRouter(prefix="/exam-service/exam-orders", tags=["检查申请"])

@router.get("/list", response_model=ApiResponse)
async def get_exam_orders_by_doctor(
        # 删掉前端传来的 doctor_id: str = Query(...)
        status: Optional[str] = Query(None),
        urgency_level: Optional[str] = Query(None),
        patient_id: Optional[str] = Query(None),
        page: int = Query(1, ge=1),
        page_size: int = Query(20, ge=1, le=100),
        db: Session = Depends(get_db),
        # 后端自动从token解析医生ID
        current_doctor_id: str = Depends(get_current_doctor_id)
):
    try:
        # 把解析出来的doctorId传入参数
        params = ExamOrderQueryParams(
            doctor_id=current_doctor_id,
            status=status,
            urgency_level=urgency_level,
            patient_id=patient_id,
            page=page,
            page_size=page_size
        )
        items, total = ExamOrderService.get_exam_orders_by_doctor(db, params)

        items_data = []
        for item in items:
            items_data.append({
                "order_item_id": item.order_item_id,
                "order_id": item.order_id,
                "item_id": item.item_id,
                "item_code": item.item_code,
                "item_name": item.item_name,
                "item_category": item.item_category,
                "assigned_dept_id": item.assigned_dept_id,
                "urgency_level": item.urgency_level,
                "price": float(item.price) if item.price else 0,
                "status": item.status,
                "create_time": item.create_time.isoformat() if item.create_time else None,
                "update_time": item.update_time.isoformat() if item.update_time else None
            })

        return ApiResponse(
            code=200,
            message="查询成功",
            data={
                "items": items_data,
                "total": total,
                "page": page,
                "page_size": page_size,
                "total_pages": (total + page_size - 1) // page_size if total > 0 else 0
            }
        )
    except Exception as e:
        return ApiResponse(code=500, message=f"查询失败: {str(e)}", data=None)
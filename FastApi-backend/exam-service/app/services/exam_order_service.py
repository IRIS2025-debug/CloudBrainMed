from sqlalchemy.orm import Session
from sqlalchemy import and_, desc, join
from typing import List, Tuple, Optional
from datetime import datetime
from app.models.medical_order import MedicalOrder
from app.models.medical_order_item import MedicalOrderItem
from app.schemas.exam_order import ExamOrderQueryParams


class ExamOrderService:
    """检查申请业务逻辑服务"""

    @staticmethod
    def get_exam_orders_by_doctor(
            db: Session,
            params: ExamOrderQueryParams
    ) -> Tuple[List[MedicalOrderItem], int]:
        """
        1. 根据doctor_id筛选medical_order医生开的单
        2. 关联medical_order_item，只取item_category=EXAM明细
        3. 返回明细列表，分页统计明细条数
        """
        # 关联查询：MedicalOrderItem 内连 MedicalOrder
        query = db.query(MedicalOrderItem).join(
            MedicalOrder,
            MedicalOrder.order_id == MedicalOrderItem.order_id
        ).filter(
            # 医生ID过滤
            MedicalOrder.doctor_id == params.doctor_id,
            # 只筛选检查项目
            MedicalOrderItem.item_category == "EXAM"
        )

        # 主单筛选条件
        if params.status:
            query = query.filter(MedicalOrder.status == params.status)
        if params.urgency_level:
            query = query.filter(MedicalOrder.urgency_level == params.urgency_level)
        if params.patient_id:
            query = query.filter(MedicalOrder.patient_id == params.patient_id)

        # 明细总条数
        total = query.count()

        # 按申请单创建时间倒序
        query = query.order_by(desc(MedicalOrder.create_time))

        # 分页
        offset = (params.page - 1) * params.page_size
        item_list = query.offset(offset).limit(params.page_size).all()

        return item_list, total

    # 下面get_exam_order_detail、update_exam_order_status、get_exam_order_statistics 代码不动保持原样
    @staticmethod
    def get_exam_order_detail(
            db: Session,
            order_id: str,
            doctor_id: str
    ) -> Optional[MedicalOrder]:
        """
        获取检查申请详情（需要验证医生权限）
        同时验证订单包含检查项目
        """
        # 检查订单是否包含检查项目
        has_exam_item = db.query(MedicalOrderItem).filter(
            and_(
                MedicalOrderItem.order_id == order_id,
                MedicalOrderItem.item_category == 'EXAM'
            )
        ).first()

        if not has_exam_item:
            return None

        return db.query(MedicalOrder).filter(
            and_(
                MedicalOrder.order_id == order_id,
                MedicalOrder.doctor_id == doctor_id
            )
        ).first()

    @staticmethod
    def update_exam_order_status(
            db: Session,
            order_id: str,
            doctor_id: str,
            status: str
    ) -> Optional[MedicalOrder]:
        """
        更新检查申请状态（检查医生执行操作）
        """
        # 先验证订单包含检查项目
        has_exam_item = db.query(MedicalOrderItem).filter(
            and_(
                MedicalOrderItem.order_id == order_id,
                MedicalOrderItem.item_category == 'EXAM'
            )
        ).first()

        if not has_exam_item:
            return None

        order = db.query(MedicalOrder).filter(
            and_(
                MedicalOrder.order_id == order_id,
                MedicalOrder.doctor_id == doctor_id
            )
        ).first()

        if order:
            order.status = status
            order.update_time = datetime.now()

            # 如果状态变为"已确认"，记录确认时间
            if status == 'CONFIRMED' and not order.confirmed_time:
                order.confirmed_time = datetime.now()

            db.commit()
            db.refresh(order)

        return order

    @staticmethod
    def get_exam_order_statistics(
            db: Session,
            doctor_id: str
    ) -> dict:
        """
        获取检查申请统计数据
        只统计包含检查项目的订单
        """
        # 子查询：找出包含检查项目的 order_id
        exam_order_ids = db.query(MedicalOrderItem.order_id).filter(
            MedicalOrderItem.item_category == 'EXAM'
        ).distinct().subquery()

        # 总申请数
        total = db.query(MedicalOrder).filter(
            and_(
                MedicalOrder.doctor_id == doctor_id,
                MedicalOrder.order_id.in_(exam_order_ids)
            )
        ).count()

        # 待处理数量
        pending = db.query(MedicalOrder).filter(
            and_(
                MedicalOrder.doctor_id == doctor_id,
                MedicalOrder.order_id.in_(exam_order_ids),
                MedicalOrder.status == 'PENDING'
            )
        ).count()

        # 已确认数量
        confirmed = db.query(MedicalOrder).filter(
            and_(
                MedicalOrder.doctor_id == doctor_id,
                MedicalOrder.order_id.in_(exam_order_ids),
                MedicalOrder.status == 'CONFIRMED'
            )
        ).count()

        # 已完成数量
        completed = db.query(MedicalOrder).filter(
            and_(
                MedicalOrder.doctor_id == doctor_id,
                MedicalOrder.order_id.in_(exam_order_ids),
                MedicalOrder.status == 'COMPLETED'
            )
        ).count()

        return {
            "total": total,
            "pending": pending,
            "confirmed": confirmed,
            "completed": completed
        }
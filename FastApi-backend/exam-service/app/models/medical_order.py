from sqlalchemy import Column, String, DateTime, Text
from app.core.database import Base
from datetime import datetime


class MedicalOrder(Base):
    __tablename__ = "medical_order"

    order_id = Column(String(64), primary_key=True, nullable=False, comment="申请单ID")
    patient_id = Column(String(64), nullable=False, comment="患者ID")
    register_id = Column(String(64), nullable=False, comment="挂号ID")
    doctor_id = Column(String(64), nullable=False, comment="开单医生ID")
    clinical_summary = Column(Text, nullable=True, comment="临床摘要")
    urgency_level = Column(String(20), nullable=True, comment="紧急程度")
    source_type = Column(String(20), nullable=True, comment="来源类型：EXAM检查/LAB检验")
    ai_trace_id = Column(String(64), nullable=True, comment="AI追踪ID")
    status = Column(String(20), nullable=True, comment="状态")
    pay_status = Column(String(20), nullable=True, comment="支付状态")
    confirmed_time = Column(DateTime, nullable=True, comment="确认时间")
    create_time = Column(DateTime, default=datetime.now, comment="创建时间")
    update_time = Column(DateTime, default=datetime.now, onupdate=datetime.now, comment="更新时间")
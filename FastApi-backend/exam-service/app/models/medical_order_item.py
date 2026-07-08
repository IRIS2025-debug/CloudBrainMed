from sqlalchemy import Column, String, Numeric, DateTime
from sqlalchemy.sql import func
from app.core.database import Base


class MedicalOrderItem(Base):
    __tablename__ = "medical_order_item"

    order_item_id = Column(String(32), primary_key=True, index=True)
    order_id = Column(String(32), index=True)
    item_id = Column(String(32))
    item_code = Column(String(50))
    item_name = Column(String(100))
    item_category = Column(String(20))  # EXAM, LAB, SURGERY 等
    assigned_dept_id = Column(String(32))
    urgency_level = Column(String(20), default='NORMAL')
    price = Column(Numeric(10, 2), default=0)
    status = Column(String(30), default='WAITING_ASSIGN')
    create_time = Column(DateTime, server_default=func.now())
    update_time = Column(DateTime, server_default=func.now(), onupdate=func.now())
# app/models/exam_report.py

from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class ExamReport(Base):
    """检查报告数据库模型"""
    __tablename__ = "exam_report"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="报告主键ID")
    exam_order_id = Column(Integer, ForeignKey("exam_order.id"), nullable=False, comment="关联检查单ID")
    patient_name = Column(String(50), nullable=False, comment="患者姓名")
    patient_age = Column(Integer, nullable=False, comment="患者年龄")
    patient_gender = Column(String(10), nullable=False, comment="患者性别")
    exam_type = Column(String(50), nullable=False, comment="检查类型")

    # AI生成的四段报告内容
    findings = Column(Text, nullable=False, comment="影像所见")
    impression = Column(Text, nullable=False, comment="诊断意见")
    recommendation = Column(Text, comment="建议")
    full_report = Column(Text, nullable=False, comment="完整报告")

    create_time = Column(DateTime, server_default=func.now(), comment="报告生成时间")
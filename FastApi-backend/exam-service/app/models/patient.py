from sqlalchemy import Column, String, DateTime, SmallInteger, Date
from app.core.database import Base

class Patient(Base):
    __tablename__ = "patient"

    # 主键 patient_id varchar(32)
    patient_id = Column(String(32), primary_key=True, index=True, comment="患者唯一ID")
    # 姓名 varchar(20)
    name = Column(String(20), nullable=False, comment="患者姓名")
    # 性别 smallint 1-男，2-女
    gender = Column(SmallInteger, nullable=True, comment="性别：1-男，2-女")
    # 手机号 varchar(11)
    phone = Column(String(11), nullable=True, comment="手机号码")
    # 身份证 varchar(18)
    id_card = Column(String(18), nullable=True, comment="身份证号码")
    # 地址 varchar(100)
    address = Column(String(100), nullable=True, comment="家庭住址")
    # 登录密码 varchar(64)
    password = Column(String(64), nullable=False, comment="登录密码")
    # 生日 date
    birthday = Column(Date, nullable=True, comment="出生日期")
    # 创建时间 timestamp
    create_time = Column(DateTime, nullable=True, comment="创建时间")
    # 更新时间 timestamp
    update_time = Column(DateTime, nullable=True, comment="更新时间")
    # 软删除 smallint 默认0
    is_deleted = Column(SmallInteger, default=0, comment="是否删除：0-未删除，1-已删除")
    # 头像地址 varchar(255)
    avatar = Column(String(255), nullable=True, comment="头像图片地址")
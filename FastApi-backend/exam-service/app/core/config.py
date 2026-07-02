from pydantic_settings import BaseSettings
from dotenv import load_dotenv
import os

load_dotenv()

class Settings(BaseSettings):
    # 数据库配置
    DB_HOST: str = "localhost"
    DB_PORT: int = 5432
    DB_USER: str = "postgres"
    DB_PASSWORD: str = ""
    DB_NAME: str = "postgres"

    # 服务配置
    SERVICE_PORT: int = 8006
    SERVICE_NAME: str = "exam-service"
    SERVICE_IP: str = ""

    # Nacos 配置
    NACOS_SERVER_ADDR: str = "nacos:8848"
    NACOS_NAMESPACE: str = ""

    # 新增：本地开发开关
    LOCAL_DEV: bool = False

    class Config:
        env_file = ".env"
        env_file_encoding = 'utf-8'
        extra = "ignore"


settings = Settings()
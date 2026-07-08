from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # 阿里云百炼配置
    DASHSCOPE_API_KEY: str = ""
    DASHSCOPE_MODEL: str = "deepseek-v4-flash"
    DASHSCOPE_BASE_URL: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"

    # 服务配置
    SERVICE_HOST: str = "0.0.0.0"
    SERVICE_PORT: int = 8006

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()

# 调试输出（确认加载成功）
print("=== Settings 加载完成 ===")
print(f"DASHSCOPE_MODEL: {settings.DASHSCOPE_MODEL}")
print(f"DASHSCOPE_API_KEY: {settings.DASHSCOPE_API_KEY[:10] if settings.DASHSCOPE_API_KEY else '未设置'}...")
print(f"DASHSCOPE_BASE_URL: {settings.DASHSCOPE_BASE_URL}")
print("========================")
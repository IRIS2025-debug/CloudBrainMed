import uvicorn
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import logging
import sys
from datetime import datetime

# 全局日志配置
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ],
    force=True
)
logger = logging.getLogger(__name__)

print("=" * 70)
print(f"✅ main.py 已加载 [时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}]")
print("=" * 70)

# 全部外部导入，main不再重复定义配置/模型/服务/接口
from app.config.settings import settings
# 导入你现有的路由文件（按你实际文件路径修改）
from app.api.report import router as exam_router

print(f"✅ Settings配置加载完成")
print(f"   - 模型: {settings.DASHSCOPE_MODEL}")
print(f"   - API地址: {settings.DASHSCOPE_BASE_URL}")
print(f"   - 服务端口: {settings.SERVICE_PORT}")

# FastAPI主应用实例
app = FastAPI(
    title="CT报告AI分析服务",
    description="基于LangChain和阿里云百炼DeepSeek的CT影像AI分析服务",
    version="1.0.0"
)
print("✅ FastAPI应用创建完成")

# 跨域中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
print("✅ CORS配置完成")

# 全局请求日志中间件
@app.middleware("http")
async def log_all_requests(request: Request, call_next):
    print(f"\n🌐 [中间件] {request.method} {request.url.path}")
    if request.client:
        print(f"   📍 客户端: {request.client.host}:{request.client.port}")

    import time
    start_time = time.time()
    response = await call_next(request)
    elapsed = (time.time() - start_time) * 1000
    print(f"   ✅ 响应状态: {response.status_code} [耗时: {elapsed:.1f}ms]")
    return response

# 注册你已写好的业务路由
app.include_router(exam_router)
print("✅ exam业务路由注册完成")

# 打印全部注册路由
print("\n" + "=" * 70)
print("📋 已注册的路由:")
for route in app.routes:
    if hasattr(route, 'path') and hasattr(route, 'methods'):
        print(f"   {route.methods} {route.path}")
print("=" * 70 + "\n")

# 健康检查接口
@app.get("/health")
async def health_check():
    print("💚 健康检查请求")
    return {"status": "healthy", "service": "ct-ai-analysis"}

# 根路径
@app.get("/")
async def root():
    print("📌 根路径请求")
    return {"message": "CT AI Analysis Service is running", "endpoints": ["/exam-service/report/analyze"]}

# 服务启动入口
if __name__ == "__main__":
    print("\n" + "=" * 70)
    print("🚀🚀🚀 启动 CT AI 分析服务 🚀🚀🚀")
    print(f"📍 服务地址: http://{settings.SERVICE_HOST}:{settings.SERVICE_PORT}")
    print(f"📡 接口地址: http://{settings.SERVICE_HOST}:{settings.SERVICE_PORT}/exam-service/report/analyze")
    print(f"💚 健康检查: http://{settings.SERVICE_HOST}:{settings.SERVICE_PORT}/health")
    print("=" * 70)
    print("\n按 Ctrl+C 停止服务\n")

    uvicorn.run(
        "app.main:app",
        host=settings.SERVICE_HOST,
        port=settings.SERVICE_PORT,
        reload=True,
        log_level="debug"
    )
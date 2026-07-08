from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    print("🚀 启动服务")
    yield
    print("🛑 关闭服务")

# 创建app
app = FastAPI(
    title="检查服务",
    version="1.0.0",
    docs_url="/api/docs",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===== 1. 先在app上直接定义路由 =====
@app.get("/direct-test")
async def direct_test():
    return {"status": "direct route works"}

# ===== 2. 再导入并注册你的router =====
print("开始导入 router...")
from app.api.endpoints.exam_orders import router
print(f"router 对象: {router}")
print(f"router.routes: {router.routes}")

print("开始注册 router...")
app.include_router(router)
print("router 注册完成")

# ===== 3. 打印所有路由（修复版） =====
print("\n=== 最终路由列表 ===")
for i, route in enumerate(app.routes):
    try:
        if hasattr(route, 'path'):
            print(f"{i}. path: {route.path}, methods: {getattr(route, 'methods', 'N/A')}")
        else:
            print(f"{i}. {type(route).__name__}: (特殊路由对象)")
    except Exception:
        print(f"{i}. 无法读取路由信息")
print("===================\n")

# ===== 4. 根路径 =====
@app.get("/")
async def root():
    return {"service": "exam-service", "status": "running"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8006)
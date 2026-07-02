from app.api.endpoints.exam_orders import router

print("router下注册的路由数量：", len(router.routes))
for r in router.routes:
    print("路由路径：", r.path)

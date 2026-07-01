from fastapi import Depends, HTTPException, Request
from app.common.utils.jwt_util import JwtUtil
from app.schemas.exam_order import ApiResponse

# 去掉 HTTPBearer，改用 Request 手动读取header
def get_current_doctor_id(request: Request) -> str:
    # 优先读取自定义 token 头
    print("读取到token")
    raw_token = request.headers.get("token")
    print("token:")
    print(raw_token)
    # 没有则兼容 Bearer Authorization
    if not raw_token:
        auth_header = request.headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            raw_token = auth_header[7:]
        else:
            # 两种头都不存在，直接401
            raise HTTPException(
                status_code=401,
                detail=ApiResponse(code=401, message="未携带登录凭证", data=None).model_dump()
            )
    try:
        payload = JwtUtil.parse_token(raw_token)
        role_type = payload.get("roleType")
        if role_type != 2:
            raise HTTPException(
                status_code=403,
                detail=ApiResponse(code=403, message="权限不足，仅医生可查看检查申请", data=None).model_dump()
            )
        doctor_id = payload.get("userId")
        if not doctor_id:
            raise HTTPException(
                status_code=401,
                detail=ApiResponse(code=401, message="登录凭证缺失医生ID，请重新登录", data=None).model_dump()
            )
        return str(doctor_id)
    except Exception as err:
        raise HTTPException(
            status_code=401,
            detail=ApiResponse(code=401, message=str(err), data=None).model_dump()
        )
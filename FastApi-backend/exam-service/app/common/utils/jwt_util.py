import jwt
from datetime import datetime
import os
from dotenv import load_dotenv

load_dotenv()

JWT_SECRET_KEY = os.getenv("JWT_SECRET", "medical-cloud-jwt-secret-2026-very-long-key-32bytes")


class JwtUtil:
    @staticmethod
    def trim_bearer(raw_token: str):
        if raw_token.startswith("Bearer "):
            return raw_token[7:]
        return raw_token

    @staticmethod
    def parse_token(token: str) -> dict:
        real_token = JwtUtil.trim_bearer(token)
        try:
            # 先获取token使用的算法
            header = jwt.get_unverified_header(real_token)
            algorithm = header.get('alg', 'HS256')

            print(f"Token使用的算法: {algorithm}")  # 调试信息

            # 使用token指定的算法进行验证
            payload = jwt.decode(
                real_token,
                JWT_SECRET_KEY,
                algorithms=[algorithm],
                options={"verify_exp": True}
            )
            return payload

        except jwt.ExpiredSignatureError:
            raise Exception("token已过期，请重新登录")
        except jwt.InvalidTokenError as e:
            raise Exception(f"token非法，签名校验失败：{str(e)}")
        except Exception as e:
            raise Exception(f"token解析异常：{str(e)}")
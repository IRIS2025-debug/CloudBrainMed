import os
import shutil
from typing import Optional
from datetime import datetime
import uuid


class FileService:
    """文件处理服务"""

    UPLOAD_DIR = "uploads/images"
    ANALYSIS_DIR = "uploads/analysis"

    @classmethod
    def save_upload_file(cls, file_data, filename: str) -> str:
        """保存上传文件"""
        os.makedirs(cls.UPLOAD_DIR, exist_ok=True)

        # 生成唯一文件名
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        unique_id = str(uuid.uuid4())[:8]
        safe_filename = f"{timestamp}_{unique_id}_{filename}"
        file_path = os.path.join(cls.UPLOAD_DIR, safe_filename)

        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file_data, buffer)

        return file_path

    @classmethod
    def get_file_url(cls, file_path: str) -> str:
        """获取文件访问URL"""
        # 将本地路径转换为URL
        if file_path.startswith("uploads/"):
            return "/" + file_path.replace("\\", "/")
        return file_path

    @classmethod
    def delete_file(cls, file_path: str) -> bool:
        """删除文件"""
        try:
            if os.path.exists(file_path):
                os.remove(file_path)
                return True
            return False
        except Exception:
            return False

    @classmethod
    def get_file_size(cls, file_path: str) -> Optional[int]:
        """获取文件大小"""
        try:
            return os.path.getsize(file_path)
        except Exception:
            return None


file_service = FileService()
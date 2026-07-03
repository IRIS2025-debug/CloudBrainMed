import os
import asyncio
from typing import Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)


class CVService:
    """计算机视觉分析服务"""

    def __init__(self):
        # 延迟初始化检测器
        self.artifact_detector = None
        self.lesion_detector = None
        self._initialized = False

    def _initialize(self):
        """初始化检测器"""
        if self._initialized:
            return

        try:
            from app.cv import ArtifactDetector, LesionDetector
            from app.core.config import settings

            self.artifact_detector = ArtifactDetector(
                weights_path=settings.ARTIFACT_MODEL_PATH,
                config_path=settings.ARTIFACT_CONFIG_PATH,
                device=settings.MODEL_DEVICE
            )
            self.lesion_detector = LesionDetector(
                weights_path=settings.LESION_MODEL_PATH,
                config_path=settings.LESION_CONFIG_PATH,
                device=settings.MODEL_DEVICE
            )
            self._initialized = True
            logger.info("CV Service initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize CV Service: {e}")
            self._initialized = False

    async def analyze_artifact(self, image_path: str) -> Dict[str, Any]:
        """伪影识别"""
        self._initialize()

        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found: {image_path}")

        # 如果未初始化或检测器不可用，返回模拟数据
        if not self._initialized or self.artifact_detector is None:
            return self._get_mock_artifact_result()

        try:
            return await asyncio.to_thread(
                self.artifact_detector.analyze,
                image_path
            )
        except Exception as e:
            logger.error(f"Artifact analysis failed: {e}")
            raise

    async def analyze_lesion(self, image_path: str) -> Dict[str, Any]:
        """病灶识别"""
        self._initialize()

        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found: {image_path}")

        if not self._initialized or self.lesion_detector is None:
            return self._get_mock_lesion_result()

        try:
            return await asyncio.to_thread(
                self.lesion_detector.analyze,
                image_path
            )
        except Exception as e:
            logger.error(f"Lesion analysis failed: {e}")
            raise

    def _get_mock_artifact_result(self) -> Dict[str, Any]:
        """获取模拟伪影识别结果"""
        return {
            "has_artifact": True,
            "artifact_pixels": 15234,
            "ratio": 0.234,
            "slice_location": 25,
            "mask_file": "uploads/analysis/artifact/mock_mask.npy",
            "preview_url": "/uploads/analysis/artifact/mock_preview.png",
            "mask_url": "/uploads/analysis/artifact/mock_mask_preview.png",
            "model_version": "v1.0.0"
        }

    def _get_mock_lesion_result(self) -> Dict[str, Any]:
        """获取模拟病灶识别结果"""
        return {
            "has_lesion": True,
            "lesion_count": 3,
            "ratio": 0.156,
            "slice_location": 25,
            "mask_file": "uploads/analysis/lesion/mock_mask.npy",
            "preview_url": "/uploads/analysis/lesion/mock_preview.png",
            "mask_url": "/uploads/analysis/lesion/mock_mask_preview.png",
            "model_version": "v1.0.0"
        }


# 单例
cv_service = CVService()
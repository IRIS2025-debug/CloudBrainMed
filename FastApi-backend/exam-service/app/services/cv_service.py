import asyncio
import logging
import os
from typing import Any, Dict

logger = logging.getLogger(__name__)


class CVService:
    """Computer vision analysis service for the legacy exam-service."""

    def __init__(self):
        self.artifact_detector = None
        self.lesion_detector = None
        self._initialized = False

    def _initialize(self):
        if self._initialized:
            return

        try:
            from app.core.config import settings
            from app.cv import ArtifactDetector, LesionDetector

            self.artifact_detector = ArtifactDetector(
                weights_path=settings.ARTIFACT_MODEL_PATH,
                config_path=settings.ARTIFACT_CONFIG_PATH,
                device=settings.MODEL_DEVICE,
            )
            self.lesion_detector = LesionDetector(
                weights_path=settings.LESION_MODEL_PATH,
                config_path=settings.LESION_CONFIG_PATH,
                device=settings.MODEL_DEVICE,
            )
            self._initialized = True
            logger.info("CV service initialized successfully")
        except Exception as exception:
            logger.error("Failed to initialize CV service: %s", exception)
            self._initialized = False

    async def analyze_artifact(self, image_path: str) -> Dict[str, Any]:
        self._initialize()
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found: {image_path}")
        if not self._initialized or self.artifact_detector is None:
            raise RuntimeError("Artifact detector is unavailable")

        try:
            return await asyncio.to_thread(
                self.artifact_detector.analyze,
                image_path,
            )
        except Exception as exception:
            logger.error("Artifact analysis failed: %s", exception)
            raise

    async def analyze_lesion(self, image_path: str) -> Dict[str, Any]:
        self._initialize()
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image not found: {image_path}")
        if not self._initialized or self.lesion_detector is None:
            raise RuntimeError("Lesion detector is unavailable")

        try:
            return await asyncio.to_thread(
                self.lesion_detector.analyze,
                image_path,
            )
        except Exception as exception:
            logger.error("Lesion analysis failed: %s", exception)
            raise


cv_service = CVService()

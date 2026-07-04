"""
影像分析接口
提供伪影识别和病灶识别功能
"""
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from typing import Optional, List
from pydantic import BaseModel
import os
import shutil
from datetime import datetime
import uuid

# 导入CV服务（稍后创建）
from app.services.cv_service import cv_service
from app.services.file_service import file_service

router = APIRouter(
    prefix="/image-analysis",
    tags=["影像分析"]
)


# ============ 请求/响应模型 ============

class AnalysisRequest(BaseModel):
    """分析请求"""
    image_path: str
    patient_info: Optional[dict] = None


class AnalysisResponse(BaseModel):
    """分析响应"""
    status: str
    result: Optional[dict] = None
    message: str


class BatchAnalysisRequest(BaseModel):
    """批量分析请求"""
    image_paths: List[str]
    patient_info: Optional[dict] = None


# ============ 接口实现 ============

@router.post("/upload", summary="上传影像文件")
async def upload_image(
        file: UploadFile = File(...),
        patient_name: Optional[str] = Form(None),
        patient_age: Optional[int] = Form(None),
        patient_gender: Optional[str] = Form(None)
):
    """
    上传影像文件

    - **file**: 影像文件 (支持 DICOM, JPG, PNG, NIfTI)
    - **patient_name**: 患者姓名
    - **patient_age**: 患者年龄
    - **patient_gender**: 患者性别
    """
    try:
        # 验证文件类型
        allowed_extensions = {'.dcm', '.jpg', '.jpeg', '.png', '.nii', '.nii.gz'}
        file_ext = os.path.splitext(file.filename)[1].lower()

        # 处理 .nii.gz 特殊情况
        if file.filename.endswith('.nii.gz'):
            file_ext = '.nii.gz'

        if file_ext not in allowed_extensions:
            raise HTTPException(
                status_code=400,
                detail=f"不支持的文件格式，支持: {', '.join(allowed_extensions)}"
            )

        # 生成唯一文件名
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        unique_id = str(uuid.uuid4())[:8]
        filename = f"{timestamp}_{unique_id}_{file.filename}"

        # 保存文件
        upload_dir = "uploads/images"
        os.makedirs(upload_dir, exist_ok=True)
        file_path = os.path.join(upload_dir, filename)

        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # 保存患者信息
        patient_info = {
            "name": patient_name,
            "age": patient_age,
            "gender": patient_gender,
            "upload_time": datetime.now().isoformat()
        }

        # 保存到数据库或缓存（示例）
        # await save_upload_record(file_path, patient_info)

        return {
            "status": "success",
            "data": {
                "file_path": file_path,
                "filename": filename,
                "patient_info": patient_info,
                "upload_time": datetime.now().isoformat()
            },
            "message": "文件上传成功"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/artifact", response_model=AnalysisResponse, summary="伪影识别")
async def analyze_artifact(request: AnalysisRequest):
    """
    伪影识别接口

    检测影像中是否存在伪影，并返回相关信息：
    - 是否检测到伪影
    - 伪影像素数
    - 占比
    - 所在切片
    - Mask文件
    - 预览图
    - 模型版本
    """
    try:
        # 验证文件是否存在
        if not os.path.exists(request.image_path):
            raise HTTPException(
                status_code=404,
                detail=f"图像文件不存在: {request.image_path}"
            )

        # 调用CV服务进行伪影识别
        result = await cv_service.analyze_artifact(request.image_path)

        return AnalysisResponse(
            status="success",
            result=result,
            message="伪影识别完成"
        )

    except FileNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/lesion", response_model=AnalysisResponse, summary="病灶识别")
async def analyze_lesion(request: AnalysisRequest):
    """
    病灶识别接口

    检测影像中是否存在病灶，并返回相关信息：
    - 是否检测到病灶
    - 病灶数量
    - 病灶占比
    - 所在切片
    - Mask文件
    - 预览图
    - 模型版本
    """
    try:
        # 验证文件是否存在
        if not os.path.exists(request.image_path):
            raise HTTPException(
                status_code=404,
                detail=f"图像文件不存在: {request.image_path}"
            )

        # 调用CV服务进行病灶识别
        result = await cv_service.analyze_lesion(request.image_path)

        return AnalysisResponse(
            status="success",
            result=result,
            message="病灶识别完成"
        )

    except FileNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/batch", summary="批量分析")
async def analyze_batch(request: BatchAnalysisRequest):
    """
    批量影像分析接口

    同时对多张影像进行伪影识别和病灶识别
    """
    try:
        results = []
        for image_path in request.image_paths:
            if not os.path.exists(image_path):
                results.append({
                    "image_path": image_path,
                    "error": "文件不存在",
                    "artifact": None,
                    "lesion": None
                })
                continue

            try:
                # 并行执行伪影和病灶识别
                artifact_result = await cv_service.analyze_artifact(image_path)
                lesion_result = await cv_service.analyze_lesion(image_path)

                results.append({
                    "image_path": image_path,
                    "artifact": artifact_result,
                    "lesion": lesion_result,
                    "error": None
                })
            except Exception as e:
                results.append({
                    "image_path": image_path,
                    "artifact": None,
                    "lesion": None,
                    "error": str(e)
                })

        return {
            "status": "success",
            "data": {
                "total": len(results),
                "results": results
            },
            "message": f"批量分析完成，共处理 {len(results)} 张影像"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/result/{analysis_id}", summary="获取分析结果")
async def get_analysis_result(analysis_id: str):
    """
    获取历史分析结果

    - **analysis_id**: 分析ID
    """
    try:
        # 从数据库或缓存获取结果
        # result = await get_analysis_by_id(analysis_id)

        # 示例返回
        return {
            "status": "success",
            "data": {
                "analysis_id": analysis_id,
                "result": {
                    "artifact": {
                        "has_artifact": True,
                        "artifact_pixels": 15234,
                        "ratio": 0.234
                    },
                    "lesion": {
                        "has_lesion": True,
                        "lesion_count": 3,
                        "ratio": 0.156
                    }
                },
                "analyzed_at": datetime.now().isoformat()
            },
            "message": "获取结果成功"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/analysis/{analysis_id}", summary="删除分析结果")
async def delete_analysis_result(analysis_id: str):
    """
    删除分析结果
    """
    try:
        # 从数据库删除
        # await delete_analysis_by_id(analysis_id)

        return {
            "status": "success",
            "message": f"分析结果 {analysis_id} 已删除"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
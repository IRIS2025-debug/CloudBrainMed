"""
CT 金属伪影检测 AI 微服务（FastAPI）
- 全局加载模型（只加载一次，性能最优）
- NIfTI 文件上传 + AI 推理 + 掩码下载
- 特征向量提取接口
"""
import os
import shutil
import uuid
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
import SimpleITK as sitk

from Detection.CTArtifactInfer import CTArtifactInfer
from Detection.CTLesionInfer import CTLesionInfer
from Detection.ct_artifact_preview import save_ct_mask_preview_png, select_preview_slice_index
from Detection.ct_artifact_result import build_artifact_result
from Detection.ct_lesion_result import build_lesion_result

app = FastAPI(title="CT金属伪影检测AI服务", version="2.0")

# 跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局加载模型（启动时加载一次）
MODEL_PATH = os.environ.get("MODEL_PATH", "./Model/weights/best_attention_adamw.pth")
MODEL_TYPE = os.environ.get("MODEL_TYPE", "attention")
MODEL_VERSION = os.environ.get("MODEL_VERSION", "attention_adamw_e4")
LESION_MODEL_PATH = os.environ.get("LESION_MODEL_PATH", "./Model/weights/best_lesion_attention.pth")
LESION_MODEL_TYPE = os.environ.get("LESION_MODEL_TYPE", "attention")
LESION_MODEL_VERSION = os.environ.get("LESION_MODEL_VERSION", "lesion_attention_v1")

try:
    infer = CTArtifactInfer(model_weight_path=MODEL_PATH, model_type=MODEL_TYPE)
    lesion_infer = CTLesionInfer(model_weight_path=LESION_MODEL_PATH, model_type=LESION_MODEL_TYPE)
except Exception as e:
    print(f"Model load failed: {e}")
    raise

UPLOAD_DIR = "uploads"
RESULT_DIR = "results"
PREVIEW_DIR = "previews"
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(RESULT_DIR, exist_ok=True)
os.makedirs(PREVIEW_DIR, exist_ok=True)


def is_nifti_file(filename: str):
    return filename.lower().endswith((".nii", ".nii.gz"))


def is_safe_filename(filename: str):
    return bool(filename) and "/" not in filename and "\\" not in filename


# ============================
# 核心接口：CT 伪影检测
# ============================
@app.post("/predict-ct-artifact")
async def predict_ct(file: UploadFile = File(...)):
    try:
        if not is_nifti_file(file.filename):
            raise HTTPException(status_code=400, detail="只支持 .nii 或 .nii.gz 格式")

        original_filename = file.filename
        unique_id = str(uuid.uuid4())
        name_without_ext = original_filename.replace(".nii.gz", "").replace(".nii", "")

        upload_path = os.path.join(UPLOAD_DIR, f"{unique_id}_{original_filename}")
        with open(upload_path, "wb") as f:
            shutil.copyfileobj(file.file, f)

        sitk_ct = sitk.ReadImage(upload_path)
        mask_filename = f"{unique_id}_{name_without_ext}_mask.nii.gz"
        mask_save_path = os.path.join(RESULT_DIR, mask_filename)
        mask_sitk = infer.predict_from_sitk(sitk_ct, save_mask_path=mask_save_path)
        mask_array = sitk.GetArrayFromImage(mask_sitk)
        ct_array = sitk.GetArrayFromImage(sitk_ct)
        preview_slice_index = select_preview_slice_index(mask_array)
        preview_filename = f"{unique_id}_{name_without_ext}_preview_z{preview_slice_index}.png"
        preview_save_path = os.path.join(PREVIEW_DIR, preview_filename)
        save_ct_mask_preview_png(
            ct_array=ct_array,
            mask_array=mask_array,
            slice_index=preview_slice_index,
            save_path=preview_save_path,
        )

        return build_artifact_result(
            original_filename=original_filename,
            mask_filename=mask_filename,
            mask_array=mask_array,
            image_size=mask_sitk.GetSize(),
            spacing=sitk_ct.GetSpacing(),
            origin=sitk_ct.GetOrigin(),
            download_url=f"/results/{mask_filename}",
            preview_filename=preview_filename,
            preview_url=f"/previews/{preview_filename}",
            preview_slice_index=preview_slice_index,
            model_type=MODEL_TYPE,
            model_version=MODEL_VERSION,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"服务处理失败: {str(e)}")


@app.post("/predict-ct-lesion")
async def predict_ct_lesion(file: UploadFile = File(...)):
    try:
        if not is_nifti_file(file.filename):
            raise HTTPException(status_code=400, detail="只支持 .nii 或 .nii.gz 格式")

        original_filename = file.filename
        unique_id = str(uuid.uuid4())
        name_without_ext = original_filename.replace(".nii.gz", "").replace(".nii", "")

        upload_path = os.path.join(UPLOAD_DIR, f"{unique_id}_{original_filename}")
        with open(upload_path, "wb") as f:
            shutil.copyfileobj(file.file, f)

        sitk_ct = sitk.ReadImage(upload_path)
        mask_filename = f"{unique_id}_{name_without_ext}_lesion_mask.nii.gz"
        mask_save_path = os.path.join(RESULT_DIR, mask_filename)
        mask_sitk = lesion_infer.predict_from_sitk(sitk_ct, save_mask_path=mask_save_path)
        mask_array = sitk.GetArrayFromImage(mask_sitk)
        ct_array = sitk.GetArrayFromImage(sitk_ct)
        preview_slice_index = select_preview_slice_index(mask_array)
        preview_filename = f"{unique_id}_{name_without_ext}_lesion_preview_z{preview_slice_index}.png"
        preview_save_path = os.path.join(PREVIEW_DIR, preview_filename)
        save_ct_mask_preview_png(
            ct_array=ct_array,
            mask_array=mask_array,
            slice_index=preview_slice_index,
            save_path=preview_save_path,
        )

        model_version = LESION_MODEL_VERSION if not lesion_infer.fallback else "heuristic_no_weights"
        return build_lesion_result(
            original_filename=original_filename,
            mask_filename=mask_filename,
            mask_array=mask_array,
            image_size=mask_sitk.GetSize(),
            spacing=sitk_ct.GetSpacing(),
            origin=sitk_ct.GetOrigin(),
            download_url=f"/results/{mask_filename}",
            preview_filename=preview_filename,
            preview_url=f"/previews/{preview_filename}",
            preview_slice_index=preview_slice_index,
            model_type=LESION_MODEL_TYPE,
            model_version=model_version,
            fallback=lesion_infer.fallback,
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"病灶推理失败: {str(e)}")


# ============================
# 特征向量提取接口
# ============================
@app.post("/extract-features")
async def extract_features(file: UploadFile = File(...)):
    """推理并返回融合特征向量"""
    try:
        if not is_nifti_file(file.filename):
            raise HTTPException(status_code=400, detail="只支持 .nii 或 .nii.gz 格式")

        unique_id = str(uuid.uuid4())
        upload_path = os.path.join(UPLOAD_DIR, f"{unique_id}_{file.filename}")
        with open(upload_path, "wb") as f:
            shutil.copyfileobj(file.file, f)

        sitk_ct = sitk.ReadImage(upload_path)
        _ = infer.predict_from_sitk(sitk_ct)  # 必须先推理

        feature_vector = infer.extract_features()
        return {
            "status": "success",
            "feature_dim": feature_vector.shape[1],
            "feature_vector": feature_vector.detach().cpu().numpy().tolist()
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"特征提取失败: {str(e)}")


# ============================
# 掩码下载
# ============================
@app.get("/results/{mask_filename}")
async def download_mask(mask_filename: str):
    if not is_safe_filename(mask_filename):
        raise HTTPException(status_code=400, detail="掩码文件名无效")
    mask_path = os.path.join(RESULT_DIR, mask_filename)
    if not os.path.exists(mask_path):
        raise HTTPException(status_code=404, detail="掩码文件不存在")
    return FileResponse(path=mask_path, media_type="application/octet-stream", filename=mask_filename)


@app.get("/previews/{preview_filename}")
async def download_preview(preview_filename: str):
    if not is_safe_filename(preview_filename):
        raise HTTPException(status_code=400, detail="预览图文件名无效")
    preview_path = os.path.join(PREVIEW_DIR, preview_filename)
    if not os.path.exists(preview_path):
        raise HTTPException(status_code=404, detail="预览图文件不存在")
    return FileResponse(path=preview_path, media_type="image/png", filename=preview_filename)


# ============================
# 健康检查
# ============================
@app.get("/")
async def root():
    return {
        "status": "ok",
        "message": "CT金属伪影检测服务运行中",
        "model_type": MODEL_TYPE,
        "model_version": MODEL_VERSION,
        "lesion_model_type": LESION_MODEL_TYPE,
        "lesion_model_version": LESION_MODEL_VERSION if not lesion_infer.fallback else "heuristic_no_weights",
        "lesion_fallback": lesion_infer.fallback,
        "device": str(infer.device)
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "8010")))

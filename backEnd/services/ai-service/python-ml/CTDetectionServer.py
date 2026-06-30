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
MODEL_PATH = os.environ.get("MODEL_PATH", "./Model/weights/best.pth")
MODEL_TYPE = os.environ.get("MODEL_TYPE", "unet")

try:
    infer = CTArtifactInfer(model_weight_path=MODEL_PATH, model_type=MODEL_TYPE)
except Exception as e:
    print(f"❌ 模型加载失败: {e}")
    raise

UPLOAD_DIR = "uploads"
RESULT_DIR = "results"
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(RESULT_DIR, exist_ok=True)


def is_nifti_file(filename: str):
    return filename.lower().endswith((".nii", ".nii.gz"))


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

        return {
            "status": "success",
            "message": "CT金属伪影检测完成",
            "original_file": original_filename,
            "mask_file": mask_filename,
            "shape": mask_sitk.GetSize(),
            "spacing": list(sitk_ct.GetSpacing()),
            "origin": list(sitk_ct.GetOrigin()),
            "download_url": f"/results/{mask_filename}"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"服务处理失败: {str(e)}")


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
    mask_path = os.path.join(RESULT_DIR, mask_filename)
    if not os.path.exists(mask_path):
        raise HTTPException(status_code=404, detail="掩码文件不存在")
    return FileResponse(path=mask_path, media_type="application/octet-stream", filename=mask_filename)


# ============================
# 健康检查
# ============================
@app.get("/")
async def root():
    return {
        "status": "ok",
        "message": "CT金属伪影检测服务运行中",
        "model_type": MODEL_TYPE,
        "device": str(infer.device)
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "8010")))

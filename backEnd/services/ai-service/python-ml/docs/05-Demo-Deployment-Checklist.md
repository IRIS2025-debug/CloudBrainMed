# 模块演示与部署检查清单

> 用途：答辩或联调前逐项确认，覆盖新云数据库、AI 辅助接诊、MLOps、CT 推理和六组模型实验留痕。

## 1. 新云数据库

- 后端源码配置已指向新库：`cloudbrainmed.rwlb.rds.aliyuncs.com:5432/cloudbrainmed`。
- 已检查服务：`auth-service`、`payment-service`、`admin-service`、`patient-service`、`doctor-service`、`ai-service`。
- 不要在文档或截图中展示数据库密码。

训练任务持久化还需要在新库执行：

```sql
\i backEnd/services/ai-service/src/main/resources/db/ai_training_task.sql
```

如果用图形化工具执行，直接打开 `backEnd/services/ai-service/src/main/resources/db/ai_training_task.sql` 并运行即可。

## 2. 后端服务

至少启动以下服务用于你的模块演示：

- `gateway-server`
- `auth-service`
- `doctor-service`
- `admin-service`
- `patient-service`
- `ai-service`

AI 报告分析需要配置大模型 Key：

```powershell
$env:DASHSCOPE_API_KEY="实际 Key"
```

CT 推理和 Web 端训练任务需要 AI Python 服务可访问：

```powershell
$env:AI_PYTHON_SERVICE_URL="http://localhost:8010"
```

## 3. Python 推理服务

在 `python-ml` 目录启动：

```powershell
python CTDetectionServer.py
```

默认监听 `http://localhost:8010`，避免和 `admin-service` 的 `8000` 端口冲突。如需改端口，启动前设置：

```powershell
$env:PORT="8010"
python CTDetectionServer.py
```

演示前确认：

- `Model/weights/best.pth` 存在。
- 上传 CT 文件后，管理员端 CT 推理页能返回 mask 结果。
- mask 下载链接走 Java 后端代理，不直接暴露 Python 服务地址。

## 4. Web 前端

本项目医生端、管理员端、接诊页面按 Web 端演示，不考虑手机端。

本地联调可直接使用 Vite 代理；如果网关不是本机默认地址，构建或启动前配置：

```powershell
$env:VITE_API_BASE_URL="http://localhost"
```

`VITE_API_BASE_URL` 必须指向 Spring 网关或后端 API 入口，不能指向 Python AI 服务。Python AI 服务只给 Java 后端通过 `AI_PYTHON_SERVICE_URL` 调用。

重点演示页面：

- 医生端：接诊列表、接诊详情、AI 报告分析、草稿保存、完成后禁改。
- 医生端：个人信息编辑、头像上传、修改密码。
- 管理员端：个人信息编辑、头像上传、修改密码。
- 管理员端：MLOps Dashboard、模型列表、训练任务、样本标注、CT 推理。

AI 排班前端大面积缺失属于其他负责人未上传代码，不纳入本模块验收清单。

## 5. 六组模型实验留痕

老师要求的“留痕”主要看这部分，不等同于 Java 后端的训练任务持久化。

队友跑完后，把 `experiments/` 目录同步回：

```text
backEnd/services/ai-service/python-ml/experiments/
```

每组实验至少保留：

- `config.yaml`
- `train.log`
- `metrics.json`
- `best.pth`
- `loss_curve.png`
- `dice_f1_curve.png`
- `acc_curve.png`

生成汇总表：

```powershell
cd backEnd/services/ai-service/python-ml
python tools/summarize_experiments.py
```

输出文件：

- `docs/generated/experiment-summary.md`
- `docs/generated/experiment-summary.csv`

最终选择模型时，优先按 Best Dice 排序；Dice 接近时再看 F1、Accuracy、曲线稳定性和预测样例。

## 6. 最后一轮验证命令

后端聚焦验证：

```powershell
cd backEnd
.\mvnw.cmd -pl services/patient-service,services/doctor-service,services/ai-service -am "-Dtest=PatientProfileServiceImplTest,DoctorServiceImplTest,ConsultServiceImplTest,ConsultControllerTest,MlOpsControllerTest,MlOpsServiceImplTest,ModelTrainerTest,InferenceEngineTest,AiReportControllerTest,AiReportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

前端验证：

```powershell
cd frontEnd/vueFront
npm run build
```

训练留痕脚本验证：

```powershell
cd backEnd/services/ai-service/python-ml
python -m py_compile tools/summarize_experiments.py
python tools/summarize_experiments.py
```

# CloudBrainMed Agent Instructions

Before working in this repository, read the project documents under `D:\CloudBrainMed\docs` and use them as the source of truth for implementation decisions:

- `CloudBrainMed API 接口文档.md`
- `CloudBrainMed 数据库设计文档.md`
- `CloudBrainMed项目架构文档.md`
- `CloudBrainMed——docker使用说明.md`

For feature work, especially anything involving the doctor reception workflow, medical records, prescriptions, AI assistant, or service routes, cross-check the docs against the current source code before changing files. If the docs and code disagree, mention the discrepancy and prefer the current code for runtime behavior.

When changing an owned area that already appears in `CloudBrainMed API 接口文档.md`, keep that API document in sync. Update affected interface paths, request parameters, response bodies, DTO/entity field descriptions, and business rules in the same task whenever implementation changes them or source-code verification shows the document is stale.

Prioritize implemented business logic, smooth project workflows, clear business rules, and bug-free behavior. When the request concerns an end-to-end workflow, trace and verify the related read/write/API flow so the implemented business function actually works without runtime errors or obvious downstream bugs. If business workflow completeness conflicts with keeping a tiny diff, business workflow completeness wins; make every necessary code, API, frontend/backend, and integration change needed to genuinely connect the flow. Concise/surgical changes mean avoiding unrelated refactors and speculative features, not leaving a workflow partially connected. Avoid unnecessary abstractions, broad rewrites, and unrelated cleanup. Do not modify generated build artifacts, `target` directories, secrets, or deployment configuration unless explicitly requested.

Current owned scope for this checkout: patient backend, doctor frontend/backend, doctor reception, administrator frontend/backend scaffolding, and the full CV model module, including CT metal artifact recognition and CT lesion recognition/segmentation. For cross-module integration that affects this scope, verify the other module's implemented contract and wire the flow through when possible; if another owner’s unfinished module blocks the flow, report the blocker with concrete evidence instead of silently bypassing it.

If you create a temporary task plan document while executing a task, delete that plan document after the task has been implemented or completed. Do not delete permanent project documentation or user-provided documents.

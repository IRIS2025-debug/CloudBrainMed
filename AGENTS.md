# CloudBrainMed Agent Instructions

Before working in this repository, read the project documents under `D:\CloudBrainMed\docs` and use them as the source of truth for implementation decisions:

- `CloudBrainMed API 接口文档.md`
- `CloudBrainMed 数据库设计文档.md`
- `CloudBrainMed项目架构文档.md`
- `CloudBrainMed——docker使用说明.md`

For feature work, especially anything involving the doctor reception workflow, medical records, prescriptions, AI assistant, or service routes, cross-check the docs against the current source code before changing files. If the docs and code disagree, mention the discrepancy and prefer the current code for runtime behavior.

When changing an owned area that already appears in `CloudBrainMed API 接口文档.md`, keep that API document in sync. Update affected interface paths, request parameters, response bodies, DTO/entity field descriptions, and business rules in the same task whenever implementation changes them or source-code verification shows the document is stale.

Prioritize complete business workflows, working project functionality, and bug-free behavior. Keep code as simple and focused as possible while meeting those goals. Do not modify generated build artifacts, `target` directories, secrets, or deployment configuration unless explicitly requested.

If you create a temporary task plan document while executing a task, delete that plan document after the task has been implemented or completed. Do not delete permanent project documentation or user-provided documents.

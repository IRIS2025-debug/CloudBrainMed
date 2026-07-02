# Project Materials

This folder keeps restored references and CT model defense evidence out of the
frontend and backend source trees.

## Folders

- `restored-reference-materials/`: course examples, teacher-provided reference
  code, temporary inspection output, and other restored materials that are not
  part of the running system.
- `ct-model-defense-traces/`: CT metal artifact dataset copies, six training
  experiment archives, generated summaries, report notes, and defense evidence.

## Git Policy

Large local evidence files are intentionally ignored, including DICOM/NIfTI
images, archives, checkpoints, logs, and temporary binaries. Trackable files here
should be lightweight indexes, Markdown notes, CSV summaries, and README files.

The deployed CT artifact inference weight remains in
`backEnd/services/ai-service/python-ml/Model/weights/` because
`CTDetectionServer.py` loads it at runtime.

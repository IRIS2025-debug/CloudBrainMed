# Doctor Consult AI Workbench Design

## Context

The doctor consult detail page is the main screen for patient reception, medical record editing, exam orders, prescriptions, report analysis, and AI assistance. The existing backend stores medical records as a single `medical_record.description` text field and the doctor consult APIs accept a single `recordDesc` string for draft save and record confirmation.

The implementation must preserve backend compatibility while improving the frontend editing experience and AI assistant workflow.

## Assumptions

- Medical record persistence remains string-based through `recordDesc`; no database or doctor-service API change is required for this feature.
- The frontend may maintain structured medical record sections locally and serialize them into a single record string before save, confirm, or AI calls.
- AI reception chat runtime behavior should be based on current backend DTOs and controllers. If the API document differs from current source behavior in the owned work area, update the API document.
- AI prescription review requires `medicineId`, `usage`, and `quantity`; drug name alone is not enough for a valid backend request.

## Scope

1. Replace the single medical record textarea in `frontEnd/vueFront/src/pages/doctor/consult/Detail.vue` with sectioned inputs.
2. Keep save draft and confirm record compatible by serializing sections into `recordDesc`.
3. Replace the AI reception panel with a chat-style doctor assistant input and quick actions.
4. Support the seven current reception chat `actionType` values:
   - `FOLLOW_UP_QUESTION`
   - `MISSING_INFORMATION`
   - `CONTEXT_SUMMARY`
   - `CONTEXT_QA`
   - `MEDICAL_RECORD_DRAFT`
   - `PRESCRIPTION_REVIEW`
   - `DIAGNOSIS_ASSISTANT`
5. Show compact quick actions; if space is tight, place overflow actions under a More menu.
6. Add a medicine selector to the prescription form so AI prescription review can send `medicineId`.
7. Update `docs/CloudBrainMed API 接口文档.md` for every touched or verified owned-area interface that already appears in the document. This includes interface paths, request parameters, response bodies, DTO/entity fields, and business rules.

## Medical Record Editing

The record editor will use these frontend sections:

- Chief complaint
- History of present illness
- Past medical history
- Physical examination
- Auxiliary examination
- Diagnosis
- Treatment plan

When loading existing `description`, the frontend will parse known headings such as `主诉：`, `现病史：`, `既往史：`, `体格检查：`, `辅助检查：`, `诊断意见：`, and `处理计划：`. If parsing fails, the original text will be placed into the history section so no existing content is lost.

Before save, confirm, report append, or AI calls, the frontend will serialize the sections into a readable multiline medical record string.

## AI Assistant Chat

The AI assistant area will keep the existing "辅助接诊 / 报告分析" tab split. The reception tab becomes a chat surface:

- Message history with doctor and AI bubbles.
- Bottom input box for free-text questions.
- Quick actions inspired by the provided Doubao-style example.
- Loading state per AI request.
- Result status displayed in the AI response when returned.

For normal chat and assistant-handled action types, requests will include:

- `registerId`
- `message`
- `actionType`
- `currentRecordDesc`
- `symptomDescription`
- `structuredParameters`
- `followUpAnswers` when available

For `MEDICAL_RECORD_DRAFT`, the frontend will also ensure `conversationText` or `structuredParameters` is populated, because the backend medical record generation request requires at least one of them. If the response includes a generated draft in `moduleResult.draftRecordDesc`, the UI will offer to apply it into the sectioned editor.

For `PRESCRIPTION_REVIEW`, the frontend will send the selected medicine's `medicineId`, usage, and quantity. If no medicine is selected, the UI will ask the doctor to select a medicine before calling AI review.

## Medicine Selector

The prescription form will add medicine selection before AI prescription review. The selector should use an existing medicine list API if one is already available in the frontend/backend. It should populate or preserve:

- `medicineId`
- `medicineName`
- `spec`
- `usage`
- `price`

Manual edits to usage, quantity, and price remain possible after selection. Creating a prescription should continue to work through the current doctor consult API. If that API does not yet accept `medicineId`, the API document and code discrepancy should be documented, and the smallest compatible code change should be made only if required by runtime behavior.

## Report Analysis

Report analysis remains on its existing tab and continues to call `/ai-service/report/analyze` with `registerId`, `reportType`, and `reportText`. When writing report analysis back into the record, it should append into the serialized medical record content without losing sectioned editor state.

## Testing And Verification

Minimum verification:

- Frontend type check or build for `frontEnd/vueFront`.
- Focused manual/runtime check of the consult detail page if a dev server can run.
- Verify save draft and confirm record still send a single `recordDesc`.
- Verify AI chat requests include the expected `actionType` and structured context.
- Verify AI prescription review is blocked until `medicineId`, usage, and quantity are available.
- Verify API documentation is updated for any discovered or introduced mismatch in the touched owned areas, including request and response models.

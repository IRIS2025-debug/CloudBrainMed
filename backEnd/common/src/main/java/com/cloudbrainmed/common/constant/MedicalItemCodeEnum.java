package com.cloudbrainmed.common.constant;

public enum MedicalItemCodeEnum {
    // 🏥 EXAM（检查类）
    NEURO_CT_001(MedicalItemCategoryEnum.EXAM),
    NEURO_CT_002(MedicalItemCategoryEnum.EXAM),
    NEURO_MRI_001(MedicalItemCategoryEnum.EXAM),
    NEURO_MRI_002(MedicalItemCategoryEnum.EXAM),
    NEURO_MRA_001(MedicalItemCategoryEnum.EXAM),
    CRANIAL_CT_PLAIN(MedicalItemCategoryEnum.EXAM),
    // 🧪 LAB（检验类）
    NEURO_LAB_001(MedicalItemCategoryEnum.LAB),
    NEURO_LAB_002(MedicalItemCategoryEnum.LAB),
    NEURO_LAB_003(MedicalItemCategoryEnum.LAB),
    CSF_ROUTINE(MedicalItemCategoryEnum.LAB);

    private final MedicalItemCategoryEnum category;

    MedicalItemCodeEnum(MedicalItemCategoryEnum category) {
        this.category = category;
    }

    public MedicalItemCategoryEnum getCategory() {
        return category;
    }

    public static boolean isSupported(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        try {
            valueOf(code);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

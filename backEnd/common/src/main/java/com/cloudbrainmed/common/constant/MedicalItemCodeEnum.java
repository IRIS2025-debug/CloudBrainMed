package com.cloudbrainmed.common.constant;

public enum MedicalItemCodeEnum {
    CRANIAL_CT_PLAIN(MedicalItemCategoryEnum.EXAM),
    CRANIAL_CT_ENHANCED(MedicalItemCategoryEnum.EXAM),
    CRANIAL_MRI_PLAIN(MedicalItemCategoryEnum.EXAM),
    CRANIAL_MRI_ENHANCED(MedicalItemCategoryEnum.EXAM),
    HEAD_NECK_CTA(MedicalItemCategoryEnum.EXAM),
    CRANIAL_MRA(MedicalItemCategoryEnum.EXAM),
    CEREBRAL_DSA(MedicalItemCategoryEnum.EXAM),
    TCD(MedicalItemCategoryEnum.EXAM),
    ROUTINE_EEG(MedicalItemCategoryEnum.EXAM),
    VIDEO_EEG(MedicalItemCategoryEnum.EXAM),
    EMG(MedicalItemCategoryEnum.EXAM),
    EVOKED_POTENTIAL(MedicalItemCategoryEnum.EXAM),
    CSF_ROUTINE(MedicalItemCategoryEnum.LAB),
    CSF_BIOCHEMISTRY(MedicalItemCategoryEnum.LAB),
    CSF_CYTOLOGY(MedicalItemCategoryEnum.LAB),
    CSF_CULTURE(MedicalItemCategoryEnum.LAB),
    CSF_OLIGOCLONAL_BANDS(MedicalItemCategoryEnum.LAB),
    AUTOIMMUNE_ENCEPHALITIS_ANTIBODY(MedicalItemCategoryEnum.LAB),
    DEMYELINATING_DISEASE_ANTIBODY(MedicalItemCategoryEnum.LAB);

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

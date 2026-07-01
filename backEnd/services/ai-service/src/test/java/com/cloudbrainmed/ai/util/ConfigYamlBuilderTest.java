package com.cloudbrainmed.ai.util;

import com.cloudbrainmed.ai.model.CnnModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigYamlBuilderTest {

    @Test
    void buildUsesRequestedDatasetPathForCtAndMaskDirs() {
        String yaml = ConfigYamlBuilder.build(
                CnnModel.HyperParams.defaultParams(),
                CnnModel.ModelType.UNET,
                "D:/CloudBrainMed/datasets");

        assertThat(yaml).contains("ct_dir: \"D:/CloudBrainMed/datasets/CT\"");
        assertThat(yaml).contains("mask_dir: \"D:/CloudBrainMed/datasets/MASK\"");
        assertThat(yaml).doesNotContain("../../../../BrainCT/BrainCT/Datasets");
    }
}

import unittest

import numpy as np

from Detection.ct_artifact_result import build_artifact_result


class CtArtifactResultTest(unittest.TestCase):
    def test_build_result_returns_structured_fields_for_llm_report(self):
        mask = np.array(
            [
                [[0, 1], [0, 0]],
                [[1, 1], [0, 0]],
            ],
            dtype=np.int16,
        )

        result = build_artifact_result(
            original_filename="scan.nii.gz",
            mask_filename="scan_mask.nii.gz",
            mask_array=mask,
            image_size=(2, 2, 2),
            spacing=(0.5, 0.5, 1.0),
            origin=(0.0, 0.0, 0.0),
            download_url="/results/scan_mask.nii.gz",
            model_type="attention",
            model_version="attention_adamw_e4",
        )

        self.assertEqual(result["status"], "success")
        self.assertTrue(result["artifactDetected"])
        self.assertEqual(result["positivePixels"], 3)
        self.assertEqual(result["totalPixels"], 8)
        self.assertEqual(result["artifactRatio"], 37.5)
        self.assertEqual(result["maskFile"], "scan_mask.nii.gz")
        self.assertEqual(result["modelType"], "attention")
        self.assertEqual(result["artifactSliceIndices"], [0, 1])

        report_input = result["reportInput"]
        self.assertEqual(report_input["task"], "CT_ARTIFACT_REPORT")
        self.assertEqual(report_input["finding"]["artifactDetected"], True)
        self.assertEqual(report_input["finding"]["artifactRatio"], 37.5)
        self.assertEqual(report_input["finding"]["artifactSliceIndices"], [0, 1])
        self.assertIn("检测到CT金属伪影", report_input["summary"])

    def test_build_result_returns_preview_slice_fields_for_web_display(self):
        mask = np.array(
            [
                [[0, 1], [0, 0]],
                [[1, 1], [0, 0]],
            ],
            dtype=np.int16,
        )

        result = build_artifact_result(
            original_filename="scan.nii.gz",
            mask_filename="scan_mask.nii.gz",
            mask_array=mask,
            image_size=(2, 2, 2),
            spacing=(0.5, 0.5, 1.0),
            origin=(0.0, 0.0, 0.0),
            download_url="/results/scan_mask.nii.gz",
            preview_filename="scan_preview_z1.png",
            preview_url="/previews/scan_preview_z1.png",
            preview_slice_index=1,
            model_type="attention",
            model_version="attention_adamw_e4",
        )

        self.assertEqual(result["previewImageFile"], "scan_preview_z1.png")
        self.assertEqual(result["previewImageUrl"], "/previews/scan_preview_z1.png")
        self.assertEqual(result["previewSliceIndex"], 1)
        self.assertEqual(result["reportInput"]["finding"]["previewSliceIndex"], 1)


if __name__ == "__main__":
    unittest.main()

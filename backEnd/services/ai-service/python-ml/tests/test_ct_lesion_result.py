import unittest

import numpy as np

from Detection.ct_lesion_result import build_lesion_result


class CtLesionResultTest(unittest.TestCase):
    def test_build_result_returns_structured_fields_for_lesion_report(self):
        mask = np.array(
            [
                [[0, 1], [0, 0]],
                [[1, 1], [0, 0]],
            ],
            dtype=np.int16,
        )

        result = build_lesion_result(
            original_filename="scan.nii.gz",
            mask_filename="scan_lesion_mask.nii.gz",
            mask_array=mask,
            image_size=(2, 2, 2),
            spacing=(0.5, 0.5, 1.0),
            origin=(0.0, 0.0, 0.0),
            download_url="/results/scan_lesion_mask.nii.gz",
            model_type="attention",
            model_version="lesion_attention_v1",
        )

        self.assertEqual(result["status"], "success")
        self.assertTrue(result["lesionDetected"])
        self.assertEqual(result["lesionPixels"], 3)
        self.assertEqual(result["totalPixels"], 8)
        self.assertEqual(result["lesionRatio"], 37.5)
        self.assertEqual(result["lesionSliceIndices"], [0, 1])
        self.assertEqual(result["lesionCount"], 1)
        self.assertEqual(result["largestLesionPixels"], 3)
        self.assertEqual(result["modelType"], "attention")

        report_input = result["reportInput"]
        self.assertEqual(report_input["task"], "CT_LESION_REPORT")
        self.assertEqual(report_input["finding"]["lesionDetected"], True)
        self.assertEqual(report_input["finding"]["lesionRatio"], 37.5)
        self.assertEqual(report_input["finding"]["lesionSliceIndices"], [0, 1])
        self.assertIn("CT病灶候选区", report_input["summary"])

    def test_build_result_returns_preview_fields_for_web_display(self):
        mask = np.array(
            [
                [[0, 0], [0, 0]],
                [[1, 1], [0, 0]],
            ],
            dtype=np.int16,
        )

        result = build_lesion_result(
            original_filename="scan.nii.gz",
            mask_filename="scan_lesion_mask.nii.gz",
            mask_array=mask,
            image_size=(2, 2, 2),
            spacing=(0.5, 0.5, 1.0),
            origin=(0.0, 0.0, 0.0),
            download_url="/results/scan_lesion_mask.nii.gz",
            preview_filename="scan_lesion_preview_z1.png",
            preview_url="/previews/scan_lesion_preview_z1.png",
            preview_slice_index=1,
            model_type="attention",
            model_version="lesion_attention_v1",
        )

        self.assertEqual(result["previewImageFile"], "scan_lesion_preview_z1.png")
        self.assertEqual(result["previewImageUrl"], "/previews/scan_lesion_preview_z1.png")
        self.assertEqual(result["previewSliceIndex"], 1)
        self.assertEqual(result["reportInput"]["finding"]["previewSliceIndex"], 1)


if __name__ == "__main__":
    unittest.main()

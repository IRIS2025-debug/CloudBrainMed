import tempfile
import unittest
from pathlib import Path

import numpy as np

from Detection.ct_artifact_preview import (
    save_ct_mask_preview_png,
    select_preview_slice_index,
)


class CtArtifactPreviewTest(unittest.TestCase):
    def test_select_preview_slice_index_uses_slice_with_most_mask_pixels(self):
        mask = np.array(
            [
                [[0, 1], [0, 0]],
                [[1, 1], [0, 0]],
                [[0, 0], [0, 0]],
            ],
            dtype=np.int16,
        )

        self.assertEqual(select_preview_slice_index(mask), 1)

    def test_save_ct_mask_preview_png_writes_rgb_overlay(self):
        try:
            import SimpleITK as sitk
        except ModuleNotFoundError:
            self.skipTest("SimpleITK is not installed in this test environment")

        ct = np.arange(12, dtype=np.float32).reshape(3, 2, 2)
        mask = np.zeros((3, 2, 2), dtype=np.int16)
        mask[1, 0, 0] = 1

        with tempfile.TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "preview.png"

            save_ct_mask_preview_png(
                ct_array=ct,
                mask_array=mask,
                slice_index=1,
                save_path=path,
            )

            self.assertTrue(path.exists())
            image = sitk.GetArrayFromImage(sitk.ReadImage(str(path)))
            self.assertEqual(image.shape, (2, 2, 3))
            self.assertGreater(int(image[0, 0, 0]), int(image[0, 0, 1]))


if __name__ == "__main__":
    unittest.main()

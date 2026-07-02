import tempfile
import unittest
from pathlib import Path

from training.dataset import list_paired_training_files


class TrainingDatasetFilesTest(unittest.TestCase):
    def test_list_paired_training_files_supports_npy_lesion_slices(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            ct_dir = root / "CT"
            mask_dir = root / "MASK"
            ct_dir.mkdir()
            mask_dir.mkdir()
            (ct_dir / "case_001_z010.npy").write_bytes(b"ct")
            (mask_dir / "case_001_z010.npy").write_bytes(b"mask")
            (ct_dir / "case_002_z011.npy").write_bytes(b"ct")

            files = list_paired_training_files(str(ct_dir), str(mask_dir))

        self.assertEqual(files, ["case_001_z010.npy"])


if __name__ == "__main__":
    unittest.main()

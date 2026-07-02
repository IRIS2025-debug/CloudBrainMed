import ast
from pathlib import Path
import unittest


class CtDetectionServerErrorHandlingTest(unittest.TestCase):
    def test_predict_ct_lesion_reraises_http_exception_before_broad_exception(self):
        source_path = Path(__file__).resolve().parents[1] / "CTDetectionServer.py"
        module = ast.parse(source_path.read_text(encoding="utf-8"))
        function = next(
            node for node in module.body
            if isinstance(node, ast.AsyncFunctionDef) and node.name == "predict_ct_lesion"
        )
        try_node = next(node for node in ast.walk(function) if isinstance(node, ast.Try))

        caught_names = [
            handler.type.id if isinstance(handler.type, ast.Name) else None
            for handler in try_node.handlers
        ]

        self.assertIn("HTTPException", caught_names)
        self.assertLess(caught_names.index("HTTPException"), caught_names.index("Exception"))


if __name__ == "__main__":
    unittest.main()

# scripts/init_rag_from_docs.py
# !/usr/bin/env python
"""
从文档目录初始化RAG知识库
支持 .docx 和 .pdf 格式，自动分块
"""
import sys
import os
import asyncio
import logging
from pathlib import Path

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.services.document_parser import BatchDocumentParser
from app.services.rag_service import RAGService

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ✅ 默认文档目录：data/（简化）
DEFAULT_DOCS_DIR = Path(__file__).parent.parent / "data"


async def init_from_directory(
        directory_path: str = None,
        enable_chunking: bool = True,
        clear_existing: bool = False
):
    """
    从目录解析文档并初始化知识库

    Args:
        directory_path: 文档目录路径（默认：data/）
        enable_chunking: 是否启用文本分割
        clear_existing: 是否清空现有数据
    """
    # 使用默认路径
    if directory_path is None:
        directory_path = str(DEFAULT_DOCS_DIR)

    logger.info("=" * 60)
    logger.info("从文档初始化RAG知识库")
    logger.info(f"文档目录: {directory_path}")
    logger.info(f"文本分割: {'启用' if enable_chunking else '禁用'}")
    logger.info("=" * 60)

    # 1. 检查目录
    directory = Path(directory_path)
    if not directory.exists():
        logger.error(f"目录不存在: {directory_path}")
        return

    # 2. 获取文档列表（支持 .docx 和 .pdf）
    docs = list(directory.glob("*.docx")) + list(directory.glob("*.pdf"))
    if not docs:
        logger.warning(f"未找到 .docx 或 .pdf 文件在: {directory_path}")
        logger.info("请将案例文档放在 data/ 目录下")
        logger.info("文档格式参考: 病例编号, 影像特征, AI分析结论, 医生确认")
        return

    logger.info(f"找到 {len(docs)} 个文档")
    for doc in docs:
        logger.info(f"  - {doc.name}")

    # 3. 解析文档
    parser = BatchDocumentParser(enable_chunking=enable_chunking)
    cases = parser.parse_directory(directory_path, recursive=False)

    if not cases:
        logger.warning("没有解析到任何案例")
        return

    logger.info(f"成功解析 {len(cases)} 个案例")

    # 统计分块情况
    chunked_cases = [c for c in cases if c.get('chunk_total', 1) > 1]
    if chunked_cases:
        logger.info(f"其中包含分块案例: {len(chunked_cases)} 个")
        total_chunks = sum(c.get('chunk_total', 1) for c in chunked_cases)
        logger.info(f"总块数: {total_chunks}")

    # 4. 插入数据库
    rag_service = RAGService()
    result = await rag_service.insert_batch_cases(cases)

    logger.info("=" * 60)
    logger.info(f"初始化完成！成功: {result['success']}, 失败: {result['fail']}")
    logger.info("=" * 60)

    # 5. 显示统计信息
    total_count = await rag_service.get_case_count()
    logger.info(f"知识库总案例数: {total_count}")


async def test_search():
    """测试检索"""
    logger.info("\n" + "=" * 60)
    logger.info("测试RAG检索功能")
    logger.info("=" * 60)

    rag_service = RAGService()

    test_input = {
        "task": "肺结节检测",
        "summary": "左肺上叶磨玻璃结节，大小约7mm，边缘模糊",
        "ratio": 1.0,
        "slice_indices": [11, 12, 13]
    }

    results = await rag_service.search_similar_cases(test_input, top_k=3)

    if results:
        logger.info(f"检索到 {len(results)} 个相似案例:")
        for i, result in enumerate(results, 1):
            case = result.case
            case_id = case.metadata.get('case_id', 'N/A')
            # 如果是分块案例，显示父案例ID
            if case.metadata.get('parent_case_id'):
                case_id = f"{case.metadata.get('parent_case_id')} (块 {case.metadata.get('chunk_index', 0) + 1}/{case.metadata.get('chunk_total', 1)})"

            logger.info(f"\n  {i}. 案例: {case_id}")
            logger.info(f"     相似度: {result.similarity:.2%}")
            logger.info(f"     内容: {case.content[:100]}...")
            logger.info(f"     风险等级: {case.metadata.get('ai_analysis', {}).get('riskLevel', 'N/A')}")
    else:
        logger.warning("未找到相似案例")


async def show_template():
    """显示文档模板"""
    template = """
========================================
RAG知识库文档模板
========================================

病例编号: CASE_20240001
任务类型: 肺结节检测
医院: XX医院

--- 影像特征 ---
影像摘要: 左肺上叶发现磨玻璃密度结节，最大径8mm，边缘模糊
结节类型: 磨玻璃
结节大小: 8.0
结节位置: 左肺上叶
边缘特征: 模糊
阳性像素数: 1250
总像素数: 100000
阳性占比: 1.25
涉及切片: 12, 13, 14, 15

--- AI分析结论 ---
AI诊断结论: 左肺上叶磨玻璃结节，大小约8mm，边缘模糊，考虑早期肺腺癌可能
风险等级: MEDIUM
处理建议: 
- 建议3个月后复查CT
- 必要时穿刺活检
- 胸外科会诊
随访建议: 建议3个月后复查

--- 医生确认 ---
最终诊断: 早期肺腺癌
治疗方案: 手术切除
病理结果: 肺腺癌
确诊医生: 张医生
随访月数: 12
治疗效果: 良好
是否复发: false
质量评分: 0.95

========================================
使用说明:
1. 在 data/ 目录下创建 .docx 或 .pdf 文件
2. 按照上述模板格式填写内容
3. 运行: python scripts/init_rag_from_docs.py
"""
    logger.info(template)


async def clear_data(confirm: bool = False):
    """清空知识库数据"""
    if not confirm:
        logger.warning("⚠️  清空操作需要确认，请使用 --confirm 参数")
        return

    logger.warning("⚠️  准备清空所有RAG数据...")
    rag_service = RAGService()
    success = await rag_service.clear_all_cases(confirm=True)

    if success:
        logger.info("✓ 所有数据已清空")
    else:
        logger.error("✗ 清空失败")


async def show_stats():
    """显示知识库统计"""
    rag_service = RAGService()
    count = await rag_service.get_case_count()

    logger.info("=" * 60)
    logger.info("RAG知识库统计")
    logger.info("=" * 60)
    logger.info(f"总案例数: {count}")

    if count > 0:
        # 获取前5个案例
        cases = await rag_service.get_all_cases(limit=5)
        logger.info("\n最近案例:")
        for case in cases:
            case_id = case.metadata.get('case_id', 'N/A')
            ai_summary = case.metadata.get('ai_analysis', {}).get('summary', 'N/A')[:50]
            logger.info(f"  - {case_id}: {ai_summary}...")

    logger.info("=" * 60)


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='从文档初始化RAG知识库')
    parser.add_argument('--dir', type=str, default=None,
                        help=f'文档目录路径（默认: {DEFAULT_DOCS_DIR}）')
    parser.add_argument('--no-chunk', action='store_true',
                        help='禁用文本分割')
    parser.add_argument('--test', action='store_true',
                        help='测试检索功能')
    parser.add_argument('--template', action='store_true',
                        help='显示文档模板')
    parser.add_argument('--stats', action='store_true',
                        help='显示知识库统计')
    parser.add_argument('--clear', action='store_true',
                        help='清空所有数据')
    parser.add_argument('--confirm', action='store_true',
                        help='确认操作（配合--clear使用）')

    args = parser.parse_args()

    if args.template:
        asyncio.run(show_template())
    elif args.stats:
        asyncio.run(show_stats())
    elif args.test:
        asyncio.run(test_search())
    elif args.clear:
        asyncio.run(clear_data(args.confirm))
    else:
        # 默认执行初始化
        asyncio.run(init_from_directory(
            args.dir,
            enable_chunking=not args.no_chunk
        ))
        # 初始化后显示统计
        asyncio.run(show_stats())
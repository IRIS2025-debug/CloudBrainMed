# app/utils/langchain_splitter.py
"""
LangChain文本分割器封装
用于将长文档切分成适合向量检索的文本块
"""
import logging
from typing import List, Dict, Any, Optional
from langchain_text_splitters import (
    RecursiveCharacterTextSplitter,
    CharacterTextSplitter,
    Language,
    RecursiveCharacterTextSplitter as LanguageSplitter
)

logger = logging.getLogger(__name__)


class LangChainTextSplitter:
    """LangChain文本分割器封装类"""

    def __init__(
            self,
            chunk_size: int = 500,
            chunk_overlap: int = 50,
            separators: Optional[List[str]] = None,
            use_chinese: bool = True
    ):
        """
        初始化分割器

        Args:
            chunk_size: 每个块的最大字符数
            chunk_overlap: 块之间的重叠字符数
            separators: 自定义分隔符列表
            use_chinese: 是否使用中文优化
        """
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

        # 中文优化的分隔符
        if use_chinese and separators is None:
            separators = [
                "\n\n",  # 段落分隔
                "\n",  # 换行
                "。",  # 中文句号
                "！",  # 中文感叹号
                "？",  # 中文问号
                "；",  # 中文分号
                "，",  # 中文逗号
                "、",  # 中文顿号
                ".",  # 英文句号
                "!",  # 英文感叹号
                "?",  # 英文问号
                ";",  # 英文分号
                ",",  # 英文逗号
                " ",  # 空格
                ""  # 保底分隔符
            ]
        elif separators is None:
            separators = None  # 使用默认分隔符

        # 初始化分割器
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=separators,
            length_function=len,
            is_separator_regex=False
        )

        logger.info(f"✓ LangChain分割器初始化: chunk_size={chunk_size}, chunk_overlap={chunk_overlap}")

    def split_text(self, text: str) -> List[str]:
        """
        分割文本

        Args:
            text: 要分割的文本

        Returns:
            文本块列表
        """
        if not text or len(text) <= self.chunk_size:
            return [text]

        chunks = self.splitter.split_text(text)
        logger.info(f"✓ 文本分割完成: {len(chunks)} 个块")
        return chunks

    def split_documents(self, documents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        分割文档列表

        Args:
            documents: 文档列表，每个文档包含 'content' 和 'metadata'

        Returns:
            分割后的文档列表（每个文档包含 'content' 和 'metadata'）
        """
        from langchain_core.documents import Document

        # 转换为LangChain Document
        lc_docs = []
        for doc in documents:
            lc_docs.append(
                Document(
                    page_content=doc.get('content', ''),
                    metadata=doc.get('metadata', {})
                )
            )

        # 分割
        split_docs = self.splitter.split_documents(lc_docs)

        # 转换回字典格式
        result = []
        for doc in split_docs:
            result.append({
                'content': doc.page_content,
                'metadata': doc.metadata
            })

        logger.info(f"✓ 文档分割完成: {len(result)} 个块")
        return result

    def split_text_with_metadata(
            self,
            text: str,
            base_metadata: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """
        分割文本并附带元数据

        Args:
            text: 要分割的文本
            base_metadata: 基础元数据

        Returns:
            分割后的文档列表，每个包含content和metadata
        """
        chunks = self.split_text(text)
        result = []

        for idx, chunk in enumerate(chunks):
            metadata = base_metadata.copy()
            metadata['chunk_index'] = idx
            metadata['chunk_count'] = len(chunks)
            metadata['chunk_length'] = len(chunk)

            result.append({
                'content': chunk,
                'metadata': metadata
            })

        return result


def get_default_splitter() -> LangChainTextSplitter:
    """获取默认配置的分割器"""
    return LangChainTextSplitter(
        chunk_size=500,
        chunk_overlap=50,
        use_chinese=True
    )


def get_splitter_for_ct_report() -> LangChainTextSplitter:
    """获取CT报告优化的分割器"""
    return LangChainTextSplitter(
        chunk_size=800,  # CT报告通常较长，使用更大的块
        chunk_overlap=100,  # 保持足够的上下文重叠
        use_chinese=True
    )
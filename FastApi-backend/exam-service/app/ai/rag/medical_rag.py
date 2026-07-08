# app/ai/rag/medical_rag.py

import psycopg2
from psycopg2.extras import Json
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_dashscope import DashScopeEmbeddings
from app.core.config import settings
import os
from dotenv import load_dotenv

load_dotenv()

# ========== Embedding 模型 ==========
embedding = DashScopeEmbeddings(
    model="text-embedding-v1",
    dashscope_api_key=os.getenv("DASHSCOPE_API_KEY")
)

# ========== 文本分割器 ==========
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=400,
    chunk_overlap=80,
    separators=["\n\n", "\n", "，", "。"]
)


class PgVectorStore:
    """PostgreSQL + pgvector 向量存储"""

    def __init__(self, conn_string: str):
        self.conn = psycopg2.connect(conn_string)

    def add_texts(self, texts: list, metadatas: list = None, table_name: str = "exam_report_vector_store"):
        """批量插入文本"""
        if metadatas is None:
            metadatas = [{}] * len(texts)

        with self.conn.cursor() as cur:
            for text, meta in zip(texts, metadatas):
                vector = embedding.embed_query(text)
                cur.execute(
                    f"""
                    INSERT INTO {table_name} (id, content, embedding, metadata)
                    VALUES (uuid_generate_v4(), %s, %s::vector, %s)
                    """,
                    (text, vector, Json(meta))
                )
        self.conn.commit()

    def similarity_search(self, query: str, table_name: str = "exam_report_vector_store",
                          top_k: int = 3, filter_condition: str = None) -> list:
        """向量相似度检索"""
        query_vector = embedding.embed_query(query)

        sql = f"""
            SELECT content, metadata, 1 - (embedding <=> %s::vector) AS similarity
            FROM {table_name}
        """
        if filter_condition:
            sql += f" WHERE {filter_condition}"
        sql += " ORDER BY embedding <=> %s::vector LIMIT %s"

        with self.conn.cursor() as cur:
            cur.execute(sql, (query_vector, query_vector, top_k))
            rows = cur.fetchall()

        return [
            {
                "page_content": row[0],
                "metadata": row[1],
                "similarity": row[2]
            }
            for row in rows
        ]


# ========== 全局实例 ==========
pg_vector_store = PgVectorStore(os.getenv("DATABASE_URL"))


def retrieve_similar_reports(
        query: str,
        top_k: int = 3,
        filter_condition: str = None,
        table_name: str = "exam_report_vector_store"
) -> str:
    """
    从向量表中检索相似的历史报告范文

    Returns:
        格式化的参考范文文本，用于注入Prompt
    """
    docs = pg_vector_store.similarity_search(
        query=query,
        table_name=table_name,
        top_k=top_k,
        filter_condition=filter_condition
    )

    if not docs:
        return "（未找到相似的历史报告范文）"

    ref_text = "=====历史报告范文参考=====\n"
    for idx, doc in enumerate(docs):
        ref_text += f"{idx + 1}. {doc['page_content']}\n"
        # 从metadata中提取诊断意见
        diagnosis = doc['metadata'].get('diagnosis', '')
        if diagnosis:
            ref_text += f"   诊断：{diagnosis}\n"
        ref_text += f"   相似度：{doc['similarity']:.3f}\n\n"

    return ref_text


def load_medical_standard_to_pg(doc_content: str, doc_id: str, table_name: str = "exam_report_vector_store"):
    """加载文档入库（一次性执行）"""
    chunks = text_splitter.split_text(doc_content)

    metadatas = [
        {
            "doc_id": doc_id,
            "chunk_index": i,
            "source": "clinical_standard",
            "chunk_text": chunk
        }
        for i, chunk in enumerate(chunks)
    ]

    pg_vector_store.add_texts(
        texts=chunks,
        metadatas=metadatas,
        table_name=table_name
    )
    print(f"✅ 已入库 {len(chunks)} 条数据")
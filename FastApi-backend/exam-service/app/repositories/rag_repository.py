# app/repositories/rag_repository.py
import json
import logging
from typing import List, Optional, Dict, Any
from datetime import datetime
import asyncpg
from pgvector.asyncpg import register_vector
from langchain_openai import OpenAIEmbeddings
from app.config.settings import settings

logger = logging.getLogger(__name__)


class RAGRepository:
    """RAG知识库数据操作 - 使用pgvector"""

    def __init__(self):
        self.pool = None
        self.embedding_model = None
        logger.info("✓ RAGRepository初始化完成")

    def _get_embedding_model(self):
        """获取嵌入模型（延迟初始化）"""
        if self.embedding_model is None:
            self.embedding_model = OpenAIEmbeddings(
                model=settings.EMBEDDING_MODEL,
                openai_api_key=settings.DASHSCOPE_API_KEY,
                openai_api_base=settings.DASHSCOPE_BASE_URL
            )
            logger.info(f"✓ 嵌入模型初始化: {settings.EMBEDDING_MODEL}")
        return self.embedding_model

    async def get_pool(self):
        """获取连接池"""
        if self.pool is None:
            try:
                self.pool = await asyncpg.create_pool(
                    dsn=settings.DATABASE_URL,
                    min_size=1,
                    max_size=10,
                    command_timeout=60
                )
                # 注册vector类型
                async with self.pool.acquire() as conn:
                    await register_vector(conn)
                    # 验证pgvector是否可用
                    result = await conn.fetchval("SELECT 1 FROM pg_extension WHERE extname = 'vector'")
                    if not result:
                        logger.warning("pgvector扩展未安装，请执行: CREATE EXTENSION IF NOT EXISTS vector;")
                logger.info("✓ 数据库连接池初始化完成")
            except Exception as e:
                logger.error(f"数据库连接池初始化失败: {str(e)}")
                raise
        return self.pool

    def _build_search_text(self, model_features: Dict[str, Any]) -> str:
        """构建检索文本"""
        summary = model_features.get('summary', '')
        ratio = model_features.get('ratio', 0)
        slice_count = len(model_features.get('slice_indices', []))
        task = model_features.get('task', '')

        # 提取关键特征
        features = []
        if '磨玻璃' in summary:
            features.append('磨玻璃结节')
        if '实性' in summary:
            features.append('实性结节')
        if '毛刺' in summary:
            features.append('毛刺征')
        if '分叶' in summary:
            features.append('分叶征')
        if '胸膜牵拉' in summary:
            features.append('胸膜牵拉')

        # 提取大小信息
        import re
        size_match = re.search(r'(\d+\.?\d*)mm', summary)
        if size_match:
            features.append(f"{size_match.group(1)}mm")

        search_parts = [
            f"任务:{task}",
            f"摘要:{summary}",
            f"阳性占比:{ratio:.2f}%",
            f"涉及切片数:{slice_count}"
        ]
        if features:
            search_parts.append(f"特征:{','.join(features)}")

        return ' '.join(search_parts)

    async def _generate_embedding(self, text: str) -> List[float]:
        """生成文本向量"""
        try:
            embedding_model = self._get_embedding_model()

            # 调用嵌入模型生成向量
            embedding = await embedding_model.aembed_query(text)

            # 确保向量维度为1024（匹配你的表结构）
            if len(embedding) > 1024:
                embedding = embedding[:1024]
            elif len(embedding) < 1024:
                # 填充到1024维
                embedding = embedding + [0.0] * (1024 - len(embedding))

            logger.debug(f"生成embedding成功，维度: {len(embedding)}")
            return embedding

        except Exception as e:
            logger.error(f"生成embedding失败: {str(e)}")
            # 返回零向量作为降级方案
            return [0.0] * 1024

    async def insert_case(self, case_data: Dict[str, Any]) -> bool:
        """插入案例到数据库（自动生成embedding）"""
        pool = await self.get_pool()

        try:
            # 1. 构建content（检索文本）
            content = case_data.get('search_text', '')
            if not content:
                model_features = case_data.get('model_features', {})
                content = self._build_search_text(model_features)

            # 2. 构建metadata
            metadata = {
                'case_id': case_data.get('case_id'),
                'model_features': case_data.get('model_features', {}),
                'ai_analysis': case_data.get('ai_analysis', {}),
                'doctor_confirmed': case_data.get('doctor_confirmed', {}),
                'validation': case_data.get('validation', {}),
                'model_version': case_data.get('model_version', ''),
                'hospital': case_data.get('hospital', ''),
                'is_active': case_data.get('is_active', True),
                'created_at': datetime.now().isoformat()
            }

            # 3. 生成embedding
            embedding = await self._generate_embedding(content)

            # 4. 插入数据
            async with pool.acquire() as conn:
                await conn.execute(
                    """
                    INSERT INTO exam_report_vector_store
                        (content, embedding, metadata)
                    VALUES ($1, $2, $3)
                    """,
                    content,
                    embedding,
                    json.dumps(metadata)
                )

            logger.info(f"✓ 插入案例成功: {case_data.get('case_id')}")
            return True

        except Exception as e:
            logger.error(f"插入案例失败: {str(e)}")
            return False

    async def insert_batch_cases(self, cases: List[Dict[str, Any]]) -> Dict[str, int]:
        """批量插入案例"""
        pool = await self.get_pool()
        success_count = 0
        fail_count = 0

        try:
            async with pool.acquire() as conn:
                async with conn.transaction():
                    for case_data in cases:
                        try:
                            # 构建content
                            content = case_data.get('search_text', '')
                            if not content:
                                model_features = case_data.get('model_features', {})
                                content = self._build_search_text(model_features)

                            # 构建metadata
                            metadata = {
                                'case_id': case_data.get('case_id'),
                                'model_features': case_data.get('model_features', {}),
                                'ai_analysis': case_data.get('ai_analysis', {}),
                                'doctor_confirmed': case_data.get('doctor_confirmed', {}),
                                'validation': case_data.get('validation', {}),
                                'model_version': case_data.get('model_version', ''),
                                'hospital': case_data.get('hospital', ''),
                                'is_active': case_data.get('is_active', True),
                                'created_at': datetime.now().isoformat()
                            }

                            # 生成embedding
                            embedding = await self._generate_embedding(content)

                            # 插入数据
                            await conn.execute(
                                """
                                INSERT INTO exam_report_vector_store
                                    (content, embedding, metadata)
                                VALUES ($1, $2, $3)
                                """,
                                content,
                                embedding,
                                json.dumps(metadata)
                            )

                            success_count += 1
                            logger.info(f"✓ 批量插入成功: {case_data.get('case_id')}")

                        except Exception as e:
                            fail_count += 1
                            logger.error(f"批量插入失败 {case_data.get('case_id')}: {str(e)}")

            logger.info(f"批量插入完成 - 成功: {success_count}, 失败: {fail_count}")
            return {"success": success_count, "fail": fail_count}

        except Exception as e:
            logger.error(f"批量插入事务失败: {str(e)}")
            return {"success": success_count, "fail": fail_count}

    async def search_similar(
            self,
            query_text: str,
            limit: int = 5,
            similarity_threshold: float = 0.6,
            filter_conditions: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """搜索相似案例（使用向量相似度）"""
        pool = await self.get_pool()

        try:
            # 生成查询向量
            query_embedding = await self._generate_embedding(query_text)

            # 构建SQL查询
            sql = """
                  SELECT id, \
                         content, \
                         metadata, \
                         1 - (embedding <=> $1) AS similarity
                  FROM exam_report_vector_store
                  WHERE is_active = true \
                  """

            params = [query_embedding]
            param_index = 2

            # 添加相似度阈值过滤
            sql += f" AND 1 - (embedding <=> $1) >= ${param_index}"
            params.append(similarity_threshold)
            param_index += 1

            # 添加过滤条件（查询metadata中的字段）
            if filter_conditions:
                for key, value in filter_conditions.items():
                    # 使用jsonb查询
                    sql += f" AND metadata->>'{key}' = ${param_index}"
                    params.append(value)
                    param_index += 1

            # 排序和限制
            sql += f" ORDER BY embedding <=> $1 LIMIT ${param_index}"
            params.append(limit)

            # 执行查询
            async with pool.acquire() as conn:
                results = await conn.fetch(sql, *params)

            # 格式化结果
            formatted_results = []
            for record in results:
                formatted_results.append({
                    'id': str(record['id']),
                    'content': record['content'],
                    'metadata': record['metadata'],
                    'similarity': float(record['similarity'])
                })

            logger.info(f"✓ 搜索完成，找到 {len(formatted_results)} 个结果")
            return formatted_results

        except Exception as e:
            logger.error(f"搜索失败: {str(e)}")
            return []

    async def search_by_text(
            self,
            query_text: str,
            limit: int = 10
    ) -> List[Dict[str, Any]]:
        """基于文本关键词搜索（备用检索方式）"""
        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                results = await conn.fetch(
                    """
                    SELECT id,
                           content,
                           metadata,
                           ts_rank(to_tsvector('chinese', content), plainto_tsquery('chinese', $1)) AS rank
                    FROM exam_report_vector_store
                    WHERE to_tsvector('chinese', content) @@ plainto_tsquery('chinese'
                        , $1)
                    ORDER BY rank DESC
                        LIMIT $2
                    """,
                    query_text,
                    limit
                )

            formatted_results = []
            for record in results:
                formatted_results.append({
                    'id': str(record['id']),
                    'content': record['content'],
                    'metadata': record['metadata'],
                    'rank': float(record['rank']) if record['rank'] else 0
                })

            logger.info(f"✓ 文本搜索完成，找到 {len(formatted_results)} 个结果")
            return formatted_results

        except Exception as e:
            logger.error(f"文本搜索失败: {str(e)}")
            return []

    async def get_case_by_case_id(self, case_id: str) -> Optional[Dict[str, Any]]:
        """根据业务ID获取案例"""
        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                record = await conn.fetchrow(
                    """
                    SELECT id, content, metadata
                    FROM exam_report_vector_store
                    WHERE metadata ->>'case_id' = $1
                        LIMIT 1
                    """,
                    case_id
                )

            if record:
                return {
                    'id': str(record['id']),
                    'content': record['content'],
                    'metadata': record['metadata']
                }
            return None

        except Exception as e:
            logger.error(f"获取案例失败: {str(e)}")
            return None

    async def get_cases_by_ids(self, case_ids: List[str]) -> List[Dict[str, Any]]:
        """批量获取案例"""
        if not case_ids:
            return []

        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                records = await conn.fetch(
                    """
                    SELECT id, content, metadata
                    FROM exam_report_vector_store
                    WHERE metadata ->>'case_id' = ANY ($1)
                    """,
                    case_ids
                )

            return [
                {
                    'id': str(record['id']),
                    'content': record['content'],
                    'metadata': record['metadata']
                }
                for record in records
            ]

        except Exception as e:
            logger.error(f"批量获取案例失败: {str(e)}")
            return []

    async def get_cases_by_filter(
            self,
            filter_dict: Dict[str, Any],
            limit: int = 100
    ) -> List[Dict[str, Any]]:
        """根据metadata过滤条件获取案例"""
        pool = await self.get_pool()

        try:
            # 构建过滤条件
            where_clauses = []
            params = []
            param_index = 1

            for key, value in filter_dict.items():
                where_clauses.append(f"metadata->>'{key}' = ${param_index}")
                params.append(value)
                param_index += 1

            where_sql = " AND ".join(where_clauses) if where_clauses else "1=1"
            params.append(limit)

            sql = f"""
                SELECT id, content, metadata
                FROM exam_report_vector_store
                WHERE {where_sql}
                LIMIT ${param_index}
            """

            async with pool.acquire() as conn:
                records = await conn.fetch(sql, *params)

            return [
                {
                    'id': str(record['id']),
                    'content': record['content'],
                    'metadata': record['metadata']
                }
                for record in records
            ]

        except Exception as e:
            logger.error(f"过滤查询失败: {str(e)}")
            return []

    async def get_high_quality_cases(
            self,
            min_quality: float = 0.7,
            limit: int = 10
    ) -> List[Dict[str, Any]]:
        """获取高质量案例"""
        return await self.get_cases_by_filter(
            filter_dict={'quality_score': str(min_quality)},
            limit=limit
        )

    async def update_case(self, case_id: str, update_data: Dict[str, Any]) -> bool:
        """更新案例"""
        pool = await self.get_pool()

        try:
            # 获取现有数据
            existing = await self.get_case_by_case_id(case_id)
            if not existing:
                logger.warning(f"案例不存在: {case_id}")
                return False

            # 更新metadata
            metadata = existing['metadata']

            # 递归更新metadata
            for key, value in update_data.items():
                if key in ['model_features', 'ai_analysis', 'doctor_confirmed', 'validation']:
                    metadata[key] = value
                else:
                    metadata[key] = value

            # 更新content（如果检索文本变化）
            content = existing['content']
            if 'search_text' in update_data:
                content = update_data['search_text']
            elif 'model_features' in update_data:
                content = self._build_search_text(update_data['model_features'])

            # 重新生成embedding
            embedding = await self._generate_embedding(content)

            # 更新数据库
            async with pool.acquire() as conn:
                await conn.execute(
                    """
                    UPDATE exam_report_vector_store
                    SET content   = $1,
                        embedding = $2,
                        metadata  = $3
                    WHERE metadata ->>'case_id' = $4
                    """,
                    content,
                    embedding,
                    json.dumps(metadata),
                    case_id
                )

            logger.info(f"✓ 更新案例成功: {case_id}")
            return True

        except Exception as e:
            logger.error(f"更新案例失败: {str(e)}")
            return False

    async def delete_case(self, case_id: str, soft_delete: bool = True) -> bool:
        """删除案例（支持软删除）"""
        pool = await self.get_pool()

        try:
            if soft_delete:
                # 软删除：标记is_active为false
                async with pool.acquire() as conn:
                    await conn.execute(
                        """
                        UPDATE exam_report_vector_store
                        SET metadata = jsonb_set(metadata, '{is_active}', 'false')
                        WHERE metadata ->>'case_id' = $1
                        """,
                        case_id
                    )
            else:
                # 硬删除：物理删除记录
                async with pool.acquire() as conn:
                    await conn.execute(
                        """
                        DELETE
                        FROM exam_report_vector_store
                        WHERE metadata ->>'case_id' = $1
                        """,
                        case_id
                    )

            logger.info(f"✓ 删除案例成功: {case_id} (软删除: {soft_delete})")
            return True

        except Exception as e:
            logger.error(f"删除案例失败: {str(e)}")
            return False

    async def get_all_cases(self, limit: int = 100, offset: int = 0) -> List[Dict[str, Any]]:
        """获取所有案例（分页）"""
        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                records = await conn.fetch(
                    """
                    SELECT id, content, metadata
                    FROM exam_report_vector_store
                    WHERE metadata ->>'is_active' = 'true'
                    ORDER BY metadata->>'created_at' DESC
                        LIMIT $1
                    OFFSET $2
                    """,
                    limit,
                    offset
                )

            return [
                {
                    'id': str(record['id']),
                    'content': record['content'],
                    'metadata': record['metadata']
                }
                for record in records
            ]

        except Exception as e:
            logger.error(f"获取所有案例失败: {str(e)}")
            return []

    async def get_case_count(self) -> int:
        """获取案例总数"""
        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                count = await conn.fetchval(
                    """
                    SELECT COUNT(*)
                    FROM exam_report_vector_store
                    WHERE metadata ->>'is_active' = 'true'
                    """
                )
            return count

        except Exception as e:
            logger.error(f"获取案例数量失败: {str(e)}")
            return 0

    async def clear_all_cases(self, confirm: bool = False) -> bool:
        """清空所有数据（谨慎使用）"""
        if not confirm:
            logger.warning("清空操作需要确认")
            return False

        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                await conn.execute("TRUNCATE TABLE exam_report_vector_store")

            logger.warning("⚠️ 所有数据已清空")
            return True

        except Exception as e:
            logger.error(f"清空数据失败: {str(e)}")
            return False

    async def optimize_index(self) -> bool:
        """优化向量索引"""
        pool = await self.get_pool()

        try:
            async with pool.acquire() as conn:
                # 重建索引
                await conn.execute("REINDEX INDEX idx_embedding")

            logger.info("✓ 向量索引优化完成")
            return True

        except Exception as e:
            logger.error(f"索引优化失败: {str(e)}")
            return False


# 全局单例
_rag_repository = None


async def get_rag_repository() -> RAGRepository:
    """获取RAG仓库单例"""
    global _rag_repository
    if _rag_repository is None:
        _rag_repository = RAGRepository()
        await _rag_repository.get_pool()
    return _rag_repository
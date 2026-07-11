# app/services/rag_service.py
"""
RAG检索增强服务
负责知识库的增删改查、向量检索等功能
"""
import json
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime

from app.models.rag import RAGCase, RAGSearchResult
from app.repositories.rag_repository import get_rag_repository, RAGRepository
from app.config.settings import settings

logger = logging.getLogger(__name__)


class RAGService:
    """RAG检索增强服务"""

    def __init__(self):
        self.repository = None
        self.rag_enabled = getattr(settings, 'RAG_ENABLED', True)
        self.top_k = getattr(settings, 'RAG_TOP_K', 3)
        self.similarity_threshold = getattr(settings, 'RAG_SIMILARITY_THRESHOLD', 0.6)
        logger.info("✓ RAGService初始化完成")
        logger.info(f"  RAG启用: {self.rag_enabled}")
        logger.info(f"  默认检索数量: {self.top_k}")
        logger.info(f"  相似度阈值: {self.similarity_threshold}")

    async def _get_repository(self) -> RAGRepository:
        """获取仓库实例（延迟初始化）"""
        if self.repository is None:
            self.repository = await get_rag_repository()
        return self.repository

    def _build_search_text(self, model_output: Dict[str, Any]) -> str:
        """
        构建检索文本

        Args:
            model_output: 模型输出数据

        Returns:
            检索文本
        """
        summary = model_output.get('summary', '')
        ratio = model_output.get('ratio', 0)
        slice_count = len(model_output.get('slice_indices', []))
        task = model_output.get('task', '')

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
        if '空洞' in summary:
            features.append('空洞')
        if '钙化' in summary:
            features.append('钙化')

        # 提取大小信息
        import re
        size_match = re.search(r'(\d+\.?\d*)mm', summary)
        if size_match:
            features.append(f"{size_match.group(1)}mm")

        # 提取位置信息
        if '左肺' in summary:
            features.append('左肺')
        if '右肺' in summary:
            features.append('右肺')
        if '上叶' in summary:
            features.append('上叶')
        if '下叶' in summary:
            features.append('下叶')

        search_parts = [
            f"任务:{task}",
            f"摘要:{summary}",
            f"阳性占比:{ratio:.2f}%",
            f"涉及切片数:{slice_count}"
        ]
        if features:
            search_parts.append(f"特征:{','.join(features)}")

        search_text = ' '.join(search_parts)
        logger.debug(f"构建检索文本: {search_text[:100]}...")
        return search_text

    def _extract_filter_conditions(self, model_output: Dict[str, Any]) -> Optional[Dict[str, str]]:
        """
        提取过滤条件

        Args:
            model_output: 模型输出数据

        Returns:
            过滤条件字典
        """
        conditions = {}
        summary = model_output.get('summary', '')

        # 结节类型
        if '磨玻璃' in summary:
            conditions['nodule_type'] = '磨玻璃'
        elif '实性' in summary:
            conditions['nodule_type'] = '实性'
        elif '混合' in summary:
            conditions['nodule_type'] = '混合'

        # 位置
        if '左肺' in summary and '上叶' in summary:
            conditions['location'] = '左肺上叶'
        elif '左肺' in summary and '下叶' in summary:
            conditions['location'] = '左肺下叶'
        elif '右肺' in summary and '上叶' in summary:
            conditions['location'] = '右肺上叶'
        elif '右肺' in summary and '下叶' in summary:
            conditions['location'] = '右肺下叶'
        elif '左肺' in summary:
            conditions['location'] = '左肺'
        elif '右肺' in summary:
            conditions['location'] = '右肺'

        # 风险等级（从summary中提取）
        if '高危' in summary or '高度可疑' in summary:
            conditions['risk_level'] = 'HIGH'
        elif '低危' in summary or '良性' in summary:
            conditions['risk_level'] = 'LOW'

        return conditions if conditions else None

    async def insert_case(self, case_data: Dict[str, Any]) -> bool:
        """
        插入案例到知识库

        Args:
            case_data: 案例数据

        Returns:
            是否插入成功
        """
        if not self.rag_enabled:
            logger.warning("RAG功能已禁用，跳过插入")
            return False

        try:
            repository = await self._get_repository()

            # 确保有search_text
            if not case_data.get('search_text'):
                model_features = case_data.get('model_features', {})
                case_data['search_text'] = self._build_search_text(model_features)

            # 如果case_id不存在，自动生成
            if not case_data.get('case_id'):
                case_data['case_id'] = f"CASE_{datetime.now().strftime('%Y%m%d%H%M%S')}"
                logger.info(f"自动生成案例ID: {case_data['case_id']}")

            # 插入数据库
            success = await repository.insert_case(case_data)

            if success:
                logger.info(f"✓ 插入RAG案例成功: {case_data.get('case_id')}")
            else:
                logger.error(f"✗ 插入RAG案例失败: {case_data.get('case_id')}")

            return success

        except Exception as e:
            logger.error(f"插入RAG案例异常: {str(e)}")
            return False

    async def insert_batch_cases(self, cases: List[Dict[str, Any]]) -> Dict[str, int]:
        """
        批量插入案例

        Args:
            cases: 案例列表

        Returns:
            {'success': 成功数, 'fail': 失败数}
        """
        if not self.rag_enabled:
            logger.warning("RAG功能已禁用，跳过批量插入")
            return {"success": 0, "fail": len(cases)}

        if not cases:
            logger.warning("没有案例需要插入")
            return {"success": 0, "fail": 0}

        try:
            repository = await self._get_repository()

            # 处理每个案例
            processed_cases = []
            for case in cases:
                # 确保有search_text
                if not case.get('search_text'):
                    model_features = case.get('model_features', {})
                    case['search_text'] = self._build_search_text(model_features)

                # 如果case_id不存在，自动生成
                if not case.get('case_id'):
                    case['case_id'] = f"CASE_{datetime.now().strftime('%Y%m%d%H%M%S')}_{len(processed_cases) + 1}"

                processed_cases.append(case)

            # 批量插入
            result = await repository.insert_batch_cases(processed_cases)

            logger.info(f"批量插入完成: 成功 {result['success']}, 失败 {result['fail']}")
            return result

        except Exception as e:
            logger.error(f"批量插入异常: {str(e)}")
            return {"success": 0, "fail": len(cases)}

    async def search_similar_cases(
            self,
            model_output: Dict[str, Any],
            top_k: Optional[int] = None,
            similarity_threshold: Optional[float] = None
    ) -> List[RAGSearchResult]:
        """
        检索相似案例

        Args:
            model_output: 模型输出数据
            top_k: 返回结果数量（默认使用配置值）
            similarity_threshold: 相似度阈值（默认使用配置值）

        Returns:
            检索结果列表
        """
        if not self.rag_enabled:
            logger.warning("RAG功能已禁用，返回空结果")
            return []

        try:
            repository = await self._get_repository()

            # 使用配置或传入参数
            k = top_k if top_k is not None else self.top_k
            threshold = similarity_threshold if similarity_threshold is not None else self.similarity_threshold

            # 1. 构建查询文本
            search_text = self._build_search_text(model_output)
            logger.info(f"检索文本: {search_text[:100]}...")

            # 2. 获取过滤条件
            filter_conditions = self._extract_filter_conditions(model_output)
            if filter_conditions:
                logger.info(f"应用过滤条件: {filter_conditions}")

            # 3. 执行向量检索
            results = await repository.search_similar(
                query_text=search_text,
                limit=k,
                similarity_threshold=threshold,
                filter_conditions=filter_conditions
            )

            if not results:
                logger.info("未找到相似案例")
                return []

            # 4. 转换为RAGSearchResult
            search_results = []
            for result in results:
                # 创建RAGCase对象
                case = RAGCase(
                    id=result['id'],
                    content=result['content'],
                    metadata=result['metadata'],
                    embedding=None  # 不返回embedding节省带宽
                )
                search_results.append(RAGSearchResult(
                    case=case,
                    similarity=result['similarity']
                ))

            logger.info(f"✓ 检索到 {len(search_results)} 个相似案例")

            # 打印检索结果概要
            for idx, result in enumerate(search_results[:3], 1):
                case_id = result.case.metadata.get('case_id', 'N/A')
                logger.info(f"  {idx}. {case_id} (相似度: {result.similarity:.2%})")

            return search_results

        except Exception as e:
            logger.error(f"检索失败: {str(e)}")
            return []

    async def search_by_text(
            self,
            query_text: str,
            limit: Optional[int] = None
    ) -> List[RAGSearchResult]:
        """
        基于文本关键词搜索（备用检索方式）

        Args:
            query_text: 查询文本
            limit: 返回结果数量

        Returns:
            检索结果列表
        """
        if not self.rag_enabled:
            logger.warning("RAG功能已禁用，返回空结果")
            return []

        try:
            repository = await self._get_repository()
            k = limit if limit is not None else self.top_k

            results = await repository.search_by_text(query_text, limit=k)

            if not results:
                logger.info("未找到匹配案例")
                return []

            # 转换为RAGSearchResult
            search_results = []
            for result in results:
                case = RAGCase(
                    id=result['id'],
                    content=result['content'],
                    metadata=result['metadata'],
                    embedding=None
                )
                search_results.append(RAGSearchResult(
                    case=case,
                    similarity=result.get('rank', 0)  # 文本搜索使用rank作为相似度
                ))

            logger.info(f"✓ 文本搜索到 {len(search_results)} 个案例")
            return search_results

        except Exception as e:
            logger.error(f"文本搜索失败: {str(e)}")
            return []

    async def get_case(self, case_id: str) -> Optional[RAGCase]:
        """
        获取单个案例

        Args:
            case_id: 案例ID

        Returns:
            案例对象
        """
        try:
            repository = await self._get_repository()
            result = await repository.get_case_by_case_id(case_id)

            if result:
                return RAGCase(
                    id=result['id'],
                    content=result['content'],
                    metadata=result['metadata']
                )
            return None

        except Exception as e:
            logger.error(f"获取案例失败: {str(e)}")
            return None

    async def get_cases_by_filter(
            self,
            filter_dict: Dict[str, Any],
            limit: int = 100
    ) -> List[RAGCase]:
        """
        根据过滤条件获取案例

        Args:
            filter_dict: 过滤条件
            limit: 返回数量限制

        Returns:
            案例列表
        """
        try:
            repository = await self._get_repository()
            results = await repository.get_cases_by_filter(filter_dict, limit)

            return [
                RAGCase(
                    id=result['id'],
                    content=result['content'],
                    metadata=result['metadata']
                )
                for result in results
            ]

        except Exception as e:
            logger.error(f"过滤查询失败: {str(e)}")
            return []

    async def get_high_quality_cases(self, min_quality: float = 0.7, limit: int = 10) -> List[RAGCase]:
        """
        获取高质量案例

        Args:
            min_quality: 最低质量评分
            limit: 返回数量限制

        Returns:
            案例列表
        """
        try:
            repository = await self._get_repository()
            results = await repository.get_high_quality_cases(min_quality, limit)

            return [
                RAGCase(
                    id=result['id'],
                    content=result['content'],
                    metadata=result['metadata']
                )
                for result in results
            ]

        except Exception as e:
            logger.error(f"获取高质量案例失败: {str(e)}")
            return []

    async def update_case(self, case_id: str, update_data: Dict[str, Any]) -> bool:
        """
        更新案例

        Args:
            case_id: 案例ID
            update_data: 更新数据

        Returns:
            是否更新成功
        """
        try:
            repository = await self._get_repository()

            # 如果更新了model_features，重新生成search_text
            if 'model_features' in update_data:
                update_data['search_text'] = self._build_search_text(update_data['model_features'])

            success = await repository.update_case(case_id, update_data)

            if success:
                logger.info(f"✓ 更新案例成功: {case_id}")
            else:
                logger.error(f"✗ 更新案例失败: {case_id}")

            return success

        except Exception as e:
            logger.error(f"更新案例异常: {str(e)}")
            return False

    async def delete_case(self, case_id: str, soft_delete: bool = True) -> bool:
        """
        删除案例

        Args:
            case_id: 案例ID
            soft_delete: 是否软删除（默认True）

        Returns:
            是否删除成功
        """
        try:
            repository = await self._get_repository()
            success = await repository.delete_case(case_id, soft_delete)

            if success:
                logger.info(f"✓ 删除案例成功: {case_id} (软删除: {soft_delete})")
            else:
                logger.error(f"✗ 删除案例失败: {case_id}")

            return success

        except Exception as e:
            logger.error(f"删除案例异常: {str(e)}")
            return False

    async def get_case_count(self) -> int:
        """
        获取案例总数

        Returns:
            案例总数
        """
        try:
            repository = await self._get_repository()
            return await repository.get_case_count()
        except Exception as e:
            logger.error(f"获取案例数量失败: {str(e)}")
            return 0

    async def get_all_cases(self, limit: int = 100, offset: int = 0) -> List[RAGCase]:
        """
        获取所有案例（分页）

        Args:
            limit: 每页数量
            offset: 偏移量

        Returns:
            案例列表
        """
        try:
            repository = await self._get_repository()
            results = await repository.get_all_cases(limit, offset)

            return [
                RAGCase(
                    id=result['id'],
                    content=result['content'],
                    metadata=result['metadata']
                )
                for result in results
            ]

        except Exception as e:
            logger.error(f"获取所有案例失败: {str(e)}")
            return []

    async def clear_all_cases(self, confirm: bool = False) -> bool:
        """
        清空所有数据（谨慎使用）

        Args:
            confirm: 确认标志

        Returns:
            是否清空成功
        """
        try:
            repository = await self._get_repository()
            return await repository.clear_all_cases(confirm)
        except Exception as e:
            logger.error(f"清空数据失败: {str(e)}")
            return False

    async def optimize_index(self) -> bool:
        """
        优化向量索引

        Returns:
            是否优化成功
        """
        try:
            repository = await self._get_repository()
            return await repository.optimize_index()
        except Exception as e:
            logger.error(f"索引优化失败: {str(e)}")
            return False

    def is_enabled(self) -> bool:
        """检查RAG是否启用"""
        return self.rag_enabled

    def get_stats(self) -> Dict[str, Any]:
        """
        获取服务统计信息

        Returns:
            统计信息字典
        """
        return {
            "enabled": self.rag_enabled,
            "top_k": self.top_k,
            "similarity_threshold": self.similarity_threshold,
            "repository_initialized": self.repository is not None
        }


# 全局单例
_rag_service = None


async def get_rag_service() -> RAGService:
    """
    获取RAG服务单例

    Returns:
        RAGService实例
    """
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service


# 便捷函数
async def search_similar(model_output: Dict[str, Any], top_k: int = 3) -> List[RAGSearchResult]:
    """
    快捷搜索函数

    Args:
        model_output: 模型输出
        top_k: 返回数量

    Returns:
        检索结果列表
    """
    service = await get_rag_service()
    return await service.search_similar_cases(model_output, top_k)


async def insert_case(case_data: Dict[str, Any]) -> bool:
    """
    快捷插入函数

    Args:
        case_data: 案例数据

    Returns:
        是否插入成功
    """
    service = await get_rag_service()
    return await service.insert_case(case_data)
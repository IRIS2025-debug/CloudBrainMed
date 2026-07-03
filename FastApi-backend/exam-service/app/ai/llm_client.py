import os
from typing import Optional
from dotenv import load_dotenv
from langchain_core.language_models.base import BaseLanguageModel
from langchain_community.llms import Tongyi

load_dotenv()


class AliBaiLianLLM:
    _instance: Optional[BaseLanguageModel] = None

    @classmethod
    def get_llm(cls) -> BaseLanguageModel:
        """单例获取同步百炼LLM实例"""
        if cls._instance is None:
            api_key = os.getenv("DASHSCOPE_API_KEY")
            model_name = os.getenv("DASHSCOPE_MODEL_NAME", "qwen-plus")  # 提供默认值

            if not api_key:
                raise ValueError("请在.env中配置 DASHSCOPE_API_KEY")

            cls._instance = Tongyi(
                dashscope_api_key=api_key,
                model_name=model_name,
                temperature=0.1,
                max_tokens=1500
            )
        return cls._instance
import json
from typing import Dict, Optional
from langchain_core.output_parsers import JsonOutputParser
from app.ai.llm_client import AliBaiLianLLM
from app.ai.prompts.report_prompt import report_prompt
from app.ai.schemas import ExamReportInput, ExamReportOutput
# 导入RAG检索函数（从向量表检索历史报告范文）
from app.ai.rag.medical_rag import retrieve_similar_reports  # 改名，更清晰

# 初始化LLM、解析器、链
llm = AliBaiLianLLM.get_llm()

# 使用普通的 JsonOutputParser，不传入 pydantic_object
json_parser = JsonOutputParser()

report_generate_chain = report_prompt | llm | json_parser


def generate_exam_report(input_data: ExamReportInput) -> ExamReportOutput:
    """
    同步生成CT/MRI检查报告（集成RAG历史报告范文检索）

    :param input_data: 患者信息 + 影像所见草稿
    :return: 结构化AI医疗报告
    """

    # ========== 1. RAG检索：从向量表中检索相似的历史报告范文 ==========
    # 用影像所见草稿作为查询文本
    rag_query = input_data.image_findings  # 直接用影像所见作为查询

    # 可选：添加过滤条件，比如只看同类检查、同部位的报告
    filter_condition = None
    if input_data.exam_type:
        # 只检索同类型检查的报告
        filter_condition = f"metadata->>'exam_type' = '{input_data.exam_type}'"

    similar_reports = retrieve_similar_reports(
        query=rag_query,
        top_k=3,
        filter_condition=filter_condition,
        table_name="exam_report_vector_store"  # 指定用检查报告表
    )

    # ========== 2. 组装Prompt入参 ==========
    chain_input: Dict = {
        "patient_name": input_data.patient_name,
        "patient_age": input_data.patient_age,
        "patient_gender": input_data.patient_gender,
        "exam_type": input_data.exam_type,
        "image_findings": input_data.image_findings,  # 影像所见草稿
        "doctor_hint": input_data.doctor_hint or "",  # 医生补充提示
        "similar_reports": similar_reports,  # RAG检索到的范文
    }

    # ========== 3. 同步调用大模型生成报告 ==========
    raw_result = report_generate_chain.invoke(chain_input)

    # ========== 4. 校验并返回结构化结果 ==========
    return ExamReportOutput.model_validate(raw_result)

<template>
  <div class="agent-dashboard-container">
    <!-- 顶部标题栏美化 -->
    <div class="page-header">
      <div class="header-left">
        <el-icon size="28" color="#409EFF"><DataAnalysis /></el-icon>
        <h2>AI智慧云脑 - Agent智能体数据统计控制台</h2>
      </div>
      <el-button type="primary" icon="Refresh" @click="refreshAllData">刷新页面</el-button>
    </div>

    <!-- 时间筛选栏 -->
    <div class="time-select-wrap">
      <el-date-picker
        v-model="startTime"
        type="date"
        placeholder="统计开始日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
      ></el-date-picker>
      <span class="split-line">~</span>
      <el-date-picker
        v-model="endTime"
        type="date"
        placeholder="统计结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
      ></el-date-picker>
      <el-text type="info">不选择日期，后端默认统计最近30天全院运营数据</el-text>
    </div>

    <div class="main-layout">
      <!-- 左侧：数据可视化区域 -->
      <div class="left-area">
        <!-- 核心指标统计模块 - 只显示 summary 中的数据 -->
        <div class="stats-card-wrap">
          <div class="stats-header">
            <el-icon color="#409EFF"><DataAnalysis /></el-icon>
            <span>核心运营指标</span>
            <el-tag size="small" type="info">近30天</el-tag>
          </div>
          <div class="stats-grid" v-if="displaySummaryList.length > 0">
            <el-card 
              v-for="item in displaySummaryList" 
              :key="item.title" 
              shadow="hover" 
              class="stat-card"
            >
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">{{ formatValue(item.value) }}</div>
            </el-card>
          </div>
          <div class="empty-stats" v-else>
            <el-icon size="40" color="#C0C4CC"><Tickets /></el-icon>
            <p>暂无统计数据，请发送运营分析指令</p>
          </div>
        </div>

        <!-- 图表区域 - 根据数据类型动态渲染 -->
        <div class="chart-area">
          <!-- 折线图 -->
          <el-card shadow="hover" class="chart-card" v-if="lineChartData">
            <template #header>
              <div class="card-header-title">
                <el-icon color="#409EFF"><DataLine /></el-icon>
                <span>{{ lineChartData.title || '时序趋势图' }}</span>
              </div>
            </template>
            <div ref="lineChartRef" class="chart-box"></div>
          </el-card>

          <!-- 柱状图区域 - 支持多个柱状图并列 -->
          <div class="bar-charts-wrapper" v-if="barChartList.length > 0">
            <el-card 
              v-for="(chart, index) in barChartList" 
              :key="index"
              shadow="hover" 
              class="chart-card bar-chart-card"
            >
              <template #header>
                <div class="card-header-title">
                  <el-icon color="#67C23A"><PieChart /></el-icon>
                  <span>{{ chart.title }}</span>
                </div>
              </template>
              <div :ref="el => setBarChartRef(el, index)" class="chart-box"></div>
            </el-card>
          </div>

          <!-- 饼图 -->
          <el-card shadow="hover" class="chart-card" v-if="pieChartData">
            <template #header>
              <div class="card-header-title">
                <el-icon color="#E6A23C"><PieChart /></el-icon>
                <span>{{ pieChartData.title || '占比分析' }}</span>
              </div>
            </template>
            <div ref="pieChartRef" class="chart-box"></div>
          </el-card>

          <!-- 空状态提示 -->
          <div class="chart-placeholder" v-if="!lineChartData && barChartList.length === 0 && !pieChartData">
            <el-icon size="48" color="#C0C4CC"><TrendCharts /></el-icon>
            <p>发送包含趋势、对比、占比等分析指令，自动渲染图表</p>
            <p class="hint-text">例如：分析最近30天医院整体运行情况</p>
          </div>
        </div>
      </div>

      <!-- 右侧：AI交互对话面板 -->
      <div class="right-chat-area">
        <el-card shadow="hover" class="chat-card">
          <template #header>
            <div class="chat-header">
              <div class="card-header-title">
                <el-icon color="#E6A23C"><ChatLineRound /></el-icon>
                <span>Agent实时对话调试</span>
              </div>
              <el-button text type="danger" size="small" @click="clearChat">清空会话</el-button>
            </div>
          </template>

          <!-- 对话消息列表 -->
          <div class="chat-message-box" ref="chatScrollRef">
            <template v-for="msg in chatList" :key="msg.id">
              <div class="msg-item user-msg" v-if="msg.type === 'user'">
                <div class="msg-role">管理员</div>
                <div class="msg-content">{{ msg.content }}</div>
              </div>
              <div class="msg-item ai-msg" v-else>
                <div class="msg-role">AI Agent</div>
                <div class="msg-content">{{ msg.content }}</div>
              </div>
            </template>
          </div>

          <!-- 发送输入框 -->
          <div class="chat-input-wrap">
            <el-input
              v-model="sendText"
              type="textarea"
              :rows="3"
              placeholder="输入运营统计指令，例如：分析最近30天影像科、检验科AI检查总量与患者就诊趋势"
              @keyup.enter="sendMessage"
              resize="none"
            />
            <div class="btn-row">
              <el-button type="primary" @click="sendMessage" :loading="chatLoading">发送消息</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, onUnmounted, computed } from 'vue'
import {
  Refresh,
  DataAnalysis,
  Tickets,
  DataLine,
  TrendCharts,
  PieChart,
  ChatLineRound
} from '@element-plus/icons-vue'
import { getAdminAnalysis } from '@/api/admin/analyze'
import type { AdminAnalysisResponse, ChartItem, SummaryItem } from '@/api/admin/analyze'
import * as echarts from 'echarts'

// ==================== DOM引用 ====================
const chatScrollRef = ref<HTMLDivElement | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)
const barChartRefs = ref<HTMLDivElement[]>([])

// ==================== 图表实例 ====================
let lineChartInstance: echarts.ECharts | null = null
let pieChartInstance: echarts.ECharts | null = null
const barChartInstances: echarts.ECharts[] = []

// ==================== 响应式数据 ====================
const loading = ref(false)
const chatLoading = ref(false)
const sendText = ref('')
const startTime = ref('')
const endTime = ref('')

// 后端返回数据存储
const summaryList = ref<SummaryItem[]>([])
const chartList = ref<ChartItem[]>([])
const lineChartData = ref<ChartItem | null>(null)
const pieChartData = ref<ChartItem | null>(null)

// ==================== 计算属性 ====================

// 只显示 summary 中的数据作为核心指标
const displaySummaryList = computed<SummaryItem[]>(() => {
  if (!summaryList.value || summaryList.value.length === 0) {
    return []
  }
  return summaryList.value
})

// 获取所有柱状图数据（排除统计类型）
const barChartList = computed<ChartItem[]>(() => {
  if (!chartList.value || chartList.value.length === 0) {
    return []
  }
  return chartList.value.filter(item => 
    item.type === 'bar' && 
    item.series && 
    item.series.length > 0 && 
    item.xaxis && 
    item.xaxis.length > 0
  )
})

// ==================== 辅助函数 ====================

const formatValue = (value: string | number): string => {
  if (typeof value === 'number') {
    if (value >= 10000) {
      return (value / 10000).toFixed(1) + '万'
    }
    if (value >= 1000) {
      return value.toLocaleString('zh-CN')
    }
    return value.toString()
  }
  return value
}

// ==================== 设置柱状图引用 ====================
const setBarChartRef = (el: any, index: number) => {
  if (el) {
    barChartRefs.value[index] = el
    // 渲染柱状图
    setTimeout(() => {
      renderBarChart(index)
    }, 300)
  }
}

// ==================== ECharts 渲染函数 ====================

// 渲染折线图
const renderLineChart = (chartData: ChartItem) => {
  if (!lineChartRef.value || !chartData) return
  
  if (!lineChartInstance) {
    lineChartInstance = echarts.init(lineChartRef.value)
  }
  
  const seriesData = chartData.series || []
  const xAxisData = chartData.xaxis || []
  
  if (seriesData.length === 0 || xAxisData.length === 0) {
    return
  }
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: seriesData.map(s => s.name),
      bottom: 0,
      icon: 'roundRect',
      itemWidth: 16,
      itemHeight: 8
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '18%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        rotate: xAxisData.length > 8 ? 30 : 0,
        interval: 0,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: '数量',
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#e8ecf1'
        }
      }
    },
    series: seriesData.map(s => ({
      name: s.name,
      type: 'line',
      data: s.data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 3
      },
      areaStyle: {
        opacity: 0.1
      }
    })),
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  }
  
  lineChartInstance.setOption(option, true)
  lineChartInstance.resize()
}

// 渲染柱状图
const renderBarChart = (index: number) => {
  const chartData = barChartList.value[index]
  if (!chartData) return
  
  const chartRef = barChartRefs.value[index]
  if (!chartRef) return
  
  // 如果实例不存在则创建
  if (!barChartInstances[index]) {
    barChartInstances[index] = echarts.init(chartRef)
  }
  
  const instance = barChartInstances[index]
  const seriesData = chartData.series || []
  const xAxisData = chartData.xaxis || []
  
  if (seriesData.length === 0 || xAxisData.length === 0) {
    return
  }
  
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        rotate: xAxisData.length > 6 ? 30 : 0,
        interval: 0,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: '数量',
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#e8ecf1'
        }
      }
    },
    series: seriesData.map((s, idx) => ({
      name: s.name,
      type: 'bar',
      data: s.data,
      barWidth: '45%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: colors[idx % colors.length]
      },
      label: {
        show: true,
        position: 'top',
        formatter: (params: any) => params.value,
        fontSize: 12,
        fontWeight: 500
      }
    })),
    color: colors
  }
  
  instance.setOption(option, true)
  instance.resize()
}

// 渲染饼图
const renderPieChart = (chartData: ChartItem) => {
  if (!pieChartRef.value || !chartData) return
  
  if (!pieChartInstance) {
    pieChartInstance = echarts.init(pieChartRef.value)
  }
  
  // 检查数据格式
  let pieData = chartData.data || []
  if (pieData.length === 0 && chartData.series && chartData.series.length > 0) {
    // 如果 data 为空但有 series，尝试从 series 构建数据
    const seriesData = chartData.series[0]
    const xAxisData = chartData.xaxis || []
    if (seriesData && seriesData.data && xAxisData.length === seriesData.data.length) {
      pieData = xAxisData.map((name, index) => ({
        name: name,
        value: seriesData.data[index] || 0
      }))
    }
  }
  
  if (pieData.length === 0) {
    return
  }
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `<div style="font-weight:bold;">${params.name}</div>
                <div>数值: ${params.value}</div>
                <div>占比: ${params.percent}%</div>`
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      itemWidth: 12,
      itemHeight: 12
    },
    series: [
      {
        name: chartData.title,
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['45%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: (params: any) => {
            return `${params.name}\n${params.percent}%`
          },
          fontSize: 11
        },
        data: pieData
      }
    ],
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#5B6ABF', '#4ECB73', '#F7B731']
  }
  
  pieChartInstance.setOption(option, true)
  pieChartInstance.resize()
}

// ==================== 图表数据监听 ====================

watch(lineChartData, (newVal) => {
  if (newVal && lineChartRef.value) {
    setTimeout(() => {
      renderLineChart(newVal)
    }, 300)
  }
})

watch(pieChartData, (newVal) => {
  if (newVal && pieChartRef.value) {
    setTimeout(() => {
      renderPieChart(newVal)
    }, 300)
  }
})

watch(barChartList, (newVal) => {
  if (newVal && newVal.length > 0) {
    setTimeout(() => {
      newVal.forEach((_, index) => {
        renderBarChart(index)
      })
    }, 400)
  }
}, { deep: true })

const handleResize = () => {
  lineChartInstance?.resize()
  pieChartInstance?.resize()
  barChartInstances.forEach(instance => instance?.resize())
}

// ==================== 对话相关 ====================
let msgId = 2

interface ChatMsg {
  id: number
  type: 'user' | 'ai'
  content: string
}

const chatList = ref<ChatMsg[]>([
  { id: 1, type: 'ai', content: '您好，管理员！您可以输入自然语言运营统计问题，系统自动识别业务维度、查询真实业务数据，通过DeepSeek生成专业运营分析，不传时间默认统计近30天全院数据。\n\n示例提问：\n1. 分析最近30天医院整体运行情况\n2. 统计各科室挂号与接诊排行\n3. 查看影像科、检验科检查完成率与异常报告数量\n4. 统计医院月度营收与每日缴费趋势' }
])

// ==================== 业务逻辑 ====================

const refreshAllData = async () => {
  loading.value = true
  summaryList.value = []
  chartList.value = []
  lineChartData.value = null
  pieChartData.value = null
  barChartRefs.value = []
  barChartInstances.length = 0
  lineChartInstance?.clear()
  pieChartInstance?.clear()
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

const sendMessage = async () => {
  const text = sendText.value.trim()
  if (!text) return
  
  chatList.value.push({ id: msgId++, type: 'user', content: text })
  const userQuestion = text
  sendText.value = ''
  chatLoading.value = true
  
  await nextTick()
  if (chatScrollRef.value) {
    chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
  }

  try {
    const reqData = {
      question: userQuestion,
      startTime: startTime.value || null,
      endTime: endTime.value || null
    }
    
    const res = await getAdminAnalysis(reqData)
    
    if (res.code === 200) {
      const data: AdminAnalysisResponse = res.data
      
      // 清空旧数据
      summaryList.value = []
      chartList.value = []
      lineChartData.value = null
      pieChartData.value = null
      barChartRefs.value = []
      barChartInstances.length = 0
      
      // 设置新数据
      summaryList.value = data.summary || []
      chartList.value = data.charts || []
      
      // 查找不同类型的图表
      lineChartData.value = chartList.value.find(c => c.type === 'line') || null
      pieChartData.value = chartList.value.find(c => c.type === 'pie') || null
      
      const analysisResult = data.analysis || '暂无分析结果'
      chatList.value.push({
        id: msgId++,
        type: 'ai',
        content: `【运营数据智能分析报告】\n${analysisResult}`
      })
      
      // 等待DOM更新后渲染图表
      await nextTick()
      
      // 渲染折线图
      if (lineChartData.value && lineChartRef.value) {
        renderLineChart(lineChartData.value)
      }
      
      // 渲染饼图
      if (pieChartData.value && pieChartRef.value) {
        renderPieChart(pieChartData.value)
      }
      
      // 柱状图会在 setBarChartRef 中自动渲染
    } else {
      chatList.value.push({
        id: msgId++,
        type: 'ai',
        content: `分析失败：${res.msg || '未知错误'}`
      })
    }
  } catch (err: any) {
    const errMsg = err?.msg || err?.message || '服务异常，运营数据智能分析失败，请稍后重试'
    chatList.value.push({
      id: msgId++,
      type: 'ai',
      content: `❌ 分析失败：${errMsg}`
    })
  } finally {
    chatLoading.value = false
    await nextTick()
    if (chatScrollRef.value) {
      chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  }
}

const clearChat = () => {
  chatList.value = [{ id: msgId++, type: 'ai', content: '✅ 会话已清空，请重新下发统计指令。' }]
  summaryList.value = []
  chartList.value = []
  lineChartData.value = null
  pieChartData.value = null
  barChartRefs.value = []
  barChartInstances.length = 0
  lineChartInstance?.clear()
  pieChartInstance?.clear()
}

// ==================== 生命周期 ====================

onMounted(() => {
  refreshAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  lineChartInstance?.dispose()
  pieChartInstance?.dispose()
  barChartInstances.forEach(instance => instance?.dispose())
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.agent-dashboard-container {
  padding: 24px;
  width: 100%;
  box-sizing: border-box;
  background-color: #f5f7fa;
  min-height: 100vh;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(64, 159, 255, 0.08);
    margin-bottom: 16px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
      h2 {
        margin: 0;
        font-size: 22px;
        color: #1f2937;
        font-weight: 600;
      }
    }
  }

  .time-select-wrap {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 10px;
    flex-wrap: wrap;
    .split-line {
      color: #909399;
    }
    .el-text {
      font-size: 13px;
    }
  }

  .main-layout {
    display: flex;
    gap: 24px;
    height: calc(100vh - 240px);
    min-height: 600px;
  }

  .left-area {
    flex: 0 0 70%;
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-width: 0;
    overflow-y: auto;

    // 统计卡片区域
    .stats-card-wrap {
      background: #fff;
      border-radius: 12px;
      padding: 16px 20px 20px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

      .stats-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #1f2937;
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid #f0f2f5;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
        gap: 12px;
      }

      .stat-card {
        padding: 14px 16px;
        text-align: center;
        border-radius: 8px;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(64, 159, 255, 0.12);
        }
        
        .stat-title {
          font-size: 12px;
          color: #909399;
          margin-bottom: 6px;
          font-weight: 500;
        }
        .stat-value {
          font-size: 22px;
          font-weight: 700;
          background: linear-gradient(135deg, #409EFF, #2979e2);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }
      }

      .empty-stats {
        text-align: center;
        padding: 40px 20px;
        color: #909399;
        p {
          margin-top: 12px;
          font-size: 14px;
        }
      }
    }

    // 图表区域
    .chart-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 16px;
      min-height: 300px;

      .chart-card {
        border-radius: 12px;
        
        :deep(.el-card__header) {
          padding: 12px 20px;
          border-bottom: 1px solid #f0f2f5;
          
          .card-header-title {
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: 500;
            font-size: 14px;
            color: #1f2937;
          }
        }
        
        :deep(.el-card__body) {
          padding: 12px;
          
          .chart-box {
            width: 100%;
            height: 280px;
          }
        }
      }

      // 柱状图包装器 - 支持多个柱状图并列
      .bar-charts-wrapper {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 16px;

        .bar-chart-card {
          :deep(.el-card__body) {
            .chart-box {
              height: 260px;
            }
          }
        }
      }

      .chart-placeholder {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: #fff;
        border-radius: 12px;
        border: 1.5px dashed #dcdfe6;
        padding: 60px 20px;
        min-height: 300px;
        
        p {
          margin: 12px 0 0;
          color: #909399;
          font-size: 14px;
        }
        .hint-text {
          color: #c0c4cc;
          font-size: 13px;
          margin-top: 4px;
        }
      }
    }
  }

  .right-chat-area {
    flex: 0 0 30%;
    min-width: 300px;
    max-width: 420px;
    
    .chat-card {
      height: 100%;
      display: flex;
      flex-direction: column;
      border-radius: 12px;
      
      :deep(.el-card__header) {
        padding: 14px 20px;
        border-bottom: 1px solid #f0f2f5;
        flex-shrink: 0;
        .chat-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          .card-header-title {
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: 500;
            font-size: 15px;
          }
        }
      }
      
      :deep(.el-card__body) {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 0;
        overflow: hidden;
      }
      
      .chat-message-box {
        flex: 1;
        padding: 16px;
        overflow-y: auto;
        background: #f7f8fa;
        
        &::-webkit-scrollbar {
          width: 5px;
        }
        &::-webkit-scrollbar-thumb {
          background: #dcdfe6;
          border-radius: 3px;
        }
        &::-webkit-scrollbar-track {
          background: transparent;
        }
        
        .msg-item {
          margin-bottom: 16px;
          
          .msg-role {
            font-size: 12px;
            color: #909399;
            margin-bottom: 4px;
            font-weight: 500;
          }
          
          .msg-content {
            padding: 10px 14px;
            border-radius: 10px;
            font-size: 14px;
            line-height: 1.7;
            white-space: pre-wrap;
            word-break: break-word;
            max-width: 92%;
          }
        }
        
        .user-msg {
          text-align: right;
          .msg-content {
            background: linear-gradient(135deg, #409EFF, #2979e2);
            color: #fff;
            margin-left: auto;
            border-bottom-right-radius: 2px;
            box-shadow: 0 2px 8px rgba(64, 159, 255, 0.3);
          }
        }
        
        .ai-msg {
          text-align: left;
          .msg-content {
            background: #ffffff;
            border: 1px solid #e4e7ed;
            border-bottom-left-radius: 2px;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
          }
        }
      }
      
      .chat-input-wrap {
        padding: 16px;
        border-top: 1px solid #e4e7ed;
        background: #fff;
        flex-shrink: 0;
        
        :deep(.el-textarea__inner) {
          border-radius: 8px;
          font-size: 13px;
          line-height: 1.6;
        }
        
        .btn-row {
          margin-top: 12px;
          text-align: right;
        }
      }
    }
  }
}

@media screen and (max-width: 1400px) {
  .main-layout {
    flex-direction: column;
    height: auto !important;
    
    .left-area {
      flex: 1 !important;
      max-height: 800px;
    }
    
    .right-chat-area {
      flex: 1 !important;
      min-width: unset !important;
      max-width: unset !important;
      
      .chat-card {
        height: 500px;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .agent-dashboard-container {
    padding: 12px;
  }
  
  .page-header .header-left h2 {
    font-size: 16px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)) !important;
  }
  
  .bar-charts-wrapper {
    grid-template-columns: 1fr !important;
  }
}
</style>
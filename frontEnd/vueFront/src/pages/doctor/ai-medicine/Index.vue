<template>
  <div class="page-container">
    <div class="chat-header">
      <div class="header-left">
        <div class="logo-icon"></div>
        <div class="header-title">
          <h2>AI 药物推荐</h2>
          <span>CloudBrainMed · 医生专用药品快速查询系统</span>
        </div>
      </div>
      <div class="header-right">
        <el-tag type="primary" effect="light">在线问诊</el-tag>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-sidebar">
        <div class="sidebar-title">用药查询指引</div>
        <div class="sidebar-desc">
          <p>本模块面向临床医生，可快速查询：</p>
          <div class="desc-item">• 各类药品适应症、用法用量</div>
          <div class="desc-item">• 用药禁忌与不良反应提示</div>
          <div class="desc-item">• 常见病对症用药推荐</div>
        </div>
        <div class="sidebar-tip">
          <p>💡 使用提示：输入病症/药品名称，AI自动返回权威用药参考</p>
        </div>
      </div>

      <div class="chat-content">
        <div class="chat-box" ref="chatBox">
          <div class="chat-item ai-item" v-if="chatList.length === 0">
            <div class="avatar ai-avatar">AI</div>
            <div class="bubble ai-bubble">
              您好医生，欢迎使用CloudBrainMed药品智能查询系统。请输入病症或药品名称，我将为您快速查询用药信息、适应症、用量及注意事项。
            </div>
          </div>

          <div v-for="(item, idx) in chatList" :key="idx" class="chat-item" :class="item.role === 'user' ? 'user-item' : 'ai-item'">
            <div class="avatar" :class="item.role === 'user' ? 'user-avatar' : 'ai-avatar'">
              {{ item.role === 'user' ? '医' : 'AI' }}
            </div>
            <div class="bubble" :class="item.role === 'user' ? 'user-bubble' : 'ai-bubble'" v-html="formatContent(item.content)"></div>
          </div>
        </div>

        <div class="input-bar">
          <el-input
              v-model="question"
              placeholder="请输入病症或药品名称，例如：风寒感冒常用药？"
              @keyup.enter="sendQuestion"
              size="large"
              class="input-box"
              clearable
              :disabled="loading"
          />
          <el-button type="primary" @click="sendQuestion" size="large" class="send-btn" :loading="loading">
            {{ loading ? '查询中...' : '发送查询' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'

interface ChatMessage {
  role: 'user' | 'ai'
  content: string
}

const sessionId = ref<string>('session_' + Date.now())
const question = ref<string>('')
const chatList = ref<ChatMessage[]>([])
const chatBox = ref<HTMLDivElement | null>(null)
const loading = ref<boolean>(false)

const formatContent = (text: string): string => {
  if (!text) return ''
  let html = text.replace(/^>\s?/gm, '')
  html = html.replace(/>\s*/g, '')
  html = html.replace(/【(.*?)】/g, '<div class="section-title">【$1】</div>')
  html = html.replace(/\n/g, '<br>')
  return html
}

const scrollToBottom = () => {
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}

const sendQuestion = async (): Promise<void> => {
  if (!question.value.trim() || loading.value) return
  const q = question.value
  chatList.value.push({ role: 'user', content: q })
  question.value = ''
  loading.value = true
  chatList.value.push({ role: 'ai', content: '' })
  const lastIdx = chatList.value.length - 1
  await nextTick(() => scrollToBottom())

  try {
    const response = await fetch('/api/ai/medicine/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: sessionId.value, question: q })
    })
    if (!response.ok) {
      // 读取响应文本用于调试（例如 404/500 的错误信息），并抛出详细错误以便在 catch 中记录
      const errText = await response.text().catch(() => '')
      throw new Error(`请求失败 ${response.status} ${errText}`)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const msg = chatList.value[lastIdx]
          if (msg) msg.content += line.substring(5)
        }
      }
      await nextTick(() => scrollToBottom())
    }
    if (buffer.startsWith('data:')) {
      const msg = chatList.value[lastIdx]
      if (msg) msg.content += buffer.substring(5)
    }
  } catch (e) {
    // 更详细地在控制台输出错误，方便定位（网络/跨域/404/后端异常）
    console.error('AI 请求失败：', e)
    const msg = chatList.value[lastIdx]
    if (msg) msg.content = '抱歉，查询失败，请稍后重试。'
  } finally {
    loading.value = false
    await nextTick(() => scrollToBottom())
  }
}
</script>

<style scoped>
html, body, #app {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
}

/* 页面容器：使用系统全局底色 --bg */
.page-container {
  display: flex;
  flex-direction: column;
  width: 100%; /* 使用 100% 避免 100vw + padding 导致水平溢出被隐藏 */
  height: 100vh;
  background: var(--bg, #f1f5f9);
  padding: 24px 32px;
  box-sizing: border-box;
}

/* 头部导航：使用系统品牌色 --brand */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--brand, #2563eb);
  padding: 20px 28px;
  border-radius: 12px;
  box-shadow: 0 1px 12px rgba(37, 99, 235, 0.12);
  margin-bottom: 20px;
  color: #FFFFFF;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(255,255,255,0.2);
}

.header-title h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #ffffff;
}
.header-title span {
  font-size: 14px;
  color: rgba(255,255,255,0.85);
}

.chat-main {
  display: flex;
  flex: 1;
  gap: 20px;
  min-width: 0;
  min-height: 0;
  /* 使用 auto 允许出现滚动而不是裁剪右侧内容 */
  overflow: auto;
}

/* 左侧侧边栏卡片：和首页卡片样式统一 */
.chat-sidebar {
  width: 290px;
  flex-shrink: 0;
  background: #FFFFFF;
  border-radius: 12px;
  padding: 26px 22px;
  box-shadow: 0 1px 12px rgba(0,0,0,0.06);
  color: #5A6A7E;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1A1F36;
  padding-bottom: 12px;
  border-bottom: 1px solid #E5EBF5;
  margin-bottom: 18px;
}

.sidebar-desc {
  font-size: 14px;
  color: #5A6A7E;
  line-height: 1.75;
}

.desc-item {
  margin: 7px 0;
  padding-left: 4px;
}

.sidebar-tip {
  margin-top: 32px;
  padding: 15px;
  background: #f0f4ff;
  border-radius: 10px;
  font-size:13px;
  color:#485770;
  line-height:1.65;
}

/* 右侧聊天主体卡片，同系统卡片圆角阴影 */
.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 1px 12px rgba(0,0,0,0.06);
  overflow: hidden;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 26px 30px;
  background: #FCFDFF;
  min-height: 0;
  color: #1A1F36;
}

.input-bar {
  display: flex;
  gap: 12px;
  padding: 20px 30px;
  border-top: 1px solid #E5EBF5;
  flex-shrink: 0;
  align-items: center;
}
.input-box {
  flex: 1;
  min-width: 0; /* 允许在小屏时输入框缩小，避免按钮被挤出 */
}
.send-btn {
  min-width: 130px;
  background-color: var(--brand, #2563eb);
  flex-shrink: 0;
}

.bubble {
  max-width: 72%;
  padding: 13px 17px;
  border-radius: 12px;
  line-height: 1.75;
  font-size: 14px;
  color: #1A1F36;
}

.ai-bubble {
  background: #F7FAFF;
  border: 1px solid #E5EBF5;
  color: #1A1F36;
  white-space: normal;
  word-break: break-word;
  line-height: 1.9;
}

.user-bubble {
  background: var(--brand, #2563eb);
  color: #FFFFFF;
}

.avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  color: #FFFFFF;
}

.ai-avatar {
  background: var(--brand, #2563eb);
  color: #fff;
}

.user-avatar {
  background: var(--brand-light, #74B3FF);
  color: #fff;
}

.chat-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

/* 响应式：在窄屏隐藏侧边栏，使聊天主体完整可见 */
@media (max-width: 900px) {
  .chat-sidebar {
    display: none;
  }
  .page-container {
    padding: 16px;
  }
  .chat-content {
    border-radius: 8px;
  }
  .input-bar {
    padding: 12px 16px;
  }
}

.user-item {
  flex-direction: row-reverse;
}

.section-title {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 700;
  color: #2563eb;
  font-size: 15px;
}
</style>

<template>
  <div class="page">
    <header class="page-top">
      <div class="page-top-left">
        <h2>检查检验工作台</h2>
        <p class="top-sub">管理检查检验任务，按优先级处理</p>
      </div>
      <div class="page-top-right">
        <el-button type="primary" @click="$router.push('/doctor/queue')">
          <el-icon style="margin-right:4px"><List /></el-icon>查看队列
        </el-button>
        <el-button @click="fetchTasks" :loading="loading">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </header>

    <div class="card">
      <el-table :data="tasks" stripe v-loading="loading" empty-text="暂无待处理任务">
        <el-table-column label="紧急程度" width="80">
          <template #default="{ row }">
            <span class="badge" :class="'badge-' + row.urgencyLevel">{{ row.urgencyLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="项目名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类别" width="70">
          <template #default="{ row }">
            <span>{{ row.itemCategory === 'EXAM' ? '检查' : '检验' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="患者" width="130">
          <template #default="{ row }">
            <div class="patient-cell">
              <div class="pc-avatar">{{ row.patientName?.charAt(0) || '?' }}</div>
              <div>
                <div class="pc-name">{{ row.patientName }}</div>
                <div class="pc-meta">{{ row.gender === 1 ? '男' : '女' }} · {{ row.age }}岁</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="status-tag" :class="'st-' + row.status">{{ row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/doctor/task/${row.orderItemId}`)">
              查看
            </el-button>
            <el-button v-if="row.status === 'QUEUED'" type="success" size="small" @click="handleStart(row)">
              开始
            </el-button>
            <el-button v-if="row.status === 'IN_PROCESS'" type="warning" size="small" @click="$router.push(`/doctor/task/${row.orderItemId}`)">
              提交报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWorkbench, startTask } from '@/api/doctor/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Refresh } from '@element-plus/icons-vue'

const tasks = ref<any[]>([])
const loading = ref(false)

onMounted(() => fetchTasks())

async function fetchTasks() {
  loading.value = true
  try {
    const res = await getWorkbench()
    tasks.value = res.data || []
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

async function handleStart(row: any) {
  try {
    await ElMessageBox.confirm(`确认开始处理「${row.itemName}」？`, '提示', { type: 'info' })
    await startTask(row.orderItemId)
    ElMessage.success('已开始处理')
    fetchTasks()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

</script>

<style scoped>
.page { padding: 24px; }
.page-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-top-left h2 { margin: 0 0 4px; font-size: 20px; }
.top-sub { color: #999; font-size: 13px; margin: 0; }
.page-top-right { display: flex; gap: 8px; }
.card { background: #fff; border-radius: 8px; padding: 20px; }

.badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.badge-EMERGENCY { background: #fef0f0; color: #f56c6c; }
.badge-URGENT { background: #fdf6ec; color: #e6a23c; }
.badge-NORMAL { background: #f0f9eb; color: #67c23a; }

.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.st-QUEUED { background: #ecf5ff; color: #409eff; }
.st-IN_PROCESS { background: #fdf6ec; color: #e6a23c; }
.st-COMPLETED { background: #f0f9eb; color: #67c23a; }
.st-WAITING_ASSIGN { background: #f4f4f5; color: #909399; }

.patient-cell { display: flex; align-items: center; gap: 8px; }
.pc-avatar { width: 32px; height: 32px; border-radius: 50%; background: #409eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.pc-name { font-size: 13px; }
.pc-meta { font-size: 11px; color: #999; }
</style>

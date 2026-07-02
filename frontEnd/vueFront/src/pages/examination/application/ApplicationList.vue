<template>
  <div class="exam-order-page">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="28"><Document /></el-icon>
        </div>
        <div class="header-content">
          <h2>检查申请管理</h2>
          <p class="header-desc">管理和查看所有检查申请记录</p>
        </div>
      </div>
      <div class="header-right">
        <el-tag type="info" size="large" effect="plain">
          <el-icon><User /></el-icon>
          医生ID: {{ doctorId }}
        </el-tag>
        <el-button 
          type="primary" 
          :icon="Refresh" 
          @click="refreshData" 
          :loading="loading"
          plain
        >
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #ecf5ff; color: #409eff">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ pagination.total }}</div>
                <div class="stat-label">总申请数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">
                  {{ tableData.filter(item => item.status === 'WAITING_ASSIGN').length }}
                </div>
                <div class="stat-label">待处理</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #f0f9ff; color: #67c23a">
                <el-icon><Check /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">
                  {{ tableData.filter(item => item.status === 'COMPLETED').length }}
                </div>
                <div class="stat-label">已完成</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #fef0f0; color: #f56c6c">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">
                  {{ tableData.filter(item => item.urgency_level === 'URGENT').length }}
                </div>
                <div class="stat-label">紧急申请</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="search-area">
      <el-card shadow="never" class="search-card">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="状态">
            <el-select 
              v-model="queryParams.status" 
              placeholder="全部状态" 
              clearable
              @change="handleSearch"
              style="width: 160px"
            >
              <el-option label="待分配" value="WAITING_ASSIGN" />
              <el-option label="已分配" value="ASSIGNED" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
          </el-form-item>

          <el-form-item label="紧急程度">
            <el-select 
              v-model="queryParams.urgency_level" 
              placeholder="全部" 
              clearable
              @change="handleSearch"
              style="width: 140px"
            >
              <el-option label="紧急" value="URGENT" />
              <el-option label="普通" value="NORMAL" />
            </el-select>
          </el-form-item>

          <el-form-item label="患者ID">
            <el-input 
              v-model="queryParams.patient_id" 
              placeholder="请输入患者ID"
              clearable
              @keyup.enter="handleSearch"
              style="width: 200px"
              prefix-icon="Search"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch" :icon="Search">
              查询
            </el-button>
            <el-button @click="resetSearch" :icon="RefreshRight">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 数据表格 -->
    <div class="table-area">
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="table-header">
            <div class="table-header-left">
              <span class="table-title">📋 申请列表</span>
              <el-tag type="info" size="small" round>
                共 {{ pagination.total }} 条记录
              </el-tag>
            </div>
            <div class="table-header-right">
              <el-button 
                size="small" 
                :icon="Refresh" 
                @click="refreshData"
                :loading="loading"
                text
              >
                刷新
              </el-button>
            </div>
          </div>
        </template>

        <el-table 
          :data="tableData" 
          v-loading="loading"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          @row-click="viewDetail"
        >
          <el-table-column prop="order_item_id" label="项目ID" width="140" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info" effect="plain">
                {{ row.order_item_id }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="order_id" label="申请ID" width="140" align="center" />
          
          <el-table-column prop="item_code" label="项目编码" width="120" align="center" />
          
          <el-table-column prop="item_name" label="项目名称" min-width="150">
            <template #default="{ row }">
              <span class="item-name">{{ row.item_name }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="item_category" label="类别" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="primary" round>
                {{ row.item_category }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="urgency_level" label="紧急程度" width="110" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="row.urgency_level === 'URGENT' ? 'danger' : 'info'"
                size="small"
                :effect="row.urgency_level === 'URGENT' ? 'dark' : 'plain'"
                round
              >
                <el-icon v-if="row.urgency_level === 'URGENT'" class="mr-5"><WarningFilled /></el-icon>
                {{ row.urgency_level === 'URGENT' ? '紧急' : '普通' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="price" label="价格" width="100" align="right">
            <template #default="{ row }">
              <span class="price">¥{{ row.price.toFixed(2) }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="getStatusType(row.status)"
                size="small"
                round
                :effect="row.status === 'WAITING_ASSIGN' ? 'dark' : 'plain'"
              >
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="create_time" label="创建时间" width="170" align="center">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.create_time) }}</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="130" fixed="right" align="center">
            <template #default="{ row }">
              <el-button 
                size="small" 
                type="primary" 
                plain
                round
                @click.stop="viewDetail(row)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-area">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            background
          />
        </div>
      </el-card>
    </div>

    <!-- 详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      title="检查项目详情" 
      width="750px"
      destroy-on-close
      class="detail-dialog"
    >
      <div v-if="detailData" class="detail-content">
        <div class="detail-header">
          <el-tag :type="getStatusType(detailData.status)" size="large" effect="dark">
            {{ getStatusText(detailData.status) }}
          </el-tag>
          <el-tag 
            :type="detailData.urgency_level === 'URGENT' ? 'danger' : 'info'"
            size="large"
            effect="plain"
          >
            {{ detailData.urgency_level === 'URGENT' ? '🔴 紧急' : '🟢 普通' }}
          </el-tag>
        </div>
        
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="项目ID" label-class-name="label-class">
            <el-tag size="small" type="info">{{ detailData.order_item_id }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请ID">
            {{ detailData.order_id }}
          </el-descriptions-item>
          <el-descriptions-item label="项目编码">
            {{ detailData.item_code }}
          </el-descriptions-item>
          <el-descriptions-item label="项目名称" span="2">
            <span class="detail-item-name">{{ detailData.item_name }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="类别">
            <el-tag size="small" type="primary">{{ detailData.item_category }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="科室ID">
            {{ detailData.assigned_dept_id || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="价格" span="2">
            <span class="detail-price">¥{{ detailData.price.toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" span="2">
            {{ formatTime(detailData.create_time) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" span="2">
            {{ formatTime(detailData.update_time) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="detailDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { 
  Document, User, Refresh, Search, RefreshRight, 
  Clock, Check, Warning, WarningFilled 
} from '@element-plus/icons-vue';
import { examApi } from '@/api/examination/examApi';

const router = useRouter();
const doctorId = ref(sessionStorage.getItem('doctorId') || '');

const queryParams = reactive({
  status: '',
  urgency_level: '',
  patient_id: '',
  page: 1,
  page_size: 20
});

const tableData = ref<any[]>([]);
const loading = ref(false);
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
});
const detailDialogVisible = ref(false);
const detailData = ref<any>(null);

const fetchExamOrders = async () => {
  loading.value = true;
  try {
    const params = {
      status: queryParams.status || undefined,
      urgency_level: queryParams.urgency_level || undefined,
      patient_id: queryParams.patient_id || undefined,
      page: pagination.currentPage,
      page_size: pagination.pageSize
    };
    const response = await examApi.getExamOrders(params);
    console.log('API响应:', response);
    
    if (response.code === 200) {
      tableData.value = response.data.items || [];
      pagination.total = response.data.total || 0;
    } else {
      ElMessage.error(response.message || '获取数据失败');
    }
  } catch (err: any) {
    if (err.response?.status === 401) {
      ElMessageBox.alert('登录失效，请重新登录', '提示').then(() => {
        sessionStorage.clear();
        router.push('/login');
      });
    } else {
      ElMessage.error('获取数据失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.currentPage = 1;
  fetchExamOrders();
};

const resetSearch = () => {
  queryParams.status = '';
  queryParams.urgency_level = '';
  queryParams.patient_id = '';
  pagination.currentPage = 1;
  fetchExamOrders();
};

const refreshData = () => fetchExamOrders();

const handleSizeChange = (val: number) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  fetchExamOrders();
};

const handleCurrentChange = (val: number) => {
  pagination.currentPage = val;
  fetchExamOrders();
};

const viewDetail = (row: any) => {
  detailData.value = row;
  detailDialogVisible.value = true;
};

const formatTime = (time: string) => {
  if (!time) return '-';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit'
  });
};

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'WAITING_ASSIGN': 'warning',
    'ASSIGNED': 'primary',
    'COMPLETED': 'success',
    'CANCELLED': 'info'
  };
  return map[status] || 'info';
};

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'WAITING_ASSIGN': '待分配',
    'ASSIGNED': '已分配',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  };
  return map[status] || status;
};

onMounted(async () => {
  const token = sessionStorage.getItem('token');
  console.log('成功读到token');
  if (!token) {
    ElMessage.warning('未检测到登录身份，请重新登录');
    router.push('/login');
    return;
  }
  await refreshData();
});
</script>

<style scoped>
.exam-order-page {
  padding: 20px;
  background: #f0f2f5;
  min-height: 100vh;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-content h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.header-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 统计卡片 */
.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 搜索区域 */
.search-area {
  margin-bottom: 20px;
}

.search-card {
  border-radius: 12px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

/* 表格区域 */
.table-area {
  border-radius: 12px;
  overflow: hidden;
}

.table-card {
  border-radius: 12px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.mr-5 {
  margin-right: 5px;
}

.item-name {
  font-weight: 500;
  color: #303133;
}

.price {
  font-weight: 600;
  color: #f56c6c;
  font-size: 14px;
}

.time-text {
  font-size: 13px;
  color: #909399;
}

/* 分页 */
.pagination-area {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 详情对话框 */
.detail-dialog :deep(.el-dialog) {
  border-radius: 12px;
}

.detail-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.detail-descriptions :deep(.el-descriptions__label) {
  font-weight: 600;
  color: #606266;
  background: #fafafa;
}

.detail-item-name {
  font-weight: 500;
  color: #303133;
}

.detail-price {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header-right {
    width: 100%;
    justify-content: flex-start;
  }
  
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-form :deep(.el-form-item) {
    margin-bottom: 12px;
    width: 100%;
  }
  
  .search-form :deep(.el-form-item .el-input),
  .search-form :deep(.el-form-item .el-select) {
    width: 100% !important;
  }
  
  .statistics-cards .el-col {
    margin-bottom: 12px;
  }
}
</style>
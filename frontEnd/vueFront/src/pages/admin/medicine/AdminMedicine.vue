<!-- src/pages/admin/medicine/AdminMedicine.vue -->
<template>
  <div class="medicine-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon><PieChart /></el-icon>
          药品信息管理
        </h2>
        <div class="header-tags">
          <el-tag size="small" type="info">实时库存</el-tag>
          <el-tag size="small" type="success">智能预警</el-tag>
          <el-tag size="small" type="warning">批次管理</el-tag>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加药品
        </el-button>
      </div>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" @click="handleFilter('all')">
        <div class="stat-icon total">
          <el-icon><Box /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ totalCount }}</div>
          <div class="stat-label">药品总数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          +{{ totalCount > 0 ? Math.round(totalCount * 0.02) : 0 }}%
        </div>
      </div>
      
      <div class="stat-card warning" @click="handleWarningCardClick('warning')">
        <div class="stat-icon warn">
          <el-icon><WarningFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ warningCount }}</div>
          <div class="stat-label">库存预警</div>
        </div>
        <div class="stat-trend down">
          <el-icon><Warning /></el-icon>
          {{ warningCount > 0 ? '需关注' : '正常' }}
        </div>
      </div>
      
      <div class="stat-card danger" @click="handleWarningCardClick('danger')">
        <div class="stat-icon danger">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ outOfStockCount }}</div>
          <div class="stat-label">缺货药品</div>
        </div>
        <div class="stat-trend down">
          <el-icon><Timer /></el-icon>
          {{ outOfStockCount > 0 ? '紧急补货' : '充足' }}
        </div>
      </div>
      
      <div class="stat-card success" @click="handleFilter('normal')">
        <div class="stat-icon normal">
          <el-icon><Check /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ normalCount }}</div>
          <div class="stat-label">库存正常</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          +{{ normalCount > 0 ? Math.round(normalCount * 0.01) : 0 }}%
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索药品名称"
          clearable
          style="width: 260px"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="statusFilter"
          placeholder="库存状态"
          clearable
          style="width: 120px"
          @change="handleFilterChange"
        >
          <el-option label="全部状态" value="" />
          <el-option label="正常" value="NORMAL" />
          <el-option label="库存不足" value="LOW_STOCK" />
          <el-option label="缺货" value="OUT_OF_STOCK" />
        </el-select>

        <el-select
          v-model="sortField"
          placeholder="排序方式"
          style="width: 120px"
          @change="handleSortChange"
        >
          <el-option label="按名称 A-Z" value="name_asc" />
          <el-option label="按名称 Z-A" value="name_desc" />
          <el-option label="按库存从高到低" value="stock_desc" />
          <el-option label="按库存从低到高" value="stock_asc" />
          <el-option label="按价格从高到低" value="price_desc" />
          <el-option label="按价格从低到高" value="price_asc" />
        </el-select>

        <el-tag 
          type="warning" 
          size="default" 
          style="margin-left: 8px; cursor: pointer;"
          @click="openWarnConfigDialog"
        >
          <el-icon><Setting /></el-icon>
          预警线: {{ globalWarnThreshold }}
        </el-tag>
      </div>
      
      <div class="toolbar-right">
        <el-badge :value="warningCount" :hidden="warningCount === 0" type="danger">
          <el-button @click="fetchWarnings">
            <el-icon><Bell /></el-icon>
            预警通知
          </el-button>
        </el-badge>
        
        <el-dropdown @command="handleBatchCommand">
          <el-button>
            批量操作 <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">批量编辑</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除选中</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 药品表格 -->
    <div class="table-card">
      <el-table
        :data="filteredAndSortedData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :row-class-name="getRowClassName"
        row-key="medicineId"
        :reserve-selection="true"
      >
        <el-table-column type="selection" width="40" :reserve-selection="true" />
        
        <el-table-column prop="name" label="药品名称" min-width="150">
          <template #default="{ row }">
            <el-tooltip :content="row.name" placement="top" effect="dark" :disabled="row.name.length <= 6">
              <span class="drug-name-text">{{ row.name }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="spec" label="规格" width="160">
          <template #default="{ row }">
            <el-tooltip :content="row.spec" placement="top" effect="dark" :disabled="row.spec.length <= 12">
              <span class="spec-text">{{ row.spec }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="stock" label="库存" width="150" align="center">
          <template #default="{ row }">
            <div class="stock-indicator">
              <el-progress
                :percentage="getStockPercentage(row)"
                :color="getProgressColor(row)"
                :stroke-width="6"
                :show-text="false"
              />
              <span :class="getStockClass(row)">
                {{ row.stock }}
              </span>
              <span class="stock-unit">件</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="minStock" label="预警线" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.minStock }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="price" label="单价" width="110" align="center">
          <template #default="{ row }">
            ¥{{ Number(row.price).toFixed(2) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" effect="dark">
              <el-icon v-if="row.status === 'OUT_OF_STOCK'" size="12"><CircleClose /></el-icon>
              <el-icon v-else-if="row.status === 'LOW_STOCK'" size="12"><Warning /></el-icon>
              <el-icon v-else size="12"><Check /></el-icon>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="290" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" link @click="handleView(row)">
                <el-icon><View /></el-icon>详情
              </el-button>
              <el-divider direction="vertical" />
              <el-button size="small" type="warning" link @click="handleDeduct(row)" :disabled="row.stock <= 0">
                <el-icon><Minus /></el-icon>扣除
              </el-button>
              <el-divider direction="vertical" />
              <el-button size="small" type="success" link @click="handleAddStock(row)">
                <el-icon><Plus /></el-icon>补货
              </el-button>
              <el-divider direction="vertical" />
              <el-button size="small" type="primary" link @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-divider direction="vertical" />
              <el-button size="small" type="danger" link @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:page-size="pageSize"
          v-model:current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalCount"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 药品详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="药品详情" width="600px">
      <div v-if="detailData" class="detail-content">
        <div class="detail-header">
          <el-avatar :size="64" :style="{ backgroundColor: '#409EFF' }">
            <el-icon size="36"><Box /></el-icon>
          </el-avatar>
          <div class="detail-title">
            <h3>{{ detailData.name }}</h3>
            <div class="detail-meta">
              <el-tag size="small">药品</el-tag>
              <span class="meta-item">规格: {{ detailData.spec }}</span>
              <span class="meta-item">创建: {{ detailData.createTime }}</span>
            </div>
          </div>
        </div>
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="库存数量">
            <span :class="getStockClass(detailData)">{{ detailData.stock }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预警线">
            {{ detailData.minStock }}
          </el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ Number(detailData.price).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用法用量" :span="2">{{ detailData.usage || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="适应症" :span="2">{{ detailData.indication || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="注意事项" :span="2">{{ detailData.attention || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 添加/编辑弹窗 - 移除了预警线字段 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="药品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item label="规格" prop="spec">
          <el-input v-model="formData.spec" placeholder="如: 10mg×20片" />
        </el-form-item>
        <el-form-item label="用法用量" prop="usage">
          <el-input v-model="formData.usage" placeholder="如: 口服，每次1片" />
        </el-form-item>
        <el-form-item label="适应症" prop="indication">
          <el-input v-model="formData.indication" placeholder="请输入适应症" />
        </el-form-item>
        <el-form-item label="注意事项" prop="attention">
          <el-input v-model="formData.attention" type="textarea" :rows="2" placeholder="请输入注意事项" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number v-model="formData.stock" :min="0" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价" prop="price">
              <el-input-number v-model="formData.price" :min="0" :precision="2" :step="0.5" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert 
          title="预警线为全局配置，可在顶部点击「预警线: X」进行修改" 
          type="info" 
          :closable="false"
          show-icon
          style="margin-top: 8px;"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 扣除库存弹窗 -->
    <el-dialog v-model="deductDialogVisible" title="扣除库存" width="420px" destroy-on-close>
      <div class="dialog-info">
        <div class="info-row">
          <span class="info-label">药品名称：</span>
          <span class="info-value">{{ deductTarget?.name || '' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前库存：</span>
          <span class="info-value" :class="getStockClass(deductTarget || {} as Medicine)">
            {{ deductTarget?.stock || 0 }}
          </span>
        </div>
        <div class="info-row">
          <span class="info-label">预警线：</span>
          <span class="info-value">{{ deductTarget?.minStock || 0 }}</span>
        </div>
      </div>
      <el-form label-width="100px">
        <el-form-item label="扣除数量">
          <el-input-number v-model="deductQuantity" :min="1" :max="deductTarget?.stock || 0" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <div v-if="deductTarget && (deductTarget.stock - deductQuantity) <= deductTarget.minStock" class="warning-hint">
          <el-alert :title="(deductTarget.stock - deductQuantity) <= 0 ? '扣除后将缺货' : '扣除后将低于预警线'" type="warning" :closable="false" show-icon />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="deductDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deductLoading" @click="handleDeductConfirm">确认扣除</el-button>
      </template>
    </el-dialog>

    <!-- 补货弹窗 -->
    <el-dialog v-model="addStockDialogVisible" title="补货" width="420px" destroy-on-close>
      <div class="dialog-info">
        <div class="info-row">
          <span class="info-label">药品名称：</span>
          <span class="info-value">{{ addStockTarget?.name || '' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前库存：</span>
          <span class="info-value" :class="getStockClass(addStockTarget || {} as Medicine)">
            {{ addStockTarget?.stock || 0 }}
          </span>
        </div>
        <div class="info-row" v-if="addStockTarget">
          <span class="info-label">建议补货量：</span>
          <el-tag type="warning" size="small">{{ addStockTarget.reorderQuantity || 50 }}</el-tag>
        </div>
      </div>
      <el-form label-width="100px">
        <el-form-item label="补货数量">
          <el-input-number v-model="addStockQuantity" :min="1" :step="5" controls-position="right" style="width: 100%" />
        </el-form-item>
        <div v-if="addStockTarget" class="warning-hint">
          <el-alert title="补货后库存将恢复正常水平" type="success" :closable="false" show-icon />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="addStockDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="addStockLoading" @click="handleAddStockConfirm">确认补货</el-button>
      </template>
    </el-dialog>

    <!-- 批量编辑弹窗 - 移除了预警线 -->
    <el-dialog v-model="batchEditDialogVisible" title="批量编辑" width="520px" destroy-on-close>
      <div class="dialog-info">
        <div class="info-row">
          <span class="info-label">已选药品：</span>
          <span class="info-value">{{ selectedRows.length }} 个</span>
        </div>
        <div class="info-row" v-if="selectedRows.length > 0">
          <span class="info-label">药品列表：</span>
          <span class="info-value" style="font-size: 13px; color: #6b7280;">
            {{ selectedRows.map(r => r.name).join('、') }}
          </span>
        </div>
        <div class="info-row" style="margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e5e7eb;">
          <span class="info-label">当前预警线：</span>
          <span class="info-value" style="color: #d97706;">
            {{ globalWarnThreshold }}
          </span>
        </div>
      </div>
      
      <el-form ref="batchFormRef" :model="batchFormData" label-width="100px">
        <el-form-item label="单价">
          <el-input-number 
            v-model="batchFormData.price" 
            :min="0" 
            :precision="2" 
            :step="0.5" 
            controls-position="right" 
            style="width: 100%" 
            placeholder="不修改请留空"
          />
          <div class="form-hint">留空表示不修改</div>
        </el-form-item>
        <el-form-item label="建议补货量">
          <el-input-number 
            v-model="batchFormData.reorderQuantity" 
            :min="0" 
            :step="5" 
            controls-position="right" 
            style="width: 100%" 
            placeholder="不修改请留空"
          />
          <div class="form-hint">留空表示不修改</div>
        </el-form-item>
      </el-form>
      <el-alert 
        title="预警线为全局配置，请点击顶部「预警线: X」进行统一修改" 
        type="info" 
        :closable="false"
        show-icon
        style="margin-top: 8px;"
      />
      <template #footer>
        <el-button @click="batchEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchEditLoading" @click="handleBatchEditConfirm">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 全局预警线配置弹窗 -->
    <el-dialog v-model="warnConfigDialogVisible" title="全局预警线配置" width="420px" destroy-on-close>
      <div class="warn-config-content">
        <el-alert 
          title="修改全局预警线将影响所有药品的库存预警判断" 
          type="info" 
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />
        
        <div class="config-preview">
          <div class="preview-item">
            <span class="preview-label">当前预警线：</span>
            <span class="preview-value" style="color: #d97706; font-weight: 700;">
              {{ globalWarnThreshold }}
            </span>
          </div>
          <div class="preview-item">
            <span class="preview-label">受影响的药品数量：</span>
            <span class="preview-value">{{ totalCount }} 个</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">当前预警药品：</span>
            <span class="preview-value" :style="{ color: warningCount > 0 ? '#dc2626' : '#67C23A' }">
              {{ warningCount }} 个
            </span>
          </div>
        </div>

        <el-form label-width="140px">
          <el-form-item label="预警线数值">
            <el-input-number 
              v-model="warnConfigForm.threshold" 
              :min="0" 
              :max="9999"
              :step="1" 
              controls-position="right" 
              style="width: 100%" 
            />
            <div class="form-hint">当库存低于或等于该数值时触发预警</div>
          </el-form-item>
        </el-form>

        <div v-if="warnConfigForm.threshold !== globalWarnThreshold" class="preview-result">
          <el-divider>预览效果</el-divider>
          <div class="preview-item">
            <span class="preview-label">修改后预警药品：</span>
            <span class="preview-value" :style="{ color: previewWarningCount > 0 ? '#dc2626' : '#67C23A' }">
              {{ previewWarningCount }} 个
            </span>
          </div>
          <div v-if="previewWarningCount > 0" class="preview-drugs">
            <span class="preview-label">涉及药品：</span>
            <span class="preview-value" style="font-size: 13px; color: #6b7280;">
              {{ previewDrugNames.join('、') }}
            </span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="warnConfigDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          :loading="warnConfigLoading" 
          @click="handleWarnConfigConfirm"
          :disabled="warnConfigForm.threshold === globalWarnThreshold"
        >
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 预警列表弹窗 -->
    <el-dialog v-model="warnDialogVisible" title="库存预警通知" width="800px" destroy-on-close>
      <div class="warn-summary">
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="warn-stat-item">
              <span class="stat-number warn-all">{{ warnList.length }}</span>
              <span class="stat-label">总预警</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="warn-stat-item">
              <span class="stat-number warn-low">{{ warnList.filter((item: MedicineWarnVo) => item.status === 'LOW_STOCK').length }}</span>
              <span class="stat-label">库存不足</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="warn-stat-item">
              <span class="stat-number warn-danger">{{ warnList.filter((item: MedicineWarnVo) => item.status === 'OUT_OF_STOCK').length }}</span>
              <span class="stat-label">缺货</span>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="warn-tabs">
        <el-radio-group v-model="warnFilter" @change="handleWarnFilterChange" size="default">
          <el-radio-button value="all">全部预警 ({{ warnList.length }})</el-radio-button>
          <el-radio-button value="LOW_STOCK">库存不足 ({{ warnList.filter((item: MedicineWarnVo) => item.status === 'LOW_STOCK').length }})</el-radio-button>
          <el-radio-button value="OUT_OF_STOCK">缺货 ({{ warnList.filter((item: MedicineWarnVo) => item.status === 'OUT_OF_STOCK').length }})</el-radio-button>
        </el-radio-group>
      </div>

      <el-table 
        :data="filteredWarnList" 
        border 
        stripe 
        style="width: 100%; margin-top: 16px;"
        v-loading="warnLoading"
        :row-class-name="({ row }: { row: MedicineWarnVo }) => row.status === 'OUT_OF_STOCK' ? 'warn-danger-row' : 'warn-warning-row'"
      >
        <el-table-column prop="name" label="药品名称" min-width="130" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="stock" label="当前库存" width="110" align="center">
          <template #default="{ row }">
            <span class="warn-stock">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="预警线" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OUT_OF_STOCK' ? 'danger' : 'warning'" size="default" effect="dark">
              {{ row.status === 'OUT_OF_STOCK' ? '缺货' : '库存不足' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缺口" width="100" align="center">
          <template #default="{ row }">
            <span class="gap-number">{{ row.minStock - row.stock > 0 ? row.minStock - row.stock : 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="quickAddStock(row)">
              <el-icon><Plus /></el-icon>补货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="filteredWarnList.length === 0 && !warnLoading" class="empty-tip">
        <el-empty description="暂无符合条件的预警" />
      </div>
    </el-dialog>

    <!-- 操作日志 -->
    <div class="operation-toast" v-if="recentOperations.length > 0">
      <el-badge :value="recentOperations.length" type="primary">
        <el-button circle @click="showOperationLog">
          <el-icon><List /></el-icon>
        </el-button>
      </el-badge>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { 
  Plus, Search, Refresh, View, Edit, Delete, Minus, 
  Box, WarningFilled, CircleCloseFilled, Check, 
  TrendCharts, Timer, Warning, Bell, ArrowDown,
  PieChart, Setting, CircleClose, List
} from '@element-plus/icons-vue'
import {
  getMedicineList,
  addMedicine,
  updateMedicine,
  deleteMedicine,
  deductStock,
  addStock,
  getWarnings,
  getGlobalWarnThreshold,
  updateGlobalWarnThreshold
} from '@/api/admin/medicine'
import type { Medicine, MedicineDto, MedicineWarnVo } from '@/types/admin/adminMedicine'

// ============================================================
// 状态定义
// ============================================================

const loading = ref<boolean>(false)
const warnLoading = ref<boolean>(false)
const keyword = ref<string>('')
const statusFilter = ref<string>('')
const sortField = ref<string>('name_asc')
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const selectedRows = ref<Medicine[]>([])
const selectedIds = ref<Set<string>>(new Set())

const tableData = ref<Medicine[]>([])
const filteredData = ref<Medicine[]>([])

const totalCount = ref<number>(0)
const warningCount = ref<number>(0)
const outOfStockCount = ref<number>(0)
const normalCount = ref<number>(0)

const dialogVisible = ref<boolean>(false)
const dialogTitle = ref<string>('添加药品')
const isEdit = ref<boolean>(false)
const submitLoading = ref<boolean>(false)

const detailDialogVisible = ref<boolean>(false)
const detailData = ref<Medicine | null>(null)

const formRef = ref<FormInstance | null>(null)
const formData = reactive<MedicineDto>({
  name: '',
  spec: '',
  usage: '',
  indication: '',
  attention: '',
  stock: 0,
  price: 0,
  minStock: 10,
  reorderQuantity: 50
})

const deductDialogVisible = ref<boolean>(false)
const deductTarget = ref<Medicine | null>(null)
const deductQuantity = ref<number>(1)
const deductLoading = ref<boolean>(false)

const addStockDialogVisible = ref<boolean>(false)
const addStockTarget = ref<Medicine | null>(null)
const addStockQuantity = ref<number>(1)
const addStockLoading = ref<boolean>(false)

const warnDialogVisible = ref<boolean>(false)
const warnList = ref<MedicineWarnVo[]>([])
const warnFilter = ref<string>('all')

// 全局预警线
const globalWarnThreshold = ref<number>(10)
const warnConfigDialogVisible = ref<boolean>(false)
const warnConfigLoading = ref<boolean>(false)
const warnConfigForm = reactive<{
  threshold: number
}>({
  threshold: 10
})

// 批量编辑相关
const batchEditDialogVisible = ref<boolean>(false)
const batchEditLoading = ref<boolean>(false)
const batchFormRef = ref<FormInstance | null>(null)
const batchFormData = reactive<{
  price: number | null
  reorderQuantity: number | null
}>({
  price: null,
  reorderQuantity: null
})

const recentOperations = ref<Array<{ time: string; action: string; name: string }>>([])

// ============================================================
// 计算属性
// ============================================================

const filteredAndSortedData = computed(() => {
  let data = [...tableData.value]
  
  if (statusFilter.value) {
    data = data.filter((item: Medicine) => item.status === statusFilter.value)
  }
  
  const [field, order] = sortField.value.split('_')
  data.sort((a: Medicine, b: Medicine) => {
    let valA: any = a[field as keyof Medicine]
    let valB: any = b[field as keyof Medicine]
    
    if (field === 'name') {
      valA = (valA || '').toString().toLowerCase()
      valB = (valB || '').toString().toLowerCase()
      return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
    }
    
    if (typeof valA === 'string') {
      valA = valA.toLowerCase()
      valB = valB.toLowerCase()
      return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
    }
    
    return order === 'asc' ? (valA - valB) : (valB - valA)
  })
  
  filteredData.value = data
  return data.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value)
})

const filteredWarnList = computed(() => {
  if (warnFilter.value === 'all') {
    return warnList.value
  }
  return warnList.value.filter((item: MedicineWarnVo) => item.status === warnFilter.value)
})

// 预览预警数量
const previewWarningCount = computed(() => {
  if (!tableData.value.length) return 0
  const threshold = warnConfigForm.threshold
  return tableData.value.filter((item: Medicine) => {
    return item.stock <= threshold
  }).length
})

const previewDrugNames = computed(() => {
  if (!tableData.value.length) return []
  const threshold = warnConfigForm.threshold
  return tableData.value
    .filter((item: Medicine) => item.stock <= threshold)
    .map((item: Medicine) => item.name)
    .slice(0, 10)
})

// ============================================================
// 表单验证规则
// ============================================================
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入药品名称', trigger: 'blur' },
    { min: 1, max: 100, message: '药品名称长度在1-100个字符', trigger: 'blur' }
  ],
  spec: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存数量不能为负数', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    { type: 'number', min: 0, message: '单价不能为负数', trigger: 'blur' }
  ]
}

// ============================================================
// 方法
// ============================================================

function getStockPercentage(row: Medicine): number {
  if (row.stock === 0) return 0
  const max = row.minStock * 3 || 100
  return Math.min((row.stock / max) * 100, 100)
}

function getProgressColor(row: Medicine): string {
  if (row.stock === 0) return '#F56C6C'
  if (row.stock <= row.minStock) return '#E6A23C'
  return '#67C23A'
}

function getStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  const statusMap: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    NORMAL: 'success',
    LOW_STOCK: 'warning',
    OUT_OF_STOCK: 'danger'
  }
  return statusMap[status] || 'info'
}

function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    NORMAL: '正常',
    LOW_STOCK: '库存不足',
    OUT_OF_STOCK: '缺货'
  }
  return statusMap[status] || status
}

function getStockClass(row: Medicine): string {
  if (!row) return ''
  if (row.stock === 0) return 'stock-danger'
  if (row.stock <= (row.minStock || 10)) return 'stock-warning'
  return 'stock-normal'
}

function getRowClassName({ row }: { row: Medicine }): string {
  if (row.status === 'OUT_OF_STOCK') return 'row-out-of-stock'
  if (row.status === 'LOW_STOCK') return 'row-low-stock'
  return ''
}

function sortByPinyin(data: Medicine[]): Medicine[] {
  if (!data || data.length === 0) return []
  return [...data].sort((a: Medicine, b: Medicine) => {
    return a.name.localeCompare(b.name, 'zh-Hans-CN', { sensitivity: 'base' })
  })
}

async function fetchGlobalWarnThreshold(): Promise<void> {
  try {
    const res = await getGlobalWarnThreshold()
    if (res.code === 200 && res.data !== undefined) {
      globalWarnThreshold.value = res.data
    }
  } catch (error) {
    console.error('获取全局预警线失败:', error)
  }
}

async function fetchData(): Promise<void> {
  loading.value = true
  try {
    const res = await getMedicineList(keyword.value)
    if (res.code === 200) {
      tableData.value = sortByPinyin(res.data || [])
      
      // 恢复之前选中的行
      const selectedRowsFromIds = tableData.value.filter(
        (item: Medicine) => selectedIds.value.has(item.medicineId)
      )
      selectedRows.value = selectedRowsFromIds
      
      updateStatistics()
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

function updateStatistics(): void {
  totalCount.value = tableData.value.length
  
  const warningItems = tableData.value.filter(
    (item: Medicine) => item.status === 'LOW_STOCK' || item.status === 'OUT_OF_STOCK'
  )
  warningCount.value = warningItems.length
  
  outOfStockCount.value = tableData.value.filter(
    (item: Medicine) => item.status === 'OUT_OF_STOCK'
  ).length
  
  normalCount.value = tableData.value.filter(
    (item: Medicine) => item.status === 'NORMAL'
  ).length
}

async function fetchWarnings(): Promise<void> {
  warnLoading.value = true
  try {
    const res = await getWarnings()
    if (res.code === 200) {
      warnList.value = sortByPinyinWarn(res.data || [])
      warnFilter.value = 'all'
      warnDialogVisible.value = true
    } else {
      ElMessage.error(res.message || '获取预警失败')
    }
  } catch (error) {
    console.error('获取预警失败:', error)
    ElMessage.error('获取预警失败')
  } finally {
    warnLoading.value = false
  }
}

function sortByPinyinWarn(data: MedicineWarnVo[]): MedicineWarnVo[] {
  if (!data || data.length === 0) return []
  return [...data].sort((a: MedicineWarnVo, b: MedicineWarnVo) => {
    return a.name.localeCompare(b.name, 'zh-Hans-CN', { sensitivity: 'base' })
  })
}

async function handleWarningCardClick(type: 'warning' | 'danger'): Promise<void> {
  warnLoading.value = true
  try {
    const res = await getWarnings()
    if (res.code === 200) {
      warnList.value = sortByPinyinWarn(res.data || [])
      warnFilter.value = type === 'danger' ? 'OUT_OF_STOCK' : 'all'
      warnDialogVisible.value = true
    } else {
      ElMessage.error(res.message || '获取预警失败')
    }
  } catch (error) {
    console.error('获取预警失败:', error)
    ElMessage.error('获取预警失败')
  } finally {
    warnLoading.value = false
  }
}

function handleFilter(type: string): void {
  if (type === 'all') {
    statusFilter.value = ''
  } else if (type === 'normal') {
    statusFilter.value = 'NORMAL'
  }
}

function handleFilterChange(): void {
  currentPage.value = 1
}

function handleSortChange(): void {
  currentPage.value = 1
}

function handlePageChange(): void {}

function handleSizeChange(): void {
  currentPage.value = 1
}

function handleSelectionChange(rows: Medicine[]): void {
  selectedIds.value = new Set(rows.map(r => r.medicineId))
  selectedRows.value = rows
}

// ============================================================
// 全局预警线配置
// ============================================================

function openWarnConfigDialog(): void {
  warnConfigForm.threshold = globalWarnThreshold.value
  warnConfigDialogVisible.value = true
}

async function handleWarnConfigConfirm(): Promise<void> {
  if (warnConfigForm.threshold === globalWarnThreshold.value) {
    ElMessage.warning('预警线未发生变化')
    return
  }
  
  // 显示确认对话框
  try {
    const newCount = previewWarningCount.value
    let confirmMessage = `确定将全局预警线从 ${globalWarnThreshold.value} 修改为 ${warnConfigForm.threshold} 吗？\n`
    confirmMessage += `修改后将影响 ${totalCount.value} 个药品，其中 ${newCount} 个药品将处于预警状态。`
    
    await ElMessageBox.confirm(confirmMessage, '预警线修改确认', {
      type: 'warning',
      confirmButtonText: '确认修改',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  
  warnConfigLoading.value = true
  try {
    const res = await updateGlobalWarnThreshold(warnConfigForm.threshold)
    if (res.code === 200) {
      globalWarnThreshold.value = warnConfigForm.threshold
      warnConfigDialogVisible.value = false
      ElMessage.success(`全局预警线已修改为 ${globalWarnThreshold.value}`)
      await fetchData()
    } else {
      ElMessage.error(res.message || '修改预警线失败')
    }
  } catch (error) {
    console.error('修改预警线失败:', error)
    ElMessage.error('修改预警线失败')
  } finally {
    warnConfigLoading.value = false
  }
}

// ============================================================
// 批量操作
// ============================================================

function handleBatchCommand(command: string): void {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择药品')
    return
  }
  
  if (command === 'delete') {
    handleBatchDelete()
  } else if (command === 'edit') {
    batchFormData.price = null
    batchFormData.reorderQuantity = null
    batchEditDialogVisible.value = true
  }
}

async function handleBatchDelete(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个药品吗？此操作不可恢复！`,
      '批量删除确认',
      { type: 'warning' }
    )
    
    let successCount = 0
    let failCount = 0
    const names: string[] = []
    
    for (const row of selectedRows.value) {
      try {
        const res = await deleteMedicine(row.medicineId)
        if (res.code === 200) {
          successCount++
          names.push(row.name)
        } else {
          failCount++
        }
      } catch (e) {
        failCount++
        console.error(e)
      }
    }
    
    if (successCount > 0) {
      ElMessage.success(`成功删除 ${successCount} 个药品${failCount > 0 ? `，${failCount} 个失败` : ''}`)
      addOperationLog('批量删除', `${successCount} 个药品: ${names.join('、')}`)
    } else {
      ElMessage.error('批量删除失败')
    }
    selectedRows.value = []
    selectedIds.value = new Set()
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

async function handleBatchEditConfirm(): Promise<void> {
  const hasChanges = 
    batchFormData.price !== null ||
    batchFormData.reorderQuantity !== null
  
  if (!hasChanges) {
    ElMessage.warning('请至少修改一个字段')
    return
  }
  
  try {
    let confirmMessage = `确定要批量编辑 ${selectedRows.value.length} 个药品吗？\n`
    if (batchFormData.price !== null) confirmMessage += `• 单价改为: ${batchFormData.price}\n`
    if (batchFormData.reorderQuantity !== null) confirmMessage += `• 建议补货量改为: ${batchFormData.reorderQuantity}`
    
    await ElMessageBox.confirm(confirmMessage, '批量编辑确认', {
      type: 'info',
      confirmButtonText: '确认修改',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  
  batchEditLoading.value = true
  try {
    let successCount = 0
    let failCount = 0
    const names: string[] = []
    
    for (const row of selectedRows.value) {
      try {
        const updateData: MedicineDto = {
          medicineId: row.medicineId,
          name: row.name,
          spec: row.spec,
          usage: row.usage || '',
          indication: row.indication || '',
          attention: row.attention || '',
          stock: row.stock,
          price: batchFormData.price !== null ? batchFormData.price : row.price,
          reorderQuantity: batchFormData.reorderQuantity !== null ? batchFormData.reorderQuantity : row.reorderQuantity
        }
        
        const res = await updateMedicine(updateData)
        if (res.code === 200) {
          successCount++
          names.push(row.name)
        } else {
          failCount++
          console.error(`更新失败: ${row.name}`, res.message)
        }
      } catch (e) {
        failCount++
        console.error(`更新异常: ${row.name}`, e)
      }
    }
    
    if (successCount > 0) {
      const changes: string[] = []
      if (batchFormData.price !== null) changes.push(`单价=${batchFormData.price}`)
      if (batchFormData.reorderQuantity !== null) changes.push(`补货量=${batchFormData.reorderQuantity}`)
      
      ElMessage.success(`成功更新 ${successCount} 个药品${failCount > 0 ? `，${failCount} 个失败` : ''}`)
      addOperationLog('批量编辑', `${successCount} 个药品: ${changes.join('、')}`)
      batchEditDialogVisible.value = false
      
      selectedRows.value = []
      selectedIds.value = new Set()
      await fetchData()
    } else {
      ElMessage.error('批量更新失败')
    }
  } catch (error) {
    console.error('批量更新失败:', error)
    ElMessage.error('批量更新失败')
  } finally {
    batchEditLoading.value = false
  }
}

function handleView(row: Medicine): void {
  detailData.value = row
  detailDialogVisible.value = true
}

function handleAdd(): void {
  isEdit.value = false
  dialogTitle.value = '添加药品'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: Medicine): void {
  isEdit.value = true
  dialogTitle.value = '编辑药品'
  formData.medicineId = row.medicineId
  formData.name = row.name
  formData.spec = row.spec
  formData.usage = row.usage || ''
  formData.indication = row.indication || ''
  formData.attention = row.attention || ''
  formData.stock = row.stock
  formData.price = row.price
  formData.reorderQuantity = row.reorderQuantity || 50
  // minStock 使用全局预警线
  formData.minStock = globalWarnThreshold.value
  dialogVisible.value = true
}

function resetForm(): void {
  formData.medicineId = ''
  formData.name = ''
  formData.spec = ''
  formData.usage = ''
  formData.indication = ''
  formData.attention = ''
  formData.stock = 0
  formData.price = 0
  formData.minStock = globalWarnThreshold.value
  formData.reorderQuantity = 50
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateMedicine(formData)
    } else {
      res = await addMedicine(formData)
    }
    if (res.code === 200) {
      ElMessage.success(res.message || '操作成功')
      dialogVisible.value = false
      addOperationLog(isEdit.value ? '编辑' : '添加', formData.name)
      await fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: Medicine): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除药品 "${row.name}" 吗？`,
      '删除确认',
      { 
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
    const res = await deleteMedicine(row.medicineId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      addOperationLog('删除', row.name)
      // 从选中集合中移除
      selectedIds.value.delete(row.medicineId)
      await fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

function handleDeduct(row: Medicine): void {
  if (row.stock <= 0) {
    ElMessage.warning('该药品已无库存，无法扣除')
    return
  }
  deductTarget.value = row
  deductQuantity.value = 1
  deductDialogVisible.value = true
}

async function handleDeductConfirm(): Promise<void> {
  if (!deductTarget.value) return
  if (deductQuantity.value <= 0) {
    ElMessage.warning('请输入有效的扣除数量')
    return
  }
  if (deductQuantity.value > deductTarget.value.stock) {
    ElMessage.warning('库存不足')
    return
  }
  
  deductLoading.value = true
  try {
    const res = await deductStock(deductTarget.value.medicineId, deductQuantity.value)
    if (res.code === 200) {
      ElMessage.success('扣库存成功')
      deductDialogVisible.value = false
      addOperationLog('扣库存', `${deductTarget.value.name} -${deductQuantity.value}`)
      await fetchData()
    } else {
      ElMessage.error(res.message || '扣库存失败')
    }
  } catch (error) {
    console.error('扣库存失败:', error)
    ElMessage.error('扣库存失败')
  } finally {
    deductLoading.value = false
  }
}

function handleAddStock(row: Medicine): void {
  addStockTarget.value = row
  addStockQuantity.value = row.reorderQuantity || 50
  addStockDialogVisible.value = true
}

async function handleAddStockConfirm(): Promise<void> {
  if (!addStockTarget.value) return
  if (addStockQuantity.value <= 0) {
    ElMessage.warning('请输入有效的补货数量')
    return
  }
  
  addStockLoading.value = true
  try {
    const res = await addStock(addStockTarget.value.medicineId, addStockQuantity.value)
    if (res.code === 200) {
      ElMessage.success('补货成功')
      addStockDialogVisible.value = false
      addOperationLog('补货', `${addStockTarget.value.name} +${addStockQuantity.value}`)
      await fetchData()
    } else {
      ElMessage.error(res.message || '补货失败')
    }
  } catch (error) {
    console.error('补货失败:', error)
    ElMessage.error('补货失败')
  } finally {
    addStockLoading.value = false
  }
}

function quickAddStock(row: MedicineWarnVo): void {
  const fullMedicine = tableData.value.find((item: Medicine) => item.medicineId === row.medicineId)
  
  if (fullMedicine) {
    addStockTarget.value = fullMedicine
    addStockQuantity.value = fullMedicine.reorderQuantity || 50
  } else {
    const target: Medicine = {
      medicineId: row.medicineId,
      name: row.name,
      spec: row.spec,
      stock: row.stock,
      price: 0,
      usage: '',
      indication: '',
      attention: '',
      minStock: row.minStock,
      reorderQuantity: 50,
      status: row.status as Medicine['status'],
      createTime: ''
    }
    addStockTarget.value = target
    addStockQuantity.value = 50
  }
  
  warnDialogVisible.value = false
  addStockDialogVisible.value = true
}

function addOperationLog(action: string, name: string): void {
  const now = new Date()
  const time = now.toLocaleTimeString()
  recentOperations.value.unshift({ time, action, name })
  if (recentOperations.value.length > 10) {
    recentOperations.value.pop()
  }
}

function showOperationLog(): void {
  if (recentOperations.value.length === 0) {
    ElMessage.info('暂无操作记录')
    return
  }
  ElMessageBox.alert(
    recentOperations.value.map((op: { time: string; action: string; name: string }) => 
      `[${op.time}] ${op.action}：${op.name}`
    ).join('\n'),
    '最近操作记录',
    { confirmButtonText: '关闭' }
  )
}

function handleSearch(): void {
  fetchData()
}

function handleWarnFilterChange(): void {}

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await fetchGlobalWarnThreshold()
  await fetchData()
})
</script>

<style scoped>
/* ... 样式与之前相同，保持不变 ... */
.medicine-page {
  padding: 24px 32px;
  min-height: 100vh;
  background: #f0f4f8;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left .page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-tags {
  display: flex;
  gap: 6px;
}

.header-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: transparent;
  border-radius: 12px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  border: 1px solid #e5e7eb;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #409EFF;
  border-radius: 12px 12px 0 0;
}

.stat-card.warning::after {
  background: #d97706;
}

.stat-card.danger::after {
  background: #dc2626;
}

.stat-card.success::after {
  background: #10b981;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #d1d5db;
}

.stat-card .stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-card .stat-icon.total {
  background: #e8f4fd;
  color: #409EFF;
}
.stat-card .stat-icon.warn {
  background: #fef3e2;
  color: #d97706;
}
.stat-card .stat-icon.danger {
  background: #fee2e2;
  color: #dc2626;
}
.stat-card .stat-icon.normal {
  background: #d1fae5;
  color: #10b981;
}

.stat-card .stat-content {
  flex: 1;
}

.stat-card .stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.stat-card .stat-label {
  font-size: 14px;
  color: #94a3b8;
  margin-top: 2px;
}

.stat-card .stat-trend {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 20px;
  background: #f1f5f9;
}

.stat-card .stat-trend.up {
  color: #10b981;
}
.stat-card .stat-trend.down {
  color: #dc2626;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.drug-name-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: #0f172a;
  font-size: 14px;
}

.spec-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #475569;
}

.stock-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-indicator .el-progress {
  flex: 1;
  min-width: 50px;
  max-width: 80px;
}

.stock-indicator .stock-unit {
  font-size: 12px;
  color: #94a3b8;
}

.stock-danger {
  color: #dc2626;
  font-weight: 700;
}

.stock-warning {
  color: #d97706;
  font-weight: 600;
}

.stock-normal {
  color: #0f172a;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.row-out-of-stock) {
  background-color: #fef2f2 !important;
}
:deep(.row-low-stock) {
  background-color: #fffbeb !important;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  flex-wrap: nowrap;
}

.action-buttons .el-button {
  padding: 4px 6px;
  font-size: 13px;
}

.action-buttons .el-button .el-icon {
  margin-right: 2px;
  font-size: 14px;
}

.action-buttons .el-divider--vertical {
  margin: 0 2px;
  height: 16px;
}

.detail-content {
  padding: 8px 0;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-title h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.detail-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #6b7280;
}

.meta-item {
  display: inline-block;
}

.detail-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  background: #f8fafc;
}

.dialog-info {
  margin-bottom: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.info-row {
  display: flex;
  padding: 4px 0;
}

.info-label {
  width: 80px;
  color: #6b7280;
  flex-shrink: 0;
}

.info-value {
  font-weight: 500;
  color: #0f172a;
}

.form-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.warning-hint {
  margin-top: 12px;
}

.warn-summary {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.warn-stat-item {
  text-align: center;
}

.warn-stat-item .stat-number {
  font-size: 28px;
  font-weight: 700;
  display: block;
}

.warn-stat-item .stat-number.warn-all {
  color: #d97706;
}
.warn-stat-item .stat-number.warn-low {
  color: #f59e0b;
}
.warn-stat-item .stat-number.warn-danger {
  color: #dc2626;
}

.warn-stat-item .stat-label {
  font-size: 13px;
  color: #6b7280;
}

.warn-tabs {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.warn-stock {
  font-size: 18px;
  font-weight: 700;
  color: #dc2626;
}

.gap-number {
  font-weight: 600;
  color: #dc2626;
}

:deep(.warn-danger-row) {
  background-color: #fef2f2 !important;
}
:deep(.warn-warning-row) {
  background-color: #fffbeb !important;
}

.empty-tip {
  padding: 20px 0;
}

.operation-toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
}

/* 预警线配置弹窗样式 */
.warn-config-content {
  padding: 8px 0;
}

.config-preview {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.preview-item {
  display: flex;
  padding: 4px 0;
}

.preview-label {
  width: 140px;
  color: #6b7280;
  flex-shrink: 0;
}

.preview-value {
  font-weight: 500;
  color: #0f172a;
}

.preview-result {
  margin-top: 12px;
}

.preview-drugs {
  display: flex;
  padding: 4px 0;
  flex-wrap: wrap;
}

.preview-drugs .preview-label {
  width: 140px;
  color: #6b7280;
  flex-shrink: 0;
}

.preview-drugs .preview-value {
  flex: 1;
  word-break: break-all;
}

@media (max-width: 1400px) {
  .action-buttons .el-button {
    padding: 4px 4px;
    font-size: 12px;
  }
  .action-buttons .el-button .el-icon {
    margin-right: 1px;
    font-size: 12px;
  }
  .action-buttons .el-divider--vertical {
    margin: 0 1px;
    height: 14px;
  }
}

@media (max-width: 1200px) {
  .action-buttons .el-button {
    padding: 2px 3px;
    font-size: 11px;
  }
  .action-buttons .el-button .el-icon {
    margin-right: 0;
    font-size: 11px;
  }
  .action-buttons .el-divider--vertical {
    margin: 0 1px;
    height: 12px;
  }
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .toolbar-left {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left .el-input,
  .toolbar-left .el-select {
    width: 100% !important;
  }
}

@media (max-width: 768px) {
  .medicine-page {
    padding: 12px;
  }
  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .stat-card {
    padding: 12px 14px;
    gap: 10px;
  }
  .stat-card .stat-number {
    font-size: 20px;
  }
  .stat-card .stat-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  .header-left {
    flex-wrap: wrap;
  }
  .toolbar {
    padding: 12px;
  }
  .toolbar-left {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
  }
  .toolbar-left .el-input,
  .toolbar-left .el-select {
    width: 100% !important;
  }
  .toolbar-right {
    width: 100%;
    flex-wrap: wrap;
  }
  .toolbar-right .el-button {
    flex: 1;
  }
  .table-card {
    padding: 8px;
    overflow-x: auto;
  }
  .action-buttons .el-button {
    padding: 2px 4px;
    font-size: 11px;
  }
  .action-buttons .el-button .el-icon {
    font-size: 11px;
  }
  .warn-tabs {
    flex-direction: column;
    align-items: stretch;
  }
  .warn-tabs .el-radio-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .warn-tabs .el-radio-button {
    width: 100%;
  }
  .warn-tabs .el-radio-button .el-radio-button__inner {
    width: 100%;
    text-align: center;
  }
  .preview-item {
    flex-direction: column;
  }
  .preview-label {
    width: 100%;
  }
  .preview-drugs {
    flex-direction: column;
  }
  .preview-drugs .preview-label {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  .stat-card {
    padding: 10px 12px;
    gap: 8px;
  }
  .stat-card .stat-number {
    font-size: 18px;
  }
  .stat-card .stat-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
  .stat-card .stat-trend {
    font-size: 11px;
    padding: 1px 6px;
  }
}
</style>
<!-- src/pages/admin/scheduling/ScheduleManage.vue -->
<!-- src/pages/admin/scheduling/ScheduleManage.vue -->
<template>
  <div class="schedule-manage-page">
    <!-- ===== 页面头部 ===== -->
    <header class="page-header">
      <div class="header-left">
        <h2 class="page-title">排班管理</h2>
        <p class="page-subtitle">管理所有医生排班，支持新增、编辑、删除</p>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button size="default" @click="handlePrevWeek">
            <el-icon><ArrowLeft /></el-icon>
            上一周
          </el-button>
          <el-button size="default" @click="handleCurrentWeek" type="primary">本周</el-button>
          <el-button size="default" @click="handleNextWeek">
            下一周
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-button-group>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          style="width: 160px; margin-left: 12px"
        />
      </div>
    </header>

    <!-- ===== 筛选与操作栏 ===== -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          v-model="filterDoctorId"
          placeholder="选择医生筛选"
          clearable
          filterable
          @change="fetchSchedule"
          style="width: 200px"
        >
          <el-option
            v-for="doctor in doctorList"
            :key="doctor.doctorId"
            :label="doctor.name"
            :value="doctor.doctorId"
          />
        </el-select>
        <el-select
          v-model="filterDeptId"
          placeholder="选择科室筛选"
          clearable
          @change="fetchSchedule"
          style="width: 160px; margin-left: 8px"
        >
          <el-option
            v-for="dept in deptList"
            :key="dept.deptId"
            :label="dept.deptName"
            :value="dept.deptId"
          />
        </el-select>
        <span class="filter-result">共 {{ totalDoctors }} 位医生</span>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增排班
        </el-button>
        <el-button type="success" plain @click="handleAiSchedule">
          <el-icon><MagicStick /></el-icon> AI智能排班
        </el-button>
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- ===== 统计信息 ===== -->
    <div v-if="!loading && doctorScheduleData.length > 0" class="stats-bar">
      <span class="stats-range">{{ weekStartDisplay || '--' }} ~ {{ weekEndDisplay || '--' }}</span>
      <span class="stats-divider">|</span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #4f46e5" />
        总排班：{{ totalSchedules }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #f59e0b" />
        剩余号源：{{ totalRemain }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #10b981" />
        已预约：{{ totalBooked }}
      </span>
      <span class="stats-item">
        <span class="stats-dot" style="background: #ef4444" />
        号源已满：{{ fullSchedules }}
      </span>
    </div>

    <!-- ===== 周视图表格 ===== -->
    <div class="schedule-card" v-loading="loading">
      <div v-if="doctorScheduleData.length > 0" class="schedule-grid">
        <div class="grid-doctor-column">
          <div class="doctor-header">医生</div>
          <div
            v-for="doctor in doctorScheduleData"
            :key="doctor.doctorId"
            class="doctor-cell"
          >
            <div class="doctor-avatar">{{ getDoctorName(doctor.doctorId)?.charAt(0) || '?' }}</div>
            <div class="doctor-name">{{ getDoctorName(doctor.doctorId) || '未知医生' }}</div>
            <div class="doctor-dept">{{ getDeptName(doctor.deptId) || '' }}</div>
            <el-button size="small" type="primary" link @click="handleEditDoctor(doctor)" class="edit-btn">编辑</el-button>
          </div>
        </div>

        <div v-for="day in weekDays" :key="day.date" class="grid-day-column">
          <div class="day-header" :class="{ 'is-today': isToday(day.date) }">
            <div class="day-name">{{ day.dayName || '--' }}</div>
            <div class="day-date">{{ formatDateShort(day.date) }}</div>
          </div>
          <div class="day-body">
            <div v-for="doctor in doctorScheduleData" :key="doctor.doctorId" class="doctor-schedule-cell">
              <template v-if="getSchedulesForDoctorAndDay(doctor.doctorId, day.date).length > 0">
                <div
                  v-for="schedule in getSchedulesForDoctorAndDay(doctor.doctorId, day.date)"
                  :key="schedule.scheduleId"
                  class="schedule-block"
                  :class="{
                    'is-full': schedule.remainNum === 0,
                    'is-low': schedule.remainNum > 0 && schedule.remainNum <= 2,
                    'is-past': !canModifySchedule(schedule.workDate),
                    'is-disabled': schedule.status === 0,
                    'is-ai-generated': schedule.sourceType === 'AI_GENERATED'
                  }"
                  @click="handleScheduleClick(schedule)"
                >
                  <div class="schedule-time">{{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</div>
                  <div class="schedule-room"><el-icon><Location /></el-icon> {{ schedule.room || '未指定' }}</div>
                  <div class="schedule-remain">
                    <el-tag :type="getRemainTagType(schedule.remainNum, schedule.maxNum)" size="small">{{ schedule.remainNum }}/{{ schedule.maxNum }}</el-tag>
                  </div>
                  <div class="schedule-ai-tag" v-if="schedule.sourceType === 'AI_GENERATED'"><el-tag size="small" type="warning">AI</el-tag></div>
                  <div class="schedule-actions" v-if="schedule.status === 1 && canModifySchedule(schedule.workDate)">
                    <el-button size="small" type="primary" link @click.stop="handleEdit(schedule)">编辑</el-button>
                    <el-button size="small" type="danger" link @click.stop="handleDelete(schedule)">删除</el-button>
                  </div>
                  <div class="schedule-actions" v-else-if="schedule.status === 0 && canModifySchedule(schedule.workDate)">
                    <el-button size="small" type="success" link @click.stop="handleEnable(schedule)">启用</el-button>
                  </div>
                  <div class="schedule-past-tag" v-else-if="!canModifySchedule(schedule.workDate)"><el-tag size="small" type="danger">已过期</el-tag></div>
                  <div class="schedule-disabled-tag" v-else-if="schedule.status === 0"><el-tag size="small" type="info">已停用</el-tag></div>
                </div>
              </template>
              <div v-else class="add-schedule-btn" @click="handleAddForDoctorAndDay(doctor.doctorId, day.date)">
                <el-icon><Plus /></el-icon>
                <span>添加排班</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="本周暂无排班数据" />
    </div>

    <!-- ===== 编辑排班弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
      :append-to-body="true"
      :modal-append-to-body="true"
      :z-index="3000"
      top="8vh"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" class="schedule-form">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="formData.doctorId" placeholder="请选择医生" filterable @change="handleDoctorChange" style="width: 100%" teleported>
            <el-option v-for="doctor in doctorList" :key="doctor.doctorId" :label="doctor.name" :value="doctor.doctorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" prop="deptId">
          <el-select v-model="formData.deptId" placeholder="请选择科室" style="width: 100%" :disabled="!!formData.doctorId" teleported>
            <el-option v-for="dept in deptList" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="workDate">
          <el-date-picker v-model="formData.workDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="disabledPastDate" teleported />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="formData.startTime" placeholder="选择开始时间" value-format="HH:mm:ss" format="HH:mm" style="width: 100%" teleported />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="formData.endTime" placeholder="选择结束时间" value-format="HH:mm:ss" format="HH:mm" style="width: 100%" teleported />
        </el-form-item>
        <el-form-item label="诊室" prop="room">
          <el-input v-model="formData.room" placeholder="请输入诊室，如：门诊楼A101" />
        </el-form-item>
        <el-form-item label="最大号源" prop="maxNum">
          <el-input-number v-model="formData.maxNum" :min="1" :max="50" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="剩余号源" prop="remainNum" v-if="isEdit">
          <el-input-number v-model="formData.remainNum" :min="0" :max="formData.maxNum || 20" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="挂号费" prop="price">
          <el-input-number v-model="formData.price" :min="0" :precision="2" :step="5" style="width: 100%" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ isEdit ? '更新' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- ===== 排班详情弹窗 ===== -->
    <el-dialog v-model="detailVisible" title="排班详情" width="480px" destroy-on-close :append-to-body="true" :modal-append-to-body="true" top="10vh">
      <div v-if="selectedSchedule" class="detail-content">
        <div class="detail-row"><span class="detail-label">医生</span><span class="detail-value">{{ getDoctorName(selectedSchedule.doctorId) || selectedSchedule.doctorName || selectedSchedule.doctorId }}</span></div>
        <div class="detail-row"><span class="detail-label">科室</span><span class="detail-value">{{ getDeptName(selectedSchedule.deptId) || selectedSchedule.deptId }}</span></div>
        <div class="detail-row"><span class="detail-label">日期</span><span class="detail-value">{{ formatDateShort(selectedSchedule.workDate) }}</span></div>
        <div class="detail-row"><span class="detail-label">时段</span><span class="detail-value">{{ formatTime(selectedSchedule.startTime) }} - {{ formatTime(selectedSchedule.endTime) }}</span></div>
        <div class="detail-row"><span class="detail-label">诊室</span><span class="detail-value">{{ selectedSchedule.room || '未指定' }}</span></div>
        <div class="detail-row"><span class="detail-label">号源</span><span class="detail-value">总号 <strong>{{ selectedSchedule.maxNum }}</strong>，剩余 <strong :class="getRemainClass(selectedSchedule.remainNum)">{{ selectedSchedule.remainNum }}</strong></span></div>
        <div class="detail-row"><span class="detail-label">挂号费</span><span class="detail-value">¥{{ (selectedSchedule.price || 0).toFixed(2) }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value"><el-tag :type="selectedSchedule.status === 1 ? 'success' : 'danger'" size="small">{{ selectedSchedule.status === 1 ? '启用' : '停用' }}</el-tag></span></div>
        <div class="detail-row"><span class="detail-label">来源</span><span class="detail-value"><el-tag :type="selectedSchedule.sourceType === 'AI_GENERATED' ? 'warning' : 'info'" size="small">{{ selectedSchedule.sourceType === 'AI_GENERATED' ? '🤖 AI 排班' : '✏️ 人工创建' }}</el-tag></span></div>
        <div v-if="!canModifySchedule(selectedSchedule.workDate)" class="detail-row past-warning"><span class="detail-label">提示</span><span class="detail-value"><el-tag type="danger" size="default">⛔ 过去的排班不可修改</el-tag></span></div>
        <div class="detail-actions" v-if="canModifySchedule(selectedSchedule.workDate)">
          <el-button type="primary" @click="handleEdit(selectedSchedule); detailVisible = false">编辑排班</el-button>
          <el-button type="danger" @click="handleDelete(selectedSchedule); detailVisible = false">删除排班</el-button>
        </div>
        <div class="detail-actions" v-else><el-button @click="detailVisible = false">关闭</el-button></div>
      </div>
    </el-dialog>

    <!-- ===== AI智能排班弹窗 - 支持手动删除冲突项 ===== -->
    <el-dialog
      v-model="aiDialogVisible"
      title="🤖 AI智能排班"
      width="960px"
      destroy-on-close
      :append-to-body="true"
      :modal-append-to-body="true"
      top="3vh"
      @close="resetAiForm"
    >
      <div class="ai-schedule-content">
        <!-- 配置表单 -->
        <el-form ref="aiFormRef" :model="aiFormData" :rules="aiFormRules" label-width="120px" class="ai-schedule-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="科室" prop="deptId">
                <el-select v-model="aiFormData.deptId" placeholder="请选择科室" clearable filterable style="width: 100%" teleported @change="handleAiDeptChange">
                  <el-option v-for="dept in deptList" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="医生" prop="doctorId">
                <el-select v-model="aiFormData.doctorId" placeholder="请选择医生" filterable style="width: 100%" teleported @change="(val: string) => { const d = doctorList.find(item => item.doctorId === val); if (d) aiFormData.doctorName = d.name }">
                  <el-option v-for="doctor in aiFilteredDoctors" :key="doctor.doctorId" :label="doctor.name" :value="doctor.doctorId" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开始日期" prop="periodStart">
                <el-date-picker v-model="aiFormData.periodStart" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="disabledPastDate" teleported />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期" prop="periodEnd">
                <el-date-picker v-model="aiFormData.periodEnd" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="(time: Date) => disabledEndDate(time, aiFormData.periodStart)" teleported />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="时段设置" prop="timeWindows">
            <div class="time-slots-wrapper">
              <div v-for="(slot, index) in (aiFormData.timeWindows || [])" :key="index" class="time-slot-item">
                <el-time-picker v-model="slot.startTime" placeholder="开始" value-format="HH:mm:ss" format="HH:mm" size="small" style="width: 120px" teleported />
                <span class="time-slot-sep">至</span>
                <el-time-picker v-model="slot.endTime" placeholder="结束" value-format="HH:mm:ss" format="HH:mm" size="small" style="width: 120px" teleported />
                <el-button type="danger" size="small" link @click="removeTimeSlot(index)" :disabled="(aiFormData.timeWindows || []).length <= 1"><el-icon><Delete /></el-icon></el-button>
              </div>
              <el-button size="small" type="primary" plain @click="addTimeSlot"><el-icon><Plus /></el-icon> 添加时段</el-button>
            </div>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="每时段号源" prop="defaultMaxNum">
                <el-input-number v-model="aiFormData.defaultMaxNum" :min="1" :max="50" style="width: 100%" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="挂号费" prop="defaultPrice">
                <el-input-number v-model="aiFormData.defaultPrice" :min="0" :precision="2" :step="5" style="width: 100%" controls-position="right" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="诊室列表" prop="rooms">
            <div class="rooms-wrapper">
              <div v-for="(room, index) in (aiFormData.rooms || [])" :key="index" class="room-item">
                <el-input v-model="aiFormData.rooms![index]" placeholder="请输入诊室，如：门诊楼A101" size="small" style="width: 200px" />
                <el-button type="danger" size="small" link @click="removeRoom(index)" :disabled="(aiFormData.rooms || []).length <= 1"><el-icon><Delete /></el-icon></el-button>
              </div>
              <el-button size="small" type="primary" plain @click="addRoom"><el-icon><Plus /></el-icon> 添加诊室</el-button>
            </div>
            <div style="font-size: 12px; color: #94a3b8; margin-top: 4px;">💡 AI将自动检测诊室冲突，避免同一时段同一诊室被不同医生占用</div>
          </el-form-item>

          <el-form-item label="额外要求" prop="requirement">
            <el-input v-model="aiFormData.requirement" type="textarea" :rows="2" placeholder="请输入额外排班要求，如：一天上午值班一天下午值班，周六周日休息" maxlength="1000" show-word-limit />
          </el-form-item>
        </el-form>

        <!-- ===== 预览结果 - 支持手动删除冲突项 ===== -->
        <div v-if="aiPreviewResult" class="ai-preview-result">
          
          <!-- 统计头部 -->
          <div class="preview-header">
            <span class="preview-title">📊 生成结果</span>
            <el-tag type="success" size="large">共 {{ editableSchedules.length }} 条排班</el-tag>
            
            <el-tag v-if="conflictCount > 0" type="danger" size="large" effect="dark">
              ⚠️ {{ conflictCount }} 个冲突
            </el-tag>
            <el-tag v-else-if="editableSchedules.length > 0" type="success" size="large">✅ 无冲突</el-tag>

            <!-- 批量操作按钮 -->
            <div style="margin-left: auto; display: flex; gap: 8px;">
              <el-button 
                v-if="conflictCount > 0" 
                size="small" 
                type="danger" 
                plain 
                @click="removeAllConflicts"
              >
                <el-icon><Delete /></el-icon> 一键删除全部冲突项
              </el-button>
              <el-button 
                v-if="deletedCount > 0" 
                size="small" 
                type="warning" 
                plain 
                @click="restoreAllDeleted"
              >
                <el-icon><Refresh /></el-icon> 恢复已删除项
              </el-button>
            </div>
          </div>

          <!-- 排班列表 - 带删除按钮 -->
          <div class="preview-schedules" v-if="editableSchedules.length > 0">
            <el-alert
              v-if="conflictCount > 0"
               :title='`检测到 ${conflictCount} 个冲突项。您可以手动删除冲突项后再发布，或点击「一键删除全部冲突项」`'
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 12px;"
            />
            
            <el-table
              :data="editableSchedules"
              size="small"
              max-height="280"
              border
              style="width: 100%"
              :row-class-name="getEditableRowClassName"
            >
              <el-table-column label="序号" width="55" align="center">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="doctorName" label="医生" width="90" />
              <el-table-column prop="workDate" label="日期" width="110">
                <template #default="{ row }">{{ formatDateShort(row.workDate) }}</template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始" width="80" />
              <el-table-column prop="endTime" label="结束" width="80" />
              <el-table-column prop="room" label="诊室" width="120">
                <template #default="{ row }">
                  <span>{{ row.room || '未指定' }}</span>
                  <el-tag v-if="isRoomConflictType(row.conflictType)" type="danger" size="small" style="margin-left: 4px;">🏠 房间冲突</el-tag>
                  <el-tag v-else-if="isDoctorConflictType(row.conflictType)" type="danger" size="small" style="margin-left: 4px;">👨‍⚕️ 医生冲突</el-tag>
                  <el-tag v-else-if="row.conflict" type="warning" size="small" style="margin-left: 4px;">⚠️</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="maxNum" label="号源" width="60" />
              <el-table-column prop="price" label="挂号费" width="80">
                <template #default="{ row }">¥{{ row.price?.toFixed(2) || '0.00' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row._deleted" type="info" size="small">🗑️ 已删除</el-tag>
                  <el-tag v-else-if="isRoomConflictType(row.conflictType)" type="danger" size="small">🏠 冲突</el-tag>
                  <el-tag v-else-if="isDoctorConflictType(row.conflictType)" type="danger" size="small">👨‍⚕️ 冲突</el-tag>
                  <el-tag v-else-if="row.conflict" type="warning" size="small">⚠️ 冲突</el-tag>
                  <el-tag v-else type="success" size="small">✅ 正常</el-tag>
                </template>
              </el-table-column>
              
              <!-- ===== 操作列：删除/恢复 ===== -->
              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="!row._deleted"
                    size="small"
                    type="danger"
                    link
                    @click="removeScheduleItem(row)"
                  >
                    <el-icon><Delete /></el-icon> 删除
                  </el-button>
                  <el-button
                    v-else
                    size="small"
                    type="primary"
                    link
                    @click="restoreScheduleItem(row)"
                  >
                    <el-icon><Refresh /></el-icon> 恢复
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 已删除项统计 -->
          <div v-if="deletedCount > 0" class="deleted-summary">
            <el-alert
              :title="`已删除 ${deletedCount} 条排班，其中 ${deletedConflictCount} 条为冲突项。当前可发布 ${publishableCount} 条正常排班`"
              type="info"
              :closable="false"
              show-icon
            />
          </div>

          <!-- 无数据提示 -->
          <el-empty v-if="editableSchedules.length === 0" description="所有排班已删除，请重新生成" />

          <!-- 冲突详情（收起/展开） -->
          <el-collapse v-if="conflictCount > 0" style="margin-top: 12px;">
            <el-collapse-item title="📋 冲突详情 (点击展开)" name="conflicts">
              <div class="preview-conflicts">
                <div class="conflicts-title">
                  ⚠️ 以下为检测到的冲突项
                  <span style="font-size: 12px; color: #94a3b8; margin-left: 8px;">💡 建议删除冲突项后再发布，或手动调整后重新预览</span>
                </div>
                <el-table :data="groupedConflicts" size="small" max-height="200" border style="width: 100%" :row-class-name="getConflictRowClassName">
                  <el-table-column prop="doctorName" label="医生" width="90" />
                  <el-table-column prop="workDate" label="日期" width="110">
                    <template #default="{ row }">{{ formatDateShort(row.workDate) }}</template>
                  </el-table-column>
                  <el-table-column prop="startTime" label="开始" width="80" />
                  <el-table-column prop="endTime" label="结束" width="80" />
                  <el-table-column prop="conflictType" label="冲突类型" width="150">
                    <template #default="{ row }">
                      <el-tag :type="getConflictTagType(row.conflictType)" size="small" :effect="isRoomConflictType(row.conflictType) ? 'dark' : 'light'">
                        {{ getConflictIcon(row.conflictType) }} {{ getConflictTypeText(row.conflictType) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="conflictDetail" label="冲突详情" min-width="150">
                    <template #default="{ row }">
                      <span>{{ row.conflictDetail }}</span>
                      <span v-if="isRoomConflictType(row.conflictType)" style="color: #dc2626; font-weight: 500; margin-left: 6px;">🏠 请更换诊室或调整时间</span>
                      <span v-else-if="isDoctorConflictType(row.conflictType)" style="color: #dc2626; font-weight: 500; margin-left: 6px;">👨‍⚕️ 请调整医生排班时间</span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAiPreview" :loading="aiPreviewLoading" :disabled="aiPreviewLoading">
          <el-icon><View /></el-icon> 预览并检测冲突
        </el-button>
        <el-button 
          type="success" 
          @click="handleAiPublish" 
          :loading="aiPublishLoading" 
          :disabled="!canPublish"
        >
          <el-icon><Check /></el-icon> 
          发布排班 ({{ publishableCount }}条)
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Location, Plus, Refresh, MagicStick, View, Check, Delete
} from '@element-plus/icons-vue'
import {
  getWeeklySchedule, getAllWeeklySchedule, getScheduleDetail,
  createSchedule, updateSchedule, enableSchedule, deleteSchedule,
  type DoctorSchedule, type ScheduleSaveDto, type ScheduleUpdateDto
} from '@/api/admin/schedule'
import {
  previewAiSchedule, publishAiSchedule,
  isRoomConflict, isDoctorConflict,
  getConflictTypeText, getConflictTagType, getConflictIcon,
  type AiScheduleGenerateRequest, type AiScheduleGenerateResponse,
  type AiSchedulePublishRequest, type AiScheduleConflictItem,
  type AiScheduleItem, type ConflictType
} from '@/api/ai/aiSchedule'
import { getDoctorList } from '@/api/admin/doctor'
import { getDeptList } from '@/api/admin/dept'
import { formatDateShort, formatTime, isToday, getThisWeekStart, formatDate, getWeekDays, getWeekStart } from '@/utils/date'

// ============================================================
// 类型定义
// ============================================================
interface DoctorInfo { doctorId: string; name: string; deptId: string }
interface DeptInfo { deptId: string; deptName: string }
interface DoctorScheduleData extends DoctorInfo { schedules: Record<string, DoctorSchedule[]> }
interface WeekDay { date: string; dayName: string }

// 可编辑的排班项（扩展删除标记）
interface EditableScheduleItem extends AiScheduleItem {
  _deleted?: boolean      // 手动删除标记
  _originalIndex?: number // 原始索引
}

interface AiPreviewResponseItem {
  doctorId: string; doctorName: string; deptId: string
  workDate: string; startTime: string; endTime: string
  maxNum?: number; price?: number; room?: string
  conflict?: boolean; conflictType?: string | null; conflictReason?: string | null
}

interface AiPreviewResponseData {
  items?: AiPreviewResponseItem[]; summary?: string; status?: string
  fallback?: boolean; modelVersion?: string
  warnings?: string[]; optimizationReasons?: string[]
}

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const submitting = ref(false)
const selectedDate = ref(getThisWeekStart())
const filterDoctorId = ref('')
const filterDeptId = ref('')
const doctorList = ref<DoctorInfo[]>([])
const deptList = ref<DeptInfo[]>([])
const doctorScheduleData = ref<DoctorScheduleData[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const selectedSchedule = ref<DoctorSchedule | null>(null)

// AI排班状态
const aiDialogVisible = ref(false)
const aiPreviewLoading = ref(false)
const aiPublishLoading = ref(false)
const aiPreviewResult = ref<AiScheduleGenerateResponse | null>(null)

// ===== 核心：可编辑的排班列表 =====
const editableSchedules = ref<EditableScheduleItem[]>([])

const aiFormData = reactive<AiScheduleGenerateRequest>({
  doctorId: '', doctorName: '', deptId: '',
  periodStart: '', periodEnd: '', requirement: '',
  defaultMaxNum: 20, defaultPrice: 0,
  rooms: ['门诊楼A101', '门诊楼A102'],
  timeWindows: [
    { startTime: '08:00:00', endTime: '12:00:00' },
    { startTime: '14:00:00', endTime: '17:00:00' }
  ],
  unavailableDates: []
})

const aiFormRef = ref()
const aiFormRules = {
  periodStart: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  timeWindows: [{ required: true, message: '请至少设置一个时段', trigger: 'change' }]
}

const aiFilteredDoctors = computed(() => {
  if (!aiFormData.deptId) return doctorList.value
  return doctorList.value.filter(d => d.deptId === aiFormData.deptId)
})

const dialogTitle = computed(() => isEdit.value ? '编辑排班' : '新增排班')

const formData = reactive<ScheduleUpdateDto & ScheduleSaveDto & { remainNum?: number }>({
  scheduleId: '', doctorId: '', doctorName: '', deptId: '',
  workDate: '', startTime: '', endTime: '',
  maxNum: 20, remainNum: 20, price: 0, room: ''
})

const formRef = ref()
const formRules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  maxNum: [{ required: true, message: '请输入最大号源', trigger: 'blur' }]
}

// ============================================================
// 计算属性 - 可编辑列表统计
// ============================================================

// 冲突总数（仅未删除的）
const conflictCount = computed(() => {
  return editableSchedules.value.filter(s => !s._deleted && s.conflict).length
})

// 已删除数量
const deletedCount = computed(() => {
  return editableSchedules.value.filter(s => s._deleted).length
})

// 已删除的冲突项数量
const deletedConflictCount = computed(() => {
  return editableSchedules.value.filter(s => s._deleted && s.conflict).length
})

// 可发布数量（未删除且无冲突）
const publishableCount = computed(() => {
  return editableSchedules.value.filter(s => !s._deleted && !s.conflict).length
})

// 是否可以发布
const canPublish = computed(() => {
  return editableSchedules.value.length > 0 && publishableCount.value > 0 && !aiPublishLoading.value
})

// 冲突列表（用于冲突详情展示，仅未删除的冲突项）
const groupedConflicts = computed((): AiScheduleConflictItem[] => {
  const conflicts = editableSchedules.value
    .filter(s => !s._deleted && s.conflict)
    .map(s => ({
      doctorId: s.doctorId,
      doctorName: s.doctorName,
      workDate: s.workDate,
      startTime: s.startTime,
      endTime: s.endTime,
      conflictType: s.conflictType || 'TIME_CONFLICT',
      conflictDetail: s.conflictReason || '排班冲突'
    } as AiScheduleConflictItem))
  
  return conflicts.sort((a, b) => {
    const aIsRoom = isRoomConflict(a.conflictType)
    const bIsRoom = isRoomConflict(b.conflictType)
    if (aIsRoom && !bIsRoom) return -1
    if (!aIsRoom && bIsRoom) return 1
    return 0
  })
})

// ============================================================
// 计算属性 - 周视图
// ============================================================
const weekDays = computed<WeekDay[]>(() => {
  const weekStart = selectedDate.value
  const dateStrings = getWeekDays(weekStart)
  return dateStrings.map((date) => {
    const d = new Date(date)
    const dayOfWeek = d.getDay() === 0 ? 7 : d.getDay()
    const dayNames: Record<number, string> = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
    return { date, dayName: dayNames[dayOfWeek] || `周${['日','一','二','三','四','五','六'][d.getDay()]}` }
  })
})

const weekStartDisplay = computed<string>(() => {
  const days = weekDays.value
  if (days.length === 0) return ''
  return formatDateShort(days[0]?.date || '')
})
const weekEndDisplay = computed<string>(() => {
  const days = weekDays.value
  if (days.length === 0) return ''
  return formatDateShort(days[days.length - 1]?.date || '')
})

const totalSchedules = computed(() => doctorScheduleData.value.reduce((sum, d) => sum + Object.values(d.schedules || {}).reduce((s, l) => s + (l?.length || 0), 0), 0))
const totalRemain = computed(() => doctorScheduleData.value.reduce((sum, d) => sum + Object.values(d.schedules || {}).reduce((s, l) => s + (l?.reduce((t, i) => t + (i?.remainNum || 0), 0) || 0), 0), 0))
const totalBooked = computed(() => doctorScheduleData.value.reduce((sum, d) => sum + Object.values(d.schedules || {}).reduce((s, l) => s + (l?.reduce((t, i) => t + ((i?.maxNum || 0) - (i?.remainNum || 0)), 0) || 0), 0), 0))
const fullSchedules = computed(() => doctorScheduleData.value.reduce((sum, d) => sum + Object.values(d.schedules || {}).reduce((s, l) => s + (l?.filter(i => (i?.remainNum || 0) === 0).length || 0), 0), 0))
const totalDoctors = computed(() => doctorScheduleData.value.length)

// ============================================================
// 工具方法
// ============================================================
function isRoomConflictType(type?: string | null): boolean { return isRoomConflict(type) }
function isDoctorConflictType(type?: string | null): boolean { return isDoctorConflict(type) }

function getShortConflictText(type?: string | null): string {
  if (!type) return '冲突'
  const map: Record<string, string> = { 'DOCTOR_TIME': '医生冲突', 'ROOM_TIME': '房间冲突', 'BATCH_DOCTOR_TIME': '批量医生冲突', 'BATCH_ROOM_TIME': '批量房间冲突', 'TIME_CONFLICT': '时间冲突', 'SAME_DOCTOR': '医生重复', 'SAME_ROOM': '诊室占用' }
  return map[type] || '冲突'
}

function getEditableRowClassName({ row }: { row: EditableScheduleItem }): string {
  if (row._deleted) return 'preview-row-deleted'
  if (isRoomConflictType(row.conflictType)) return 'preview-row-room-conflict'
  if (row.conflict) return 'preview-row-conflict'
  return ''
}

function getConflictRowClassName({ row }: { row: AiScheduleConflictItem }): string {
  if (isRoomConflictType(row.conflictType)) return 'conflict-row-room'
  if (isDoctorConflictType(row.conflictType)) return 'conflict-row-doctor'
  return ''
}

function getDoctorName(doctorId: string): string {
  return doctorList.value.find(d => d.doctorId === doctorId)?.name || ''
}

function getDeptName(deptId: string): string {
  return deptList.value.find(d => d.deptId === deptId)?.deptName || ''
}

function canModifySchedule(workDate: string): boolean {
  if (!workDate) return false
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const date = new Date(workDate); date.setHours(0, 0, 0, 0)
  return date >= today
}

function disabledPastDate(time: Date): boolean {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  return time < today
}

function disabledEndDate(time: Date, startDate: string): boolean {
  if (!startDate) return false
  const start = new Date(startDate); start.setHours(0, 0, 0, 0)
  return time < start
}

function getSchedulesForDoctorAndDay(doctorId: string, date: string): DoctorSchedule[] {
  return doctorScheduleData.value.find(d => d.doctorId === doctorId)?.schedules?.[date] || []
}

function getRemainTagType(remain: number | undefined, max: number | undefined): 'danger' | 'warning' | 'success' | 'info' {
  const r = remain ?? 0; const m = max ?? 1
  if (r === 0) return 'danger'
  const ratio = r / m
  if (ratio < 0.2) return 'danger'; if (ratio < 0.4) return 'warning'; if (ratio < 0.7) return 'info'
  return 'success'
}

function getRemainClass(remain: number | undefined): string {
  const r = remain ?? 0
  if (r === 0) return 'remain-full'; if (r < 3) return 'remain-low'
  return 'remain-normal'
}

// ============================================================
// 数据获取
// ============================================================
async function fetchSchedule() {
  loading.value = true
  try {
    const weekStart = selectedDate.value
    
    let allDoctors = [...doctorList.value]
    
    if (filterDoctorId.value) {
      allDoctors = allDoctors.filter(d => d.doctorId === filterDoctorId.value)
    }
    if (filterDeptId.value) {
      allDoctors = allDoctors.filter(d => d.deptId === filterDeptId.value)
    }
    
    let schedulesData: Record<string, Record<string, DoctorSchedule[]>> = {}
    
    if (filterDoctorId.value) {
      const response = await getWeeklySchedule(filterDoctorId.value, weekStart)
      schedulesData = {
        [filterDoctorId.value]: response.data || {}
      }
    } else {
      const response = await getAllWeeklySchedule(weekStart)
      schedulesData = response.data || {}
    }
    
    doctorScheduleData.value = allDoctors.map(doctor => {
      const doctorSchedules = schedulesData[doctor.doctorId] || {}
      return {
        ...doctor,
        schedules: doctorSchedules
      }
    })
    
  } catch (error) {
    console.error('获取排班数据失败:', error)
    ElMessage.error('获取排班数据失败')
    doctorScheduleData.value = []
  } finally {
    loading.value = false
  }
}

async function fetchDoctorList() {
  try {
    const res = await getDoctorList()
    const list = res.data || []
    doctorList.value = list
      .filter((item: any) => item.status === 1)
      .map((item: any) => ({
        doctorId: item.doctorId,
        name: item.name,
        deptId: item.deptId || item.departmentId || ''
      }))
  } catch (error) {
    console.error('获取医生列表失败:', error)
    ElMessage.error('获取医生列表失败')
  }
}

async function fetchDeptList() {
  try {
    const res = await getDeptList()
    const list = res.data || []
    deptList.value = list.map((item: any) => ({
      deptId: item.deptId,
      deptName: item.deptName
    }))
  } catch (error) {
    console.error('获取科室列表失败:', error)
    ElMessage.error('获取科室列表失败')
  }
}

// ============================================================
// 排班CRUD操作
// ============================================================
async function handleEnable(schedule: DoctorSchedule) {
  try {
    await ElMessageBox.confirm(
      `确认启用排班吗？\n医生：${getDoctorName(schedule.doctorId) || schedule.doctorName}\n日期：${formatDateShort(schedule.workDate)}\n时段：${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      '启用确认',
      { type: 'info' }
    )
    await enableSchedule(schedule.scheduleId)
    ElMessage.success('启用成功')
    fetchSchedule()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('启用失败')
    }
  }
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleAddForDoctorAndDay(doctorId: string, date: string) {
  isEdit.value = false
  resetForm()
  formData.doctorId = doctorId
  formData.workDate = date
  const doctor = doctorList.value.find(d => d.doctorId === doctorId)
  if (doctor) {
    formData.doctorName = doctor.name
    formData.deptId = doctor.deptId
  }
  dialogVisible.value = true
}

async function handleEdit(schedule: DoctorSchedule) {
  isEdit.value = true
  try {
    const res = await getScheduleDetail(schedule.scheduleId)
    const data = res.data
    Object.assign(formData, {
      scheduleId: data.scheduleId,
      doctorId: data.doctorId,
      doctorName: data.doctorName || getDoctorName(data.doctorId),
      deptId: data.deptId,
      workDate: data.workDate,
      startTime: data.startTime,
      endTime: data.endTime,
      maxNum: data.maxNum,
      remainNum: data.remainNum,
      price: data.price || 0,
      room: data.room
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取排班详情失败')
  }
}

function handleEditDoctor(doctor: DoctorScheduleData) {
  ElMessage.info(`编辑医生: ${getDoctorName(doctor.doctorId)}`)
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate?.()
  
  submitting.value = true
  try {
    if (!formData.doctorId || !formData.deptId || !formData.workDate || 
        !formData.startTime || !formData.endTime || !formData.maxNum) {
      ElMessage.error('请完整填写表单')
      return
    }
    
    const doctor = doctorList.value.find(d => d.doctorId === formData.doctorId)
    if (doctor) {
      formData.doctorName = doctor.name
    }
    
    if (isEdit.value) {
      await updateSchedule(formData as ScheduleUpdateDto)
      ElMessage.success('更新排班成功')
    } else {
      await createSchedule(formData as ScheduleSaveDto)
      ElMessage.success('创建排班成功')
    }
    dialogVisible.value = false
    fetchSchedule()
  } catch (error: any) {
    ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(schedule: DoctorSchedule) {
  try {
    await ElMessageBox.confirm(
      `确认删除排班吗？\n医生：${getDoctorName(schedule.doctorId) || schedule.doctorName}\n日期：${formatDateShort(schedule.workDate)}\n时段：${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteSchedule(schedule.scheduleId)
    ElMessage.success('删除成功')
    fetchSchedule()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleScheduleClick(schedule: DoctorSchedule) {
  selectedSchedule.value = schedule
  detailVisible.value = true
}

function handleDoctorChange(doctorId: string) {
  const doctor = doctorList.value.find(d => d.doctorId === doctorId)
  if (doctor) {
    formData.doctorName = doctor.name
    formData.deptId = doctor.deptId
  }
}

function resetForm() {
  formData.scheduleId = ''
  formData.doctorId = ''
  formData.doctorName = ''
  formData.deptId = ''
  formData.workDate = ''
  formData.startTime = ''
  formData.endTime = ''
  formData.maxNum = 20
  formData.remainNum = 20
  formData.price = 0
  formData.room = ''
  formRef.value?.resetFields()
}

// ============================================================
// 周导航
// ============================================================

function handlePrevWeek() {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() - 7)
  selectedDate.value = formatDate(date)
  fetchSchedule()
}

function handleNextWeek() {
  const date = new Date(selectedDate.value)
  date.setDate(date.getDate() + 7)
  selectedDate.value = formatDate(date)
  fetchSchedule()
}

function handleCurrentWeek() {
  selectedDate.value = getThisWeekStart()
  fetchSchedule()
}

function handleDateChange(val: string) {
  if (val) {
    selectedDate.value = getWeekStart(val)
    fetchSchedule()
  }
}

function handleRefresh() {
  fetchSchedule()
}

// ============================================================
// AI排班方法
// ============================================================
function handleAiSchedule() {
  const today = new Date()
  const nextWeek = new Date(today); nextWeek.setDate(today.getDate() + 7)
  aiFormData.deptId = filterDeptId.value || ''
  aiFormData.doctorId = filterDoctorId.value || ''
  aiFormData.doctorName = filterDoctorId.value ? getDoctorName(filterDoctorId.value) : ''
  aiFormData.periodStart = formatDate(today)
  aiFormData.periodEnd = formatDate(nextWeek)
  aiFormData.requirement = ''
  aiFormData.defaultMaxNum = 20; aiFormData.defaultPrice = 0
  aiFormData.rooms = ['门诊楼A101', '门诊楼A102']
  aiFormData.timeWindows = [
    { startTime: '08:00:00', endTime: '12:00:00' },
    { startTime: '14:00:00', endTime: '17:00:00' }
  ]
  aiFormData.unavailableDates = []
  aiPreviewResult.value = null
  editableSchedules.value = []
  aiDialogVisible.value = true
}

function handleAiDeptChange() {
  aiFormData.doctorId = ''
  aiFormData.doctorName = ''
}

function addTimeSlot() {
  if (!aiFormData.timeWindows) {
    aiFormData.timeWindows = []
  }
  if (aiFormData.timeWindows.length < 5) {
    aiFormData.timeWindows.push({ startTime: '', endTime: '' })
  } else {
    ElMessage.warning('最多支持5个时段')
  }
}

function removeTimeSlot(index: number) {
  if (!aiFormData.timeWindows) {
    aiFormData.timeWindows = []
    return
  }
  if (aiFormData.timeWindows.length > 1) {
    aiFormData.timeWindows.splice(index, 1)
  } else {
    ElMessage.warning('至少保留一个时段')
  }
}

function addRoom() {
  if (!aiFormData.rooms) {
    aiFormData.rooms = []
  }
  if (aiFormData.rooms.length < 10) {
    aiFormData.rooms.push('')
  } else {
    ElMessage.warning('最多支持10个诊室')
  }
}

function removeRoom(index: number) {
  if (!aiFormData.rooms) {
    aiFormData.rooms = []
    return
  }
  if (aiFormData.rooms.length > 1) {
    aiFormData.rooms.splice(index, 1)
  }
}

// ============================================================
// 手动删除/恢复排班项
// ============================================================

/**
 * 删除单条排班项（标记为已删除）
 */
function removeScheduleItem(item: EditableScheduleItem) {
  item._deleted = true
  ElMessage.success('已标记为删除，发布时将跳过此项')
}

/**
 * 恢复单条排班项
 */
function restoreScheduleItem(item: EditableScheduleItem) {
  item._deleted = false
  ElMessage.success('已恢复此项')
}

/**
 * 一键删除所有冲突项
 */
function removeAllConflicts() {
  const conflictItems = editableSchedules.value.filter(s => !s._deleted && s.conflict)
  if (conflictItems.length === 0) {
    ElMessage.info('没有可删除的冲突项')
    return
  }
  
  ElMessageBox.confirm(
    `确认删除全部 ${conflictItems.length} 个冲突项吗？\n删除后这些排班将不会被发布。`,
    '批量删除冲突项',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  ).then(() => {
    conflictItems.forEach(item => { item._deleted = true })
    ElMessage.success(`已删除 ${conflictItems.length} 个冲突项`)
  }).catch(() => {})
}

/**
 * 一键恢复所有已删除项
 */
function restoreAllDeleted() {
  const deletedItems = editableSchedules.value.filter(s => s._deleted)
  if (deletedItems.length === 0) {
    ElMessage.info('没有已删除的项')
    return
  }
  
  ElMessageBox.confirm(
    `确认恢复全部 ${deletedItems.length} 个已删除项吗？\n恢复后冲突项仍需手动处理。`,
    '批量恢复',
    { type: 'info', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
  ).then(() => {
    deletedItems.forEach(item => { item._deleted = false })
    ElMessage.success(`已恢复 ${deletedItems.length} 项`)
  }).catch(() => {})
}

// ============================================================
// AI预览
// ============================================================
async function handleAiPreview() {
  if (!aiFormRef.value) return
  try { await aiFormRef.value.validate() } catch { return }

  if (!aiFormData.periodStart || !aiFormData.periodEnd) { ElMessage.error('请选择日期范围'); return }
  if (!aiFormData.doctorId) { ElMessage.error('请选择一位医生'); return }

  const doctor = doctorList.value.find(d => d.doctorId === aiFormData.doctorId)
  if (doctor) { aiFormData.doctorName = doctor.name } else { ElMessage.error('请选择有效的医生'); return }

  const validSlots = (aiFormData.timeWindows || [])
    .filter((s: any) => s?.startTime && s?.endTime)
    .map((s: any) => ({ startTime: s.startTime, endTime: s.endTime }))
  
  if (validSlots.length === 0) { ElMessage.error('请至少设置一个有效时段'); return }
  aiFormData.timeWindows = validSlots

  aiPreviewLoading.value = true
  aiPreviewResult.value = null
  editableSchedules.value = []
  
  try {
    const requestData: AiScheduleGenerateRequest = {
      doctorId: aiFormData.doctorId, doctorName: aiFormData.doctorName,
      deptId: aiFormData.deptId, periodStart: aiFormData.periodStart,
      periodEnd: aiFormData.periodEnd, requirement: aiFormData.requirement || '',
      defaultMaxNum: aiFormData.defaultMaxNum || 20,
      defaultPrice: aiFormData.defaultPrice || 0,
      rooms: (aiFormData.rooms || []).filter((r: string) => r && r.trim() !== ''),
      timeWindows: validSlots, unavailableDates: aiFormData.unavailableDates || []
    }
    
    const response = await previewAiSchedule(requestData)
    
    if (response?.data) {
      const responseData = response.data as AiPreviewResponseData
      const items = responseData.items || []
      
      // 转换为可编辑列表
      editableSchedules.value = items.map((item: AiPreviewResponseItem, index: number) => ({
        doctorId: item.doctorId,
        doctorName: item.doctorName,
        deptId: item.deptId,
        workDate: item.workDate,
        startTime: item.startTime,
        endTime: item.endTime,
        maxNum: item.maxNum || requestData.defaultMaxNum || 20,
        price: item.price || requestData.defaultPrice || 0,
        room: item.room || '未指定',
        conflict: item.conflict || false,
        conflictType: (item.conflictType as ConflictType) || null,
        conflictReason: item.conflictReason || null,
        _deleted: false,
        _originalIndex: index
      }))
      
      const totalConflicts = editableSchedules.value.filter(s => s.conflict).length
      
      if (items.length === 0) {
        ElMessage.warning('未生成任何排班，请检查配置条件')
      } else if (totalConflicts === 0) {
        ElMessage.success(`✅ 生成 ${items.length} 条排班，无冲突，可直接发布`)
      } else {
        ElMessage.warning(`⚠️ 生成 ${items.length} 条排班，其中 ${totalConflicts} 条有冲突。请手动删除冲突项或点击"一键删除全部冲突项"`)
      }
      
      // 保留原始响应用于其他信息展示
      aiPreviewResult.value = {
        generatedCount: items.length,
        schedules: editableSchedules.value.filter(s => !s._deleted),
        conflicts: editableSchedules.value.filter(s => !s._deleted && s.conflict).map(s => ({
          doctorId: s.doctorId, doctorName: s.doctorName,
          workDate: s.workDate, startTime: s.startTime, endTime: s.endTime,
          conflictType: s.conflictType || 'TIME_CONFLICT',
          conflictDetail: s.conflictReason || '排班冲突'
        } as AiScheduleConflictItem)),
        summary: responseData.summary, status: responseData.status,
        fallback: responseData.fallback, modelVersion: responseData.modelVersion,
        warnings: responseData.warnings, optimizationReasons: responseData.optimizationReasons
      }
    } else {
      ElMessage.error('AI排班预览返回数据格式异常')
    }
  } catch (error: any) {
    console.error('AI排班预览失败:', error)
    editableSchedules.value = []
    aiPreviewResult.value = null
    ElMessage.error(error.response?.data?.message || error.message || 'AI排班预览失败')
  } finally {
    aiPreviewLoading.value = false
  }
}

// ============================================================
// AI发布 - 只发布未删除且无冲突的项
// ============================================================
async function handleAiPublish() {
  // 获取待发布的排班（未删除且无冲突）
  const toPublish = editableSchedules.value.filter(s => !s._deleted && !s.conflict)
  
  if (toPublish.length === 0) {
    ElMessage.warning('没有可发布的排班。请检查是否所有正常项都被删除，或仍存在未解决的冲突。')
    return
  }

  const conflictCount = editableSchedules.value.filter(s => !s._deleted && s.conflict).length
  const deletedCount = editableSchedules.value.filter(s => s._deleted).length
  
  // 构建确认消息
  let confirmMessage = `即将发布 ${toPublish.length} 条排班`
  if (deletedCount > 0) {
    confirmMessage += `\n\n已排除 ${deletedCount} 条已删除项`
  }
  if (conflictCount > 0) {
    confirmMessage += `\n⚠️ 仍有 ${conflictCount} 条冲突项未被删除，这些项不会被发布`
  }
  confirmMessage += '\n\n确认发布吗？'

  try {
    await ElMessageBox.confirm(confirmMessage, '发布确认', {
      type: 'info',
      confirmButtonText: `确认发布 (${toPublish.length}条)`,
      cancelButtonText: '取消'
    })
  } catch { return }

  aiPublishLoading.value = true
  try {
    const requestData: AiSchedulePublishRequest = {
      items: toPublish.map(s => ({
        doctorId: s.doctorId, doctorName: s.doctorName, deptId: s.deptId,
        workDate: s.workDate, startTime: s.startTime, endTime: s.endTime,
        maxNum: s.maxNum, price: s.price, room: s.room
      }))
    }
    
    const response = await publishAiSchedule(requestData)
    
    if (response.data?.createdCount > 0) {
      ElMessage.success(`✅ 成功发布 ${response.data.createdCount} 条排班`)
    } else {
      ElMessage.warning('发布失败，请检查排班数据')
    }
    
    if (response.data?.warnings?.length) {
      response.data.warnings.forEach((w: string) => ElMessage.warning(w))
    }
    
    aiDialogVisible.value = false
    await fetchSchedule()
  } catch (error: any) {
    console.error('发布排班失败:', error)
    ElMessage.error(error.response?.data?.message || error.message || '发布失败')
  } finally {
    aiPublishLoading.value = false
  }
}

function resetAiForm() {
  aiPreviewResult.value = null
  editableSchedules.value = []
  aiPreviewLoading.value = false
  aiPublishLoading.value = false
  aiFormRef.value?.resetFields()
}

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await Promise.all([fetchDoctorList(), fetchDeptList()])
  await fetchSchedule()
})
</script>

<style scoped lang="scss">
.schedule-manage-page {
  padding: 24px 32px;
  min-height: 100vh;
  background: #f1f5f9;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  .page-title {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 2px 0;
  }
  .page-subtitle {
    font-size: 13px;
    color: #94a3b8;
    margin: 0;
  }
}

.header-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #ffffff;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  gap: 8px;

  .filter-left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .filter-result {
      font-size: 13px;
      color: #94a3b8;
      margin-left: 8px;
    }
  }

  .filter-right {
    display: flex;
    gap: 8px;
  }
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  background: #ffffff;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #475569;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  .stats-range {
    font-weight: 600;
    color: #0f172a;
  }

  .stats-divider {
    color: #e2e8f0;
  }

  .stats-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .stats-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
}

.schedule-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 16px 20px 20px;
  overflow: hidden;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 140px repeat(7, 1fr);
  gap: 2px;
  min-height: 460px;
  overflow-x: auto;
}

.grid-doctor-column {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 8px 0 0 8px;
  overflow: hidden;
  flex-shrink: 0;

  .doctor-header {
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: #94a3b8;
    background: #f1f5f9;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;
  }

  .doctor-cell {
    height: 80px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 10px;
    border-bottom: 1px solid #f1f5f9;
    flex-shrink: 0;

    .doctor-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #eef2ff;
      color: #4f46e5;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      flex-shrink: 0;
    }

    .doctor-name {
      font-size: 13px;
      font-weight: 500;
      color: #0f172a;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      min-width: 30px;
    }

    .doctor-dept {
      font-size: 11px;
      color: #94a3b8;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      flex: 1;
      text-align: left;
    }

    .edit-btn {
      font-size: 12px;
      opacity: 0;
      transition: opacity 0.2s;
      flex-shrink: 0;
    }

    &:hover .edit-btn {
      opacity: 1;
    }
  }
}

.grid-day-column {
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  min-width: 100px;

  .day-header {
    height: 44px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #f1f5f9;
    border-bottom: 2px solid #e2e8f0;
    padding: 4px 0;
    flex-shrink: 0;

    &.is-today {
      background: #eef2ff;
      border-bottom-color: #4f46e5;

      .day-name {
        color: #4f46e5;
      }
    }

    .day-name {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      line-height: 1.2;
    }

    .day-date {
      font-size: 11px;
      color: #94a3b8;
      line-height: 1.2;
    }
  }

  .day-body {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
}

.doctor-schedule-cell {
  height: 80px;
  padding: 4px;
  border-bottom: 1px solid #f1f5f9;
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.schedule-block {
  flex: 1;
  min-width: 60px;
  max-width: 100%;
  border-radius: 6px;
  padding: 4px 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #eef2ff;
  border-left: 3px solid #4f46e5;
  position: relative;

  &:hover {
    transform: scale(1.02);
    z-index: 10;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

    .schedule-actions {
      opacity: 1;
    }
  }

  &.is-full {
    background: #fef2f2;
    border-left-color: #ef4444;
  }

  &.is-low {
    background: #fffbeb;
    border-left-color: #f59e0b;
  }

  &.is-past {
    opacity: 0.7;
    background: #f1f5f9;
    border-left-color: #94a3b8;
    cursor: default;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }

  &.is-ai-generated {
    background: #fef3c7;
    border-left-color: #f59e0b;
    
    &::after {
      content: 'AI';
      position: absolute;
      top: -2px;
      right: -2px;
      font-size: 8px;
      font-weight: 700;
      color: #f59e0b;
      background: #fff8e7;
      padding: 0 4px;
      border-radius: 2px;
      border: 1px solid #fcd34d;
    }
  }

  .schedule-time {
    font-size: 11px;
    font-weight: 600;
    color: #1e293b;
    white-space: nowrap;
  }

  .schedule-room {
    font-size: 10px;
    color: #64748b;
    display: flex;
    align-items: center;
    gap: 2px;
    margin-top: 1px;
    white-space: nowrap;

    .el-icon {
      font-size: 11px;
    }
  }

  .schedule-remain {
    margin-top: 2px;
  }

  .schedule-ai-tag {
    position: absolute;
    top: 2px;
    right: 4px;
  }

  .schedule-actions {
    position: absolute;
    top: 2px;
    right: 4px;
    opacity: 0;
    transition: opacity 0.2s;
    display: flex;
    gap: 2px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 4px;
    padding: 0 4px;

    .el-button {
      font-size: 11px;
      padding: 0 4px;
    }
  }

  .schedule-past-tag {
    margin-top: 2px;
  }

  &.is-disabled {
    opacity: 0.5;
    background: #f1f5f9;
    border-left-color: #94a3b8;
    
    &:hover {
      transform: none;
      box-shadow: none;
    }
    
    .schedule-time {
      color: #94a3b8;
    }
    
    .schedule-room {
      color: #b0b8c4;
    }
  }

  .schedule-disabled-tag {
    margin-top: 2px;
  }
}

.add-schedule-btn {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  border: 2px dashed #e2e8f0;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    border-color: #4f46e5;
    color: #4f46e5;
    background: #f8fafc;
  }

  .el-icon {
    font-size: 18px;
    margin-bottom: 2px;
  }
}

.detail-content {
  padding: 4px 0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child {
    border-bottom: none;
  }

  .detail-label {
    color: #94a3b8;
    font-size: 14px;
  }

  .detail-value {
    color: #0f172a;
    font-size: 14px;
    text-align: right;
  }
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  justify-content: flex-end;
}

.remain-full {
  color: #dc2626;
  font-weight: 600;
}

.remain-low {
  color: #d97706;
  font-weight: 600;
}

.remain-normal {
  color: #0f172a;
  font-weight: 600;
}

// ============================================================
// AI排班弹窗样式
// ============================================================
.ai-schedule-content {
  .ai-schedule-form {
    .time-slots-wrapper {
      .time-slot-item {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .time-slot-sep {
          color: #94a3b8;
          font-size: 13px;
        }
      }
    }

    .rooms-wrapper {
      .room-item {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
      }
    }
  }

  .ai-preview-result {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 2px solid #f1f5f9;

    .preview-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      flex-wrap: wrap;

      .preview-title {
        font-size: 15px;
        font-weight: 600;
        color: #0f172a;
      }
    }

    .preview-schedules {
      margin-bottom: 12px;
    }

    .preview-conflicts {
      .conflicts-title {
        font-size: 14px;
        font-weight: 500;
        color: #dc2626;
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
      }
    }

    .preview-no-conflict {
      margin-top: 12px;
    }

    // ===== 新增：已删除统计提示 =====
    .deleted-summary {
      margin-top: 12px;
      padding: 0;
    }
  }
}

// ============================================================
// 预览表格行样式（冲突 + 已删除）
// ============================================================

// 房间冲突行 - 红色背景
:deep(.preview-row-room-conflict) {
  background-color: #fef2f2 !important;
  
  td {
    background-color: #fef2f2 !important;
  }
}

// 普通冲突行 - 黄色背景
:deep(.preview-row-conflict) {
  background-color: #fffbeb !important;
  
  td {
    background-color: #fffbeb !important;
  }
}

// ===== 新增：已删除行样式 - 灰色 + 删除线 =====
:deep(.preview-row-deleted) {
  background-color: #f9fafb !important;
  opacity: 0.5;
  
  td {
    background-color: #f9fafb !important;
    color: #9ca3af !important;
    text-decoration: line-through;
  }

  // 已删除行的按钮仍可点击
  .el-button {
    opacity: 1 !important;
    text-decoration: none !important;
  }
}

// ===== 新增：已删除行 hover 效果 =====
:deep(.preview-row-deleted:hover) {
  td {
    background-color: #f3f4f6 !important;
  }
}

// 冲突详情表格 - 房间冲突行
:deep(.conflict-row-room) {
  background-color: #fef2f2 !important;
  
  td {
    background-color: #fef2f2 !important;
  }
}

// 冲突详情表格 - 医生冲突行
:deep(.conflict-row-doctor) {
  background-color: #fefce8 !important;
  
  td {
    background-color: #fefce8 !important;
  }
}

// ============================================================
// AI 预览表格 - 固定操作列样式
// ============================================================
.ai-preview-result {
  .el-table {
    // 固定列阴影
    .el-table__fixed-right {
      box-shadow: -2px 0 8px rgba(0, 0, 0, 0.08);
    }

    // 操作列按钮样式
    .el-button--small.is-link {
      font-size: 12px;
      padding: 2px 6px;
      
      &.el-button--danger {
        color: #dc2626;
        &:hover {
          color: #b91c1c;
          background: #fef2f2;
        }
      }

      &.el-button--primary {
        color: #4f46e5;
        &:hover {
          color: #3730a3;
          background: #eef2ff;
        }
      }
    }
  }
}

// ============================================================
// 弹窗样式
// ============================================================
.schedule-form,
.ai-schedule-form {
  ::v-deep(.el-input__wrapper) {
    background-color: #ffffff !important;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
  }

  ::v-deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px #c0c4cc inset;
  }

  ::v-deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #409eff inset;
  }

  ::v-deep(.el-textarea__inner) {
    background-color: #ffffff !important;
  }

  ::v-deep(.el-input-number .el-input__wrapper) {
    background-color: #ffffff !important;
  }
}

::v-deep(.el-dialog) {
  border-radius: 12px;

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 10px 24px 20px;
  }
}

// ============================================================
// el-collapse 冲突详情折叠面板样式
// ============================================================
:deep(.el-collapse) {
  border: none;
  
  .el-collapse-item__header {
    font-size: 13px;
    font-weight: 500;
    color: #475569;
    border: none;
    padding: 8px 0;
    
    &:hover {
      color: #4f46e5;
    }
  }

  .el-collapse-item__wrap {
    border: none;
    background: transparent;
  }

  .el-collapse-item__content {
    padding: 0 0 8px 0;
  }
}

// ============================================================
// 响应式
// ============================================================
@media (max-width: 1200px) {
  .schedule-grid {
    grid-template-columns: 120px repeat(7, 160px);
  }
}

@media (max-width: 768px) {
  .schedule-manage-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-right {
    flex-wrap: wrap;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;

    .filter-left {
      flex-wrap: wrap;
    }

    .filter-right {
      justify-content: flex-end;
    }
  }

  .stats-bar {
    flex-wrap: wrap;
    gap: 8px 16px;
    font-size: 13px;
    padding: 10px 16px;
  }

  .schedule-grid {
    grid-template-columns: 90px repeat(7, 100px);
  }

  .grid-doctor-column .doctor-cell {
    height: 100px;
    flex-direction: column;
    padding: 6px;

    .doctor-dept {
      text-align: center;
    }
  }

  .doctor-schedule-cell {
    height: 100px;
  }

  .schedule-block {
    .schedule-time {
      font-size: 9px;
    }
    .schedule-room {
      font-size: 8px;
    }
    .schedule-actions {
      opacity: 1;
      position: static;
      background: transparent;
      padding: 0;
    }
  }

  .ai-schedule-content {
    .ai-schedule-form {
      .time-slots-wrapper {
        .time-slot-item {
          flex-wrap: wrap;
        }
      }
      .rooms-wrapper {
        .room-item {
          flex-wrap: wrap;
        }
      }
    }

    .ai-preview-result {
      .preview-header {
        flex-direction: column;
        align-items: flex-start;
        
        > div {
          width: 100%;
          justify-content: flex-start;
        }
      }
    }
  }
}
</style>

<style>
/* 确保所有 teleported 组件的弹出层在弹窗之上 */
.el-select-dropdown,
.el-picker-panel,
.el-time-panel,
.el-popper {
  z-index: 9999 !important;
}

.el-select-dropdown {
  background-color: #ffffff !important;
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;
}

.el-select-dropdown__item {
  background-color: #ffffff !important;
  color: #303133 !important;
}

.el-select-dropdown__item:hover {
  background-color: #f5f7fa !important;
}

.el-select-dropdown__item.is-selected {
  background-color: #ecf5ff !important;
  color: #409eff !important;
}
</style>
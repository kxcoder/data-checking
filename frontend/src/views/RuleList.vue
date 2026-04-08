<template>
  <div class="rule-list">
    <div class="header">
      <h1>规则管理</h1>
      <el-button type="primary" @click="goCreate">新增规则</el-button>
    </div>

    <el-table :data="rules" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="ruleName" label="规则名称" />
      <el-table-column prop="methodPattern" label="方法匹配" />
      <el-table-column prop="matchType" label="匹配类型" width="100">
        <template #default="{ row }">
          <el-tag :type="matchTypeTag(row.matchType)">{{ matchTypeText(row.matchType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ruleType" label="引擎类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.ruleType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="80" />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="goEdit(row.id)">编辑</el-button>
          <el-button size="small" type="warning" @click="goVersions(row.id)">版本</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ruleApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const rules = ref([])
const loading = ref(false)

const matchTypeText = (type) => {
  const map = { 0: '精确', 1: '通配', 2: '正则' }
  return map[type] || type
}

const matchTypeTag = (type) => {
  return type === 0 ? 'info' : type === 1 ? 'warning' : 'success'
}

const fetchRules = async () => {
  loading.value = true
  try {
    const { data } = await ruleApi.listAll()
    rules.value = data
  } catch (e) {
    ElMessage.error('加载规则失败')
  } finally {
    loading.value = false
  }
}

const goCreate = () => router.push('/rules/new')
const goEdit = (id) => router.push(`/rules/${id}/edit`)
const goVersions = (id) => router.push(`/rules/${id}/versions`)

const handleDelete = (id) => {
  ElMessageBox.confirm('确认删除该规则?', '警告', { type: 'warning' })
    .then(async () => {
      await ruleApi.delete(id)
      ElMessage.success('删除成功')
      fetchRules()
    })
    .catch(() => {})
}

onMounted(fetchRules)
</script>

<style scoped>
.rule-list {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
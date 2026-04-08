<template>
  <div class="rule-version">
    <div class="header">
      <h1>版本历史 - {{ ruleId }}</h1>
      <el-button @click="goBack">返回</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="版本列表" name="versions">
        <el-table :data="versions" v-loading="loading">
          <el-table-column prop="id" label="版本ID" width="100" />
          <el-table-column prop="ruleName" label="规则名称" />
          <el-table-column prop="methodPattern" label="方法匹配" />
          <el-table-column prop="expression" label="表达式" show-overflow-tooltip />
          <el-table-column prop="version" label="版本号" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="primary" :disabled="row.status === 'published'" @click="handlePublish(row)">发布</el-button>
              <el-button size="small" type="warning" :disabled="row.status === 'current'" @click="handleRollback(row)">回滚</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="发布管理" name="publish">
        <el-alert title="发布说明" type="info" :closable="false" style="margin-bottom: 20px">
          <p>• 每次保存规则会自动创建新版本</p>
          <p>• 只有"已发布"状态的版本才会被实际使用</p>
          <p>• 可以回滚到历史版本，系统会自动创建新版本</p>
        </el-alert>

        <el-card>
          <template #header>
            <span>当前生效版本</span>
          </template>
          <div v-if="currentVersion">
            <p><strong>版本号：</strong>{{ currentVersion.version }}</p>
            <p><strong>规则名称：</strong>{{ currentVersion.ruleName }}</p>
            <p><strong>表达式：</strong>{{ currentVersion.expression }}</p>
            <p><strong>发布时间：</strong>{{ currentVersion.publishedAt }}</p>
          </div>
          <el-empty v-else description="暂无已发布版本" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ruleApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const ruleId = route.params.id
const activeTab = ref('versions')

const versions = ref([])
const currentVersion = ref(null)
const loading = ref(false)

const statusTag = (status) => {
  return status === 'published' ? 'success' : status === 'current' ? 'warning' : 'info'
}

const statusText = (status) => {
  return status === 'published' ? '已发布' : status === 'current' ? '当前' : '历史'
}

const fetchVersions = async () => {
  loading.value = true
  try {
    const { data } = await ruleApi.getVersions(ruleId)
    versions.value = data.versions || []
    currentVersion.value = data.currentVersion
  } catch (e) {
    ElMessage.error('加载版本失败')
  } finally {
    loading.value = false
  }
}

const handlePublish = async (version) => {
  await ElMessageBox.confirm('确认发布该版本?', '提示', { type: 'info' })
  try {
    await ruleApi.publish(ruleId, version.id)
    ElMessage.success('发布成功')
    fetchVersions()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const handleRollback = async (version) => {
  await ElMessageBox.confirm('确认回滚到该版本? 将创建新版本记录', '警告', { type: 'warning' })
  try {
    await ruleApi.rollback(ruleId, version.id)
    ElMessage.success('回滚成功')
    fetchVersions()
  } catch (e) {
    ElMessage.error('回滚失败')
  }
}

const goBack = () => router.push('/rules')

onMounted(fetchVersions)
</script>

<style scoped>
.rule-version {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
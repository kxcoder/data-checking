<template>
  <div class="rule-editor">
    <div class="header">
      <h1>{{ isEdit ? '编辑规则' : '新增规则' }}</h1>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" style="max-width: 600px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="form.ruleName" />
      </el-form-item>
      <el-form-item label="方法匹配" prop="methodPattern">
        <el-input v-model="form.methodPattern" placeholder="如: orderService.createOrder" />
      </el-form-item>
      <el-form-item label="匹配类型" prop="matchType">
        <el-select v-model="form.matchType">
          <el-option :value="0" label="精确匹配" />
          <el-option :value="1" label="通配符匹配" />
          <el-option :value="2" label="正则匹配" />
        </el-select>
      </el-form-item>
      <el-form-item label="引擎类型" prop="ruleType">
        <el-select v-model="form.ruleType">
          <el-option value="SpEL" label="SpEL" />
          <el-option value="Groovy" label="Groovy" />
        </el-select>
      </el-form-item>
      <el-form-item label="核验表达式" prop="expression">
        <el-input v-model="form.expression" type="textarea" :rows="6" placeholder="#root.orderId != null" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="form.priority" :min="0" :max="100" />
      </el-form-item>
      <el-form-item label="是否启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ruleApi } from '../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  ruleName: '',
  methodPattern: '',
  matchType: 0,
  ruleType: 'SpEL',
  expression: '',
  priority: 0,
  enabled: true
})

const rules = {
  ruleName: [{ required: true, message: '请输入规则名称' }],
  methodPattern: [{ required: true, message: '请输入方法匹配' }],
  expression: [{ required: true, message: '请输入核验表达式' }]
}

const loadData = async () => {
  if (!isEdit.value) return
  try {
    const { data } = await ruleApi.getById(route.params.id)
    Object.assign(form, data)
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEdit.value) {
      await ruleApi.update(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      await ruleApi.create(form)
      ElMessage.success('创建成功')
    }
    router.push('/rules')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const goBack = () => router.push('/rules')

onMounted(loadData)
</script>

<style scoped>
.rule-editor {
  padding: 20px;
}
.header {
  margin-bottom: 20px;
}
</style>
import { createRouter, createWebHistory } from 'vue-router'
import RuleList from '../views/RuleList.vue'
import RuleEditor from '../views/RuleEditor.vue'
import RuleVersion from '../views/RuleVersion.vue'

const routes = [
  { path: '/', redirect: '/rules' },
  { path: '/rules', name: 'RuleList', component: RuleList },
  { path: '/rules/new', name: 'RuleNew', component: RuleEditor },
  { path: '/rules/:id/edit', name: 'RuleEdit', component: RuleEditor },
  { path: '/rules/:id/versions', name: 'RuleVersion', component: RuleVersion }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
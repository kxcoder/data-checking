import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/data-check',
  timeout: 10000
})

export const ruleApi = {
  listAll: () => api.get('/rules'),
  getById: (id) => api.get(`/rules/${id}`),
  create: (data) => api.post('/rules', data),
  update: (id, data) => api.put(`/rules/${id}`, data),
  delete: (id) => api.delete(`/rules/${id}`),
  getVersions: (id) => api.get(`/rules/${id}/versions`),
  publish: (id, versionId) => api.post(`/rules/${id}/publish/${versionId}`),
  rollback: (id, versionId) => api.post(`/rules/${id}/rollback/${versionId}`)
}
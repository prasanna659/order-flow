import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const api = axios.create({ baseURL: BASE_URL })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('orderflow_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const authApi = {
  register:         (payload) => api.post('/api/auth/register', payload),
  login:            (payload) => api.post('/api/auth/login', payload),
  forgotPassword:   (payload) => api.post('/api/auth/forgot-password', payload),
  validateResetToken: (token) => api.get('/api/auth/reset-password/validate', { params: { token } }),
  resetPassword:    (payload) => api.post('/api/auth/reset-password', payload),
}

export const inventoryApi = {
  listProducts: () => api.get('/api/inventory/products'),
}

export const orderApi = {
  placeOrder: (payload) => api.post('/api/orders', payload),
  getOrder: (id) => api.get(`/api/orders/${id}`),
  getOrdersForUser: (userId) => api.get(`/api/orders/user/${userId}`),
  getOrdersPaginated: (userId, page = 0, size = 10, status = null) => {
    const params = { userId, page, size }
    if (status) params.status = status
    return api.get('/api/orders', { params })
  },
}

export default api

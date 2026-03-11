import axios from 'axios'
import type { Result } from '@/types/api'

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

const client = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

client.interceptors.request.use((config) => {
  if (!config.headers['Content-Type']) {
    config.headers['Content-Type'] = 'application/json'
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code !== 200) {
      return Promise.reject(new ApiError(result.code, result.message))
    }
    return result.data as any
  },
  (error) => {
    if (error.response) {
      const msg = error.response.data?.message || error.message
      return Promise.reject(new ApiError(error.response.status, msg))
    }
    return Promise.reject(new ApiError(0, error.message || 'Network Error'))
  },
)

export default client

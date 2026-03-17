import apiClient from './axios'
import type { Business, BusinessStats, CreateBusinessRequest, UpdateBusinessRequest } from '@/lib/types/business'

const BASE = '/api/v1/businesses'

export const businessApi = {
  getMyBusinesses: () => apiClient.get<Business[]>(`${BASE}/my`).then((r) => r.data),
  getBusiness: (id: string) => apiClient.get<Business>(`${BASE}/${id}`).then((r) => r.data),
  createBusiness: (data: CreateBusinessRequest) => apiClient.post<Business>(BASE, data).then((r) => r.data),
  updateBusiness: (id: string, data: UpdateBusinessRequest) =>
    apiClient.put<Business>(`${BASE}/${id}`, data).then((r) => r.data),
  deleteBusiness: (id: string) => apiClient.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  getBusinessStats: (id: string) =>
    apiClient.get<BusinessStats>(`${BASE}/${id}/statistics`).then((r) => r.data),
}
import apiClient from './axios'
import type {
  Staff, StaffDetail, StaffInvitation,
  CreateStaffRequest, InviteStaffRequest, UpdateStaffRequest,
  Permission, AcceptInvitationRequest, StaffSalaryPayment, MarkSalaryPaidRequest,
} from '@/lib/types/staff'
import type { PageResponse } from '@/lib/types/business'

const sbase = (bid: string) => `/api/v1/businesses/${bid}/staff`

export const staffApi = {
  getStaff: (bid: string, params?: { page?: number; size?: number; search?: string; status?: string }) =>
    apiClient.get<PageResponse<Staff>>(sbase(bid), { params }).then((r) => r.data),

  getStaffList: (bid: string) =>
    apiClient.get<Staff[]>(`${sbase(bid)}/list`).then((r) => r.data),

  getStaffMember: (bid: string, staffId: string) =>
    apiClient.get<Staff>(`${sbase(bid)}/${staffId}`).then((r) => r.data),

  getStaffDetail: (bid: string, staffId: string) =>
    apiClient.get<StaffDetail>(`${sbase(bid)}/${staffId}/detail`).then((r) => r.data),

  createStaff: (bid: string, data: CreateStaffRequest) =>
    apiClient.post<Staff>(sbase(bid), data).then((r) => r.data),

  inviteStaff: (bid: string, data: InviteStaffRequest) =>
    apiClient.post<StaffInvitation>(`${sbase(bid)}/invite`, data).then((r) => r.data),

  updateStaff: (bid: string, staffId: string, data: UpdateStaffRequest) =>
    apiClient.put<Staff>(`${sbase(bid)}/${staffId}`, data).then((r) => r.data),

  terminateStaff: (bid: string, staffId: string, terminationDate?: string) =>
    apiClient.post<void>(
      `${sbase(bid)}/${staffId}/terminate`,
      null,
      terminationDate ? { params: { terminationDate } } : undefined
    ).then((r) => r.data),

  suspendStaff: (bid: string, staffId: string) =>
    apiClient.post<void>(`${sbase(bid)}/${staffId}/suspend`).then((r) => r.data),

  activateStaff: (bid: string, staffId: string) =>
    apiClient.post<void>(`${sbase(bid)}/${staffId}/activate`).then((r) => r.data),

  grantPermission: (bid: string, staffId: string, permission: Permission) =>
    apiClient.post<void>(`${sbase(bid)}/${staffId}/permissions/${permission}/grant`).then((r) => r.data),

  revokePermission: (bid: string, staffId: string, permission: Permission) =>
    apiClient.post<void>(`${sbase(bid)}/${staffId}/permissions/${permission}/revoke`).then((r) => r.data),

  getEffectivePermissions: (bid: string, staffId: string) =>
    apiClient.get<Permission[]>(`${sbase(bid)}/${staffId}/permissions`).then((r) => r.data),

  countActiveStaff: (bid: string) =>
    apiClient.get<number>(`${sbase(bid)}/count`).then((r) => r.data),

  getMonthlySalaryPayments: (bid: string, month?: string) =>
    apiClient
      .get<StaffSalaryPayment[]>(`${sbase(bid)}/salary-payments`, { params: month ? { month } : undefined })
      .then((r) => r.data),

  getUnpaidSalaryPayments: (bid: string, month?: string) =>
    apiClient
      .get<StaffSalaryPayment[]>(`${sbase(bid)}/salary-payments/unpaid`, { params: month ? { month } : undefined })
      .then((r) => r.data),

  markSalaryPaid: (bid: string, staffId: string, data: MarkSalaryPaidRequest) =>
    apiClient.post<StaffSalaryPayment>(`${sbase(bid)}/${staffId}/salary/mark-paid`, data).then((r) => r.data),

  searchStaff: (bid: string, query: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<Staff>>(`${sbase(bid)}/search`, { params: { query, ...params } }).then((r) => r.data),

  getInvitations: (bid: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<StaffInvitation>>(`${sbase(bid)}/invitations`, { params }).then((r) => r.data),

  getPendingInvitations: (bid: string) =>
    apiClient.get<StaffInvitation[]>(`${sbase(bid)}/invitations/pending`).then((r) => r.data),

  resendInvitation: (bid: string, invitationId: string) =>
    apiClient.post<void>(`${sbase(bid)}/invitations/${invitationId}/resend`).then((r) => r.data),

  cancelInvitation: (bid: string, invitationId: string) =>
    apiClient.delete<void>(`${sbase(bid)}/invitations/${invitationId}`).then((r) => r.data),

  // Public endpoints
  getInvitationByToken: (token: string) =>
    apiClient.get<StaffInvitation>('/api/v1/staff/invitation', { params: { token } }).then((r) => r.data),

  acceptInvitation: (data: AcceptInvitationRequest) =>
    apiClient.post<StaffInvitation>('/api/v1/staff/accept-invitation', data).then((r) => r.data),
}
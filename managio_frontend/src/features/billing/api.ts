"use client";

import { keepPreviousData, skipToken, useMutation, useQuery } from "@tanstack/react-query";
import { businessKey, useBusiness } from "@/features/business/context";
import { useRefreshCustomerData } from "@/features/customers/api";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { useSubmissionKey } from "@/lib/api/submission-key";

export type PaymentMethod = Schemas["PaymentBody"]["method"];
export type InvoiceState = Schemas["InvoiceRow"]["state"];
export type InvoiceView = "ALL" | "OPEN" | "OVERDUE" | "PAID" | "VOID";

export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CASH", label: "Cash" },
  { value: "UPI", label: "UPI" },
  { value: "CARD", label: "Card" },
  { value: "BANK_TRANSFER", label: "Bank transfer" },
  { value: "CHEQUE", label: "Cheque" },
  { value: "ONLINE", label: "Online" },
  { value: "OTHER", label: "Other" },
];

export const methodLabel = (method: PaymentMethod) => PAYMENT_METHODS.find((m) => m.value === method)?.label ?? method;

export const INVOICE_STATE: Record<InvoiceState, { label: string; tone: "ok" | "due" | "bad" | "neutral" | "info" }> = {
  PAID: { label: "Paid", tone: "ok" },
  PARTLY_PAID: { label: "Part paid", tone: "due" },
  DUE: { label: "Due", tone: "info" },
  OVERDUE: { label: "Overdue", tone: "bad" },
  VOID: { label: "Void", tone: "neutral" },
};

export function useInvoices(params: { view?: InvoiceView; customerId?: number; q?: string; page?: number; size?: number }, enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "invoices", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/invoices", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
    enabled,
  });
}

export function useInvoice(invoiceId: number | null) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "invoices", "one", invoiceId),
    queryFn:
      invoiceId == null
        ? skipToken
        : () => unwrap(api.GET("/api/v1/businesses/{businessId}/invoices/{invoiceId}", { params: { path: { businessId: id, invoiceId } } })),
  });
}

export function usePayments(params: { from?: string; to?: string; method?: PaymentMethod; customerId?: number; page?: number; size?: number }, enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "payments", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/payments", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
    enabled,
  });
}

export function useRecordPayment() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: ({ invoiceId, body }: { invoiceId: number; body: Schemas["PaymentBody"] }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/invoices/{invoiceId}/payments", { params: { path: { businessId: id, invoiceId }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return refresh();
    },
  });
}

export function useCreateInvoice() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: (body: Schemas["InvoiceBody"]) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/invoices", { params: { path: { businessId: id }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return refresh();
    },
  });
}

export function useSell() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: (body: Schemas["SaleBody"]) => unwrap(api.POST("/api/v1/businesses/{businessId}/sales", { params: { path: { businessId: id }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return refresh();
    },
  });
}

export function useVoidInvoice() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  return useMutation({
    mutationFn: ({ invoiceId, reason }: { invoiceId: number; reason: string }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/invoices/{invoiceId}/void", { params: { path: { businessId: id, invoiceId } }, body: { reason } })),
    onSuccess: refresh,
  });
}

export function useVoidPayment() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  return useMutation({
    mutationFn: ({ paymentId, reason }: { paymentId: number; reason: string }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/payments/{paymentId}/void", { params: { path: { businessId: id, paymentId } }, body: { reason } })),
    onSuccess: refresh,
  });
}

export function useRefund() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: ({ paymentId, body }: { paymentId: number; body: Schemas["RefundBody"] }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/payments/{paymentId}/refunds", { params: { path: { businessId: id, paymentId }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return refresh();
    },
  });
}

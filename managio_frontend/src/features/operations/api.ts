"use client";

import { keepPreviousData, skipToken, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessKey, useBusiness } from "@/features/business/context";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { useSubmissionKey } from "@/lib/api/submission-key";

export type Task = Schemas["TaskResponse"];
export type TaskStatus = Task["status"];
export type TaskPriority = Task["priority"];
export type ApprovalRequest = Schemas["RequestResponse"];
export type RequestType = ApprovalRequest["type"];
export type RequestStatus = ApprovalRequest["status"];

/* ── Tasks (TSK-1 to TSK-3) ───────────────────────────────────────────────────────────── */

export function useTasks(params: { view?: "OPEN" | "DONE" | "ALL"; mine?: boolean; assigneeId?: number; page?: number; size?: number }) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "tasks", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/tasks", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
  });
}

function useInvalidate(part: string) {
  const queryClient = useQueryClient();
  const { id } = useBusiness();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: businessKey(id, part) }),
      queryClient.invalidateQueries({ queryKey: businessKey(id, "today") }),
    ]);
}

export function useSaveTask(taskId?: number) {
  const { id } = useBusiness();
  const invalidate = useInvalidate("tasks");
  return useMutation({
    mutationFn: (body: Schemas["TaskBody"]) =>
      taskId == null
        ? unwrap(api.POST("/api/v1/businesses/{businessId}/tasks", { params: { path: { businessId: id } }, body }))
        : unwrap(api.PUT("/api/v1/businesses/{businessId}/tasks/{taskId}", { params: { path: { businessId: id, taskId } }, body })),
    onSuccess: invalidate,
  });
}

export function useSetTaskStatus() {
  const { id } = useBusiness();
  const invalidate = useInvalidate("tasks");
  return useMutation({
    mutationFn: ({ taskId, status }: { taskId: number; status: TaskStatus }) =>
      unwrap(api.PATCH("/api/v1/businesses/{businessId}/tasks/{taskId}/status", { params: { path: { businessId: id, taskId } }, body: { status } })),
    onSuccess: invalidate,
  });
}

export function useDeleteTask() {
  const { id } = useBusiness();
  const invalidate = useInvalidate("tasks");
  return useMutation({
    mutationFn: (taskId: number) => unwrap(api.DELETE("/api/v1/businesses/{businessId}/tasks/{taskId}", { params: { path: { businessId: id, taskId } } })),
    onSuccess: invalidate,
  });
}

export const PRIORITY: Record<TaskPriority, { label: string; tone: "ok" | "due" | "bad" | "neutral" | "info" }> = {
  URGENT: { label: "Urgent", tone: "bad" },
  HIGH: { label: "High", tone: "due" },
  NORMAL: { label: "Normal", tone: "neutral" },
  LOW: { label: "Low", tone: "neutral" },
};

/* ── Requests and approvals (REQ-1 to REQ-5) ──────────────────────────────────────────── */

export function useRequests(params: { status?: RequestStatus; mine?: boolean; page?: number; size?: number }) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "requests", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/requests", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
  });
}

export function useRequest(requestId: number | null) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "requests", "one", requestId),
    queryFn:
      requestId == null
        ? skipToken
        : () => unwrap(api.GET("/api/v1/businesses/{businessId}/requests/{requestId}", { params: { path: { businessId: id, requestId } } })),
  });
}

export function useCreateRequest() {
  const { id } = useBusiness();
  const invalidate = useInvalidate("requests");
  return useMutation({
    mutationFn: (body: Schemas["RequestPayload"]) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/requests", { params: { path: { businessId: id } }, body })),
    onSuccess: invalidate,
  });
}

export function useDecideRequest() {
  const { id } = useBusiness();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (
      input:
        | { requestId: number; decision: "approve"; note?: string; method?: Schemas["ApproveBody"]["method"] }
        | { requestId: number; decision: "reject"; note: string }
        | { requestId: number; decision: "cancel" },
    ) => {
      const path = { businessId: id, requestId: input.requestId };
      if (input.decision === "approve") {
        return unwrap(api.POST("/api/v1/businesses/{businessId}/requests/{requestId}/approve", { params: { path }, body: { note: input.note, method: input.method } }));
      }
      if (input.decision === "reject") {
        return unwrap(api.POST("/api/v1/businesses/{businessId}/requests/{requestId}/reject", { params: { path }, body: { note: input.note } }));
      }
      return unwrap(api.POST("/api/v1/businesses/{businessId}/requests/{requestId}/cancel", { params: { path } }));
    },
    onSuccess: () =>
      Promise.all(
        ["requests", "expenses", "invoices", "today", "customers", "day-book"].map((part) =>
          queryClient.invalidateQueries({ queryKey: businessKey(id, part) }),
        ),
      ),
  });
}

export const REQUEST_TYPE: Record<RequestType, string> = {
  EXPENSE: "Expense claim",
  PURCHASE: "Purchase",
  LEAVE: "Leave",
  DISCOUNT: "Discount",
  OTHER: "Other",
};

export const REQUEST_STATUS: Record<RequestStatus, { label: string; tone: "ok" | "due" | "bad" | "neutral" | "info" }> = {
  PENDING: { label: "Waiting", tone: "due" },
  APPROVED: { label: "Approved", tone: "ok" },
  REJECTED: { label: "Rejected", tone: "bad" },
  CANCELLED: { label: "Cancelled", tone: "neutral" },
};

/* ── Expenses (EXP-1 to EXP-4) ────────────────────────────────────────────────────────── */

export function useExpenses(params: { from?: string; to?: string; categoryId?: number; q?: string; includeVoid?: boolean; page?: number; size?: number }) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "expenses", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/expenses", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
  });
}

export function useExpenseSummary(from?: string, to?: string) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "expenses", "summary", from, to),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/expenses/summary", { params: { path: { businessId: id }, query: { from, to } } })),
  });
}

export function useExpenseCategories() {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "expense-categories"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/expense-categories", { params: { path: { businessId: id } } })),
  });
}

export function useSaveCategory(categoryId?: number) {
  const { id } = useBusiness();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: Schemas["CategoryBody"]) =>
      categoryId == null
        ? unwrap(api.POST("/api/v1/businesses/{businessId}/expense-categories", { params: { path: { businessId: id } }, body }))
        : unwrap(api.PATCH("/api/v1/businesses/{businessId}/expense-categories/{categoryId}", { params: { path: { businessId: id, categoryId } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(id, "expense-categories") }),
  });
}

function useInvalidateMoney() {
  const queryClient = useQueryClient();
  const { id } = useBusiness();
  return () =>
    Promise.all(["expenses", "today", "reports", "day-book", "payroll"].map((part) => queryClient.invalidateQueries({ queryKey: businessKey(id, part) })));
}

export function useRecordExpense() {
  const { id } = useBusiness();
  const invalidate = useInvalidateMoney();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: (body: Schemas["ExpenseBody"]) => unwrap(api.POST("/api/v1/businesses/{businessId}/expenses", { params: { path: { businessId: id }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return invalidate();
    },
  });
}

export function useVoidExpense() {
  const { id } = useBusiness();
  const invalidate = useInvalidateMoney();
  return useMutation({
    mutationFn: ({ expenseId, reason }: { expenseId: number; reason: string }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/expenses/{expenseId}/void", { params: { path: { businessId: id, expenseId } }, body: { reason } })),
    onSuccess: invalidate,
  });
}

/* ── Day book (DAY-1 to DAY-3) ────────────────────────────────────────────────────────── */

export function useDayBook(date?: string) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "day-book", "day", date ?? "today"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/day-book", { params: { path: { businessId: id }, query: { date } } })),
  });
}

export function useDayBookHistory(days = 30) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "day-book", "history", days),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/day-book/history", { params: { path: { businessId: id }, query: { days } } })),
  });
}

export function useCloseDay() {
  const { id } = useBusiness();
  const invalidate = useInvalidateMoney();
  return useMutation({
    mutationFn: (body: Schemas["CloseBody"]) => unwrap(api.POST("/api/v1/businesses/{businessId}/day-book/close", { params: { path: { businessId: id } }, body })),
    onSuccess: invalidate,
  });
}

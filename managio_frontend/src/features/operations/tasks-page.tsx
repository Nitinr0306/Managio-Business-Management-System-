"use client";

import { Check, Ellipsis, ListChecks, Plus, Repeat } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Field, FormError, Input, Select, Textarea } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton } from "@/components/ui/layout";
import { Menu, MenuItem, MenuSeparator, Segmented } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { useBusiness } from "@/features/business/context";
import { CustomerPicker, type PickedCustomer } from "@/features/customers/customer-picker";
import { useCustomerPanel } from "@/features/customers/panel-context";
import { useTeam } from "@/features/team/api";
import { useStaffSession } from "@/lib/auth/use-session";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { cn } from "@/lib/cn";
import { formatDate, humanize } from "@/lib/format";
import { PRIORITY, useDeleteTask, useSaveTask, useSetTaskStatus, useTasks, type Task, type TaskPriority, type TaskStatus } from "./api";

const PAGE_SIZE = 30;

/**
 * Tasks (TSK-1 to TSK-3). People with tasks.manage see and assign everyone's tasks; everyone else sees their
 * own. Finishing a repeating task brings back the next one.
 */
export function TasksPage() {
  const business = useBusiness();
  const params = useSearchParams();
  const router = useRouter();
  const manager = business.can("tasks.manage");
  const [scope, setScope] = useState<"mine" | "everyone">(manager ? "everyone" : "mine");
  const [view, setView] = useState<"OPEN" | "DONE" | "ALL">("OPEN");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Task | "new" | null>(null);
  const requested = params.get("new") === "1";
  const tasks = useTasks({ view, mine: scope === "mine", page, size: PAGE_SIZE });

  const closeDialog = () => {
    setEditing(null);
    if (requested) router.replace(`/app/${business.id}/tasks`);
  };

  return (
    <>
      <PageHeader title="Tasks" actions={<Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setEditing("new")}>New task</Button>} />
      <div className="mb-4 flex flex-wrap gap-3">
        {manager && (
          <Segmented label="Whose" value={scope} onChange={(v) => { setScope(v); setPage(0); }} options={[{ value: "everyone", label: "Everyone's" }, { value: "mine", label: "Mine" }]} />
        )}
        <Segmented label="Show" value={view} onChange={(v) => { setView(v); setPage(0); }} options={[{ value: "OPEN", label: "To do" }, { value: "DONE", label: "Done" }, { value: "ALL", label: "All" }]} />
      </div>
      <Panel>
        {tasks.isPending ? (
          <div className="space-y-2 p-5">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
        ) : tasks.isError ? (
          <EmptyState title="Tasks couldn't load" body={errorMessage(tasks.error)} />
        ) : tasks.data.items.length === 0 ? (
          <EmptyState icon={<ListChecks className="h-5 w-5" />} title={view === "OPEN" ? "Nothing to do" : "No tasks here"} body={view === "OPEN" ? "New tasks you're given will show up here." : undefined} />
        ) : (
          <>
            <ul className="divide-y divide-line">
              {tasks.data.items.map((task) => (
                <TaskRow key={task.id} task={task} onEdit={() => setEditing(task)} />
              ))}
            </ul>
            <Pagination page={page} size={PAGE_SIZE} total={tasks.data.totalItems} onPage={setPage} />
          </>
        )}
      </Panel>
      <Dialog open={editing != null || requested} onOpenChange={(o) => !o && closeDialog()} title={editing && editing !== "new" ? "Edit task" : "New task"}>
        <TaskForm task={editing && editing !== "new" ? editing : undefined} onDone={closeDialog} />
      </Dialog>
    </>
  );
}

function TaskRow({ task, onEdit }: { task: Task; onEdit: () => void }) {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const setStatus = useSetTaskStatus();
  const remove = useDeleteTask();
  const done = task.status === "DONE" || task.status === "CANCELLED";
  const customer = task.customer;
  const manager = business.can("tasks.manage");

  const change = async (status: TaskStatus) => {
    try {
      await setStatus.mutateAsync({ taskId: task.id, status });
      if (status === "DONE") toast.success(task.repeat !== "NONE" ? `Done. The next “${task.title}” is on the list.` : "Done");
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <li className="flex items-start gap-3 px-5 py-3">
      <button
        type="button"
        onClick={() => change(done ? "OPEN" : "DONE")}
        aria-label={done ? `Mark “${task.title}” as not done` : `Mark “${task.title}” as done`}
        disabled={setStatus.isPending}
        className={cn(
          "mt-0.5 grid h-5 w-5 shrink-0 place-items-center rounded-full border transition-colors",
          done ? "border-ok bg-ok text-white" : "border-line-strong hover:border-cobalt",
        )}
      >
        {done && <Check className="h-3.5 w-3.5" aria-hidden />}
      </button>
      <div className="min-w-0 flex-1">
        <p className={cn("text-[14px] font-medium", done ? "text-faint line-through" : "text-ink")}>
          {task.title}
          {task.status === "IN_PROGRESS" && <Pill tone="info" className="ml-2">In progress</Pill>}
          {task.status === "CANCELLED" && <Pill className="ml-2">Cancelled</Pill>}
        </p>
        {task.description && <p className="mt-0.5 text-[13px] text-muted">{task.description}</p>}
        <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12.5px] text-muted">
          {task.assignee && <span>{task.assignee.name}</span>}
          {task.dueOn && <span className={task.overdue ? "font-medium text-bad" : ""}>{task.overdue ? "Was due " : "Due "}{formatDate(task.dueOn)}</span>}
          {task.repeat !== "NONE" && (
            <span className="inline-flex items-center gap-1">
              <Repeat className="h-3.5 w-3.5" aria-hidden /> {humanize(task.repeat)}
            </span>
          )}
          {customer && (
            <button type="button" onClick={() => panel.open(customer.id)} className="font-medium text-cobalt hover:underline">
              {customer.name}
            </button>
          )}
        </p>
      </div>
      {task.priority !== "NORMAL" && <Pill tone={PRIORITY[task.priority].tone}>{PRIORITY[task.priority].label}</Pill>}
      <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${task.title}`}><Ellipsis className="h-4 w-4" /></Button>}>
        {!done && task.status !== "IN_PROGRESS" && <MenuItem onSelect={() => change("IN_PROGRESS")}>Start</MenuItem>}
        {!done && <MenuItem onSelect={() => change("DONE")}>Mark done</MenuItem>}
        {done && <MenuItem onSelect={() => change("OPEN")}>Reopen</MenuItem>}
        {!done && <MenuItem onSelect={() => change("CANCELLED")}>Cancel task</MenuItem>}
        {manager && (
          <>
            <MenuSeparator />
            <MenuItem onSelect={onEdit}>Edit</MenuItem>
            <MenuItem
              danger
              onSelect={async () => {
                try {
                  await remove.mutateAsync(task.id);
                  toast.success("Task deleted");
                } catch (error) {
                  toast.error(errorMessage(error));
                }
              }}
            >
              Delete
            </MenuItem>
          </>
        )}
      </Menu>
    </li>
  );
}

function TaskForm({ task, onDone }: { task?: Task; onDone: () => void }) {
  const business = useBusiness();
  const manager = business.can("tasks.manage");
  const pickAssignee = manager && business.can("team.view");
  const team = useTeam(false, pickAssignee);
  const session = useStaffSession();
  const save = useSaveTask(task?.id);
  const [values, setValues] = useState({
    title: task?.title ?? "",
    description: task?.description ?? "",
    assigneeId: task?.assignee ? String(task.assignee.id) : "",
    dueOn: task?.dueOn ?? "",
    priority: (task?.priority ?? "NORMAL") as TaskPriority,
    repeat: task?.repeat ?? "NONE",
  });
  const [customer, setCustomer] = useState<PickedCustomer | null>(task?.customer ? { id: task.customer.id, name: task.customer.name } : null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const set = (key: keyof typeof values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [key]: e.target.value }));

  // New tasks default to the person creating them; the backend leaves a task unassigned when no one is chosen.
  const chosenAssignee = pickAssignee
    ? values.assigneeId || (task ? "" : String(team.data?.find((m) => m.userId === session.principal?.id)?.id ?? ""))
    : "";

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    if (!values.title.trim()) {
      setErrors({ title: "Enter what needs doing" });
      return;
    }
    setErrors({});
    try {
      await save.mutateAsync({
        title: values.title.trim(),
        description: values.description.trim() || undefined,
        assigneeId: chosenAssignee ? Number(chosenAssignee) : undefined,
        dueOn: values.dueOn || undefined,
        priority: values.priority,
        repeat: values.repeat as Task["repeat"],
        customerId: customer?.id,
      });
      toast.success(task ? "Task saved" : "Task added");
      onDone();
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors);
      else setFormError(errorMessage(error));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={formError} />
        <Field label="What needs doing" error={errors.title}>
          <Input autoFocus value={values.title} onChange={set("title")} placeholder="Call members whose plans ended last week" />
        </Field>
        <Field label="Details" optional>
          <Textarea rows={2} value={values.description} onChange={set("description")} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          {manager && business.can("team.view") ? (
            <Field label="Assign to" error={errors.assigneeId}>
              <Select
                value={chosenAssignee}
                onChange={set("assigneeId")}
                disabled={team.isPending}
              >
                <option value="">Unassigned</option>
                {(team.data ?? []).filter((m) => m.status === "ACTIVE").map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.fullName}
                    {m.userId === session.principal?.id ? " (you)" : m.title ? ` · ${m.title}` : ""}
                  </option>
                ))}
              </Select>
            </Field>
          ) : (
            <p className="self-end text-[13px] text-muted">This task will be yours.</p>
          )}
          <Field label="Due" optional>
            <Input type="date" value={values.dueOn} onChange={set("dueOn")} />
          </Field>
          <Field label="Priority">
            <Select value={values.priority} onChange={set("priority")}>
              <option value="LOW">Low</option>
              <option value="NORMAL">Normal</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </Select>
          </Field>
          <Field label="Repeats" hint={values.repeat !== "NONE" ? "The next one appears when this is done" : undefined}>
            <Select value={values.repeat} onChange={set("repeat")}>
              <option value="NONE">Doesn&apos;t repeat</option>
              <option value="DAILY">Every day</option>
              <option value="WEEKLY">Every week</option>
              <option value="MONTHLY">Every month</option>
            </Select>
          </Field>
        </div>
        {business.can("customers.view") && (
          <Field label={`About a ${business.labels.customer.toLowerCase()}`} optional>
            <CustomerPicker value={customer} onChange={setCustomer} />
          </Field>
        )}
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={save.isPending}>{task ? "Save" : "Add task"}</Button>
      </DialogFooter>
    </form>
  );
}

"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarCheck, Ellipsis, Plus, UsersRound, X } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Checkbox, Field, FormError, Input, Textarea } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton } from "@/components/ui/layout";
import { Menu, MenuItem, Tabs } from "@/components/ui/menu";
import { Pill } from "@/components/ui/pill";
import { LoadingBlock } from "@/components/ui/spinner";
import { businessKey, useBusiness } from "@/features/business/context";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatDate, formatTime, localToday, plural } from "@/lib/format";
import { useCustomers } from "./api";
import { CustomerPicker, type PickedCustomer } from "./customer-picker";
import { useCustomerPanel } from "./panel-context";

/** Attendance and groups (batches, classes): check people in at the desk, or take a roll call for a group. */
export function AttendancePage() {
  const business = useBusiness();
  const tabs = [
    ...(business.has("ATTENDANCE") ? [{ value: "today", label: "Check-ins" }] : []),
    ...(business.has("GROUPS") ? [{ value: "groups", label: business.labels.groups }] : []),
  ];
  const [tab, setTab] = useState(tabs[0]?.value ?? "today");
  return (
    <>
      <PageHeader title={business.has("GROUPS") ? `${business.labels.groups} and attendance` : "Attendance"} />
      {tabs.length > 1 && <Tabs value={tab} onValueChange={setTab} items={tabs} />}
      <div className="mt-4">{tab === "today" ? <CheckIns /> : <Groups />}</div>
    </>
  );
}

function useAttendance(date: string, groupId?: number) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "attendance", date, groupId ?? "all"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/attendance", { params: { path: { businessId: id }, query: { date, groupId } } })),
  });
}

function CheckIns() {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(localToday());
  const [customer, setCustomer] = useState<PickedCustomer | null>(null);
  const attendance = useAttendance(date);
  const canMark = business.can("attendance.mark");
  const invalidate = () => queryClient.invalidateQueries({ queryKey: businessKey(business.id, "attendance") });

  const checkIn = useMutation({
    mutationFn: (customerId: number) => unwrap(api.POST("/api/v1/businesses/{businessId}/attendance", { params: { path: { businessId: business.id } }, body: { customerId } })),
    onSuccess: invalidate,
  });
  const undo = useMutation({
    mutationFn: (attendanceId: number) =>
      unwrap(api.DELETE("/api/v1/businesses/{businessId}/attendance/{attendanceId}", { params: { path: { businessId: business.id, attendanceId } } })),
    onSuccess: invalidate,
    onError: (error) => toast.error(errorMessage(error)),
  });

  const submit = async (picked: PickedCustomer | null) => {
    setCustomer(picked);
    if (!picked) return;
    try {
      const result = await checkIn.mutateAsync(picked.id);
      if (result.alreadyCheckedIn) toast.info(`${picked.name} was already checked in today`);
      else if (!result.covered) toast.warning(`${picked.name} is checked in, but has no active ${business.labels.plan.toLowerCase()}`, { action: { label: "Open", onClick: () => panel.open(picked.id) } });
      else toast.success(`${picked.name} checked in`);
      setCustomer(null);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const items = attendance.data ?? [];
  return (
    <div className="grid gap-5 lg:grid-cols-[1fr_1.4fr]">
      {canMark && date === localToday() && (
        <Panel title="Check someone in" bodyClassName="px-5 py-4 space-y-2">
          <CustomerPicker value={customer} onChange={submit} placeholder={`Type a ${business.labels.customer.toLowerCase()}'s name, phone or #number`} />
          <p className="text-[12.5px] text-muted">Picking someone checks them in straight away. Anyone without an active {business.labels.plan.toLowerCase()} is flagged.</p>
        </Panel>
      )}
      <Panel
        title={date === localToday() ? `In today · ${items.length}` : `${formatDate(date)} · ${items.length}`}
        actions={<Input type="date" max={localToday()} value={date} onChange={(e) => setDate(e.target.value || localToday())} className="w-[160px]" aria-label="Day" />}
        className={canMark && date === localToday() ? undefined : "lg:col-span-2"}
      >
        {attendance.isPending ? (
          <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-8 w-full" />)}</div>
        ) : items.length === 0 ? (
          <EmptyState icon={<CalendarCheck className="h-5 w-5" />} title="Nobody checked in" />
        ) : (
          <ul className="divide-y divide-line">
            {items.map((c: Schemas["CheckIn"]) => (
              <li key={c.id} className="flex items-center gap-3 px-5 py-2.5 text-[13.5px]">
                <span className="w-16 tabular-nums text-faint">{formatTime(c.checkedInAt)}</span>
                <button type="button" className="min-w-0 flex-1 truncate text-left font-medium hover:text-cobalt" onClick={() => panel.open(c.customerId)}>
                  {c.customerName} <span className="text-[12px] font-normal tabular-nums text-faint">#{c.customerNumber}</span>
                </button>
                {!c.covered && <Pill tone="due">No active plan</Pill>}
                {c.markedBy && <span className="hidden text-[12px] text-muted sm:inline">by {c.markedBy}</span>}
                {canMark && (
                  <Button size="icon" variant="ghost" aria-label={`Undo check-in for ${c.customerName}`} onClick={() => undo.mutate(c.id)}>
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </Panel>
    </div>
  );
}

function useGroups() {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "groups"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/groups", { params: { path: { businessId: id } } })),
  });
}

function Groups() {
  const business = useBusiness();
  const groups = useGroups();
  const [editing, setEditing] = useState<Schemas["GroupResponse"] | "new" | null>(null);
  const [selected, setSelected] = useState<Schemas["GroupResponse"] | null>(null);
  const canManage = business.can("customers.manage");
  const singular = business.labels.group;

  return (
    <div className="grid gap-5 lg:grid-cols-[1fr_1.4fr]">
      <Panel title={business.labels.groups} actions={canManage && <Button size="sm" icon={<Plus className="h-4 w-4" />} onClick={() => setEditing("new")}>New {singular.toLowerCase()}</Button>}>
        {groups.isPending ? (
          <div className="space-y-2 p-5">{[0, 1].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
        ) : (groups.data ?? []).length === 0 ? (
          <EmptyState icon={<UsersRound className="h-5 w-5" />} title={`No ${business.labels.groups.toLowerCase()} yet`} body={`Use ${business.labels.groups.toLowerCase()} for timings or levels, like "Morning 6 am".`} />
        ) : (
          <ul className="divide-y divide-line">
            {(groups.data ?? []).map((g) => (
              <li key={g.id}>
                <button
                  type="button"
                  onClick={() => setSelected(g)}
                  className={`flex w-full items-center justify-between gap-3 px-5 py-3 text-left hover:bg-canvas ${selected?.id === g.id ? "bg-cobalt-soft/50" : ""}`}
                >
                  <span className="min-w-0">
                    <span className="block font-medium text-ink">{g.name}</span>
                    {g.description && <span className="block truncate text-[12.5px] text-muted">{g.description}</span>}
                  </span>
                  <span className="text-[12.5px] tabular-nums text-muted">{plural(g.memberCount, "person", "people")}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Panel>
      {selected ? (
        <GroupDetail key={selected.id} group={(groups.data ?? []).find((g) => g.id === selected.id) ?? selected} onEdit={() => setEditing(selected)} onDeleted={() => setSelected(null)} />
      ) : (
        <Panel bodyClassName="py-16">
          <EmptyState title={`Choose a ${singular.toLowerCase()}`} body="to see who's in it and take a roll call" />
        </Panel>
      )}
      <Dialog open={editing != null} onOpenChange={(o) => !o && setEditing(null)} title={editing === "new" ? `New ${singular.toLowerCase()}` : editing ? `Edit ${editing.name}` : ""} size="sm">
        {editing && <GroupForm group={editing === "new" ? undefined : editing} onDone={() => setEditing(null)} />}
      </Dialog>
    </div>
  );
}

function GroupForm({ group, onDone }: { group?: Schemas["GroupResponse"]; onDone: () => void }) {
  const business = useBusiness();
  const queryClient = useQueryClient();
  const [name, setName] = useState(group?.name ?? "");
  const [description, setDescription] = useState(group?.description ?? "");
  const [error, setError] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: (body: Schemas["GroupRequest"]) =>
      group
        ? unwrap(api.PATCH("/api/v1/businesses/{businessId}/groups/{groupId}", { params: { path: { businessId: business.id, groupId: group.id } }, body }))
        : unwrap(api.POST("/api/v1/businesses/{businessId}/groups", { params: { path: { businessId: business.id } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(business.id, "groups") }),
  });
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!name.trim()) {
      setError("Enter a name");
      return;
    }
    try {
      await save.mutateAsync({ name: name.trim(), description: description.trim() || undefined });
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };
  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <Field label="Name">
          <Input autoFocus value={name} onChange={(e) => setName(e.target.value)} placeholder="Morning 6 am" />
        </Field>
        <Field label="Description" optional>
          <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={save.isPending}>{group ? "Save" : "Add"}</Button>
      </DialogFooter>
    </form>
  );
}

function GroupDetail({ group, onEdit, onDeleted }: { group: Schemas["GroupResponse"]; onEdit: () => void; onDeleted: () => void }) {
  const business = useBusiness();
  const queryClient = useQueryClient();
  const members = useCustomers({ groupId: group.id, view: "ALL", size: 100, sort: "NAME" });
  const [adding, setAdding] = useState<PickedCustomer | null>(null);
  const [rollCall, setRollCall] = useState(false);
  const canManage = business.can("customers.manage");
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: businessKey(business.id, "groups") }),
      queryClient.invalidateQueries({ queryKey: businessKey(business.id, "customers") }),
    ]);
  const path = { businessId: business.id, groupId: group.id };

  const add = async (picked: PickedCustomer | null) => {
    setAdding(picked);
    if (!picked) return;
    try {
      await unwrap(api.POST("/api/v1/businesses/{businessId}/groups/{groupId}/members", { params: { path }, body: { customerIds: [picked.id] } }));
      await refresh();
      setAdding(null);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };
  const removeMember = async (customerId: number) => {
    try {
      await unwrap(api.DELETE("/api/v1/businesses/{businessId}/groups/{groupId}/members/{customerId}", { params: { path: { ...path, customerId } } }));
      await refresh();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };
  const deleteGroup = async () => {
    try {
      await unwrap(api.DELETE("/api/v1/businesses/{businessId}/groups/{groupId}", { params: { path } }));
      await refresh();
      onDeleted();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  return (
    <Panel
      title={group.name}
      actions={
        <>
          {business.has("ATTENDANCE") && business.can("attendance.mark") && (members.data?.items.length ?? 0) > 0 && (
            <Button size="sm" variant="primary" onClick={() => setRollCall(true)}>Roll call</Button>
          )}
          {canManage && (
            <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${group.name}`}><Ellipsis className="h-4 w-4" /></Button>}>
              <MenuItem onSelect={onEdit}>Rename</MenuItem>
              <MenuItem danger onSelect={deleteGroup}>Delete {business.labels.group.toLowerCase()}</MenuItem>
            </Menu>
          )}
        </>
      }
    >
      {canManage && (
        <div className="border-b border-line px-5 py-3">
          <CustomerPicker value={adding} onChange={add} placeholder={`Add a ${business.labels.customer.toLowerCase()} to ${group.name}`} />
        </div>
      )}
      {members.isPending ? (
        <LoadingBlock />
      ) : (members.data?.items ?? []).length === 0 ? (
        <p className="px-5 py-6 text-[13px] text-muted">Nobody in this {business.labels.group.toLowerCase()} yet.</p>
      ) : (
        <ul className="divide-y divide-line">
          {(members.data?.items ?? []).map((c) => (
            <li key={c.id} className="flex items-center gap-3 px-5 py-2.5 text-[13.5px]">
              <span className="w-10 tabular-nums text-faint">#{c.number}</span>
              <span className="min-w-0 flex-1 truncate font-medium">{c.fullName}</span>
              {canManage && (
                <Button size="icon" variant="ghost" aria-label={`Remove ${c.fullName} from ${group.name}`} onClick={() => removeMember(c.id)}>
                  <X className="h-4 w-4" />
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
      <Dialog open={rollCall} onOpenChange={setRollCall} title={`Roll call: ${group.name}`} size="sm">
        <RollCallForm group={group} members={members.data?.items ?? []} onDone={() => setRollCall(false)} />
      </Dialog>
    </Panel>
  );
}

function RollCallForm({ group, members, onDone }: { group: Schemas["GroupResponse"]; members: Schemas["CustomerRow"][]; onDone: () => void }) {
  const business = useBusiness();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(localToday());
  const [present, setPresent] = useState<Set<number>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: () =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/groups/{groupId}/attendance", {
        params: { path: { businessId: business.id, groupId: group.id } },
        body: { date, present: [...present] },
      })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(business.id, "attendance") }),
  });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await save.mutateAsync();
      toast.success(`Marked ${present.size} of ${members.length} present`);
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-3">
        <FormError message={error} />
        <Field label="Day">
          <Input type="date" max={localToday()} value={date} onChange={(e) => setDate(e.target.value)} />
        </Field>
        <div className="flex items-center justify-between text-[12.5px] text-muted">
          <span>{present.size} of {members.length} present</span>
          <button type="button" className="font-medium text-cobalt hover:underline" onClick={() => setPresent(new Set(members.map((m) => m.id)))}>Everyone</button>
        </div>
        <div className="max-h-[320px] space-y-2 overflow-y-auto">
          {members.map((m) => (
            <Checkbox
              key={m.id}
              label={m.fullName}
              checked={present.has(m.id)}
              onChange={() =>
                setPresent((current) => {
                  const next = new Set(current);
                  if (next.has(m.id)) next.delete(m.id);
                  else next.add(m.id);
                  return next;
                })
              }
            />
          ))}
        </div>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={save.isPending}>Save attendance</Button>
      </DialogFooter>
    </form>
  );
}

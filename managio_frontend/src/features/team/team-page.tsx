"use client";

import { Ellipsis, IdCard, Plus, ShieldCheck, UserPlus } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Checkbox, Field, FormError, Input, Select, Textarea } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Menu, MenuItem, MenuSeparator, Tabs } from "@/components/ui/menu";
import { Pill, Tag } from "@/components/ui/pill";
import { resendVerificationEmail } from "@/features/auth/api";
import { useBusiness } from "@/features/business/context";
import { useStaffSession } from "@/lib/auth/use-session";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { formatDate, formatDateTime, formatMoney, humanize } from "@/lib/format";
import {
  PERMISSION_GROUPS,
  toPermissionName,
  useDeleteRole,
  useInvitationAction,
  useInvitations,
  useInvite,
  useMemberAction,
  usePermissionCatalogue,
  useRoles,
  useSaveRole,
  useTeam,
  useUpdateMember,
  type Role,
  type TeamMember,
} from "./api";

/** The team (TEAM-1 to TEAM-9): people, pending invitations, and the roles that decide what each person can do. */
export function TeamPage() {
  const business = useBusiness();
  const canManage = business.can("team.manage");
  const [tab, setTab] = useState("people");
  const [inviting, setInviting] = useState(false);
  const tabs = [
    { value: "people", label: "People" },
    ...(canManage ? [{ value: "invitations", label: "Invitations" }, { value: "roles", label: "Roles" }] : []),
  ];

  return (
    <>
      <PageHeader
        title="Team"
        subtitle="Everyone who works here and what they can do"
        actions={canManage && <Button variant="primary" icon={<UserPlus className="h-4 w-4" />} onClick={() => setInviting(true)}>Invite someone</Button>}
      />
      <Tabs value={tab} onValueChange={setTab} items={tabs} />
      <div className="mt-4">
        {tab === "people" && <People />}
        {tab === "invitations" && <Invitations onInvite={() => setInviting(true)} />}
        {tab === "roles" && <Roles />}
      </div>
      <Dialog open={inviting} onOpenChange={setInviting} title={`Invite someone to ${business.name}`} description="They get an email with a link to join. It works for 7 days.">
        <InviteForm onDone={() => setInviting(false)} />
      </Dialog>
    </>
  );
}

function People() {
  const business = useBusiness();
  const session = useStaffSession();
  const [includeLeft, setIncludeLeft] = useState(false);
  const [editing, setEditing] = useState<TeamMember | null>(null);
  const team = useTeam(includeLeft);
  const action = useMemberAction();
  const canManage = business.can("team.manage");
  const showSalary = business.can("payroll.manage");

  const run = async (member: TeamMember, kind: "suspend" | "reactivate" | "remove") => {
    try {
      await action.mutateAsync({ memberId: member.id, action: kind });
      toast.success(kind === "suspend" ? `${member.fullName} is suspended` : kind === "reactivate" ? `${member.fullName} is back` : `${member.fullName} was removed from the team`);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <Panel actions={<Checkbox label="Show people who left" checked={includeLeft} onChange={(e) => setIncludeLeft(e.target.checked)} />} title="People">
      {team.isPending ? (
        <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
      ) : team.isError ? (
        <EmptyState title="The team couldn't load" body={errorMessage(team.error)} />
      ) : (
        <Table>
          <THead>
            <tr>
              <TH>Name</TH>
              <TH>Role</TH>
              <TH>Joined</TH>
              {showSalary && <TH className="text-right">Monthly salary</TH>}
              <TH>Status</TH>
              <TH><span className="sr-only">Actions</span></TH>
            </tr>
          </THead>
          <tbody>
            {(team.data ?? []).map((m) => {
              const self = m.userId === session.principal?.id;
              return (
                <TR key={m.id} className={m.status === "LEFT" ? "text-faint" : undefined}>
                  <TD>
                    <span className="font-medium">{m.fullName}</span>
                    {self && <span className="ml-1 text-[12px] text-faint">(you)</span>}
                    <span className="block text-[12px] text-muted">{[m.title, m.email].filter(Boolean).join(" · ")}</span>
                  </TD>
                  <TD>{m.roleName}</TD>
                  <TD className="tabular-nums text-muted">{m.joinedOn ? formatDate(m.joinedOn) : "—"}{m.leftOn ? ` – ${formatDate(m.leftOn)}` : ""}</TD>
                  {showSalary && <TD className="text-right tabular-nums">{m.monthlySalary != null ? formatMoney(m.monthlySalary) : <span className="text-faint">—</span>}</TD>}
                  <TD>
                    <Pill tone={m.status === "ACTIVE" ? "ok" : m.status === "SUSPENDED" ? "due" : "neutral"}>{m.status === "LEFT" ? "Left" : humanize(m.status)}</Pill>
                  </TD>
                  <TD className="w-10 text-right">
                    {canManage && !m.owner && !self && m.status !== "LEFT" && (
                      <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${m.fullName}`}><Ellipsis className="h-4 w-4" /></Button>}>
                        <MenuItem onSelect={() => setEditing(m)}>Edit</MenuItem>
                        {m.status === "ACTIVE" ? (
                          <MenuItem onSelect={() => run(m, "suspend")}>Suspend</MenuItem>
                        ) : (
                          <MenuItem onSelect={() => run(m, "reactivate")}>Reactivate</MenuItem>
                        )}
                        <MenuSeparator />
                        <MenuItem danger onSelect={() => run(m, "remove")}>Remove from team</MenuItem>
                      </Menu>
                    )}
                    {(m.owner || self) && showSalary && m.status !== "LEFT" && (
                      <Button size="sm" variant="ghost" onClick={() => setEditing(m)}>Edit</Button>
                    )}
                  </TD>
                </TR>
              );
            })}
          </tbody>
        </Table>
      )}
      <Dialog open={editing != null} onOpenChange={(o) => !o && setEditing(null)} title={editing ? `Edit ${editing.fullName}` : ""}>
        {editing && <MemberForm member={editing} onDone={() => setEditing(null)} />}
      </Dialog>
    </Panel>
  );
}

function MemberForm({ member, onDone }: { member: TeamMember; onDone: () => void }) {
  const business = useBusiness();
  const session = useStaffSession();
  const roles = useRoles(business.can("team.manage"));
  const update = useUpdateMember(member.id);
  const canChangeRole = business.can("team.manage") && !member.owner && member.userId !== session.principal?.id;
  const [values, setValues] = useState({
    roleId: String(member.roleId),
    title: member.title ?? "",
    employeeCode: member.employeeCode ?? "",
    phone: member.phone ?? "",
    joinedOn: member.joinedOn ?? "",
    notes: member.notes ?? "",
    monthlySalary: member.monthlySalary != null ? String(member.monthlySalary) : "",
  });
  const [error, setError] = useState<string | null>(null);
  const set = (key: keyof typeof values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [key]: e.target.value }));
  const canPay = business.can("payroll.manage");

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (values.monthlySalary && !/^\d+(\.\d{1,2})?$/.test(values.monthlySalary)) {
      setError("Enter the salary like 25000");
      return;
    }
    try {
      await update.mutateAsync({
        roleId: canChangeRole ? Number(values.roleId) : undefined,
        title: values.title.trim() || undefined,
        employeeCode: values.employeeCode.trim() || undefined,
        phone: values.phone.trim() || undefined,
        joinedOn: values.joinedOn || undefined,
        notes: values.notes.trim() || undefined,
        ...(canPay ? (values.monthlySalary ? { monthlySalary: Number(values.monthlySalary) } : member.monthlySalary != null ? { clearSalary: true } : {}) : {}),
      });
      toast.success(`Saved ${member.fullName}`);
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        {canChangeRole && (
          <Field label="Role">
            <Select value={values.roleId} onChange={set("roleId")} disabled={roles.isPending}>
              {(roles.data ?? []).filter((r) => !r.owner).map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </Select>
          </Field>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Job title" optional>
            <Input value={values.title} onChange={set("title")} placeholder="Senior trainer" />
          </Field>
          <Field label="Employee code" optional>
            <Input value={values.employeeCode} onChange={set("employeeCode")} />
          </Field>
          <Field label="Phone" optional>
            <Input type="tel" value={values.phone} onChange={set("phone")} />
          </Field>
          <Field label="Joined on" optional>
            <Input type="date" value={values.joinedOn} onChange={set("joinedOn")} />
          </Field>
          {canPay && (
            <Field label="Monthly salary" optional hint="Used when preparing payroll">
              <Input inputMode="decimal" value={values.monthlySalary} onChange={set("monthlySalary")} />
            </Field>
          )}
        </div>
        <Field label="Notes" optional>
          <Textarea rows={2} value={values.notes} onChange={set("notes")} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={update.isPending}>Save</Button>
      </DialogFooter>
    </form>
  );
}

function Invitations({ onInvite }: { onInvite: () => void }) {
  const invitations = useInvitations();
  const action = useInvitationAction();
  const run = async (invitationId: number, kind: "resend" | "revoke", email: string) => {
    try {
      await action.mutateAsync({ invitationId, action: kind });
      toast.success(kind === "resend" ? `Sent a fresh link to ${email}` : `Cancelled the invitation for ${email}`);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };
  return (
    <Panel title="Waiting to join">
      {invitations.isPending ? (
        <div className="space-y-2 p-5">{[0, 1].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
      ) : (invitations.data ?? []).length === 0 ? (
        <EmptyState icon={<UserPlus className="h-5 w-5" />} title="No invitations waiting" action={<Button variant="primary" onClick={onInvite}>Invite someone</Button>} />
      ) : (
        <Table>
          <THead>
            <tr>
              <TH>Email</TH>
              <TH>Role</TH>
              <TH>Invited</TH>
              <TH>Link works until</TH>
              <TH><span className="sr-only">Actions</span></TH>
            </tr>
          </THead>
          <tbody>
            {(invitations.data ?? []).map((i) => (
              <TR key={i.id}>
                <TD className="font-medium">{i.email}{i.title && <span className="block text-[12px] font-normal text-muted">{i.title}</span>}</TD>
                <TD>{i.roleName}</TD>
                <TD className="text-muted">{formatDateTime(i.createdAt)}{i.invitedBy ? ` by ${i.invitedBy}` : ""}</TD>
                <TD className="tabular-nums text-muted">{formatDateTime(i.expiresAt)}</TD>
                <TD className="w-10 text-right">
                  <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${i.email}`}><Ellipsis className="h-4 w-4" /></Button>}>
                    <MenuItem onSelect={() => run(i.id, "resend", i.email)}>Send a new link</MenuItem>
                    <MenuItem danger onSelect={() => run(i.id, "revoke", i.email)}>Cancel invitation</MenuItem>
                  </Menu>
                </TD>
              </TR>
            ))}
          </tbody>
        </Table>
      )}
    </Panel>
  );
}

function InviteForm({ onDone }: { onDone: () => void }) {
  const roles = useRoles();
  const invite = useInvite();
  const [email, setEmail] = useState("");
  const [roleId, setRoleId] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [unverified, setUnverified] = useState(false);
  const assignable = (roles.data ?? []).filter((r) => !r.owner);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    const chosenRole = roleId || (assignable[0] ? String(assignable[0].id) : "");
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setError("Enter a valid email");
      return;
    }
    try {
      await invite.mutateAsync({ email: email.trim(), roleId: Number(chosenRole), title: title.trim() || undefined });
      toast.success(`Invitation sent to ${email.trim()}`);
      onDone();
    } catch (e) {
      setUnverified(e instanceof ApiError && e.is("invitation.unverified_sender"));
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        {unverified && (
          <Button
            size="sm"
            onClick={async () => {
              try {
                await resendVerificationEmail();
                toast.success("Check your inbox for the confirmation link");
              } catch (e) {
                toast.error(errorMessage(e));
              }
            }}
          >
            Send me the confirmation email again
          </Button>
        )}
        <Field label="Their email">
          <Input type="email" autoFocus value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Role" hint="You can change it later">
            <Select value={roleId || (assignable[0] ? String(assignable[0].id) : "")} onChange={(e) => setRoleId(e.target.value)} disabled={roles.isPending}>
              {assignable.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </Select>
          </Field>
          <Field label="Job title" optional>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} />
          </Field>
        </div>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={invite.isPending}>Send invitation</Button>
      </DialogFooter>
    </form>
  );
}

function Roles() {
  const business = useBusiness();
  const roles = useRoles();
  const remove = useDeleteRole();
  const [editing, setEditing] = useState<Role | "new" | null>(null);
  const catalogue = usePermissionCatalogue();
  const names = new Map((catalogue.data ?? []).map((p) => [p.key, p.name]));

  return (
    <Panel
      title="Roles"
      actions={business.owner && <Button size="sm" icon={<Plus className="h-4 w-4" />} onClick={() => setEditing("new")}>New role</Button>}
    >
      {!business.owner && <p className="border-b border-line px-5 py-3 text-[13px] text-muted">Only the owner can change roles.</p>}
      {roles.isPending ? (
        <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
      ) : (
        <ul className="divide-y divide-line">
          {(roles.data ?? []).map((role) => (
            <li key={role.id} className="flex items-start gap-4 px-5 py-4">
              <span className="mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-full bg-cobalt-soft text-cobalt">
                {role.owner ? <ShieldCheck className="h-4 w-4" aria-hidden /> : <IdCard className="h-4 w-4" aria-hidden />}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-[14px] font-semibold text-ink">
                  {role.name} <span className="ml-1 text-[12.5px] font-normal text-muted">{role.memberCount === 1 ? "1 person" : `${role.memberCount} people`}</span>
                </p>
                {role.description && <p className="text-[13px] text-muted">{role.description}</p>}
                <p className="mt-1.5 flex flex-wrap gap-1">
                  {role.owner ? <Tag>Everything</Tag> : role.permissions.map((key) => <Tag key={key}>{names.get(key) ?? key}</Tag>)}
                </p>
              </div>
              {business.owner && !role.owner && (
                <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${role.name}`}><Ellipsis className="h-4 w-4" /></Button>}>
                  <MenuItem onSelect={() => setEditing(role)}>Edit</MenuItem>
                  <MenuItem
                    danger
                    disabled={role.memberCount > 0}
                    onSelect={async () => {
                      try {
                        await remove.mutateAsync(role.id);
                        toast.success(`Deleted ${role.name}`);
                      } catch (error) {
                        toast.error(errorMessage(error));
                      }
                    }}
                  >
                    {role.memberCount > 0 ? "Delete (move its people first)" : "Delete"}
                  </MenuItem>
                </Menu>
              )}
            </li>
          ))}
        </ul>
      )}
      <Dialog open={editing != null} onOpenChange={(o) => !o && setEditing(null)} title={editing === "new" ? "New role" : editing ? `Edit ${editing.name}` : ""} size="lg">
        {editing && <RoleForm role={editing === "new" ? undefined : editing} onDone={() => setEditing(null)} />}
      </Dialog>
    </Panel>
  );
}

function RoleForm({ role, onDone }: { role?: Role; onDone: () => void }) {
  const catalogue = usePermissionCatalogue();
  const save = useSaveRole(role?.id);
  const [name, setName] = useState(role?.name ?? "");
  const [description, setDescription] = useState(role?.description ?? "");
  const [selected, setSelected] = useState<Set<string>>(new Set(role?.permissions ?? []));
  const [error, setError] = useState<string | null>(null);
  const labels = new Map((catalogue.data ?? []).map((p) => [p.key, p.name]));

  const toggle = (key: string) =>
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!name.trim()) {
      setError("Enter a role name");
      return;
    }
    try {
      await save.mutateAsync({ name: name.trim(), description: description.trim() || undefined, permissions: [...selected].map(toPermissionName) });
      toast.success(role ? `Saved ${name.trim()}` : `Added the ${name.trim()} role`);
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name">
            <Input autoFocus value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label="Description" optional>
            <Input value={description} onChange={(e) => setDescription(e.target.value)} />
          </Field>
        </div>
        <div className="grid gap-5 sm:grid-cols-2">
          {PERMISSION_GROUPS.map((group) => (
            <fieldset key={group.title}>
              <legend className="mb-2 text-[13px] font-semibold text-ink">{group.title}</legend>
              <div className="space-y-2">
                {group.keys.map((key) => (
                  <Checkbox key={key} label={labels.get(key) ?? key} checked={selected.has(key)} onChange={() => toggle(key)} />
                ))}
              </div>
            </fieldset>
          ))}
        </div>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={save.isPending}>{role ? "Save role" : "Add role"}</Button>
      </DialogFooter>
    </form>
  );
}

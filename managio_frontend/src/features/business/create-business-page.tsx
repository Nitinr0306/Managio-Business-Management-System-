"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Briefcase, Dumbbell, GraduationCap, School, Scissors, Stethoscope, Store, type LucideIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, FormError, Input } from "@/components/ui/field";
import { api, unwrap } from "@/lib/api/client";
import { cn } from "@/lib/cn";
import { applyApiError } from "@/lib/forms";
import { BUSINESS_TYPE_LABELS, type BusinessType } from "./access";
import { myBusinessesKey, useMyBusinesses } from "./context";
import { Logo } from "@/components/brand/logo";

const TYPES: { type: BusinessType; icon: LucideIcon }[] = [
  { type: "FITNESS", icon: Dumbbell },
  { type: "SALON", icon: Scissors },
  { type: "COACHING", icon: GraduationCap },
  { type: "SCHOOL", icon: School },
  { type: "CLINIC", icon: Stethoscope },
  { type: "RETAIL", icon: Store },
  { type: "OTHER", icon: Briefcase },
];

const schema = z.object({
  name: z.string().trim().min(1, "Enter the business name").max(150),
  type: z.enum(["FITNESS", "SALON", "COACHING", "SCHOOL", "CLINIC", "RETAIL", "OTHER"], "Choose what kind of business it is"),
  phone: z.string().max(20).optional(),
  city: z.string().max(100).optional(),
});
type Values = z.infer<typeof schema>;

/**
 * Creating a business sets it up from its type's template: the words it uses for customers and plans, the
 * modules it starts with, its roles and its expense categories (BUS-1, BUS-2). Everything can be changed later.
 */
export function CreateBusinessPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const businesses = useMyBusinesses();
  const types = useQuery({ queryKey: ["reference", "business-types"], queryFn: () => unwrap(api.GET("/api/v1/reference/business-types")) });
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError, setValue, control } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", phone: "", city: "" },
  });
  const selected = useWatch({ control, name: "type" });
  const template = types.data?.find((t) => t.type === selected);
  const firstBusiness = businesses.data?.length === 0;

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const created = await unwrap(
        api.POST("/api/v1/businesses", { body: { name: values.name, type: values.type, phone: values.phone || undefined, city: values.city || undefined } }),
      );
      await queryClient.invalidateQueries({ queryKey: myBusinessesKey });
      router.push(`/app/${created.id}`);
    } catch (error) {
      setFormError(applyApiError(error, setError, ["name", "type", "phone", "city"]));
    }
  });

  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex h-14 max-w-[880px] items-center justify-between px-4 sm:px-8">
          <Logo size="sm" />
          {!firstBusiness && (
            <Link href="/app?view=all" className="text-[13px] font-medium text-cobalt hover:underline">
              Back to your businesses
            </Link>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-[880px] px-4 py-10 sm:px-8">
        <h1 className="text-[26px] font-semibold leading-8 text-ink text-balance">
          {firstBusiness ? "Set up your first business" : "Add a business"}
        </h1>
        <p className="mt-1 max-w-[60ch] text-[14px] text-muted">
          Pick the kind of business and Managio sets up the right words, roles and expense categories. You can change any of it later.
        </p>
        <form onSubmit={submit} className="mt-8 space-y-6" noValidate>
          <FormError message={formError} />
          <fieldset>
            <legend className="mb-2 text-[13px] font-medium text-ink">What kind of business is it?</legend>
            <div role="radiogroup" className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              {TYPES.map(({ type, icon: Icon }) => (
                <button
                  key={type}
                  type="button"
                  role="radio"
                  aria-checked={selected === type}
                  onClick={() => setValue("type", type, { shouldValidate: true })}
                  className={cn(
                    "flex flex-col items-start gap-2 rounded-[var(--radius-panel)] border bg-surface p-3 text-left transition-colors",
                    selected === type ? "border-cobalt shadow-[0_0_0_1px_var(--color-cobalt)]" : "border-line hover:border-line-strong",
                  )}
                >
                  <Icon className={cn("h-5 w-5", selected === type ? "text-cobalt" : "text-faint")} aria-hidden />
                  <span className="text-[13.5px] font-medium text-ink">{BUSINESS_TYPE_LABELS[type]}</span>
                </button>
              ))}
            </div>
            {formState.errors.type && <p className="mt-1.5 text-[12.5px] font-medium text-bad">{formState.errors.type.message}</p>}
            {template && (
              <p className="mt-3 text-[13px] text-muted">
                You&apos;ll call the people you serve <strong className="text-ink">{template.labels.customers.toLowerCase()}</strong> and what they buy{" "}
                <strong className="text-ink">{template.labels.plans.toLowerCase()}</strong>.
              </p>
            )}
          </fieldset>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Business name" error={formState.errors.name?.message} className="sm:col-span-2">
              <Input placeholder="Ironwood Fitness" {...register("name")} />
            </Field>
            <Field label="Phone" optional error={formState.errors.phone?.message}>
              <Input type="tel" {...register("phone")} />
            </Field>
            <Field label="City" optional error={formState.errors.city?.message}>
              <Input {...register("city")} />
            </Field>
          </div>
          <Button type="submit" variant="primary" size="lg" loading={formState.isSubmitting}>
            Create business
          </Button>
        </form>
      </main>
    </div>
  );
}

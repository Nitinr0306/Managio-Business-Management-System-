import Link from "next/link";

export default function NotFound() {
  return (
    <div className="grid min-h-screen place-items-center bg-canvas px-4">
      <div className="max-w-[420px] text-center">
        <p className="text-[13px] font-semibold tabular-nums text-cobalt">404</p>
        <h1 className="mt-2 text-[22px] font-semibold text-ink">There&apos;s no page here</h1>
        <p className="mt-2 text-[14px] text-muted">The link may be old or mistyped.</p>
        <Link href="/" className="mt-5 inline-flex h-9 items-center rounded-[var(--radius-control)] bg-cobalt px-4 text-[13.5px] font-semibold text-white hover:bg-cobalt-dark">
          Go to the home page
        </Link>
      </div>
    </div>
  );
}

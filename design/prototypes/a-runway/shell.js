// Variant A "Runway": shared tokens, sidebar and status pill.
tailwind.config = {
  theme: {
    extend: {
      fontFamily: { sans: ['"Public Sans"', 'system-ui', 'sans-serif'] },
      colors: {
        canvas: '#F5F6F8', surface: '#FFFFFF', ink: '#17202E', muted: '#5B6577',
        faint: '#8A93A3', line: '#E3E6EB', cobalt: { DEFAULT: '#2952CC', soft: '#EBF0FC', dark: '#1F3FA3' },
        ok: '#1E8E5A', due: '#B7791F', bad: '#C8382E',
      },
    },
  },
};

const A = {
  nav: [
    ['layout-dashboard', 'Overview', 'dashboard.html'],
    ['users', 'Members', 'members.html'],
    ['badge-indian-rupee', 'Plans', '#'],
    ['receipt-indian-rupee', 'Payments', '#'],
    ['id-card', 'Staff', '#'],
    ['list-checks', 'Tasks', '#'],
    ['wallet', 'Salaries', '#'],
    ['history', 'Activity log', '#'],
  ],

  sidebar(active) {
    const { business } = MANAGIO;
    const items = A.nav.map(([icon, label, href]) => {
      const on = label === active;
      return `<a href="${href}" ${on ? 'aria-current="page"' : ''}
        class="group flex items-center gap-2.5 h-8 px-2.5 rounded-md text-[13.5px] ${on
          ? 'bg-surface text-cobalt font-semibold shadow-[0_0_0_1px_#E3E6EB]'
          : 'text-muted hover:text-ink hover:bg-black/[0.03]'} focus-visible:outline focus-visible:outline-2 focus-visible:outline-cobalt">
        <i data-lucide="${icon}" class="w-4 h-4 ${on ? '' : 'text-faint group-hover:text-muted'}"></i>${label}</a>`;
    }).join('');
    return `
    <aside class="hidden lg:flex w-[232px] shrink-0 flex-col border-r border-line bg-canvas h-screen sticky top-0">
      <button class="m-3 mb-2 flex items-center gap-2.5 rounded-md p-2 text-left hover:bg-black/[0.03] focus-visible:outline focus-visible:outline-2 focus-visible:outline-cobalt" aria-label="Switch business">
        <span class="grid place-items-center w-8 h-8 rounded-md bg-ink text-white text-[13px] font-bold">IF</span>
        <span class="min-w-0 flex-1">
          <span class="block text-[13px] leading-[17px] font-semibold text-ink">${business.name}</span>
          <span class="block text-[12px] text-faint tabular-nums">${business.publicId}</span>
        </span>
        <i data-lucide="chevrons-up-down" class="w-4 h-4 text-faint"></i>
      </button>
      <nav class="flex-1 px-3 py-2 space-y-0.5" aria-label="Main">${items}</nav>
      <div class="px-3 pb-2"><a href="#" class="flex items-center gap-2.5 h-8 px-2.5 rounded-md text-[13.5px] text-muted hover:text-ink hover:bg-black/[0.03]"><i data-lucide="settings" class="w-4 h-4 text-faint"></i>Settings</a></div>
      <div class="border-t border-line p-3 flex items-center gap-2.5">
        <span class="grid place-items-center w-8 h-8 rounded-full bg-cobalt-soft text-cobalt text-[12px] font-semibold">AR</span>
        <span class="text-[13px] leading-tight"><span class="block font-medium text-ink">${business.owner}</span><span class="text-faint">Owner</span></span>
      </div>
    </aside>`;
  },

  pill(m) {
    const tone = { active: 'text-ok border-ok/30', due: 'text-due border-due/35', lapsed: 'text-bad border-bad/30', none: 'text-muted border-line' }[m.status];
    const dot = { active: 'bg-ok', due: 'bg-due', lapsed: 'bg-bad', none: 'bg-faint' }[m.status];
    return `<span class="inline-flex items-center gap-1.5 h-6 px-2 rounded-full border bg-surface text-[12px] font-medium whitespace-nowrap ${tone}"><span class="w-1.5 h-1.5 rounded-full ${dot}"></span>${MANAGIO.statusLabel(m)}</span>`;
  },
};

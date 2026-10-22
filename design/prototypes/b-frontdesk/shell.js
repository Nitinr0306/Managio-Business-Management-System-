// Variant B "Front Desk": shared tokens, top bar and status pill.
tailwind.config = {
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Hanken Grotesk"', 'system-ui', 'sans-serif'],
        display: ['Epilogue', '"Hanken Grotesk"', 'sans-serif'],
      },
      colors: {
        pine: { DEFAULT: '#123C3A', 700: '#0D2E2C', 100: '#E3ECEA' },
        sun: { DEFAULT: '#F4C430', dark: '#E2B21C' },
        canvas: '#F2F4F1', surface: '#FFFFFF', ink: '#172322', muted: '#5D6B69', line: '#DDE3DF',
        ok: '#1F7A4A', due: '#9A5B00', bad: '#B3362B',
      },
    },
  },
};

const B = {
  topbar(active) {
    const tabs = ['Today', 'Members', 'Plans', 'Payments', 'Team', 'Activity'];
    return `
    <header class="sticky top-0 z-10">
      <div class="bg-pine text-white">
        <div class="h-16 px-4 sm:px-6 flex items-center gap-4">
          <span class="font-display text-[20px] font-bold tracking-tight">Managio</span>
          <button class="hidden md:inline-flex items-center gap-1.5 h-9 px-3 rounded-lg text-[14px] text-white/85 hover:bg-white/10">${MANAGIO.business.name}<i data-lucide="chevron-down" class="w-4 h-4"></i></button>
          <label class="relative flex-1 max-w-[560px] mx-auto">
            <span class="sr-only">Find a member</span>
            <i data-lucide="search" class="w-5 h-5 text-muted absolute left-3.5 top-1/2 -translate-y-1/2"></i>
            <input class="w-full h-11 pl-11 pr-12 rounded-xl bg-white text-ink text-[15px] placeholder:text-muted focus:outline-none focus:ring-4 focus:ring-sun/60" placeholder="Find a member by name, phone or member ID">
            <kbd class="absolute right-3 top-1/2 -translate-y-1/2 text-[12px] text-muted border border-line rounded px-1.5">/</kbd>
          </label>
          <button class="h-11 px-4 rounded-xl bg-sun text-pine font-semibold text-[15px] hover:bg-sun-dark inline-flex items-center gap-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sun"><i data-lucide="indian-rupee" class="w-4 h-4"></i><span class="hidden sm:inline">Record payment</span></button>
          <span class="grid place-items-center w-10 h-10 rounded-full bg-white/15 text-[13px] font-semibold">DM</span>
        </div>
      </div>
      <nav class="bg-surface border-b border-line px-4 sm:px-6 flex gap-1 overflow-x-auto" aria-label="Sections">
        ${tabs.map((t) => `<a href="${t === 'Today' ? 'dashboard.html' : t === 'Members' ? 'members.html' : '#'}" ${t === active ? 'aria-current="page"' : ''}
          class="h-12 px-3 inline-flex items-center text-[15px] border-b-[3px] ${t === active ? 'border-pine text-ink font-semibold' : 'border-transparent text-muted hover:text-ink'}">${t}</a>`).join('')}
      </nav>
    </header>`;
  },

  pill(m, large = true) {
    const tone = { active: 'bg-[#E3F3EA] text-ok', due: 'bg-[#FDF0D2] text-due', lapsed: 'bg-[#FBE3E0] text-bad', none: 'bg-[#E9EDEB] text-muted' }[m.status];
    const label = {
      active: `Active, ${m.days} days left`,
      due: m.days === 1 ? 'Ends tomorrow' : `Ends in ${m.days} days`,
      lapsed: `Expired ${-m.days} days ago`,
      none: 'No plan',
    }[m.status];
    return `<span class="inline-flex items-center ${large ? 'h-8 px-3 text-[14px]' : 'h-6 px-2 text-[12.5px]'} rounded-full font-semibold whitespace-nowrap ${tone}">${label}</span>`;
  },

  avatar(name, tone = 'bg-pine-100 text-pine') {
    return `<span class="grid place-items-center w-11 h-11 shrink-0 rounded-full ${tone} text-[14px] font-bold">${MANAGIO.initials(name)}</span>`;
  },
};

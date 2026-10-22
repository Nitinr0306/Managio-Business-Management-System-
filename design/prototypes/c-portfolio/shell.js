// Variant C "Portfolio": shared tokens, icon rail and command bar.
tailwind.config = {
  theme: {
    extend: {
      fontFamily: { sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'] },
      colors: {
        canvas: '#12151C', surface: '#1A1F29', raised: '#222835', line: '#2C3342',
        ink: '#E7EAF0', muted: '#9AA3B5', faint: '#6B7486',
        aqua: { DEFAULT: '#36C2B4', dark: '#2BA89B', soft: '#163A3A' },
        risk: '#E3A93B', neg: '#EF7A63',
      },
    },
  },
};

const C = {
  rail(active) {
    const items = [['layout-grid', 'Portfolio', 'dashboard.html'], ['users', 'Members', 'members.html'], ['badge-indian-rupee', 'Plans', '#'],
      ['receipt-indian-rupee', 'Payments', '#'], ['id-card', 'Team', '#'], ['history', 'Activity', '#']];
    return `
    <nav class="hidden md:flex w-16 shrink-0 flex-col items-center gap-1 py-3 border-r border-line bg-canvas h-screen sticky top-0" aria-label="Main">
      <span class="grid place-items-center w-9 h-9 mb-3 rounded bg-aqua text-canvas font-semibold">M</span>
      ${items.map(([icon, label, href]) => `
        <a href="${href}" title="${label}" aria-label="${label}" ${label === active ? 'aria-current="page"' : ''}
          class="relative grid place-items-center w-10 h-10 rounded ${label === active ? 'bg-raised text-aqua' : 'text-faint hover:text-ink hover:bg-surface'} focus-visible:outline focus-visible:outline-2 focus-visible:outline-aqua">
          ${label === active ? '<span class="absolute -left-3 top-2 bottom-2 w-0.5 bg-aqua rounded"></span>' : ''}
          <i data-lucide="${icon}" class="w-[18px] h-[18px]"></i></a>`).join('')}
      <a href="#" title="Settings" aria-label="Settings" class="mt-auto grid place-items-center w-10 h-10 rounded text-faint hover:text-ink hover:bg-surface"><i data-lucide="settings" class="w-[18px] h-[18px]"></i></a>
    </nav>`;
  },

  topbar(right = '') {
    return `
    <div class="h-14 px-5 sm:px-8 flex items-center gap-3 border-b border-line">
      <label class="relative w-full max-w-[420px]">
        <span class="sr-only">Search or jump to</span>
        <i data-lucide="search" class="w-4 h-4 text-faint absolute left-3 top-1/2 -translate-y-1/2"></i>
        <input class="w-full h-8 pl-9 pr-16 rounded bg-surface border border-line text-[13px] text-ink placeholder:text-faint focus:outline-none focus:border-aqua" placeholder="Search or jump to">
        <kbd class="absolute right-2 top-1/2 -translate-y-1/2 text-[11px] text-faint border border-line rounded px-1.5 py-px">Ctrl K</kbd>
      </label>
      <div class="ml-auto flex items-center gap-3">${right}
        <span class="flex items-center gap-2 text-[13px] text-muted"><span class="grid place-items-center w-7 h-7 rounded-full bg-raised text-ink text-[11px] font-medium">AR</span><span class="hidden sm:inline">Ananya Rao</span></span>
      </div>
    </div>`;
  },

  spark(series, color = '#36C2B4') {
    const min = Math.min(...series), max = Math.max(...series), w = 96, h = 24;
    const pts = series.map((v, i) => `${(i / (series.length - 1) * w).toFixed(1)},${(h - 2 - (v - min) / (max - min) * (h - 4)).toFixed(1)}`).join(' ');
    return `<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true"><polyline points="${pts}" fill="none" stroke="${color}" stroke-width="1.5" stroke-linejoin="round" stroke-linecap="round"/></svg>`;
  },
};

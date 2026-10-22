// Shared sample data for the three design prototypes.
// Shapes mirror Managio's API (MemberResponse, PaymentResponse, OwnerDashboardResponse),
// so whichever direction is chosen can be wired to real endpoints without remodelling.
window.MANAGIO = (() => {
  const inr = (n) => '₹' + Math.round(n).toLocaleString('en-IN');
  const lakh = (n) => '₹' + (n / 100000).toFixed(1) + 'L';

  const plans = {
    monthly: { name: 'Monthly Unlimited', price: 2499, days: 30 },
    quarterly: { name: 'Quarterly Strength', price: 6499, days: 90 },
    annual: { name: 'Annual Unlimited', price: 21999, days: 365 },
    yoga: { name: 'Yoga 3x/week Monthly', price: 1999, days: 30 },
  };

  // status: active | due | lapsed | none ; days: days left (negative = days since lapse)
  const members = [
    { name: 'Kavya Iyer', id: 'MBR-2MN6R5WE', phone: '+91 90080 31942', plan: 'yoga', ends: '21 Oct 2026', days: 1, status: 'due', last: [1999, '21 Sep 2026'], since: 'Apr 2026' },
    { name: 'Rohan Mehta', id: 'MBR-7K2Q9XLD', phone: '+91 98450 12873', plan: 'quarterly', ends: '24 Oct 2026', days: 4, status: 'due', last: [6499, '20 Oct 2026'], since: 'Apr 2026' },
    { name: 'Neha Gupta', id: 'MBR-5TY7U2PS', phone: '+91 96111 70358', plan: 'quarterly', ends: '26 Oct 2026', days: 6, status: 'due', last: [6499, '28 Jul 2026'], since: 'Nov 2025' },
    { name: 'Arjun Shetty', id: 'MBR-9PX4T7AC', phone: '+91 97409 88310', plan: 'monthly', ends: '15 Oct 2026', days: -5, status: 'lapsed', last: [2499, '15 Sep 2026'], since: 'Feb 2026' },
    { name: 'Meera Pillai', id: 'MBR-7GV3N6ZR', phone: '+91 94480 35176', plan: 'monthly', ends: '2 Oct 2026', days: -18, status: 'lapsed', last: [2499, '2 Sep 2026'], since: 'Dec 2025' },
    { name: 'Ishita Banerjee', id: 'MBR-4WC2X8JK', phone: '+91 98201 66734', plan: null, ends: null, days: null, status: 'none', last: null, since: 'Oct 2026' },
    { name: 'Priya Nair', id: 'MBR-3HD8Q2LM', phone: '+91 99001 45211', plan: 'annual', ends: '12 Mar 2027', days: 143, status: 'active', last: [21999, '20 Oct 2026'], since: 'Mar 2025' },
    { name: 'Sameer Khan', id: 'MBR-8QZ1K3VB', phone: '+91 98860 22407', plan: 'annual', ends: '30 Jun 2027', days: 253, status: 'active', last: [21999, '19 Oct 2026'], since: 'Jun 2024' },
    { name: 'Vikram Joshi', id: 'MBR-6RF9D4NH', phone: '+91 99860 51129', plan: 'monthly', ends: '18 Nov 2026', days: 29, status: 'active', last: [2499, '20 Oct 2026'], since: 'Jan 2026' },
    { name: 'Aditya Kulkarni', id: 'MBR-1LB5M9QT', phone: '+91 97311 20945', plan: 'quarterly', ends: '9 Jan 2027', days: 81, status: 'active', last: [6499, '11 Oct 2026'], since: 'Jul 2025' },
  ];

  const today = {
    dateLong: 'Tuesday, 20 October 2026',
    payments: [
      { time: '10:42', member: 'Priya Nair', method: 'UPI', amount: 21999, receipt: 'PAY-8F2K1Q7Z' },
      { time: '10:15', member: 'Rohan Mehta', method: 'Cash', amount: 6499, receipt: 'PAY-2C9L4M8D' },
      { time: '09:30', member: 'Vikram Joshi', method: 'UPI', amount: 2499, receipt: 'PAY-6T1R7W3E' },
      { time: '08:05', member: 'Sana Qureshi', method: 'Card', amount: 10503, receipt: 'PAY-4N8B2V6Y' },
    ],
  };
  today.total = today.payments.reduce((s, p) => s + p.amount, 0);
  today.byMethod = ['UPI', 'Cash', 'Card'].map((m) => ({
    method: m,
    amount: today.payments.filter((p) => p.method === m).reduce((s, p) => s + p.amount, 0),
  }));

  const business = {
    name: 'Ironwood Fitness Indiranagar', publicId: '4821IRWD', owner: 'Ananya Rao',
    activeMembers: 412, newThisMonth: 38, collectedMonth: 684200, collectedPrevMonth: 610900,
    endingIn7: 23, lapsed30: 17,
    counts: { all: 486, active: 412, ending: 23, lapsed: 34, none: 40 },
    paymentMix: [
      { method: 'UPI', pct: 58 }, { method: 'Cash', pct: 24 },
      { method: 'Card', pct: 15 }, { method: 'Bank transfer', pct: 3 },
    ],
  };

  // Memberships ending per day, 20 Oct to 18 Nov (first 7 days sum to 23, all 30 to 61).
  const runwayCounts = [2, 3, 4, 3, 6, 2, 3, 1, 2, 1, 3, 2, 1, 2, 1, 2, 3, 1, 1, 2, 1, 2, 1, 3, 1, 2, 1, 2, 2, 1];
  const runway = runwayCounts.map((count, i) => {
    const d = new Date(2026, 9, 20 + i);
    return {
      count,
      label: d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }),
      weekday: d.getDay(),
      inWeek: i < 7,
    };
  });

  const months = ['Nov', 'Dec', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'];
  const branches = [
    { name: 'Ironwood Fitness Indiranagar', short: 'Indiranagar', members: 412, renewal: 84, ending: 23, lapsed: 17,
      series: [492000, 503500, 561200, 548000, 572300, 590100, 601800, 585400, 598200, 604700, 610900, 684200] },
    { name: 'Ironwood Fitness HSR Layout', short: 'HSR Layout', members: 338, renewal: 72, ending: 21, lapsed: 26,
      series: [455200, 468900, 512300, 520100, 534800, 527600, 519900, 508300, 501200, 498700, 491600, 471900] },
    { name: 'Prana Yoga Studio', short: 'Prana Yoga', members: 196, renewal: 89, ending: 9, lapsed: 4,
      series: [118300, 121900, 132400, 138800, 142100, 149600, 155300, 161800, 168200, 176400, 185100, 218400] },
    { name: 'Taal Dance Academy', short: 'Taal Dance', members: 100, renewal: 80, ending: 4, lapsed: 3,
      series: [96200, 98100, 104500, 101900, 103300, 99800, 104100, 106700, 108200, 109900, 111400, 118100] },
  ].map((b) => {
    const oct = b.series[11], sep = b.series[10];
    return { ...b, collected: oct, delta: Math.round(((oct - sep) / sep) * 100) };
  });
  const portfolio = {
    collected: branches.reduce((s, b) => s + b.collected, 0),
    prev: branches.reduce((s, b) => s + b.series[10], 0),
    members: branches.reduce((s, b) => s + b.members, 0),
    ending: branches.reduce((s, b) => s + b.ending, 0),
    renewal: 81,
  };
  portfolio.delta = Math.round(((portfolio.collected - portfolio.prev) / portfolio.prev) * 100);

  const statusLabel = (m) => {
    if (m.status === 'none') return 'No plan';
    if (m.status === 'lapsed') return `Lapsed ${-m.days} days`;
    if (m.status === 'due') return m.days === 1 ? 'Due tomorrow' : `Due in ${m.days} days`;
    return 'Active';
  };
  const initials = (name) => name.split(' ').map((p) => p[0]).join('').slice(0, 2);

  return { inr, lakh, plans, members, today, business, runway, months, branches, portfolio, statusLabel, initials };
})();

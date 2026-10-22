// Generates Managio's raster brand assets from the mark in src/app/icon.svg:
//   src/app/favicon.ico (16, 32, 48), src/app/apple-icon.png (180), public/icon-192.png, public/icon-512.png,
//   public/icon-maskable-512.png, src/app/opengraph-image.png and src/app/twitter-image.png (1200 × 630).
// Run after changing the mark or the sharing image: `node scripts/brand-assets.mjs` (uses Playwright's Chromium,
// and Google Fonts for Public Sans while rendering the sharing image).
import { chromium } from "@playwright/test";
import { readFileSync, writeFileSync } from "node:fs";

const root = new URL("..", import.meta.url);
const file = (path) => new URL(path, root);
const iconSvg = readFileSync(file("src/app/icon.svg"), "utf8");
const M_PATH = iconSvg.match(/ d="([^"]+)"/)[1];

/** The mark as HTML: rounded square (or full bleed for platforms that round icons themselves). */
function markHtml(size, { rounded = true, padding = 0 } = {}) {
  const inner = size - padding * 2;
  return `<html><body style="margin:0;background:${rounded ? "transparent" : "#2952cc"}">
    <div style="width:${size}px;height:${size}px;display:grid;place-items:center">
      <svg viewBox="0 0 32 32" width="${inner}" height="${inner}">
        <rect width="32" height="32" rx="${rounded ? 8 : 0}" fill="#2952cc"/>
        <path d="${M_PATH}" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div></body></html>`;
}

const sharingHtml = `<html><head>
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=block" rel="stylesheet">
  <style>
    body { margin: 0; width: 1200px; height: 630px; overflow: hidden; font-family: "Public Sans", sans-serif;
           background: #17202e; color: #fff; position: relative; }
    .ledger { position: absolute; inset: 0; opacity: .14;
              background-image: linear-gradient(90deg, #fff 1px, transparent 1px); background-size: 32px 100%; }
    .glow { position: absolute; right: -160px; top: -140px; width: 560px; height: 560px; border-radius: 50%;
            background: rgba(41, 82, 204, .45); filter: blur(120px); }
    .content { position: absolute; inset: 72px 80px; display: flex; flex-direction: column; }
    .lockup { display: flex; align-items: center; gap: 16px; font-weight: 700; font-size: 40px; letter-spacing: -.02em; }
    h1 { margin: 64px 0 0; font-size: 68px; line-height: 1.04; letter-spacing: -.035em; font-weight: 700; max-width: 900px; }
    p { margin: 24px 0 0; font-size: 27px; line-height: 1.4; color: rgba(255,255,255,.74); max-width: 900px; }
    .foot { margin-top: auto; display: flex; justify-content: space-between; align-items: center; font-size: 24px;
            color: rgba(255,255,255,.6); }
    .free { color: #fff; font-weight: 600; border: 1px solid rgba(255,255,255,.28); border-radius: 999px; padding: 8px 20px; }
  </style></head><body>
  <div class="ledger"></div><div class="glow"></div>
  <div class="content">
    <div class="lockup">
      <svg viewBox="0 0 32 32" width="56" height="56"><rect width="32" height="32" rx="8" fill="#fff"/>
        <path d="${M_PATH}" fill="none" stroke="#2952cc" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      managio
    </div>
    <h1>Run every business you own from one screen.</h1>
    <p>Members, renewals, payments, expenses and staff for gyms, salons, coaching centres, schools, clinics and shops.</p>
    <div class="foot"><span>managio.me</span><span class="free">Free to use</span></div>
  </div></body></html>`;

/** A Windows icon file holding PNG images (supported since Windows Vista and by every browser). */
function ico(images) {
  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(images.length, 4);
  let offset = 6 + images.length * 16;
  const entries = images.map(({ size, png }) => {
    const entry = Buffer.alloc(16);
    entry.writeUInt8(size >= 256 ? 0 : size, 0);
    entry.writeUInt8(size >= 256 ? 0 : size, 1);
    entry.writeUInt16LE(1, 4);
    entry.writeUInt16LE(32, 6);
    entry.writeUInt32LE(png.length, 8);
    entry.writeUInt32LE(offset, 12);
    offset += png.length;
    return entry;
  });
  return Buffer.concat([header, ...entries, ...images.map((i) => i.png)]);
}

const browser = await chromium.launch();
const render = async (html, width, height, transparent = true) => {
  const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });
  await page.setContent(html, { waitUntil: "networkidle" });
  await page.evaluate(() => document.fonts.ready);
  const png = await page.screenshot({ omitBackground: transparent, clip: { x: 0, y: 0, width, height } });
  await page.close();
  return png;
};

const favicon = [];
for (const size of [16, 32, 48]) favicon.push({ size, png: await render(markHtml(size), size, size) });
writeFileSync(file("src/app/favicon.ico"), ico(favicon));
writeFileSync(file("src/app/apple-icon.png"), await render(markHtml(180, { rounded: false, padding: 0 }), 180, 180, false));
writeFileSync(file("public/icon-192.png"), await render(markHtml(192), 192, 192));
writeFileSync(file("public/icon-512.png"), await render(markHtml(512), 512, 512));
// Maskable: the platform crops to its own shape, so the mark sits inside the central safe zone on a full bleed.
writeFileSync(file("public/icon-maskable-512.png"), await render(markHtml(512, { rounded: false, padding: 64 }), 512, 512, false));
const sharing = await render(sharingHtml, 1200, 630, false);
writeFileSync(file("src/app/opengraph-image.png"), sharing);
writeFileSync(file("src/app/twitter-image.png"), sharing);
await browser.close();
console.log("Brand assets written.");

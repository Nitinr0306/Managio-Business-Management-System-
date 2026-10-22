import { expect, test, type Page } from "@playwright/test";

/**
 * What search engines and Google's consent screen see: one canonical host, the public pages indexable with their own
 * titles, descriptions and sharing cards, legal pages reachable from every public page, and everything private kept
 * out of the index.
 */
const SITE = "https://managio.me";

async function head(page: Page) {
  return page.evaluate(() => ({
    title: document.title,
    description: document.querySelector('meta[name="description"]')?.getAttribute("content") ?? null,
    canonical: document.querySelector('link[rel="canonical"]')?.getAttribute("href") ?? null,
    robots: document.querySelector('meta[name="robots"]')?.getAttribute("content") ?? null,
    ogUrl: document.querySelector('meta[property="og:url"]')?.getAttribute("content") ?? null,
    ogImage: document.querySelector('meta[property="og:image"]')?.getAttribute("content") ?? null,
    twitterCard: document.querySelector('meta[name="twitter:card"]')?.getAttribute("content") ?? null,
    h1: Array.from(document.querySelectorAll("h1")).map((h) => h.textContent?.trim()),
  }));
}

test("public pages have their own titles, canonical URLs and sharing cards", async ({ page }) => {
  const pages = [
    ["/", "Run every business you own from one screen."],
    ["/privacy", "Privacy Policy"],
    ["/terms", "Terms of Service"],
    ["/security", "Security"],
    ["/contact", "Contact"],
  ] as const;
  const titles = new Set<string>();
  for (const [path, heading] of pages) {
    const response = await page.goto(path);
    expect(response?.status(), path).toBe(200);
    const meta = await head(page);
    expect(meta.h1, path).toEqual([heading]);
    expect(meta.canonical, path).toBe(path === "/" ? SITE : `${SITE}${path}`);
    expect(meta.ogUrl, path).toBe(meta.canonical);
    expect(meta.robots, path).toBeNull();
    expect(meta.description?.length ?? 0, path).toBeGreaterThan(60);
    expect(meta.ogImage, path).toContain(`${SITE}/opengraph-image.png`);
    expect(meta.twitterCard, path).toBe("summary_large_image");
    titles.add(meta.title);
    // Every public page links to the legal pages Google's consent screen points at, and the footer keeps to help
    // and legal links: signing in and creating an account belong to the header and the page itself.
    const footer = page.getByRole("navigation", { name: "Footer" });
    for (const [name, href] of [["Contact", "/contact"], ["Security", "/security"], ["Privacy Policy", "/privacy"], ["Terms of Service", "/terms"]]) {
      await expect(footer.getByRole("link", { name, exact: true }), `${path} footer ${name}`).toHaveAttribute("href", href);
    }
    await expect(footer.getByRole("link", { name: /sign in|create an account|sign up/i }), `${path} footer`).toHaveCount(0);
  }
  expect(titles.size).toBe(pages.length);
});

test("the landing page describes itself with structured data that matches what it shows", async ({ page }) => {
  await page.goto("/");
  const data = JSON.parse((await page.locator('script[type="application/ld+json"]').textContent()) ?? "{}");
  const app = data["@graph"].find((node: { "@type": string }) => node["@type"] === "SoftwareApplication");
  expect(app).toMatchObject({ name: "Managio", url: SITE, offers: { price: "0", priceCurrency: "INR" } });
  expect(JSON.stringify(data)).not.toMatch(/aggregateRating|review/i);
  await expect(page.getByText("Free to use.", { exact: false })).toBeVisible();
  await expect(page.getByText("Example", { exact: true })).toBeVisible();
});

test("robots.txt, the sitemap and the manifest point at the public pages only", async ({ request }) => {
  const robots = await (await request.get("/robots.txt")).text();
  for (const rule of ["Disallow: /app$", "Disallow: /app/", "Disallow: /admin", "Disallow: /portal", "Disallow: /api/"]) {
    expect(robots).toContain(rule);
  }
  expect(robots).toContain(`Sitemap: ${SITE}/sitemap.xml`);

  const sitemap = await (await request.get("/sitemap.xml")).text();
  const urls = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map((m) => m[1]).sort();
  expect(urls).toEqual([SITE, `${SITE}/contact`, `${SITE}/privacy`, `${SITE}/security`, `${SITE}/terms`].sort());

  const manifest = await (await request.get("/manifest.webmanifest")).json();
  expect(manifest.name).toBe("Managio");
  for (const icon of manifest.icons) {
    expect((await request.get(icon.src)).headers()["content-type"], icon.src).toContain("image/png");
  }
  for (const asset of ["/icon.svg", "/favicon.ico", "/apple-icon.png", "/opengraph-image.png"]) {
    expect((await request.get(asset)).status(), asset).toBe(200);
  }
});

test("sign-in pages are kept out of search results and private areas are never indexed", async ({ page, request }) => {
  for (const path of ["/sign-in", "/sign-up", "/forgot-password"]) {
    const response = await page.goto(path);
    expect(response?.headers()["x-robots-tag"], path).toBe("noindex");
    expect((await head(page)).robots, path).toBe("noindex, follow");
  }
  for (const path of ["/app", "/app/1/customers", "/admin", "/portal"]) {
    const response = await request.get(path, { maxRedirects: 0 });
    expect(response.headers()["x-robots-tag"], path).toBe("noindex, nofollow");
  }
  expect((await request.get("/api/v1/me")).headers()["x-robots-tag"]).toBe("noindex, nofollow");
});

test("www and trailing-slash variants redirect permanently to one canonical URL", async ({ request }) => {
  const www = await request.get("/privacy", { headers: { host: "www.managio.me" }, maxRedirects: 0 });
  expect(www.status()).toBe(308);
  expect(www.headers().location).toBe(`${SITE}/privacy`);
  const slash = await request.get("/privacy/", { maxRedirects: 0 });
  expect(slash.status()).toBe(308);
  expect(new URL(slash.headers().location, SITE).pathname).toBe("/privacy");
});

test("legal pages name the operator and the real contact address, and sign-up links to them", async ({ page }) => {
  await page.goto("/privacy");
  await expect(page.getByText("run by Nitin, an individual based in India", { exact: false }).first()).toBeVisible();
  for (const path of ["/privacy", "/terms", "/contact", "/security"]) {
    await page.goto(path);
    await expect(page.getByRole("link", { name: "nitinr0306@gmail.com" }).first(), path).toHaveAttribute("href", "mailto:nitinr0306@gmail.com");
    expect(await page.locator('a[href^="mailto:"]').evaluateAll((links) => links.map((a) => a.getAttribute("href"))), path)
      .toEqual(expect.arrayContaining(["mailto:nitinr0306@gmail.com"]));
    await expect(page.locator('a[href^="mailto:"]:not([href="mailto:nitinr0306@gmail.com"])'), path).toHaveCount(0);
  }
  // The agreement inside the sign-up form, apart from the legal links under the card.
  await page.goto("/sign-up");
  const form = page.locator("form");
  await expect(form.getByRole("link", { name: "Terms of Service" })).toHaveAttribute("href", "/terms");
  await expect(form.getByRole("link", { name: "Privacy Policy" })).toHaveAttribute("href", "/privacy");
});

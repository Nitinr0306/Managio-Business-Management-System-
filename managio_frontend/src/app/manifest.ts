import type { MetadataRoute } from "next";
import { SITE } from "@/lib/site";

/** Lets phones add Managio to the home screen with its own name, icon and colours. Icons: scripts/brand-assets.mjs. */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Managio",
    short_name: "Managio",
    description: SITE.description,
    start_url: "/app",
    scope: "/",
    display: "standalone",
    background_color: "#f5f6f8",
    theme_color: "#17202e",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}

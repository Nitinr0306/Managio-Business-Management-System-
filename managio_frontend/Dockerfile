# Managio web app image (for self-hosting and the local Compose stack; Vercel builds from source instead).
# Build from managio_frontend:
#   docker build --build-arg API_ORIGIN=http://api:8080 -t managio-web .
# API_ORIGIN and NEXT_PUBLIC_GOOGLE_CLIENT_ID are fixed when the app is built (docs/ENVIRONMENT.md).

# ── Dependencies ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS deps
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# ── Build ─────────────────────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /src
ARG API_ORIGIN=http://api:8080
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID=
ENV API_ORIGIN=$API_ORIGIN \
    NEXT_PUBLIC_GOOGLE_CLIENT_ID=$NEXT_PUBLIC_GOOGLE_CLIENT_ID \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /src/node_modules node_modules
COPY . .
# public/ holds static files; it can be empty, and git does not keep empty folders.
RUN mkdir -p public && npm run build

# ── Run ───────────────────────────────────────────────────────────────────────
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=3000
RUN addgroup -S managio && adduser -S managio -G managio
COPY --from=build --chown=managio:managio /src/.next/standalone ./
COPY --from=build --chown=managio:managio /src/.next/static .next/static
COPY --from=build --chown=managio:managio /src/public public
USER managio
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/sign-in" > /dev/null || exit 1

CMD ["node", "server.js"]

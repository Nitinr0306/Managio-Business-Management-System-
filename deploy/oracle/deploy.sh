#!/bin/sh
# Deploys (or rolls back to) an API image tag on this VM and waits until the API reports ready.
# Usage: ./deploy.sh <tag>, for example ./deploy.sh 6783a7d5-41. The tag must exist on Docker Hub.
set -eu
cd "$(dirname "$0")"

tag="${1:?Usage: ./deploy.sh <image tag>}"
case "$tag" in
  *[!A-Za-z0-9._-]*) echo "Not a valid image tag: $tag" >&2; exit 2 ;;
esac

previous=$(sed -n 's/^API_TAG=//p' .env)
if grep -q '^API_TAG=' .env; then
  sed -i "s/^API_TAG=.*/API_TAG=$tag/" .env
else
  echo "API_TAG=$tag" >> .env
fi

docker compose pull api
docker compose up -d api

# The image's HEALTHCHECK calls /actuator/health/readiness; wait up to 10 minutes for "healthy".
container=$(docker compose ps -q api)
for _ in $(seq 1 120); do
  status=$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || echo missing)
  if [ "$status" = "healthy" ]; then
    echo "Deployed $tag (was ${previous:-none})."
    docker compose up -d caddy
    docker image prune -f > /dev/null
    exit 0
  fi
  sleep 5
done

echo "$tag did not become ready. Last log lines:" >&2
docker compose logs --tail 40 api >&2
echo "Roll back with: ./deploy.sh ${previous:-<previous tag>}" >&2
exit 1

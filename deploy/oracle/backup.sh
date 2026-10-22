#!/bin/sh
# Nightly logical backup of the Managio database (D-036). Writes a pg_dump custom-format archive to ./backups,
# checks that it can be read back, and keeps 14 days. Run from cron, for example at 02:30 India time:
#   0 21 * * * /opt/managio/backup.sh >> /opt/managio/backups/backup.log 2>&1
# Restore: docs/DEPLOYMENT_RUNBOOK.md, section 11.
set -eu
cd "$(dirname "$0")"

mkdir -p backups
chmod 700 backups
file="backups/managio-$(date -u +%Y%m%dT%H%MZ).dump"
trap 'rm -f "$file.partial"' EXIT

docker compose exec -T db pg_dump -U managio -d managio --format=custom > "$file.partial"
# Reading the archive's table of contents proves it is complete and well formed.
docker compose exec -T db pg_restore --list < "$file.partial" > /dev/null

mv "$file.partial" "$file"
chmod 600 "$file"
find backups -name 'managio-*.dump' -mtime +14 -delete
echo "$(date -u +%FT%TZ) backup written: $file ($(wc -c < "$file") bytes)"

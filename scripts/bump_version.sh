#!/usr/bin/env bash
set -euo pipefail

file="app/build.gradle.kts"

if [[ ! -f "${file}" ]]; then
  echo "No se encontró ${file}" >&2
  exit 1
fi

current_code=$(grep -n "versionCode" "${file}" | head -n1 | sed -E 's/.*= ([0-9]+).*/\1/')
if [[ -z "${current_code}" ]]; then
  echo "No se pudo leer versionCode en ${file}" >&2
  exit 1
fi

# No usar un versionCode ya desplegado en Play (evita "Version code X has already been used")
# Fuente 1: scripts/last_deployed_version.txt
# Fuente 2: tags deploy/android/* (si existen en el clone; en CI se hace checkout con fetch-tags)
last_deployed_file="scripts/last_deployed_version.txt"
last_deployed=0
if [[ -f "${last_deployed_file}" ]]; then
  last_deployed=$(grep -E '^[0-9]+' "${last_deployed_file}" | head -n1 | tr -d '\r' || true)
  last_deployed=${last_deployed:-0}
fi

max_tag_deployed=0
if git rev-parse --git-dir >/dev/null 2>&1; then
  tag_codes=$(git tag -l 'deploy/android/*/*' \
    | sed -E 's#^deploy/android/[^/]+/([0-9]+)$#\1#' \
    | grep -E '^[0-9]+$' || true)
  if [[ -n "${tag_codes}" ]]; then
    max_tag_deployed=$(printf '%s\n' "${tag_codes}" | sort -nr | head -n1)
  fi
fi

if [[ "${max_tag_deployed}" -gt "${last_deployed}" ]]; then
  last_deployed="${max_tag_deployed}"
fi

new_code=$((current_code + 1))
if [[ "${new_code}" -le "${last_deployed}" ]]; then
  new_code=$((last_deployed + 1))
  echo "Ajustado versionCode a ${new_code} (max deployed=${last_deployed})"
fi

year=$(date -u +%Y)
month=$(date -u +%-m)
today=$(date -u +%Y-%m-%d)

commit_count=$(git rev-list --count --since="${today} 00:00:00" --until="${today} 23:59:59" HEAD)
if [[ "${commit_count}" -lt 1 ]]; then
  commit_count=1
fi

version_name="${year}.${month}.${commit_count}"

perl -0pi -e "s/versionCode = \\d+/versionCode = ${new_code}/" "${file}"
perl -0pi -e "s/versionName = \"[^\"]+\"/versionName = \"${version_name}\"/" "${file}"

echo "Bumped to versionCode=${new_code} versionName=${version_name}"

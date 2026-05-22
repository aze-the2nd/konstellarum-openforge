#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
if [[ -z "$TAG" ]]; then
  echo "usage: $0 <tag>" >&2
  exit 2
fi

PUBLIC_REPO="${PUBLIC_REPO:-aze-the2nd/blackforest-openforge}"
PRIVATE_REPO_URL="${PRIVATE_REPO_URL:-git@github.com:aze-the2nd/blackforest_engineering.git}"

if curl -fsSL -o /dev/null "https://api.github.com/repos/${PUBLIC_REPO}/releases/tags/${TAG}"; then
  echo "Refusing public push: release already exists on ${PUBLIC_REPO} for tag ${TAG}." >&2
  exit 1
fi

if ! git ls-remote --tags "$PRIVATE_REPO_URL" "refs/tags/${TAG}" | grep -q "refs/tags/${TAG}"; then
  echo "Refusing public push: private tag ${TAG} is missing on ${PRIVATE_REPO_URL}. Push private first." >&2
  exit 1
fi

echo "OK: private tag exists and no public release exists for ${TAG}."

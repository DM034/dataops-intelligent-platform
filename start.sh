#!/usr/bin/env bash

set -euo pipefail

PROFILE="dataops-demo"
CONTEXT="colima-${PROFILE}"
LIMA_HOME="${HOME}/.colima/_lima"
LIMA_INSTANCE="${CONTEXT}"

cd "$(dirname "$0")"

command -v colima >/dev/null 2>&1 || {
  echo "Erreur : Colima n'est pas installe."
  exit 1
}

command -v docker >/dev/null 2>&1 || {
  echo "Erreur : Docker CLI n'est pas installe."
  exit 1
}

echo "Demarrage de Colima (${PROFILE})..."
if ! colima start --profile "${PROFILE}"; then
  echo "Etat VZ/Lima stale detecte, nettoyage force de l'instance arretee..."
  LIMA_HOME="${LIMA_HOME}" limactl stop --force "${LIMA_INSTANCE}" || true
  colima start --profile "${PROFILE}"
fi

echo "Selection du contexte Docker (${CONTEXT})..."
docker context use "${CONTEXT}"

echo "Verification du daemon Docker..."
docker info >/dev/null

echo "Demarrage des services..."
exec docker compose up --build "$@"
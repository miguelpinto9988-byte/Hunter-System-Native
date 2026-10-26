#!/usr/bin/env bash
# Aplica el paquete autorizado Hunter 2000 a una copia clonada del repositorio.
# Requiere git con permiso de push; no cambia la rama main.
set -euo pipefail

EXPECTED_BRANCH="integration/hunter-v43-compose-2000"
ZIP="${1:-}"
if [[ -z "$ZIP" ]]; then
  echo "Uso: bash tools/aplicar_hunter2000_bundle.sh /ruta/Hunter_System_V41_Paquete_Integracion_2000_SVG.zip"
  exit 2
fi
ZIP="$(cd "$(dirname "$ZIP")" && pwd)/$(basename "$ZIP")"
if [[ ! -f "$ZIP" ]]; then echo "No se encuentra el ZIP: $ZIP"; exit 2; fi

cd "$(dirname "$0")/.."
BRANCH="$(git branch --show-current)"
if [[ "$BRANCH" != "$EXPECTED_BRANCH" ]]; then
  echo "ERROR: debes estar en la rama $EXPECTED_BRANCH (actual: $BRANCH)"
  exit 3
fi
if [[ -n "$(git status --porcelain)" ]]; then
  echo "ERROR: existen cambios locales sin confirmar. Guarda tus cambios antes."
  exit 3
fi

echo "Comprobando SHA-256 del paquete..."
ZIP_SHA="$(sha256sum "$ZIP" | awk '{print $1}')"
EXPECTED_SHA="658c8bdc8fe00a67f22c7f7aca8bbdc372d55804d50bb8ac4d5c1600abc22366"
if [[ "$ZIP_SHA" != "$EXPECTED_SHA" ]]; then
  echo "ERROR: checksum diferente, verifica que es el archivo correcto"; exit 4
fi
python3 tools/import_hunter2000_assets.py "$ZIP"
python3 tools/check_hunter2000_complete.py

git add app/src/main/assets/demos30/ app/src/main/assets/catalog-hunter2000-extra.json \
  app/src/main/assets/hunter2000-manifest.json app/src/main/assets/hunter2000-qa-status.json
if git diff --cached --quiet; then
  echo "No hay archivos nuevos para confirmar."
else
  echo "Cambios preparados:"
  git diff --cached --stat | tail -10
  git commit -m "Integrar 2000 SVG y 800 fichas Hunter 2000, pendientes de validacion"
  git push origin "HEAD:$EXPECTED_BRANCH"
fi
echo "Recursos subidos; revisar Actions -> Validar y compilar Hunter 2000 completo."

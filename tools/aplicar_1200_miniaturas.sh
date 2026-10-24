#!/usr/bin/env bash
# Importa el ZIP de 1200 imágenes en la rama V39; no publica el ZIP ni toca main.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
ZIP="${1:-Hunter_System_1200_Miniaturas_Limpias(1).zip}"
if [[ ! -f "$ZIP" ]]; then
  echo "ERROR: falta el ZIP: $ZIP"
  echo "Sube el archivo al explorador de Codespaces en la raiz del repositorio."
  exit 1
fi

BRANCH="redesign/v39-referencia-20261023"
git fetch origin "$BRANCH"
if [[ "$(git branch --show-current)" != "$BRANCH" ]]; then
  git switch "$BRANCH" 2>/dev/null || git switch -c "$BRANCH" --track "origin/$BRANCH"
fi
git pull --ff-only origin "$BRANCH"

python3 tools/importar_1200_miniaturas.py "$ZIP"

git add app/src/main/assets/catalogo_miniaturas_1200.json
git add app/src/main/assets/exercise_thumbnails/
git add MINIATURAS-1200-REPORTE.json
if git diff --cached --quiet; then
  echo "Las 1200 miniaturas ya estaban publicadas."
else
  git commit -m "assets(v39): instalar 1200 miniaturas WebP por ID real"
  git push origin "$BRANCH"
fi

echo
echo "LISTO: 1200 miniaturas publicadas en rama $BRANCH."
echo "Revisar compilacion: https://github.com/miguelpinto9988-byte/Hunter-System-Native/actions"
echo "Cambios antes de fusionar: https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/2"

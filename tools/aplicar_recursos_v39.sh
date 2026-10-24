#!/usr/bin/env bash
# Para usar en Codespaces después de colocar ZIP V39 en el explorador.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
FILE="${1:-Hunter_System_V39_Kotlin_Compose_Referencia_y_Miniaturas(2).zip}"
if [[ ! -f "$FILE" ]]; then echo "No existe: $FILE"; exit 1; fi
git fetch origin
git switch redesign/v39-referencia-20261023 || git switch -c redesign/v39-referencia-20261023 --track origin/redesign/v39-referencia-20261023
git pull --ff-only origin redesign/v39-referencia-20261023
python3 tools/importar_referencia_v39.py "$FILE"
git add app/src/main/assets/v39-miniatures V39-MINIATURAS-REPORTE.json
if git diff --cached --quiet; then echo "No hubo nuevas miniaturas reconocidas."; exit 0; fi
git commit -m "assets(v39): importar miniaturas de referencia compatibles"
git push origin redesign/v39-referencia-20261023
echo "Recursos importados. Revisar reporte V39-MINIATURAS-REPORTE.json"

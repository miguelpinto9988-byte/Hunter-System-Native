#!/usr/bin/env bash
# Importa las 9 partes verificables de Hunter System 38.50 desde GitHub Codespaces.
# No sube archivos ZIP ni archivos de firma. Conserva una rama local de respaldo.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
if [[ "$(git branch --show-current)" != "main" ]]; then
  echo "ERROR: cambia a la rama main antes de ejecutar este importador."
  exit 1
fi
for cmd in git unzip rsync find; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: Falta el comando $cmd."
    exit 1
  fi
done

parts=(
  "Hunter3850-Parte-01-Codigo-Configuracion-y-Disenos.zip"
  "Hunter3850-Parte-02A-Demostraciones.zip"
  "Hunter3850-Parte-02B-Demostraciones.zip"
  "Hunter3850-Parte-03-Videos-Ejercicios.zip"
  "Hunter3850-Parte-04-Videos-Ejercicios.zip"
  "Hunter3850-Parte-05-Videos-Ejercicios.zip"
  "Hunter3850-Parte-06-Videos-Ejercicios.zip"
  "Hunter3850-Parte-07-Videos-Ejercicios.zip"
  "Hunter3850-Parte-08-Videos-Ejercicios.zip"
)
for f in "${parts[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "ERROR: Falta $f en $(pwd)"
    exit 1
  fi
done

tmp="$(mktemp -d -t hunter3850-XXXXXXXX)"
trap 'rm -rf "$tmp"' EXIT
echo "Verificando y extrayendo los 9 ZIP..."
for f in "${parts[@]}"; do
  echo "  - $f"
  unzip -tq "$f" >/dev/null
  unzip -oq "$f" -d "$tmp"
done
src="$tmp/Hunter-System-Native"
if [[ ! -f "$src/settings.gradle.kts" || ! -f "$src/app/build.gradle.kts" ]]; then
  echo "ERROR: Los ZIP no contienen los archivos esperados del proyecto Android."
  exit 1
fi
count="$(find "$src" -type f | wc -l | tr -d ' ')"
if [[ "$count" != "4071" ]]; then
  echo "ERROR: Se esperaban 4071 archivos originales, pero hay $count."
  exit 1
fi
echo "Archivos originales verificados: $count"

# Resguarda commits locales antiguos para poder abandonar cualquier commit
# que contenía el ZIP de más de 100 MB sin perder los archivos de trabajo.
echo "Sincronizando la base con GitHub y creando respaldo local..."
git fetch origin main
git branch "respaldo-hunter-3850-$(date +%Y%m%d-%H%M%S)"
git reset --mixed origin/main

echo "Copiando los archivos Android descomprimidos..."
rsync -a \
  --exclude='.git' \
  --exclude='*.zip' \
  --exclude='*.apk' \
  --exclude='*.jks' \
  --exclude='*.keystore' \
  --exclude='*.p12' \
  --exclude='*.pem' \
  --exclude='*.key' \
  --exclude='.env*' \
  --exclude='local.properties' \
  --exclude='contrasena.txt' \
  --exclude='.gradle' \
  --exclude='build' \
  "$src/" ./

cat >> .gitignore <<'IGNORE'

# Hunter System: archivos locales y privados
*.zip
*.apk
*.jks
*.keystore
*.p12
*.pem
*.key
.env*
local.properties
contrasena.txt
.gradle/
**/build/
IGNORE

# Evita agregar archivos binarios que GitHub rechazaría.
big="$(find . -path './.git' -prune -o -type f -size +99M ! -name '*.zip' -print -quit)"
if [[ -n "$big" ]]; then
  echo "ERROR: Archivo individual mayor de 99 MiB: $big"
  exit 1
fi

echo "Preparando archivos para el commit..."
git add -A
# Verifica que no se haya indexado ninguna clave ni ZIP, incluido un ZIP
# previamente incluido en un commit local.
bad="$(git diff --cached --name-only | grep -Ei '\.(zip|apk|jks|keystore|p12|pem|key)$' || true)"
if [[ -n "$bad" ]]; then
  echo "ERROR: No se debe publicar el siguiente archivo comprimido o privado:"
  echo "$bad"
  exit 1
fi

if git diff --cached --quiet; then
  echo "No hay cambios para publicar (comprueba si ya se subió el proyecto)."
  exit 0
fi
git commit -m "feat: importar Hunter System 38.50 Kotlin y recursos"
echo "Subiendo el proyecto Android a GitHub..."
git push origin main

echo
echo "PUBLICACION COMPLETA: https://github.com/miguelpinto9988-byte/Hunter-System-Native"

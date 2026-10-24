#!/usr/bin/env python3
"""Importa las 1200 miniaturas originales de la referencia V39 al proyecto nativo.

Comprobaciones antes de escribir: ZIP íntegro, manifest sin duplicados, identidad
exacta con catalog.json, ruta segura y WebP auténtico para cada ID. No toca
catálogo de ejercicios, rutinas, misiones, firmas, escenas ni animaciones.
"""
from __future__ import annotations

from pathlib import Path, PurePosixPath
from zipfile import ZipFile, BadZipFile
import hashlib
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
CATALOG = ASSETS / "catalog.json"
SOURCE_MANIFEST = "app/src/main/assets/catalogo_miniaturas_1200.json"
ASSET_PREFIX = "app/src/main/assets/"
DEST_MANIFEST = ASSETS / "catalogo_miniaturas_1200.json"
REPORT = ROOT / "MINIATURAS-1200-REPORTE.json"
IMAGE_RE = re.compile(r"exercise_thumbnails/[A-Za-z0-9_-]+\.webp\Z")

def main() -> None:
    if len(sys.argv) != 2:
        sys.exit("Uso: python3 tools/importar_1200_miniaturas.py ARCHIVO.zip")
    source_zip = Path(sys.argv[1]).expanduser().resolve()
    if not source_zip.is_file():
        sys.exit(f"No existe el archivo: {source_zip}")
    try:
        with ZipFile(source_zip) as archive:
            bad = archive.testzip()
            if bad:
                sys.exit(f"ZIP dañado: {bad}")
            if SOURCE_MANIFEST not in archive.namelist():
                sys.exit("Falta el manifiesto oficial de las 1200 miniaturas.")
            reference = json.loads(archive.read(SOURCE_MANIFEST))
            original = json.loads(CATALOG.read_text(encoding="utf-8"))
            if not isinstance(reference, list) or len(reference) != 1200:
                sys.exit("El manifiesto no tiene exactamente 1200 registros.")
            if not isinstance(original, list) or len(original) != 1200:
                sys.exit("El catálogo Android original no tiene 1200 registros.")
            original_ids = [row["id"] for row in original]
            reference_ids = [row["id"] for row in reference]
            if len(set(original_ids)) != 1200 or len(set(reference_ids)) != 1200:
                sys.exit("Hay ID duplicados. No se importó nada.")
            if set(original_ids) != set(reference_ids):
                sys.exit("Los identificadores no coinciden con el catálogo Android.")
            images = [row.get("imagen", "") for row in reference]
            if len(set(images)) != 1200:
                sys.exit("El manifiesto contiene rutas de imágenes duplicadas.")
            for relative in images:
                if not IMAGE_RE.fullmatch(relative):
                    sys.exit(f"Ruta WebP no permitida: {relative!r}")
                name = ASSET_PREFIX + relative
                if name not in archive.namelist():
                    sys.exit(f"Falta archivo: {name}")
                info = archive.getinfo(name)
                if not (16 <= info.file_size <= 300_000):
                    sys.exit(f"Tamaño de recurso fuera de rango: {name}")
                data = archive.read(name)
                if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
                    sys.exit(f"Imagen WebP inválida: {name}")
            # Solo se escriben archivos después de validar los 1200.
            for relative in images:
                dest = ASSETS / relative
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(archive.read(ASSET_PREFIX + relative))
            DEST_MANIFEST.write_bytes(archive.read(SOURCE_MANIFEST))
    except (OSError, BadZipFile, ValueError, KeyError) as exc:
        sys.exit(f"Error de importación: {exc}")
    size=sum((ASSETS / relative).stat().st_size for relative in images)
    REPORT.write_text(json.dumps({
        "catalogo_original": len(original),
        "miniaturas_importadas": len(images),
        "coincidencias_id": len(set(original_ids) & set(reference_ids)),
        "sin_coincidencia": [],
        "bytes_webp": size,
        "sha256_manifiesto": hashlib.sha256(DEST_MANIFEST.read_bytes()).hexdigest(),
        "observacion": "Las miniaturas son referencias visuales; las instrucciones técnicas continúan en el catálogo y las animaciones originales."
    }, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(f"OK: {len(images)} miniaturas WebP integradas para {len(original_ids)} ejercicios.")
    print(f"Informe: {REPORT.relative_to(ROOT)}")

if __name__ == "__main__":
    main()

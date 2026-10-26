#!/usr/bin/env python3
"""Criterio ESTRICTO de catálogo Hunter 2000, a ejecutar antes de publicar su APK.

Comprueba existencia e integridad, sin afirmar validación biomecánica.
"""
from pathlib import Path
import hashlib, json, re, sys, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
EXPECTED = 2000
NEW = 800


def read_json(relative):
    f = ASSETS / relative
    if not f.is_file():
        raise ValueError(f"Archivo requerido ausente: {relative}")
    return json.loads(f.read_text(encoding="utf-8"))


def verify():
    original = read_json("catalog.json")
    extra = read_json("catalog-hunter2000-extra.json")
    manifest = read_json("hunter2000-manifest.json")
    qa = read_json("hunter2000-qa-status.json")
    if len(original) != 1200 or len(extra) != NEW:
        raise ValueError(f"Conteos inesperados: original={len(original)}, nuevo={len(extra)}")
    records = original + extra
    ids = [e["id"] for e in records]
    if len(ids) != EXPECTED or len(set(ids)) != EXPECTED:
        raise ValueError("No hay exactamente 2000 identificadores únicos")
    if any(
        e.get("sourceExpansion2k") is not True or
        e.get("pendingTechnicalReview") is not True or
        e.get("approvedForAutoplan") is not False or
        e.get("autoProgressionApproved") is not False
        for e in extra
    ):
        raise ValueError("Alguna de las 800 fichas está incorrectamente autorizada")
    if qa.get("validated") is not False or qa.get("pendingNew") != 800:
        raise ValueError("Estado de QA inconsistente; no declarar ejercicios aprobados")
    entries = manifest.get("items", [])
    if len(entries) != EXPECTED or len({x["id"] for x in entries}) != EXPECTED:
        raise ValueError("Manifiesto incompleto")
    if set(ids) != {x["id"] for x in entries}:
        raise ValueError("Manifiesto y catálogo no contienen los mismos IDs")
    checksums = qa.get("SHA256_by_asset", {})
    if len(checksums) != EXPECTED:
        raise ValueError("No existen 2000 hashes SVG")
    for index, record in enumerate(records):
        rel = record.get("demoPath30", "")
        if (not isinstance(rel, str) or
            not re.fullmatch(r"demos30/[a-zA-Z0-9_\-]+\.svg", rel)):
            raise ValueError(f"Ruta SVG incorrecta: {record['id']}: {rel}")
        path = ASSETS / rel
        if not path.is_file():
            raise ValueError(f"SVG ausente ({index+1}): {rel}")
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != checksums.get(rel):
            raise ValueError(f"SHA-256 inválido: {rel}")
        if re.search(rb'<!\s*(DOCTYPE|ENTITY)|<\s*(script|foreignObject)\b', data, re.I):
            raise ValueError(f"Contenido no permitido: {rel}")
        try:
            xml = ET.fromstring(data)
        except ET.ParseError as ex:
            raise ValueError(f"SVG XML inválido: {rel}: {ex}") from ex
        if xml.tag.rsplit("}", 1)[-1] != "svg":
            raise ValueError(f"No es un SVG: {rel}")
    total_svg = len(list((ASSETS / "demos30").glob("*.svg")))
    if total_svg != EXPECTED:
        raise ValueError(f"El directorio tiene {total_svg} SVG, se requieren 2000")
    return {
        "status": "PASS", "catalog_total": EXPECTED, "original": 1200,
        "pending_new": NEW, "svg_verified": EXPECTED,
        "safety": "800 nuevos excluidos de planificación; QA técnica pendiente"
    }


if __name__ == "__main__":
    try:
        print(json.dumps(verify(), ensure_ascii=False, indent=2))
    except Exception as exc:
        print(json.dumps({"status": "FAIL", "reason": str(exc)}, ensure_ascii=False))
        sys.exit(1)

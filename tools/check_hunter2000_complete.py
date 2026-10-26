#!/usr/bin/env python3
"""Auditoría estricta de Hunter2000-Rive, sin confundir conversión con QA anatómica."""
from pathlib import Path
import json, hashlib, re, sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
def read(path):
    file=ASSETS/path
    if not file.is_file():raise ValueError(f"Recurso ausente: {path}")
    return json.loads(file.read_text("utf-8"))
def verify():
    base=read("catalog.json")
    extra=read("catalog-hunter2000-extra.json")
    provenance=read("hunter2000-manifest.json")
    qa=read("hunter2000-qa-status.json")
    manifest=read("demos-rive/manifest.json")
    if len(base)!=1200 or len(extra)!=800:raise ValueError("El catálogo no es 1200 + 800")
    ids=[x["id"] for x in base+extra]
    if len(ids)!=2000 or len(set(ids))!=2000:raise ValueError("IDs duplicados/incompletos")
    if any(not re.fullmatch(r"[A-Za-z0-9_-]{1,100}",x) for x in ids):
        raise ValueError("ID no válido para un archivo Rive")
    if len(provenance.get("items",[]))!=2000 or {x["id"] for x in provenance["items"]}!=set(ids):
        raise ValueError("Proveniencia original incompleta")
    if qa.get("validated") is not False or qa.get("pendingNew")!=800:
        raise ValueError("El estado QA debe continuar pendiente")
    if any(x.get("sourceExpansion2k") is not True or x.get("pendingTechnicalReview") is not True
           or x.get("approvedForAutoplan") is not False
           or x.get("autoProgressionApproved") is not False for x in extra):
        raise ValueError("Se han habilitado los 800 ejercicios sin QA")
    paths=list((ASSETS/"demos-rive").glob("*.riv"))
    actual={f.stem for f in paths}
    if len(paths)!=2000 or actual!=set(ids):raise ValueError(
        f"Se esperaban 2000 Rive unívocos, reales={len(paths)}; faltan={list(set(ids)-actual)[:6]}"
    )
    if manifest.get("count")!=2000 or manifest.get("format")!="Rive":
        raise ValueError("Manifiesto Rive incorrecto")
    fields=manifest.get("files",{})
    if set(fields)!=set(ids):raise ValueError("Manifiesto Rive no corresponde al catálogo")
    for p in paths:
        raw=p.read_bytes()
        if len(raw)<100 or raw[:4]!=b"RIVE":raise ValueError(f"Binario Rive inválido: {p.name}")
        if hashlib.sha256(raw).hexdigest()!=fields[p.stem].get("sha256"):
            raise ValueError(f"SHA256 Rive incorrecto: {p.name}")
        if len(raw)!=fields[p.stem].get("bytes"):
            raise ValueError(f"Tamaño Rive incorrecto: {p.name}")
    old=list((ASSETS/"demos30").glob("*.svg"))+list((ASSETS/"demos30").glob("*.json"))
    if old:raise ValueError(f"Quedan {len(old)} recursos de demostración heredados en la APK")
    return {"status":"PASS","exercise_count":2000,"rive_verified":2000,
            "source_svg_in_app":0,"extra_pending_review":800,
            "appearance":"original_svg_converted_to_rive_NOT_xray","runtime_qa":"PENDIENTE"}
if __name__=="__main__":
    try:print(json.dumps(verify(),ensure_ascii=False,indent=2))
    except Exception as ex:
        print(json.dumps({"status":"FAIL","reason":str(ex)},ensure_ascii=False));sys.exit(1)

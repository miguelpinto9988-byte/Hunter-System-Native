#!/usr/bin/env python3
"""Importa las 12 miniaturas de referencia V39 sin alterar acompañantes ni lógica.

Ejecutar desde la raíz del repositorio:
    python3 tools/importar_referencia_v39.py "/ruta/Hunter_System_V39_Kotlin_Compose_Referencia_y_Miniaturas(2).zip"

No sobreescribe recursos previos salvo el mismo nombre ex_<id>.webp en la carpeta V39.
Solo utiliza coincidencia exacta del ID del catálogo; no asigna dibujos a ejercicios distintos.
"""
from pathlib import Path, PurePosixPath
from zipfile import ZipFile, BadZipFile
import json
import re
import sys
import unicodedata

PREFIX = "Hunter_System_V39_Kotlin_Referencia/app/src/main/res/drawable-nodpi/"
ASSET_ROOT = Path("app/src/main/assets/v39-miniatures")
CATALOG_PATH = Path("app/src/main/assets/catalog.json")
VALID = re.compile(r"ex_([a-z0-9_]+)\.webp$")

def slug(value):
    value = unicodedata.normalize("NFKD", str(value))
    value = "".join(c for c in value if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")

def catalog_entries(root):
    """Obtiene fichas JSON reales; no modifica ni supone la estructura del catálogo."""
    seen = set()
    result = []
    def walk(node):
        if isinstance(node, dict):
            if "id" in node and any(k in node for k in ("name", "title", "nombre")):
                key = str(node["id"])
                if key not in seen:
                    seen.add(key)
                    result.append({"id":key,"name":str(node.get("name",node.get("title",node.get("nombre",""))))})
            for val in node.values(): walk(val)
        elif isinstance(node,list):
            for val in node: walk(val)
    walk(root)
    return result

def main():
    if len(sys.argv) != 2:
        raise SystemExit("Uso: python3 tools/importar_referencia_v39.py <ZIP_V39>")
    archive=Path(sys.argv[1]).expanduser()
    if not archive.is_file(): raise SystemExit("No se encuentra el ZIP.")
    if not CATALOG_PATH.is_file(): raise SystemExit("Falta el catálogo original; no se sustituirá por el de ejemplo.")
    entries=catalog_entries(json.loads(CATALOG_PATH.read_text(encoding="utf-8")))
    by_id={slug(x["id"]):x["id"] for x in entries}
    by_name={}
    for e in entries: by_name.setdefault(slug(e["name"]),[]).append(e["id"])
    total=0;installed=[];unmatched=[]
    with ZipFile(archive) as z:
        names=sorted(n for n in z.namelist() if n.startswith(PREFIX) and n.endswith(".webp"))
        if len(names)!=12:
            raise SystemExit(f"Se esperaban 12 miniaturas en la referencia, encontradas {len(names)}.")
        for name in names:
            filename=PurePosixPath(name).name
            match=VALID.fullmatch(filename)
            if not match: unmatched.append(filename);continue
            token=match.group(1)
            dest_id=by_id.get(token)
            if dest_id is None:
                matches=by_name.get(token,[])
                if len(matches)==1: dest_id=matches[0]
            if dest_id is None or not re.fullmatch(r"[A-Za-z0-9_\-]+",dest_id):
                unmatched.append(filename);continue
            data=z.read(name)
            if len(data)<16 or data[0:4] != b"RIFF" or data[8:12] != b"WEBP":
                raise SystemExit("Archivo WebP inválido: "+filename)
            target=ASSET_ROOT/("ex_"+dest_id+".webp")
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(data)
            installed.append(str(target));total+=1
    report=Path("V39-MINIATURAS-REPORTE.json")
    report.write_text(json.dumps({"catalogo_fichas":len(entries),"miniaturas_referencia":12,
        "instaladas":total,"archivos":installed,"sin_coincidencia":unmatched,
        "nota":"No se han generado miniaturas para las otras fichas del catálogo."},ensure_ascii=False,indent=2),encoding="utf-8")
    print(report.read_text(encoding="utf-8"))
    print("Se conservaron los personajes, videos y catálogo originales.")
if __name__=="__main__":
    main()

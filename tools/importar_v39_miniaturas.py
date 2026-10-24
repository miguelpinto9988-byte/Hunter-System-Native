#!/usr/bin/env python3
"""Importa las 12 miniaturas de ejemplo V39 conservando el catálogo real.

Uso desde la raíz de Hunter-System-Native (Codespaces):
    python3 tools/importar_v39_miniaturas.py "Hunter_System_V39_Kotlin_Compose_Referencia_y_Miniaturas(2).zip"

No importa el catálogo de demostración ni elimina acompañantes, animaciones o ejercicios.
"""
import argparse
import json
import pathlib
import zipfile

SOURCE = "Hunter_System_V39_Kotlin_Referencia/app/src/main/res/drawable-nodpi/"
MAPPING = {
    "flexiones": "pushup",
    "sentadilla_silla": "chair",
    "remo_mancuerna": "row",
    "elevacion_lateral": "lateral",
    "zancadas": "reverselunge",
    "plancha": "plank",
    "burpees": "new46-burpee",
    "puente_gluteos": "bridge",
    "salto_sentadilla": "squatjump",
    "peso_muerto": "rdl",
    "jumping_jacks": "jumpjack",
    "bird_dog": "bird",
}


def import_minis(archive: pathlib.Path, project: pathlib.Path) -> int:
    catalog = json.loads((project / "app/src/main/assets/catalog.json").read_text())
    all_ids = {entry["id"] for entry in catalog}
    assert len(catalog) == 1200 and len(all_ids) == 1200, "Catálogo real inconsistente"
    missing = [eid for eid in MAPPING.values() if eid not in all_ids]
    if missing:
        raise ValueError("IDs no presentes en el catálogo: " + ", ".join(missing))

    dest = project / "app/src/main/assets/v39-miniatures"
    with zipfile.ZipFile(archive) as z:
        if z.testzip():
            raise ValueError("El ZIP está dañado")
        pending = []
        for sample, exercise_id in MAPPING.items():
            name = SOURCE + "ex_" + sample + ".webp"
            try:
                data = z.read(name)
            except KeyError as e:
                raise ValueError("Falta la miniatura: " + name) from e
            if not data.startswith(b"RIFF") or data[8:12] != b"WEBP":
                raise ValueError("Miniatura WebP inválida: " + name)
            pending.append((exercise_id, data))
    dest.mkdir(parents=True, exist_ok=True)
    for exercise_id, data in pending:
        (dest / ("ex_" + exercise_id + ".webp")).write_bytes(data)
    found = {p.name[3:-5] for p in dest.glob("ex_*.webp")}
    covered = len(found & all_ids)
    print(f"Importadas: {len(pending)} miniaturas únicas; cobertura verificada {covered}/1200")
    print("Faltan miniaturas específicas para", 1200-covered, "fichas; mantienen su demostración SVG nativa.")
    return covered


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("zipfile", type=pathlib.Path)
    args = parser.parse_args()
    import_minis(args.zipfile, pathlib.Path.cwd())

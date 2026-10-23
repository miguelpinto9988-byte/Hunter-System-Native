#!/usr/bin/env python3
"""Importa al repositorio el ZIP de código completo de Hunter System 38.50.

Uso:
    python tools/importar_fuentes.py "Hunter-System-38.50-Kotlin-Fuente(2).zip"

Requiere Python 3.9+, Git y una sesión autorizada en GitHub.
Las claves privadas de firma se excluyen.
"""
import argparse
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path, PurePosixPath

REPO = "https://github.com/miguelpinto9988-byte/Hunter-System-Native.git"
PREFIX = "Hunter-System-Native/"
PRIVATE_EXTENSIONS = {".p12", ".jks", ".keystore", ".pem", ".key"}
PRIVATE_FILES = {".env", ".env.local", "local.properties", "contrasena.txt"}
MAX_FILE_SIZE = 95 * 1024 * 1024  # Limite conservador de GitHub por archivo


def command(*parts, cwd=None):
    subprocess.run(parts, cwd=cwd, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="ZIP de fuentes descargado")
    args = parser.parse_args()
    archive = args.archive.expanduser().resolve()
    if not archive.is_file():
        parser.error("No se encuentra el ZIP indicado")

    with tempfile.TemporaryDirectory(prefix="hunter38-50-") as tmp:
        target = Path(tmp) / "repo"
        print("Clonando repositorio...")
        command("git", "clone", "--depth", "1", "--branch", "main", REPO, str(target))
        uploaded = skipped = 0
        with zipfile.ZipFile(archive) as z:
            for entry in z.infolist():
                if entry.is_dir():
                    continue
                if not entry.filename.startswith(PREFIX):
                    raise ValueError("Ruta fuera del proyecto: " + entry.filename)
                rel = PurePosixPath(entry.filename[len(PREFIX):])
                if (not rel.parts or rel.is_absolute()
                        or any(segment in ("", ".", "..", ".git") for segment in rel.parts)):
                    raise ValueError("Ruta insegura: " + entry.filename)
                if (rel.suffix.lower() in PRIVATE_EXTENSIONS
                        or rel.name.lower() in PRIVATE_FILES):
                    print("Se excluye dato privado:", rel)
                    skipped += 1
                    continue
                if entry.file_size > MAX_FILE_SIZE:
                    raise ValueError(f"Archivo demasiado grande para GitHub: {rel}")
                out = target.joinpath(*rel.parts)
                out.parent.mkdir(parents=True, exist_ok=True)
                with z.open(entry) as src, out.open("wb") as dest:
                    shutil.copyfileobj(src, dest, length=1024 * 1024)
                uploaded += 1

        print(f"Archivos preparados: {uploaded}; archivos privados excluidos: {skipped}")
        command("git", "add", "-A", cwd=target)
        diff = subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=target)
        if diff.returncode == 0:
            print("El repositorio ya contiene los mismos archivos.")
            return
        if diff.returncode != 1:
            raise RuntimeError("No se pudo validar los cambios de Git")

        if subprocess.run(["git", "config", "user.name"], cwd=target, capture_output=True).returncode:
            command("git", "config", "user.name", "miguelpinto9988-byte", cwd=target)
        if subprocess.run(["git", "config", "user.email"], cwd=target, capture_output=True).returncode:
            command("git", "config", "user.email", "344119090+miguelpinto9988-byte@users.noreply.github.com", cwd=target)

        command("git", "commit", "-m", "feat: importar Hunter System 38.50 Kotlin y recursos", cwd=target)
        print("Enviando al repositorio. Si Git solicita iniciar sesion, completa la autenticacion.")
        command("git", "push", "origin", "main", cwd=target)
        print("Importacion completa: " + REPO.removesuffix(".git"))


if __name__ == "__main__":
    main()

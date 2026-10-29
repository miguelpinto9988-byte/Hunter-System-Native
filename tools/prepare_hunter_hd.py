#!/usr/bin/env python3
"""Prepara retratos HD para Hunter System V43.12 antes de ensamblar la APK.

Los originales 4K se conservan en art-masters (fuera de los assets de Android).
Ningun cazador dentro de la APK supera 720 x 1080 pixeles; las versiones
anteriores E-S solo se reducen si exceden el limite, sin ampliarlas ni recortarlas.
"""
from pathlib import Path
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
MASTERS = ROOT / "art-masters" / "hunter-4k"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
HD = ASSETS / "hunter-hd"
MAX_SIZE = (720, 1080)
NAMES = ("reader", "regressor", "hunter-male", "hunter-female")
RANKS = ("e", "d", "c", "b", "a", "s")


def encode_hd(source: Path, destination: Path) -> tuple[int, int, int]:
    with Image.open(source) as original:
        image = ImageOps.exif_transpose(original).convert("RGBA")
        image.thumbnail(MAX_SIZE, Image.Resampling.LANCZOS)
        destination.parent.mkdir(parents=True, exist_ok=True)
        image.save(destination, "WEBP", quality=88, method=6, exact=True)
        dimensions = image.size
    with Image.open(destination) as check:
        assert check.size == dimensions and "A" in check.getbands(), destination
    return (*dimensions, destination.stat().st_size)


def main() -> None:
    HD.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        src = MASTERS / f"{name}.webp"
        if not src.is_file():
            raise FileNotFoundError(f"Falta arte maestro del cazador: {src}")
        width, height, size = encode_hd(src, HD / f"{name}.webp")
        if (width, height) != MAX_SIZE:
            raise AssertionError(f"El maestro {name} debe producir 720x1080: {(width, height)}")
        print(f"HD {name}: {width}x{height}, {size:,} bytes", flush=True)

    optimized = 0
    for directory in ("ui33", "ui34"):
        base = ASSETS / directory
        for name in NAMES:
            for rank in RANKS:
                target = base / f"{name}-{rank}.webp"
                if not target.is_file():
                    continue
                with Image.open(target) as image:
                    needs_downsample = image.width > MAX_SIZE[0] or image.height > MAX_SIZE[1]
                if needs_downsample:
                    width, height, size = encode_hd(target, target)
                    optimized += 1
                    print(f"Evolution HD {directory}/{target.name}: {width}x{height}, {size:,} bytes")
    if (ASSETS / "hunter-4k").exists():
        raise AssertionError("No debe haber originales 4K en app/src/main/assets")
    print(f"4 personajes HD listos, {optimized} evoluciones anteriores reducidas.")


if __name__ == "__main__":
    main()

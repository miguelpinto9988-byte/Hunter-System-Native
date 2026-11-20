#!/usr/bin/env python3
"""Comprobación de compatibilidad para la instalación de Hunter 2000."""
from pathlib import Path
import json, sys
from check_hunter2000_complete import verify

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
EXTRA='catalog-hunter2000-extra.json'

def validate_installed():
    dest=ASSETS/EXTRA
    if not dest.exists():
        print('PREPARADO: faltan 800 fichas. Ejercicios originales: 1200.')
        return
    base=json.loads((ASSETS/'catalog.json').read_text('utf8'))
    new=json.loads(dest.read_text('utf8'))
    if len(base)!=1200 or len(new)!=800 or len({x['id'] for x in base+new})!=2000:
        raise ValueError('Catálogo instalado inválido')
    report=verify()
    print(f"INTEGRADOS: {report['catalog_active']} ejercicios con el personaje vectorial.")

def main():
    if len(sys.argv)==2 and sys.argv[1]=='--check':
        validate_installed()
        return
    raise SystemExit('Usa --check. La importación de demostraciones históricas está retirada.')

if __name__=='__main__':
    main()

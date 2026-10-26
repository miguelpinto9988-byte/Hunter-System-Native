#!/usr/bin/env python3
"""Importa recursos Hunter 2000 a la rama Kotlin/Compose; no activa ejercicios sin QA."""
from pathlib import Path
from collections import Counter
import json, zipfile, sys, shutil, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
PREFIX='app/src/main/assets/'
EXTRA='catalog-hunter2000-extra.json'
META=('catalog-hunter2000-extra.json','hunter2000-manifest.json','hunter2000-qa-status.json')

def validate_zip(archive):
    with zipfile.ZipFile(archive) as pack:
        names=pack.namelist()
        svg=[x for x in names if x.startswith(PREFIX+'demos30/') and x.endswith('.svg')]
        if len(svg)!=2000: raise ValueError(f'SVG esperados: 2000; recibidos: {len(svg)}')
        for item in META:
            if PREFIX+item not in names: raise ValueError('Falta '+item)
        base=json.loads((ASSETS/'catalog.json').read_text('utf8'))
        new=json.loads(pack.read(PREFIX+EXTRA))
        if len(base)!=1200 or len(new)!=800: raise ValueError('Se requieren 1200 + 800 fichas')
        ids=[e['id'] for e in base+new]
        if len(set(ids))!=2000: raise ValueError('IDs duplicados')
        if any(e.get('approvedForAutoplan') or e.get('autoProgressionApproved') or not e.get('pendingTechnicalReview') for e in new):
            raise ValueError('Se bloquearon registros nuevos: falta revisión técnica')
        all_names=set(svg)
        if any(PREFIX+e.get('demoPath30','') not in all_names for e in new):
            raise ValueError('SVG faltante para alguna ficha')
        for name in svg:
            root=ET.fromstring(pack.read(name))
            if not root.tag.endswith('svg'): raise ValueError('SVG mal formado: '+name)
        print('Validación paquete: 1200 originales, 800 pendientes, 2000 SVG XML correctos')
        return [x for x in names if x in {PREFIX+e for e in META} or x in all_names]

def validate_installed():
    dest=ASSETS/EXTRA
    if not dest.exists():
        print('PREPARADO: faltan 800 fichas y SVG. Ejercicios activos: 1200.')
        return
    base=json.loads((ASSETS/'catalog.json').read_text('utf8'))
    new=json.loads(dest.read_text('utf8'))
    if len(base)!=1200 or len(new)!=800 or len({x['id'] for x in base+new})!=2000:
        raise ValueError('Catálogo instalado inválido')
    if any(x.get('approvedForAutoplan') or x.get('autoProgressionApproved') for x in new):
        raise ValueError('Se detectaron fichas sin QA aprobadas para planificación')
    missing=[x['id'] for x in base+new if not (ASSETS/x.get('demoPath30','')).is_file()]
    if missing: raise ValueError(f'Faltan {len(missing)} SVG, p.ej.: {missing[:3]}')
    print('INTEGRADOS: 2000 fichas y recursos presentes; 800 nuevos pendientes de QA.')

def main():
    if len(sys.argv)==2 and sys.argv[1]=='--check':
        validate_installed()
        return
    if len(sys.argv)!=2:
        raise SystemExit('Uso: python3 tools/import_hunter2000_assets.py /ruta/Hunter_System_V41_Paquete_Integracion_2000_SVG.zip')
    path=Path(sys.argv[1]).resolve()
    names=validate_zip(path)
    with zipfile.ZipFile(path) as pack:
        for name in names:
            relative=name.removeprefix(PREFIX)
            target=(ASSETS/relative).resolve()
            if not target.is_relative_to(ASSETS.resolve()):
                raise ValueError('Ruta no autorizada: '+relative)
            target.parent.mkdir(parents=True,exist_ok=True)
            with pack.open(name) as src, target.open('wb') as dst: shutil.copyfileobj(src,dst)
    validate_installed()
    print('Recursos incorporados; pendiente compilación, firma y QA en dispositivo.')

if __name__=='__main__':
    main()

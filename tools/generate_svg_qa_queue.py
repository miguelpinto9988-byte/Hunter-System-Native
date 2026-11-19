#!/usr/bin/env python3
"""Exporta las fichas activas para la revisión visual y de reproducción."""
import argparse, csv, hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
FIELDS=['exercise_id','source','svg_asset','sha256','bytes','visual_review','motion_review',
'android_playback_review','biomechanics_review','autoplan','device','android_version','reviewer','review_date','notes']
def read(path): return json.loads((ASSETS/path).read_text(encoding='utf8'))
def build_queue():
    original=read('catalog.json')
    extra=read('catalog-hunter2000-extra.json')
    sports=read('catalog-sports.json')
    aliases=read('exercise-aliases.json')
    if len(original)!=1200 or len(extra)!=800: raise ValueError('Catálogo incompleto')
    sources = {item['id']: source for source, items in (
        ('original', original), ('hunter2000', extra), ('deportivo', sports)
    ) for item in items}
    rows=[]
    for item in sorted(original+extra+sports,key=lambda x:x['id']):
        identity = item['id']
        if identity in aliases:
            continue
        source = sources[identity]
        path = ASSETS / 'media2d' / f'{identity}.svg'
        static = path.read_bytes() if path.is_file() else b''
        rows.append(dict(exercise_id=identity,source=source,
            svg_asset=f'media2d/{identity}.svg' if static else '',
            sha256=hashlib.sha256(static).hexdigest() if static else '',bytes=len(static),
            visual_review='PENDIENTE',motion_review='PENDIENTE',
            android_playback_review='PENDIENTE',biomechanics_review='PENDIENTE',
            autoplan='REGLAS_DEL_PERFIL',
            device='',android_version='',reviewer='',review_date='',
            notes='Demostración de dos gestos' if item.get('sequenceParts') else 'Demostración individual'))
    return rows
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--output',type=Path,default=ROOT/'build/reports/hunter-2000-qa-pendiente.csv')
    args=parser.parse_args()
    rows=build_queue()
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('w',encoding='utf8',newline='') as stream:
        writer=csv.DictWriter(stream,fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    print(json.dumps({'result':'QA_QUEUE_GENERATED_NOT_VALIDATED','rows':len(rows),'pending_visual_review':len(rows)}))
if __name__=='__main__': main()

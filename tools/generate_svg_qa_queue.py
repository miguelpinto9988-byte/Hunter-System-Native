#!/usr/bin/env python3
"""Cola de revisión de ilustraciones estáticas (1200 disponibles; 800 pendientes)."""
import argparse, csv, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
FIELDS=['exercise_id','source','svg_asset','sha256','bytes','visual_review','motion_review',
'android_playback_review','biomechanics_review','autoplan','device','android_version','reviewer','review_date','notes']
def read(path): return json.loads((ASSETS/path).read_text(encoding='utf8'))
def build_queue():
    original=read('catalog.json')
    extra=read('catalog-hunter2000-extra.json')
    if len(original)!=1200 or len(extra)!=800: raise ValueError('Catálogo incompleto')
    rows=[]
    for item in sorted(original+extra,key=lambda x:x['id']):
        new=item.get('sourceExpansion2k',False)
        path=ASSETS/'media2d'/(item['id']+'.svg') if not new else None
        if path is not None and not path.is_file(): raise ValueError('Falta SVG estático: '+item['id'])
        rows.append(dict(exercise_id=item['id'],source='expansion_800' if new else 'original_1200',
            svg_asset=('media2d/'+item['id']+'.svg') if path is not None else '',
            sha256='',bytes=path.stat().st_size if path is not None else 0,
            visual_review='PENDIENTE',motion_review='NO_APLICA',
            android_playback_review='NO_APLICA',biomechanics_review='PENDIENTE',
            autoplan='BLOQUEADO_SIN_QA' if new else 'REGLAS_ORIGINALES',
            device='',android_version='',reviewer='',review_date='',
            notes='Ilustración estática' if not new else 'Sin demostración SVG'))
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
    print(json.dumps({'result':'QA_QUEUE_GENERATED_NOT_VALIDATED','rows':len(rows),'pending_expansion':800}))
if __name__=='__main__': main()

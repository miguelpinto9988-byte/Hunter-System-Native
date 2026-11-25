#!/usr/bin/env python3
"""Genera 2000 filas de QA SVG, siempre pendientes de revisión física."""
import argparse,csv,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
FIELDS=['exercise_id','source','svg_asset','sha256','bytes','visual_review','motion_review',
'android_playback_review','biomechanics_review','autoplan','device','android_version','reviewer','review_date','notes']
def read(path):return json.loads((ASSETS/path).read_text(encoding='utf8'))
def build_queue():
    original=read('catalog.json')
    extra=read('catalog-hunter2000-extra.json')
    sha=read('hunter2000-qa-status.json')['SHA256_by_asset']
    if len(original)!=1200 or len(extra)!=800:raise ValueError('Catálogo incompleto')
    items=original+extra
    if len({x['id'] for x in items})!=2000:raise ValueError('IDs duplicados')
    if any(x.get('approvedForAutoplan') is not False or x.get('autoProgressionApproved') is not False or x.get('pendingTechnicalReview') is not True for x in extra):
        raise ValueError('Ejercicio extra habilitado sin aprobación')
    rows=[]
    for item in sorted(items,key=lambda x:x['id']):
        rel=item['demoPath30']
        raw=(ASSETS/rel).read_bytes()
        digest=hashlib.sha256(raw).hexdigest()
        if digest!=sha.get(rel):raise ValueError('Hash SVG incorrecto: '+rel)
        is_extra=item.get('sourceExpansion2k',False)
        rows.append(dict(exercise_id=item['id'],source='expansion_800' if is_extra else 'original_1200',svg_asset=rel,
        sha256=digest,bytes=len(raw),visual_review='PENDIENTE',motion_review='PENDIENTE',
        android_playback_review='PENDIENTE',biomechanics_review='PENDIENTE',
        autoplan='BLOQUEADO_SIN_QA' if is_extra else 'REGLAS_ORIGINALES',
        device='',android_version='',reviewer='',review_date='',notes=''))
    return rows
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--output',type=Path,default=ROOT/'build/reports/hunter-svg-2000-qa-pendiente.csv')
    args=parser.parse_args()
    rows=build_queue()
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('w',encoding='utf8',newline='') as stream:
        writer=csv.DictWriter(stream,fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    print(json.dumps({'result':'QA_QUEUE_GENERATED_NOT_VALIDATED','rows':len(rows),
          'pending':len(rows),'pending_expansion':sum(x['source']=='expansion_800' for x in rows)}))
if __name__=='__main__':main()

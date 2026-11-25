#!/usr/bin/env python3
"""Exporta las fichas activas para la revisión visual y de reproducción."""
import argparse, csv, hashlib, json
from functools import lru_cache
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
FIELDS=['exercise_id','source','motion_asset','sha256','bytes','visual_review','motion_review',
'android_playback_review','biomechanics_review','autoplan','device','android_version','reviewer','review_date','notes']
def read(path): return json.loads((ASSETS/path).read_text(encoding='utf8'))
def motion_path(exercise_id):
    if exercise_id.startswith('sport-'):
        return 'exercise-actor/sports-poses.json'
    if exercise_id.startswith('hx_'):
        return 'hunter2000-poses.json'
    return f'demos30/{exercise_id}.json'

@lru_cache(maxsize=None)
def asset_digest(path):
    payload = (ASSETS / path).read_bytes()
    return hashlib.sha256(payload).hexdigest(), len(payload)

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
        paths = list(dict.fromkeys(map(motion_path, item.get('sequenceParts') or [identity])))
        fingerprints = [asset_digest(path) for path in paths]
        legacy_sequence = identity.startswith(('hx_cardio_', 'hx_mob_'))
        authored_core = identity.startswith('hx_core_')
        if legacy_sequence:
            note = 'Dos gestos resueltos en el reproductor según la ficha'
        elif authored_core:
            note = 'Posición y movimiento resueltos en el reproductor'
        else:
            note = 'Demostración de dos gestos' if item.get('sequenceParts') else 'Demostración individual'
        rows.append(dict(exercise_id=identity,source=source,
            motion_asset='; '.join(paths),
            sha256='; '.join(digest for digest, _ in fingerprints),
            bytes=sum(size for _, size in fingerprints),
            visual_review='PENDIENTE',motion_review='PENDIENTE',
            android_playback_review='PENDIENTE',biomechanics_review='PENDIENTE',
            autoplan='REGLAS_DEL_PERFIL',
            device='',android_version='',reviewer='',review_date='',
            notes=note))
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

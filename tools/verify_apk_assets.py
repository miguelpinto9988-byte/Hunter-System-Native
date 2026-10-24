#!/usr/bin/env python3
"""Verify original resource hashes in a compiled APK against asset-manifest.json."""
import argparse,hashlib,json,zipfile,pathlib,sys
p=argparse.ArgumentParser();p.add_argument('apk',type=pathlib.Path);p.add_argument('--manifest',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parent.parent/'asset-manifest.json');args=p.parse_args();rows=json.loads(args.manifest.read_text());failures=[]
with zipfile.ZipFile(args.apk) as z:
 names=set(z.namelist())
 for row in rows:
  path='assets/'+row['path']
  if path not in names:failures.append({'path':row['path'],'reason':'missing'})
  elif hashlib.sha256(z.read(path)).hexdigest()!=row['sha256']:failures.append({'path':row['path'],'reason':'changed'})
 executable_assets=[n for n in names if n.startswith('assets/') and pathlib.PurePosixPath(n).suffix.lower() in ['.js','.html','.css']]
 result={'resources_checked':len(rows),'failures':failures,'web_runtime_assets':executable_assets,'apk_sha256':hashlib.sha256(args.apk.read_bytes()).hexdigest(),'apk_bytes':args.apk.stat().st_size};print(json.dumps(result,ensure_ascii=False,indent=2));sys.exit(bool(failures or executable_assets))

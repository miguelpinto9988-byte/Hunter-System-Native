#!/usr/bin/env python3
"""Download the original promotional JPG bytes for PRIVATE TEST BUILDS ONLY.

The original media belongs to Solo Leveling Animation Partners / Netmarble.
Downloading a public promo image does not grant redistribution rights.
Never alter or enhance the source bitmap. Static assets stay inside the APK;
the Android app itself stays fully offline.

Run before compiling:
    python3 tools/download_monarch_boss_originals.py
"""
from __future__ import annotations
import hashlib
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import Request, urlopen

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"app/src/main/assets/monarch-boss-original"
SOURCES={
 "guardia":[
  "https://sololeveling-anime.net/assets/img/special/shadows-visual/igrit.jpg"
 ],
 "fortaleza":[
  "https://sololeveling-anime.net/assets/img/special/shadows-visual/beru.jpg"
 ],
 "trono":[
  "https://channeln.gcdn.netmarble.com/channeln/Upload/b465a733-ad0b-4c0d-843f-bc62b2e59c2f.jpg",
  "https://channeln.gcdn.netmarble.com/channeln/Upload/cb57f23b-2f83-41b2-a402-6338e0b99042.jpg",
 ],
}
HOSTS={"sololeveling-anime.net","channeln.gcdn.netmarble.com"}

def download(source:str)->bytes:
 url=urlparse(source)
 if url.scheme!="https" or url.hostname not in HOSTS:
  raise ValueError("Source host not on allow-list: "+source)
 req=Request(source,headers={
  "User-Agent":"Mozilla/5.0 (HunterSystem private build; source attribution preserved)",
  "Accept":"image/jpeg,image/*;q=0.8,*/*;q=0.1",
  "Referer":"https://sololeveling-anime.net/" if url.hostname=="sololeveling-anime.net" else "https://ch.netmarble.com/",
 })
 with urlopen(req,timeout=35) as response:
  final=urlparse(response.geturl())
  if final.scheme!="https" or final.hostname not in HOSTS:
   raise ValueError("Untrusted redirect")
  data=response.read(8_000_001)
 if not 10000<=len(data)<=8_000_000 or data[:3]!=b"\\xff\\xd8\\xff":
  raise ValueError("Image is not an original JPEG of expected size")
 return data

def main()->None:
 OUT.mkdir(parents=True,exist_ok=True)
 for boss,urls in SOURCES.items():
  errors=[]
  for url in urls:
   try:
    raw=download(url)
    (OUT/(boss+".jpg")).write_bytes(raw)
    print(f"{boss}: {len(raw)} original JPEG bytes, sha256={hashlib.sha256(raw).hexdigest()} source={url}",flush=True)
    break
   except Exception as err:
    errors.append(type(err).__name__+": "+str(err))
  else:
   raise RuntimeError(f"Unable to download original {boss} (fail closed): "+repr(errors))

if __name__=="__main__":main()

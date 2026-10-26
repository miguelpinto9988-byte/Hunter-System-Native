#!/usr/bin/env python3
"""Renderiza exactamente 120 cuadros H.264 desde SVG/SMIL existentes.
Este proceso NO crea .riv ni músculos x-ray ni valida la biomecánica.
Requiere: lxml, cairosvg, pillow, ffmpeg, ffprobe. Salida por lotes, no altera assets.
"""
import argparse,io,json,math,re,subprocess,os,sys
from pathlib import Path
from lxml import etree
from PIL import Image
import cairosvg

NUMBER=re.compile(r"[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?")
HEX=re.compile(r"^#[0-9a-fA-F]{6}$")
def mix(a,b,t):
    if a==b:return a
    if HEX.fullmatch(a) and HEX.fullmatch(b):
        aa,bb=int(a[1:],16),int(b[1:],16)
        return "#"+''.join(f"{round(((aa>>s)&255)*(1-t)+((bb>>s)&255)*t):02x}" for s in (16,8,0))
    na=list(map(float,NUMBER.findall(a)));nb=list(map(float,NUMBER.findall(b)))
    if not na or len(na)!=len(nb):raise ValueError("Topología de fotogramas incompatible")
    idx=iter(range(len(na)))
    return NUMBER.sub(lambda _:f"{na[(i:=next(idx))]*(1-t)+nb[i]*t:.4f}",a)

def ease(b,t):
    x1,y1,x2,y2=b
    def curve(a,c,v):return 3*(1-v)**2*v*a+3*(1-v)*v*v*c+v**3
    lo,hi=0.,1.
    for _ in range(20):
        mid=(lo+hi)/2
        if curve(x1,x2,mid)<t:lo=mid
        else:hi=mid
    return curve(y1,y2,(lo+hi)/2)

class Motion:
    def __init__(self,e):
        self.parent=e.getparent();self.attr=e.get("attributeName")
        self.val=[v.strip() for v in (e.get("values") or "").split(";") if v.strip()]
        if self.attr not in ("d","cx","cy","transform","opacity","fill") or len(self.val)<2:
            raise ValueError("Animación SVG no soportada: "+str(self.attr))
        self.times=[float(v) for v in (e.get("keyTimes") or "").split(";") if v.strip()]
        if len(self.times)!=len(self.val):self.times=[i/(len(self.val)-1) for i in range(len(self.val))]
        self.duration=float((e.get("dur") or "4.8s").removesuffix("s"))
        self.begin=float((e.get("begin") or "0s").removesuffix("s"))
        self.mode=e.get("calcMode","linear")
        self.splines=[list(map(float,x.split())) for x in (e.get("keySplines") or "").split(";") if x.strip()]
        if self.duration<=0:raise ValueError("dur inválida")
        self.parent.remove(e)
    def apply(self,sec):
        phase=((sec-self.begin)/self.duration)%1.
        i=max(0,min(len(self.val)-2,next((j for j in range(len(self.times)-2,-1,-1) if self.times[j]<=phase),0)))
        span=self.times[i+1]-self.times[i]
        t=max(0.,min(1.,(phase-self.times[i])/span)) if span>0 else 0.
        if self.mode=="discrete":t=0.
        if self.mode=="spline" and i<len(self.splines) and len(self.splines[i])==4:
            t=ease(self.splines[i],t)
        self.parent.set(self.attr,mix(self.val[i],self.val[i+1],t))

def convert(svg:Path,dest:Path,frames=120,fps=25,size=360):
    raw=svg.read_bytes()
    if re.search(rb"<!\s*(DOCTYPE|ENTITY)",raw,re.I):raise ValueError("DTD/ENTITY prohibido")
    parser=etree.XMLParser(resolve_entities=False,load_dtd=False,no_network=True)
    root=etree.fromstring(raw,parser)
    if not root.tag.endswith("}svg"):raise ValueError("SVG inválido")
    for script in root.xpath('//*[local-name()="script"]'):script.getparent().remove(script)
    motions=[Motion(e) for e in root.xpath('//*[local-name()="animate"]')]
    if not motions:raise ValueError("SVG sin animación")
    cycle=max(x.duration for x in motions)
    dest.parent.mkdir(parents=True,exist_ok=True)
    temp=dest.with_name(dest.name+".part.mp4")
    proc=subprocess.Popen(["ffmpeg","-hide_banner","-loglevel","error","-y",
        "-f","rawvideo","-pix_fmt","rgb24","-s",f"{size}x{size}","-r",str(fps),
        "-i","pipe:0","-an","-c:v","libx264","-preset","veryfast","-crf","28",
        "-pix_fmt","yuv420p","-frames:v",str(frames),"-movflags","+faststart",str(temp)],
        stdin=subprocess.PIPE,stderr=subprocess.PIPE)
    try:
        for i in range(frames):
            for m in motions:m.apply(cycle*i/frames)
            png=cairosvg.svg2png(bytestring=etree.tostring(root),output_width=size,output_height=size)
            img=Image.open(io.BytesIO(png)).convert("RGBA")
            base=Image.new("RGBA",(size,size),(6,9,15,255));base.alpha_composite(img)
            proc.stdin.write(base.convert("RGB").tobytes())
        proc.stdin.close();err=proc.stderr.read().decode(errors="replace")
        if proc.wait()!=0:raise RuntimeError("FFmpeg: "+err[-200:])
        inspect=subprocess.check_output(["ffprobe","-v","error","-count_frames","-select_streams","v:0",
            "-show_entries","stream=nb_read_frames","-of","default=noprint_wrappers=1:nokey=1",str(temp)],
            text=True).strip()
        if int(inspect)!=120:raise ValueError(f"Cuadros: {inspect}, esperados 120")
        temp.replace(dest)
        return dict(id=svg.stem,frames=120,fps=fps,seconds=120/fps,bytes=dest.stat().st_size,
            status="source_svg_rendered",xray=False,riv=False)
    except Exception:
        proc.kill();proc.wait();temp.unlink(missing_ok=True);raise

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source",type=Path,required=True);p.add_argument("--output",type=Path,required=True)
    p.add_argument("--start",type=int,default=0);p.add_argument("--count",type=int,default=100)
    p.add_argument("--size",type=int,default=360);p.add_argument("--frames",type=int,default=120)
    p.add_argument("--manifest-only",action="store_true")
    opt=p.parse_args();assert opt.frames==120
    source=sorted(opt.source.glob("*.svg")) if opt.source.is_dir() else [opt.source]
    assert len(source)==2000 if opt.source.is_dir() else len(source)==1
    selected=source[opt.start:opt.start+opt.count]
    opt.output.mkdir(parents=True,exist_ok=True)
    results=[]
    for idx,s in enumerate(selected):
        if opt.manifest_only:
            results.append(dict(id=s.stem,status="planned",frames=120))
            continue
        dest=opt.output/(s.stem+".mp4")
        try:
            report=convert(s,dest,size=opt.size);results.append(report)
            print(f"{idx+1}/{len(selected)} OK {s.stem}",flush=True)
        except Exception as error:
            results.append(dict(id=s.stem,status="failed",error=str(error)[:400]))
            print(f"{idx+1}/{len(selected)} ERROR {s.stem}: {error}",file=sys.stderr,flush=True)
    report=dict(source_count=len(source),processed=len(results),frame_count=120,format="MP4 H.264",
        style="original SVG (NOT anatomical X-Ray)",rive_generated=0,entries=results)
    (opt.output/"manifest.json").write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(json.dumps(dict(total=len(results),successful=sum(r["status"]=="source_svg_rendered" for r in results),
        failed=sum(r["status"]=="failed" for r in results))))
    return 1 if any(r["status"]=="failed" for r in results) else 0

if __name__=="__main__":sys.exit(main())

/** Subdivide exterior stations without changing original control stations.
 * Uses monotone Hermite interpolation for left/right widths; centers are linear.
 * No writes to original motion, proportions, or layer-2 assets.
 */
function monotoneTangents(x,y){
  const n=x.length,d=[];for(let i=0;i<n-1;i++)d.push((y[i+1]-y[i])/(x[i+1]-x[i]));
  const m=new Array(n);m[0]=d[0];m[n-1]=d[n-2];
  for(let i=1;i<n-1;i++)m[i]=d[i-1]*d[i]<=0?0:2/(1/d[i-1]+1/d[i]);
  for(let i=0;i<n-1;i++){
    if(d[i]===0){m[i]=0;m[i+1]=0;continue;}
    const a=m[i]/d[i],b=m[i+1]/d[i],q=a*a+b*b;
    if(q>9){const t=3/Math.sqrt(q);m[i]=t*a*d[i];m[i+1]=t*b*d[i];}
  }
  return m;
}
function smoothStations(stations, subdivisions=4){
  if(!Number.isInteger(subdivisions)||subdivisions<1||subdivisions>16)throw Error('subdivisions 1..16');
  if(stations.length<3)throw Error('at least three stations');
  const pts=stations.map(s=>{
    if(!Array.isArray(s)||s.length!==4||s.some(v=>!Number.isFinite(v))||s[2]<=0||s[3]<=0)throw Error('invalid station');
    return s;
  });
  const dist=[0];for(let i=1;i<pts.length;i++){
    const len=Math.hypot(pts[i][0]-pts[i-1][0],pts[i][1]-pts[i-1][1]);
    if(len<1e-6)throw Error('duplicate center');dist.push(dist[i-1]+len);
  }
  const mL=monotoneTangents(dist,pts.map(p=>p[2])),mR=monotoneTangents(dist,pts.map(p=>p[3]));
  const result=[];
  for(let i=0;i<pts.length-1;i++){
    const a=pts[i],b=pts[i+1],h=dist[i+1]-dist[i];
    for(let j=0;j<subdivisions;j++){
      const t=j/subdivisions,u=1-t;
      const interp=(idx,m)=> (2*t*t*t-3*t*t+1)*a[idx]+(t*t*t-2*t*t+t)*h*m[i]+(-2*t*t*t+3*t*t)*b[idx]+(t*t*t-t*t)*h*m[i+1];
      const p=[a[0]*u+b[0]*t,a[1]*u+b[1]*t,interp(2,mL),interp(3,mR)];
      if(p[2]<=0||p[3]<=0)throw Error('nonpositive width');
      result.push(p);
    }
  }
  result.push(pts[pts.length-1].slice());
  return result;
}
if(typeof module!=='undefined')module.exports={smoothStations,monotoneTangents};

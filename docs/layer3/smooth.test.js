const assert=require('node:assert/strict');
const {smoothStations}=require('./smooth.js');
const input=[[100,100,8,10],[101,120,20,14],[103,150,10,8],[105,180,7,6]];
const result=smoothStations(input,8);
assert.equal(result.length,25);
for(let i=0;i<input.length;i++)assert.deepEqual(result[i*8],input[i]);
for(let i=0;i<input.length-1;i++)for(let j=0;j<8;j++){
  const p=result[i*8+j];for(const k of [2,3])assert.ok(p[k]>=Math.min(input[i][k],input[i+1][k])-1e-9&&p[k]<=Math.max(input[i][k],input[i+1][k])+1e-9);
}
assert.throws(()=>smoothStations([[0,0,2,2],[0,0,3,3],[2,2,4,4]]),/duplicate/);
assert.throws(()=>smoothStations(input,0),/subdivisions/);
console.log('PASS: stations preserved; width monotonicity; invalid inputs rejected');

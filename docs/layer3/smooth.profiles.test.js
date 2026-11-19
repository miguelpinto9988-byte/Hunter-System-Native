// Regression test: every real calf exterior profile, not synthetic-only examples.
// Run: node docs/layer3/smooth.profiles.test.js
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {smoothStations} = require('./smooth.js');
const data = JSON.parse(fs.readFileSync(path.join(__dirname,'profiles/calf.json'),'utf8'));
assert.equal(data.profiles.length,24);
let original=0, refined=0;
for(const profile of data.profiles){
  const stations=profile.stations;
  const smooth=smoothStations(stations,4);
  original+=stations.length; refined+=smooth.length;
  assert.equal(smooth.length,(stations.length-1)*4+1,profile.id);
  for(let i=0;i<stations.length;i++)assert.deepEqual(smooth[i*4],stations[i],profile.id);
  for(let i=0;i<stations.length-1;i++){
    for(let j=0;j<4;j++){
      const p=smooth[i*4+j];
      assert(p.every(Number.isFinite),profile.id);
      assert(p[0]>0&&p[0]<720&&p[1]>0&&p[1]<800,profile.id);
      for(const k of [2,3]){
        assert(p[k]>=Math.min(stations[i][k],stations[i+1][k])-1e-8,profile.id);
        assert(p[k]<=Math.max(stations[i][k],stations[i+1][k])+1e-8,profile.id);
      }
    }
  }
}
assert.equal(original,104);
assert.equal(refined,344);
console.log('PASS: 24 exterior profiles; 104 original stations preserved; 344 interpolated stations; bounded widths');

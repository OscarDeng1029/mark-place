import {writeFile} from 'node:fs/promises';
const base = 'http://127.0.0.1:3212';
const start = Date.now();
async function post(path, data) {
  const response = await fetch(base + path, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(data), signal:AbortSignal.timeout(240000)});
  const result = await response.json();
  if(!response.ok) throw new Error(`${response.status}: ${result.code}: ${result.error}`);
  return result;
}
try {
  const parsed = await post('/api/parse', {url:'https://xhslink.cn/o/9RRYFuh4f5H', city:'深圳', recognitionMode:'auto', deferMatching:true});
  console.log(JSON.stringify({stage:'read-and-recognize',title:parsed.note.title,count:parsed.shops.length,method:parsed.method,seconds:(Date.now()-start)/1000}));
  const match = await post('/api/match', {shops:parsed.shops, city:'深圳', title:parsed.note.title, text:parsed.note.text});
  parsed.shops = match.shops; parsed.warnings.push(...match.warnings);
  await writeFile(new URL('./tmp/live-import.json',import.meta.url), JSON.stringify(parsed,null,2));
  console.log(JSON.stringify({stage:'match',shops:match.shops.map(s=>({name:s.name,matched:!!s.place,address:s.place?.address})),seconds:(Date.now()-start)/1000}));
  const p = match.shops.find(s=>s.place)?.place;
  if(p) {
    const z=14,n=2**z,x=Math.floor((p.gcjLng+180)/360*n),rad=p.gcjLat*Math.PI/180,y=Math.floor((1-Math.log(Math.tan(rad)+1/Math.cos(rad))/Math.PI)/2*n);
    const tile = await fetch(`${base}/tiles/road/${z}/${x}/${y}.png`);
    console.log(JSON.stringify({stage:'real-tile',status:tile.status,type:tile.headers.get('content-type'),bytes:(await tile.arrayBuffer()).byteLength}));
  }
} catch(error) { console.error(error.message); process.exitCode=1; }

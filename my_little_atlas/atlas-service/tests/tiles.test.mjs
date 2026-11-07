import {test} from 'node:test';
import assert from 'node:assert/strict';
import {getTile} from '../lib/tiles.mjs';

const picture=(bytes=[137,80,78,71],contentType='image/png')=>new Response(new Uint8Array(bytes),{headers:{'Content-Type':contentType,'Cache-Control':'no-store'}});

test('瓦片接口拒绝已移除的卫星源、未知源和越界坐标，不发出网络请求',async()=>{
  let count=0;const fake=async()=>{count++;return picture();};
  for(const args of [['france',2,1,1],['satellite',2,1,1],['evil',2,1,1],['hot',2,1,1],['standard',2,1,1],['road',2,4,1],['road',19,0,0],['road',1,-1,0]])await assert.rejects(()=>getTile(...args,fake),/无效/);
  assert.equal(count,0);
});

test('相同高德瓦片合并请求并在缓存期内复用，保持二进制完整',async()=>{
  let calls=0;let release;const gate=new Promise(resolve=>release=resolve);const bytes=new Uint8Array([137,80,78,71,0,255]);
  const fake=async()=>{calls++;await gate;return new Response(bytes,{headers:{'Content-Type':'image/png','Cache-Control':'public, max-age=3600'}});};
  const a=getTile('road',3,1,2,fake),b=getTile('road',3,1,2,fake);release();
  const [one,two]=await Promise.all([a,b]);assert.equal(calls,1);assert.deepEqual(one.bytes,Buffer.from(bytes));assert.equal(one,two);assert.equal(one.contentType,'image/png');
  assert.equal(await getTile('road',3,1,2,fake),one);assert.equal(calls,1);
});

test('错误响应不作为地图图片缓存，后续可以重试',async()=>{
  await assert.rejects(()=>getTile('road',3,3,3,async()=>new Response('blocked',{status:403})),/暂时无法连接/);
  const good=await getTile('road',3,3,3,async()=>picture([1,2]));assert.equal(good.cacheControl,'no-store');
});

test('高德普通地图主源超时后读取备用源，保留中文样式与瓦片坐标',async()=>{
  const calls=[],bytes=[137,80,78,71,13,10];
  const fake=async url=>{
    calls.push(new URL(url));
    if(calls.length===1)throw new DOMException('timeout','TimeoutError');
    return picture(bytes);
  };
  const tile=await getTile('road',5,20,20,fake);
  assert.deepEqual(calls.map(u=>u.hostname),['webrd01.is.autonavi.com','webrd02.is.autonavi.com']);
  for(const url of calls){assert.equal(url.pathname,'/appmaptile');assert.deepEqual(Object.fromEntries(url.searchParams),{lang:'zh_cn',size:'1',scale:'1',style:'7',x:'20',y:'20',z:'5'});}
  assert.deepEqual(tile.bytes,Buffer.from(bytes));
});

test('主源失败后短期跳过主源，后续瓦片直接使用可达备用源',async()=>{
  const calls=[];
  const fake=async url=>{
    calls.push(new URL(url));
    return calls.at(-1).hostname==='webrd01.is.autonavi.com'?new Response('blocked',{status:503}):picture([1,2]);
  };
  await getTile('road',5,21,20,fake);await getTile('road',5,22,20,fake);
  assert.equal(calls.length,3);assert.equal(calls[2].hostname,'webrd02.is.autonavi.com');assert.equal(calls[2].searchParams.get('x'),'22');
});

test('所有高德瓦片源失败返回明确的 502 错误，恢复后同一请求可以重试',async()=>{
  let available=false;const calls=[];
  const fake=async url=>{calls.push(url);if(!available)throw new TypeError('network unreachable');return picture([3,4]);};
  await assert.rejects(()=>getTile('road',5,23,20,fake),e=>e.status===502&&e.code==='TILE_UNAVAILABLE');
  assert.equal(calls.length,2);available=true;
  assert.deepEqual((await getTile('road',5,23,20,fake)).bytes,Buffer.from([3,4]));
});

test('主源返回非图片或过大数据时改读备用源，不缓存错误内容',async()=>{
  for(const [x,bad] of [[24,()=>new Response('<html>error</html>',{headers:{'Content-Type':'text/html'}})],[25,()=>new Response(new Uint8Array(1_000_001),{headers:{'Content-Type':'image/png'}})]]){
    const calls=[];
    const fake=async url=>{calls.push(url);return calls.length===1?bad():picture([5,6]);};
    assert.deepEqual((await getTile('road',5,x,20,fake)).bytes,Buffer.from([5,6]));assert.equal(calls.length,2);
  }
});

import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {createServer} from '../server.mjs';
const html=await readFile(new URL('./fixtures/mobile-coffee-note.html',import.meta.url),'utf8');

async function withService(config, fetcher, run) {
  const server=createServer(config,{fetcher});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  try {
    const base=`http://127.0.0.1:${server.address().port}`;
    await run(async(path,data,headers={})=>{
      const res=await fetch(base+path,{method:data===undefined?'GET':'POST',headers:{'Content-Type':'application/json',...headers},body:data===undefined?undefined:JSON.stringify(data)});
      return {status:res.status,data:await res.json()};
    });
  } finally { await new Promise(resolve=>server.close(resolve)); }
}

test('native API exposes configuration readiness without credentials and blocks cross-site writes',async()=>{
  await withService({amapKey:'private-map',aiKey:'private-model',aiBase:'https://ai.example',aiModel:'model'},()=>{throw Error('no network expected')},async call=>{
    const config=await call('/api/config');
    assert.deepEqual(config.data,{mapConfigured:true,aiConfigured:true,model:'model'});
    assert.ok(!JSON.stringify(config).includes('private-'));
    assert.equal((await call('/api/parse',{text:'test',city:'深圳'},{Origin:'https://attacker.example'})).status,403);
  });
});

test('native staged import reads seven shops before matching; matching failure preserves parse response',async()=>{
  await withService({},async()=>new Response(html,{headers:{'Content-Type':'text/html'}}),async call=>{
    const parse=await call('/api/parse',{url:'https://xhslink.cn/o/example',city:'深圳',recognitionMode:'rules',deferMatching:true});
    assert.equal(parse.status,200);
    assert.equal(parse.data.shops.length,7);
    assert.ok(parse.data.shops.every(s=>s.place===null));
    assert.equal(parse.data.matchingDeferred,true);
    const match=await call('/api/match',{city:'深圳',shops:parse.data.shops,title:parse.data.note.title,text:parse.data.note.text});
    assert.equal(match.status,422); assert.equal(match.data.code,'KEY_REQUIRED');
    assert.equal(parse.data.shops.length,7);
  });
});

test('native API propagates restricted-note errors without a success-shaped empty result',async()=>{
  await withService({},async()=>new Response('<html><title>小红书</title>请登录后查看</html>'),async call=>{
    const response=await call('/api/parse',{url:'https://www.xiaohongshu.com/explore/123',city:'深圳',deferMatching:true});
    assert.ok(response.status>=400);
    assert.ok(response.data.error);
    assert.equal(response.data.shops,undefined);
  });
});

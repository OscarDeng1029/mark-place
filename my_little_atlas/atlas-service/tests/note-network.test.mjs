import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {fetchNote} from '../lib/core.mjs';

const html=await readFile(new URL('./fixtures/mobile-coffee-note.html',import.meta.url),'utf8');
const input='https://xhslink.cn/o/fixture',fast={retryDelays:[0,0]};
const failed=code=>Object.assign(new TypeError('fetch failed'),{cause:Object.assign(new Error('connection failure'),{code})});

test('笔记连接短暂重置会自动重试，成功后返回正文',async()=>{
  let calls=0;
  const note=await fetchNote(input,async()=>{if(++calls<3)throw failed('ECONNRESET');return new Response(html);},fast);
  assert.equal(calls,3);assert.equal(note.title,'福田/罗湖美味咖啡店');
});

test('重试当前跳转页，保留签名，不重新请求已完成的短链',async()=>{
  const target='https://www.xiaohongshu.com/discovery/item/6a1afe70000000003803474b?xsec_token=fixture%2Bsignature';
  const urls=[];
  const note=await fetchNote(input,async url=>{
    urls.push(url.href);
    if(urls.length===1)return new Response(null,{status:302,headers:{location:target}});
    if(urls.length===2)throw failed('UND_ERR_CONNECT_TIMEOUT');
    return new Response(html);
  },fast);
  assert.deepEqual(urls,[input,target,target]);assert.equal(note.sourceUrl,target);
});

test('响应正文中途断开会重新读取，避免把不完整页面当作需要登录',async()=>{
  let calls=0;
  const note=await fetchNote(input,async()=>++calls===1?new Response(new ReadableStream({start(controller){controller.error(failed('UND_ERR_SOCKET'));}})):new Response(html),fast);
  assert.equal(calls,2);assert.match(note.text,/银勺/);
});

test('临时 HTTP 错误可恢复，限流或上游故障持续时返回明确状态',async()=>{
  const statuses=[503,429,200];
  const note=await fetchNote(input,async()=>new Response(html,{status:statuses.shift()}),fast);
  assert.equal(note.title,'福田/罗湖美味咖啡店');
  let calls=0;
  await assert.rejects(()=>fetchNote(input,async()=>{calls++;return new Response('',{status:502});},fast),e=>e.status===503&&e.code==='NOTE_UPSTREAM_BUSY');
  assert.equal(calls,3);
});

test('联网权限和证书错误直接准确提示，不误报超时或反复请求',async()=>{
  for(const [cause,code] of [['EACCES','NOTE_NETWORK_DENIED'],['EPERM','NOTE_NETWORK_DENIED'],['UNABLE_TO_VERIFY_LEAF_SIGNATURE','NOTE_TLS_ERROR']]){
    let calls=0;
    await assert.rejects(()=>fetchNote(input,async()=>{calls++;throw failed(cause);},fast),e=>e.code===code&&!e.message.includes('超时'));
    assert.equal(calls,1);
  }
  await assert.rejects(()=>fetchNote(input,async()=>{throw Object.assign(new TypeError('fetch failed'),{cause:new AggregateError([{code:'EACCES'}])});},fast),e=>e.code==='NOTE_NETWORK_DENIED');
});

test('DNS、超时和连接失败分别返回错误码，且重试次数有上限',async()=>{
  for(const [cause,code] of [['ENOTFOUND','NOTE_DNS_ERROR'],['EAI_AGAIN','NOTE_DNS_ERROR'],['UND_ERR_CONNECT_TIMEOUT','NOTE_TIMEOUT'],['ECONNREFUSED','NOTE_CONNECTION_ERROR']]){
    let calls=0;
    await assert.rejects(()=>fetchNote(input,async()=>{calls++;throw failed(cause);},fast),e=>e.code===code);
    assert.equal(calls,3);
  }
});

test('登录、访问受限和不安全跳转不会重试',async()=>{
  for(const create of [()=>new Response('',{status:403}),()=>new Response('<html>扫码登录</html>'),()=>new Response(null,{status:302,headers:{location:'https://127.0.0.1/private'}})]){
    let calls=0;
    await assert.rejects(()=>fetchNote(input,async()=>{calls++;return create();},fast));assert.equal(calls,1);
  }
});

test('单次请求和整个跳转流程均有超时上限，避免无限重试',async()=>{
  const hung=async(url,{signal})=>new Promise((resolve,reject)=>{
    signal.addEventListener('abort',()=>reject(signal.reason),{once:true});
    // A real socket keeps the event loop alive; reproduce that with a short timer.
    const keepAlive=setTimeout(()=>resolve(new Response(html)),1000);
    signal.addEventListener('abort',()=>clearTimeout(keepAlive),{once:true});
  });
  let calls=0;
  await assert.rejects(()=>fetchNote(input,async(...args)=>{calls++;return hung(...args);},{attemptTimeoutMs:15,totalTimeoutMs:35,retryDelays:[0,0]}),e=>e.code==='NOTE_TIMEOUT');
  assert.ok(calls>=2&&calls<=3);
});

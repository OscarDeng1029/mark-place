import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { extractUrl, readInitialState, parseNoteHtml, fetchNote, extractShopsByRules, extractShops, wgsToGcj, gcjToWgs, searchPlaces, chooseExact, limitedText } from '../lib/core.mjs';
import { distanceMeters, wgsToGcj as browserWgsToGcj } from './fixtures/reference-geo.mjs';
const mobileHtml=await readFile(new URL('./fixtures/mobile-coffee-note.html',import.meta.url),'utf8');
const coffeeNames=['银勺SilverSpoon（福田香梅北）','TonTon小屋（福田圆岭）','ouur偶屋（福田下梅林）','Albee Bakery (福田圆岭）','PattPatt（福田岗厦）','Muchh咖啡馆（罗湖洪湖）','好运到咖啡士多（福田八卦岭）'];

test('支持完整分享内容、HTTP 短链和签名参数',()=>{
  const u=extractUrl('来看看这篇笔记！ http://xhslink.com/a/Ab12，复制打开小红书');
  assert.equal(u.href,'https://xhslink.com/a/Ab12');
  assert.equal(extractUrl('https://www.xiaohongshu.com/explore/123?xsec_token=a%2Bb&xsec_source=pc').searchParams.get('xsec_token'),'a+b');
});
test('支持用户提供的 xhslink.cn 繁体分享文案和短链',()=>{
  const input='福田/罗湖美味咖啡店 分享几家去过的 适合工作日下午... https://xhslink.cn/o/9RRYFuh4f5H 複製後開啟小紅書查看筆記';
  assert.equal(extractUrl(input).href,'https://xhslink.cn/o/9RRYFuh4f5H');
  assert.equal(extractUrl('http://www.xhslink.cn/o/9RRYFuh4f5H').href,'https://www.xhslink.cn/o/9RRYFuh4f5H');
});
test('xhslink.cn 跳转到签名笔记时保留完整查询参数并读取正文',async()=>{
  const destination='https://www.xiaohongshu.com/discovery/item/abc?xsec_token=a%2Bb&xsec_source=app_share';
  const urls=[];
  const note=await fetchNote('https://xhslink.cn/o/9RRYFuh4f5H',async url=>{
    urls.push(url.href);
    return urls.length===1?new Response(null,{status:302,headers:{location:destination}}):new Response('window.__INITIAL_STATE__='+JSON.stringify({note:{noteDetailMap:{abc:{note:{title:'福田/罗湖美味咖啡店',desc:'1. 梧桐咖啡\n2. 树下咖啡'}}}}}));
  });
  assert.deepEqual(urls,['https://xhslink.cn/o/9RRYFuh4f5H',destination]);
  assert.equal(note.title,'福田/罗湖美味咖啡店');assert.equal(extractShopsByRules(note.text).length,2);
});
test('拒绝内网、伪造域名、凭证和异常端口',()=>{
  for(const s of ['https://127.0.0.1/a','https://www.xiaohongshu.com.evil.test/a','https://xhslink.cn.evil.test/a','https://xhslink.cn@evil.test/a','https://xiaohongshu.com@evil.test/a','https://user:pass@xiaohongshu.com/a','https://xiaohongshu.com:8080/a','没有链接'])assert.throws(()=>extractUrl(s));
});
test('SSR JSON 中只替换值 undefined，不损坏字符串，也不执行脚本',()=>{
  const state=readInitialState('<script>window.__INITIAL_STATE__={"note":{"desc":"undefined 和 } 都是文本","a":undefined,"b":"a\\\"b"}};global.evil=1;</script>');
  assert.equal(state.note.desc,'undefined 和 } 都是文本');assert.equal(state.note.a,null);assert.equal(state.note.b,'a"b');assert.equal(global.evil,undefined);
  assert.equal(readInitialState('window.__INITIAL_STATE__={get x(){return 1}}'),null);
});
test('读取目标笔记的 SSR 正文和图片',()=>{
  const html='window.__INITIAL_STATE__='+JSON.stringify({note:{noteDetailMap:{abc:{note:{noteId:'abc',title:'上海咖啡地图',desc:'1. 梧桐咖啡\n2. 晴天面馆',user:{nickname:'周末'},imageList:[{urlDefault:'https://sns-img.xhscdn.com/a.jpg'}]}}}}});
  const note=parseNoteHtml(html,'https://www.xiaohongshu.com/explore/abc');
  assert.match(note.text,/梧桐咖啡/);assert.equal(note.author,'周末');assert.equal(note.images.length,1);
});
test('读取公开移动分享页的 noteData 结构、原始换行、作者与图片',()=>{
  const note=parseNoteHtml(mobileHtml,'https://www.xiaohongshu.com/discovery/item/6a1afe70000000003803474b');
  assert.equal(note.title,'福田/罗湖美味咖啡店');assert.equal(note.author,'测试作者');
  assert.match(note.text,/\np1  银勺/);assert.equal(note.images[0],'https://sns-webpic-qc.xhscdn.com/fixture.jpg');
});
test('用户笔记 p1 到 p7 全部识别并保留位置，咖啡与烘焙类型正确',()=>{
  const note=parseNoteHtml(mobileHtml,'https://www.xiaohongshu.com/discovery/item/6a1afe70000000003803474b');
  const shops=extractShopsByRules(note.text);
  assert.deepEqual(shops.map(x=>x.name),coffeeNames);assert.equal(shops[3].category,'dessert');
  assert.equal(shops[0].category,'coffee');assert.equal(shops[4].category,'coffee');
  assert.match(shops[6].evidence,/^p7 好运到/);
});
test('元信息正文换行被压平时，仍可识别 p1 到 p7 而不合并多家店',()=>{
  const note=parseNoteHtml(mobileHtml,'https://www.xiaohongshu.com/discovery/item/6a1afe70000000003803474b');
  assert.deepEqual(extractShopsByRules(note.text.replace(/\s+/g,' ')).map(x=>x.name),coffeeNames);
});
test('短链读取使用公开移动页，跳转签名保持不变，实际移动 SSR 可以解析',async()=>{
  let calls=0;
  const note=await fetchNote('https://xhslink.cn/o/9RRYFuh4f5H',async(url,options)=>{
    assert.match(options.headers['User-Agent'],/iPhone/);
    if(++calls===1)return new Response(null,{status:302,headers:{location:'https://www.xiaohongshu.com/discovery/item/6a1afe70000000003803474b?xsec_token=signed%2Bvalue'}});
    assert.equal(url.searchParams.get('xsec_token'),'signed+value');return new Response(mobileHtml);
  });
  assert.deepEqual(extractShopsByRules(note.text).map(s=>s.name),coffeeNames);
});
test('公开页要求登录时停止跳转并提示，不把登录页当作笔记',async()=>{
  let calls=0;
  await assert.rejects(()=>fetchNote('https://xhslink.cn/o/fixture',async()=>{calls++;return new Response(null,{status:302,headers:{location:'https://www.xiaohongshu.com/login?redirectPath=note'}});}),/需要登录/);
  assert.equal(calls,1);
});
test('HTML 元信息支持属性次序、单双引号、实体和登录页检测',()=>{
  const note=parseNoteHtml(`<meta content='A &amp; B' property='og:title'><meta name="description" content="店名：树下咖啡&#10;一杯热拿铁">`);
  assert.equal(note.title,'A & B');assert.match(note.text,/树下咖啡\n/);
  assert.equal(parseNoteHtml('<meta name="description" content="小红书是年轻人的生活方式平台">').text,'');
});
test('逐跳检查短链跳转目标，禁止跳转到内网',async()=>{
  let calls=0;
  await assert.rejects(()=>fetchNote('https://xhslink.com/a/x',async()=>{calls++;return new Response('',{status:302,headers:{location:'http://127.0.0.1/private'}});}),/仅支持/);
  assert.equal(calls,1);
});
test('登录页面返回可操作的正文替代提示',async()=>{
  await assert.rejects(()=>fetchNote('https://www.xiaohongshu.com/explore/abc',async()=>new Response('<html>扫码登录</html>')),/粘贴正文/);
});
test('限制页面读取体积，防止超大响应',async()=>{
  await assert.rejects(()=>limitedText(new Response('x'.repeat(200)),100),/内容过大/);
});
test('识别编号、店名标签、括号和同句多店，保留分店信息并去重',()=>{
  const shops=extractShopsByRules('周末探店\n1. 梧桐咖啡（安福路店）\n地址：安福路 123 号\n② 晴天面馆：推荐牛肉面\n店名：小鹿烘焙\n推荐【白日书店】和【山里火锅店】\n📍梧桐咖啡（安福路店）\n📍地址：某某路');
  assert.equal(shops.length,5);assert.equal(shops[0].name,'梧桐咖啡（安福路店）');assert.equal(shops[0].addressHint,'安福路 123 号');
  assert.equal(shops[1].category,'food');assert.equal(shops[2].category,'dessert');assert.equal(shops[4].category,'food');
});
test('不把没有证据的自由正文伪装成成功识别',()=>{
  assert.deepEqual(extractShopsByRules('今天的阳光好好，和朋友一起散步，真是快乐的一天。'),[]);
});
test('模型失效时保留规则结果并提示',async()=>{
  const result=await extractShops({text:'1. 梧桐咖啡',images:[]},{aiKey:'test',aiBase:'https://example.test/v1',aiModel:'test'},async()=>new Response('',{status:500}));
  assert.equal(result.method,'rules');assert.equal(result.shops[0].name,'梧桐咖啡');assert.match(result.warnings[0],/暂时不可用/);
});
test('正文模型生成的虚构店名不会进入结果，且保留实际店名',async()=>{
  for(const aiVision of [false,true]){
  const result=await extractShops({text:'推荐梧桐咖啡，一家安静的小店。',images:[]},{aiKey:'test',aiBase:'https://example.test/v1',aiModel:'test',aiVision},async()=>Response.json({choices:[{message:{content:JSON.stringify({shops:[{name:'梧桐咖啡',evidence:'推荐梧桐咖啡，一家安静的小店。'},{name:'凭空创造的面包店',evidence:'没有这段正文'}]})}}]}));
  assert.equal(result.shops.length,1);assert.equal(result.shops[0].name,'梧桐咖啡');assert.equal(result.shops[0].evidence,'推荐梧桐咖啡，一家安静的小店。');
  }
});
test('WGS-84 与高德坐标往返误差小于一米，浏览器转换保持一致',()=>{
  const start=[121.447,31.213],gcj=wgsToGcj(...start),back=gcjToWgs(...gcj);
  assert.ok(distanceMeters({lng:start[0],lat:start[1]},{lng:back[0],lat:back[1]})<1);
  assert.deepEqual(gcj,browserWgsToGcj(...start));assert.deepEqual(wgsToGcj(-74,40),[-74,40]);
});
test('同名多个分店不自动标注；唯一同名候选可匹配',()=>{
  const s={name:'梧桐咖啡'},a={id:'a',name:'梧桐咖啡'},b={id:'b',name:'梧桐咖啡'};
  assert.equal(chooseExact(s,[a,b]),null);assert.equal(chooseExact(s,[a]),a);assert.equal(chooseExact(s,[{name:'梧桐咖啡（A店）'}]),null);
  assert.equal(chooseExact(s,[a,{name:'梧桐咖啡（B店）'}]),null);
});
test('真实 POI 请求限制城市并转换高德坐标',async()=>{
  const results=await searchPlaces('梧桐咖啡','上海','test',async input=>{
    assert.equal(input.searchParams.get('citylimit'),'true');assert.equal(input.searchParams.get('city'),'上海');
    return Response.json({status:'1',pois:[{id:'a',name:'梧桐咖啡',location:'121.45,31.21',pname:'上海市',cityname:'上海市',adname:'徐汇区',address:'某某路'}]});
  });
  assert.equal(results.length,1);assert.notEqual(results[0].lng,121.45);assert.equal(results[0].gcjLng,121.45);assert.equal(results[0].address,'上海市徐汇区某某路');
});
test('错误 Key 返回明确的检索提示',async()=>{
  await assert.rejects(()=>searchPlaces('x','上海','bad',async()=>Response.json({status:'0',info:'INVALID_USER_KEY'})),/Web 服务 Key/);
});
test('距离计算在相同点、跨日界线与地球对跖点仍有效',()=>{
  assert.equal(distanceMeters({lat:31,lng:121},{lat:31,lng:121}),0);
  assert.ok(distanceMeters({lat:0,lng:179.9},{lat:0,lng:-179.9})<23000);
  assert.ok(Number.isFinite(distanceMeters({lat:90,lng:0},{lat:-90,lng:0})));
});

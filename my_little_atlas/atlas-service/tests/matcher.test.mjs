import {test} from 'node:test';
import assert from 'node:assert/strict';
import {autoMatchShops,adjudicateMatches,searchQueries} from '../lib/matcher.mjs';
const config={aiProvider:'custom',aiBase:'https://model.test/v1',aiKey:'test-secret',aiModel:'test-model'};
const place=(id,name,address='深圳市福田区八卦岭')=>({id,name,address,city:'深圳市',district:'福田区',lat:22.56,lng:114.08,gcjLat:22.557,gcjLng:114.085,type:'餐饮服务;咖啡厅'});
const shop=(id,name='好运到咖啡士多',hint='福田八卦岭')=>({id,name,addressHint:hint,evidence:`${name}（${hint}）`,category:'coffee'});
const response=matches=>Response.json({choices:[{message:{content:JSON.stringify({matches})},finish_reason:'stop'}]});

test('AI 根据笔记的八卦岭线索选择第二个高德候选，并直接给出可标注门店',async()=>{
  const shops=[shop('s1')],wrong=place('p1','好运到咖啡士多（梅林店）','深圳市福田区上梅林'),right=place('p2','好运到咖啡士多（鹏基八卦岭工业区店）');
  const result=await autoMatchShops(shops,{title:'咖啡店',text:'去好运到咖啡士多，福田八卦岭'},'深圳','map-secret',config,{searchIntervalMs:0,search:async()=>[wrong,right],fetcher:async(_url,opts)=>{
    const request=JSON.parse(opts.body),context=JSON.parse(request.messages[1].content);assert.equal(context.shops[0].candidates.length,2);assert.match(context.shops[0].addressHint,/八卦岭/);
    return response([{shopId:'s1',candidateId:'p2',confidence:.97,reason:'八卦岭分店与正文一致'}]);
  }});
  assert.equal(shops[0].place.id,'p2');assert.equal(shops[0].status,'matched');assert.equal(shops[0].matching.method,'ai');assert.equal(result.summary.matched,1);
});
test('高德候选 ID 必须来自该店列表，模型不能创造坐标或串用其他店的候选',async()=>{
  const a={...shop('a'),candidates:[place('pa','A店')]},b={...shop('b'),candidates:[place('pb','B店')]};
  const decisions=await adjudicateMatches([a,b],{text:''},'深圳',config,async()=>response([{shopId:'a',candidateId:'pb',confidence:1,reason:'选错别店'},{shopId:'b',candidateId:'invented',confidence:1,reason:'虚构ID',lat:1,lng:2}]));
  assert.equal(decisions.get('a').place,null);assert.equal(decisions.get('b').place,null);
});
test('低置信、无理由、重复 shopId 和缺失条目均不会直接标记错误位置',async()=>{
  const shops=['a','b','c','d'].map(id=>({...shop(id),candidates:[place('p'+id,'咖啡店')]}));
  const rows=await adjudicateMatches(shops,{text:''},'深圳',config,async()=>response([{shopId:'a',candidateId:'pa',confidence:.3,reason:'不确定'},{shopId:'b',candidateId:'pb',confidence:.99,reason:''},{shopId:'c',candidateId:'pc',confidence:1,reason:'yes'},{shopId:'c',candidateId:'pc',confidence:1,reason:'yes'}]));
  assert.ok([...rows.values()].every(x=>x.place===null));
});
test('AI 自动用英文别名重试检索，第二轮可直接补全门店',async()=>{
  const shops=[shop('s1','TonTon小屋','福田圆岭')],queries=[];let judging=0;
  await autoMatchShops(shops,{text:'p2 TonTon小屋（福田圆岭）'},'深圳','key',config,{searchIntervalMs:0,search:async q=>{queries.push(q);return q==='TonTon 圆岭'?[place('real','TON TON HOUSE','深圳市福田区园岭')]:[];},fetcher:async()=>{
    judging++;return response([{shopId:'s1',candidateId:judging===1?null:'real',confidence:judging===1?0:.96,reason:judging===1?'尝试英文检索':'同品牌且圆岭位置一致',searchQueries:['TonTon 圆岭']}]);
  }});
  assert.ok(queries.includes('TonTon 圆岭'));assert.equal(shops[0].place.id,'real');assert.equal(judging,2);
});
test('高德报错会保留已提取店铺并明确提示，不伪造地址',async()=>{
  const shops=[shop('s1')];const result=await autoMatchShops(shops,{text:''},'深圳','bad',config,{searchIntervalMs:0,search:async()=>{throw new Error('bad');}});
  assert.equal(shops[0].place,null);assert.equal(result.summary.matched,0);assert.match(result.warnings.join(''),/高德/);
});
test('模型故障不自动取搜索第一名，返回可自动重试的状态',async()=>{
  const shops=[shop('s1')];const result=await autoMatchShops(shops,{text:''},'深圳','key',config,{searchIntervalMs:0,search:async()=>[place('p1','好运到咖啡士多')],fetcher:async()=>new Response(null,{status:401})});
  assert.equal(shops[0].place,null);assert.equal(shops[0].matching.method,'error');assert.match(result.warnings.join(''),/API Key/);
});
test('候选去重、过滤跨城与非法坐标；无模型时只采用唯一同名',async()=>{
  const shops=[shop('s1','银勺')],p=place('p1','银勺');
  const result=await autoMatchShops(shops,{text:''},'深圳','key',{}, {searchIntervalMs:0,search:async()=>[p,p,{...p,id:'bad',city:'广州市'},{...p,id:'invalid',lat:NaN}]});
  assert.equal(shops[0].candidates.length,1);assert.equal(shops[0].place.id,'p1');assert.equal(result.summary.method,'exact');
});
test('没有高德 Key 时不发网络请求；中英文检索方案保留别名',async()=>{
  const shops=[shop('s1')];let count=0;await autoMatchShops(shops,{text:''},'深圳','',config,{search:async()=>count++});assert.equal(count,0);
  const queries=searchQueries(shop('x','银勺SilverSpoon（香梅北店）','福田香梅北'));
  assert.ok(queries.includes('SilverSpoon'));assert.ok(queries.includes('银勺'));
});
test('扩大检索找到的新候选不会被旧的无关候选挤出候选列表',async()=>{
  const shops=[shop('s','TargetCafe','八卦岭')];let calls=0,round=0;
  await autoMatchShops(shops,{text:'TargetCafe 八卦岭'},'深圳','key',config,{searchIntervalMs:0,search:async q=>{
    calls++;if(q==='TargetCafe Coffee')return [place('right','TargetCafe（八卦岭店）')];
    return Array.from({length:8},(_,i)=>place(`unrelated-${calls}-${i}`,`无关商店${i}`));
  },fetcher:async(_url,options)=>{
    round++;const context=JSON.parse(JSON.parse(options.body).messages[1].content);
    if(round===2)assert.ok(context.shops[0].candidates.some(p=>p.id==='right'));
    return response([{shopId:'s',candidateId:round===1?null:'right',confidence:round===1?0:.99,reason:round===1?'待扩大检索':'品牌与分店均符合',searchQueries:['TargetCafe Coffee']}]);
  }});
  assert.equal(shops[0].place.id,'right');
});

test('保留英文词间空格作为不同检索词，多轮 AI 根据已搜候选继续换词',async()=>{
  const shops=[shop('s','TonTon小屋','圆岭')],queries=[];let round=0;
  const result=await autoMatchShops(shops,{text:'TonTon小屋（圆岭）'},'深圳','key',config,{searchIntervalMs:0,search:async q=>{
    queries.push(q);
    if(q==='TonTon House 园岭')return [place('right','TON TON HOUSE','深圳市福田区园岭')];
    if(q==='Ton Ton')return [place('wrong','Ton Ton','深圳市罗湖区南湖')];
    return [];
  },fetcher:async(_url,options)=>{
    round++;const context=JSON.parse(JSON.parse(options.body).messages[1].content),current=context.shops[0];
    if(round>1)assert.ok(current.attemptedQueries.includes('Ton Ton'));
    return response([{shopId:'s',candidateId:round===3?'right':null,confidence:round===3?.96:0,reason:round===3?'英文品牌且园岭一致':'现有候选位置不符，继续换词',searchQueries:round===1?['Ton Ton']:['TonTon House 园岭']}]);
  }});
  assert.equal(round,3);assert.ok(queries.includes('Ton Ton'));assert.ok(queries.includes('TonTon'));assert.equal(shops[0].place.id,'right');assert.equal(result.summary.matched,1);
});

test('同批搜索临时失败不会放弃其他查询，跨城包含相同字也不能入选',async()=>{
  const shops=[shop('s')];let calls=0;
  await autoMatchShops(shops,{text:''},'深圳','key',{}, {searchIntervalMs:0,search:async()=>{
    if(++calls===1)throw new Error('temporary timeout');
    return [place('right','好运到咖啡士多'),{...place('wrong','好运到咖啡士多'),city:'深圳市其他城'}];
  }});
  assert.equal(shops[0].place.id,'right');assert.equal(shops[0].candidates.length,1);
});

test('常见七店笔记找到品牌候选后不重复检索，仍由 AI 逐店核对',async()=>{
  const shops=Array.from({length:7},(_,i)=>shop('s'+i,'CornerCafe'+i,'八卦岭'));
  let searches=0,judgments=0;
  const result=await autoMatchShops(shops,{text:'七家咖啡店'},'深圳','key',config,{searchIntervalMs:0,search:async q=>{
    searches++;const name=q.split(' ')[0];return [place('p'+name,name)];
  },fetcher:async(_url,options)=>{
    judgments++;const context=JSON.parse(JSON.parse(options.body).messages[1].content);
    assert.equal(context.shops.length,7);
    return response(context.shops.map(s=>({shopId:s.shopId,candidateId:s.candidates[0].id,confidence:.98,reason:'品牌与八卦岭地址一致'})));
  }});
  assert.equal(searches,7);assert.equal(judgments,1);assert.equal(result.summary.matched,7);
  assert.ok(shops.every(s=>s.matching.method==='ai'));
});

test('同一批重复关键词合并网络请求，空格别名仍分别搜索',async()=>{
  const shops=[shop('a','TonTon小屋','园岭'),shop('b','TonTon小屋','园岭')];
  const calls=[];
  await autoMatchShops(shops,{text:''},'深圳','key',config,{searchIntervalMs:0,search:async q=>{calls.push(q);return [place('p','TonTon小屋','深圳市福田区园岭')];},fetcher:async(_url,options)=>{
    const context=JSON.parse(JSON.parse(options.body).messages[1].content);
    return response(context.shops.map(s=>({shopId:s.shopId,candidateId:'p',confidence:.99,reason:'园岭与原文一致'})));
  }});
  assert.equal(calls.length,1);assert.equal(shops.filter(s=>s.place).length,2);
});

test('名称相近但位置冲突时不直接匹配，AI 会继续检索正确分店',async()=>{
  const shops=[shop('a','CornerCafe','八卦岭')];let judgments=0;
  await autoMatchShops(shops,{text:'CornerCafe 八卦岭'},'深圳','key',config,{searchIntervalMs:0,search:async q=>q==='CornerCafe 八卦街'?[place('right','CornerCafe','深圳市福田区八卦岭')]:[place('wrong','CornerCafe','深圳市罗湖区')],fetcher:async()=>{
    judgments++;return response([{shopId:'a',candidateId:judgments===1?null:'right',confidence:.99,reason:judgments===1?'位置冲突':'八卦岭一致',searchQueries:['CornerCafe 八卦街']}]);
  }});
  assert.equal(shops[0].place.id,'right');assert.equal(judgments,2);
});

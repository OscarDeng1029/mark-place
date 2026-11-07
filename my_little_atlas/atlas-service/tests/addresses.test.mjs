import {test} from 'node:test';
import assert from 'node:assert/strict';
import {geocodeAddress,reverseAddress} from '../lib/addresses.mjs';
import {wgsToGcj} from '../lib/core.mjs';

test('详细地址查询保留高德地址精度，转换坐标并过滤无效结果',async()=>{
  const results=await geocodeAddress('园岭路12号','深圳','fixture-key',async url=>{
    assert.equal(url.pathname,'/v3/geocode/geo');assert.equal(url.searchParams.get('address'),'园岭路12号');assert.equal(url.searchParams.get('city'),'深圳');
    return Response.json({status:'1',geocodes:[{formatted_address:'广东省深圳市福田区园岭路12号',city:'深圳市',district:'福田区',location:'114.09,22.56',level:'门牌号'},{location:'181,91'},{location:''}]});
  });
  assert.equal(results.length,1);assert.equal(results[0].level,'门牌号');assert.equal(results[0].source,'geocode');assert.notEqual(results[0].lng,114.09);assert.equal(results[0].gcjLng,114.09);
});
test('地图选点查询使用转换后的高德坐标，保留用户原始选点',async()=>{
  const lng=114.08,lat=22.56,converted=wgsToGcj(lng,lat);
  const place=await reverseAddress(lng,lat,'key',async url=>{
    assert.equal(url.pathname,'/v3/geocode/regeo');assert.equal(url.searchParams.get('location'),converted.join(','));
    return Response.json({status:'1',regeocode:{formatted_address:'深圳市福田区园岭路',addressComponent:{city:'深圳市',district:'福田区'}}});
  });
  assert.equal(place.lng,lng);assert.equal(place.lat,lat);assert.equal(place.source,'pin');assert.equal(place.gcjLng,converted[0]);assert.equal(place.address,'深圳市福田区园岭路');
});
test('地址服务无权限、无结果与选点非法时返回可操作错误',async()=>{
  await assert.rejects(()=>geocodeAddress('园岭路','深圳','',async()=>{throw Error('should not call');}),/Web 服务 Key/);
  await assert.rejects(()=>geocodeAddress('园岭路','深圳','key',async()=>Response.json({status:'0'})),/权限及配额/);
  await assert.rejects(()=>reverseAddress(114,22,'key',async()=>Response.json({status:'1',regeocode:{}})),/移动图钉或手动补充/);
  for(const [lng,lat] of [['114',22],[NaN,22],[181,91],[-74,40]])await assert.rejects(()=>reverseAddress(lng,lat,'key'),/大陆范围/);
  assert.deepEqual(await geocodeAddress('园岭路','深圳','key',async()=>Response.json({status:'1',geocodes:[]})),[]);
});

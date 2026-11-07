import { AppError, gcjToWgs, wgsToGcj } from './core.mjs';

const location = value => {
  if(typeof value!=='string'||!/^\d+(?:\.\d+)?,\d+(?:\.\d+)?$/.test(value))return null;
  const [lng,lat]=value.split(',').map(Number);
  return lng>0&&lng<=180&&lat>0&&lat<=90?[lng,lat]:null;
};
const text = value => typeof value==='string'?value:'';
async function amap(path,params,key,fetcher){
  if(!key)throw new AppError('请先连接高德 Web 服务 Key。',422,'KEY_REQUIRED');
  const url=new URL(`https://restapi.amap.com/v3/geocode/${path}`);
  url.search=new URLSearchParams({...params,key}).toString();
  let response;
  try{response=await fetcher(url,{signal:AbortSignal.timeout(12000)});}catch{throw new AppError('地址查询连接超时，请重试。',502,'MAP_ERROR');}
  if(!response.ok)throw new AppError('地址服务暂时不可用，请稍后重试。',502,'MAP_ERROR');
  const data=await response.json();
  if(data.status!=='1')throw new AppError('高德地址查询失败，请检查 Key 的 Web 服务权限及配额。',422,'MAP_ERROR');
  return data;
}
export async function geocodeAddress(address,city,key,fetcher=fetch){
  const data=await amap('geo',{address,city,batch:'false'},key,fetcher);
  return (data.geocodes||[]).flatMap((p,index)=>{
    const coords=location(p.location);if(!coords)return [];
    const [gcjLng,gcjLat]=coords,[lng,lat]=gcjToWgs(...coords);
    return [{id:`address:${gcjLng},${gcjLat}:${index}`,name:address,address:text(p.formatted_address),city:text(p.city),district:text(p.district),level:text(p.level),type:'手动地址',source:'geocode',lng,lat,gcjLng,gcjLat}];
  });
}
export async function reverseAddress(lng,lat,key,fetcher=fetch){
  if(!Number.isFinite(lng)||!Number.isFinite(lat)||lng<72||lng>138||lat<.8||lat>56)throw new AppError('请选择中国大陆范围内的位置。');
  const [gcjLng,gcjLat]=wgsToGcj(lng,lat);
  const data=await amap('regeo',{location:`${gcjLng},${gcjLat}`,extensions:'base',radius:'100',roadlevel:'0'},key,fetcher);
  const address=text(data.regeocode?.formatted_address);
  if(!address)throw new AppError('没有查到此点的地址，请移动图钉或手动补充地址。',422,'ADDRESS_NOT_FOUND');
  const part=data.regeocode.addressComponent||{};
  return {id:`pin:${lng.toFixed(6)},${lat.toFixed(6)}`,name:'地图选点',address,city:text(part.city)||text(part.province),district:text(part.district),source:'pin',lng,lat,gcjLng,gcjLat};
}

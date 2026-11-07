import { AppError } from './core.mjs';
const cache=new Map(),pending=new Map();
const unavailable=new WeakMap();
const sources={
  road:['https://webrd01.is.autonavi.com','https://webrd02.is.autonavi.com']
};
async function readTile(host,style,z,x,y,fetcher){
  const url=`${host}/appmaptile?${new URLSearchParams({lang:'zh_cn',size:'1',scale:'1',style:'7',x:String(x),y:String(y),z:String(z)})}`;
  const response=await fetcher(url,{signal:AbortSignal.timeout(9000),headers:{'User-Agent':'ShidianMap/1.0 (local desktop note-to-map application)',Accept:'image/png,image/jpeg,image/webp,image/avif'}});
  const contentType=(response.headers.get('content-type')||'').split(';')[0].trim().toLowerCase();
  if(!response.ok || !['image/png','image/jpeg','image/webp','image/avif'].includes(contentType)){await response.body?.cancel();throw new AppError('地图瓦片暂时不可用。',502);}
  const reader=response.body.getReader(),chunks=[];let size=0;
  try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>1_000_000)throw new AppError('地图瓦片过大。',502);chunks.push(value);}}
  finally{await reader.cancel().catch(()=>{});}
  const control=response.headers.get('cache-control')||'',ttl=Math.min(Number(/max-age=(\d+)/.exec(control)?.[1]||3600),86400);
  return {bytes:Buffer.concat(chunks),contentType,until:Date.now()+ttl*1000,cacheControl:/no-store|no-cache/.test(control)?'no-store':`public, max-age=${ttl}`};
}
export async function getTile(style,z,x,y,fetcher=fetch){
  if(!Object.hasOwn(sources,style)||![z,x,y].every(Number.isInteger)||z<0||z>18||x<0||y<0||x>=2**z||y>=2**z)throw new AppError('无效的地图瓦片。',400);
  const key=`${style}/${z}/${x}/${y}`,cached=cache.get(key);
  if(cached && cached.until>Date.now())return cached;
  if(pending.has(key))return pending.get(key);
  if(pending.size>=48)throw new AppError('地图正在加载，请稍后重试。',503);
  const task=(async()=>{
    if(!unavailable.has(fetcher))unavailable.set(fetcher,new Map());
    const health=unavailable.get(fetcher),healthy=sources[style].filter(host=>(health.get(host)||0)<=Date.now());
    const hosts=healthy.length?healthy:sources[style];let tile;
    for(const host of hosts){
      try{tile=await readTile(host,style,z,x,y,fetcher);health.delete(host);break;}
      catch{health.set(host,Date.now()+60000);}
    }
    if(!tile)throw new AppError('地图服务暂时无法连接，请重试。',502,'TILE_UNAVAILABLE');
    if(tile.cacheControl!=='no-store'){
      cache.delete(key);cache.set(key,tile);
      while(cache.size>256)cache.delete(cache.keys().next().value);
    }
    return tile;
  })();
  pending.set(key,task);
  try{return await task;}finally{pending.delete(key);}
}

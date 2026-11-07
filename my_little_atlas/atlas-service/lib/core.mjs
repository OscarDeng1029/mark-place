import { recognizeWithModel } from './model.mjs';
import { setTimeout as delay } from 'node:timers/promises';
const XHS_HOSTS = new Set(['xiaohongshu.com', 'www.xiaohongshu.com', 'xhslink.com', 'www.xhslink.com', 'xhslink.cn', 'www.xhslink.cn']);
// 公开移动分享页会返回笔记 SSR；桌面页面可能直接跳转至登录页。
const SHARE_USER_AGENT = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1';
export class AppError extends Error {
  constructor(message, status = 400, code = 'INPUT_ERROR') { super(message); this.status = status; this.code = code; }
}
export function extractUrl(input) {
  const match = String(input).match(/https?:\/\/[^\s<>"'，。；！）】]+/i);
  if (!match) throw new AppError('请粘贴小红书笔记链接或完整的分享内容。');
  let url;
  try { url = new URL(match[0].replace(/[),.;!?]+$/, '')); } catch { throw new AppError('链接格式不正确，请重新复制。'); }
  if (!XHS_HOSTS.has(url.hostname) || url.username || url.password || (url.port && url.port !== '443')) {
    throw new AppError('仅支持小红书笔记链接，以及 xhslink.com、xhslink.cn 分享短链接。');
  }
  // 小红书分享短链常使用 HTTP；在发起网络请求前升级至 HTTPS。
  if (!['https:', 'http:'].includes(url.protocol)) throw new AppError('不支持的链接格式。');
  url.protocol = 'https:';
  return url;
}
export async function limitedText(response, max = 2_500_000) {
  const reader = response.body.getReader(); const chunks = []; let size = 0;
  try {
    while (true) {
      const { value, done } = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > max) throw new AppError('页面内容过大，请改用粘贴正文。', 422, 'PAGE_TOO_LARGE');
      chunks.push(value);
    }
  } finally { await reader.cancel().catch(() => {}); }
  return Buffer.concat(chunks).toString('utf8');
}
const NOTE_REDIRECTS=new Set([301,302,303,307,308]);
const NOTE_RETRY_STATUS=new Set([408,429,500,502,503,504]);
function networkCodes(error){
  return [error?.code,error?.cause?.code,...(error?.cause?.errors||[]).map(e=>e.code)].filter(Boolean);
}
function noteConnectionError(error,url){
  const codes=networkCodes(error);
  let message='连接小红书失败，自动重试后仍无法读取。请检查网络后重试。',status=502,code='NOTE_CONNECTION_ERROR';
  if(codes.some(c=>['EPERM','EACCES'].includes(c))){
    message='本地服务没有联网权限，无法读取小红书链接。请重新启动本地服务并允许联网。';status=503;code='NOTE_NETWORK_DENIED';
  }else if(codes.some(c=>/CERT|TLS|SSL|SELF_SIGNED|UNABLE_TO_VERIFY/.test(c))){
    message='本地服务无法验证小红书连接证书，请检查系统时间或网络代理证书后重试。';code='NOTE_TLS_ERROR';
  }else if(error?.name==='TimeoutError'||error?.name==='AbortError'||codes.some(c=>['ETIMEDOUT','UND_ERR_CONNECT_TIMEOUT','UND_ERR_HEADERS_TIMEOUT','UND_ERR_BODY_TIMEOUT'].includes(c))){
    message='读取小红书笔记超时，自动重试后仍未完成，请稍后重试。';status=504;code='NOTE_TIMEOUT';
  }else if(codes.some(c=>['ENOTFOUND','EAI_AGAIN'].includes(c))){
    message='无法解析小红书域名，自动重试后仍失败，请检查网络或 DNS 后重试。';code='NOTE_DNS_ERROR';
  }else if(error?.code==='NOTE_UPSTREAM_BUSY'){
    message='小红书服务暂时繁忙，自动重试后仍无法读取，请稍后再试。';status=503;code='NOTE_UPSTREAM_BUSY';
  }
  const result=new AppError(message,status,code);
  result.cause=error;result.upstreamHost=url.hostname;
  return result;
}
async function readNotePage(url,fetcher,overall,{attemptTimeoutMs,retryDelays}){
  for(let attempt=0;attempt<=retryDelays.length;attempt++){
    try{
      overall.throwIfAborted();
      const response=await fetcher(url,{
        redirect:'manual',signal:AbortSignal.any([overall,AbortSignal.timeout(attemptTimeoutMs)]),
        headers:{'User-Agent':SHARE_USER_AGENT,Accept:'text/html','Accept-Language':'zh-CN,zh;q=0.9'}
      });
      if(NOTE_REDIRECTS.has(response.status)){
        const redirect=response.headers.get('location');await response.body?.cancel();return {redirect};
      }
      if(NOTE_RETRY_STATUS.has(response.status)){
        await response.body?.cancel();throw Object.assign(new Error('Temporary upstream failure'),{code:'NOTE_UPSTREAM_BUSY'});
      }
      if(!response.ok){await response.body?.cancel();throw new AppError('小红书暂时无法读取这篇笔记，请复制笔记正文后重试。',422,'NOTE_UNAVAILABLE');}
      return {html:await limitedText(response)};
    }catch(error){
      if(error instanceof AppError)throw error;
      const diagnosed=noteConnectionError(error,url);
      if(['NOTE_NETWORK_DENIED','NOTE_TLS_ERROR'].includes(diagnosed.code)||attempt===retryDelays.length||overall.aborted)throw diagnosed;
      try{await delay(retryDelays[attempt],undefined,{signal:overall});}
      catch(error){throw noteConnectionError(error,url);}
    }
  }
}
export async function fetchNote(input, fetcher = fetch, {attemptTimeoutMs=10000,totalTimeoutMs=45000,retryDelays=[400,1200]}={}) {
  let url = extractUrl(input);
  const overall = AbortSignal.timeout(totalTimeoutMs);
  for (let hop = 0; hop < 6; hop++) {
    const page=await readNotePage(url,fetcher,overall,{attemptTimeoutMs,retryDelays});
    if (Object.hasOwn(page,'redirect')) {
      const target=page.redirect;
      if (!target) break;
      url = extractUrl(new URL(target, url).href);
      if (/^\/(?:login|404|website-login)(?:\/|$)/i.test(url.pathname)) throw new AppError('这篇笔记需要登录或暂时无法读取。请切换「粘贴正文」继续。', 422, 'NOTE_UNAVAILABLE');
      continue;
    }
    const note = parseNoteHtml(page.html, url.href);
    if (!note.text || (note.text.length < 8 && !note.images.length)) throw new AppError('这篇笔记需要登录或暂时无法读取。请切换「粘贴正文」继续。', 422, 'NOTE_UNAVAILABLE');
    return note;
  }
  throw new AppError('分享链接跳转异常，请使用笔记的完整链接，或粘贴正文。', 422, 'NOTE_UNAVAILABLE');
}
function decodeEntities(s = '') {
  return s.replace(/&(?:amp|lt|gt|quot|apos|#39|#x[\da-f]+|#\d+);/gi, token => {
    const fixed = { '&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&apos;': "'", '&#39;': "'" };
    if (fixed[token]) return fixed[token];
    const n = token[2].toLowerCase() === 'x' ? parseInt(token.slice(3, -1), 16) : parseInt(token.slice(2, -1), 10);
    return n >= 0 && n <= 0x10ffff ? String.fromCodePoint(n) : '';
  });
}
// 读取平衡的大括号，同时仅替换字符串之外的 undefined；不执行页面中的 JavaScript。
export function readInitialState(html) {
  const marker = /(?:window\.)?__INITIAL_STATE__\s*=\s*/g.exec(html);
  if (!marker) return null;
  const start = marker.index + marker[0].length;
  let depth = 0, quoted = false, escaped = false, out = '';
  for (let i = start; i < html.length; i++) {
    const ch = html[i];
    if (quoted) {
      out += ch;
      if (escaped) escaped = false; else if (ch === '\\') escaped = true; else if (ch === '"') quoted = false;
      continue;
    }
    if (ch === '"') { quoted = true; out += ch; continue; }
    if (html.slice(i, i + 9) === 'undefined' && !/[\w$]/.test(html[i - 1] || '') && !/[\w$]/.test(html[i + 9] || '')) { out += 'null'; i += 8; continue; }
    if (ch === '{' || ch === '[') depth++;
    if (ch === '}' || ch === ']') depth--;
    out += ch;
    if (depth === 0 && out.trim()) { try { return JSON.parse(out); } catch { return null; } }
  }
  return null;
}
export function parseNoteHtml(html, sourceUrl = '') {
  const state = readInitialState(html);
  const id = /\/(?:explore|discovery\/item)\/([a-z0-9]+)/i.exec(sourceUrl)?.[1];
  const map = state?.note?.noteDetailMap || {};
  const entries = Object.values(map);
  const desktopTarget = (id && (map[id]?.note || entries.find(x => x?.note?.noteId === id)?.note)) || entries[0]?.note;
  const mobileTarget = state?.noteData?.data?.noteData;
  const target = desktopTarget || (mobileTarget && (!id || mobileTarget.noteId === id) ? mobileTarget : null);
  if (target?.desc || target?.title) {
    const images = (Array.isArray(target.imageList) ? target.imageList : []).map(img => img.urlDefault || img.infoList?.find(i => ['WB_DFT','H5_DTL'].includes(i.imageScene))?.url || img.url).filter(u => typeof u === 'string').map(u => u.startsWith('//') ? `https:${u}` : u.replace(/^http:/,'https:'));
    return { title: target.title || '一篇新的探店笔记', text: [target.title, target.desc].filter(Boolean).join('\n'), author: target.user?.nickname || target.user?.nickName || '', images, sourceUrl };
  }
  const meta = {};
  for (const match of html.matchAll(/<meta\s+[^>]*>/gi)) {
    const attrs = {};
    for (const a of match[0].matchAll(/([\w:-]+)\s*=\s*(["'])(.*?)\2/gs)) attrs[a[1].toLowerCase()] = decodeEntities(a[3]);
    if (attrs.property || attrs.name) meta[attrs.property || attrs.name] = attrs.content || '';
  }
  let text = meta['og:description'] || meta.description || '';
  if (/登录后|扫码登录|小红书是年轻人的生活方式平台|你访问的页面|页面不存在|内容不存在/.test(text)) text = '';
  return { title: meta['og:title']?.replace(/\s*[-|]\s*小红书.*$/, '') || '小红书笔记', text, author: '', images: meta['og:image'] ? [meta['og:image']] : [], sourceUrl };
}
export function cleanName(name) {
  return String(name).replace(/^[\s\d.、)）①②③④⑤⑥⑦⑧⑨⑩📍🏠🏡☕🍜🍰🍽️⭐✨✅❤️🧁📌🍞🥐·•\-]+/u, '').replace(/^(?:店名|店铺名称|门店|餐厅|咖啡店|咖啡馆|推荐店铺)\s*[：:]\s*/, '').replace(/[#＃].*$/, '').replace(/[，,。！!；;|｜].*$/, '').trim().slice(0, 65);
}
export function inferCategory(name, context = '') {
  const s = name + ' ' + context;
  if (/咖啡|coffee|café|cafe|arabica|%|MANNER|M Stand/i.test(s)) return 'coffee';
  if (/面包|烘焙|蛋糕|甜品|冰淇淋|bakery|bake|gelato|可颂/i.test(s)) return 'dessert';
  if (/餐|面馆|饭|火锅|小吃|食堂|菜|烧烤|馄饨|酒家|料理|饺|拉面|牛肉面/i.test(s)) return 'food';
  return 'other';
}
export function extractShopsByRules(text) {
  const found = []; const seen = new Set();
  const add = (raw, evidence, address = '') => {
    const name = cleanName(raw); const key = name.replace(/[\s（）()·]/g, '').toLowerCase();
    if (name.length < 2 || name.length > 60 || /^(地址|人均|推荐|营业|交通|路线|时间|地铁|电话|注意|建议|周末|今天|收藏|打卡|分享|上海|北京|杭州|成都|广州|深圳)[：:]/.test(name) || seen.has(key)) return;
    seen.add(key); found.push({ name, category: inferCategory(name, evidence), evidence: evidence.slice(0, 180), addressHint: address, extraction: 'rules' });
  };
  // 移动页元信息会压平换行，先恢复 p1/p2、图1/图2 这类图片编号段落。
  const lines = text.replace(/(^|[ \t])((?:[pP]|图)\s*\d{1,2})(?=\s|[.、:：])/g,'$1\n$2').split(/\n+/).map(s => s.trim()).filter(Boolean);
  const coffeeList = /咖啡(?:店|馆|探店|地图|清单)/.test(lines[0] || '');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]; let name = '';
    const explicit = line.match(/(?:店名|店铺名称|推荐店铺|门店)\s*[：:]\s*([^\n]+)/);
    const numbered = line.match(/^(?:[①②③④⑤⑥⑦⑧⑨⑩]|(?:\d{1,2}\s*[.、)）]|[1-9]️⃣))\s*(.+)/);
    const imageNumbered = line.match(/^(?:[pP]|图)\s*\d{1,2}(?:\s+|[.、:：]\s*)(.+)/);
    const pin = line.match(/^[📍🏠🏡☕🍜🍰🍽️🧁📌🍞🥐]+\s*(.+)/u);
    const bracket = line.match(/[【「『]([^】」』\n]{2,45})[】」』]/);
    if (explicit) name = explicit[1];
    else if (imageNumbered) name = imageNumbered[1].split(/\s+[—–-]\s+|[：:]/)[0];
    else if (numbered) name = numbered[1].split(/\s+[—–-]\s+|[：:]/)[0];
    else if (pin && !/^(地址|交通|位置|人均|地铁|营业)/.test(pin[1])) name = pin[1].split(/[：:]/)[0];
    else if (bracket) name = bracket[1];
    else if (line.length <= 55 && /(?:咖啡|咖啡馆|面馆|餐厅|面包店|甜品店|火锅店|书店|烘焙|coffee|bakery|café|cafe)(?:[（(][^）)]*[）)])?$/i.test(line)) name = line;
    if (name) {
      const next = lines[i + 1] || '';
      const address = /^(?:📍\s*)?(?:地址|位置)\s*[：:]\s*(.+)/u.exec(next)?.[1] || '';
      add(name, line, address);
    }
  }
  // 同一句正文可列举多家店，例如“推荐【A】和【B】”。
  for (const m of text.matchAll(/[【「『]([^】」』\n]{2,45})[】」』]/g)) add(m[1], m[0]);
  if (coffeeList) for (const shop of found) if (shop.category === 'other') shop.category = 'coffee';
  return found.slice(0, 40);
}
export async function extractShops(note, config, fetcher = fetch, mode = 'auto') {
  const rules = () => ({ shops: extractShopsByRules(note.text), method: 'rules', warnings: ['当前使用基础规则识别。接入大模型可识别自由描述、别名和分店信息。'], recognition: null });
  if (mode === 'rules') return rules();
  if (!config.aiKey || !config.aiBase || !config.aiModel) {
    if (mode === 'ai') throw new AppError('请先在「连接服务」中配置并连接大模型。', 422, 'AI_NOT_CONFIGURED');
    return rules();
  }
  try { return await recognizeWithModel(note, config, fetcher); }
  catch (error) {
    if (mode === 'ai') throw error;
    const result = rules();
    result.warnings = [(error instanceof AppError ? error.message : '模型识别失败。') + '本次已退回基础规则识别，可连接模型后重试。'];
    result.modelError = error.code || 'AI_ERROR';
    return result;
  }
}
// 高德服务使用 GCJ-02；存储和距离计算使用 WGS-84，绘图时转回高德坐标。
export function wgsToGcj(lng, lat) {
  if (lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271) return [lng, lat];
  const x = lng - 105, y = lat - 35, pi = Math.PI;
  let dLat = -100 + 2*x + 3*y + .2*y*y + .1*x*y + .2*Math.sqrt(Math.abs(x));
  dLat += (20*Math.sin(6*x*pi)+20*Math.sin(2*x*pi))*2/3 + (20*Math.sin(y*pi)+40*Math.sin(y/3*pi))*2/3 + (160*Math.sin(y/12*pi)+320*Math.sin(y*pi/30))*2/3;
  let dLng = 300 + x + 2*y + .1*x*x + .1*x*y + .1*Math.sqrt(Math.abs(x));
  dLng += (20*Math.sin(6*x*pi)+20*Math.sin(2*x*pi))*2/3 + (20*Math.sin(x*pi)+40*Math.sin(x/3*pi))*2/3 + (150*Math.sin(x/12*pi)+300*Math.sin(x/30*pi))*2/3;
  const rad = lat/180*pi, magic = 1-.00669342162296594323*Math.sin(rad)**2, root = Math.sqrt(magic);
  return [lng+dLng*180/(6378245/root*Math.cos(rad)*pi), lat+dLat*180/(6378245*(1-.00669342162296594323)/(magic*root)*pi)];
}
export function gcjToWgs(lng, lat) {
  let x = lng, y = lat;
  for (let i=0; i<4; i++) { const [a,b]=wgsToGcj(x,y); x-=a-lng; y-=b-lat; }
  return [x,y];
}
export function normalizeName(s) { return s.toLowerCase().replace(/[\s·%（）()\-]/g, ''); }
export function chooseExact(shop, candidates) {
  const name = normalizeName(shop.name);
  const exact = candidates.filter(c => normalizeName(c.name) === name);
  // 多个分店时保留为候选，让用户确认。
  return exact.length === 1 && (candidates.length === 1 || /[（(][^）)]*店[）)]/.test(shop.name)) ? exact[0] : null;
}
export async function searchPlaces(keyword, city, key, fetcher = fetch) {
  if (!key) return [];
  const url = new URL('https://restapi.amap.com/v3/place/text');
  url.search = new URLSearchParams({ key, keywords: keyword, city, citylimit: 'true', offset: '20', page: '1', extensions: 'all' }).toString();
  const response = await fetcher(url, { signal: AbortSignal.timeout(12000) });
  if (!response.ok) throw new AppError('门店检索暂时不可用，请稍后重试。', 502, 'MAP_ERROR');
  const data = await response.json();
  if (data.status !== '1') throw new AppError('高德门店检索失败，请检查 Web 服务 Key、权限或配额。', 422, 'MAP_ERROR');
  return (data.pois || []).filter(p => /^\d+(?:\.\d+)?,\d+(?:\.\d+)?$/.test(p.location || '')).map(p => {
    const [lng,lat] = p.location.split(',').map(Number); const [wgsLng,wgsLat]=gcjToWgs(lng,lat);
    return { id: p.id, name: p.name, address: [p.pname,p.cityname,p.adname,p.address].filter(x => typeof x === 'string').filter((x,i,a) => a.indexOf(x)===i).join(''), city:typeof p.cityname==='string'?p.cityname:'',district:typeof p.adname==='string'?p.adname:'',lng:wgsLng, lat:wgsLat, gcjLng:lng, gcjLat:lat, type: typeof p.type === 'string' ? p.type : '' };
  });
}

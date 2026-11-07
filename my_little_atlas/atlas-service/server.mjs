import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {AppError, searchPlaces} from './lib/core.mjs';
import {parseNote, matchShops, textField} from './lib/service.mjs';
import {geocodeAddress} from './lib/addresses.mjs';
import {getTile} from './lib/tiles.mjs';
import {configFromEnv} from './lib/runtime-config.mjs';

// Keys remain on the service host. The native clients receive no credentials.
export async function localConfig() {
  const env = {...process.env};
  try {
    for (const line of (await readFile(new URL('.env', import.meta.url), 'utf8')).split(/\r?\n/)) {
      const match = /^\s*([A-Z_][A-Z_0-9]*)\s*=\s*(.*?)\s*$/.exec(line);
      if (match && env[match[1]] === undefined) env[match[1]] = match[2].replace(/^(['"])(.*)\1$/, '$2');
    }
  } catch (e) { if (e.code !== 'ENOENT') throw e; }
  return configFromEnv(env);
}

export function createServer(config, {fetcher = fetch} = {}) {
  let active = 0;
  const server = http.createServer(async (req, res) => {
    const json = (status, data) => {
      res.writeHead(status, {'Content-Type':'application/json; charset=utf-8', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff'});
      res.end(JSON.stringify(data));
    };
    let claimed = false;
    try {
      // This development service is for native clients, not cross-site browser calls.
      if (req.headers.origin) throw new AppError('不允许浏览器跨站调用。', 403);
      const url = new URL(req.url, 'http://localhost');
      if (req.method === 'GET' && url.pathname === '/api/config') {
        return json(200, {mapConfigured:!!config.amapKey, aiConfigured:!!(config.aiKey && config.aiBase), model:config.aiModel});
      }
      const tile = /^\/tiles\/road\/(\d{1,2})\/(\d{1,6})\/(\d{1,6})\.png$/.exec(url.pathname);
      if (req.method === 'GET' && tile) {
        const data = await getTile('road', ...tile.slice(1).map(Number), fetcher);
        res.writeHead(200, {'Content-Type':data.contentType, 'Cache-Control':data.cacheControl});
        res.end(data.bytes); return;
      }
      if (req.method !== 'POST') throw new AppError('接口不存在。', 404);
      if (!String(req.headers['content-type']).startsWith('application/json')) throw new AppError('需要 JSON 请求。', 415);
      if (active >= 4) throw new AppError('解析服务正在忙，请稍后重试。', 429);
      active++; claimed = true;
      let size = 0; const chunks = [];
      for await (const chunk of req) {
        size += chunk.length;
        if (size > 150000) throw new AppError('输入内容过长。', 413);
        chunks.push(chunk);
      }
      let data;
      try { data = JSON.parse(Buffer.concat(chunks).toString()); } catch { throw new AppError('请求内容格式不正确。'); }
      if (!data || Array.isArray(data) || typeof data !== 'object') throw new AppError('请求内容格式不正确。');
      if (url.pathname === '/api/parse') return json(200, await parseNote(data, config, {fetcher}));
      if (url.pathname === '/api/match') return json(200, await matchShops(data, config, {fetcher}));
      if (url.pathname === '/api/search') {
        const keyword = textField(data.keyword, 100), city = textField(data.city, 40);
        if (!keyword || !city) throw new AppError('请填写店名和城市。');
        if (!config.amapKey) throw new AppError('解析服务尚未配置高德 Web 服务 Key。', 422, 'KEY_REQUIRED');
        return json(200, {candidates:await searchPlaces(keyword, city, config.amapKey, fetcher)});
      }
      if (url.pathname === '/api/address/geocode') {
        const address = textField(data.address, 180), city = textField(data.city, 40);
        if (!address || !city) throw new AppError('请填写详细地址和城市。');
        return json(200, {candidates:await geocodeAddress(address, city, config.amapKey, fetcher)});
      }
      throw new AppError('接口不存在。', 404);
    } catch (e) {
      if (!res.headersSent) json(e.status || 500, {error:e instanceof AppError ? e.message : '解析服务暂时不可用，请重试。', code:e.code || 'SERVER_ERROR'});
      else res.end();
    } finally { if (claimed) active--; }
  });
  server.requestTimeout = 240000;
  return server;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const server = createServer(await localConfig());
  const host = process.env.HOST || '127.0.0.1', port = Number(process.env.PORT || 3212);
  server.listen(port, host, () => console.log(`Atlas native API ready on http://${host}:${port}`));
}

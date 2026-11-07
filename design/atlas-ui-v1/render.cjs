const {chromium}=require('./.tools/node_modules/playwright-core');
const path=require('path'),fs=require('fs');
const {pathToFileURL}=require('url');
const base=__dirname,url=pathToFileURL(path.join(base,'index.html')).href;
async function ready(p){await p.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.images].map(i=>i.complete?Promise.resolve():new Promise(r=>{i.onload=r;i.onerror=r})));});}
(async()=>{
const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:['--disable-gpu','--no-sandbox','--allow-file-access-from-files']});
const context=await browser.newContext({viewport:{width:1840,height:1160},deviceScaleFactor:2});
const p=await context.newPage(),errors=[];p.on('pageerror',e=>errors.push(e.message));
await p.goto(url+'?board=1');await ready(p);
const catalog=await p.evaluate(()=>window.ATLAS_DESIGN);if(!catalog)throw Error(JSON.stringify(errors));
fs.writeFileSync(path.join(base,'catalog.json'),JSON.stringify(catalog,null,2));
const qa=[];
for(let n=1;n<=catalog.boards.length;n++){
await p.goto(url+'?board='+n);await ready(p);
qa.push(await p.evaluate(()=>({imagesOk:[...document.images].every(i=>i.naturalWidth>0),phones:document.querySelectorAll('.phone').length,overflow:[...document.querySelectorAll('.phone')].map(x=>({id:x.dataset.screen,width:x.scrollWidth,nestedButtons:x.querySelectorAll('button button').length})),brokenTargets:[...document.querySelectorAll('[data-go]')].map(x=>x.dataset.go).filter(id=>!window.ATLAS_DESIGN.screens.some(s=>s.id===id))})));
await p.screenshot({path:path.join(base,'boards',String(n).padStart(2,'0')+'-'+catalog.groups[n-1].title.split(' / ')[1]+'.png')});
console.log('Board '+n+'/'+catalog.boards.length+' exported');
}
await p.setViewportSize({width:2400,height:1270});await p.goto(url+'?overview=1');await ready(p);await p.screenshot({path:path.join(base,'boards','00-核心页面总览.png')});
await p.setViewportSize({width:390,height:844});
for(let i=0;i<catalog.screens.length;i++){
const s=catalog.screens[i];await p.goto(url+'?screen='+s.id);await ready(p);
await p.screenshot({path:path.join(base,'screens',String(i+1).padStart(2,'0')+'-'+s.id+'.png')});
qa.push(await p.evaluate(()=>({id:document.querySelector('.phone').dataset.screen,width:document.querySelector('.phone').scrollWidth,imgOk:[...document.images].every(x=>x.naturalWidth>0),sheets:[...document.querySelectorAll('.sheet')].map(x=>({top:x.getBoundingClientRect().top,bottom:x.getBoundingClientRect().bottom})),content:[...document.querySelectorAll('.content')].map(x=>({visible:x.clientHeight,total:x.scrollHeight}))})));
if((i+1)%8===0)console.log('Screens '+(i+1)+'/'+catalog.screens.length+' exported');
}
await p.setViewportSize({width:1280,height:970});await p.goto(url+'#home');await ready(p);
const flow=['add','import','parsing','results','import-success','map','place','visit','visit-success','journal','templates','share','saved'];
for(const id of flow){await p.locator('.phone [data-go="'+id+'"]').first().click();const got=await p.locator('.phone').getAttribute('data-screen');if(got!==id)throw Error('Expected '+id+', got '+got);}
await p.goto(url+'#results');await p.locator('[data-toggle-check]').first().click();if(!(await p.locator('[data-result-summary]').textContent()).includes('1 个'))throw Error('Selection count failed');
await p.goto(url+'#visited');await p.locator('[data-switch]').first().click();if(await p.locator('.toggle').first().evaluate(e=>e.classList.contains('on')))throw Error('Revisit switch failed');
await p.goto(url);await ready(p);
const galleryQa=await p.evaluate(()=>({phones:document.querySelectorAll('.phone').length,nestedButtons:document.querySelectorAll('button button').length,brokenImages:[...document.images].filter(i=>!i.naturalWidth).map(i=>i.src)}));
const report={generatedAt:'2026-11-07',screenCount:catalog.screens.length,boardCount:catalog.boards.length,errors,galleryQa,flow,selectionAndSwitch:'passed',qa};
fs.writeFileSync(path.join(base,'verification.json'),JSON.stringify(report,null,2));
let md='# My Little Atlas · 页面与动线索引\n\n2026-11-07 · '+catalog.screens.length+' 个页面／状态 · 390 × 844 设计画布 · 2× PNG 导出。\n\n## 主动线\n\n收藏首页 → 新增 → 导入 → 核对分店 → 明确勾选 → 收藏完成 → 地图 → 地点详情 → 记录到访 → 保存 → 手账 → 海报模板 → 预览 → 保存／系统分享。\n\n## 状态约定\n\n- 想去：没有到访记录；去过：至少一条正式到访；再去：去过后的独立意愿标签。\n- 同一地点可加入多个收藏夹，多次到访挂在同一地点。\n- 导入结果首次不预选；截图展示已经主动选中两项的中间状态。\n- 草稿不计入到访，导航与定位不触发自动打卡。\n- 无坐标地点进入待定位，不放置误导地图标记。\n- 照片、地址、距离、统计为演示内容；地图为非地理示意。\n- 账号与云同步为后续能力；本机 JSON 备份不含照片。\n\n## 页面清单\n\n|编号|页面|入口|设计重点|PNG|\n|---|---|---|---|---|\n';
catalog.screens.forEach((s,i)=>{const no=String(i+1).padStart(2,'0');md+='|'+no+'|'+s.name+'|'+s.entry+'|'+s.note+'|[查看](screens/'+no+'-'+s.id+'.png)|\n'});
md+='\n## 查看方式\n\n双击 index.html 打开完整画廊，可按功能组筛选。点击「体验完整动线」或页面按钮进入单页预览。业务数据为静态示例，搜索、导入、导航、登录与分享未连接真实服务。boards 目录含核心总览与 13 张功能分组图，screens 目录含全部独立页面。\n\n## 制作方式\n\n使用本地 HTML/CSS 排版，由浏览器渲染 PNG，保证中文与组件一致。照片复用项目已有素材；上海城市封面裁自用户参考图，仅用于本轮设计展示。未调用图像生成 API，未修改 Kotlin App。\n';
fs.writeFileSync(path.join(base,'flows.md'),md);
console.log(JSON.stringify({screenCount:catalog.screens.length,boards:catalog.boards.length,errors,galleryQa,flow:'passed',bad:qa.filter(q=>q.imagesOk===false||q.brokenTargets?.length||q.width>390||q.overflow?.some(x=>x.width>390||x.nestedButtons))}));
await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
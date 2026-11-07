const {chromium}=require('../../atlas-ui-v1/.tools/node_modules/playwright-core');
const fs=require('fs'),path=require('path'),{pathToFileURL}=require('url');
const root=path.resolve(__dirname,'..'),out=path.join(root,'preview/layers');
const asset=rel=>pathToFileURL(path.join(root,rel)).href;
(async()=>{
 fs.mkdirSync(out,{recursive:true});
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:['--allow-file-access-from-files']});
 const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:3}),p=await context.newPage();
 await p.goto(asset('preview/index.html')+'?screen=journal');
 await p.evaluate(async()=>{window.applyMaterials?.();await document.fonts.ready;await Promise.all([...document.images].map(i=>i.complete?Promise.resolve():new Promise(r=>i.onload=i.onerror=r)))});
 const spec=JSON.parse(fs.readFileSync(path.join(root,'specs/pages/journal.json'),'utf8'));
 const parent=spec.nodes.find(n=>n.class==='journal-cover'),children=spec.nodes.filter(n=>n.parent===parent.id);
 fs.writeFileSync(path.join(root,'specs/components/journal-cover.json'),JSON.stringify({screen:'journal',parent,children,order:['photo','frame','caption (z3)','tape (z4)'],note:'The tape is above the caption in paint order. Caption is native text. All dimensions use logical units; viewportBounds include rotation.'},null,2));
 await p.screenshot({path:path.join(out,'journal-cover-assembled.png'),clip:{x:12,y:134,width:366,height:250}});
 const photo=children.find(n=>n.kind==='image'&&!n.class.includes('mat-asset'));
 const photoUrl=asset(photo.src.replace('../',''));
 const frame=asset('assets/materials/frame_polaroid_wide@3x.png'),tape=asset('assets/materials/tape_oat@3x.png');
 const html=`<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>手账照片组件分层</title><style>
 *{box-sizing:border-box}body{margin:0;background:#eee9dc;color:#46543e;font-family:Microsoft YaHei,sans-serif}main{padding:44px 48px}h1{font-size:29px;font-weight:400;margin:0 0 13px}.lead{font-size:13px;color:#7f896e;line-height:1.8}.layout{display:grid;grid-template-columns:920px 1fr;gap:24px;margin-top:27px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.card,.result{border:1px solid #cdd3c0;border-radius:12px;background:#f9f6ed;padding:19px}h2{font-size:17px;font-weight:500;margin:0 0 12px}.stage{height:222px;display:flex;align-items:center;justify-content:center;background:repeating-conic-gradient(#e1e6d7 0% 25%,#d5ddca 0% 50%) 50%/20px 20px;overflow:hidden;border-radius:5px}.stage img{object-fit:fill}.stage .photo{width:334px;height:183px;object-fit:cover}.frame{width:346px;height:213px}.tape{width:176px;height:44px}.caption{font:22px KaiTi,serif;color:#888b75}p{font-size:12px;line-height:1.9;margin:12px 0 0;color:#7d876e}.result img{width:100%;display:block;margin:25px 0}.result .meta{border-top:1px solid #d6d9cb;padding-top:20px}.flow{margin-top:22px;padding:20px 25px;background:#e1e6d7;border-radius:10px;font-size:15px;line-height:2}.foot{font-size:12px;color:#92977f;margin-top:17px}code{font-family:Consolas,monospace;font-size:11px}</style><main>
 <h1>一张手账照片，拆成四个独立元素</h1><div class="lead">同一套素材直接用于效果图和 App · 位置为未旋转的局部坐标 · 单页画布 390 × 844<br>棋盘格只用于说明透明区域，不存在于 PNG 文件内。</div>
 <div class="layout"><div class="grid">
 <section class="card"><h2>01　照片内容</h2><div class="stage"><img class="photo" src="${photoUrl}"></div><p><code>photo_cafe_interior.jpg</code><br>照片窗：x 6 / y 6 / 334 × 183；cover 居中裁切。<br>照片内容可以更换，边框与用户文字独立保留。</p></section>
 <section class="card"><h2>02　透明相纸框</h2><div class="stage"><img class="frame" src="${frame}"></div><p><code>frame_polaroid_wide@3x.png</code><br>本页显示 346 × 213；标准原稿 344 × 233。<br>相框下沿无文字，中间真实透明。</p></section>
 <section class="card"><h2>03　半透明胶带</h2><div class="stage"><img class="tape" src="${tape}"></div><p><code>tape_oat@3x.png</code><br>本页 x 129 / y -8 / 88 × 22，局部旋转 +4°。<br>左图放大 2 倍便于查看；透明度已在 PNG 内。</p></section>
 <section class="card"><h2>04　可变手写说明</h2><div class="stage"><span class="caption">a slow afternoon in Shanghai</span></div><p>真实文字组件，不输出为图片。<br>本页 x 16 / y 192；11 逻辑单位，颜色 #888B75。<br>当前预览使用 KaiTi；实机需匹配有许可字体。</p></section>
 </div><aside class="result"><h2>组合后的实际页面局部</h2><img src="journal-cover-assembled.png"><p>从手账页面直接截取，保留真实胶带、照片、框和文字。</p><div class="meta"><p>父容器：346 × 213<br>父级旋转：-2°<br>位置：页面局部 x 22 / y 153<br>柔和阴影：0 / 4 / 9，约 13% 不透明度</p><p>父级整体旋转后再叠加胶带自身 +4°；不要用旋转后的外包框反推图片尺寸。</p><p>完整层级：<br><code>specs/components/journal-cover.json</code></p></div></aside></div>
 <div class="flow">纸张背景 → 照片（z0） → 相纸框（z2） → 说明文字（z3） → 胶带（z4）<br>组件标准尺寸与页面实际尺寸可能不同，开发按各页 JSON 验收。</div><div class="foot">现有照片为过渡素材；替换统一摄影后，重新导出此图与页面规格。未把整个页面烘焙为可交互图片。</div></main></html>`;
 fs.writeFileSync(path.join(out,'index.html'),html);
 const board=await browser.newContext({viewport:{width:1540,height:1160},deviceScaleFactor:1}),bp=await board.newPage();
 await bp.goto(pathToFileURL(path.join(out,'index.html')).href);await bp.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.images].map(i=>i.complete?Promise.resolve():new Promise(r=>i.onload=i.onerror=r)))});
 await bp.screenshot({path:path.join(out,'journal-exploded.png'),fullPage:true});
 console.log(JSON.stringify({output:out,photo:photo.asset,componentChildren:children.length}));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});

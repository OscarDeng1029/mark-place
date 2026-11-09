'use strict';
const assets='../../assets/';
const A=(folder,name)=>assets+folder+'/'+name;
const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const paths={book:'M3 4h7l2 2 2-2h7v17h-7l-2 2-2-2H3z M12 6v16',list:'M8 6h13M8 12h13M8 18h13M3 6h.1M3 12h.1M3 18h.1',map:'m3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2z M9 3v16M15 5v16',plus:'M5 12h14M12 5v14',bookmark:'M6 3h12v18l-6-4-6 4z',person:'M8 7a4 4 0 1 0 8 0 4 4 0 0 0-8 0M4 22v-2a8 8 0 0 1 16 0v2',pin:'M12 22s-8-8-8-13a8 8 0 0 1 16 0c0 5-8 13-8 13z M9 9a3 3 0 1 0 6 0 3 3 0 0 0-6 0',edit:'m4 16 12-12 4 4-12 12-5 1zM13 7l4 4',left:'m15 5-7 7 7 7',right:'m9 5 7 7-7 7',close:'m6 6 12 12M6 18 18 6',signal:'M4 19v-3M9 19v-7M14 19V8M19 19V4'};
const icon=n=>`<svg viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[n]||paths.book}"/></svg>`;
const styles=[
  {id:'daily',title:'随手一贴',sub:'相纸 · 撕纸小记',paper:'torn',slots:1,caption:'a slow afternoon',copy:'把平凡的一天，贴进喜欢的本子。'},
  {id:'duo',title:'双影拼贴',sub:'大相纸 · 方形便签',paper:'sticky',slots:2,caption:'two little moments',copy:'大照片错落叠放，方形便利贴夹在右上角。'},
  {id:'square',title:'方形留影',sub:'正方形满版 · 可调整裁切',paper:'sticky',slots:1,caption:'one square moment',copy:'照片默认铺满正方形相纸，也可以进入构图恢复完整画面。'},
  {id:'postcard',title:'旅途明信片',sub:'邮戳 · 纸上留白',paper:'lined',slots:1,caption:'a postcard to myself',copy:'明信片是纸张的样子，不规定照片必须横向。'},
  {id:'film',title:'胶片散页',sub:'交叠胶片 · 剪报',paper:'clipping',slots:2,caption:'moments on film',copy:'上下交叠的胶片，左侧夹一张不规则剪报。'},
  {id:'collect',title:'旅途采集',sub:'大双图 · 切角纸笺',paper:'torn',slots:2,caption:'little things collected',copy:'左上角写几句，照片从右上铺到左下。'},
  {id:'quiet',title:'纸上留白',sub:'单图 · 横线纸笺',paper:'lined',slots:1,caption:'nothing to hurry',copy:'减少装饰，让照片和几句手写字成为主体。'}
];
const photoCatalog=[
  {id:'dessert',src:A('photos','photo_dessert.jpg'),w:1000,h:1293,label:'窗边的下午茶'},
  {id:'coffee',src:A('photos','photo_coffee_table.jpg'),w:1000,h:667,label:'桌上的手冲'},
  {id:'interior',src:A('photos','photo_cafe_interior.jpg'),w:1000,h:686,label:'阳光照进小店'},
  {id:'exterior',src:A('photos','photo_cafe_exterior.jpg'),w:1000,h:661,label:'街角的咖啡店'},
  {id:'bookshop',src:A('photos','photo_bookshop.jpg'),w:1000,h:586,label:'翻到喜欢的一页'}
];
const sampleSets={mixed:[0,1,2,3,4],landscape:[3,2,1,4],portrait:[0]};
const papers={auto:'随排版',sticky:'便利贴',torn:'撕纸',clipping:'剪报',lined:'横线纸'};
const noteShapes={auto:'随排版',square:'方形',strip:'纸条',cut:'切角'};
const states=new Map();
const params=new URLSearchParams(location.search);
const board=params.get('view')==='board',single=params.has('single');
function newState(id,style='duo') {return {id,style,sample:'mixed',photos:sampleSets.mixed.map(i=>photoCatalog[i]),indices:[0,1],arrangement:'auto',paper:'auto',noteShape:'auto',frontSlot:1,edits:{},title:'把日子，过成喜欢的样子',body:'在窗边坐了很久，喝完一杯手冲。\n没有什么特别的事，\n只是想把这个下午留下来。',sheet:null};}
const mainState=newState('main',params.get('style')||'duo');
if(!styles.some(s=>s.id===mainState.style))mainState.style='duo';
states.set('main',mainState);
const styleOf=s=>styles.find(x=>x.id===s.style);
const selectedPhoto=(s,slot)=>s.photos[(s.indices[slot]||0)%s.photos.length];
const editFor=(s,p,slot=0)=>s.edits[`${slot}:${p.id}`]||(s.style==='square'?{mode:'crop',ratio:1,x:50,y:50,zoom:1}:{mode:'fit',ratio:1.5,x:50,y:50,zoom:1});
const photoType=p=>p.w/p.h>1.18?'横图':p.w/p.h<.85?'竖图':'方图';
function selectedButton(value,current,label,attrs=''){return `<button ${attrs} class="${value===current?'active':''}" aria-pressed="${value===current}">${label}</button>`;}
function styleButtons(s){return styles.map(x=>`<button class="style-choice ${x.id===s.style?'active':''}" data-style="${x.id}" aria-pressed="${x.id===s.style}"><span class="mini-layout ${x.id}"><i></i><b></b></span><span><strong>${x.title}</strong><small>${x.sub}</small></span></button>`).join('');}
function metadata(s){const count=Math.min(styleOf(s).slots,s.photos.length);const types=Array.from({length:count},(_,i)=>photoType(selectedPhoto(s,i)));return `${types.join(' + ')} · ${s.photos.length} 张照片<br>保留原画幅，轻点照片可看完整画面或调整构图。`;}
function renderShell(){
  if(board){
    const boardStyles=params.get('set')==='double'?styles.filter(x=>x.slots===2):styles;
    document.getElementById('app').innerHTML=`<main class="board"><header class="board-heading"><div><span class="eyebrow">MY LITTLE ATLAS / NOTEBOOK STUDIES 04</span><h1>照片大一点，纸条随手贴。</h1></div><p>放大的交叠双图 · 方形便利贴 · 侧边切角小笺<br>保留原画幅，轻点照片可查看完整画面</p></header><div class="board-grid">${boardStyles.map(x=>{const s=newState('board-'+x.id,x.id);states.set(s.id,s);return `<section class="board-item"><div data-phone-host="${s.id}"></div><h3>${x.title}</h3><p>${x.copy}</p></section>`;}).join('')}</div></main>`;
    for(const s of states.values())if(s.id.startsWith('board-'))renderPhone(s);
  }else if(single){document.getElementById('app').innerHTML='<main class="single-view"><div data-phone-host="main"></div></main>';renderPhone(mainState);}
  else {
    document.getElementById('app').innerHTML=`<header class="topbar"><div class="brand">${icon('book')}<span>My Little Atlas</span></div><div class="topbar-right"><span class="preview-tag">DESIGN PREVIEW 04</span><a href="?view=board&set=double" target="_blank">查看双图排版 ↗</a></div></header>
    <main class="workspace"><section class="intro"><p class="eyebrow">A LITTLE MORE PERSONAL</p><h1>把日子，<br>贴得自在一点。</h1><p class="intro-copy">不必规整，也不用费力装饰。<br>照片多一点，纸张有点性格，<br>像一本慢慢用起来的手账。</p>
    <section class="control-section style-section"><div class="section-title"><small>01</small> 选一种纸上的心情</div><div class="style-options" id="style-options"></div></section>
    <section class="control-section sample-section"><div class="section-title"><small>02</small> 换组照片，看看怎么排</div><div class="sample-options" id="sample-options"></div><button class="upload-button" data-action="upload">${icon('plus')} 用自己的照片试试</button><p class="upload-hint">可选择多张；只在当前浏览器预览，不会上传。</p><p id="live-metadata" class="live-metadata" aria-live="polite"></p></section>
    <section class="control-section arrangement-section"><div class="section-title"><small>03</small> 照片怎么放，由你决定</div><div class="segments" id="arrangement-options"></div><button class="layer-button" data-action="swap-layers">${icon('map')}交换照片前后</button></section>
    <section class="control-section note-section"><div class="section-title"><small>04</small> 文字，也可以随手贴</div><div class="paper-options" id="paper-options"></div><div class="segments shape-options" id="shape-options"></div></section></section>
    <section class="phone-wrap"><div data-phone-host="main"></div><p class="phone-caption">照片上左右滑动换图 · 轻点照片调整构图</p></section>
    <aside class="insights"><p class="insight-label">LARGER PHOTOS, LOOSER PAGES</p><section class="insight"><div class="index">01 / let them overlap</div><h3>照片大一点，边角叠一点。</h3><p>双图不再挤在两列里。主照片先铺开，另一张错位压住边角，照片和纸条共同使用整页空间。</p></section><section class="insight"><div class="index">02 / a note on the side</div><h3>想写几句，就贴在旁边。</h3><p>方形便利贴可以在右上，切角纸笺可以在左侧，剪报也可以夹在照片之间。文字不再固定占据页面下方。</p></section><section class="insight"><div class="index">03 / keep your choices</div><h3>遮住的片刻，仍然看得到。</h3><p>照片保留原画幅；轻点可查看完整画面，左右滑动换照片。还可以交换两张照片的前后，或自己调整构图。</p></section><div class="logic-card"><strong>照片与纸条，是同一张构图。</strong>位置和大小随画幅重新组合；印章沿着露出的照片边缘放，覆盖仍少于一半。</div></aside></main>
    <footer class="preview-foot"><span>使用现有照片与纸张素材 · 本轮仅为 HTML 设计预览 · 未修改 App</span><a href="?view=board" target="_blank">展开完整排版对照 ↗</a></footer>`;
    renderControls();renderPhone(mainState);
  }
}
function renderControls(){
  const s=mainState;if(!document.getElementById('style-options'))return;
  document.getElementById('style-options').innerHTML=styleButtons(s);
  document.getElementById('sample-options').innerHTML=[['mixed','横竖混合'],['landscape','都是横图'],['portrait','一张竖图']].map(([v,l])=>selectedButton(v,s.sample,l,`data-sample="${v}"`)).join('');
  document.getElementById('arrangement-options').innerHTML=[['auto','自然叠放'],['side','左右'],['stack','上下']].map(([v,l])=>selectedButton(v,s.arrangement,l,`data-arrangement="${v}"`)).join('');
  document.getElementById('paper-options').innerHTML=Object.entries(papers).map(([v,l])=>selectedButton(v,s.paper,l,`data-paper="${v}"`)).join('');
  document.getElementById('shape-options').innerHTML=Object.entries(noteShapes).map(([v,l])=>selectedButton(v,s.noteShape,l,`data-shape="${v}"`)).join('');
  document.querySelector('.layer-button').disabled=Math.min(styleOf(s).slots,s.photos.length)<2;
  document.getElementById('live-metadata').innerHTML=metadata(s);
}
function photoFrame(s,slot,last){
  const p=selectedPhoto(s,slot),e=editFor(s,p,slot),style=styleOf(s);
  return `<figure class="photo-frame ${style.id}" data-slot="${slot}" style="--fx:${e.x}%;--fy:${e.y}%;--zoom:${e.zoom}">
    <img class="tape ${slot?'sage':''}" alt="" src="${A('materials',slot?'tape_sage.png':'tape_oat.png')}">
    <div class="photo-box"><div class="photo-canvas ${e.mode==='crop'?'cropped':''}" role="button" tabindex="0" aria-label="照片 ${slot+1}：${esc(p.label)}，轻点调整构图，左右滑动换图" data-photo="${slot}"><img src="${p.src}" alt="${esc(p.label)}" draggable="false"></div>
    <button class="photo-edit" data-focus="${slot}" aria-label="调整第 ${slot+1} 张照片构图">${icon('edit')}</button>
    ${s.photos.length>1?`<span class="photo-count">‹ ${(s.indices[slot]||0)%s.photos.length+1} / ${s.photos.length} ›</span>`:''}
    ${last&&s.style!=='quiet'?`<span class="coffee-stamp" aria-hidden="true"><img src="${A('stickers','stamp_ring.png')}" alt=""><img src="${A('stickers','stamp_coffee_ink.png')}" alt=""></span>`:''}</div>
    <figcaption class="photo-caption"><span>${slot?'a little moment':style.caption}</span><span>${String(slot+1).padStart(2,'0')}</span></figcaption></figure>`;
}
function renderPhone(s){
  const host=document.querySelector(`[data-phone-host="${s.id}"]`);if(!host)return;
  const style=styleOf(s),n=styles.indexOf(style),slots=Math.min(style.slots,s.photos.length),paper=s.paper==='auto'?style.paper:s.paper;
  host.innerHTML=`<article class="phone" data-state="${s.id}" aria-label="${style.title}手账预览"><div class="status"><span>9:41</span><div class="status-right">${icon('signal')}<span>5G</span><span class="battery"></span></div></div>
    <header class="app-header"><h2>我的手账</h2><button class="directory" data-action="directory">${icon('list')}目录</button></header>
    <div class="book-wrap"><article class="book" data-book><div class="rings" aria-hidden="true">${'<i></i>'.repeat(5)}</div><header class="entry-heading"><p class="entry-date">2026.11.08 <span> · 上海</span></p><h3>${esc(s.title)}</h3><p class="entry-place">${icon('pin')}街角的小店 · 咖啡与散步</p></header>
    <div class="photo-stage scrap-composition layout-${style.id}" data-composition data-layout="${style.id}">${Array.from({length:slots},(_,i)=>photoFrame(s,i,i===(slots>1?s.frontSlot:0))).join('')}
    <div class="writing-zone"><section class="note-paper ${paper}" aria-label="${papers[paper]}" data-action="read" role="button" tabindex="0"><p>${esc(s.body).replace(/\n/g,' ')}</p><span class="note-caption">a small piece of today.</span></section></div>
    ${s.style==='collect'?'<div class="small-ticket">ONE DAY<strong>08 · NOV</strong>KEEP IT</div>':''}${s.style==='quiet'?'<span class="margin-scribble">slow down, look around.</span>':''}</div>
    <footer class="entry-footer"><button data-action="read">展开小记 ↗</button><span class="folio">— ${String(n+1).padStart(2,'0')} —</span><button data-action="edit">${icon('edit')}调整这一页</button></footer></article></div>
    <nav class="pager" aria-label="翻阅排版预览"><button data-step="-1" aria-label="前一页" ${n===0?'disabled':''}>${icon('left')}</button><span class="page-number">${styles.map((x,i)=>`<i class="${i===n?'active':''}"></i>`).join('')}</span><button data-step="1" aria-label="后一页" ${n===styles.length-1?'disabled':''}>${icon('right')}</button></nav>
    <div class="tabbar" aria-label="底部导航示意"><div class="tab active"><span>${icon('book')}</span>手账</div><div class="tab"><span>${icon('map')}</span>地图</div><button class="add-tab" data-action="upload" aria-label="添加照片">${icon('plus')}</button><div class="tab"><span>${icon('bookmark')}</span>收藏</div><div class="tab"><span>${icon('person')}</span>我的</div><div class="home-indicator"></div></div></article>`;
  const phone=host.querySelector('.phone');
  phone.addEventListener('click',ev=>handlePhoneClick(s,ev));
  phone.querySelectorAll('.photo-canvas').forEach(el=>wirePhoto(s,el));
  phone.querySelector('.note-paper').addEventListener('click',()=>openReading(s));
  phone.querySelector('.note-paper').addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();openReading(s);}});
  let down;
  phone.querySelector('[data-book]').addEventListener('pointerdown',e=>{if(!e.target.closest('button,.photo-frame'))down={x:e.clientX,y:e.clientY};});
  phone.querySelector('[data-book]').addEventListener('pointerup',e=>{if(down){const dx=e.clientX-down.x;if(Math.abs(dx)>55&&Math.abs(dx)>Math.abs(e.clientY-down.y)*1.4)step(s,dx<0?1:-1);down=null;}});
  requestAnimationFrame(()=>layoutPhotos(s));
}
function layoutPhotos(s){
  const host=document.querySelector(`[data-phone-host="${s.id}"]`),stage=host?.querySelector('.photo-stage');if(!stage)return;
  const frames=[...stage.querySelectorAll('.photo-frame')],W=stage.clientWidth-4,H=stage.clientHeight;
  const ratios=frames.map((_,i)=>{const p=selectedPhoto(s,i),e=editFor(s,p,i);return e.mode==='crop'?e.ratio:p.w/p.h;});
  const pad=s.style==='film'?14:s.style==='postcard'?18:s.style==='square'?18:s.style==='quiet'?0:12;
  const footer=s.style==='film'?42:s.style==='postcard'?44:s.style==='square'?39:s.style==='quiet'?24:28;
  const mode=s.arrangement==='auto'?'overlap':s.arrangement;
  stage.dataset.arrangement=frames.length===1?'single':mode;
  const boxes=[];
  let note={x:2,y:H-115,w:W-6,h:108,angle:-2,shape:'rectangle',position:'bottom'};
  function size(width,index,maxHeight=H-55){
    const w=Math.max(60,Math.min(width,(maxHeight-footer)*ratios[index]+pad));
    return {w,h:(w-pad)/ratios[index]+footer};
  }
  if(frames.length===1){
    if(s.style==='square'){
      // The square style uses a square photo window. The caption sits below it,
      // so the outside paper card is slightly taller than one square.
      const side=Math.min(W-38,H-205);
      const d={w:side+16,h:side+16+39};
      boxes.push({x:(W-d.w)/2,y:Math.max(8,(H-154-d.h)/2),...d,angle:-1});
      note={x:W-140,y:H-138,w:132,h:132,angle:4,shape:'square',position:'bottom-right'};
    }else{
      const d=size(W-12,0,H-151);
      boxes.push({x:(W-d.w)/2,y:Math.max(6,(H-148-d.h)/2),...d,angle:s.style==='quiet'?0:s.style==='postcard'?-1.5:-3});
      if(s.style==='postcard')note={x:W-153,y:H-150,w:151,h:145,angle:2,shape:'cut',position:'bottom-right'};
    }
  }else if(mode==='overlap'){
    const allLandscape=ratios.every(r=>r>1.18);
    if(allLandscape){
      const first=size(W*.94,0,H*.52),second=size(W*.90,1,H*.52);
      const y=Math.min(H-second.h-63,first.h-37);
      boxes.push({x:2,y:7,...first,angle:-3},{x:W-second.w-1,y,...second,angle:3.5});
      note={x:s.style==='collect'?W-155:2,y:H-150,w:151,h:145,angle:s.style==='film'?-3:3,shape:s.style==='film'?'cut':'square',position:s.style==='collect'?'bottom-right':'bottom-left'};
    }else{
      const first=size(W*.73,0,H*.66),second=size(W*.87,1,H*.67);
      const y=Math.max(94,Math.min(H-second.h-39,first.h-47));
      if(s.style==='collect'){
        boxes.push({x:W-first.w-3,y:8,...first,angle:3.5},{x:0,y,...second,angle:-3.5});
        note={x:0,y:22,w:148,h:151,angle:-4,shape:'cut',position:'top-left'};
      }else{
        boxes.push({x:0,y:9,...first,angle:-4},{x:W-second.w-1,y,...second,angle:3.5});
        note=s.style==='film'?{x:0,y:H-151,w:150,h:146,angle:-3,shape:'cut',position:'bottom-left'}:
          {x:W-148,y:21,w:146,h:146,angle:4,shape:'square',position:'top-right'};
      }
    }
  }else{
    const photoH=H-147;
    if(mode==='stack'){
      const gap=12,w=Math.min(W-14,(photoH-2*footer-gap)/(1/ratios[0]+1/ratios[1])+pad);
      const h1=(w-pad)/ratios[0]+footer,h2=(w-pad)/ratios[1]+footer;
      boxes.push({x:3,y:5,w,h:h1,angle:-2.5},{x:W-w-3,y:5+h1+gap,w,h:h2,angle:2.5});
    }else{
      const dims=ratios.map((r,i)=>size((W-15)/2,i,photoH-15));
      boxes.push({x:0,y:Math.max(6,(photoH-dims[0].h)/2-10),...dims[0],angle:-4},
        {x:W-dims[1].w-2,y:Math.min(photoH-dims[1].h,Math.max(6,(photoH-dims[1].h)/2+20)),...dims[1],angle:4});
    }
  }
  if(s.noteShape!=='auto'){
    if(s.noteShape==='strip')note={...note,x:2,y:H-105,w:W-6,h:98,shape:'strip',position:'bottom'};
    else{
      note={...note,w:146,h:s.noteShape==='square'?146:151,shape:s.noteShape};
      if(note.position.includes('right'))note.x=W-note.w-2;
      if(note.position.includes('bottom')||note.position==='bottom'){note.y=H-note.h-7;if(note.position==='bottom'){note.x=W-note.w-2;note.position='bottom-right';}}
    }
  }
  const writing=stage.querySelector('.writing-zone');
  writing.style.cssText=`left:${note.x}px;top:${note.y}px;width:${note.w}px;height:${note.h}px;--note-angle:${note.angle}deg`;
  writing.dataset.position=note.position;writing.dataset.shape=note.shape;
  frames.forEach((frame,i)=>{const b=boxes[i];frame.style.cssText+=`;left:${b.x}px;top:${b.y}px;width:${b.w}px;height:${b.h}px;--angle:${b.angle}deg;z-index:${i===s.frontSlot?3:2}`;});
  const ticket=stage.querySelector('.small-ticket');if(ticket){ticket.style.left=(W-70)+'px';ticket.style.top=(H-82)+'px';}
  const scribble=stage.querySelector('.margin-scribble');if(scribble){scribble.style.right='5px';scribble.style.bottom='5px';}
  placeStamp(stage);
  placePhotoControls(stage);
}
function placePhotoControls(stage){
  const paper=stage.querySelector('.note-paper').getBoundingClientRect();
  const layers=[...stage.querySelectorAll('.photo-frame')].map(el=>{const b=el.getBoundingClientRect();return{el,cx:(b.left+b.right)/2,cy:(b.top+b.bottom)/2,w:el.offsetWidth,h:el.offsetHeight,a:parseFloat(el.style.getPropertyValue('--angle'))*Math.PI/180,z:Number(el.style.zIndex)};});
  for(const frame of stage.querySelectorAll('.photo-frame')){
    const canvas=frame.querySelector('.photo-canvas'),b=canvas.getBoundingClientRect(),w=canvas.clientWidth,h=canvas.clientHeight;
    const a=parseFloat(frame.style.getPropertyValue('--angle'))*Math.PI/180,cx=(b.left+b.right)/2,cy=(b.top+b.bottom)/2;
    for(const selector of ['.photo-count','.photo-edit']){
      const control=frame.querySelector(selector);if(!control)continue;
      const cw=control.offsetWidth,ch=control.offsetHeight;
      const corners=[{x:w-cw-6,y:h-ch-6},{x:6,y:h-ch-6},{x:w-cw-6,y:6},{x:6,y:6}];
      if(selector==='.photo-edit')corners.reverse();
      const point=corners.find(p=>{const dx=p.x+cw/2-w/2,dy=p.y+ch/2-h/2;
        const x=cx+Math.cos(a)*dx-Math.sin(a)*dy,y=cy+Math.sin(a)*dx+Math.cos(a)*dy;
        if(x>paper.left&&x<paper.right&&y>paper.top&&y<paper.bottom)return false;
        return !layers.some(b=>{if(b.el===frame||b.z<=Number(frame.style.zIndex))return false;const dx=x-b.cx,dy=y-b.cy;return Math.abs(Math.cos(b.a)*dx+Math.sin(b.a)*dy)<b.w/2&&Math.abs(-Math.sin(b.a)*dx+Math.cos(b.a)*dy)<b.h/2;});
      })||corners[0];
      control.style.cssText=`left:${point.x}px;top:${point.y}px;right:auto;bottom:auto`;
    }
  }
}
function placeStamp(stage){
  const stamp=stage.querySelector('.coffee-stamp');if(!stamp)return;
  const frame=stamp.closest('.photo-frame'),canvas=frame.querySelector('.photo-canvas'),box=canvas.getBoundingClientRect();
  const angle=parseFloat(frame.style.getPropertyValue('--angle'))*Math.PI/180,w=canvas.clientWidth,h=canvas.clientHeight,r=32;
  const cx=(box.left+box.right)/2,cy=(box.top+box.bottom)/2;
  const world=(x,y)=>({x:cx+Math.cos(angle)*(x-w/2)-Math.sin(angle)*(y-h/2),y:cy+Math.sin(angle)*(x-w/2)+Math.cos(angle)*(y-h/2)});
  const photos=[...stage.querySelectorAll('.photo-canvas')].map(el=>{const b=el.getBoundingClientRect(),a=parseFloat(el.closest('.photo-frame').style.getPropertyValue('--angle'))*Math.PI/180;return{cx:(b.left+b.right)/2,cy:(b.top+b.bottom)/2,w:el.clientWidth,h:el.clientHeight,a};});
  const note=stage.querySelector('.note-paper').getBoundingClientRect(),book=stage.closest('.book'),bounds=book.getBoundingClientRect();
  const top=book.querySelector('.entry-heading').getBoundingClientRect().bottom+3,bottom=book.querySelector('.entry-footer').getBoundingClientRect().top-2;
  const candidates=[{x:w-17,y:h+13},{x:17,y:h+13},{x:w+15,y:h-22},{x:w+15,y:24},{x:-15,y:24},{x:-15,y:h-24},{x:w-24,y:-15},{x:24,y:-15}];
  let best;
  for(const local of candidates){
    const p=world(local.x,local.y);let total=0,covered=0,noteCovered=0;
    for(let x=-31;x<32;x+=3)for(let y=-31;y<32;y+=3)if(x*x+y*y<r*r){
      total++;const px=p.x+x,py=p.y+y;
      if(photos.some(b=>{const dx=px-b.cx,dy=py-b.cy;return Math.abs(Math.cos(b.a)*dx+Math.sin(b.a)*dy)<b.w/2&&Math.abs(-Math.sin(b.a)*dx+Math.cos(b.a)*dy)<b.h/2;}))covered++;
      if(px>note.left&&px<note.right&&py>note.top&&py<note.bottom)noteCovered++;
    }
    const overlap=covered/total;
    const outside=Math.max(0,bounds.left+7-(p.x-r))+Math.max(0,p.x+r-(bounds.right-3))+Math.max(0,top-(p.y-r))+Math.max(0,p.y+r-bottom);
    const score=Math.abs(overlap-.18)+outside*.06+noteCovered/total*3+(overlap>=.45?10:0);
    if(!best||score<best.score)best={...local,score,overlap};
  }
  stamp.style.left=(best.x-r)+'px';stamp.style.top=(best.y-r)+'px';stamp.style.right='auto';stamp.style.bottom='auto';
  stamp.dataset.photoOverlap=best.overlap.toFixed(3);
}
function wirePhoto(s,el){
  let down=null,swiped=false;
  el.addEventListener('pointerdown',e=>{down={x:e.clientX,y:e.clientY};swiped=false;el.setPointerCapture(e.pointerId);e.stopPropagation();});
  el.addEventListener('pointerup',e=>{if(!down)return;const dx=e.clientX-down.x,dy=e.clientY-down.y;down=null;
    if(Math.abs(dx)>28&&Math.abs(dx)>Math.abs(dy)*1.2){swiped=true;const slot=Number(el.dataset.photo);s.indices[slot]=((s.indices[slot]||0)+(dx<0?1:-1)+s.photos.length)%s.photos.length;renderPhone(s);renderControls();}
    e.stopPropagation();});
  el.addEventListener('pointercancel',()=>down=null);
  el.addEventListener('click',e=>{e.stopPropagation();if(!swiped)openFocus(s,Number(el.dataset.photo));});
  el.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();openFocus(s,Number(el.dataset.photo));}else if(['ArrowLeft','ArrowRight'].includes(e.key)){e.preventDefault();e.stopPropagation();const slot=Number(el.dataset.photo);s.indices[slot]=((s.indices[slot]||0)+(e.key==='ArrowRight'?1:-1)+s.photos.length)%s.photos.length;renderPhone(s);}});
}
function step(s,n){const i=Math.max(0,Math.min(styles.length-1,styles.indexOf(styleOf(s))+n));s.style=styles[i].id;renderPhone(s);if(s.id==='main')renderControls();}
function handlePhoneClick(s,e){
  const b=e.target.closest('button');if(!b)return;
  if(b.dataset.step)return step(s,Number(b.dataset.step));
  if(b.dataset.focus!==undefined)return openFocus(s,Number(b.dataset.focus));
  if(b.dataset.action==='directory')return directory(s);
  if(b.dataset.action==='read')return openReading(s);
  if(b.dataset.action==='edit')return pageEditor(s);
  if(b.dataset.action==='upload')return upload(s);
}
function openReading(s){return showSheet(s,`<div class="sheet-header"><h3>${esc(s.title)}</h3><button data-close aria-label="关闭">${icon('close')}</button></div><div class="reading"><small>2026.11.08 · 上海 · 街角的小店</small><p>${esc(s.body).replace(/\n/g,'<br>')}</p></div>`);}
let sheetReturnFocus=null;
function showSheet(s,html){
  const phone=document.querySelector(`[data-state="${s.id}"]`);phone.querySelector('.sheet-scrim')?.remove();
  if(!sheetReturnFocus)sheetReturnFocus=document.activeElement;
  const overlay=document.createElement('div');overlay.className='sheet-scrim';overlay.innerHTML=`<section class="sheet" role="dialog" aria-modal="true" aria-label="调整手账"><div class="sheet-handle"></div>${html}</section>`;
  phone.append(overlay);s.sheet=overlay;
  const close=()=>{overlay.remove();s.sheet=null;s.pageDraft=null;sheetReturnFocus?.focus();sheetReturnFocus=null;};
  overlay.addEventListener('click',e=>{if(e.target===overlay||e.target.closest('[data-close]'))close();});
  overlay.addEventListener('keydown',e=>{if(e.key==='Escape'){e.preventDefault();e.stopPropagation();close();}if(e.key==='Tab'){const fs=[...overlay.querySelectorAll('button,input')].filter(el=>!el.disabled&&!el.closest('.hidden'));const first=fs[0],last=fs.at(-1);if(e.shiftKey&&document.activeElement===first){e.preventDefault();last?.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first?.focus();}}});
  overlay.querySelector('button')?.focus({preventScroll:true});return overlay;
}
function directory(s){
  const overlay=showSheet(s,`<div class="sheet-header"><h3>这一本的六种心情</h3><button data-close aria-label="关闭目录">${icon('close')}</button></div><p class="sheet-help">在预览中翻阅不同排版，照片和文字会一直保留。</p>${styles.map((x,i)=>`<button class="directory-item" data-dir-style="${x.id}"><small>0${i+1}</small><div><strong>${x.title}</strong><span>${x.sub}</span></div></button>`).join('')}`);
  overlay.querySelectorAll('[data-dir-style]').forEach(b=>b.onclick=()=>{s.style=b.dataset.dirStyle;s.sheet=null;sheetReturnFocus=null;renderPhone(s);renderControls();});
}
function pageEditor(s){
  const draft=s.pageDraft||(s.pageDraft={style:s.style,arrangement:s.arrangement,paper:s.paper});
  const overlay=showSheet(s,`<div class="sheet-header"><h3>让这一页，更像你。</h3><button data-close aria-label="关闭调整">${icon('close')}</button></div><p class="sheet-help">风格可以换，照片不用迁就排版。</p><div class="style-options">${styleButtons(draft)}</div><p class="sheet-help">照片的摆放</p><div class="segments">${[['auto','依照片排'],['side','左右'],['stack','上下']].map(([v,l])=>selectedButton(v,draft.arrangement,l,`data-arrangement="${v}"`)).join('')}</div><p class="sheet-help">文字写在哪种纸上？</p><div class="paper-options">${Object.entries(papers).map(([v,l])=>selectedButton(v,draft.paper,l,`data-paper="${v}"`)).join('')}</div><button class="primary" data-apply-page>收好这一页</button>`);
  overlay.querySelector('[data-apply-page]').onclick=()=>{Object.assign(s,draft);s.pageDraft=null;s.sheet=null;sheetReturnFocus=null;renderPhone(s);renderControls();};
}
function openFocus(s,slot){
  const p=selectedPhoto(s,slot),draft={...editFor(s,p,slot)};
  const overlay=showSheet(s,`<div class="sheet-header"><h3>留下你想留下的。</h3><button data-close aria-label="取消构图调整">${icon('close')}</button></div><div class="segments" data-focus-mode><button data-mode="fit">完整展示</button><button data-mode="crop">自己裁切</button></div><div class="focus-preview"><img src="${p.src}" alt="完整照片与构图预览" draggable="false"><div class="focus-guide"></div></div><p class="sheet-help" data-focus-help></p><div class="focus-controls"><div class="segments" data-ratios>${[[1.5,'横幅'],[.75,'竖幅'],[1,'方形']].map(([r,l])=>`<button data-ratio="${r}">${l}</button>`).join('')}</div>${[['x','左右位置',0,100,1],['y','上下位置',0,100,1],['zoom','放大',1,2.5,.05]].map(([k,l,min,max,step])=>`<label class="range-row">${l}<input type="range" data-range="${k}" min="${min}" max="${max}" step="${step}" value="${draft[k]}"><output data-output="${k}"></output></label>`).join('')}</div><div class="focus-actions"><button class="secondary" data-reset>恢复完整照片</button><span class="sheet-help">${p.w} × ${p.h} · ${photoType(p)}</span></div><button class="primary" data-apply>用这个构图</button>`);
  const preview=overlay.querySelector('.focus-preview');
  function sync(){
    const crop=draft.mode==='crop';preview.classList.toggle('crop',crop);
    preview.style.setProperty('--fx',draft.x+'%');preview.style.setProperty('--fy',draft.y+'%');preview.style.setProperty('--zoom',draft.zoom);
    const ratio=crop?draft.ratio:p.w/p.h;
    const innerWidth=Math.min(overlay.querySelector('.sheet').clientWidth-54,241*ratio);
    preview.style.width=innerWidth+14+'px';preview.style.height=innerWidth/ratio+14+'px';
    overlay.querySelector('.focus-controls').classList.toggle('hidden',!crop);
    overlay.querySelector('[data-focus-help]').textContent=crop?'拖动照片或调整滑杆，选好位置与大小。也可以随时恢复完整照片。':'保留完整画面。照片框会适应原图比例，不切掉画面边缘。';
    overlay.querySelectorAll('[data-mode]').forEach(b=>{b.classList.toggle('active',b.dataset.mode===draft.mode);b.setAttribute('aria-pressed',b.dataset.mode===draft.mode);});
    overlay.querySelectorAll('[data-ratio]').forEach(b=>{b.classList.toggle('active',Number(b.dataset.ratio)===draft.ratio);b.setAttribute('aria-pressed',Number(b.dataset.ratio)===draft.ratio);});
    for(const k of ['x','y','zoom']){overlay.querySelector(`[data-range="${k}"]`).value=draft[k];overlay.querySelector(`[data-output="${k}"]`).textContent=k==='zoom'?draft.zoom.toFixed(1)+'×':Math.round(draft[k])+'%';}
  }
  overlay.querySelectorAll('[data-mode]').forEach(b=>b.onclick=()=>{draft.mode=b.dataset.mode;sync();});
  overlay.querySelectorAll('[data-ratio]').forEach(b=>b.onclick=()=>{draft.ratio=Number(b.dataset.ratio);sync();});
  overlay.querySelectorAll('[data-range]').forEach(input=>input.oninput=()=>{draft[input.dataset.range]=Number(input.value);sync();});
  overlay.querySelector('[data-reset]').onclick=()=>{Object.assign(draft,{mode:'fit',ratio:1.5,x:50,y:50,zoom:1});sync();};
  overlay.querySelector('[data-apply]').onclick=()=>{s.edits[`${slot}:${p.id}`]={...draft};s.sheet=null;sheetReturnFocus=null;renderPhone(s);renderControls();};
  let drag;
  preview.onpointerdown=e=>{if(draft.mode!=='crop')return;drag={x:e.clientX,y:e.clientY,fx:draft.x,fy:draft.y};preview.setPointerCapture(e.pointerId);};
  preview.onpointermove=e=>{if(!drag)return;draft.x=Math.max(0,Math.min(100,drag.fx-(e.clientX-drag.x)/preview.clientWidth*100));draft.y=Math.max(0,Math.min(100,drag.fy-(e.clientY-drag.y)/preview.clientHeight*100));sync();};
  preview.onpointerup=preview.onpointercancel=()=>drag=null;sync();
}
const uploadedUrls=new Set();
function upload(s){
  const input=document.createElement('input');input.type='file';input.accept='image/*';input.multiple=true;input.hidden=true;input.dataset.uploadInput='';document.body.append(input);
  input.oncancel=()=>input.remove();
  input.onchange=async()=>{
    const files=[...input.files||[]];input.remove();if(!files.length)return;
    const decoded=await Promise.all(files.map(file=>new Promise(resolve=>{const src=URL.createObjectURL(file),img=new Image();img.onload=()=>{uploadedUrls.add(src);resolve({id:'upload-'+crypto.randomUUID(),src,w:img.naturalWidth,h:img.naturalHeight,label:file.name});};img.onerror=()=>{URL.revokeObjectURL(src);resolve(null);};img.src=src;})));
    const photos=decoded.filter(Boolean);if(!photos.length)return toast(s,'这些图片暂时无法打开，请尝试 JPG、PNG 或 WebP。');
    s.photos=photos;s.sample='upload';s.indices=[0,Math.min(1,photos.length-1)];s.edits={};s.sheet=null;sheetReturnFocus=null;renderPhone(s);renderControls();
    if(photos.length!==files.length)toast(s,`已打开 ${photos.length} 张照片，其余格式暂不支持。`);
  };input.click();
}
function toast(s,message){const phone=document.querySelector(`[data-state="${s.id}"]`);phone.querySelector('.toast')?.remove();const div=document.createElement('div');div.className='toast';div.role='status';div.textContent=message;phone.append(div);setTimeout(()=>div.remove(),4000);}
document.addEventListener('click',e=>{
  const b=e.target.closest('button');if(!b||b.closest('.phone')&&!b.closest('.sheet'))return;
  const s=b.closest('.phone')?states.get(b.closest('.phone').dataset.state):mainState;
  if(b.dataset.style){if(b.closest('.sheet')&&s.pageDraft){s.pageDraft.style=b.dataset.style;pageEditor(s);}else{s.style=b.dataset.style;renderPhone(s);renderControls();}}
  if(b.dataset.paper){if(b.closest('.sheet')&&s.pageDraft){s.pageDraft.paper=b.dataset.paper;pageEditor(s);}else{s.paper=b.dataset.paper;renderPhone(s);renderControls();}}
  if(b.dataset.arrangement){if(b.closest('.sheet')&&s.pageDraft){s.pageDraft.arrangement=b.dataset.arrangement;pageEditor(s);}else{s.arrangement=b.dataset.arrangement;renderPhone(s);renderControls();}}
  if(b.dataset.shape){s.noteShape=b.dataset.shape;renderPhone(s);renderControls();}
  if(b.dataset.action==='swap-layers'){s.frontSlot=s.frontSlot===1?0:1;renderPhone(s);renderControls();}
  if(b.dataset.sample){s.sample=b.dataset.sample;s.photos=sampleSets[s.sample].map(i=>photoCatalog[i]);s.indices=[0,Math.min(1,s.photos.length-1)];renderPhone(s);renderControls();}
  if(b.dataset.action==='upload'&&!b.closest('.phone'))upload(s);
});
document.addEventListener('keydown',e=>{if(e.target.closest('input,button,.photo-canvas,.sheet'))return;if(!board&&['ArrowLeft','ArrowRight'].includes(e.key)){e.preventDefault();step(mainState,e.key==='ArrowRight'?1:-1);}});
window.addEventListener('resize',()=>states.forEach(layoutPhotos));
window.addEventListener('beforeunload',()=>uploadedUrls.forEach(URL.revokeObjectURL));
renderShell();
document.fonts.ready.then(()=>states.forEach(layoutPhotos));

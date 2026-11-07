'use strict';

const ASSETS = '../../assets/';
const photo = name => ASSETS + 'photos/photo_' + name + '.jpg';
const material = name => ASSETS + 'materials/' + name + '.png';
const sticker = name => ASSETS + 'stickers/' + name + '.png';
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const iconPaths = {
  book:'<path d="M3 4c3-1 6-1 9 1 3-2 6-2 9-1v16c-3-1-6-1-9 1-3-2-6-2-9-1z"/><path d="M12 5v16"/>',
  map:'<path d="m3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2zM9 3v16M15 5v16"/>',
  plus:'<path d="M12 4v16M4 12h16"/>',
  bookmark:'<path d="M6 3h12v18l-6-4-6 4z"/>',
  user:'<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2z"/>',
  list:'<path d="M8 5h13M8 12h13M8 19h13M3 5h.1M3 12h.1M3 19h.1"/>',
  back:'<path d="m14 5-7 7 7 7"/>',
  arrow:'<path d="m10 5 7 7-7 7"/>',
  close:'<path d="m6 6 12 12M18 6 6 18"/>',
  pin:'<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0z"/><circle cx="12" cy="10" r="2.5"/>',
  search:'<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>',
  edit:'<path d="m4 16 12-12 4 4L8 20H4zM14 6l4 4"/>',
  camera:'<path d="M3 7h4l2-3h6l2 3h4v14H3z"/><circle cx="12" cy="14" r="4"/>',
  check:'<path d="m5 12 4 4L20 5"/>',
  down:'<path d="m6 9 6 6 6-6"/>',
  settings:'<circle cx="12" cy="12" r="3"/><path d="m9 3 1-1h4l1 3 3 1 3 3-1 3 1 3-3 3-3 1-1 3h-4l-1-3-3-1-3-3 1-3-1-3 3-3z"/>',
  folder:'<path d="M3 6V4h7l2 3h9v13H3z"/>',
  clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l4 2"/>',
  wifi:'<path d="M3 8a15 15 0 0 1 18 0M6 12a10 10 0 0 1 12 0M9 16a5 5 0 0 1 6 0M12 20h.1"/>',
  signal:'<path d="M4 18v-3M9 18v-6M14 18V9M19 18V5"/>',
  heart:'<path d="M12 21 3 12a6 6 0 0 1 9-8 6 6 0 0 1 9 8z"/>',
};
const icon = name => '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true">' + (iconPaths[name] || iconPaths.book) + '</svg>';
const button = (action, content, className = '', extra = '') => '<button type="button" data-action="' + action + '" class="' + className + '" ' + extra + '>' + content + '</button>';

const INITIAL_PLACES = [
  {id:'p1',name:'Hidden Coffee',city:'上海',kind:'咖啡',photo:photo('cafe_interior')},
  {id:'p2',name:'衡山·和集',city:'上海',kind:'书店',photo:photo('bookshop')},
  {id:'p3',name:'武康路',city:'上海',kind:'散步',photo:photo('city_walk')},
  {id:'p4',name:'O.P.S. 咖啡',city:'上海',kind:'咖啡',photo:photo('coffee_table')},
  {id:'p5',name:'山丘面包',city:'上海',kind:'面包店',photo:photo('dessert')},
  {id:'p6',name:'湖边的小店',city:'杭州',kind:'咖啡',photo:photo('coffee_table')},
  {id:'p7',name:'巷口面包',city:'杭州',kind:'面包店',photo:photo('dessert')},
  {id:'p8',name:'街角书店',city:'南京',kind:'书店',photo:photo('bookshop')},
];
const INITIAL_VISITS = [
  {id:'v1',placeId:'p1',date:'2026-11-07',title:'把下午，\n留给一杯咖啡。',note:'阳光刚好落在窗边。\n慢慢喝完这一杯，什么也不用赶。',photo:photo('cafe_interior')},
  {id:'v2',placeId:'p2',date:'2026-11-03',title:'在书页之间，\n把时间放慢一点。',note:'没有想买的书，也可以待上很久。\n最后带走一本诗集，和一个安静的下午。',photo:photo('bookshop')},
  {id:'v3',placeId:'p3',date:'2026-11-01',title:'没有目的地，\n也算一次出发。',note:'沿着梧桐树走了一会儿。\n今天的好心情，是路过时捡到的。',photo:photo('city_walk')},
  {id:'v6',placeId:'p6',date:'2026-10-26',title:'换一座城，\n慢慢喝一杯。',note:'在湖边坐了一下午。\n把没赶上的日落，留给下次再来。',photo:photo('coffee_table')},
];
function blankEditor(){return {place:'',date:'2026-11-08',note:'',photo:null};}
function makeState(){return {tab:'home',page:0,overlay:null,query:'',directoryCity:'all',collectionCity:'all',filter:'all',mapFilter:'all',mapCity:'上海',visits:INITIAL_VISITS.map(v=>({...v})),places:INITIAL_PLACES.map(p=>({...p})),editor:blankEditor(),draft:null,editingId:null,selectedPlace:'p1',nickname:'小满',signature:'收集生活，也收藏自己。',toast:'',saved:false};}
const sampleEditor = () => ({place:'Hidden Coffee',date:'2026-11-08',note:'下雨的周末，适合慢慢坐一会儿。\n今天喝到了喜欢的拿铁。',photo:photo('coffee_table')});
function scenario(name){
  const s=makeState();
  if(name==='directory')s.overlay='directory';
  if(name==='directory-city'){s.overlay='directory';s.directoryCity='杭州';}
  if(name==='collections-city'){s.tab='collections';s.collectionCity='杭州';}
  if(name==='add')s.overlay='add';
  if(name==='editor'){s.tab='editor';s.editor=sampleEditor();}
  if(name==='saved'){
    s.visits.unshift({id:'v4',placeId:'p1',date:'2026-11-08',title:'下雨的周末，\n适合慢慢坐一会儿。',note:sampleEditor().note,photo:photo('coffee_table')});s.saved=true;
  }
  if(name==='empty'){s.visits=[];s.places=[];}
  if(['profile','map','collections'].includes(name))s.tab=name;
  return s;
}
const params=new URLSearchParams(location.search);
let state=scenario(params.get('screen') || 'home');
let toastTimer;

function statusBar(){return '<div class="status"><span>9:41</span><span class="status-icons">'+icon('signal')+icon('wifi')+'<i class="battery"></i></span></div>';}
function tabs(s){return '<nav class="tabbar" aria-label="主导航">'+[
  ['home','book','手账'],['map','map','地图'],['add','plus','添加'],['collections','bookmark','收藏'],['profile','user','我的']
].map(([id,glyph,label])=>id==='add'?button('add',icon(glyph),'add-tab','aria-label="添加"'):button('tab','<span class="tab-icon">'+icon(glyph)+'</span><span>'+label+'</span>','tab '+(s.tab===id?'active':''),'data-tab="'+id+'" aria-label="'+label+'"')).join('')+'</nav>';}
function header(title,subtitle,action=''){return '<header class="app-header"><div><h1>'+title+'</h1><p>'+subtitle+'</p></div>'+action+'</header>';}
const rings = '<div class="rings" aria-hidden="true">'+Array(5).fill('<i></i>').join('')+'</div>';
function leaf(s){
  if(!s.visits.length)return '<div class="leaf empty-leaf">'+rings+'<div class="leaf-date">MY LITTLE ATLAS</div><h2 class="leaf-heading">第一篇，<br>从你开始。</h2><img class="empty-doodle" src="'+sticker('doodle_cat@3x')+'" alt=""><p class="empty-copy">记下地点、日期和一点心情，<br>就能自动变成一页手账。</p><div class="empty-actions">'+button('write',icon('edit')+'写下第一篇手账','primary')+button('collect','先收藏一个想去的地方','text-link')+'</div></div>';
  const index=Math.min(s.page,s.visits.length-1),v=s.visits[index],p=s.places.find(p=>p.id===v.placeId);
  const title=escapeHtml(v.title || v.note.split(/[。\n]/)[0] || '这一天，值得记住。').replace(/\n/g,'<br>');
  const image=v.photo?'<div class="polaroid"><img class="scene-photo" src="'+escapeHtml(v.photo)+'" alt="'+escapeHtml(p.name)+'的到访照片"><img class="photo-frame" src="'+material('frame_polaroid_wide@3x')+'" alt=""><img class="tape" src="'+material('tape_oat@3x')+'" alt=""><span class="photo-caption">'+escapeHtml(p.name)+'</span></div>':'<div class="blank-photo">'+escapeHtml(p.name)+'<br><span class="tiny">留下一点，属于这一天的心情。</span></div>';
  return '<article class="leaf variant-'+index%3+(!v.photo?' no-photos':'')+'" aria-label="手账第 '+(index+1)+' 页">'+rings+'<div class="leaf-date">'+escapeHtml(v.date.replaceAll('-','.'))+' · '+escapeHtml(p.city)+'</div><h2 class="leaf-heading">'+title+'</h2><div class="leaf-place">'+icon('pin')+escapeHtml(p.name)+' · '+escapeHtml(p.kind)+'</div>'+image+'<div class="stamp" aria-hidden="true"><img src="'+sticker('stamp_ring@3x')+'" alt=""><img class="cup" src="'+sticker('stamp_coffee_ink@3x')+'" alt=""></div><div class="memory-note"><p>'+escapeHtml(v.note || '来过这里，就已经很值得记住。').replace(/\n/g,'<br>')+'</p></div><div class="leaf-bottom"><span>MY LITTLE MOMENTS</span>'+button('edit-visit',icon('edit')+'编辑这一页','','data-id="'+escapeHtml(v.id)+'"')+'</div></article>';
}
function journal(s){return header('我的手账',s.visits.length?'那些值得记住的日常':'把生活，慢慢记下来',s.visits.length?button('directory',icon('list')+'目录','directory-btn','aria-label="打开手账目录"'):'')+'<div class="book-wrap">'+leaf(s)+'</div>'+(s.visits.length?'<div class="page-turn">'+button('previous',icon('back'),'','aria-label="前一页" '+(s.page===0?'disabled':''))+'<div class="page-meta"><strong>第 '+(s.page+1)+' / '+s.visits.length+' 页</strong><span>左右翻阅 · 最近的记录在前</span></div>'+button('next',icon('arrow'),'','aria-label="后一页" '+(s.page>=s.visits.length-1?'disabled':''))+'</div>':'<div class="empty-bottom">每一次记录，都会长成新的一页。</div>');}
function cityTabs(s,scope){
  const places=scope==='directory'?s.visits.map(v=>s.places.find(p=>p.id===v.placeId)).filter(Boolean):s.places;
  const cities=[...new Set(places.map(p=>p.city))];
  if(cities.length<2)return '';
  const selected=scope==='directory'?s.directoryCity:s.collectionCity;
  return '<div class="city-tabs" role="group" aria-label="'+(scope==='directory'?'手账城市筛选':'收藏城市筛选')+'">'+[['all','全部城市',places.length],...cities.map(city=>[city,city,places.filter(p=>p.city===city).length])].map(([id,label,count])=>button('city-filter',escapeHtml(label)+'<span>'+count+'</span>',selected===id?'active':'','data-scope="'+scope+'" data-city="'+escapeHtml(id)+'" aria-pressed="'+(selected===id)+'"')).join('')+'</div>';
}
function directoryItems(s){
  const q=s.query.trim().toLowerCase();
  return s.visits.map((v,i)=>({v,i,p:s.places.find(p=>p.id===v.placeId)})).filter(({v,p})=>p&&(s.directoryCity==='all'||p.city===s.directoryCity)&&(p.name+' '+p.city+' '+v.note+' '+v.date).toLowerCase().includes(q));
}
function directoryRows(s){
  const list=directoryItems(s);
  if(!list.length)return '<p class="directory-empty">没有找到这篇记录，换个词试试。</p>';
  let month='';
  return list.map(({v,i,p})=>{const m=v.date.slice(0,7),heading=m!==month?'<div class="month-title">'+m.replace('-',' 年 ')+' 月</div>':'';month=m;return heading+button('jump','<img src="'+escapeHtml(v.photo||p.photo)+'" alt=""><span class="grow"><span class="date">'+escapeHtml(v.date.slice(5).replace('-',' / '))+'</span><h3>'+escapeHtml(p.name)+'</h3><p>'+escapeHtml(v.note||'在这里，留下一点生活。')+'</p></span>'+(i===s.page?'<span class="current-marker">正在<br>翻阅</span>':icon('arrow')),'directory-row','data-page="'+i+'"');}).join('');
}
function sheet(content,className='',label=''){return button('close','','scrim','aria-label="关闭'+label+'"')+'<section class="sheet '+className+'" role="dialog" aria-modal="true" aria-label="'+label+'"><div class="grabber"></div>'+content+'</section>';}
function sheetHeader(title){return '<div class="sheet-header"><h2>'+title+'</h2>'+button('close',icon('close'),'icon-btn sheet-close','aria-label="关闭"')+'</div>';}
function directory(s){return sheet(sheetHeader('手账目录')+'<p class="sheet-subtitle">点一下记录，翻到那一天</p>'+cityTabs(s,'directory')+'<label class="search">'+icon('search')+'<input id="directory-search" aria-label="搜索手账" placeholder="找地点、日期或记录的文字" value="'+escapeHtml(s.query)+'"></label><div id="directory-results">'+directoryRows(s)+'</div>','directory-sheet','手账目录');}
function addSheet(){return sheet(sheetHeader('今天，想记下什么？')+button('write','<span class="choice-icon">'+icon('edit')+'</span><span class="grow"><strong>写手账</strong><small>记下去过的地方，保存后自动成页</small></span>'+icon('arrow'),'action-choice')+button('collect','<span class="choice-icon">'+icon('bookmark')+'</span><span class="grow"><strong>收藏地点</strong><small>先收下喜欢的地方，等哪天去</small></span>'+icon('arrow'),'action-choice'),'add-sheet','添加');}
function editor(s){const e=s.editor;return '<header class="editor-header">'+button('cancel-edit','取消')+'<h1>'+(s.editingId?'编辑手账':'写手账')+'</h1>'+button('save-draft','存草稿','save-draft')+'</header><div class="editor-body"><label class="field-label" for="place-input">去过哪里？</label><div class="location-field">'+icon('pin')+'<input id="place-input" aria-label="地点名称" placeholder="填写或选择一个地点" value="'+escapeHtml(e.place)+'" list="known-places" maxlength="50"><datalist id="known-places">'+s.places.map(p=>'<option value="'+escapeHtml(p.name)+'"></option>').join('')+'</datalist></div><p class="location-help">可以直接填写新地点，不用先收藏。</p><div class="date-field"><label for="date-input">哪一天</label><input id="date-input" aria-label="记录日期" type="date" value="'+escapeHtml(e.date)+'" max="2026-11-08"></div><div class="field-label"><span>留下几张照片</span><span class="optional">选填</span></div><div class="photo-picker">'+(e.photo?'<div class="picked-photo"><img src="'+escapeHtml(e.photo)+'" alt="已选择的照片">'+button('remove-photo',icon('close'),'remove-photo','aria-label="移除照片"')+'</div>':'')+button('photos',icon('plus')+'添加照片','add-photo','aria-label="添加照片"')+'</div><label class="field-label" for="note-input"><span>想记住什么？</span><span class="optional">选填</span></label><textarea id="note-input" class="editor-note" placeholder="一杯喜欢的咖啡，或今天的小小心情……" maxlength="1000">'+escapeHtml(e.note)+'</textarea><p class="editor-hint">排版交给手账，你只管记录。</p></div><footer class="editor-footer">'+button('save-visit',s.editingId?'保存修改':icon('check')+'保存，生成手账','primary','id="save-visit" '+(!validEditor(e)?'disabled':''))+'</footer>';}
function validEditor(e){return e.place.trim().length>0 && /^\d{4}-\d{2}-\d{2}$/.test(e.date) && e.date<='2026-11-08' && !Number.isNaN(new Date(e.date+'T12:00:00').getTime());}
function collections(s){const visited=id=>s.visits.some(v=>v.placeId===id);const list=s.places.filter(p=>(s.collectionCity==='all'||p.city===s.collectionCity)&&(s.filter==='all'||visited(p.id)===(s.filter==='visited')));return header('我的收藏','先收下喜欢，再慢慢出发',button('collect',icon('plus'),'icon-btn','aria-label="收藏地点"'))+'<div class="app-scroll">'+cityTabs(s,'collection')+'<div class="filters">'+[['all','全部'],['wish','想去'],['visited','去过']].map(([id,label])=>button('filter',label,s.filter===id?'active':'','data-filter="'+id+'"')).join('')+'</div><div class="place-grid">'+list.map(p=>button('place','<img src="'+p.photo+'" alt="'+escapeHtml(p.name)+'"><h2>'+escapeHtml(p.name)+'</h2><p>'+escapeHtml(p.city)+' · '+escapeHtml(p.kind)+'</p><span class="mini-status">'+(visited(p.id)?'去过':'想去')+'</span>','place-card','data-id="'+p.id+'"')).join('')+'</div>'+(!list.length?'<p class="library-note">这里还没有符合条件的地点，换个城市或状态看看。</p>':'')+'<p class="library-note">城市按地点自动归类，无需手动建立分类。</p></div>';}
function mapScreen(s){const visited=id=>s.visits.some(v=>v.placeId===id);const places=s.places.filter(p=>p.city===s.mapCity&&(s.mapFilter==='all'||visited(p.id)===(s.mapFilter==='visited')));return header('我的地图','想去和去过，都在这里')+'<div class="map-canvas"><svg class="map-art" viewBox="0 0 390 638" aria-hidden="true"><rect width="390" height="638" fill="#eef0e7"/><g fill="#e1e8d8"><rect x="24" y="92" width="72" height="80" rx="14"/><rect x="258" y="202" width="98" height="106" rx="17"/><rect x="50" y="387" width="76" height="106" rx="12"/><rect x="251" y="453" width="116" height="76" rx="14"/></g><path d="M366-10C292 112 345 193 283 271S278 412 390 500" stroke="#c9dedb" stroke-width="59" fill="none"/><g stroke="#fffdf7" stroke-width="12" fill="none"><path d="M0 124 390 311M-25 244 390 449M0 369 390 574M67 0 0 638M206 0 94 638M346 0 215 638"/></g><g stroke="#dce1d2" stroke-width="1" fill="none"><path d="M0 124 390 311M-25 244 390 449M0 369 390 574M67 0 0 638M206 0 94 638M346 0 215 638"/></g><g fill="#9aa78e" font-size="10" font-family="sans-serif"><text x="20" y="199" transform="rotate(25 20 199)">淮海中路</text><text x="162" y="387" transform="rotate(25 162 387)">衡山路</text><text x="309" y="367" transform="rotate(75 309 367)" fill="#8eadaa">黄浦江</text></g></svg><div class="map-controls">'+button('city',icon('pin')+s.mapCity+'<span class="grow"></span>'+icon('down'),'city-select')+'<div class="filters">'+[['all','全部'],['wish','想去'],['visited','去过']].map(([id,label])=>button('map-filter',label,s.mapFilter===id?'active':'','data-filter="'+id+'"')).join('')+'</div></div>'+places.map((p,i)=>button('place',icon(visited(p.id)?'check':'bookmark')+escapeHtml(p.name),'map-pin','data-id="'+p.id+'" style="left:'+([42,66,28,61,32][i%5])+'%;top:'+([35,51,64,72,47][i%5])+'%"')).join('')+'<div class="map-summary"><h2>'+escapeHtml(s.mapCity)+'，慢慢走</h2><p><strong>'+s.places.filter(p=>p.city===s.mapCity&&visited(p.id)).length+'</strong> 处去过 · <strong>'+s.places.filter(p=>p.city===s.mapCity&&!visited(p.id)).length+'</strong> 处想去</p></div></div>';}
function settingsRow(action,glyph,title,subtitle=''){return button(action,icon(glyph)+'<span class="grow">'+title+(subtitle?'<small>'+subtitle+'</small>':'')+'</span>'+icon('arrow'),'settings-row');}
function profile(s){const visitedPlaces=s.places.filter(p=>s.visits.some(v=>v.placeId===p.id));return '<div class="profile-scroll"><div class="profile-brand"><span class="eyebrow">A PERSONAL ATLAS</span>'+button('settings',icon('settings'),'icon-btn','aria-label="设置"')+'</div><div class="profile-cover"><img src="'+photo('cafe_exterior')+'" alt="个人封面"><div class="avatar">'+icon('user')+'</div>'+button('edit-profile','编辑封面','cover-edit')+'</div><div class="profile-name"><h1>'+escapeHtml(s.nickname)+'的城市手账</h1>'+button('edit-profile','编辑资料')+'</div><p class="profile-signature">'+escapeHtml(s.signature)+'</p><div class="metrics">'+[[s.places.length,'收藏地点','profile-collections'],[visitedPlaces.length,'去过的地方','profile-map'],[new Set(visitedPlaces.map(p=>p.city)).size,'去过的城市','profile-map']].map(([n,label,action])=>button(action,'<strong>'+n+'</strong><span>'+label+'</span>','metric')).join('')+'</div><div class="badges-title"><span>我的小小成就</span>'+button('badges','勋章墙 →')+'</div><div class="badges">'+[['sticker_mountain@3x','城市探索家'],['sticker_coffee@3x','咖啡爱好者'],['sticker_camera@3x','留住光影']].map(([img,label])=>button('badges','<img src="'+sticker(img)+'" alt=""><small>'+label+'</small>','badge')).join('')+'</div>'+settingsRow('profile-journal','book','我的手账',s.visits.length+' 篇记录')+settingsRow('profile-map','map','去过的城市',new Set(visitedPlaces.map(p=>p.city)).size+' 座城市 · 在地图查看')+settingsRow('drafts','edit','未完成的草稿',s.draft?'1 篇':'0 篇')+'<p class="library-note">默认私密 · 记录保存在此设备</p></div>';}
function overlay(s){
  if(s.overlay==='directory')return directory(s);
  if(s.overlay==='add')return addSheet();
  if(s.overlay==='photos')return sheet(sheetHeader('添加照片')+'<p class="sheet-subtitle">可以用示例照片体验自动排版</p><div class="photo-tray">'+['coffee_table','cafe_interior','city_walk'].map(n=>button('pick-photo','<img src="'+photo(n)+'" alt="选择'+n+'照片">','','data-photo="'+n+'"')).join('')+'</div>'+button('upload-photo',icon('camera')+'从设备选择照片','primary')+'<input type="file" id="photo-file" accept="image/*" hidden>','','添加照片');
  if(s.overlay==='collect')return sheet(sheetHeader('收藏地点')+'<p class="sheet-subtitle">先记下名字，地址以后也可以补。</p><label class="field-label" for="collect-name">地点名称</label><div class="location-field">'+icon('bookmark')+'<input id="collect-name" aria-label="收藏地点名称" placeholder="想去哪一家？" maxlength="50"></div><div style="height:22px"></div>'+button('save-place','收进我的收藏','primary'),'','收藏地点');
  if(s.overlay==='place'){const p=s.places.find(p=>p.id===s.selectedPlace);return sheet(sheetHeader(escapeHtml(p.name))+'<img src="'+escapeHtml(p.photo)+'" alt="'+escapeHtml(p.name)+'"><span class="pill">'+icon('pin')+p.city+' · '+p.kind+'</span><p>'+(s.visits.some(v=>v.placeId===p.id)?'来过的地方，也可以再记一篇新的手账。':'先收藏，等一个刚好的日子再出发。')+'</p>'+button('write-place',icon('edit')+'在这里写手账','primary','data-id="'+p.id+'"'),'place-sheet','地点详情');}
  if(s.overlay==='drafts')return sheet(sheetHeader('未完成的草稿')+(s.draft?'<div class="utility-list"><h3>'+escapeHtml(s.draft.place||'还没写下地点')+'</h3><p>'+escapeHtml(s.draft.note||'下一次，接着写。')+'</p></div>'+button('resume-draft','继续写','primary'):'<div class="utility-list"><p>写到一半，点「存草稿」就能留在这里。<br>草稿保存后，才会成为正式手账。</p></div>'),'','未完成的草稿');
  if(s.overlay==='city')return sheet(sheetHeader('选择城市')+'<div class="utility-list">'+[...new Set(s.places.map(p=>p.city))].map(city=>button('choose-city',icon('pin')+'<span class="grow">'+escapeHtml(city)+'</span>'+icon('check'),'settings-row','data-city="'+escapeHtml(city)+'"')).join('')+'</div>','','选择城市');
  if(s.overlay==='edit-profile')return sheet(sheetHeader('编辑资料')+'<p class="sheet-subtitle">把这本手账，变成你的样子。</p><label class="field-label" for="nickname">昵称</label><div class="location-field"><input id="nickname" value="'+escapeHtml(s.nickname)+'" maxlength="20"></div><div style="height:20px"></div><label class="field-label" for="signature">个人签名</label><textarea id="signature" class="editor-note">'+escapeHtml(s.signature)+'</textarea><div style="height:20px"></div>'+button('save-profile','保存资料','primary'),'','编辑资料');
  if(s.overlay==='badges')return sheet(sheetHeader('我的小小成就')+'<div class="badges">'+[['sticker_mountain@3x','城市探索家'],['sticker_coffee@3x','咖啡爱好者'],['sticker_camera@3x','留住光影']].map(([img,label])=>'<div class="badge"><img src="'+sticker(img)+'" alt="'+label+'"><small>'+label+'</small></div>').join('')+'</div><div class="utility-list"><p>走过一座城市、发现一间咖啡馆、拍下难忘的一刻。<br>小小的成就，随着记录慢慢积累。</p></div>','','我的小小成就');
  if(s.overlay==='settings')return sheet(sheetHeader('设置')+'<div class="utility-list"><h3>轻一点，记下来</h3><p>这是一版界面与动线原型。记录、照片和资料修改仅在当前预览中展示，不会修改 App 里的内容。</p>'+button('reset','重新体验示例','primary')+'</div>','','设置');
  return '';
}
function renderPhone(s){const editorMode=s.tab==='editor';return '<div class="phone '+(s.tab==='home'?'journal-phone':'')+(editorMode?' editor-phone':'')+'">'+statusBar()+(editorMode?editor(s):s.tab==='home'?journal(s):s.tab==='collections'?collections(s):s.tab==='map'?mapScreen(s):profile(s))+(!editorMode?tabs(s):'')+(s.saved?'<div class="saved-toast" role="status">'+icon('check')+'已保存，翻到新的一页</div>':'')+overlay(s)+(s.toast?'<div class="toast" role="status">'+escapeHtml(s.toast)+'</div>':'')+'<div class="home-indicator"></div></div>';}

const viewNames={home:'手账首页',directory:'目录',add:'添加',editor:'写手账',empty:'第一次使用',collections:'收藏',map:'地图',profile:'我的'};
function currentView(){return state.overlay==='directory'?'directory':state.overlay==='add'?'add':state.tab==='home'&&!state.visits.length?'empty':state.tab;}
function render(){
  const board=params.get('board');
  if(board){renderBoard(board);return;}
  if(params.has('single')){document.getElementById('app').innerHTML='<div class="single">'+renderPhone(state)+'</div>';return;}
  document.getElementById('app').innerHTML='<div class="workspace"><aside class="intro"><div class="eyebrow">MY LITTLE ATLAS / CONCEPT 01</div><h1>轻一点，<br>记下来。</h1><p>不用先分清「足迹」和「图鉴」。<br>记下生活，翻成自己的手账。</p><div class="view-buttons">'+Object.entries(viewNames).map(([id,label])=>button('view',label,currentView()===id?'active':'','data-view="'+id+'"')).join('')+'</div><p class="preview-note">可点击原型 · 示例内容<br>试试目录、翻页和中间的 ＋。<br>地图仅展示城市筛选与入口布局。</p></aside><div><div class="phone-wrap">'+renderPhone(state)+'</div><p class="prototype-caption">收藏想去的地方 · 写下去过的日常 · 自动排成手账</p></div><aside class="notes"><div class="number">01</div><h2>打开，就是你的一页</h2><p>直接回到最近的记录，<br>不用先翻过封面和心愿清单。</p><div class="number">02</div><h2>目录，只负责找到那一天</h2><p>点一下就翻到对应页，<br>查找和翻阅始终是同一本手账。</p><div class="number">03</div><h2>想去与去过，各有去处</h2><p>想去的地点收进收藏。<br>去过的日常写成手账。</p><a href="?board=flow" target="_blank">打开三屏效果图 ↗</a></aside></div>';
}
function renderBoard(name){
  if(name==='cities'){
    document.getElementById('app').innerHTML='<article class="board"><header class="board-header"><div><div class="eyebrow">MY LITTLE ATLAS / CITY FILTERS</div><h1>按城市找，内容仍在同一页</h1></div><p>目录按城市筛选，再按月份排列<br>收藏保留城市与「想去 / 去过」两个维度</p></header><div class="board-grid">'+[['directory','01 / 目录 · 全部城市','只展示有到访记录的城市，默认查看全部。'],['directory-city','02 / 目录 · 切到杭州','城市下仍按月份排列，点记录直接翻到对应页。'],['collections-city','03 / 收藏 · 杭州','先选城市，再筛想去或去过，不增加页面层级。']].map(([screen,title,description])=>'<section>'+renderPhone(scenario(screen))+'<div class="board-label"><h2>'+title+'</h2><p>'+description+'</p></div></section>').join('')+'</div><footer class="board-foot"><span>自动按地点城市归类 · 默认全部 · 单城市时隐藏多余分类</span><span>界面与动线原型 · 示例内容</span></footer></article>';return;
  }
  const data=name==='entry'?{title:'记下生活，自动成为一页',subtitle:'写手账 → 保存后回到新的一页\n第一次打开，也知道从哪里开始',items:[['editor','04 / 写手账','直接填写地点，照片与文字选填；无需先收藏。'],['saved','05 / 保存后，自动成页','保存即返回手账，不再经过额外的成功页面。'],['empty','06 / 第一次使用','只提供一个主动作：写下第一篇手账。']]}:name==='navigation'?{title:'想去、去过、我的，各有去处',subtitle:'城市汇总放回地图\n个人页保留熟悉的大封面与清晰列表',items:[['collections','07 / 收藏','地点用「想去 / 去过」区分。'],['map','08 / 地图','城市筛选与统计在同一页。此处为布局示意。'],['profile','09 / 我的','保留已选外观，内容入口直接通向手账和地图。']]}:{title:'一本手账，一条清楚的动线',subtitle:'保留纸张、胶带与不规则排版\n把功能名称和入口变得直接一点',items:[['home','01 / 打开，就看到自己的记录','最近的日常直接成页，左右翻阅。'],['directory','02 / 目录，是这本手账的目录','找到旧记录，点一下就翻到对应页。'],['add','03 / ＋，只有两个明确动作','写手账，或收藏一个想去的地点。']]};
  document.getElementById('app').innerHTML='<article class="board"><header class="board-header"><div><div class="eyebrow">MY LITTLE ATLAS / SIMPLIFIED CONCEPT 01</div><h1>'+data.title+'</h1></div><p>'+data.subtitle.replace(/\n/g,'<br>')+'</p></header><div class="board-grid">'+data.items.map(([screen,title,description])=>'<section>'+renderPhone(scenario(screen))+'<div class="board-label"><h2>'+title+'</h2><p>'+description+'</p></div></section>').join('')+'</div><footer class="board-foot"><span>收藏想去的地方 → 记录去过的日常 → 自动成为手账</span><span>界面与动线原型 · 示例内容</span></footer></article>';
}
function showToast(text){state.toast=text;render();clearTimeout(toastTimer);toastTimer=setTimeout(()=>{state.toast='';render();},2200);}
function startWriting(place=''){state.editor={...blankEditor(),place};state.editingId=null;state.overlay=null;state.saved=false;state.tab='editor';render();}
function saveVisit(){
  const e=state.editor;if(!validEditor(e))return;
  let p=state.places.find(p=>p.name===e.place.trim());
  if(!p){p={id:'p'+Date.now(),name:e.place.trim(),city:state.mapCity,kind:'日常',photo:e.photo||photo('city_walk')};state.places.unshift(p);}
  const text=e.note.trim(),firstLine=text.split('\n')[0],comma=firstLine.indexOf('，');
  const title=comma>2&&comma<15?firstLine.slice(0,comma+1)+'\n'+firstLine.slice(comma+1):firstLine||p.name;
  const visit={id:state.editingId||'v'+Date.now(),placeId:p.id,date:e.date,title,note:text,photo:e.photo};
  state.visits=state.visits.filter(v=>v.id!==visit.id);state.visits.push(visit);state.visits.sort((a,b)=>b.date.localeCompare(a.date)||b.id.localeCompare(a.id));
  state.page=state.visits.findIndex(v=>v.id===visit.id);state.tab='home';state.overlay=null;state.saved=true;state.draft=null;state.editingId=null;state.editor=blankEditor();render();
  clearTimeout(toastTimer);toastTimer=setTimeout(()=>{state.saved=false;render();},2300);
}
document.addEventListener('click',event=>{
  if(params.has('board'))return;
  const el=event.target.closest('[data-action]');if(!el||el.disabled)return;
  const action=el.dataset.action;
  if(action==='view'){state=scenario(el.dataset.view);render();return;}
  if(action==='close'){state.overlay=null;state.query='';render();return;}
  if(action==='tab'){state.tab=el.dataset.tab;state.overlay=null;state.saved=false;render();return;}
  if(action==='directory'){state.overlay='directory';state.query='';render();return;}
  if(action==='add'){state.overlay='add';state.saved=false;render();return;}
  if(action==='write'){startWriting();return;}
  if(action==='collect'){state.overlay='collect';render();return;}
  if(action==='previous'||action==='next'){state.page=Math.max(0,Math.min(state.visits.length-1,state.page+(action==='next'?1:-1)));state.saved=false;render();return;}
  if(action==='jump'){state.page=Number(el.dataset.page);state.tab='home';state.overlay=null;state.query='';state.saved=false;render();return;}
  if(action==='photos'){state.overlay='photos';render();return;}
  if(action==='pick-photo'){state.editor.photo=photo(el.dataset.photo);state.overlay=null;render();return;}
  if(action==='upload-photo'){document.getElementById('photo-file').click();return;}
  if(action==='remove-photo'){state.editor.photo=null;render();return;}
  if(action==='cancel-edit'){state.tab='home';state.overlay=null;render();return;}
  if(action==='save-draft'){state.draft={...state.editor};state.tab='home';state.overlay=null;showToast('已存入「我的」草稿');return;}
  if(action==='resume-draft'){state.editor={...state.draft};state.tab='editor';state.editingId=null;state.overlay=null;render();return;}
  if(action==='save-visit'){saveVisit();return;}
  if(action==='edit-visit'){const v=state.visits.find(v=>v.id===el.dataset.id);const p=state.places.find(p=>p.id===v.placeId);state.editor={place:p.name,date:v.date,note:v.note,photo:v.photo};state.editingId=v.id;state.tab='editor';state.overlay=null;state.saved=false;render();return;}
  if(action==='filter'){state.filter=el.dataset.filter;render();return;}
  if(action==='city-filter'){state[el.dataset.scope==='directory'?'directoryCity':'collectionCity']=el.dataset.city;render();return;}
  if(action==='map-filter'){state.mapFilter=el.dataset.filter;render();return;}
  if(action==='place'){state.selectedPlace=el.dataset.id;state.overlay='place';render();return;}
  if(action==='write-place'){startWriting(state.places.find(p=>p.id===el.dataset.id).name);return;}
  if(action==='save-place'){const input=document.getElementById('collect-name');const name=input.value.trim();if(!name){input.focus();return;}let p=state.places.find(p=>p.name===name);if(!p){p={id:'p'+Date.now(),name,city:state.mapCity,kind:'地点',photo:photo('cafe_exterior')};state.places.unshift(p);}state.tab='collections';state.collectionCity=p.city;state.filter=state.visits.some(v=>v.placeId===p.id)?'all':'wish';state.overlay=null;showToast('已收进我的收藏');return;}
  if(action==='profile-journal'){state.tab='home';state.overlay=null;render();return;}
  if(action==='profile-map'){state.tab='map';state.mapFilter='visited';state.overlay=null;render();return;}
  if(action==='profile-collections'){state.tab='collections';state.filter='all';state.collectionCity='all';state.overlay=null;render();return;}
  if(action==='save-profile'){const name=document.getElementById('nickname').value.trim();if(!name){document.getElementById('nickname').focus();return;}state.nickname=name;state.signature=document.getElementById('signature').value.trim();state.overlay=null;showToast('资料已保存');return;}
  if(action==='choose-city'){state.mapCity=el.dataset.city;state.overlay=null;render();return;}
  if(action==='reset'){state=makeState();render();return;}
  if(['drafts','city','edit-profile','badges','settings'].includes(action)){state.overlay=action;render();}
});
document.addEventListener('input',event=>{
  const el=event.target;
  if(el.id==='directory-search'){state.query=el.value;document.getElementById('directory-results').innerHTML=directoryRows(state);}
  const field={'place-input':'place','date-input':'date','note-input':'note'}[el.id];
  if(field){state.editor[field]=el.value;document.getElementById('save-visit').disabled=!validEditor(state.editor);}
});
document.addEventListener('change',event=>{
  if(event.target.id==='photo-file'&&event.target.files[0]){const file=event.target.files[0];if(!file.type.startsWith('image/'))return;const reader=new FileReader();reader.onload=()=>{state.editor.photo=reader.result;state.overlay=null;render();};reader.readAsDataURL(file);}
});
document.addEventListener('keydown',event=>{
  if(params.has('board')||event.target.matches('input,textarea'))return;
  if(event.key==='Escape'&&state.overlay){state.overlay=null;render();return;}
  if(state.tab==='home'&&!state.overlay&&(event.key==='ArrowLeft'||event.key==='ArrowRight')){state.page=Math.max(0,Math.min(state.visits.length-1,state.page+(event.key==='ArrowRight'?1:-1)));state.saved=false;render();}
});
let touchStart;
document.addEventListener('pointerdown',event=>{if(!params.has('board')&&event.target.closest('.leaf')&&!event.target.closest('button')&&!state.overlay)touchStart={x:event.clientX,y:event.clientY};});
document.addEventListener('pointerup',event=>{if(!touchStart)return;const dx=event.clientX-touchStart.x,dy=event.clientY-touchStart.y;touchStart=null;if(Math.abs(dx)>65&&Math.abs(dx)>Math.abs(dy)*1.4&&state.visits.length){state.page=Math.max(0,Math.min(state.visits.length-1,state.page+(dx<0?1:-1)));state.saved=false;render();}});
render();

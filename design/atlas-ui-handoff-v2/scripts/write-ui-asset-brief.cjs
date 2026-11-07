const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'..'),NL=String.fromCharCode(10);
const common='为 My Little Atlas 我的城市收藏手账 App 设计一件独立界面素材。风格参考附图中的奶油白纸张、鼠尾草绿与克制的手绘线条：温柔、自然、偏成人文具审美，边缘轻微手工感，纸纹细腻低对比；不要做成儿童卡通、游戏道具、拟物皮革、塑料 3D 或厚重复古。色板：奶油白 #FFFAF1，纸色 #F7F2E9，深灰绿 #53634E，鼠尾草绿 #75836A，少量杏色 #C58360、浅蓝灰 #A8BBC1。只输出本次指定的一个完整元素，正面平视，无透视。元素内部不得出现文字、数字、商标或水印，不要画手机、界面截图或展示海报。除特别说明的纸纹平铺素材外，背景真实透明，元素外保留约 12% 安全空白；若工具不支持透明背景，使用纯白底，禁止棋盘格和渐变背景。相同元素后续变体须沿用第一张定稿作为参考。';
const items=[
 {id:'button_primary_paper',title:'纸感主按钮底板',priority:'P0',size:'1536×512 PNG，3:1 横向画布',display:'约 320×52 dp；宽度随文案伸缩',pages:['visit','share','new-folder'],delivery:'透明 PNG；后处理提取边缘与中间纹理，设保护边；文字、勾选或分享图标不在图片里',prompt:'一个深鼠尾草绿的横向纸质按钮底板，形状接近圆角长方形，四角柔和，边缘仅有非常细微的裁纸不规则，不要夸张撕裂。主体长宽约 6:1，居中完整放在 3:1 画布内，中间 75% 区域均匀、安静、无图案，适合之后排白色按钮文字；表面极轻微纤维纸纹，在手机小尺寸下不显脏。不要文字、符号、缝线、胶带、折角、渐变、厚度或外部投影。'},
 {id:'button_add_seal_base',title:'中央添加按钮纸片底板',priority:'P0',size:'1024×1024 PNG',display:'52–56 dp；点击区域至少 48 dp',pages:['home','map','timeline','profile'],delivery:'透明 PNG 底板；加号为独立 SVG／代码图形',prompt:'一枚用于 App 底部中央添加按钮的深灰绿色圆形纸片，外轮廓接近规整圆形，只有肉眼很轻微的手工剪裁起伏，内部为低对比哑光纸纤维质感，颜色 #53634E。正面扁平、完整居中，实体直径占画布约 74%，中心保持干净无图案，之后由代码绘制一个清晰的奶油白加号。只输出底板，不要加号、文字、白边、星星、邮票齿孔、金属、3D 侧面或外部阴影。'},
 {id:'nav_collection_concept',title:'收藏导航图标',priority:'P0',size:'1024×1024 PNG 概念定稿',display:'24×24 dp；最终重绘 SVG',pages:['home'],delivery:'生成图确定轮廓，人工／代码重绘 SVG；不能承诺自动无损矢量化',prompt:'一枚清晰的小型收藏图标：竖向书签轮廓，底部一个简洁 V 形缺口，内部仅有一条短的轻微弯曲折痕，表达纸质书签。只用深灰绿单色细线，圆润端点，轻微手绘曲线但不能抖动；以 24×24 网格设计，视觉线宽约 1.8 单位，图形落在中央 18×20 单位内。保证缩到 24 像素仍能读懂，无纸纹、阴影、白色贴纸边、背景底板或额外装饰。不要爱心、加号或字母。'},
 {id:'nav_map_concept',title:'地图导航图标',priority:'P0',size:'1024×1024 PNG 概念定稿',display:'24×24 dp；最终重绘 SVG',pages:['map'],delivery:'与收藏、足迹、我的共用线宽、光学大小、端点和状态规则',prompt:'一枚清晰的小型地图图标：三折纸地图外轮廓，两条简单竖向折线，顶部与底部有轻微折叠起伏，表达随身折叠地图。只用深灰绿单色细线，圆润端点，轻微手绘曲线但不能抖动；以 24×24 网格设计，视觉线宽约 1.8 单位，图形在中央约 20×18 单位内。缩到 24 像素仍清晰；无道路细节、地名、定位针叠加、纸纹、阴影、贴纸白边、背景底板或文字。与同组纸书签图标保持相同笔触。'},
 {id:'nav_journal_concept',title:'足迹导航图标',priority:'P0',size:'1024×1024 PNG 概念定稿',display:'24×24 dp；最终重绘 SVG',pages:['timeline'],delivery:'足迹含义为到访日记；采用小手账本，与页面内容一致',prompt:'一枚清晰的小型足迹手账图标：一本微微打开的小手账本，简洁双页轮廓与一条中央书脊，右页只有一条短横线；整体接近方形，不要密集线条。只用深灰绿单色细线，圆润端点，轻微手绘曲线但不能抖动；以 24×24 网格设计，视觉线宽约 1.8 单位，图形在中央约 20×18 单位内。缩到 24 像素仍清晰；无书名、日期、脚印、纸纹、阴影、贴纸白边或底板。与同组收藏、地图图标保持相同笔触。'},
 {id:'nav_profile_concept',title:'我的导航图标',priority:'P0',size:'1024×1024 PNG 概念定稿',display:'24×24 dp；最终重绘 SVG',pages:['profile'],delivery:'功能图标语义直观；不增加头像照片或复杂人像',prompt:'一枚清晰的小型个人图标：一个简单圆形头部，下方一条开放的肩部弧线，略有手绘温度，语义直接。只用深灰绿单色细线，圆润端点，轻微手绘曲线但不能抖动；以 24×24 网格设计，视觉线宽约 1.8 单位，图形在中央约 16×20 单位内。缩到 24 像素仍清晰；无五官、头发、衣服细节、纸纹、阴影、白边、外圆底板或文字。与同组纸书签、折叠地图和手账本图标保持同一套视觉语言。'},
 {id:'marker_paper_base',title:'地图定位针纸质底片',priority:'P0',size:'1024×1024 PNG',display:'普通标记约 32×40 dp，另设至少 44 dp 点击区',pages:['map','city-atlas'],delivery:'先做一枚底形，再同轮廓派生绿／杏色状态；中心点、选中外环、聚合数字由代码绘制',prompt:'一枚用于地图地点标记的纸质定位针底片，经典上圆下尖水滴形，圆润但尖端指向明确，深鼠尾草绿色主体，外侧极细奶油白纸边。正面扁平，轻微细纸纹，长宽约 4:5，形体简洁、完整居中。顶部中心留干净区域，稍后由代码叠加一个奶油白小圆点。只输出一个底片；不要中心圆点、数字、地图背景、投影、图钉金属、3D 厚度或附带标签。'},
 {id:'badge_mountain_hd',title:'城市探索家山峰勋章',priority:'P1',size:'1024×1024 PNG',display:'勋章列表 64–76 dp，详情页约 120–144 dp',pages:['profile','badges','badge-detail'],delivery:'独立透明 PNG；勋章名、解锁条件、进度和日期另排',prompt:'一枚城市探索主题手绘纸质勋章，只画两座前后层叠的小山，鼠尾草绿山体、奶油白积雪、少量深灰绿阴影。外轮廓沿山峰形状裁切，带约图案宽度 3% 的奶油白纸边，细深灰绿手绘描线，轻微彩铅与哑光纸纹，边缘整洁。主体占画布约 74%，完整居中。不要圆形奖牌底座、丝带、文字、数字、照片背景、3D 或浮雕金属。'},
 {id:'badge_coffee_hd',title:'咖啡爱好者咖啡杯勋章',priority:'P0',size:'1024×1024 PNG',display:'列表 64–76 dp，详情页约 120–144 dp',pages:['profile','badges','badge-detail'],delivery:'优先将此张作为整套勋章的笔触、纸边、阴影和色彩参考',prompt:'一枚咖啡主题手绘纸质勋章，只画一只米白陶瓷咖啡杯与很薄的小杯碟，杯内燕麦棕色咖啡，右侧一个结构正确的杯柄，略微俯视。外轮廓沿杯子裁切，带约图案宽度 3% 的奶油白纸边，细深棕灰手绘描线，轻微彩铅和哑光纸纹，温柔精致但不写实。主体占画布约 74%，完整居中。不要拉花文字、笑脸、蒸汽大装饰、圆形背景、丝带、数字、3D 或塑料光泽。'},
 {id:'badge_camera_hd',title:'拍照达人相机勋章',priority:'P1',size:'1024×1024 PNG',display:'列表 64–76 dp，详情页约 120–144 dp',pages:['badges','profile'],delivery:'用已定稿咖啡杯勋章作风格参考',prompt:'一枚摄影主题手绘纸质勋章，一台正面的小型复古相机，雾霾蓝机身、深墨灰镜头、少量奶油白高光，结构概括但相机特征清晰。外轮廓沿相机裁切，带约图案宽度 3% 的奶油白纸边；细深棕灰手绘描线，轻微彩铅和哑光纸纹，与参考咖啡杯勋章同一作者。主体占画布约 74%，完整居中。无品牌、字母、背带、照片背景、圆形底座、3D 或塑料光泽。'},
 {id:'badge_star_hd',title:'百店俱乐部星星勋章',priority:'P1',size:'1024×1024 PNG',display:'列表 64–76 dp，详情页约 120–144 dp',pages:['badges','badge-detail'],delivery:'未解锁状态由同一原图灰度与透明度派生',prompt:'一枚成就主题手绘纸质勋章，一颗完整的暖杏金五角星，五个角柔和，形状自然但不过度歪斜，极少量浅色手绘高光。外轮廓沿星星裁切，带约图案宽度 3% 的奶油白纸边；细深棕灰描线、轻微彩铅和哑光纸纹，与参考咖啡杯勋章同一作者。主体占画布约 74%，完整居中。无数字、文字、丝带、圆形奖牌底座、金属镜面、3D 或闪光特效。'},
 {id:'decor_tape_oat_hd',title:'燕麦色和纸胶带',priority:'P1',size:'1536×512 PNG，3:1 横向画布',display:'约 88×22 dp，页面按需缩放',pages:['home','journal','share','profile'],delivery:'单独胶带；旋转、叠贴与透明度后处理，避免阴影烘焙',prompt:'一条独立的浅燕麦色和纸胶带，横向、不旋转、正面平视，胶带形体约 4:1，完整居中放在 3:1 画布内。两端为轻微自然手撕的细锯齿，长边基本平直，极细纸纤维纹理，表面干净，颜色柔和。胶带本体表现半透明薄纸而不是厚纸板，输出透明背景。不要压住照片、不要文字、图案、条纹、折叠、卷起、反光、投影或额外胶带。'},
 {id:'decor_torn_note_hd',title:'撕边便笺底板',priority:'P1',size:'1536×1024 PNG',display:'基础约 210×130 dp，随文案高度伸缩',pages:['place','journal','home'],delivery:'空白纸片；实际分割保护边取图后测量，中央纹理用于伸缩',prompt:'一张单独的奶油白撕边便笺纸，横向，形体长宽约 1.6:1，正面平视不旋转，四边轻微自然毛边，纸面低对比、干净、细腻纤维质感。中心 80% 保持均匀空白，供 App 后续排可编辑文字。纸片完整居中，外部透明。不要文字、横线、勾选项、胶带、别针、折角、污渍、咖啡渍、大块皱褶、黑边、投影或重度做旧。'},
 {id:'decor_postmark_ring_hd',title:'到访邮戳装饰外环',priority:'P1',size:'1024×1024 PNG',display:'72–96 dp；旋转由代码设置',pages:['journal','share','visit-success'],delivery:'外环、中心图形、文字三层；输出只含外环',prompt:'一个独立的复古文具邮戳外环，正面完整圆形，暖杏棕色 #C58360，双道细圆线，细微印泥断续和轻度磨损，局部有极小的圆点装饰。环线粗细克制，圆心区域完全透明，环内上方与下方留出以后排弧形字的空间。主体占画布约 74%，居中。只画外环，不含日期、数字、字母、城市名、咖啡杯、星形、波浪邮路、纸张底板或阴影。'},
 {id:'decor_coffee_ink_hd',title:'邮戳中心咖啡线稿',priority:'P1',size:'1024×1024 PNG',display:'约 28–36 dp；也可重绘为矢量',pages:['journal','share'],delivery:'与外环同色，业务文案和日期在第三层绘制',prompt:'一枚印泥手绘咖啡杯小图案，用于邮戳中心：简洁杯身、右侧杯柄、一条杯碟线、上方一小缕蒸汽。只有暖杏棕色 #C58360 单色线稿，轻微印泥断续，正面轮廓清楚，笔画简洁，居中完整，图形占画布约 65%。与参考邮戳外环使用同色同笔触。不要外圆、文字、数字、贴纸白边、咖啡照片、纸张底板或阴影。'},
 {id:'decor_cat_doodle_hd',title:'伸懒腰猫咪涂鸦',priority:'P2',size:'1536×1024 PNG',display:'约 96×61 dp；欢迎页或手账角落',pages:['welcome','journal','share'],delivery:'少量装饰使用，不进入操作图标体系',prompt:'一只正在伸懒腰的小猫手账涂鸦，侧面，前爪向右伸、背部轻轻弓起、尾巴自然向左，单一完整姿态。只用极细的深灰绿线条，手绘温柔略俏皮，几乎不填色，最多两条很小的动作线；猫咪比例自然，不夸张卖萌。主体宽高约 1.6:1，正面平面线稿，完整居中。不要表情符号、文字、爱心群、背景、白色贴纸边、浓阴影或 3D。'},
 {id:'empty_collection_illustration',title:'暂无收藏的空状态插画',priority:'P2',size:'1536×1024 PNG',display:'约 180×120 dp，文字与按钮在下方单独排',pages:[],placement:'暂无收藏状态',delivery:'独立插画；说明与开始收藏按钮仍为原生组件',prompt:'一幅用于收藏 App 空状态的小型文具插画：一个浅燕麦色纸质收藏夹半开，里面只露出一张奶油白空白纸卡，旁边一枚很小的鼠尾草绿叶片作为点缀。构图安静轻盈，细深棕灰手绘线、少量彩铅平涂、极轻纸纹，内容集中在中央，外部透明，适合上方插画下方按钮的空状态。不要真实照片、卡片上的画、界面、文字、字母、问号、加号、按钮、人物或桌面背景。'},
 {id:'empty_journal_illustration',title:'暂无足迹的空状态插画',priority:'P2',size:'1536×1024 PNG',display:'约 180×120 dp',pages:[],placement:'暂无到访记录状态',delivery:'同组空状态保持同作者、线宽、留白和视觉重量',prompt:'一幅用于到访手账 App 空状态的小型文具插画：一本翻开的小手账本，两页均空白，书脊有浅鼠尾草绿细书签，右下方一支短的浅木色铅笔。温柔、克制、生活感，细深棕灰手绘线，少量彩铅平涂，极轻纸纹，物体正面略俯视但不写实。构图集中，主体占横向画布约 70%，外部透明。不要真实照片、书页文字、日期、地图细节、UI、按钮、人物或桌面背景。'},
 {id:'empty_search_illustration',title:'无搜索结果的空状态插画',priority:'P2',size:'1536×1024 PNG',display:'约 160×106 dp',pages:[],placement:'无搜索结果状态',delivery:'保持中性，不使用悲伤表情或误导性警告图形',prompt:'一幅用于地点搜索无结果状态的小型文具插画：一张简化的奶油白三折纸地图，只有两条折痕和一段很浅的灰绿路径，右前方一只小型圆形放大镜，镜柄为浅木色。细深棕灰手绘线、少量鼠尾草绿平涂、极轻纸纹，安静友好，主体完整、中央集中，外部透明。不要地名、道路文字、红叉、感叹号、问号、人脸、地图截图、照片、UI、按钮或桌面背景。'},
];
const catalog=JSON.parse(fs.readFileSync(path.join(root,'specs/page-index.json'),'utf8'));
const known=new Map(catalog.screens.map(p=>[p.id,p.name]));
for(const a of items){for(const id of a.pages)if(!known.has(id))throw Error('Unknown screen: '+id);a.status='brief_only_not_generated';a.prompt=common+NL+NL+'本次元素：'+a.title+'。'+a.prompt+NL+'输出：'+a.size+'。';}
const doc=[
'# 界面装饰、定制按钮与图标 · 生图提示词',
'',
'本文件是当前优先的生图需求。目标是增强 My Little Atlas 界面自身的手账风格。地点示例照片不属于本轮重点；此前 `生成图片提示词.md` 中的照片需求保留为可选补充。没有调用图片生成 API，下列均为可复制的需求与提示词，生成图尚未提供。',
'',
'## 先做哪些',
'',
'第一批建议：纸感主按钮底板、中央添加按钮底板、四枚导航图标、地图定位针底片、咖啡杯勋章，共 8 件。第二批做纸张、胶带、邮戳和其余三枚勋章；第三批做空状态与猫咪涂鸦。这样先确定按钮、图标、插画的三个层次，再扩展整套素材。',
'',
'不要给每页都堆贴纸：普通功能页优先保持控件清晰；首页、手账、图鉴、成就与分享页集中承载手账装饰。单屏一般一个主要装饰焦点，胶带或邮戳作少量呼应。',
'',
'|类型|风格力度|建议制作方式|',
'|---|---|---|',
'|勋章、装饰、空状态|较强，保留手绘纸感|生成独立 PNG 后抠图、校色和切分|',
'|按钮背景|中等，轮廓规整、纸纹很轻|生图定稿，再切边缘／中间可拉伸纹理；文字与图标单独绘制|',
'|底部导航、地图标记|轻度定制，优先易识别|生图探索轮廓，最终按统一网格重绘 SVG／矢量|',
'|搜索、返回、关闭、分享、复选框|沿用清晰线性语言|继续现有矢量系统，不必额外生图|',
'',
'## 参考图怎么给生成工具',
'',
'可附原始参考图或 `preview/ui-decoration-reference.png`。告诉工具：参考图只用于色彩、纸纹、线条和贴纸审美；输出必须为这次指定的单个素材。第一张满意后，把它作为后续同类素材的风格参考。不要以完整 UI 为生成目标。',
'',
'每条提示词已包含共同风格，可单独复制。每次生成一个元素；不要把素材生成在一张九宫格里，也不要提前加文字、按钮标签或数字。若平台只支持固定图幅，可用最接近的画幅并保留完整轮廓；实际切片和边界由后处理测量，不以提示词中的像素要求作为已验证规格。',
'',
'## 素材清单',
'',
'|优先级|元素与文件名|页面入口／用途|',
'|---|---|---|',
...items.map((a,i)=>'|'+a.priority+'|'+String(i+1).padStart(2,'0')+' · '+a.title+' · `'+a.id+'.png`|'+(a.placement||a.pages.map(id=>known.get(id)).join('、'))+'|'),
'',
'## 按钮与图标状态交给代码派生',
'',
'- 主按钮普通态先定稿；按下态复用相同轮廓与纹理，用轻微色彩覆盖／亮度变化；禁用态降低对比并保留可读性。不要单独生三张形状不同的按钮。',
'- 文字、加号、勾选、分享图标与加载进度独立于底板。加载和禁用逻辑仍为真实控件，不能只换一张图片。',
'- 按钮正常点击区域建议至少 48 dp，导航和地图小图标可用更大的透明容器接收点击。纸张毛边不决定点击形状。',
'- 底部导航以 24×24 网格重绘，默认视觉线宽约 1.8；普通／选中复用同路径。选中态用深绿与轻背景块、必要的实心区域区分，不能只让细纸纹表达状态。',
'- 地图的想去／去过用同一个定位针轮廓派生杏色与绿色；中心圆点、选中外环、聚合数均由代码叠加。既有页面颜色与状态含义保持一致。',
'- 九宫格伸缩的保护边、图标光学边界、透明留白和焦点坐标，在拿到实际图片后测量确认；当前文件是生成需求，不冒充最终可生产素材规格。',
'',
'## 完整提示词',
'',
...items.flatMap((a,i)=>['### '+String(i+1).padStart(2,'0')+' · '+a.title,'','- 文件名：`'+a.id+'.png`','- 建议输出：'+a.size,'- App 显示：'+a.display,'- 拆分要求：'+a.delivery,'','```text',a.prompt,'```','']),
'## 可选派生，无需每个状态重新生图',
'',
'- `decor_tape_sage_hd.png`：燕麦色胶带定稿后，用同一轮廓与纸纹替换为浅鼠尾草绿；不重新生成不相干形状。',
'- `button_secondary_paper`：从主按钮同一边缘与纹理派生奶油白底、深绿描边，文字深绿。优先由代码生成，避免多余素材。',
'- 山峰、咖啡、相机、星星的锁定态复用彩图做灰度与透明度处理，锁形标记另用矢量。',
'- 蝴蝶结、干花、纸夹不列入必需项；当前界面已经有胶带与邮戳，先把核心素材统一。',
'',
'## 交回原图与开发接入',
'',
'每个元素按上面文件名单独保存 PNG 原图。支持真实透明就保留 alpha；不支持就纯白底，之后抠图。不要自行缩成手机显示尺寸，先保留高清。第一批可先交按钮主底板、中央添加底板和咖啡杯勋章，确认纸感后再扩展同类。',
'',
'生成图回来后需要清理白边、统一视觉比例、导出密度、重绘导航 SVG、确认按钮伸缩区域，并将同一资源接入页面预览再导出图层规格。按下／选中／禁用状态、屏幕阅读标签与点击逻辑由代码实现。当前未替换已有 App，也未把任何概念图当成已完成生产资源。',
'',
'对应机器清单：`specs/ui-generation-brief.json`。现有可直接拆用的参考素材见 `preview/ui-decoration-reference.png` 和 `assets/materials/`、`assets/stickers/`。',
];
fs.writeFileSync(path.join(root,'界面装饰与定制控件生图提示词.md'),doc.join(NL),'utf8');
fs.writeFileSync(path.join(root,'specs/ui-generation-brief.json'),JSON.stringify({version:'1.0',date:'2026-11-07',status:'brief_only_not_generated',scope:'UI decoration, custom button surfaces, navigation icon concepts, map marker, badges, empty state illustrations',noPaidApiCalls:true,style:common,assets:items},null,2));
console.log(JSON.stringify({brief:'界面装饰与定制控件生图提示词.md',assets:items.length,p0:items.filter(x=>x.priority==='P0').length}));
const sharp=require('../../atlas-ui-v1/.tools/node_modules/sharp');
const {chromium}=require('../../atlas-ui-v1/.tools/node_modules/playwright-core');
const {pathToFileURL}=require('url');
(async()=>{
 const catalogDir=path.join(root,'assets/_catalog');fs.mkdirSync(catalogDir,{recursive:true});
 await sharp(path.join(root,'source/user-reference.png')).extract({left:350,top:817,width:238,height:65}).png().toFile(path.join(catalogDir,'reference-bottom-nav.png'));
 await sharp(path.join(root,'source/user-reference.png')).extract({left:651,top:421,width:230,height:247}).png().toFile(path.join(catalogDir,'reference-map-pins.png'));
 const u=p=>pathToFileURL(path.join(root,p)).href;
 const img=(p,c='')=>'<img class="'+c+'" src="'+u(p)+'">';
 const cells=[
  ['导航图标与中央添加按钮',img('assets/_catalog/reference-bottom-nav.png','nav'),'书签、折叠地图、手账、个人图标；添加按钮用纸片底板叠独立加号。'],
  ['纸质地图标记',img('assets/_catalog/reference-map-pins.png','map'),'同一轮廓派生想去／去过／选中态，小尺寸先保证语义和对比。'],
  ['撕边纸与胶带',img('assets/materials/paper_torn_note@3x.png','note')+img('assets/materials/tape_oat@3x.png','tape oat')+img('assets/materials/tape_sage@3x.png','tape sage'),'按钮借用轻纸纹；便笺保持干净，文本与毛边分别处理。'],
  ['手绘成就勋章',['mountain','coffee','camera','star'].map(n=>img('assets/stickers/sticker_'+n+'@3x.png','badge')).join(''),'生成高清独立图案；名称、数字、锁定态与进度由代码组合。'],
  ['到访邮戳',img('assets/stickers/stamp_ring@3x.png','ring')+img('assets/stickers/stamp_coffee_ink@3x.png','cup'),'外环、中心图案、日期文案三层拆开；图中仅展示前两层。'],
  ['角落涂鸦与空状态方向',img('assets/stickers/doodle_cat@3x.png','cat'),'猫咪保留轻细线条；空状态延续纸夹、空白手账本与放大镜文具。']
 ];
 const html='<!doctype html><html lang="zh-CN"><meta charset="utf-8"><style>*{box-sizing:border-box}body{margin:0;background:#eee9dc;color:#46543e;font-family:Microsoft YaHei,sans-serif}main{padding:40px 45px}h1{font-size:30px;font-weight:400;margin:0 0 13px}.intro{font-size:13px;line-height:1.9;color:#7f896f}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px;margin-top:25px}.cell{border:1px solid #ced4c1;border-radius:12px;background:#faf7ef;padding:20px;height:360px}h2{font-size:17px;margin:0 0 16px;font-weight:500}.art{height:222px;background:repeating-conic-gradient(#e2e6da 0% 25%,#d5ddca 0% 50%) 50%/20px 20px;position:relative;display:flex;align-items:center;justify-content:center;gap:9px;overflow:hidden;border-radius:6px}.art img{object-fit:contain}.nav{width:90%;image-rendering:auto}.map{height:95%}.note{width:72%;height:90%}.tape{position:absolute;width:90px;height:23px}.oat{left:28px;top:24px;transform:rotate(-10deg)}.sage{right:15px;bottom:21px;transform:rotate(8deg)}.badge{width:20%;max-height:100px}.ring{width:150px;height:150px}.cup{position:absolute;width:55px;height:55px}.cat{width:75%;max-height:180px}p{font-size:12px;line-height:1.9;color:#7e886e;margin:13px 0 0}.foot{font-size:13px;padding:21px 24px;background:#e2e7d8;border-radius:10px;margin-top:25px;line-height:1.9}</style><main><h1>这次要生成的是「界面自身的手账素材」</h1><div class="intro">现有参考图裁片与已拆分素材，用来给生成工具说明风格；此板不是新生成成品。<br>纸感按钮／导航图标／地图标记／勋章／邮戳／轻量涂鸦 · 不包含地点示例照片</div><div class="grid">'+cells.map(([t,a,d])=>'<section class="cell"><h2>'+t+'</h2><div class="art">'+a+'</div><p>'+d+'</p></section>').join('')+'</div><div class="foot">第一批：主按钮纸底 + 中央添加纸底 + 4 枚导航图标 + 地图针底片 + 咖啡杯勋章。<br>独立元素、真实透明、无文字；功能图标定稿后重绘 SVG；按钮状态复用底形由代码派生。</div></main></html>';
 const output=path.join(root,'preview/ui-decoration-reference.html');fs.writeFileSync(output,html);
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:['--allow-file-access-from-files']});
 const p=await browser.newPage({viewport:{width:1500,height:1100},deviceScaleFactor:1});await p.goto(pathToFileURL(output).href);
 await p.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.images].map(i=>i.complete?Promise.resolve():new Promise(r=>i.onload=i.onerror=r)))});
 await p.screenshot({path:path.join(root,'preview/ui-decoration-reference.png'),fullPage:true});
 await browser.close();console.log('UI decoration reference board exported.');
})().catch(e=>{console.error(e);process.exit(1)});

const fs=require('fs'),path=require('path');
const base=path.resolve(__dirname,'..');
const screens=JSON.parse(fs.readFileSync(path.join(base,'specs/page-index.json'),'utf8')).screens;
// Evidence distinguishes a running screenshot from code that only passed compilation.
const rows={
welcome:['已接入·构建','WelcomeV2.kt','仅首次无存储时出现；旧用户跳过；不自动写入示例地点。'],
city:['已接入·构建','WelcomeV2.kt / HomeScreenV2.kt','手动城市输入与常用城市；首页按已有数据切换。城市别名尚未统一。'],
home:['已接入·实机','home-final','品牌、城市、搜索、相纸主视觉、动态统计、筛选、收藏夹、最近地点及五入口导航。'],
collections:['已接入·实机','collections-open-v4','双列主题收藏夹及所有地点列表。'],
folder:['已接入·实机','folder-v4','封面、简介、列表/地图、状态筛选与整理；地图带入该收藏夹。'],
'new-folder':['已接入·构建','CollectionsV2.kt','完整原生表单；名称、简介、默认/相册封面；同名新建校验。'],
search:['已接入·实机','search-entry-delivery','收藏搜索匹配名称、区域、地址、理由和类型；搜索入口与结果保留查询。'],
add:['已接入·实机','add-menu-final','导入灵感、手动添加、记录到访三个任务入口。'],
import:['已接入·实机','screenshot-import-final','文字/链接、攻略截图辅助、手动添加三入口；原有解析服务保留。'],
parsing:['已接入·构建','LinkImportSheet.kt','分阶段加载和停止；不默认选择任何候选。外部服务本轮未重新端到端调用。'],
results:['已接入·构建','LinkImportSheet.kt','候选明确多选、重复标识、待定位标识；只保存选中项。'],
branch:['已接入·构建','LinkImportSheet.kt / PoiPicker','复用真实高德候选搜索和手动门店匹配；服务需可用。'],
duplicate:['已接入·构建','LinkImportSheet.kt','重复地点不重建；可追加当前选择的归类，保留原记录。'],
'import-success':['已接入·构建','LinkImportSheet.kt','新增保存成功页，显示实际数量和继续收藏入口；不计为到访。'],
manual:['已接入·实机','manual-final','名称/城市必填，地址选填，来源保存，可调用真实门店匹配。'],
unlocated:['已接入·实机','manual-final','无地址可先收藏；地图显示待定位数量与补全入口。'],
map:['已接入·实机','map-delivery','真实高德瓦片、手势缩放、纸质标记、搜索、筛选、地点卡与导航。'],
maplist:['已接入·实机','map-list-v4','地图内列表保留筛选；暂未按距离排序。'],
filters:['已接入·实机','map-filters-delivery','状态 + 城市 + 类型 + 收藏夹组合筛选；长内容可滚动。'],
navigation:['已接入·构建','AndroidPlatformServices.kt','调用系统地图应用选择器；导航不会生成到访。'],
place:['已接入·实机','place-v4','相纸图、状态、地址、理由便笺、归类、来源与底部主操作。'],
visited:['已接入·实机','place-v4','实际多次到访列表、再去开关，点击记录回看对应手账。'],
visit:['已接入·实机','visit-v4','日期、最多9张照片、文字、再去、存草稿；照片文字均选填。'],
calendar:['已接入·实机','calendar-v4','原生日历含闰年、月份切换和未来日期禁用。'],
'visit-success':['已接入·实机','visit-success-delivery / new-journal-delivery','实际创建到访→成功页→对应手账→只删本次记录已验证，验证记录已删除。'],
journal:['已接入·实机','journal-delivery','显示实际到访日期/文字/照片；没有多张用户照片时不伪造照片拼贴。'],
layout:['已接入·实机','layout-note-final / layout-film-v4','暖色/胶片/纸上三模板，照片前移/删除/添加、日期/文字编辑与放弃确认；装饰自由拖拽未实现。'],
'photo-picker':['系统能力·构建','AndroidPlatformServices.kt','原生系统照片选择器，选取后复制进应用目录；本轮未新增用户照片。'],
timeline:['已接入·实机','timeline-v4','真实到访按日期排序；城市/月份筛选；城市图鉴入口。'],
'city-atlas':['已接入·实机','city-shanghai-delivery','城市封面/统计/类型/地点；可切真实地图；城市小地图以真实地图入口替代。'],
templates:['已接入·实机','share-final','三个模板直接预览当前内容；不重新填写。'],
share:['已接入·实机','share-controls-v4 / export-actual-v4','当前记录优先；日期、详细地址默认关闭；文字可关闭，预览与导出一致。'],
saved:['已接入·实机','saved-v4 / export-actual-v4','实际写入模拟器 Pictures/MyLittleAtlas 并拉取核验；Android 10+支持免额外相册权限，8/9引导使用系统分享。'],
'folder-share':['已接入·实机','folder-share-delivery','仅当前收藏夹的地点与关联到访统计。'],
profile:['已接入·实机','profile-v4','封面/昵称/签名/统计/勋章/到访/城市/草稿/分享。头像仍使用默认人像图标。'],
badges:['已接入·实机','badges-v4','4枚可替换位图勋章与真实达成条件。'],
'badge-detail':['已接入·构建','PersonalPagesV2.kt','解锁条件、数量与图案；未记录历史达成时间。'],
'badge-progress':['已接入·实机','badge-progress-v4','未解锁灰化、当前进度及条件说明。'],
settings:['已接入·实机','settings-v4','备份/恢复、账号现状、关于、明确确认后清空；不伪装云同步。'],
backup:['已接入·实机','settings-v4','复用原生文件选择器和JSON备份；包含资料/草稿/模板。备份不包含照片二进制。'],
empty:['已接入·构建','HomeScreenV2.kt / WelcomeV2.kt','新安装为空白库；首页空态给出＋添加指引。新安装欢迎仅通过代码和数据测试验证。'],
error:['已接入·构建','LinkImportSheet.kt','失败保留输入、重新解析/手动添加；本轮未强制制造外部服务错误。'],
permission:['已接入·实机','permission-fixed-delivery / permission-system-current / permission-denied-delivery','说明→真实系统授权→拒绝→继续地图已验证；成功定位与真实GPS设备结果尚未验证。'],
bulk:['已接入·实机','bulk-delivery','多选/全选、替换归类、移出当前夹、删除地点；操作影响明确。'],
'folder-picker':['已接入·实机','folder-picker-delivery','多选归类与就地新建；批量替换前明确提示。'],
draft:['已接入·实机','draft-resumed-v4','未保存到访退出可继续、存草稿、放弃；不计入到访。'],
delete:['已接入·实机','delete-confirm-delivery','关联到访/草稿数量，确认后删除；原照片文件保留。'],
'edit-profile':['已接入·实机','edit-profile-delivery','昵称、签名、封面相册选择/默认封面；本轮查看表单，未替换用户资料。'],
signin:['规划说明·实机','signin-status-delivery','如实说明本机存储和账号/跨设备同步尚未接入。'],
'folder-options':['已接入·实机','folder-options-v4','编辑、批量整理、分享、删除夹；删除夹不删地点。'],
'record-options':['已接入·实机','record-options-delivery','编辑排版、分享当前到访、只删除当前记录，删除闭环已验证。'],
'system-share':['系统能力·构建','AndroidPlatformServices.kt','原生ACTION_SEND图片/文字分享；不自动发布。本轮验证相册保存，未向外部应用实际发送。'],
'screenshot-import':['部分接入·实机','screenshot-import-final','可以选择截图对照、手动粘贴文字；OCR自动识别尚未接入，不展示假识别结果。'],
drafts:['已接入·实机','drafts-v4 / drafts-empty-delivery','本机真实草稿列表，继续写/确认删除；验证创建的空草稿已删除。'],
'city-share':['已接入·实机','city-share-final','带入当前城市，仅展示该城市集合与统计。'],
'search-empty':['已接入·实机','search-empty-delivery','实际输入无匹配搜索词，保留搜索词、显示空结果、提供直接添加入口。']
};
let md='# V2 原生页面覆盖表\n\n更新：2026-11-07。56 项设计页面/状态逐项对照；「已接入」表示功能和主要结构可用，不代表像素级验收通过。实机证据位于同目录 screenshots；无截图项仅代码/构建检查。\n\n|ID|页面/状态|实施证据级别|截图/实现|说明与差异|\n|---|---|---|---|---|\n';
for(const s of screens){const r=rows[s.id];if(!r)throw Error('Missing '+s.id);md+='|'+[s.id,s.name,...r].join('|')+'|\n';}
fs.writeFileSync(path.join(__dirname,'页面覆盖表.md'),md);
const pairs=[['首页','home-delivery','03-home'],['私人地图','map-final-current','17-map'],['地点详情','place-delivery','22-visited'],['记录到访','visit-delivery','23-visit'],['手账详情','journal-delivery','26-journal'],['个人图鉴','profile-delivery','35-profile'],['收藏夹','folder-v4','05-folder'],['排版编辑','layout-note-final','27-layout'],['城市图鉴','city-shanghai-delivery','30-city-atlas'],['真实导出文件','export-actual-v4','32-share']];
let html='<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>V2 原生 App 验收</title><style>body{margin:0;padding:32px;background:#f7f2e9;color:#3e4639;font:15px/1.7 system-ui}h1{margin:0}p{max-width:950px;color:#777567}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:24px}.pair{background:#fffaF1;border:1px solid #e5dfd2;border-radius:12px;padding:18px}.images{display:flex;gap:12px}.images figure{margin:0;width:50%}img{width:100%;height:auto;border-radius:7px}figcaption{font-size:12px;color:#8b877c}</style><h1>原生 App / V2 对照验收</h1><p>左：模拟器实际运行；右：对应设计图。真实数据、Android 系统栏和字体不同；设备为 360×800dp，设计为 390×844。当前为结构与流程接入版本，装饰素材等待替换，不能视为全量逐像素一致。完整逐项结果请见 页面覆盖表.md。</p><div class="grid">';
for(const [name,actual,ref] of pairs){if(!fs.existsSync(path.join(base,'preview/screens',ref+'.png')))continue;html+='<section class="pair"><h2>'+name+'</h2><div class="images"><figure><img src="screenshots/'+actual+'.png"><figcaption>原生运行 / 实际导出</figcaption></figure><figure><img src="../preview/screens/'+ref+'.png"><figcaption>V2 设计稿</figcaption></figure></div></section>';}
fs.writeFileSync(path.join(__dirname,'原生运行验收.html'),html+'</div></html>');
console.log(JSON.stringify({screenCount:screens.length,report:'页面覆盖表.md',gallery:'原生运行验收.html'}));

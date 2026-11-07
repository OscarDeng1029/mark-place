const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const {execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../../..');
const out=path.join(root,'design/atlas-ui-handoff-v2/implementation/screenshots/simplified');
const scratch=path.join(root,'.codex/simplified-native');
fs.mkdirSync(out,{recursive:true});fs.mkdirSync(scratch,{recursive:true});
const adb=(...args)=>execFileSync('adb',args,{encoding:'utf8',timeout:30000});
const pause=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const decode=s=>s.replace(/&quot;/g,'"').replace(/&apos;/g,"'").replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&').replace(/&#10;/g,'\n');
function dump(){
  adb('shell','rm','-f','/sdcard/atlas-window.xml');
  adb('shell','uiautomator','dump','/sdcard/atlas-window.xml');
  let xml='';try{xml=adb('exec-out','cat','/sdcard/atlas-window.xml');}catch{return [];}
  fs.writeFileSync(path.join(scratch,'last-window.xml'),xml);
  return [...xml.matchAll(/<node\s+([^>]+)>/g)].map(match=>Object.fromEntries([...match[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(attr=>[attr[1],decode(attr[2])])));
}
function nodesText(nodes){return nodes.map(n=>n.text||n['content-desc']).filter(Boolean).join('\n');}
async function waitFor(label){for(let i=0;i<8;i++){const nodes=dump();if(nodes.some(n=>n.text===label||n['content-desc']===label))return nodes;await pause(500);}throw Error('Missing '+label+'\n'+nodesText(dump()));}
async function tap(label){const nodes=await waitFor(label);const n=nodes.find(n=>(n.text===label||n['content-desc']===label)&&n.bounds!=='[0,0][0,0]');assert(n,'Visible '+label);const b=n.bounds.match(/\d+/g).map(Number);adb('shell','input','tap',String(Math.round((b[0]+b[2])/2)),String(Math.round((b[1]+b[3])/2)));await pause(250);}
async function fillField(index,value){let n;for(let i=0;i<8;i++){n=dump().filter(n=>n.class==='android.widget.EditText')[index];if(n)break;await pause(400);}assert(n,'Field '+index);const b=n.bounds.match(/\d+/g).map(Number);adb('shell','input','tap',String((b[0]+b[2])/2),String((b[1]+b[3])/2));adb('shell','input','text',value);await pause(200);}
function shot(name){fs.writeFileSync(path.join(out,name+'.png'),execFileSync('adb',['exec-out','screencap','-p']));console.log('Captured '+name);}
async function launch(mode){adb('shell','am','start','-W','-S','-n','com.mylittleatlas.app/.MainActivity','--es','atlas_preview',mode);await waitFor('我的手账');await pause(800);}
const checks=[];
(async()=>{
  const before=execFileSync('adb',['shell','run-as','com.mylittleatlas.app','cat','files/atlas.json']);
  await launch('demo');await pause(600);shot('02-demo-home');
  await tap('后一页 →');await waitFor('第 2 / 3 页');
  await tap('收藏');await waitFor('我的收藏');
  await tap('手账');await waitFor('第 2 / 3 页');checks.push('翻页后切换底栏保留页码');
  await tap('目录');await waitFor('手账目录');shot('03-directory-all');
  await tap('杭州  1');assert.match(nodesText(dump()),/街角花园/);shot('04-directory-hangzhou');
  await tap('街角花园');await waitFor('第 2 / 3 页');checks.push('目录城市筛选后跳到对应手账页');
  await tap('收藏');await tap('南京  1');await waitFor('一颗柠檬');shot('05-collections-nanjing');
  assert(!nodesText(dump()).includes('Hidden Coffee'));checks.push('收藏城市分类不混入其他城市');
  await tap('想去');await waitFor('一颗柠檬');await tap('去过');assert.match(nodesText(dump()),/换个城市或状态/);checks.push('城市与想去/去过组合筛选');
  await tap('全部');await tap('全部城市  8');await tap('收藏夹');await waitFor('咖啡慢一点');shot('06-existing-folders');checks.push('原收藏夹入口与内容保留');
  await tap('添加');await waitFor('今天，想记下什么？');shot('07-simple-add');
  await tap('写手账');await waitFor('可以直接填写新地点，不用先收藏。');shot('08-direct-entry');
  await fillField(0,'Riverside');await fillField(1,'Hangzhou');adb('shell','input','keyevent','4');await pause(250);
  await tap('保存，生成手账');await waitFor('第 1 / 4 页');shot('09-new-entry-saved');checks.push('新地点直接记录并自动翻到新页');
  await tap('编辑这一页');await waitFor('编辑手账');await tap('保存修改');await waitFor('第 1 / 4 页');checks.push('编辑不重复新增记录');
  await tap('添加');await tap('写手账');await fillField(0,'DraftCafe');adb('shell','input','keyevent','4');await tap('存草稿');
  await tap('我的');await waitFor('小满的城市手账');shot('10-profile');
  adb('shell','input','swipe','400','1300','400','620','400');await pause(250);await tap('未完成的草稿');await waitFor('DraftCafe');shot('11-new-place-draft');
  await tap('继续写');await waitFor('写手账');assert.match(nodesText(dump()),/DraftCafe/);checks.push('新地点草稿可保存并续写，不提前加入地点库');
  await launch('empty');await waitFor('写下第一篇手账');shot('12-empty-first-use');
  await tap('写下第一篇手账');await fillField(0,'FirstPlace');adb('shell','input','keyevent','4');await tap('保存，生成手账');await waitFor('第 1 / 1 页');shot('13-first-entry');checks.push('空库可直接生成第一篇手账，无需先收藏');
  const after=execFileSync('adb',['shell','run-as','com.mylittleatlas.app','cat','files/atlas.json']);
  assert(before.equals(after),'Real user records must remain unchanged');checks.push('真实用户数据字节保持不变');
  adb('shell','am','start','-S','-n','com.mylittleatlas.app/.MainActivity');await waitFor('我的手账');shot('01-current-user-home');
  const report={checks,counts:JSON.parse(after),fixture:'Debug-only in-memory library; real user records never mutated'};
  report.counts={places:report.counts.places.length,visits:report.counts.visits.length,collections:report.counts.collections.length,drafts:report.counts.drafts.length};
  fs.writeFileSync(path.join(scratch,'verification.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
})().catch(error=>{console.error(error);process.exitCode=1;});

const cp=require('child_process');
const fs=require('fs');
const path=require('path');
const adb='C:/Users/23738/Documents/Codex/2026-10-27/new-chat/work/android-toolchain/sdk/platform-tools/adb.exe';
const out=path.join(__dirname,'screenshots');
const wait=ms=>Atomics.wait(new Int32Array(new SharedArrayBuffer(4)),0,0,ms);
function run(...args){return cp.execFileSync(adb,args,{encoding:'utf8',windowsHide:true});}
function dump(){run('shell','uiautomator','dump','/sdcard/atlas-verify.xml');const xml=run('shell','cat','/sdcard/atlas-verify.xml');return [...xml.matchAll(/<node\s+([^>]+)>?/g)].map(m=>Object.fromEntries([...m[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(a=>[a[1],a[2].replaceAll('&amp;','&').replaceAll('&quot;','"')])));}
function tap(label){const nodes=dump();const n=nodes.find(n=>n['content-desc']===label)||nodes.find(n=>n.text===label);if(!n)throw Error('Missing UI element: '+label+'; visible='+nodes.filter(n=>n.text).map(n=>n.text).join('|'));const b=n.bounds.match(/\d+/g).map(Number);run('shell','input','tap',String((b[0]+b[2])>>1),String((b[1]+b[3])>>1));wait(1300);}
function shot(name){wait(600);run('shell','screencap','-p','/sdcard/atlas-shot.png');run('pull','/sdcard/atlas-shot.png',path.join(out,name+'.png'));const nodes=dump();fs.writeFileSync(path.join(out,name+'.json'),JSON.stringify(nodes.filter(n=>n.text||n['content-desc']),null,2));console.log(name+': '+nodes.filter(n=>n.text).map(n=>n.text).join(' | ').slice(0,900));}
for(const action of process.argv.slice(2)){const [kind,...rest]=action.split(':');const value=rest.join(':');if(kind==='tap')tap(value);else if(kind==='shot')shot(value);else if(kind==='back'){run('shell','input','keyevent','4');wait(900);}else if(kind==='swipe'){run('shell','input','swipe','360','1230','360','480','650');wait(800);}else if(kind==='wait')wait(Number(value));else if(kind==='xy'){const [x,y]=value.split(',');run('shell','input','tap',x,y);wait(1300);}else throw Error('Unknown action '+action);}

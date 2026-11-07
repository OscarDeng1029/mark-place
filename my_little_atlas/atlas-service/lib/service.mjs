import {randomUUID} from 'node:crypto';
import {AppError,fetchNote,extractShops} from './core.mjs';
import {autoMatchShops} from './matcher.mjs';

export const textField=(value,max)=>typeof value==='string'?value.trim().slice(0,max):'';
export function validateParse(data){
  if(typeof data.text==='string'&&data.text.trim().length>30000)throw new AppError('正文超过 30000 字，请分批导入或编辑后继续。',413);
  const text=textField(data.text,30000),url=textField(data.url,8000),city=textField(data.city,40);
  if(!city)throw new AppError('请先填写店铺所在城市，帮助匹配正确门店。');
  if(!text&&!url)throw new AppError('请粘贴笔记链接或正文。');
  if(text&&text.length<4)throw new AppError('正文太短，请补充店名和推荐内容。');
  const input={text,url,city,recognitionMode:['auto','ai','rules'].includes(data.recognitionMode)?data.recognitionMode:'auto',deferMatching:data.deferMatching===true};
  if(text&&['device-ocr','device-text'].includes(data.inputSource)){
    input.inputSource=data.inputSource;
    input.importedNote={title:textField(data.importedNote?.title,100),author:textField(data.importedNote?.author,100),sourceUrl:textField(data.importedNote?.sourceUrl,8000)};
    if(data.inputSource==='device-ocr')input.ocr={imageCount:Math.max(0,Math.min(8,Number(data.ocr?.imageCount)||0))};
  }
  return input;
}
export async function parseNote(data,config,{fetcher=fetch}={}){
  const started=Date.now();
  const {text,url,city,recognitionMode,deferMatching}=validateParse(data);
  let note;
  try{note=text?{title:text.split('\n')[0].slice(0,70),text,images:[],author:'手动导入的笔记',sourceUrl:''}:await fetchNote(url,fetcher);}
  catch(error){
    console.warn('[note-read]',JSON.stringify({code:error.code||'NOTE_READ_ERROR',host:error.upstreamHost,cause:error.cause?.code||error.cause?.cause?.code}));
    if(error instanceof AppError)throw error;
    throw new AppError('读取笔记失败，请稍后重试。',502,'NOTE_READ_ERROR');
  }
  const deviceInput=['device-ocr','device-text'].includes(data.inputSource)&&!!text;
  if(deviceInput){
    const imported=data.importedNote||{};
    note.title=textField(imported.title,100)||note.title;
    note.author=textField(imported.author,100)||'安卓导入';
    try{const source=new URL(imported.sourceUrl);if(source.protocol==='https:'&&/^(?:[a-z0-9-]+\.)*(?:xiaohongshu\.com|xhslink\.(?:com|cn))$/.test(source.hostname))note.sourceUrl=source.href;}catch{}
    note.inputSource=data.inputSource;
    if(data.inputSource==='device-ocr')note.ocr={engine:'mlkit-chinese-bundled',imageCount:Math.max(0,Math.min(8,Number(data.ocr?.imageCount)||0)),characters:text.length};
  }
  const readMs=Date.now()-started,recognitionStarted=Date.now();
  const result=await extractShops(note,{...config,...(deviceInput?{aiVision:false}:{})},fetcher,recognitionMode);
  const recognitionMs=Date.now()-recognitionStarted;
  if(deviceInput&&result.recognition)result.recognition.inputSource=data.inputSource;
  const shops=result.shops.map(s=>({...s,id:randomUUID(),status:'unresolved',place:null,candidates:[]}));
  const matching=deferMatching
    ? {warnings:[],summary:{method:'deferred',matched:0,total:shops.length,elapsedMs:0}}
    : await autoMatchShops(shops,note,city,config.amapKey,{...config},{fetcher});
  result.warnings.push(...matching.warnings);
  if(!shops.length)result.warnings.push('暂未识别到店铺。试试把店名分别换行，或使用「添加店铺」补充。');
  if(shops.length>=40)result.warnings.push('单次最多识别 40 家店，长合集建议分段解析，避免遗漏。');
  return {note:{...note,images:[]},shops,city,method:result.method,warnings:result.warnings,recognition:result.recognition||null,modelError:result.modelError||null,matching:matching.summary,matchingDeferred:deferMatching,timings:{readMs,recognitionMs,matchingMs:matching.summary.elapsedMs,totalMs:Date.now()-started},demo:false};
}
export async function readNoteForDevice(data,{fetcher=fetch}={}){
  const url=textField(data.url,8000);
  if(!url)throw new AppError('请粘贴小红书笔记链接。');
  const note=await fetchNote(url,fetcher);
  return {note:{title:note.title,text:note.text,author:note.author,sourceUrl:note.sourceUrl,images:note.images}};
}
export function validateMatch(data){
  const city=textField(data.city,40);
  if(!city)throw new AppError('请填写店铺所在城市。');
  if(!Array.isArray(data.shops)||!data.shops.length||data.shops.length>40)throw new AppError('请提交 1 至 40 家店铺。');
  const shops=data.shops.map((s,index)=>({id:textField(s?.id,100)||`retry-${index}`,name:textField(s?.name,65),addressHint:textField(s?.addressHint,180),evidence:textField(s?.evidence,250),category:['coffee','food','dessert','other'].includes(s?.category)?s.category:'other'}));
  if(shops.some(s=>!s.name))throw new AppError('店铺名称不能为空。');
  return {shops,city,title:textField(data.title,100),text:textField(data.text,30000)};
}
export async function matchShops(data,config,{fetcher=fetch}={}){
  const {shops,city,title,text}=validateMatch(data);
  if(!config.amapKey)throw new AppError('请先连接高德 Web 服务 Key。',422,'KEY_REQUIRED');
  const matching=await autoMatchShops(shops,{title,text},city,config.amapKey,{...config},{fetcher});
  return {shops,warnings:matching.warnings,matching:matching.summary};
}

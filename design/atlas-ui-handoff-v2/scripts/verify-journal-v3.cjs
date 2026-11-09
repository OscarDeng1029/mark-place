const {chromium}=require('../../atlas-ui-v1/.tools/node_modules/playwright-core');
const assert=require('node:assert/strict');
const path=require('node:path'),fs=require('node:fs');
const {pathToFileURL}=require('node:url');
const root=path.resolve(__dirname,'../preview/journal-v3');
const out=path.join(root,'exports');fs.mkdirSync(out,{recursive:true});
const url=pathToFileURL(path.join(root,'index.html')).href;
const styles=['daily','duo','square','postcard','film','collect','quiet'];
(async()=>{
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:['--no-sandbox','--allow-file-access-from-files']});
 const errors=[],checks=[],frames=[];
 const page=await browser.newPage({viewport:{width:1440,height:1050},deviceScaleFactor:1.5,reducedMotion:'reduce'});
 page.on('pageerror',e=>errors.push(e.message));
 const ready=async()=>{await page.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.images].map(i=>i.complete?Promise.resolve():new Promise(r=>i.onload=i.onerror=r)));});await page.waitForTimeout(120);};
 const visiblePoint=slot=>page.locator('.photo-canvas').nth(slot).evaluate(el=>{const b=el.getBoundingClientRect();for(const fy of [.5,.3,.7,.15,.85])for(const fx of [.25,.5,.75]){const x=b.left+b.width*fx,y=b.top+b.height*fy;if(document.elementFromPoint(x,y)?.closest('.photo-canvas')===el)return{x,y};}throw Error('Photo needs an exposed area for interaction');});
 const tapPhoto=async(slot=0)=>{const p=await visiblePoint(slot);await page.mouse.click(p.x,p.y);};
 try{
  await page.goto(url);await ready();
  await page.screenshot({path:path.join(out,'interactive-preview.png'),fullPage:true});
  for(const style of styles){
   await page.locator('[data-sample="mixed"]').click();await page.locator(`.intro [data-style="${style}"]`).click();await ready();
   await page.locator('.phone').screenshot({path:path.join(out,style+'.png')});
   for(const sample of ['mixed','landscape','portrait']){
    await page.locator(`[data-sample="${sample}"]`).click();await ready();
    const result=await page.evaluate(()=>{
     const phone=document.querySelector('.phone').getBoundingClientRect(),book=document.querySelector('.book').getBoundingClientRect();
     const photos=[...document.querySelectorAll('.photo-canvas')].map(el=>{const b=el.getBoundingClientRect(),img=el.querySelector('img');return {fit:getComputedStyle(img).objectFit,width:el.offsetWidth,height:el.offsetHeight,boxRatio:el.offsetWidth/el.offsetHeight,photoRatio:img.naturalWidth/img.naturalHeight,left:b.left,right:b.right,top:b.top,bottom:b.bottom};});
     const stamp=document.querySelector('.coffee-stamp'),overlap=stamp?(()=>{
      const b=stamp.getBoundingClientRect(),cx=(b.left+b.right)/2,cy=(b.top+b.bottom)/2,r=stamp.offsetWidth/2;
      const rects=[...document.querySelectorAll('.photo-canvas')].map(el=>{const b=el.getBoundingClientRect(),angle=parseFloat(el.closest('.photo-frame').style.getPropertyValue('--angle'))*Math.PI/180;return{cx:(b.left+b.right)/2,cy:(b.top+b.bottom)/2,w:el.offsetWidth,h:el.offsetHeight,angle};});
      let circle=0,covered=0;
      for(let x=-r+.5;x<r;x++)for(let y=-r+.5;y<r;y++)if(x*x+y*y<r*r){circle++;if(rects.some(p=>{const dx=cx+x-p.cx,dy=cy+y-p.cy;return Math.abs(Math.cos(p.angle)*dx+Math.sin(p.angle)*dy)<p.w/2&&Math.abs(-Math.sin(p.angle)*dx+Math.cos(p.angle)*dy)<p.h/2;}))covered++;}
      return covered/circle;
     })():0;
     const writing=document.querySelector('.writing-zone'),nb=writing.getBoundingClientRect();
     return {photos,overlap,note:{position:writing.dataset.position,shape:writing.dataset.shape,width:writing.offsetWidth,height:writing.offsetHeight,top:nb.top,bottom:nb.bottom},book:{left:book.left,right:book.right,top:book.top,bottom:book.bottom},phone:{left:phone.left,right:phone.right},stage:document.querySelector('.photo-stage').clientHeight,header:document.querySelector('.app-header').clientHeight};
    });
    assert.equal(result.header,49);
    for(const photo of result.photos){if(style==='square'){assert.equal(photo.fit,'cover');assert(Math.abs(photo.boxRatio-1)<.03,`${style}/${sample}: square photo window ${photo.boxRatio}`);}else{assert.equal(photo.fit,'contain');assert(Math.abs(photo.boxRatio-photo.photoRatio)<.045,`${style}/${sample}: preserve photo ratio ${photo.boxRatio} vs ${photo.photoRatio}`);}assert(photo.left>=result.phone.left&&photo.right<=result.phone.right,`${style}: photos within phone`);}
    assert(result.overlap<.5,`${style}/${sample}: stamp overlap below half, actual ${result.overlap}`);
    if(result.photos.length===2){const [a,b]=result.photos;assert(Math.min(a.right,b.right)>Math.max(a.left,b.left)&&Math.min(a.bottom,b.bottom)>Math.max(a.top,b.top),`${style}/${sample}: two photos should overlap`);}
    assert(result.note.bottom<result.book.bottom-20,`${style}: note clear of bottom actions`);
    frames.push({style,sample,...result});
   }
  }
  checks.push('Six adaptive layouts preserve original image ratio; square keepsake uses a square crop window','Header is 49px','Stamp overlap is below half its circular area, sampling actual rotated photo rectangles');
  const notePositions=frames.filter(x=>x.sample==='mixed'&&['duo','film','collect'].includes(x.style)).map(x=>x.note.position);
  assert.equal(new Set(notePositions).size,3);
  const duo=frames.find(x=>x.style==='duo'&&x.sample==='mixed');
  assert.equal(duo.note.width,duo.note.height,'Duo uses a square sticky note');
  const squareLayout=frames.find(x=>x.style==='square'&&x.sample==='mixed');
  assert.equal(squareLayout.photos.length,1,'Square keepsake uses one photo without inventing a second image');
  assert.equal(squareLayout.note.width,squareLayout.note.height,'Square keepsake uses a square writing card');
  const newArea=duo.photos.reduce((sum,p)=>sum+p.width*p.height,0),oldArea=Math.pow((duo.book.right-duo.book.left-55)/2-12,2)*duo.photos.reduce((sum,p)=>sum+1/p.photoRatio,0);
  assert(newArea>oldArea*2,'Double-photo area should more than double compared with the old two-column arrangement');
  checks.push('All automatic double-photo layouts overlap; example total photo area more than doubles','Square sticky note, square keepsake card, lower-left clipping and upper-left cut paper occupy distinct positions');
  await page.locator('[data-sample="mixed"]').click();await page.locator('.intro [data-style="square"]').click();await ready();
  assert.equal(await page.locator('.photo-canvas.cropped').count(),1,'Square layout starts as a square crop');
  await tapPhoto();await page.locator('[data-mode="fit"]').click();await page.locator('[data-apply]').click();await ready();
  assert.equal(await page.locator('.photo-canvas.cropped').count(),0,'Square layout can restore the complete photo');
  checks.push('Square layout defaults to a true square crop and can restore the complete original photo');
  await page.locator('[data-sample="mixed"]').click();await page.locator('.intro [data-style="duo"]').click();
  for(const paper of ['sticky','torn','clipping','lined']){await page.locator(`.intro [data-paper="${paper}"]`).click();assert(await page.locator(`.phone .note-paper.${paper}`).count());}
  await page.locator('.intro [data-paper="auto"]').click();
  await page.locator('[data-arrangement="stack"]').click();await ready();assert.equal(await page.locator('.photo-stage').getAttribute('data-arrangement'),'stack');
  await page.locator('[data-arrangement="side"]').click();await ready();assert.equal(await page.locator('.photo-stage').getAttribute('data-arrangement'),'side');
  await page.locator('[data-arrangement="auto"]').click();await ready();
  for(const shape of ['square','cut','strip']){await page.locator(`button[data-shape="${shape}"]`).click();await ready();assert.equal(await page.locator('.writing-zone').getAttribute('data-shape'),shape);}
  await page.locator('button[data-shape="auto"]').click();await ready();
  const layerBefore=await page.evaluate(()=>mainState.frontSlot);
  await page.locator('[data-action="swap-layers"]').click();await ready();
  assert.notEqual(await page.evaluate(()=>mainState.frontSlot),layerBefore);
  const stampAfterSwap=await page.locator('.coffee-stamp').getAttribute('data-photo-overlap');assert(Number(stampAfterSwap)<.5);
  await page.locator('[data-action="swap-layers"]').click();await ready();
  await page.locator('.note-paper').click();assert(await page.locator('.reading').count());await page.keyboard.press('Escape');
  checks.push('Square/strip/cut paper options, front/back swap and reading the floating note all work');
  checks.push('All four paper styles and manual side/stack arrangements work');
  const indices=await page.evaluate(()=>[...mainState.indices]);
  const b=await visiblePoint(0);
  await page.mouse.move(b.x,b.y);await page.mouse.down();await page.mouse.move(b.x-65,b.y,{steps:6});await page.mouse.up();await ready();
  const after=await page.evaluate(()=>({indices:mainState.indices,style:mainState.style}));assert.equal(after.indices[0],(indices[0]+1)%5);assert.equal(after.indices[1],indices[1]);assert.equal(after.style,'duo');
  checks.push('Photo swipe changes only that photo, preserving the other photo and notebook page');
  await tapPhoto();assert(await page.locator('.sheet').count());
  await page.locator('[data-mode="crop"]').click();await page.locator('[data-ratio="0.75"]').click();
  const cropRatio=await page.locator('.focus-preview').evaluate(el=>el.clientWidth/el.clientHeight);
  assert(Math.abs(cropRatio-.75)<.03,'Crop preview must use the same aspect ratio as the saved photo frame');
  await page.locator('[data-range="x"]').fill('27');await page.locator('[data-range="zoom"]').fill('1.35');
  await page.locator('[data-apply]').click();await ready();assert.equal(await page.locator('.photo-canvas.cropped').count(),1);
  await tapPhoto();assert.equal(await page.locator('[data-range="x"]').inputValue(),'27');
  await page.locator('[data-reset]').click();await page.locator('[data-apply]').click();await ready();assert.equal(await page.locator('.photo-canvas.cropped').count(),0);
  checks.push('User crop mode saves aspect, focus and zoom; returning to full image restores original ratio');
  await tapPhoto();await page.locator('[data-mode="crop"]').click();await page.keyboard.press('Escape');assert.equal(await page.locator('.sheet').count(),0);assert.equal(await page.locator('.photo-canvas.cropped').count(),0);
  await page.locator('[data-action="edit"]').click();await page.locator('.sheet [data-style="postcard"]').click();await page.keyboard.press('Escape');assert.equal(await page.evaluate(()=>mainState.style),'duo');
  await page.locator('[data-action="edit"]').click();await page.locator('.sheet [data-style="film"]').click();await page.locator('[data-apply-page]').click();assert.equal(await page.evaluate(()=>mainState.style),'film');
  await page.locator('[data-action="directory"]').click();await page.locator('[data-dir-style="collect"]').click();assert.equal(await page.evaluate(()=>mainState.style),'collect');
  checks.push('Crop and page editors cancel without saving; chosen layout applies on confirmation; directory switches layouts');
  const uploadFile=path.resolve(root,'../../assets/photos/photo_dessert.jpg');
  const chooser=page.waitForEvent('filechooser');await page.locator('.intro [data-action="upload"]').click();await(await chooser).setFiles(uploadFile);await ready();
  assert.equal(await page.evaluate(()=>mainState.photos[0].w),1000);assert.equal(await page.evaluate(()=>mainState.photos[0].h),1293);assert.equal(await page.locator('.photo-canvas').count(),1);
  checks.push('Local image upload reads actual dimensions, remains local and falls back to single-photo layout');
  const panoramic=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=2400;c.height=600;const ctx=c.getContext('2d');ctx.fillStyle='#b7c7a5';ctx.fillRect(0,0,2400,600);ctx.fillStyle='#963e31';ctx.fillRect(0,0,30,600);ctx.fillRect(2370,0,30,600);return c.toDataURL('image/png').split(',')[1];});
  const panoramaChooser=page.waitForEvent('filechooser');await page.locator('.intro [data-action="upload"]').click();await(await panoramaChooser).setFiles({name:'panorama-4-to-1.png',mimeType:'image/png',buffer:Buffer.from(panoramic,'base64')});await ready();
  const panorama=await page.locator('.photo-canvas').first().evaluate(el=>({ratio:el.offsetWidth/el.offsetHeight,fit:getComputedStyle(el.querySelector('img')).objectFit}));
  assert(Math.abs(panorama.ratio-4)<.06);assert.equal(panorama.fit,'contain');
  checks.push('A user-uploaded 4:1 panorama keeps its full original ratio, including both edges');
  const square=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=900;c.height=900;const ctx=c.getContext('2d');ctx.fillStyle='#cec4a5';ctx.fillRect(0,0,900,900);return c.toDataURL('image/png').split(',')[1];});
  const squareChooser=page.waitForEvent('filechooser');await page.locator('.intro [data-action="upload"]').click();await(await squareChooser).setFiles({name:'square.png',mimeType:'image/png',buffer:Buffer.from(square,'base64')});await ready();
  const squareRatio=await page.locator('.photo-canvas').first().evaluate(el=>el.offsetWidth/el.offsetHeight);assert(Math.abs(squareRatio-1)<.02);
  checks.push('A square upload keeps a square photo frame without a template forcing it wide');
  await page.setViewportSize({width:375,height:1000});await page.goto(url);await ready();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.locator('.phone').screenshot({path:path.join(out,'mobile-375.png')});
  checks.push('375px mobile view has no horizontal overflow');
  await page.setViewportSize({width:1440,height:1120});await page.goto(url+'?view=board');await ready();
  await page.locator('.board').screenshot({path:path.join(out,'seven-layouts.png')});
  await page.screenshot({path:path.join(out,'three-layouts.png'),clip:{x:0,y:0,width:1364,height:1050}});
  await page.goto(url+'?view=board&set=double');await ready();await page.locator('.board').screenshot({path:path.join(out,'overlap-double-layouts.png')});
  assert.deepEqual(await page.evaluate(()=>[...document.images].filter(i=>!i.naturalWidth).map(i=>i.src)),[]);
  assert.deepEqual(errors,[]);
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({checks,errors,frames},null,2));console.log(JSON.stringify({checks,errors,output:out},null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

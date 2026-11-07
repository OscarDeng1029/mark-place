const {chromium}=require('../../atlas-ui-v1/.tools/node_modules/playwright-core');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const root=path.resolve(__dirname,'../preview/simplified-v1');
const output=path.join(root,'exports');
const url=pathToFileURL(path.join(root,'index.html')).href;
fs.mkdirSync(output,{recursive:true});

(async()=>{
  const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:['--no-sandbox','--allow-file-access-from-files']});
  const errors=[],checks=[];
  try{
    const page=await browser.newPage({viewport:{width:1440,height:1160},deviceScaleFactor:1.5});
    page.on('pageerror',error=>errors.push(error.message));
    async function ready(){
      await page.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.images].map(image=>image.complete?Promise.resolve():new Promise(resolve=>{image.onload=resolve;image.onerror=resolve;})));});
      assert.deepEqual(await page.evaluate(()=>[...document.images].filter(image=>!image.naturalWidth).map(image=>image.src)),[],'All preview materials should load');
    }
    for(const name of ['flow','entry','navigation']){
      await page.goto(url+'?board='+name);await ready();
      await page.locator('.board').screenshot({path:path.join(output,'board-'+name+'.png')});
    }
    for(const name of ['home','directory','add','editor','saved','empty','collections','map','profile']){
      await page.goto(url+'?single=1&screen='+name);await ready();
      await page.locator('.phone').screenshot({path:path.join(output,name+'.png')});
    }
    await page.goto(url+'?single=1');await ready();
    assert.equal(await page.locator('.leaf').count(),1);
    assert.match(await page.locator('.leaf-place').innerText(),/Hidden Coffee/);
    await page.locator('[data-action="directory"]').click();
    await page.locator('#directory-search').fill('衡山');
    assert.equal(await page.locator('.directory-row').count(),1);
    await page.locator('.directory-row').click();
    assert.equal(await page.locator('.sheet').count(),0);
    assert.match(await page.locator('.leaf-place').innerText(),/衡山/);
    checks.push('目录搜索并直接翻到对应记录，无独立足迹页面');
    await page.locator('[data-tab="collections"]').click();
    await page.locator('[data-tab="home"]').click();
    assert.match(await page.locator('.page-meta').innerText(),/第 2 \/ 3 页/);
    await page.keyboard.press('ArrowLeft');
    assert.match(await page.locator('.page-meta').innerText(),/第 1 \/ 3 页/);
    checks.push('翻页及切换底栏后保留页码');
    await page.locator('[data-action="add"]').click();
    assert.equal(await page.locator('.action-choice').count(),2);
    await page.locator('[data-action="write"]').click();
    assert.equal(await page.locator('#save-visit').isDisabled(),true);
    await page.locator('#place-input').fill('周末河边');
    await page.locator('#note-input').fill('今天，听了很久的风。');
    await page.locator('#save-visit').click();
    assert.equal(await page.locator('.editor-body').count(),0);
    assert.match(await page.locator('.leaf-place').innerText(),/周末河边/);
    assert.match(await page.locator('.page-meta').innerText(),/第 1 \/ 4 页/);
    assert.equal(await page.locator('.blank-photo').count(),1);
    checks.push('新地点无需预先收藏，照片选填，保存后直接成为手账');
    await page.locator('[data-action="directory"]').click();
    assert.equal(await page.locator('.directory-row').count(),4);
    await page.locator('.sheet [data-action="close"]').click();
    await page.locator('[data-action="edit-visit"]').click();
    await page.locator('#note-input').fill('修改这一天的心情。');
    await page.locator('#save-visit').click();
    assert.match(await page.locator('.page-meta').innerText(),/4 页/);
    assert.match(await page.locator('.memory-note').innerText(),/修改这一天/);
    checks.push('编辑已有记录不增加重复页面');
    await page.locator('[data-action="add"]').click();
    await page.locator('[data-action="write"]').click();
    await page.locator('#place-input').fill('待续的小店');
    await page.locator('#note-input').fill('还没有写完。');
    await page.locator('[data-action="save-draft"]').click();
    await page.locator('[data-tab="profile"]').click();
    await page.locator('[data-action="drafts"]').click();
    await page.locator('[data-action="resume-draft"]').click();
    assert.equal(await page.locator('#note-input').inputValue(),'还没有写完。');
    checks.push('存草稿并从个人页继续写，不提前生成手账');
    await page.goto(url+'?single=1&screen=empty');
    assert.equal(await page.locator('.scene-photo').count(),0);
    await page.locator('[data-action="write"]').click();
    await page.locator('#place-input').fill('第一个地方');
    await page.locator('#save-visit').click();
    assert.match(await page.locator('.page-meta').innerText(),/第 1 \/ 1 页/);
    checks.push('首次使用不展示示例回忆，最少填写地点即可成页');
    await page.goto(url+'?single=1&screen=profile');
    await page.locator('[data-action="profile-map"]').first().click();
    assert.equal(await page.locator('.map-controls .filters .active').innerText(),'去过');
    assert.equal(await page.locator('.map-pin').count(),3);
    checks.push('个人城市足迹直达地图的去过筛选');
    await page.goto(url+'?single=1');
    await page.locator('[data-action="add"]').click();
    await page.locator('[data-action="collect"]').click();
    await page.locator('#collect-name').fill('想去的面包店');
    await page.locator('[data-action="save-place"]').click();
    assert.match(await page.locator('.place-grid').innerText(),/想去的面包店/);
    await page.locator('[data-tab="home"]').click();
    assert.match(await page.locator('.page-meta').innerText(),/3 页/);
    checks.push('收藏新地点只进入收藏，不生成到访记录');
    await page.setViewportSize({width:375,height:900});
    await page.goto(url);await ready();
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true);
    checks.push('375px 宽度无横向溢出');
    assert.deepEqual(errors,[],'No browser script errors');
    fs.writeFileSync(path.join(output,'verification.json'),JSON.stringify({checks,errors,assets:'Existing local photos, fonts, paper, tape and stickers; no paid image APIs',screens:9,boards:3},null,2));
    console.log(JSON.stringify({checks,errors,output},null,2));
  }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});

const fs=require('fs'),path=require('path');
const sharp=require('../../atlas-ui-v1/.tools/node_modules/sharp');
const root=path.resolve(__dirname,'..');
(async()=>{
 const ids=['sticker_mountain','sticker_coffee','sticker_camera','sticker_star','doodle_cat','stamp_coffee_ink'];
 const layers=[];for(let i=0;i<ids.length;i++){const s=await sharp(path.join(root,'assets/stickers',ids[i]+'@3x.png')).resize(190,150,{fit:'contain',background:'#e7e8df'}).png().toBuffer();layers.push({input:s,left:20+i*210,top:20})}
 await sharp({create:{width:1280,height:190,channels:4,background:'#e7e8df'}}).composite(layers).png().toFile(path.join(root,'source/alpha-inspection.png'));
 console.log('Alpha inspection ready');
})();

const assetBase='../assets/';
function materialImage(id,file,cls){const e=document.createElement('img');e.src=assetBase+file;e.className='mat-asset '+cls;e.dataset.asset=id;e.alt='';e.setAttribute('aria-hidden','true');return e}
function frame(node,id,file){if(node.querySelector(':scope >.mat-frame'))return;const photo=node.querySelector(':scope >img:not(.mat-asset)');if(!photo)return;photo.classList.add('mat-content-photo');node.appendChild(materialImage(id,'materials/'+file+'@3x.png','mat-frame'));node.appendChild(materialImage('tape_oat','materials/tape_oat@3x.png','mat-tape'));}
function applyMaterials(){
 document.querySelectorAll('.phone:not([data-materialized])').forEach(p=>{
  p.dataset.materialized='true';p.dataset.backgroundAsset='paper_cream_tile';
  p.querySelectorAll('.hero-photo').forEach(e=>frame(e,'frame_polaroid_thumb','frame_polaroid_thumb'));
  p.querySelectorAll('.place-hero,.journal-cover').forEach(e=>frame(e,'frame_polaroid_wide','frame_polaroid_wide'));
  p.querySelectorAll('.mini-collage>img').forEach(img=>{const box=document.createElement('div');box.className='mat-small-photo';img.replaceWith(box);box.appendChild(img);img.classList.add('mat-content-photo');box.appendChild(materialImage('frame_polaroid_small','materials/frame_polaroid_small@3x.png','mat-frame'))});
  p.querySelectorAll('.welcome-collage>img').forEach(img=>{const box=document.createElement('div');box.className='mat-welcome-photo';img.replaceWith(box);box.appendChild(img);img.classList.add('mat-content-photo');box.appendChild(materialImage('frame_polaroid_portrait','materials/frame_polaroid_portrait@3x.png','mat-frame'))});
  p.querySelectorAll('.badge').forEach(b=>{const i=b.querySelector('svg');if(!i)return;const d=i.innerHTML;let id=d.includes('m2 21')?'sticker_mountain':d.includes('M4 8h12')?'sticker_coffee':d.includes('M3 6h4')?'sticker_camera':'sticker_star';i.replaceWith(materialImage(id,'stickers/'+id+'@3x.png','mat-badge'))});
  p.querySelectorAll('.stamp').forEach(e=>{e.prepend(materialImage('stamp_ring','stickers/stamp_ring@3x.png','mat-ring'));const i=e.querySelector('svg');if(i)i.replaceWith(materialImage('stamp_coffee_ink','stickers/stamp_coffee_ink@3x.png','mat-cup'))});
  p.querySelectorAll('.profile-cover>.tape,.welcome-collage>.tape,.poster>.tape').forEach(e=>{const i=materialImage('tape_oat','materials/tape_oat@3x.png',e.closest('.profile-cover')?'mat-profile-tape':'tape');e.replaceWith(i)});
  p.querySelectorAll('.preview-ticket,.empty-photo').forEach(e=>e.appendChild(materialImage('tape_oat','materials/tape_oat@3x.png','mat-tape')));
  p.querySelectorAll('.note').forEach(e=>e.dataset.asset='paper_torn_note');
  p.querySelectorAll('img:not([data-asset])').forEach(e=>{const n=(e.getAttribute('src')||'').split('/').pop();if(n&&n.startsWith('photo_'))e.dataset.asset=n.replace('.jpg','')});
 });
}
const oldRender=render;render=function(){oldRender();applyMaterials()};
let queued=false;new MutationObserver(()=>{if(!queued){queued=true;queueMicrotask(()=>{queued=false;applyMaterials()})}}).observe(document.getElementById('app'),{childList:true,subtree:true});
window.applyMaterials=applyMaterials;
window.assetBase=assetBase;
render();applyMaterials();

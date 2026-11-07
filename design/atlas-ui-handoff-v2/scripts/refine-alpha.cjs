const fs=require('fs'),path=require('path');
const p=path.join(__dirname,'build-assets.cjs');let s=fs.readFileSync(p,'utf8');
s=s.replace("[974,1040,78,58],'M4 51 10 38 22 22 29 11 37 1 44 3 54 14 61 27 70 42 76 49 74 55 7 57 2 54Z'","[975,1041,77,55],'M4 48 9 36 22 20 30 7 37 1 45 5 53 17 61 29 72 46 72 51 6 52 2 50Z'");
s=s.replace("[1085,1038,63,62],'M31 1A30 30 0 1 1 30.9 1Z'","[1086,1040,60,56],'M30 1a28 27 0 1 1-.1 0Z'");
s=s.replace("[981,1127,70,66],'M35 1A32 32 0 1 1 34.9 1Z'","[982,1129,64,57],'M32 1a30 27 0 1 1-.1 0Z'");
s=s.replace("[1090,1127,64,65],'M32 1A31 31 0 1 1 31.9 1Z'","[1090,1129,62,56],'M31 1a29 27 0 1 1-.1 0Z'");
const a=s.indexOf(' const cupRect='),b=s.indexOf(' write(path.join(root,',a);
if(a>=0)s=s.slice(0,a)+` const cleanCup=svg(108,105,'<g transform="translate(10 9) scale(3.7)" fill="none" stroke="#b57f62" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8h12v6a6 6 0 0 1-12 0V8ZM16 9h2a3 3 0 0 1 0 6h-2M3 21h15M7 5V2m5 3V2"/></g>');await variants('stamp_coffee_ink',cleanCup,'stickers',[36,35],{type:'existing-icon-rasterization',construction:'Existing UI coffee outline; clean text-free asset, no source lettering.'},{bakedText:false});`+String.fromCharCode(10)+s.slice(b);
fs.writeFileSync(p,s);
const fs = require('fs');
const path = require('path');
const sharp = require('../../atlas-ui-v1/.tools/node_modules/sharp');
const root = path.resolve(__dirname, '../../..');
const xml = fs.readFileSync(path.join(root, 'my_little_atlas/composeApp/src/androidMain/res/drawable/ic_atlas.xml'), 'utf8');
const paths = [...xml.matchAll(/<path android:fillColor="([^"]+)" android:pathData="([^"]+)"/g)]
  .map(m => '<path fill="' + m[1] + '" d="' + m[2] + '"/>').join('');
sharp(Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="1024" height="1024">' + paths + '</svg>'))
  .flatten({ background: '#F5F0E7' }).png()
  .toFile(path.join(root, 'my_little_atlas/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png'))
  .then(() => console.log('iOS icon exported from the existing Android vector'));

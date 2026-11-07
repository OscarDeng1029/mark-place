# Compose 集成说明

`AtlasPhotoFrame.kt` 为未编译的集成示例，放在交付包内，未修改现有 App。它演示同一父级内的照片裁切、透明相框、胶带旋转、真实文字插槽和未解锁勋章。阴影、点击、页面排版和自适应安全区需要按对应页面规格接入。

## 资源放置

从本包复制到现有 `my_little_atlas/composeApp/src/commonMain/composeResources/drawable/`，只选一种密度，并重命名去除 `@`。本项目资源类命名空间为 `com.mylittleatlas.resources`。

|本包文件|建议资源名|使用场景|
|---|---|---|
|assets/photos/photo_cafe_interior.jpg|atlas_photo_cafe_interior.jpg|地点与手账大图|
|assets/materials/frame_polaroid_wide@3x.png|atlas_frame_wide.png|Wide，344 × 233 dp|
|assets/materials/frame_polaroid_thumb@3x.png|atlas_frame_thumb.png|Thumb，140 × 150 dp|
|assets/materials/frame_polaroid_small@3x.png|atlas_frame_small.png|Small，135 × 128 dp|
|assets/materials/frame_polaroid_portrait@3x.png|atlas_frame_portrait.png|Portrait，180 × 210 dp|
|assets/materials/tape_oat@3x.png|atlas_tape_oat.png|燕麦色胶带|
|assets/materials/tape_sage@3x.png|atlas_tape_sage.png|灰绿胶带|
|assets/materials/paper_torn_note@3x.png|atlas_note_torn.png|撕边便笺|
|assets/materials/paper_cream_tile.png|atlas_paper_tile.png|192 × 48 dp 平铺|
|assets/stickers/sticker_coffee@3x.png|atlas_sticker_coffee.png|咖啡勋章，标题与进度另画|

其它照片、贴纸同名加 `atlas_` 前缀即可。资源真实路径以 `specs/asset-manifest.json` 为准；若资源生成器不支持某个 SVG，再转为项目矢量格式，不要直接把图标截图。

调用形态：`AtlasPhotoFrame(photo = painterResource(Res.drawable.atlas_photo_cafe_interior), frame = painterResource(Res.drawable.atlas_frame_wide), contentDescription = 地点照片的本地化描述, tape = painterResource(Res.drawable.atlas_tape_oat), rotationDegrees = -1.5f) { Text(可编辑的说明文案) }`。其中描述和文案为业务字符串，示意表达式需要换成实际 Kotlin 字符串或状态变量。

固定 dp 使 3× 文件不会按图片像素撑大布局。外层尺寸、裁切、字型与行高仍以具体页面 JSON 为准；不要把某个组件的标准尺寸直接套到所有页面。

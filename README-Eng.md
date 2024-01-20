# SuperSpineViewer

A tool to load and export Spine animations  
**Java 21** required  
Pixel Buffers support required  
Exporting MOV requires FFmpeg  
Current version: 2.0.5

![霜叶](https://i0.hdslb.com/bfs/album/98b4fd8a12bc6dbf691b967bed625db67713dff0.png@518w.png "明日方舟 - 霜叶")

## Get SuperSpineViewer

[**Released Stable Version**](https://github.com/Aloento/SuperSpineViewer/releases/latest)

### Performance Settings Reference

* High Resolution (Camera) = High Memory Requirements
* Quality = Exponentially Higher Memory Requirements

**RAM**:

    4G+ = Extreme (240FPS)
    2G~ = Standard (120FPS)
    1G~ = Fast (60FPS)

### Sequence Usage Reference

Take importing After Effects as an example  
At default 30FPS, the Time Scaling setting

    Extreme = 800%
    Standard = 400%
    Fast = 200%

## Functionality and Compatibility

### Features

* Load Spine's Skel and Json
* Reload bones and textures
* View and play animations
* View and switch skins
* Changing the size
* Adjusting the position
* Adjusting the speed
* Loop Play
* Play Pause
* Export Transparent MOV
* Export Transparent PNG
* Adjust Performance Quality

### Supported Versions

| Version   | Support |
|-----------|---------|
| Spine 1.x | ×       |
| Spine 2.x | ?       |
| Spine 3.1 | ?       |
| Spine 3.2 | ?       |
| Spine 3.3 | ?       |
| Spine 3.4 | √       |
| Spine 3.5 | √       |
| Spine 3.6 | √       |
| Spine 3.7 | √       |
| Spine 3.8 | √       |
| Spine 4.0 | √       |
| Spine 4.1 | √       |
| Spine 4.2 | √       |

## Gallery

<html>
    <table style="margin-left: auto; margin-right: auto;">
        <tr>
            <td>
                <img src="https://i0.hdslb.com/bfs/album/73fdec47d907dc42e96a2d0d21482680fd7efb3f.png" alt="首屏加载画面">
                <img src="https://i0.hdslb.com/bfs/album/697ebe690460ee8a1f50a7bb4c4f973331b244dd.png" alt="Spine信息与导出">
                <img src="https://i0.hdslb.com/bfs/album/98b4fd8a12bc6dbf691b967bed625db67713dff0.png" alt="明日方舟 - 霜叶">
                <img src="https://i0.hdslb.com/bfs/album/79dbdaee161130460b77411f4664b4ecbd53d68e.png" alt="明日方舟 - 德克萨斯">
            </td>
            <td>
                <img src="https://i0.hdslb.com/bfs/album/56d918333fd302f9c221680008d7109fe090fb39.png" alt="明日方舟 - 闪灵">
                <img src="https://i0.hdslb.com/bfs/album/8ad8f6ca661f68909b30edce518d47614162a78f.png" alt="公主连结 - 凯留">
                <img src="https://i0.hdslb.com/bfs/album/51ee6aa61652191d4ab6c27a6e18bf8dc1997fdc.png" alt="方舟指令 - 湊阿库娅">
                <img src="https://i0.hdslb.com/bfs/album/0919e8d269e355c9b451d52e887c314a84f47faa.png" alt="万象物语">
            </td>
        </tr>
    </table>
</html>

## Build

Rewrite `MANIFEST.MF` of `org.lwjgl.lwjgl:lwjgl` to make `Sealed: false`

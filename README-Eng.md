# SuperSpineViewer

A tool to load and export Spine animations  
Requires Java 15 to run  
or use a packaged exe program  
Exporting MOV requires FFmpeg  
Current version: 1.1.0

![霜叶](https://i0.hdslb.com/bfs/album/98b4fd8a12bc6dbf691b967bed625db67713dff0.png@518w.png "明日方舟 - 霜叶")

## Get SuperSpineViewer

[**Released Stable Version**](https://github.com/Aloento/SuperSpineViewer/releases/latest)

[Running Dependencies](https://github.com/Aloento/SuperSpineViewer/releases/tag/R1.0.0)

[![Windows x64](https://github.com/Aloento/SuperSpineViewer/workflows/Windows%20x64/badge.svg "Windows x64自动构建")](https://github.com/Aloento/SuperSpineViewer/actions?query=workflow%3A%22Windows+x64%22)

### Information & About

It's essentially a practice piece for me to learn Java and OpenJFX  
While I try to make sure it works as well as possible  
I can't guarantee that it will work well or have most of the features

This tool uses the highly customizable Spine Runtime  
Spine Universal LibGDX Runtimes

### Bugs

The current rendering implementation takes up a lot of unnecessary RAM  
Will try to migrate when DriftFX is stable  
Currently requires system support for Pixel Buffers

If this appears [(This is actually a bug in the JDK)](https://bugs.openjdk.java.net/browse/JDK-8192647)  
`[warning][gc,alloc] SavePNG: Retried waiting for GCLocker too often allocating %d words`  
Then it means that there is a problem during **GC**  
In this case, frames will be **lost** and **lagged**  
and FFmpeg will get stuck when processing  
This can only be solved by restarting the program  
Please adjust the JVM parameters to allow more memory allocation  
Or reduce the **Resolution** or **Quality** and improve the **Performance**

### Performance Settings Reference

* High Resolution (Camera) = High Memory Requirements
* Quality = Exponentially Higher Memory Requirements
* Performance = Higher CPU Requirements, But Memory Usage Decreases As Performance Increases

**RAM**:

    4G+ = Extreme (240FPS)
    3G~ = Standard (120FPS)
    2G~ = Fast (60FPS)

**CPU**

    High-End = High (12)
    General = Normal (6)
    Mobile = Low (3)

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

* Spine 2.x ×
* Spine 3.1 √
* Spine 3.2 √
* Spine 3.3 √
* Spine 3.4 √
* Spine 3.5 √
* Spine 3.6 √
* Spine 3.7 √
* Spine 3.8 √
* Spine 4.0 √

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

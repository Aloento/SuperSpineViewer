# SuperSpineViewer

[**For English User**](https://github.com/Aloento/SuperSpineViewer/blob/master/README-Eng.md)

一个拿来加载与导出Spine动画的工具  
一定需要Java15才能运行  
或使用打包好的exe程序  
导出MOV需要FFmpeg  
当前版本：1.2.3

![霜叶](https://i0.hdslb.com/bfs/album/98b4fd8a12bc6dbf691b967bed625db67713dff0.png@518w.png "明日方舟 - 霜叶")

## 获得SuperSpineViewer

[**发布的稳定版本**](https://github.com/Aloento/SuperSpineViewer/releases/latest)

[通用运行依赖](https://github.com/Aloento/SuperSpineViewer/releases/tag/R1.0.0)

[![Windows x64](https://github.com/Aloento/SuperSpineViewer/workflows/Windows%20x64/badge.svg "Windows x64自动构建")](https://github.com/Aloento/SuperSpineViewer/actions?query=workflow%3A%22Windows+x64%22)

### 须知 & 关于

它本质上是我学习Java和OpenJFX的练习作品  
我虽然尽可能保证它能正常使用  
但是不能保证它好用  
也不能保证拥有大多数功能

本工具使用高度定制的Spine运行时  
Spine Universal LibGDX Runtimes

### 已知问题

目前渲染实现会占用大量不必要的带宽  
在DriftFX稳定后会尝试迁移  
目前要求系统支持像素缓冲区

如果出现 [（这其实是JDK的一个Bug）](https://bugs.openjdk.java.net/browse/JDK-8192647)  
`[warning][gc,alloc] SavePNG: Retried waiting for GCLocker too often allocating %d words`  
则说明内存回收时出现问题  
这种情况下一定会丢帧，卡顿  
并且FFmpeg处理时会卡住  
这时只能重启程序解决  
请调整JVM参数以允许更多内存分配  
或者降低分辨率或质量与提高性能

### 性能设置参考

* 高分辨率(Camera) = 高内存需求
* 高质量(Quality) = 成倍增加的内存需求
* 高性能(Performance) = 更高的CPU需求，但内存占用随着性能的增加而减少

**RAM**:

    4G+ = Extreme (240FPS)
    3G~ = Standard (120FPS)
    2G~ = Fast (60FPS)

**CPU**

    高性能 = High (12)
    普通 = Normal (6)
    低压 = Low （3）

### 序列使用参考

以导入After Effects为例  
默认30FPS时，伸缩设置

    Extreme = 800%
    Standard = 400%
    Fast = 200%

## 功能与兼容性

### 实现的功能

* 加载Spine的Skel和Json
* 重载骨骼与贴图
* 查看与播放动画
* 查看与切换皮肤
* 变换大小
* 调整位置
* 调整速度
* 循环播放
* 播放暂停
* 导出透明MOV
* 导出透明PNG
* 调整性能质量

### 支持的版本

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

## 画廊

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

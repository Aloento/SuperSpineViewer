# SuperSpineViewer

一个拿来加载与导出Spine动画的工具  
至少需要Java15才能运行  
或使用打包好的exe程序  
当前版本：0.1.25

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

如果出现  
`[warning][gc,alloc] SavePNG: Retried waiting for GCLocker too often allocating &d words`  
则说明内存可能不足  
这种情况下有很大概率丢帧，卡顿  
请调整JVM参数以允许更多分配内存  
或者降低分辨率或质量  

### 性能设置参考

* 高分辨率(Camera) = 高内存需求
* 高质量(Quality) = 成倍增加的内存需求
* 高性能(Performance) = 更高的CPU需求，但内存占用随着性能的增加而减少

**RAM**:

    4G+ = Extreme
    3G~ = Standard
    2G~ = Fast

**CPU**

    高性能 = High
    普通 = Normal
    低压 = Low

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
* 设置性能

### 支持的版本

* Spine 2.1 ●
* Spine 3.1 √
* Spine 3.2 √
* Spine 3.3 √
* Spine 3.4 √
* Spine 3.5 √
* Spine 3.6 √
* Spine 3.7 √
* Spine 3.8 √
* Spine 4.0 ●

## 画廊

![首屏](https://i0.hdslb.com/bfs/album/73fdec47d907dc42e96a2d0d21482680fd7efb3f.png "首屏加载画面")
![霜叶](https://i0.hdslb.com/bfs/album/98b4fd8a12bc6dbf691b967bed625db67713dff0.png  "明日方舟 - 霜叶")
![德克萨斯](https://i0.hdslb.com/bfs/album/79dbdaee161130460b77411f4664b4ecbd53d68e.png "明日方舟 - 德克萨斯")
![闪灵](https://i0.hdslb.com/bfs/album/56d918333fd302f9c221680008d7109fe090fb39.png "明日方舟 - 闪灵")
![凯留](https://i0.hdslb.com/bfs/album/8ad8f6ca661f68909b30edce518d47614162a78f.png "公主连结 - 凯留")
![湊阿库娅](https://i0.hdslb.com/bfs/album/51ee6aa61652191d4ab6c27a6e18bf8dc1997fdc.png "方舟指令 - 湊阿库娅")
![万象物语](https://i0.hdslb.com/bfs/album/0919e8d269e355c9b451d52e887c314a84f47faa.png "万象物语")

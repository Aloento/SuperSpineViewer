# SuperSpineViewer
一个拿来加载与导出Spine动画的工具  
至少需要Java15才能运行  
或使用打包好的EXE包  
当前版本：0.1.20  
![Java CI with Maven](https://github.com/Aloento/SuperSpineViewer/workflows/Java%20CI%20with%20Maven/badge.svg)

## 关于这个工具
它本质上是我学习Java和OpenJFX的练习作品  
所以我虽然尽可能保证它能正常使用  
但是不能保证它好用  
也不能保证拥有大多数功能  
本工具使用高度定制的Spine运行时  

### 已知问题
目前渲染实现会占用大量不必要的带宽  
在DriftFX稳定后会尝试迁移  
如果你遇到渲染性能问题  
请考虑使用LibGDX模式导出（构件中）  

## 功能
### 目前实现的
加载Spine的Skel和Json  
重载骨骼与贴图  
查看与播放动画  
查看与切换皮肤  
变换大小  
调整位置  
调整速度  
循环播放  
播放暂停  
导出透明MOV  
导出透明PNG  

### 支持的版本
Spine 2.1 ●  
Spine 3.1 ●  
Spine 3.2 √  
Spine 3.3 √  
Spine 3.4 √  
Spine 3.5 √  
Spine 3.6 √  
Spine 3.7 √  
Spine 3.8 √  
Spine 4.0 ●  

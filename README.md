# AudioPlayManagerProject
媒体语音播放器
#### 已满足的需求
1、回到桌面停止播放
</br>
2、每次贴近耳朵，黑屏，从0开始播放，拿开继续从当前的时间点播放
</br>
3、监听耳机插入拔出，同时切换到贴耳，外放
</br>
4、支持播放倍数
</br>

![图片](https://github.com/CMzhizhe/AudioPlayManagerProject/blob/5f4df2d9f61e351ecdc8dec62e5a057b8aaf526a/img/Screenshot_20221119_121836.png)


#### 教程
添加权限
```
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <!--播放语音需要的权限-->
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
```
添加依赖
```
implementation 'com.github.CMzhizhe:AudioPlayManagerProject:v1.0.5'
```

初始化
```
   AudioPlayManager.Builder().setApplication(this.application).build()
```

提供的方法
```
 /**
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description 打开assets文件夹下面的音乐
     * @param assetsName assets文件下的音乐文件
     * @param voiceId 用户自定义的语音ID
     * @param speed 播放倍数
     * @param playListener 播放结果回调
     */
    fun prepareAssetsAsync(assetsName: String,voiceId: String?,speed: Float = 1.0f,playListener:OnAudioPlayListener?) 

 /**
     * @param remoteUrl 服务器提供的地址
     * @param voiceId   用户自定义的ID
     * @param speed 播放倍数
     * @param playListener 播放结果监听
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     */
    fun prepareAsync(remoteUrl: String,voiceId: String?,speed: Float = 1.0f,playListener: OnAudioPlayListener?)

 /**
     * @param voiceId 用户自定义的ID
     * @param file    播放语音的文件
     * @param speed 播放倍数
     * @param playListener 播放结果监听
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     */
    fun prepareAsync(file: File,voiceId: String?,speed: Float = 1.0f,playListener: OnAudioPlayListener?)

    AudioPlayManager.getInstance().seekTo()//开始播放音乐 &&设置播放进度
    AudioPlayManager.getInstance().playSpeed//设置播放速度
    AudioPlayManager.getInstance().registerListenerProximity() //需要在activity onResume调用，目的注册感光监听
    AudioPlayManager.getInstance().unregisterListenerProximity()//需要在activity onPause调用，目的移除感光监听
    AudioPlayManager.getInstance().pause()//暂停音乐
    AudioPlayManager.getInstance().releaseAll()//停止播放，在onDestroy调用
```
 

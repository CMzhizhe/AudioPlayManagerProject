package com.gxx.audioplaylibrary

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.res.AssetFileDescriptor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.*
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.OnSeekCompleteListener
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import com.gxx.audioplaylibrary.broadcastreceiver.HeadsetChangeReceiver
import com.gxx.audioplaylibrary.inter.OnAudioPlayListener
import com.gxx.audioplaylibrary.model.AudioVoiceModel
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


/**
 * @author gaoxiaoxiong
 * @date 创建时间: 2022/11/10
 * @description
 * 每次贴近耳朵，都需要从0开始播放，如果从贴近耳朵离开，变成扩音，就重当前离开的时间点，继续播放
 */
class AudioPlayManager private constructor(application: Application) : SensorEventListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, HeadsetChangeReceiver.OnHeadsetChangeReceiverListener {
    private val TAG = "AudioPlayManager"
    private var mMediaPlayer: MediaPlayer? = null
    private var mWeakOnAudioPlayListener: WeakReference<OnAudioPlayListener>? = null
    private var mSensorManager: SensorManager?=null
    private var mAudioManager: AudioManager?=null
    private var mPowerManager: PowerManager?=null
    private var _wakeLock: PowerManager.WakeLock? = null
    private var mTimer: Timer? = null
    private var mApplication: Application
    private var mHandler: Handler? = null
    var audioVoiceModel: AudioVoiceModel? = null
        private set
    private val mHeadsetChangeReceiver = HeadsetChangeReceiver()

    companion object {
        private var mAudioPlayManager: AudioPlayManager? = null
        fun getInstance(): AudioPlayManager {
            return mAudioPlayManager!!
        }
    }

    init {
        mApplication = application
        initAudioPowerSensor()
        registerHeadsetPlugReceiver()//注册插入耳机监听
        if (mHandler == null) {
            mHandler = Handler(application.mainLooper)
        }
    }

    class Builder {
        private var mApplication: Application? = null
        fun setApplication(application: Application): Builder {
            this.mApplication = application
            return this
        }

        fun build(): AudioPlayManager {
            mAudioPlayManager = AudioPlayManager(mApplication!!)
            return mAudioPlayManager!!
        }
    }

    /**
     * @date 创建时间: 2023/4/28
     * @author gaoxiaoxiong
     * @description 初始化媒体，感应器
     */
    private fun initAudioPowerSensor(){
        if (mPowerManager == null){
            mPowerManager = mApplication.getSystemService(Context.POWER_SERVICE) as PowerManager
        }
        if (mAudioManager == null){
            mAudioManager = mApplication.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        if (mSensorManager == null){
            mSensorManager = mApplication.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
    }

    private val afChangeListener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {}
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    //暂时失去焦点，暂停播放音乐（将needRestart设置为true）
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "失去焦点")
                    }
                    if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null && audioVoiceModel != null) {
                        mWeakOnAudioPlayListener!!.get()!!
                            .onVoiceFocusLoss(audioVoiceModel!!.playIngVoiceId)
                    }
                    stop()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    //暂停播放音乐，不再继续播放
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "失去焦点")
                    }
                    if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null && audioVoiceModel != null) {
                        mWeakOnAudioPlayListener!!.get()!!
                            .onVoiceFocusLoss(audioVoiceModel!!.playIngVoiceId)
                    }
                    stop()
                }
            }
        }

    /**
     * @param voiceId 用户自定义的ID
     * @param file    播放语音的文件
     * @param speed 播放倍数
     * @param playListener 播放结果监听
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     */
    fun prepareAsync(
        file: File,
        voiceId: String?,
        speed: Float = 1.0f,
        playListener: OnAudioPlayListener?
    ) {
        val audioVoiceModel = AudioVoiceModel()
        audioVoiceModel.playIngFileUri = Uri.fromFile(file)
        audioVoiceModel.playIngVoiceId = voiceId
        audioVoiceModel.speed = speed
        this.prepareAsync(audioVoiceModel, playListener)
    }

    /**
     * @param remoteUrl 服务器提供的地址
     * @param voiceId   用户自定义的ID
     * @param speed 播放倍数
     * @param playListener 播放结果监听
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     */
    fun prepareAsync(
        remoteUrl: String,
        voiceId: String?,
        speed: Float = 1.0f,
        playListener: OnAudioPlayListener?
    ) {
        val audioVoiceModel = AudioVoiceModel()
        audioVoiceModel.playIngVoiceId = voiceId
        audioVoiceModel.playIngRemoteUrl = remoteUrl
        audioVoiceModel.speed = speed
        this.prepareAsync(audioVoiceModel, playListener)
    }

    /**
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description 打开assets文件夹下面的音乐
     * @param assetsName assets文件下的音乐文件
     * @param voiceId 用户自定义的语音ID
     * @param speed 播放倍数
     * @param playListener 播放结果回调
     */
    fun prepareAssetsAsync(
        assetsName: String,
        voiceId: String?,
        speed: Float = 1.0f,
        playListener: OnAudioPlayListener?
    ) {
        val audioVoiceModel = AudioVoiceModel()
        audioVoiceModel.playIngVoiceId = voiceId
        audioVoiceModel.playIngAssetsName = assetsName
        audioVoiceModel.speed = speed
        this.prepareAsync(audioVoiceModel, playListener)
    }

    /**
     * @param playListener 播放的监听
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 异步加载语音
     */
    fun prepareAsync(audioVoiceModel: AudioVoiceModel, playListener: OnAudioPlayListener?) {
        if (audioVoiceModel.playIngAssetsName.isNullOrEmpty() && audioVoiceModel.playIngFileUri == null && audioVoiceModel.playIngRemoteUrl.isNullOrEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "当前无法播放，所有的参数都是空类型");
            }
            return
        }
        initAudioPowerSensor()
        if (mAudioManager == null){
            if(BuildConfig.DEBUG){
              Log.d(TAG, "mAudioManager == null");
            }
            return
        }
        //设置亮屏 + 普通模式
        setScreenOn()
        mAudioManager!!.setSpeakerphoneOn(true)
        mAudioManager!!.setMode(AudioManager.MODE_NORMAL)
        resetMediaPlayer() //重置mediaplayer
        try {
            var musicGranted: Int = -1
            //申请获取焦点
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//android 8.0
                musicGranted = mAudioManager!!.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(afChangeListener).build()
                )
            } else {
                musicGranted = mAudioManager!!.requestAudioFocus(
                    afChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            if (musicGranted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null) {
                    mWeakOnAudioPlayListener!!.get()!!
                        .onVoiceFocusLoss(audioVoiceModel.playIngVoiceId)
                }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "暂未获取到音频焦点")
                }
                return
            }
            registerListenerProximity()
            this.audioVoiceModel = audioVoiceModel
            mWeakOnAudioPlayListener = WeakReference<OnAudioPlayListener>(playListener)
            mMediaPlayer = MediaPlayer()
            if (Build.VERSION.SDK_INT >= O_MR1) {
                mMediaPlayer!!.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
            } else {
                mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            mMediaPlayer!!.setLooping(false)
            mMediaPlayer!!.setOnPreparedListener(this) //设置加载完成后的监听
            mMediaPlayer!!.setOnSeekCompleteListener(this)
            mMediaPlayer!!.setOnCompletionListener(this)
            mMediaPlayer!!.setOnErrorListener(this)
            if (audioVoiceModel.playIngFileUri != null) {
                mMediaPlayer!!.setDataSource(mApplication, audioVoiceModel.playIngFileUri!!)
            } else if (audioVoiceModel.playIngRemoteUrl != null) {
                mMediaPlayer!!.setDataSource(audioVoiceModel.playIngRemoteUrl)
            } else if (audioVoiceModel.playIngAssetsName != null) {
                val assetFileDescriptor: AssetFileDescriptor =
                    mApplication.assets.openFd(audioVoiceModel.playIngAssetsName!!)
                mMediaPlayer!!.setDataSource(
                    assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength()
                );
            }
            mMediaPlayer!!.prepareAsync()
            startTime()
        } catch (var5: Exception) {
            var5.printStackTrace()
            releaseAll()
        }
    }

    @TargetApi(11)
    override fun onSensorChanged(event: SensorEvent) {
        if (mSensorManager == null){
            return
        }

        // 如果耳机已插入，设置距离传感器失效
        if (isHeadphonesPlugged) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "耳机已插入，设置距离传感器失效")
            }
            return
        }
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY && mMediaPlayer != null && audioVoiceModel != null) {
            if (mMediaPlayer!!.isPlaying()) {
                val range: Float = event.values.get(0) //测量出来的距离
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "range = " + range + "defaultRange = " + mSensorManager!!.getDefaultSensor(
                            Sensor.TYPE_PROXIMITY
                        )
                            .getMaximumRange()
                    )
                }
                if (range.toDouble() >= mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
                        .getMaximumRange()
                ) { // 远离手机
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "远离手机，range = $range")
                    }
                    //切换到外发
                    changeToSpeaker()
                } else { //贴近
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "贴近手机，range = $range")
                    }
                    changeToEarpiece()
                }
            }
        }
    }

    /**
     * 切换到外放
     */
    private fun changeToSpeaker() {
        if (mMediaPlayer == null || audioVoiceModel == null || mAudioManager == null) {
            return
        }
        if (mAudioManager!!.getMode() == AudioManager.MODE_NORMAL) { //普通模式，喇叭播放
            return
        }
        setScreenOn()
        //打开扬声器
        mAudioManager!!.setSpeakerphoneOn(true)
        mAudioManager!!.setMode(AudioManager.MODE_NORMAL)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "重置语音，切成扩音")
        }
        createEarpieceOrSpeakerMediaPlayer(false)
    }

    /**
     * 切换到听筒
     */
    private fun changeToEarpiece() {
        if (mMediaPlayer == null || audioVoiceModel == null || mAudioManager == null) {
            return
        }
        if (mAudioManager!!.getMode() == AudioManager.MODE_IN_COMMUNICATION) { //通话模式，包括音/视频、VoIP通话
            return
        }
        setScreenOff()
        //切换到听筒
        mAudioManager!!.setSpeakerphoneOn(false) //关闭扬声器
        //通话模式
        mAudioManager!!.setMode(AudioManager.MODE_IN_COMMUNICATION)
        //切成电话
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "重置语音，切成电话")
        }
        createEarpieceOrSpeakerMediaPlayer(true)
    }

    /**
     * @date 创建时间: 2023/3/8
     * @author gaoxiaoxiong
     * @description 设置媒体音量大小
     */
    fun setStreamVolume(volume: Int) {
        if (mAudioManager == null){
            return
        }
        if (mAudioManager!!.isSpeakerphoneOn) {//扬声器
            mAudioManager!!.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volume,
                AudioManager.FLAG_PLAY_SOUND
            )
        } else {//听筒
            mAudioManager!!.setStreamVolume(
                AudioManager.MODE_IN_COMMUNICATION,
                volume,
                AudioManager.FLAG_PLAY_SOUND
            )
        }
    }

    /**
     * @param isEarpiece 是否贴耳，true 是的
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description 创建属于贴耳或者是扩音的MediaPlayer
     */
    private fun createEarpieceOrSpeakerMediaPlayer(isEarpiece: Boolean) {
        if (mAudioManager == null){
            return
        }
        try {
            val position =
                if (mMediaPlayer!!.isPlaying()) mMediaPlayer!!.getCurrentPosition() else -1
            releaseTimer()
            resetMediaPlayer()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "已释放mediaPlayer，是否为贴耳=$isEarpiece")
            }
            var musicGranted: Int = -1
            //申请获取焦点
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//android 8.0
                musicGranted = mAudioManager!!.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(afChangeListener).build()
                )
            } else {
                musicGranted = mAudioManager!!.requestAudioFocus(
                    afChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            if (musicGranted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null) {
                    mWeakOnAudioPlayListener!!.get()!!
                        .onVoiceFocusLoss(audioVoiceModel!!.playIngVoiceId)
                }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "暂未获取到音频焦点")
                }
                stop()
                resetMediaPlayer()
                return
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "createEarpieceOrSpeakerMediaPlayer->获取到焦点")
            }
            mMediaPlayer = MediaPlayer()
            if (isEarpiece) {
                if (Build.VERSION.SDK_INT >= O_MR1) {
                    mMediaPlayer!!.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                } else {
                    mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_VOICE_CALL) //这样设置，比不设置的好处是，声音会大点
                }
            } else {
                if (Build.VERSION.SDK_INT >= O_MR1) {
                    mMediaPlayer!!.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                } else {
                    mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
            }
            mMediaPlayer!!.setOnCompletionListener(this)
            mMediaPlayer!!.setOnErrorListener(this)
            mMediaPlayer!!.setOnSeekCompleteListener(object : OnSeekCompleteListener {
                override fun onSeekComplete(mp: MediaPlayer) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "createEarpieceOrSpeakerMediaPlayer->设置播放位置完成，准备播放")
                    }
                    mp.start()
                    if (audioVoiceModel!!.speed != playSpeed) {
                        playSpeed = audioVoiceModel!!.speed
                    }
                }
            })
            mMediaPlayer!!.setOnPreparedListener(object : OnPreparedListener {
                override fun onPrepared(mp: MediaPlayer) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "createEarpieceOrSpeakerMediaPlayer->可以准备播放了")
                    }
                    if (isEarpiece) {
                        mp.start()
                        if (audioVoiceModel!!.speed != playSpeed) {
                            playSpeed = audioVoiceModel!!.speed
                        }
                    } else {
                        mp.seekTo(position)
                    }
                }
            })
            if (audioVoiceModel!!.playIngFileUri != null) {
                mMediaPlayer!!.setDataSource(mApplication, audioVoiceModel!!.playIngFileUri!!)
            } else if (audioVoiceModel!!.playIngRemoteUrl != null) {
                mMediaPlayer!!.setDataSource(audioVoiceModel!!.playIngRemoteUrl)
            } else if (audioVoiceModel!!.playIngAssetsName != null) {
                val assetFileDescriptor: AssetFileDescriptor =
                    mApplication.assets.openFd(audioVoiceModel!!.playIngAssetsName!!)
                mMediaPlayer!!.setDataSource(
                    assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength()
                );
            }
            mMediaPlayer!!.prepareAsync()
            startTime()
        } catch (e: IOException) {
            e.printStackTrace()
            unregisterListenerProximity()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            unregisterListenerProximity()
        } catch (e: SecurityException) {
            e.printStackTrace()
            unregisterListenerProximity()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            unregisterListenerProximity()
        }
    }

    /**
     * 耳机已插入
     *
     * @return
     */
    @get:SuppressLint("WrongConstant")
    private val isHeadphonesPlugged: Boolean
        private get() {
            if (mAudioManager == null){
                return false
            }else{
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val audioDevices: Array<AudioDeviceInfo> =
                        mAudioManager!!.getDevices(AudioManager.GET_DEVICES_ALL)
                    for (deviceInfo in audioDevices) {
                        if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                            || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        ) {
                            return true
                        }
                    }
                    false
                } else {
                    mAudioManager!!.isWiredHeadsetOn()
                }
            }
        }

    /**
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 设置滚动位置
     * 是一个异步方法,在Prepared, Paused 和 PlaybackCompleted状态中，都可以调用seekTo方法。
     */
    fun seekTo(position: Int) {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.seekTo(position)
        }
    }

    /**
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 释放定时器
     */
    private fun releaseTimer() {
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
        }
    }

    /**
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 开始计时
     */
    private fun startTime() {
        releaseTimer()
        if (mTimer == null) {
            mTimer = Timer()
        }
        mTimer!!.schedule(object : TimerTask() {
            override fun run() {
                if (mMediaPlayer == null || !mMediaPlayer!!.isPlaying() || audioVoiceModel == null) {
                    return
                }
                val currentPosition =
                    if (mMediaPlayer == null) 0 else mMediaPlayer!!.getCurrentPosition()
                mHandler?.post(Runnable { //回传给调用者，当前播放进度
                    if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null && mMediaPlayer != null && mMediaPlayer!!.isPlaying()) {
                        mWeakOnAudioPlayListener!!.get()!!.onVoicePlayPosition(
                            currentPosition,
                            mMediaPlayer!!.getDuration(),
                            audioVoiceModel!!.playIngVoiceId
                        )
                    }
                })
            }
        }, 0, 1000)
    }

    /**
     * @date 创建时间: 2022/11/11
     * @author gaoxiaoxiong
     * @description 关闭屏幕
     */
    @TargetApi(21)
    private fun setScreenOff() {
        if (mPowerManager == null){
            return
        }
        if (_wakeLock == null) {
            if (Build.VERSION.SDK_INT >= 21) {
                _wakeLock = mPowerManager!!.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                        "gxx:AudioPlayManager"
                    )
            }
        }
        _wakeLock?.acquire()
    }

    /**
     * @date 创建时间: 2022/11/11
     * @author gaoxiaoxiong
     * @description 屏幕长亮
     */
    private fun setScreenOn() {
        _wakeLock?.let {
            it.setReferenceCounted(false)
            it.release()
            _wakeLock = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAccuracyChanged:accuracy = $accuracy")
        }
    }

    /**
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 暂停，start状态与pause状态是异步切换
     */
    fun pause() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying()) {
            mMediaPlayer!!.pause()
        }
    }

    /**
     * @param position 希望恢复的时间点
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 是一个异步方法 从暂停状态---->恢复播放
     */
    fun resume(position: Int) {
        if (mMediaPlayer != null) {
            seekTo(position)
        }
    }


    /**
     * @date 创建时间: 2022/11/11
     * @author gaoxiaoxiong
     * @description 取消注册音频竞争
     */
    private fun abandonAudioFocus() {
        if (mAudioManager == null){
            return
        }
        mAudioManager!!.abandonAudioFocus(afChangeListener)
    }

    /**
     * @date 创建时间: 2022/11/13
     * @author gaoxiaoxiong
     * @description 获取用户自定义的 语音ID
     */
    val playIngVoiceId: String?
        get() = if (audioVoiceModel == null) {
            null
        } else audioVoiceModel!!.playIngVoiceId

    /**
     * @param speed 倍数
     * @date 创建时间: 2022/11/13
     * @author gaoxiaoxiong
     * @description 设置播放倍数
     * 处于 即处于Idle或End或Stopped 状态 不要调用该方法
     * 合法时间点 Initialized, Prepared, Started, Paused, PlaybackCompleted, Error
     */
    var playSpeed: Float
        get() {
            if (audioVoiceModel == null || mMediaPlayer == null) {
                return 1.0f
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params: PlaybackParams = mMediaPlayer!!.getPlaybackParams()
                    return params.speed
                } else {
                    return audioVoiceModel!!.speed
                }
            }
        }
        set(speed) {
            if (mMediaPlayer == null || audioVoiceModel == null || !isPlayIng) {
                return
            }
            audioVoiceModel!!.speed = speed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val params: PlaybackParams = mMediaPlayer!!.getPlaybackParams()
                    if (speed <= 0.0f) {
                        params.setSpeed(1.0f)
                    } else {
                        params.setSpeed(speed)
                    }
                    mMediaPlayer!!.setPlaybackParams(params)
                } catch (e: Exception) {
                    e.printStackTrace()
                    unregisterListenerProximity()
                }
            }
        }

    /**
     * @date 创建时间: 2022/11/11
     * @author gaoxiaoxiong
     * @description 完成播放
     */
    override fun onCompletion(mp: MediaPlayer) {
        stop()
        if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null && audioVoiceModel != null) {
            mWeakOnAudioPlayListener!!.get()!!.onVoiceComplete(audioVoiceModel!!.playIngVoiceId)
            mWeakOnAudioPlayListener!!.clear()
        }
    }

    /**
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 监听到使用了seekTo && 定位到具体位置了
     */
    override fun onSeekComplete(mp: MediaPlayer) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "开始播放")
        }
        mp.start()
        if (audioVoiceModel!!.speed != playSpeed) {
            playSpeed = audioVoiceModel!!.speed
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null && audioVoiceModel != null) {
            mWeakOnAudioPlayListener!!.get()!!.onVoicePrepared(audioVoiceModel!!.playIngVoiceId)
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "收到错误: what = $what,extra = $extra")
        }
        if (mWeakOnAudioPlayListener != null && mWeakOnAudioPlayListener!!.get() != null && audioVoiceModel != null) {
            mWeakOnAudioPlayListener!!.get()!!
                .onVoiceError(audioVoiceModel!!.playIngVoiceId, what, extra)
        }
        stop()
        resetMediaPlayer()
        return true
    }

    /**
     * @date 创建时间: 2022/11/11
     * @author gaoxiaoxiong
     * @description 停止播放，调用停止播放后，prepare() 或 prepareAsync()进入Prepared状态后，才能播放音频
     */
    private fun stop() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer!!.isPlaying()) {
                mMediaPlayer!!.stop()
            }
            unregisterListenerProximity()
            abandonAudioFocus() //取消音频竞争
            releaseTimer()
        }
    }

    /**
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description 注册传感器
     */
    fun registerListenerProximity() {
        if (mSensorManager == null){
            return
        }
        mSensorManager!!.registerListener(
            this,
            mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description 解除传感器
     */
    fun unregisterListenerProximity() {
        if (mSensorManager == null){
            return
        }
        mSensorManager!!.unregisterListener(
            this,
            mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        )
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "取消注册绑定监听")
        }
    }

    /**
     * @date 创建时间: 2022/11/13
     * @author gaoxiaoxiong
     * @description 是否正在播放音乐，true 正在播放
     */
    val isPlayIng: Boolean
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.isPlaying()
        } else false

    /**
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description 重置mediaplayer
     */
    private fun resetMediaPlayer() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer!!.reset()
                mMediaPlayer!!.release()
                mMediaPlayer = null
            } catch (var2: IllegalStateException) {
                var2.printStackTrace()
            }
        }
    }


    /**
     * @date 创建时间: 2022/11/19
     * @author gaoxiaoxiong
     * @description 注册耳机插入的广播接收
     */
    private fun registerHeadsetPlugReceiver() {
        mHeadsetChangeReceiver.setOnHeadsetChangeReceiverListener(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.HEADSET_PLUG")
        mApplication.registerReceiver(mHeadsetChangeReceiver, intentFilter)
    }

    /**
     * @date 创建时间: 2022/11/11
     * @author gaoxiaoxiong
     * @description 释放资源
     */
    fun releaseAll() {
        if (mWeakOnAudioPlayListener != null) {
            mWeakOnAudioPlayListener!!.clear()
        }
        stop()
        resetMediaPlayer()
        audioVoiceModel = null
    }

    /**
     * @date 创建时间: 2022/11/19
     * @author gaoxiaoxiong
     * @description
     * @param isEarpiece true 插入耳机
     */
    override fun onHeadsetChangeReceiver(isEarpiece: Boolean) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "接收到耳机的拔插事件->是否插入耳机->" + isEarpiece);
        }
        if (isEarpiece) {
            changeToEarpiece()
        } else {
            changeToSpeaker()
        }
    }


}
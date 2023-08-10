package com.gxx.audioplaymanagerproject

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gxx.audioplaylibrary.AudioPlayManager
import com.gxx.audioplaylibrary.inter.OnAudioPlayListener
import java.util.Locale

class MainActivity : AppCompatActivity(), View.OnClickListener, SeekBar.OnSeekBarChangeListener,
    OnAudioPlayListener, VolumeChangeObserver.VolumeChangeListener {
    private val TAG = "MainActivity"
    private lateinit var tvProgress: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvPlayStatus: TextView
    private lateinit var tvMusicName: TextView
    private lateinit var btPause: Button
    private lateinit var seekbar: SeekBar
    private lateinit var speed0_5: TextView
    private lateinit var speed1: TextView
    private lateinit var speed1_5: TextView
    private lateinit var speed2: TextView
    private val musicModelList = mutableListOf<MusicModel>()

    private var mVolumeChangeObserver: VolumeChangeObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AudioPlayManager.Builder().setApplication(this.application).build()

        seekbar = this.findViewById<SeekBar>(R.id.seek_bar_music)
        seekbar.setOnSeekBarChangeListener(this)
        btPause = this.findViewById<Button>(R.id.btn_pause)
        val tvFirstMusic = this.findViewById<TextView>(R.id.tv_first_music)
        val tvSecondMusic = this.findViewById<TextView>(R.id.tv_second_music)
        tvMusicName = this.findViewById<TextView>(R.id.tv_play_music_name)
        tvPlayStatus = this.findViewById<TextView>(R.id.tv_play_status)
        tvProgress = this.findViewById<TextView>(R.id.tv_progress)
        tvDuration = this.findViewById<TextView>(R.id.tv_duration);

        speed0_5 = this.findViewById<TextView>(R.id.tv_multiple_0_point_5)
        speed1 = this.findViewById<TextView>(R.id.tv_multiple_1)
        speed1_5 = this.findViewById<TextView>(R.id.tv_multiple_1_point5)
        speed2 = this.findViewById<TextView>(R.id.tv_multiple_2)

        speed0_5.setOnClickListener(this)
        speed1.setOnClickListener(this)
        speed1_5.setOnClickListener(this)
        speed2.setOnClickListener(this)

        btPause.setOnClickListener(this)
        tvFirstMusic.setOnClickListener(this)
        tvSecondMusic.setOnClickListener(this)

        findViewById<View>(R.id.telephone_receiver_play_auto).setOnClickListener(this)
        findViewById<View>(R.id.telephone_receiver_play).setOnClickListener(this)
        findViewById<View>(R.id.telephone_receiver_play_no).setOnClickListener(this)
        val musicModel0 = MusicModel()
        musicModel0.assetsName = "pianzi.mp3"
        musicModel0.musicName = "骗子"
        musicModel0.voiceId = "firstVoiceId"
        musicModel0.progress = 0

        val musicModel1 = MusicModel()
        musicModel1.assetsName = "shifei.mp3"
        musicModel1.musicName = "是非"
        musicModel1.voiceId = "secondVoiceId"
        musicModel1.progress = 0

        musicModelList.add(musicModel0)
        musicModelList.add(musicModel1)

        if (mVolumeChangeObserver == null) {
            mVolumeChangeObserver = VolumeChangeObserver(this)
        }
        mVolumeChangeObserver!!.volumeChangeListener = this

    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_pause -> {//暂停,如果你没有先准备播放，点击这个是会崩溃的哦
                if (AudioPlayManager.getInstance().isPlayIng) {
                    AudioPlayManager.getInstance().pause()
                    for (musicModel in musicModelList) {
                        if (musicModel.voiceId == AudioPlayManager.getInstance().playIngVoiceId) {
                            tvMusicName.setText("播放音乐的名称：" + musicModel.musicName)
                            tvPlayStatus.setText("播放状态：暂停")
                            btPause.setText("恢复")
                            break
                        }
                    }
                } else {
                    for (musicModel in musicModelList) {
                        if (musicModel.voiceId == AudioPlayManager.getInstance().playIngVoiceId) {
                            AudioPlayManager.getInstance().resume(musicModel.progress)
                            tvMusicName.setText("播放音乐的名称：" + musicModel.musicName)
                            tvPlayStatus.setText("播放状态：正在播放")
                            btPause.setText("暂停")
                            break
                        }
                    }
                }
            }

            R.id.telephone_receiver_play_auto -> {
                AudioPlayManager.getInstance().setTelephoneReceiverPlay(null)
            }

            R.id.telephone_receiver_play -> {
                AudioPlayManager.getInstance().setTelephoneReceiverPlay(true)
            }

            R.id.telephone_receiver_play_no -> {
                AudioPlayManager.getInstance().setTelephoneReceiverPlay(false)
            }

            R.id.tv_first_music -> {//第一个音乐
                AudioPlayManager.getInstance()
                    .prepareAssetsAsync(
                        musicModelList.get(0).assetsName,
                        musicModelList.get(0).voiceId,
                        1.0f,
                        true,
                        this
                    )
                speed0_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_purple)
                speed1_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed2.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
            }

            R.id.tv_second_music -> {//第二个音乐
                AudioPlayManager.getInstance()
                    .prepareAssetsAsync(
                        musicModelList.get(1).assetsName,
                        musicModelList.get(1).voiceId,
                        2.0f,
                        false,
                        this
                    )
                speed0_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed2.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_purple)
            }

            R.id.tv_multiple_0_point_5 -> {
                AudioPlayManager.getInstance().playSpeed = 0.5f
                speed0_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_purple)
                speed1.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed2.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
            }

            R.id.tv_multiple_1 -> {
                AudioPlayManager.getInstance().playSpeed = 1.0f
                speed0_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_purple)
                speed1_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed2.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
            }

            R.id.tv_multiple_1_point5 -> {
                AudioPlayManager.getInstance().playSpeed = 1.5f
                speed0_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_purple)
                speed2.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
            }

            R.id.tv_multiple_2 -> {
                AudioPlayManager.getInstance().playSpeed = 2.0f
                speed0_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed1_5.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_gray)
                speed2.setBackgroundResource(R.drawable.layerlist_rectangle_solid_white_border_1dp_purple)
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        onVoicePlayerSeekbar(seekBar.progress, "onStartTrackingTouch")
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onVoicePlayerSeekbar(seekBar.progress, "onStopTrackingTouch")
    }

    /**
     * @date 创建时间: 2022/11/18
     * @author gaoxiaoxiong
     * @description
     * @param progress 用户拖动seekbar进度
     * @param callBackName 执行的方法
     */
    private fun onVoicePlayerSeekbar(progress: Int, callBackName: String) {
        if (callBackName == "onStartTrackingTouch") {//用户开始拖动
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "用户开始拖动 语音进度条")
            }
            AudioPlayManager.getInstance().pause()
            for ((position, datum) in musicModelList.withIndex()) {
                if (datum.voiceId == AudioPlayManager.getInstance().playIngVoiceId) {
                    datum.progress = progress
                    break
                }
            }
        } else if (callBackName == "onStopTrackingTouch") {//用户停止拖动
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "用户放开语音进度条")
            }

            for ((position, datum) in musicModelList.withIndex()) {
                if (datum.voiceId == AudioPlayManager.getInstance().playIngVoiceId) {
                    datum.progress = progress
                    AudioPlayManager.getInstance().seekTo(progress)
                    break
                }
            }
        }
    }

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 准备播放
     */
    override fun onVoicePrepared(voiceId: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "正在播放的音乐，voiceId = " + voiceId)
        }

        if (voiceId.isNullOrEmpty()) {
            return
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPrepared 调用");
        }
        for (datum in musicModelList) {
            if (datum.voiceId == voiceId) {
                tvMusicName.setText("播放音乐的名称：" + datum.musicName)
                tvPlayStatus.setText("播放状态：正在播放")
                btPause.setText("暂停")
                AudioPlayManager.getInstance().seekTo(datum.progress)
                break
            }
        }
    }

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 播放完成
     */
    override fun onVoiceComplete(voiceId: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "播放完成的音乐，voiceId = " + voiceId)
        }
        for (datum in musicModelList) {
            if (datum.voiceId == voiceId) {
                tvMusicName.setText("播放音乐的名称：" + datum.musicName)
                tvPlayStatus.setText("播放状态：播放完成")
                datum.progress = 0
                break
            }
        }
    }

    /**
     * @param progress 已播放的进度，毫秒
     * @param duration 总时长，毫秒
     * @param voiceId  用自定义的ID
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 媒体已播放到的位置
     */
    override fun onVoicePlayPosition(progress: Int, duration: Int, voiceId: String?) {
        for (musicModel in musicModelList) {
            if (musicModel.voiceId == voiceId) {
                musicModel.progress = progress
                break
            }
        }
        seekbar.progress = progress
        seekbar.max = duration
        tvProgress.setText(formatRecordSeconds(Math.round(progress / 1000f)))
        tvDuration.setText(formatRecordSeconds(duration / 1000))
    }

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 播放错误
     */
    override fun onVoiceError(voiceId: String?, what: Int, extra: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "播放错误的音乐，voiceId = " + voiceId + ",what = " + what + ",extra = " + extra
            )
        }
        for (datum in musicModelList) {
            if (datum.voiceId == voiceId) {
                tvMusicName.setText("播放音乐的名称：" + datum.musicName)
                tvPlayStatus.setText("播放状态：播放错误")
                datum.progress = 0
                break
            }
        }
    }

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 语音失去焦点
     */
    override fun onVoiceFocusLoss(voiceId: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "语音失去焦点，voiceId = " + voiceId)
        }
        for (datum in musicModelList) {
            if (datum.voiceId == voiceId) {
                datum.progress = 0
                tvMusicName.setText("播放音乐的名称：" + datum.musicName)
                tvPlayStatus.setText("播放状态：语音失去焦点，需要点击对应的音乐重新播放")
                break
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AudioPlayManager.getInstance().registerListenerProximity()
        btPause.setText("恢复")
        mVolumeChangeObserver?.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        AudioPlayManager.getInstance().pause()
        AudioPlayManager.getInstance().unregisterListenerProximity()
        mVolumeChangeObserver?.unregisterReceiver()
    }

    /**
     * @param seconds 秒数
     * @return 如果时间能用小时+分钟，就返回 00:00 否则返回00:00:00
     */
    private fun formatRecordSeconds(seconds: Int): String {
        val standardTime: String
        standardTime = if (seconds <= 0) {
            "00:00"
        } else if (seconds < 60) {
            String.format(Locale.getDefault(), "00:%02d", seconds % 60)
        } else if (seconds < 3600) {
            String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
        } else {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                seconds / 3600,
                seconds % 3600 / 60,
                seconds % 60
            )
        }
        return standardTime
    }


    override fun onDestroy() {
        super.onDestroy()
        AudioPlayManager.getInstance().releaseAll()
    }


    /**
     * @date 创建时间: 2023/3/8
     * @author gaoxiaoxiong
     * @description 音量改变回调
     */

    override fun onVolumeChanged(volume: Int) {
        AudioPlayManager.getInstance().setStreamVolume(volume)
    }
}
package com.gxx.audioplaylibrary.inter

interface OnAudioPlayListener {
    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 准备播放
     */
    fun onVoicePrepared(voiceId: String?)

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 播放完成
     */
    fun onVoiceComplete(voiceId: String?)

    /**
     * @param progress 已播放的进度，毫秒
     * @param duration 总时长，毫秒
     * @param voiceId  用自定义的ID
     * @date 创建时间: 2022/11/10
     * @author gaoxiaoxiong
     * @description 媒体已播放到的位置
     */
    fun onVoicePlayPosition(progress: Int, duration: Int, voiceId: String?)

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 播放错误
     */
    fun onVoiceError(voiceId: String?, what: Int, extra: Int)

    /**
     * @param voiceId 用自定义的ID
     * @date 创建时间: 2022/11/12
     * @author gaoxiaoxiong
     * @description 语音失去焦点
     */
    fun onVoiceFocusLoss(voiceId: String?)
}
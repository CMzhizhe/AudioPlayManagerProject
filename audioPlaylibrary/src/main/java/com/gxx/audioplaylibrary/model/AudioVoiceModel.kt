package com.gxx.audioplaylibrary.model


import android.net.Uri

class AudioVoiceModel {
    var playIngFileUri: Uri? = null //本地文件Uri
    var playIngRemoteUrl: String? = null //播放远端的地址信息
    var playIngAssetsName: String? = null//打开assets文件夹里面的音乐
    var playIngVoiceId: String? = null //用户自定义的voiceID
    var speed = 1.0f //播放倍数
    var isTelephoneReceiverPlay: Boolean? = null    // 是否听筒播放， null:自动，true:听筒模式，false：外放模式
}
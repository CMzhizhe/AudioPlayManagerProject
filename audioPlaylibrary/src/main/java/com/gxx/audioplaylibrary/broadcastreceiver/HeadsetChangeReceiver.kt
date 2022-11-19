package com.gxx.audioplaylibrary.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
  * @date 创建时间: 2022/11/19
  * @author gaoxiaoxiong
  * @description 耳机插入拔出
 */
class HeadsetChangeReceiver: BroadcastReceiver() {
    private var mOnHeadsetChangeReceiverListener:OnHeadsetChangeReceiverListener? = null;
    interface OnHeadsetChangeReceiverListener{
        /**
          * @date 创建时间: 2022/11/19
          * @author gaoxiaoxiong
          * @description
         * @param isEarpiece true 插入耳机
         */
        fun onHeadsetChangeReceiver(isEarpiece:Boolean)
    }

    public fun setOnHeadsetChangeReceiverListener(listener: OnHeadsetChangeReceiverListener){
        this.mOnHeadsetChangeReceiverListener = listener
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null){
            return
        }
        val action = intent.action
        when(action){
            Intent.ACTION_HEADSET_PLUG->{
                val state = intent.getIntExtra("state", 0)
                if (state == 1){ // 连接耳机
                    mOnHeadsetChangeReceiverListener?.onHeadsetChangeReceiver(true)
                } else if (state == 0){// 断开耳机
                    mOnHeadsetChangeReceiverListener?.onHeadsetChangeReceiver(false)
                }
            }
        }
    }
}
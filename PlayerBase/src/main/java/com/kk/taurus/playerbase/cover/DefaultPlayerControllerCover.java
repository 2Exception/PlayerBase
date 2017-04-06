package com.kk.taurus.playerbase.cover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import com.kk.taurus.playerbase.R;
import com.kk.taurus.playerbase.callback.OnCoverEventListener;
import com.kk.taurus.playerbase.callback.OnPlayerEventListener;
import com.kk.taurus.playerbase.cover.base.BaseCoverObserver;
import com.kk.taurus.playerbase.cover.base.BasePlayerControllerCover;
import com.kk.taurus.playerbase.inter.ISinglePlayer;
import com.kk.taurus.playerbase.inter.MSG;
import com.kk.taurus.playerbase.setting.BaseAdVideo;
import com.kk.taurus.playerbase.setting.VideoData;

import java.util.List;

/**
 * Created by Taurus on 2017/3/24.
 */

public class DefaultPlayerControllerCover extends BasePlayerControllerCover {

    private final String TAG = "player_controller";
    private static final long MSG_HIDDEN_CONTROLLER_DELAY_TIME = 5000;
    private boolean isLandScape;
    private BatteryReceiver batteryReceiver;

    public DefaultPlayerControllerCover(Context context, BaseCoverObserver coverObserver) {
        super(context, coverObserver);
    }

    @Override
    public View initCoverLayout(Context context) {
        return View.inflate(context, R.layout.layout_player_controller_cover,null);
    }

    @Override
    protected void findView() {
        super.findView();
        mIvPlayState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(player==null)
                    return;
                if(player.getStatus()==ISinglePlayer.STATUS_PLAYING){
                    player.pause();
                    setPlayState(false);
                }else{
                    player.resume();
                    setPlayState(true);
                }
            }
        });
        mIvBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mOnBackClickListener!=null){
                    mOnBackClickListener.onClick(v);
                }
            }
        });
        initBatteryReceiver(mContext);
    }

    @Override
    protected void _handleMessage(Message msg) {
        super._handleMessage(msg);
        switch (msg.what){
            case MSG.MSG_CODE_DELAY_HIDDEN_CONTROLLER:
                setControllerState(false);
                break;
            case MSG.MSG_CODE_SEEK_TO:
                if(player==null)
                    return;
                int progress = (int) msg.obj;
                player.seekTo(progress);
                break;
        }
    }

    @Override
    public void setControllerState(boolean state) {
        super.setControllerState(state);
        notifyCoverEvent(state? OnCoverEventListener.EVENT_CODE_ON_PLAYER_CONTROLLER_SHOW
                :OnCoverEventListener.EVENT_CODE_ON_PLAYER_CONTROLLER_HIDDEN,null);
    }

    @Override
    protected void switchControllerState() {
        if(isVisibilityGone()){
            if(isLandScape){
                setTopContainerState(true);
            }else{
                setTopContainerState(false);
            }
            setControllerState(true);
            setBottomContainerState(true);
        }else{
            setControllerState(false);
        }
    }

    protected void onControllerShow() {
        super.onControllerShow();
        sendDelayHiddenControllerMsg();
    }

    protected void sendDelayHiddenControllerMsg(){
        removeDelayHiddenControllerMsg();
        mHandler.sendEmptyMessageDelayed(MSG.MSG_CODE_DELAY_HIDDEN_CONTROLLER,MSG_HIDDEN_CONTROLLER_DELAY_TIME);
    }

    protected void removeDelayHiddenControllerMsg(){
        mHandler.removeMessages(MSG.MSG_CODE_DELAY_HIDDEN_CONTROLLER);
    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser){
                Log.d(TAG,"onProgressChanged...");
                setPlayTime(progress,seekBar.getMax());
            }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d(TAG,"onStartTrackingTouch...");
            removePlayMsg();
            removeDelayHiddenControllerMsg();
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(player==null)
                return;
            if(seekBar.getMax()<=0 || player.getDuration() <= 0)
                return;
            Log.d(TAG,"onStopTrackingTouch...");
            int progress = seekBar.getProgress();
            sendSeekToMsg(progress);
            sendDelayHiddenControllerMsg();
        }
    };

    private void sendSeekToMsg(int progress) {
        mHandler.removeMessages(MSG.MSG_CODE_SEEK_TO);
        Message message = Message.obtain();
        message.what = MSG.MSG_CODE_SEEK_TO;
        message.obj = progress;
        if(player.getStatus() == ISinglePlayer.STATUS_PAUSE){
            mHandler.sendMessage(message);
        }else{
            mHandler.sendMessageDelayed(message,600);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeDelayHiddenControllerMsg();
        unRegisterBatteryReceiver();
    }

    @Override
    public void onNotifyConfigurationChanged(Configuration newConfig) {
        super.onNotifyConfigurationChanged(newConfig);
        isLandScape = newConfig.orientation== Configuration.ORIENTATION_LANDSCAPE;
        if(!isLandScape){
            setTopContainerState(false);
        }
    }

    @Override
    public void onNotifyPlayEvent(int eventCode, Bundle bundle) {
        super.onNotifyPlayEvent(eventCode, bundle);
        switch (eventCode){
            case OnPlayerEventListener.EVENT_CODE_PREPARED:
                if(mSeekBar!=null){
                    mSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
                }
                break;
            case OnPlayerEventListener.EVENT_CODE_RENDER_START:
                setPlayState(true);
                break;
        }
    }

    @Override
    public void onNotifyAdPrepared(List<BaseAdVideo> adVideos) {
        super.onNotifyAdPrepared(adVideos);
        setCoverEnable(false);
    }

    @Override
    public void onNotifyAdFinish(VideoData data, boolean isAllFinish) {
        super.onNotifyAdFinish(data, isAllFinish);
        if(isAllFinish){
            setCoverEnable(true);
        }
    }

    @Override
    public void onCoverEvent(int eventCode, Bundle bundle) {
        super.onCoverEvent(eventCode, bundle);
        switch (eventCode){
            case OnCoverEventListener.EVENT_CODE_ON_PLAYER_ERROR_SHOW:
                setCoverEnable(false);
                break;
            case OnCoverEventListener.EVENT_CODE_ON_PLAYER_ERROR_HIDDEN:
                if(adListFinish){
                    setCoverEnable(true);
                }
                break;
        }
    }

    @Override
    public void onGestureSingleTab(MotionEvent event) {
        switchControllerState();
    }

    private void initBatteryReceiver(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BatteryReceiver();
        if(context!=null){
            context.registerReceiver(batteryReceiver,filter);
        }
    }

    protected void unRegisterBatteryReceiver(){
        if(batteryReceiver !=null && mContext!=null){
            try {
                mContext.unregisterReceiver(batteryReceiver);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int current = intent.getExtras().getInt("level");// 获得当前电量
            int total = intent.getExtras().getInt("scale");// 获得总电量
            int percent = current * 100 / total;
            updateBatteryState(percent);
        }
    }

}
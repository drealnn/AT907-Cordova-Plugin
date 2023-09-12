package com.at907.app;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.util.Log;
import android.view.KeyEvent;

import android.view.View;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicBoolean;


public class General extends CordovaPlugin {


    public CallbackContext keyup_callback = null;
    public CallbackContext keydown_callback = null;
    private SoundPool mSound;
    int mSuccess, mFail, mBeep;

    private View currentView = null;
    private String TAG = "AT907 Native";
    private Context ctx = null;

    private Activity cordovaActivity;

//Context context=this.cordova.getActivity().getApplicationContext();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);

        // Events //

        if(action.equalsIgnoreCase("playSound"))
        {
            this.playSound(args.getString(0), callbackContext);
            return true;
        } else if(action.equalsIgnoreCase("register_keyDown")){
            this.keydown_callback = callbackContext;
            return true;
        }
        else if(action.equalsIgnoreCase("register_keyUp")){
            this.keyup_callback = callbackContext;
            return true;
        }
        return false;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordovaActivity = cordova.getActivity();
        this.ctx = cordovaActivity.getApplicationContext();
        Log.i(TAG, "AT907 general Initialized");
        //final CordovaWebView myWebView = webView;
        /*cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                myWebView.getView().setOnKeyListener(
                        new View.OnKeyListener(){
                            @Override
                            public boolean onKey(View view, int keyCode, KeyEvent event){
                                //boolean val = super.onKey(view, keyCode, event);
                                Log.e(TAG, ""+keyCode);
                                return doKey(view, keyCode, event);
                            }
                        }
                );
            }
        });*/

        this.currentView = webView.getView();
        registerKeyCodeReceiver();

    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        unregisterKeyCodeReceiver();
    }

    @Override
    public void onResume(boolean multitasking){
        super.onResume(multitasking);
        registerKeyCodeReceiver();
        //initScanner();
    }

    private final KeyCodeReceiver keyCodeReceiver = new KeyCodeReceiver(this);

    private class KeyCodeReceiver extends BroadcastReceiver {
        General pluginCtx;
        AtomicBoolean keyIsUp = new AtomicBoolean(true);
        KeyCodeReceiver(General pluginCtx) {
            this.pluginCtx = pluginCtx;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int keyCode = intent.getIntExtra("keyCode", 0);
            if (keyCode == 0) {
                keyCode = intent.getIntExtra("keycode", 0);
            }
            boolean isKeyDown = intent.getBooleanExtra("keydown", false);
            if (isKeyDown) {
                if (this.pluginCtx.keydown_callback != null) {
                    if (keyIsUp.getAndSet(false)) {
                        try {
                            String str = String.format("{\'keyCode\': \'%s\', \'repeatCount\' : \'%s\' }", keyCode + "", 0 + "");
                            PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
                            result.setKeepCallback(true);
                            this.pluginCtx.keydown_callback.sendPluginResult(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling key event");
                            result.setKeepCallback(true);
                            this.pluginCtx.keydown_callback.sendPluginResult(result);
                        }
                    }
                }
            } else {
                if (this.pluginCtx.keyup_callback != null) {
                    try {

                        String str = String.format("{\'keyCode\': \'%s\', \'repeatCount\' : \'%s\' }", keyCode + "", 0 + "");
                        PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
                        result.setKeepCallback(true);
                        this.pluginCtx.keyup_callback.sendPluginResult(result);

                    } catch(Exception e)
                    {
                        e.printStackTrace();
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling key event");
                        result.setKeepCallback(true);
                        this.pluginCtx.keyup_callback.sendPluginResult(result);

                    } finally {
                        keyIsUp.set(true);
                    }
                }
            }
        }
    }

    /**
     * Register the BroadcastReceiver for receive KeyEvent
     */
    private void registerKeyCodeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.rfid.FUN_KEY");
        filter.addAction("android.intent.action.FUN_KEY");
        this.cordovaActivity.registerReceiver(keyCodeReceiver, filter);
    }

    /**
     * Unregister the BroadcastReceiver for receive KeyEvent
     */
    private void unregisterKeyCodeReceiver() {
        this.cordovaActivity.unregisterReceiver(keyCodeReceiver);
    }


    private void playSound(String soundName, CallbackContext callbackContext)
    {
        switch(soundName){
            case("success"):
                SoundLoader.getInstance(ctx).playSuccess();
                break;
            case("fail"):
                SoundLoader.getInstance(ctx).playFail();
                break;
            case("beep"):
                SoundLoader.getInstance(ctx).playBeep();
                break;
            default:
                callbackContext.error("Can't find provided sound for playSound");
                return;
                

        }
        callbackContext.success("Initiating sound");
    }
}
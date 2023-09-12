package com.at907.app;

import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Activity;


import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;


public class Barcode extends CordovaPlugin {

private static final String TAG = "MainActivity";

private PowerManager.WakeLock mWakeLock = null;
private SoundPool mSoundPool;
private int mBeepSuccess;
private int mBeepFail;
private Vibrator mVibrator;
private CallbackContext keyup_callback = null;
private CallbackContext keydown_callback = null;
private CallbackContext getDecode_callback = null;
private View currentView = null;

private Activity cordovaActivity;
private Context ctx;
private ScanUtil scanUtil;

private BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        PluginResult result;
        byte[] data = intent.getByteArrayExtra("data");
        if (data != null) {
//                String barcode = Tools.Bytes2HexString(data, data.length);
            String barcode = new String(data);
            result = new PluginResult(PluginResult.Status.OK, barcode);
            SoundLoader.getInstance(context).playSuccess();
        } else {
            result = new PluginResult(PluginResult.Status.ERROR, "Scan Fail");
        }
        result.setKeepCallback(true);
        getDecode_callback.sendPluginResult(result);
    }
};

//ScanResult mScanResult;

//Context context=this.cordova.getActivity().getApplicationContext();

@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    result.setKeepCallback(true);

    if (action.equals("echo")) {
        String message = args.getString(0);
        this.echo(message, callbackContext);
        return true;
    }
    else if (action.equals("deinitalize_scanner")){
        this.deinitalize();
        return true;
    }
    else if (action.equals("wakeup_scanner")){
        Log.i(TAG, "+- wakeup scanner");
        if (scanUtil == null) {
            scanUtil = new ScanUtil(this.ctx);
            //we must set mode to 0 : BroadcastReceiver mode
            scanUtil.setScanMode(0);
        }
        //if(mScanner != null)
        //    ATScanManager.wakeUp();
        return true;
    }
    else if (action.equals("sleep_scanner")){
        Log.i(TAG, "+- sleep scanner");
        if (scanUtil != null) {
            scanUtil.setScanMode(1);
            scanUtil.close();
            scanUtil = null;
        }
        /*if(mScanner != null) {
            ATScanManager.sleep();
        }*/
        return true;
    }
    else if (action.equals("scanner_startDecode")){
        Log.i(TAG, "++Start Decode");
        //mScanResult = null;
        if (scanUtil != null) {
            scanUtil.scan();
        }
        Log.i(TAG, "--Start Decode");
        
        return true;
    }
    else if (action.equals("scanner_stopDecode")){
        Log.i(TAG, "++Stop Decode");
        if (scanUtil != null) {
            scanUtil.stopScan();
        }
        Log.i(TAG, "--Stop Decode");
        
        return true;
    }
    else if (action.equals("scanner_isDecoding")){
        Log.i(TAG, "++Is Decoding");
        //callbackContext.success("" + this.mScanner.isDecoding());
        Log.i(TAG, "--Is Decoding");
        
        return true;
    }
    // TODO: replace with  android -> javascript async call instead of javascript->android sync call
    /*else if (action.equals("scanner_getDecodeCallback")){
        Log.i(TAG, "Start Decode");
        if (mScanResult != null)
        {
            // TODO: return json instead of string
            callbackContext.success("" + mScanResult.scanResultType + 
                " : " + mScanResult.scanResultBarcode);
            mScanResult = null;
        }
        else
            callbackContext.error("Could not get decode information");
        
        return true;
    }*/
    else if(action.equalsIgnoreCase("register_keyDown")){
            this.keydown_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("register_keyUp")){
            this.keyup_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("register_decode")){
            this.getDecode_callback = callbackContext;
            return true;
    }
    return false;
}

//public Barcode2DWithSoft.ScanCallback  ScanBack = new Barcode2DWithSoft.ScanCallback(){
//    @Override
//    public void onScanComplete(int i, int length, byte[] bytes) {
//        PluginResult result;
//        if (length < 1) {
//            String errorMessage = "";
//            if (length == -1) {
//                errorMessage = "Scan Cancel";
//            } else if (length == 0) {
//                errorMessage = "Scan timeout";
//            } else {
//                errorMessage = "Scan fail";
//            }
//            Log.e(TAG, errorMessage);
//            result = new PluginResult(PluginResult.Status.ERROR, errorMessage);
//        } else {
//            String barcode = new String(bytes, 0, length, StandardCharsets.US_ASCII);
//            //result = new PluginResult(PluginResult.Status.OK, new JSONObject("{\'type\': \'N/A\' , \'barcode\': \'" + barcode + "\' }"));
//			result = new PluginResult(PluginResult.Status.OK, barcode);
//            SoundLoader.getInstance(ctx).playSuccess();
//        }
//        result.setKeepCallback(true);
//        getDecode_callback.sendPluginResult(result);
//    }
//};

@Override
public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.cordovaActivity = cordova.getActivity();
    this.ctx = cordovaActivity.getApplicationContext();
	SoundLoader.getInstance(ctx);
    scanUtil = new ScanUtil(ctx);
    scanUtil.setScanMode(0);
    IntentFilter filter = new IntentFilter();
    filter.addAction("com.rfid.SCAN");
    cordovaActivity.registerReceiver(receiver, filter);

//    boolean result = false;
//    if(mScanner != null) {
//        result = mScanner.open(ctx);
//        Log.i(TAG,"open="+result);
//        if(result){
////                mScanner.setParameter(324, 1);
////                mScanner.setParameter(300, 0); // Snapshot Aiming
////                mScanner.setParameter(361, 0); // Image Capture Illumination
//
//            // interleaved 2 of 5
//            mScanner.setParameter(6, 1);
//            mScanner.setParameter(22, 0);
//            mScanner.setParameter(23, 55);
//            mScanner.setParameter(402, 1);
//
//            mScanner.setScanCallback(ScanBack);
//        } else {
//            Log.e(TAG, "Barcode initialization failure");
//        }
//    }
    this.currentView = webView.getView();
    Log.i(TAG, "Scanning device initialized");
}

//public class InitScannerTask extends AsyncTask<String, Integer, Boolean> {
//    @Override
//    protected Boolean doInBackground(String... params) {
//        boolean result = false;
//        if(mScanner != null) {
//            result = mScanner.open(ctx);
//            Log.i(TAG,"open="+result);
//        }
//        return result;
//    }
//
//    @Override
//    protected void onPostExecute(Boolean result) {
//        super.onPostExecute(result);
//        if(result){
////                mScanner.setParameter(324, 1);
////                mScanner.setParameter(300, 0); // Snapshot Aiming
////                mScanner.setParameter(361, 0); // Image Capture Illumination
//
//            // interleaved 2 of 5
//            mScanner.setParameter(6, 1);
//            mScanner.setParameter(22, 0);
//            mScanner.setParameter(23, 55);
//            mScanner.setParameter(402, 1);
//        } else {
//            Log.e(TAG, "Barcode initialization failure");
//        }
//    }
//
//    @Override
//    protected void onPreExecute() {
//        // TODO Auto-generated method stub
//        super.onPreExecute();
//    }
//
//}

private void echo(String message, CallbackContext callbackContext) {
    if (message != null && message.length() > 0) {
        callbackContext.success(message);
    } else {
        callbackContext.error("Expected one non-empty string argument.");
    }
}

@Override
public void onDestroy(){
    super.onDestroy();
    deinitalize();
}

@Override
public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    if (scanUtil != null) {
        scanUtil.setScanMode(1);
        scanUtil.close();
        scanUtil = null;
    }
}

@Override
public void onResume(boolean multitasking){
    super.onResume(multitasking);
    if (scanUtil == null) {
        scanUtil = new ScanUtil(this.ctx);
        //we must set mode to 0 : BroadcastReceiver mode
        scanUtil.setScanMode(0);
    }
    //initScanner();
}


private void deinitalize(){
    Log.i(TAG, "+++ onDeinitalize");
    cordovaActivity.unregisterReceiver(receiver);
    scanUtil.setScanMode(1);
    scanUtil.close();
    scanUtil = null;
    Log.i(TAG, "--- onDeinitalize");
}

public class ScanUtil {

    /**
     * Open scan service
     */
    private final String ACTION_SCAN_INIT = "com.rfid.SCAN_INIT";
    /**
     * Scanning
     */
    private final String ACTION_SCAN = "com.rfid.SCAN_CMD";
    /**
     * Stop Scanning
     */
    private static final String ACTION_STOP_SCAN = "com.rfid.STOP_SCAN";
    /**
     * Close scan service
     */
    private final String ACTION_CLOSE_SCAN = "com.rfid.CLOSE_SCAN";
    /**
     * Scan result output mode, 0 -- BroadcastReceiver mode; 1 -- Focus input mode (default)
     */
    private final String ACTION_SET_SCAN_MODE = "com.rfid.SET_SCAN_MODE";
    /**
     * Scan timeout (Value:1000,2000,3000,4000,5000,6000,7000,8000,9000,10000)
     */
    private final String ACTION_SCAN_TIME = "com.rfid.SCAN_TIME";

    private Context context;

    /**
     * Initialize ScanUtil and open scan service
     * @param context Context
     */
    ScanUtil(Context context) {
        this.context = context;
        Intent intent = new Intent();
        intent.setAction(ACTION_SCAN_INIT);
        context.sendBroadcast(intent);
    }

    /**
     * Start Scanning
     */
    public void scan() {
        Intent intent = new Intent();
        intent.setAction(ACTION_SCAN);
        context.sendBroadcast(intent);
    }

    /**
     * Stop Scanning
     */
    public void stopScan() {
        Intent intent = new Intent();
        intent.setAction(ACTION_STOP_SCAN);
        context.sendBroadcast(intent);
    }

    /**
     * Set the scan result output mode
     * @param mode 0 -- BroadcastReceiver mode; 1 -- Focus input mode (default)
     */
    public void setScanMode(int mode) {
        Intent intent = new Intent();
        intent.setAction(ACTION_SET_SCAN_MODE);
        intent.putExtra("mode", mode);
        context.sendBroadcast(intent);
    }

    /**
     * Close scan service
     */
    public void close() {
        Intent toKillService = new Intent();
//        toKillService.putExtra("iscamera", true);
        toKillService.setAction(ACTION_CLOSE_SCAN);
        context.sendBroadcast(toKillService);
    }

    /**
     * Set scan timeout
     * @param timeout Value:1000,2000,3000,4000,5000(default),6000,7000,8000,9000,10000
     */
    public void setTimeout(String timeout){
        Intent intent = new Intent();
        intent.setAction(ACTION_SCAN_TIME);
        intent.putExtra("time", timeout);
        context.sendBroadcast(intent);
    }
}

/*@Override
public void onDecodeEvent(BarcodeType type, String barcode) {

    Log.i(TAG, "onDecodeEvent(" + type + ", [" + barcode
            + "])");

    mScanResult = new ScanResult();
    mScanResult.scanResultType = type;
    mScanResult.scanResultBarcode = barcode;


    
    if(type != BarcodeType.NoRead){
        //int position = this.adapterBarcode.addItem(type, barcode);
        //this.lstBarcodeList.setSelection(position);
        String str = "{\'type\': \'" + type + "\' , \'barcode\': \'" + barcode + "\' }";
        PluginResult result;
        try {
            result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
        } catch(JSONException e){
            e.printStackTrace();
            result = new PluginResult(PluginResult.Status.ERROR, "Error in constructing json for success decode callback");
        }
        result.setKeepCallback(true);
        this.getDecode_callback.sendPluginResult(result);
        //this.getDecode_callback = null;
        return;
         //result.setKeepCallback(false);
        //beep(true);
    }else{
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Barcode not found");
        result.setKeepCallback(true);
        this.getDecode_callback.sendPluginResult(result);
        //this.getDecode_callback = null;
         //result.setKeepCallback(false);
        //beep(false);
        return;
    }

    
}

@Override
public void onStateChanged(EventType state) {
    
    Log.i(TAG, "EventType : " + state.toString());
}

/*
// Beep & Vibrate
private void beep(boolean isSuccess) {
    Log.i(TAG, "@@@@ DEBUG. Play beep....!!!!");
    try{
        if(isSuccess){
            this.mSoundPool.play(mBeepSuccess, 1, 1, 0, 0, 1);
            this.mVibrator.vibrate(100);
        }else{
            this.mSoundPool.play(mBeepFail, 1, 1, 0, 0, 1);
        }
    }catch(Exception ex){
    }
}
*/

/*private class ScanResult {
   public CallbackContext scanResultCallback; 
   public BarcodeType scanResultType;
   public String scanResultBarcode;



}
*/
}


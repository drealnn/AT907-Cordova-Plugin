package com.at907.app;

import com.atid.lib.dev.ATRfidManager;
import com.atid.lib.dev.ATRfidReader;
import com.atid.lib.dev.event.RfidReaderEventListener;
import com.atid.lib.dev.rfid.type.ActionState;
import com.atid.lib.dev.rfid.type.ConnectionState;
import com.atid.lib.dev.rfid.type.ResultCode;
import com.atid.lib.dev.rfid.param.RangeValue;
import com.atid.lib.dev.rfid.type.TagType;
import com.atid.lib.dev.rfid.exception.ATRfidReaderException;
import com.atid.lib.dev.rfid.type.BankType;
import com.atid.lib.diagnostics.ATLog;


import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.content.Context;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.*;

public class Rfid extends CordovaPlugin {

private static final String TAG = "RFID Native"; 

protected ATRfidReader mReader;
private PowerManager.WakeLock mWakeLock = null;
private SoundPool mSoundPool;
private int mBeepSuccess;
private int mBeepFail;
private Vibrator mVibrator;
private Context ctx = null;

private CallbackContext keyup_callback = null;
private CallbackContext keydown_callback = null;
private CallbackContext onReaderReadTag_callback = null;
private CallbackContext onReaderResult_callback = null;
private CallbackContext onReaderStateChanged_callback = null;
private CallbackContext onReaderActionChanged_callback = null;
private View currentView = null;

private boolean loopFlag = false;
private Lock inventoryLock = new ReentrantLock();
private int inventoryFlag = 1;
Handler handler;
private int minPower = 5;
private int maxPower = 30;

private RangeValue mPowerRange;

private int mPowerLevel;
private int mOperationTime;

private TagType mTagType;

private boolean playSounds = true;

private RfidEventListenerImpl rfidEventListener = new RfidEventListenerImpl();

//Context context=this.cordova.getActivity().getApplicationContext();

@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    result.setKeepCallback(true);
    
    // lifecycle functions //
    if (action.equals("initialize")){
		try {
            mReader = ATRfidManager.getInstance();
            if (mReader != null) {
                ATRfidManager.wakeUp();
                if (mReader.getState() == ConnectionState.Connected){
                    callbackContext.success("successfully initialized RFID device");
					return true;
				} else {
					throw new Exception("Unable to initialize RFID");
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, "Failure to initialize RFID device. Aborting...");
            callbackContext.error("Failure to initialize RFID device");
			//toastMessage(ex.getMessage());
			return true;
		}
    }
    else if (action.equals("deinitalize")){
        this.deinitalize();
        return true;
    }
    else if (action.equals("wakeup")){
        Log.i(TAG, "+- wakeup scanner");
        ATRfidManager.wakeUp();
        /*if(mReader != null)
            ATRfidManager.wakeUp();*/

        callbackContext.success("Called wakeUp function");
        return true;
    }
    else if (action.equals("sleep")){
        Log.i(TAG, "+- sleep scanner");
        ATRfidManager.sleep();
        callbackContext.success("Called sleep function");
        return true;
    }
    else if (action.equals("forceSleep")){
        Log.i(TAG, "+- force sleep scanner");
//        if(mReader != null) {
//            mReader.free();
//            //mReader.disconnect();
//        }
//
//        callbackContext.success("Called sleep function");
        return true;
    }
    else if (action.equals("forceWake")){
        Log.i(TAG, "+- force wake scanner");
//        if(mReader != null) {
//            if(!mReader.init()) {
//                Log.e(TAG, "ERROR. wakeUp() - Failed to connect rfid reader");
//                callbackContext.error("ERROR. wakeUp() - Failed to connect rfid reader");
//            } else {
//                Log.i(TAG, "INFO. wakeUp()");
//                callbackContext.success("Called wakeUp function");
//            }
//        }

        
        return true;
    }
    else if (action.equals("pause_scanner")){
        Log.i(TAG, "+- pause scanner");
        if (mReader != null) {
            mReader.removeEventListener(rfidEventListener);
        }
//        if (mReader != null){
//            stopInventory(callbackContext);
//        }
        //mReader.removeEventListener(this);
        return true;
    }
    else if (action.equals("resume_scanner")){
        Log.i(TAG, "+- resume scanner");
        if (mReader != null) {
            mReader.setEventListener(rfidEventListener);
        }
        //if (mReader != null)
            //mReader.setEventListener(this);
        return true;
    }

    // getters and setters //
    
    else if (action.equals("getPowerRange")){
        Log.i(TAG, "++Get Power Range");
        try {
            if (mPowerRange == null){
                mPowerRange = mReader.getPowerRange();
            }
            callbackContext.success(new JSONObject("{\'min\': \'" + mPowerRange.getMin() + "\' , \'max\' : \'" + mPowerRange.getMax() + "\' }"));
        } catch (ATRfidReaderException e) {
            ATLog.e(TAG, e, "ERROR. initReader() - Failed to get power range");
            callbackContext.error("Failed to get power range");
        }
        Log.i(TAG, "--Get Power Range");

        return true;
    }
    else if (action.equals("getPower")){
        Log.i(TAG, "++Get Power");
        try {
            int power = mReader.getPower();
            if (power > -1){
                callbackContext.success("" + power);
            } else {
                callbackContext.error("failed to get power");
            }
        } catch (ATRfidReaderException e) {
            ATLog.e(TAG, e, "ERROR. initReader() - Failed to get power level");
            callbackContext.error("failed to get power");
        }
        Log.i(TAG, "--Get Power");
        return true;
    }
    else if (action.equals("setPower")){
        Log.i(TAG, "++set power level");
        try {
            mReader.setPower(args.getInt(0));
            callbackContext.success("successfully set power level to "+args.getInt(0));
        } catch (ATRfidReaderException e) {
            ATLog.e(TAG, e,
                    "ERROR. saveOption() - Failed to set power level [%s]",
                    args.getInt(0));
            callbackContext.error("failed to set power level");
        }
        Log.i(TAG, "--set power level");
        return true;
    }
    // Reading and Writing //

    else if (action.equals("start_readContinuous"))
    {
		Log.i(TAG, "++start readContinuous");
        startInventory(callbackContext, args.getJSONObject(0));
		Log.i(TAG, "--start readContinuous");
        return true;
    }
    else if (action.equals("start_readSingle"))
    {
        Log.i(TAG, "++start readSingle");
        startInventory(callbackContext, null, true);
        Log.i(TAG, "--start readSingle");
        return true;
    }
    else if (action.equals("start_readMemory"))
    {
        //startAction(TagType.Tag6C, args.getJSONObject(0), true, callbackContext);
        return true;
    }
    else if (action.equals("start_writeMemory"))
    {
        if (mReader.getAction() == ActionState.Stop && mReader.getState() == ConnectionState.Connected) {
            try {
                JSONObject params = args.getJSONObject(0);
                // EPC Bank
                BankType bank = BankType.EPC;
                // point 2 bytes ahead to skip header
                int offset = params.isNull("offset") ? 2 : params.getInt("offset");
                // write 6 words (2 bytes per word, 96 bits)
                int length = params.isNull("length") ? 6 : params.getInt("length");
                // blank password
                String password = params.isNull("password") ? "" : params.getString("password");
                // data to write
                String data = params.isNull("data") ? "" : params.getString("data");

                if ((mReader.writeMemory6c(bank, offset, data, password, null)) == ResultCode.NoError) {
                    callbackContext.success("successfully wrote to memory");
                } else {
                    Log.e(TAG, "ERROR. startAction() - Failed to write memory to tag");
                    callbackContext.error("ERROR. startAction() - Failed to write memory to tag");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                callbackContext.error("Failed to write to memory, invalid JSON error.");
            }
        } else {
            callbackContext.error("Failed to write to memory: Device is already in use or not connected.");
        }
        //startAction(TagType.Tag6C, args.getJSONObject(0), false, callbackContext);
        return true;
    }
    else if (action.equals("stop_read"))
    {
       /* final CallbackContext myCallbackContext = callbackContext;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                stopAction(myCallbackContext);
            }
        });*/
		Log.i(TAG, "++Stop Read");
        stopInventory(callbackContext);
		Log.i(TAG, "--Stop Read");
        return true;
    }
    else if (action.equals("is_running"))
    {
        callbackContext.success(!(mReader.getAction() == ActionState.Stop)+"");
        return true;
    }



    // Events //
    else if(action.equalsIgnoreCase("onReaderReadTag")){
            this.onReaderReadTag_callback = callbackContext;
            return true;
    }
    else if(action.equalsIgnoreCase("disableSounds")){
        playSounds = !args.getBoolean(0);
        return true;
    }
    return false;
}

@Override
public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
	try {
	    this.ctx = cordova.getActivity().getApplicationContext();
		SoundLoader.getInstance(ctx);
        handler = new ReadTagHandler();
		mReader = ATRfidManager.getInstance();
		if (mReader != null) {
            ATRfidManager.wakeUp();
			if (mReader.getState() == ConnectionState.Connected){
                mTagType = TagType.Tag6C;
                // Get Power Range
                try {
                    mPowerRange = mReader.getPowerRange();
                } catch (ATRfidReaderException e) {
                    ATLog.e(TAG, e, "ERROR. initReader() - Failed to get power range");
                }
                ATLog.i(TAG, "INFO. initReader() - [Power Range : %d, %d]", mPowerRange.getMin(), mPowerRange.getMax());

                // Get Power Level
                try {
                    mPowerLevel = mReader.getPower();
                } catch (ATRfidReaderException e) {
                    ATLog.e(TAG, e, "ERROR. initReader() - Failed to get power level");
                }
                ATLog.i(TAG, "INFO. initReader() - [Power Level : %d]", mPowerLevel);

                // Get Operation Time
                try {
                    mOperationTime = mReader.getOperationTime();
                } catch (ATRfidReaderException e) {
                    ATLog.e(TAG, e, "ERROR. initReader() - Failed to get operation time");
                }
                ATLog.i(TAG, "INFO. initReader() - [Operation Time : %d]", mOperationTime);
				//callbackContext.success("successfully initialized RFID device");
				//mReader.setEventListener(this);
				this.currentView = webView.getView();
				Log.i(TAG, "RFID device initialized");
			} else {
				throw new Exception("Unable to initialize RFID");
			}
		}
	} catch (Exception ex) {
		Log.e(TAG, "Failure to initialize RFID device. Aborting...");
		//callbackContext.error("Failure to initialize RFID device");
		//toastMessage(ex.getMessage());
		return;
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
    if (mReader != null) {
        mReader.removeEventListener(rfidEventListener);
        ATRfidManager.sleep();
    }
    //stopInventory();
}

@Override
public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    if (mReader != null) {
        mReader.setEventListener(rfidEventListener);
        ATRfidManager.wakeUp();
    }
}


private void deinitalize(){
    Log.i(TAG, "+++ onDeinitalize");
    stopInventory();
    ATRfidManager.onDestroy();
    Log.i(TAG, "--- onDeinitalize");
}

private boolean stopInventory(CallbackContext callbackContext){
    if (stopInventory()){
        callbackContext.success("Success");
        return true;
    } else {
        callbackContext.error("failure to stop inventory");
        return false;
    }
}

private boolean stopInventory() {
	boolean res = true;
	inventoryLock.lock();
	try {
		if (loopFlag) {
            if(mReader.getState() != ConnectionState.Connected || mReader.getAction() == ActionState.Stop) {
                ATLog.e(TAG, "ActionState is idle.");
                return false;
            }
			loopFlag = false;
			if (mReader != null && (mReader.stop() == ResultCode.NoError)) {
				res = true;
			} else {
				Log.e(TAG, "Failure to stop inventory.");
				res = false;
			}
		}
	} finally {
		inventoryLock.unlock();
	}
    return res;
}

private void startInventory(CallbackContext callbackContext){
    startInventory(callbackContext, null, false);
}

private void startInventory(CallbackContext callbackContext, JSONObject options){
    startInventory(callbackContext, options, false);
}

private void startInventory(CallbackContext callbackContext, JSONObject options, boolean readSingle){
    Map<String, Object> optionsMap =  new HashMap<>();
    inventoryLock.lock();
	try {
        if (options != null) {
            optionsMap.put("returnOnStop", options.optString("returnOnStop"));
            optionsMap.put("returnDistinct", options.optString("returnDistinct"));
            List<String> EPCFilter = new ArrayList<>();
            JSONArray arr = options.optJSONArray("epcFilter");
            if (arr != null) {
                if (arr.length() > 0) {
                    for (int i = 0; i < arr.length(); i++) {
                        EPCFilter.add(arr.getString(i));
                    }
                    optionsMap.put("epcFilter", EPCFilter);
                }
            }
        }
    } catch (JSONException e){
        Log.e(TAG, "startInventory: Invalid params");
        optionsMap = new HashMap<>();
    }
	try {
        rfidEventListener.setOptions(optionsMap);
        boolean res = false;
		if (!loopFlag){
            if(mReader.getAction() != ActionState.Stop) {
                ATLog.e(TAG, "ActionState is not idle.");
                callbackContext.error("Failure to start inventory");
                return;
            }
            if (readSingle){
                res = mReader.readEpc6cTag() == ResultCode.NoError;
            } else {
                res = mReader.inventory6cTag() == ResultCode.NoError;
            }
            if (res) {
                loopFlag = true;
                //cordova.getThreadPool().execute(new TagThread(optionsMap));
                callbackContext.success("successfully reading continuously");
            } else {
                ATLog.e(TAG, "ERROR. startAction() - Failed to start inventory 6C tag [%s]",
                        res);
                callbackContext.error("Failure to start inventory");
                return;
            }
		} else {
			callbackContext.error("Failure to start inventory");
		}
	} finally {
		inventoryLock.unlock();
	}
}

class ReadTagHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
        String msgResult = msg.obj + "";
        String[] strs = msgResult.split("@");
        String tag = strs[0];
        String rssi = strs.length > 1 ? strs[1] : "0";
        if (onReaderReadTag_callback == null) {
            return;
        }

        try {
            String str = "{\'tag\':\'" + tag + "\' , \'rssi\': \'" + rssi + "\'}";
            PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
            result.setKeepCallback(true);
			Log.e(TAG, "Sending tag json: "+str);
            onReaderReadTag_callback.sendPluginResult(result);
            if (playSounds){
                SoundLoader.getInstance(ctx).playBeep();
            }
        } catch(Exception e)
        {
            e.printStackTrace();
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderReadTag event");
            result.setKeepCallback(true);
            onReaderReadTag_callback.sendPluginResult(result);
        }
    }
}

class RfidEventListenerImpl implements RfidReaderEventListener {
    Map<String, Object> options = new HashMap<>();
    Set<String> epcFilterSet = new HashSet<>();
    Map<String, String> epcRssiMap = new HashMap<>();
    public RfidEventListenerImpl(){
    }

    public RfidEventListenerImpl(Map<String, Object> options){
        this.setOptions(options);
    }

    public void setOptions(Map<String, Object> options){
        this.options = options;
        if (this.options.get("epcFilter") != null){
            List<String> epcTags = (List<String>) this.options.get("epcFilter");
            if (epcTags != null) {
                epcFilterSet.addAll(epcTags);
            }
        }
    }

    private boolean isEnabled(String setting){
        return this.options.get(setting) != null && this.options.get(setting).equals("true");
    }

    private void sendTag(String epc, String rssi){
        if (!epcFilterSet.isEmpty() && !epcFilterSet.contains(epc)){
            return;
        }
        Message msg = handler.obtainMessage();
        msg.obj = epc + "@" + rssi;
        handler.sendMessage(msg);
    }

    @Override
    public void onReaderStateChanged(ATRfidReader reader, ConnectionState state) {
        ATLog.i(TAG, "EVENT. onReaderStateChanged(%s)", state);
    }

    @Override
    public void onReaderActionChanged(ATRfidReader reader, ActionState action) {
        ATLog.i(TAG, "EVENT. onReaderActionchanged(%s)", action);
    }

    @Override
    public void onReaderReadTag(ATRfidReader reader, String epc, float rssi, float phase) {
        ATLog.i(TAG, "EVENT. onReaderReadTag([%s], %.2f, %.2f)", epc, rssi, phase);
        String rssiString = Float.toString(rssi);
        epc = epc.substring(4);
        if (this.isEnabled("returnDistinct")){
            if (this.epcRssiMap.get(epc) == null){
                if (!this.isEnabled("returnOnStop")) {
                    sendTag(epc, rssiString);
                }
            }
        } else {
            if (!this.isEnabled("returnOnStop")) {
                sendTag(epc, rssiString);
            }
        }
        this.epcRssiMap.put(epc, rssiString);
    }

    @Override
    public void onReaderResult(ATRfidReader reader, ResultCode code, ActionState action, String epc, String data,
                               float rssi, float phase) {
        ATLog.i(TAG, "EVENT. onReaderResult(%s, %s, [%s], [%s], %.2f, %.2f", code, action, epc, data, rssi, phase);
    }

//    public void run() {
//        String[] res;
//        while (loopFlag) {
//            res = mReader.readTagFromBuffer();
//            if (res != null) {
//                String epc = mReader.convertUiiToEPC(res[1]);
//                String rssi = res[2];
//                if (this.isEnabled("returnDistinct")){
//                    if (this.epcRssiMap.get(epc) == null){
//                        if (!this.isEnabled("returnOnStop")) {
//                            sendTag(epc, rssi);
//                        }
//                    }
//                } else {
//                    if (!this.isEnabled("returnOnStop")) {
//                        sendTag(epc, rssi);
//                    }
//                }
//                this.epcRssiMap.put(epc, rssi);
//                Log.i("data","EPC:"+res[1]);
//            }
//        } // end while
//		Log.e(TAG, "Thread loop finished");
//        if (this.isEnabled("returnOnStop")){
//			Log.e(TAG, "Return on stop");
//            for (Map.Entry<String, String> entry : this.epcRssiMap.entrySet()) {
//                sendTag(entry.getKey(), entry.getValue());
//            }
//        }
//
//    }
}

/*protected void startAction(TagType tagType, boolean isContinuous, CallbackContext callbackContext) {

    ResultCode res = null;

    if (isContinuous) {
        // Multi Reading
        switch (tagType) {
        case Tag6C:
            if ((res = mReader.inventory6cTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start inventory 6C tag [%s]",
                                res));

                 callbackContext.error(String.format(
                                "ERROR. startAction() - Failed to start inventory 6C tag [%s]",
                                res));
                return;
            }
            break;
        case Tag6B:
            if ((res = mReader.inventory6bTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start inventory 6B tag [%s]",
                                res));
               
                return;
            }
            break;
        }
    } else {
        // Single Reading
        switch (tagType) {
        case Tag6C:
            if ((res = mReader.readEpc6cTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start read 6C tag [%s]",
                                res));
                callbackContext.error(String.format(
                                "ERROR. startAction() - Failed to start read 6C tag [%s]",
                                res));
                return;
            }
            break;
        case Tag6B:
            if ((res = mReader.readEpc6bTag()) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to start read 6B tag [%s]",
                                res));
                
                return;
            }
            break;
        }
    }
    callbackContext.success(isContinuous ? "successfully reading continuously: " + res : "successfully read single: " + res);
    Log.i(TAG, "INFO. startAction()");
}

protected void startAction(TagType tagType, JSONObject params, boolean isRead, CallbackContext callbackContext) {
    ResultCode res;
    BankType bank;
    int offset;
    int length;
    String data;
    String password;

    // TODO: set up logic for multiple banktype selection
    try {
        bank = params.isNull("bankType") ? BankType.EPC : params.getString("bankType").equalsIgnoreCase("EPC") ? BankType.EPC : null;
        offset = params.isNull("offset") ? 2 :  params.getInt("offset");
        length = params.isNull("length") ? 2 : params.getInt("length");
        password =  params.isNull("password") ? "" : params.getString("password");
        data =  params.isNull("data") ? "" : params.getString("data");
        
    } catch(JSONException e)
    {
        e.printStackTrace();
        callbackContext.error("Failed to read/write to memory, invalid JSON error.");
        return;
    }

    switch (tagType) {
        case Tag6C:
            //bank = getBank();
            if (isRead){
                if ((res = mReader.readMemory6c(bank, offset, length, password)) != ResultCode.NoError) {
                    Log.e(TAG,
                            String.format(
                                    "ERROR. startAction() - Failed to read memory 6C tag [%s]",
                                    res));
                    
                    callbackContext.error(String.format(
                                    "ERROR. startAction() - Failed to read memory 6C tag [%s]",
                                    res));
                    return;
                }

                callbackContext.success("successfully read memory : " + res);
            }
            else {
                if ((res = mReader.writeMemory6c(bank, offset, data, password)) != ResultCode.NoError) {
                    Log.e(TAG,
                            String.format(
                                    "ERROR. startAction() - Failed to write memory 6C tag [%s]",
                                    res));
                    
                    callbackContext.error(String.format(
                                    "ERROR. startAction() - Failed to write memory 6C tag [%s]",
                                    res));
                    return;
                }
                callbackContext.success("successfully wrote to memory : " + res);
            }
            break;
        case Tag6B:
            if ((res = mReader.readMemory6b(offset, length)) != ResultCode.NoError) {
                Log.e(TAG,
                        String.format(
                                "ERROR. startAction() - Failed to read memory 6B tag [%s]",
                                res));
                
                return;
            }
            break;
        }
        
        Log.i(TAG, "INFO. startAction()");
}

protected void stopAction(CallbackContext callbackContext) {

    ResultCode res;

    if ((res = mReader.stop()) != ResultCode.NoError) {
        Log.e(TAG, String.format(
                "ERROR. stopAction() - Failed to stop operation [%s]", res));
        callbackContext.error(String.format(
                "ERROR. stopAction() - Failed to stop operation [%s]", res));
        return;
    }

    callbackContext.success("successfully stopped reading operation");

    Log.i(TAG, "INFO. stopAction()");
}


/*
    ####### onReader event functions 
*/

//@Override
/*public void onReaderActionChanged(ATRfidReader reader, ActionState action) {
    Log.i(TAG, String.format("EVENT. onReaderActionchanged(%s)", action));
    if (action == ActionState.Stop) {
      //  adpTags.shutDown();
    } else {
        //adpTags.start();
    }

    if (this.onReaderActionChanged_callback == null)
        return;
    
    try {
        String str = action + "";
        PluginResult result = new PluginResult(PluginResult.Status.OK, str);
        result.setKeepCallback(true);
        this.onReaderActionChanged_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderActionChanged event");
        result.setKeepCallback(true);
        this.onReaderActionChanged_callback.sendPluginResult(result);
        return;
    }

    //enableWidgets(true);
}

@Override
public void onReaderReadTag(ATRfidReader reader, String tag, float rssi, float phase) {

    //adpTags.addItem(tag, rssi);
    //txtCount.setText(String.format("%d", adpTags.getCount()));
    //playSuccess();

    Log.i(TAG,
            String.format("EVENT. onReaderReadTag([%s], %.2f)", tag, rssi));
    if (this.onReaderReadTag_callback == null)
        return;
    
    try {
        String str = "{\'tag\':\'" + tag + "\' , \'rssi\': \'" + rssi + "\' , \'phase\': \'" + phase + "\'}";
        PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
        result.setKeepCallback(true);
        this.onReaderReadTag_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderReadTag event");
        result.setKeepCallback(true);
        this.onReaderReadTag_callback.sendPluginResult(result);
        return;
    }
}

@Override
public void onReaderResult(ATRfidReader reader, ResultCode code,
        ActionState action, String epc, String data, float rssi, float phase) {
    Log.i(TAG, String.format("EVENT. onReaderResult(%s, %s, [%s], [%s]",
            code, action, epc, data));

    if (this.onReaderResult_callback == null)
        return;
    
    try {
        String str = String.format("{\'code\': \'%s\', \'action\': \'%s\', \'epc\':\'%s\', \'data\':\'%s\'}", code, action, epc, data);
        PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject(str));
        result.setKeepCallback(true);
        this.onReaderResult_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderResult event");
        result.setKeepCallback(true);
        this.onReaderResult_callback.sendPluginResult(result);
        return;
    }
}

@Override
public void onReaderStateChanged(ATRfidReader reader, ConnectionState state) {
    Log.i(TAG, String.format("EVENT. onReaderStateChanged(%s)", state));
    if (this.onReaderStateChanged_callback == null)
        return;
    
    try {
        String str = state + "";
        PluginResult result = new PluginResult(PluginResult.Status.OK, str);
        result.setKeepCallback(true);
        this.onReaderStateChanged_callback.sendPluginResult(result);
        return;
    } catch(Exception e)
    {
        e.printStackTrace();
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error in handling onReaderStateChanged event");
        result.setKeepCallback(true);
        this.onReaderStateChanged_callback.sendPluginResult(result);
        return;
    }
}*/


}


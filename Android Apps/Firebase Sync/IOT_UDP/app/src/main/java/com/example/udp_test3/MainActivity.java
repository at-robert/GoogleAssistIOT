package com.example.udp_test3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



public class MainActivity extends AppCompatActivity{

    private final String TAG = "[Joe]";
    //UI
    Button buttonSoftAP,buttonBLEScan,buttonFirestore,buttonSpeedUp,buttonSpeedDown;
    TextView textViewState, textViewRx,textViewFanState;

    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;
    DatabaseHandler databaseHandler;
    Database mfirestore;

    final static String STR_DB_COLLECTION_NAME = "device-configs";
    private final int PORT = 1234;
    private static String device_id = "";
    String localAddress = "",serverMAC = "",serverAddress = "";

    // Firebase variables
    private int fan_button_count = 0;
    private Map<String, Object> fanSpeedTable = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //UI init
        setContentView(R.layout.activity_main);

        textViewState = (TextView)findViewById(R.id.state);
        textViewRx = (TextView)findViewById(R.id.received);
        textViewFanState = (TextView)findViewById(R.id.fan_state);

        buttonSoftAP = (Button) findViewById(R.id.btn_softap);
        buttonBLEScan = (Button) findViewById(R.id.btn_blescan);
        buttonFirestore = (Button) findViewById(R.id.btn_firestore);
        buttonSpeedUp = (Button) findViewById(R.id.btn_speedup);
        buttonSpeedDown = (Button) findViewById(R.id.btn_speeddown);


        buttonSoftAP.setOnClickListener(buttonSoftAPConnectOnClickListener);
        buttonBLEScan.setOnClickListener(buttonBLEConnectOnClickListener);
        buttonSpeedUp.setOnClickListener(buttonSetFanSpeedUpOnClickListener);
        buttonSpeedDown.setOnClickListener(buttonSetFanSpeedDownOnClickListener);
        buttonFirestore.setOnClickListener(buttonFirestoreOnClickListener);

        //UDP handler init
        udpClientHandler = new UdpClientHandler(this);
        //Firestore handler init
        databaseHandler = new DatabaseHandler(this);
    }

    public static String intToIpAddress(long ipInt) {
        StringBuffer sb = new StringBuffer();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
    //Button Click : SoftAP -> Get Device Info & Set Device Wifi
    View.OnClickListener buttonSoftAPConnectOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    //Get Wifi Information
                    on_button_get_device_IP();
                    if(serverAddress == null){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("提醒")
                                .setMessage("請確保先和產品透過Wi-Fi連線!!!")
                                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                        return;
                    }
                    on_button_connect();
                    buttonSoftAP.setEnabled(false);
                }
            };
    //Button Click : BLE Scan
    View.OnClickListener buttonBLEConnectOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                }
            };
    //Button Click : Set Fan Speed Up
    View.OnClickListener buttonSetFanSpeedUpOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    on_button_speed_up();
                }
            };

    //Button Click : Set Fan Speed Down
    View.OnClickListener buttonSetFanSpeedDownOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    on_button_speed_down();
                }
            };
    //Button Click : Set Firestore
    View.OnClickListener buttonFirestoreOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    //init_fire_store_snapshot_listener();
                    on_button_SetupDatabase();
                }
            };
    //Get device SoftAP ip
    public void on_button_get_device_IP() {
        WifiManager mWifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(mWifi.isWifiEnabled()) {
            WifiInfo wifi = mWifi.getConnectionInfo();
            localAddress = intToIpAddress(wifi.getIpAddress());//Local IP
            serverMAC =  wifi.getBSSID();//Hotspot MAC

            DhcpInfo dhcpinfo = mWifi.getDhcpInfo();
            serverAddress = intToIpAddress(dhcpinfo.serverAddress);//Hotspot IP
        }
    }
    //Click Event : Send PRC command
    public void on_button_connect() {
        textViewRx.setText("");
        udpClientThread = new UdpClientThread(
                serverAddress,
                PORT,
                udpClientHandler);
        udpClientThread.start();
    }
    //Click Event : Init Firestore on
    public void on_button_SetupDatabase(){
        mfirestore = new Database(STR_DB_COLLECTION_NAME,GetDeviceId(),databaseHandler);
        mfirestore.initial_firestore();
    }
    //Click Event : Set Fan Speed
    public void on_button_speed_up(){
        if(!fanSpeedTable.isEmpty()){
            if(Boolean.parseBoolean(fanSpeedTable.get("OnOff").toString()) == true){
                Log.d("[Joe5]","OnOff:true");
                if(fanSpeedTable.get("FanSpeed") == "Low") {
                    fanSpeedTable.put("FanSpeed","Medium");
                    Log.d("[Joe5]","FanSpeed:Low->Medium");
                }
                else if(fanSpeedTable.get("FanSpeed") == "Medium") {
                    fanSpeedTable.put("FanSpeed","High");
                    Log.d("[Joe5]","FanSpeed:Medium->High");
                }
            }
            else{
                Log.d("[Joe5]","OnOff:false");
                fanSpeedTable.put("OnOff",true);
                fanSpeedTable.put("FanSpeed","Low");
            }
        }
        else{
            Log.d("[Joe5]","init: OnOff:false,FanSpeed:Low");
            fanSpeedTable.put("OnOff",false);
            fanSpeedTable.put("FanSpeed","Low");
        }
        mfirestore.update_firestore(fanSpeedTable);
    }
    public void on_button_speed_down(){
        if(!fanSpeedTable.isEmpty()){
            if(Boolean.parseBoolean(fanSpeedTable.get("OnOff").toString()) == true){
                if(fanSpeedTable.get("FanSpeed") == "High") fanSpeedTable.put("FanSpeed","Medium");
                else if(fanSpeedTable.get("FanSpeed") == "Medium") fanSpeedTable.put("FanSpeed","Low");
                else if(fanSpeedTable.get("FanSpeed") == "Low") fanSpeedTable.put("OnOff",false);
            }
        }
        else{
            fanSpeedTable.put("OnOff",false);
            fanSpeedTable.put("FanSpeed","Low");
        }
        mfirestore.update_firestore(fanSpeedTable);
    }

    // Get/Set Device ID
    public void SetDeviceId(String id) {
        device_id = id;
    }
    public String GetDeviceId() {
        return device_id;
    }
    //UDP Message Handler======================================
    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
    }

    private void clientEnd(){
        udpClientThread = null;
        textViewState.setText("clientEnd()");

        buttonSoftAP.setEnabled(true);

    }

    private void GetSysInfo(String res) {
        try {
            JSONObject response = new JSONObject(res);
            JSONObject  result = response.getJSONObject("result");
            if(result.getString("id") != null){
                SetDeviceId(result.getString("id"));
                //Log.d("[Joe3]",GetDeviceId());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static class UdpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        public static final int UPDATE_SYSINFO = 3;
        private MainActivity parent;

        public UdpClientHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    parent.updateState((String)msg.obj);
                    break;
                case UPDATE_MSG:
                    parent.updateRxMsg((String)msg.obj);
                    break;
                case UPDATE_END:
                    parent.clientEnd();
                    break;
                case UPDATE_SYSINFO:
                    parent.GetSysInfo((String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }
    //Firestore Message Handler==========================
    public void database_update_state(Map<String,Object> state){
        GsonBuilder gsonMapBuilder = new GsonBuilder();
        Gson gsonObject = gsonMapBuilder.create();
        String result = gsonObject.toJson(state);;
        textViewFanState.setText(result);
        fanSpeedTable = state;
    }

    public static class DatabaseHandler extends Handler {
        public static final int UPDATE_STATE = 0;

        private MainActivity parent;

        public DatabaseHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    parent.database_update_state((Map<String,Object>)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }
}

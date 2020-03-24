package com.example.udp_test3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Charsets;
import com.google.api.services.cloudiot.v1.CloudIot;
import com.google.api.services.cloudiot.v1.CloudIotScopes;
import com.google.api.services.cloudiot.v1.model.Device;
import com.google.api.services.cloudiot.v1.model.DeviceCredential;
import com.google.api.services.cloudiot.v1.model.PublicKeyCredential;
import com.google.common.io.Files;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;





public class MainActivity extends AppCompatActivity{

    private final String TAG = "[Joe]";
    private final int PORT = 1234;
    //EditText editTextAddress, editTextPort, editTextTime,editTextPRC;
    Button buttonConnect,buttonFanspeed,buttonFirestore;
    TextView textViewState, textViewRx,textViewFanState;

    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;
    //private boolean responseFlag = false;
    //private String responseText = null;
    //private int RETRY_TIME = 5;
    final static String STR_DB_COLLECTION_NAME = "device-configs";
    final static String MOS_RPC_CMD_GET_SYS_INFO = "{\"id\":1999,\"method\":\"Sys.GetInfo\"}";
    final static String MOS_RPC_CMD_SET_WIFI_CONNECTION = "{\"id\": 1998, \"method\": \"AMT.ConnectToAP\", \"params\": {\"ssid\": \"VXA\", \"pass\": \"12345678\"}}";
    final static String MOS_RPC_CMD_GET_WIFI_IP = "{\"id\":1996,\"method\":\"Joe.GetDeviceIP\"}";
    private static String device_id = "";
    String localAddress = "",serverMAC = "",serverAddress = "";
    private static String device_response = "";
    // Firebase variables

    private FirebaseFirestore db;
    private int fan_button_count = 0;
    private Map<String, Object> table = new HashMap<>();

    private WifiManager mWifiMngr;
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> mWifiConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //UI init
        setContentView(R.layout.activity_main);

        textViewState = (TextView)findViewById(R.id.state);
        textViewRx = (TextView)findViewById(R.id.received);
        textViewFanState = (TextView)findViewById(R.id.fan_state);

        buttonConnect = (Button) findViewById(R.id.connect);

        buttonFanspeed = (Button) findViewById(R.id.fanspeed);
        //buttonWifi = (Button) findViewById(R.id.btn_wifi);
        buttonFirestore = (Button) findViewById(R.id.btn_firestore);
        //buttonDisconnect = (Button) findViewById(R.id.btn_disconnect);
        //buttonInternal = (Button) findViewById(R.id.btn_internal);
        //buttonExternal = (Button) findViewById(R.id.btn_external);



        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        //buttonWifi.setOnClickListener(buttonSetWifiOnClickListener);
        buttonFanspeed.setOnClickListener(buttonSetFanSpeedOnClickListener);
        //buttonDisconnect.setOnClickListener(buttonDisConnectOnClickListener);
        buttonFirestore.setOnClickListener(buttonFirestoreOnClickListener);
        //buttonInternal.setOnClickListener(buttonInternalOnClickListener);
        //buttonExternal.setOnClickListener(buttonExternalOnClickListener);
        //UDP init
        udpClientHandler = new UdpClientHandler(this);

        //firestore init
        db = FirebaseFirestore.getInstance();
        init_fire_store_snapshot_listener();
        fan_button_count = 0;
        table.put("FanSpeed", "Low");
        table.put("OnOff", false);
    }

    public static String intToIpAddress(long ipInt) {
        StringBuffer sb = new StringBuffer();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
    //Button Click : Get Device Info
    View.OnClickListener buttonConnectOnClickListener =
            new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    //Get Wifi Information
                    get_device_IP();
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
                    //thread
                    try {
                        getDeviceInfo();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        connect();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //init_fire_store_snapshot_listener();
                    buttonConnect.setEnabled(false);
                }
            };
    //Button Click: Set Wifi to Device
    /*
    View.OnClickListener buttonSetWifiOnClickListener =
            new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    //getDeviceIdFromJSON(device_response);
                    //connect();
                    buttonWifi.setEnabled(false);
                }
            };

     */
    //Button Click : Set FanSpeed
    View.OnClickListener buttonSetFanSpeedOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    setFanspeed();
                }
            };
    View.OnClickListener buttonFirestoreOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    init_fire_store_snapshot_listener();
                }
            };
    //Button Click : Wifi DisConnect
    /*
    View.OnClickListener buttonDisConnectOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    disconnect();
                    buttonDisconnect.setEnabled(false);
                }
            };

     */
    /*
    View.OnClickListener buttonInternalOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    internal_connect();
                    buttonInternal.setEnabled(false);
                    buttonExternal.setEnabled(true);
                }
            };
    View.OnClickListener buttonExternalOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    external_connect();
                    buttonExternal.setEnabled(false);
                    buttonInternal.setEnabled(true);
                }
            };

     */
    public void setFanspeed(){
        switch (fan_button_count) {
            case 0: // Power on, LOW
                fan_button_count++;
                table.put("FanSpeed", "Low");
                table.put("OnOff", true);
                Log.d("[Joe4]","FanSpeed:Low,OnOff:true");
                break;
            case 1: // MEDIUM
                fan_button_count++;
                table.put("FanSpeed", "Medium");
                table.put("OnOff", true);
                Log.d("[Joe4]","FanSpeed:Low,OnOff:true");
                break;
            case 2: // HIGH
                fan_button_count++;
                table.put("FanSpeed", "High");
                table.put("OnOff", true);
                break;
            case 3: // Power Off
                fan_button_count = 0;
                table.put("FanSpeed", "Low");
                table.put("OnOff", false);
                break;
            default:
                fan_button_count = 0;
                break;
        }
        update_fire_store_document();
    }
    //Message Handler
    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
        if(!rxmsg.equals("") || rxmsg != null){
            device_response = rxmsg;
            getDeviceIdFromJSON();

        }
    }

    private void clientEnd(){
        udpClientThread = null;
        textViewState.setText("clientEnd()");

        buttonConnect.setEnabled(true);
        //buttonWifi.setEnabled(true);

    }
    //UDP Handler
    public static class UdpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
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
                default:
                    super.handleMessage(msg);
            }

        }
    }

    //RPC : Get Sys Info
    public void getDeviceInfo() throws InterruptedException {
        textViewRx.setText("");
        udpClientThread = new UdpClientThread(
                serverAddress,
                PORT,
                MOS_RPC_CMD_GET_SYS_INFO,
                udpClientHandler);
        udpClientThread.start();
        udpClientThread.join(1000);

    }
    //RPC : Set Wifi
    public void connect() throws InterruptedException {
        if(serverAddress != null) {
            udpClientThread = new UdpClientThread(
                    serverAddress,
                    PORT,
                    MOS_RPC_CMD_SET_WIFI_CONNECTION,
                    udpClientHandler);
            udpClientThread.start();
            udpClientThread.join(4000);
        }
    }

    public void getDeviceIdFromJSON() {
        try {
            JSONObject response = new JSONObject(device_response);
            JSONObject  result = response.getJSONObject("result");
            if(result.getString("id") != null){
                device_id = result.getString("id");
                Log.d("[Joe3]",device_id);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //init_fire_store_snapshot_listener();
    }

    public void startScan() {
        mWifiMngr.startScan();
// 得到掃描結果
        mWifiList = mWifiMngr.getScanResults();
// 得到配置好的網络連接
        mWifiConfiguration = mWifiMngr.getConfiguredNetworks();
    }
    public void disconnect(){
        //WifiManager mWifiMngr;
        mWifiMngr = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        if(mWifiMngr.isWifiEnabled()){
            mWifiMngr.setWifiEnabled(false);
        }
    }
    public void internal_connect(){

        //open wifi
        mWifiMngr = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        if(!mWifiMngr.isWifiEnabled()){
            mWifiMngr.setWifiEnabled(true);
        }
        startScan();
        //connect to wifi
        //mWifiMngr.enableNetwork(mWifiConfiguration.get(index).networkId, true);
    }
    public void external_connect(){

        //open wifi
        mWifiMngr = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        if(!mWifiMngr.isWifiEnabled()){
            mWifiMngr.setWifiEnabled(true);
        }
        //connect to wifi
        //mWifiMngr.enableNetwork(mWifiConfiguration.get(index).networkId, true);
    }

    public void get_device_IP() {
        WifiManager mWifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(mWifi.isWifiEnabled()) {
            WifiInfo wifi = mWifi.getConnectionInfo();
            localAddress = intToIpAddress(wifi.getIpAddress());//Local IP
            serverMAC =  wifi.getBSSID();//Hotspot MAC

            DhcpInfo dhcpinfo = mWifi.getDhcpInfo();
            serverAddress = intToIpAddress(dhcpinfo.serverAddress);//Hotspot IP
            /*
            System.out.println("[Joe]:local IP = "+localAddress);
            System.out.println("[Joe]:server MAC = "+serverMAC);
            System.out.println("[Joe]:server IP = "+serverAddress);

             */
        }
    }

    public String get_iot_device_name() {

        return device_id;
    }


    //Init Firestore and SnapshotListener
    public void init_fire_store_snapshot_listener() {
        //getDeviceIdFromJSON();
        String _str_device_name = get_iot_device_name();
        Log.d("[Joe]",_str_device_name);
        if (_str_device_name.equals("")) {
            Log.w(TAG, "Init firestore snapshot listener fail because device name is null");
            //b_is_db_snapshot_listener_enable = false;
            return;
        }

        //Set initial value
        db.collection(STR_DB_COLLECTION_NAME).document(_str_device_name)
                .set(table)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });


        db.collection(STR_DB_COLLECTION_NAME).document(_str_device_name).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Map curTable = snapshot.getData();
                    Log.d(TAG, "Current data: " + curTable);
                    fan_state_update(curTable);
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });



        Log.i(TAG, "Init firestore snapshot listener successfully");
        //b_is_db_snapshot_listener_enable = true;
    }

    //Update FanSpeed Label
    public void fan_state_update(Map curTable) {
        table.put("FanSpeed", curTable.get("FanSpeed"));
        table.put("OnOff", curTable.get("OnOff"));

        String fan_speed = (String) table.get("FanSpeed");
        Boolean on_off = (Boolean) table.get("OnOff");

        Log.d(TAG, "FanSpeed: " + fan_speed + ", OnOff: " + on_off);

        if (on_off) {
            if (fan_speed.equals("Low")) {
                textViewFanState.setText("Speed: Low");
            } else if (fan_speed.equals("Medium")) {
                textViewFanState.setText("Speed: Medium");
            } else if (fan_speed.equals("High")) {
                textViewFanState.setText("Speed: High");
            } else {
                Log.d(TAG, "Error occurs, FanSpeed: " + fan_speed);
            }
        } else {
            textViewFanState.setText("Power OFF");
        }
    }

    //Update Firestore
    public void update_fire_store_document() {
        String _str_device_name = get_iot_device_name();
        if (_str_device_name.equals("")) {
            Log.w(TAG, "Update firestore document fail because device name is null");
            return;
        }

        db.collection(STR_DB_COLLECTION_NAME).document(_str_device_name)
                .update(table)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }
}

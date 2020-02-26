package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

class MyBleManager extends BleManager<BleManagerCallbacks> {

    final static UUID DEVICE_UUID = UUID.fromString("7d80948d-bfc4-ca07-424a-ec961a3a016d");

    final static UUID SERVICE_UUID = UUID.fromString("5f6d4f53-5f52-5043-5f53-56435f49445f");
    final static UUID MOS_RPC_TX_CTL_CHAR   = UUID.fromString("5f6d4f53-5f52-5043-5f74-785f63746c5f");
    final static UUID MOS_RPC_DATA_CHAR  = UUID.fromString("5f6d4f53-5f52-5043-5f64-6174615f5f5f");

    final static String MOS_RPC_CMD_GET_SYS_INFO = "{\"id\":1999,\"method\":\"Sys.GetInfo\"}";
    final static String MOS_RPC_CMD_SET_WIFI_CONNECTION = "{\"id\": 1998, \"method\": \"AMT.ConnectToAP\", \"params\": {\"ssid\": \"VXA\", \"pass\": \"12345678\"}}";

    // Client characteristics
    private BluetoothGattCharacteristic mos_rpc_tx_ctl_char, mos_rpc_data_char;

    // Variable for ble char reading
    private String str_raw_data = "";
    private int read_char_times = 0;

    // MainActivity object for firestore access
    private MainActivity mActivity;

    MyBleManager(@NonNull final Context context) {
        super(context);
        mActivity = (MainActivity) context;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new MyManagerGattCallback();
    }

    @Override
    public void log(final int priority, @NonNull final String message) {
        // Please, don't log in production.
            Log.d("MyBleManager", message);
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class MyManagerGattCallback extends BleManagerGattCallback{
        // This method will be called when the device is connected and services are discovered.
        // You need to obtain references to the characteristics and descriptors that you will use.
        // Return true if all required services are found, false otherwise.
        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                mos_rpc_tx_ctl_char = service.getCharacteristic(MOS_RPC_TX_CTL_CHAR);
                mos_rpc_data_char = service.getCharacteristic(MOS_RPC_DATA_CHAR);
            }
            // Validate properties
/*
            boolean notify = false;
            if (firstCharacteristic != null) {
                final int properties = dataCharacteristic.getProperties();
                notify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
            }

 */
            //boolean writeRequest = false;
            if (mos_rpc_tx_ctl_char != null) {
                //final int properties = controlPointCharacteristic.getProperties();
                //writeRequest = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                mos_rpc_tx_ctl_char.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }
            if (mos_rpc_data_char != null) {
                //final int properties = controlPointCharacteristic.getProperties();
                //writeRequest = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                mos_rpc_data_char.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }
            // Return true if all required services have been found
            return mos_rpc_tx_ctl_char != null && mos_rpc_data_char != null;
/*
            return firstCharacteristic != null && secondCharacteristic != null
                    && notify && writeRequest;

 */
        }

        // If you have any optional services, allocate them here. Return true only if
        // they are found.
        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return super.isOptionalServiceSupported(gatt);
        }

        private FluxHandler fluxHandler = new FluxHandler() {
            public void onFluxDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {

                if(read_char_times == 0)
                {
                    return;
                }

                read_char_times--;
                str_raw_data = str_raw_data + new String(data.getValue(), StandardCharsets.UTF_8);
                log(Log.WARN, "[Miles] Get JSON: " + str_raw_data);

                if(mActivity != null) {

                    String _str_device_name = "";

                    Pattern pattern = Pattern.compile("\"id\": \"(esp32_[a-zA-Z0-9]*)\"");
                    Matcher m = pattern.matcher(str_raw_data);
                    while (m.find()) {
                        _str_device_name = m.group(1);
                        log(Log.WARN, "[Miles] Device Name: " +  m.group(1));
                    }

                    if(!_str_device_name.equals("")) {
                        mActivity.set_iot_device_name(_str_device_name);
                        mActivity.init_fire_store_snapshot_listener();
                        mActivity.create_fire_store_document();

                        str_raw_data = "";
                        read_char_times = 0;
                    }
                    else
                    {
                        log(Log.WARN, "[Miles] Device Name: is NULL!!!!!!!!!!!!!!!!!!!!!");
                    }
                }
            }
        };

        private byte[] int_to_four_bytes(int value)
        {
            byte[] bytes = new byte[4];
            ByteBuffer.wrap(bytes).putInt(value);
            return bytes;
            //return Arrays.copyOfRange(bytes, 4, 8);
        }

        // Initialize your device here. Often you need to enable notifications and set required
        // MTU or write some initial data. Do it here.
        @Override
        protected void initialize() {

            // This is the length of data to write
            //byte[] byteToWrite = {00, 00, 00, 0x22};

            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                    .add(requestMtu(247) // Remember, GATT needs 3 bytes extra. This will allow packet size of 244 bytes.
                            .with((device, mtu) -> log(Log.INFO, "MTU set to " + mtu))
                            .fail((device, status) -> log(Log.WARN, "Requested MTU not supported: " + status)))
                    .add(setPreferredPhy(PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_OPTION_NO_PREFERRED)
                            .fail((device, status) -> log(Log.WARN, "Requested PHY not supported: " + status)))
                    .add(enableNotifications(mos_rpc_data_char))
                    .done(device -> log(Log.INFO, "Target initialized"))
                    .enqueue();
            // You may easily enqueue more operations here like such:

            // To write the length of RPC command we want to send to BLE device
            writeCharacteristic(mos_rpc_tx_ctl_char, int_to_four_bytes(MOS_RPC_CMD_GET_SYS_INFO.length()))
                    .done(device -> log(Log.INFO, "Greetings sent"))
                    .enqueue();

            // To write the exactly RPC command
            writeCharacteristic(mos_rpc_data_char, MOS_RPC_CMD_GET_SYS_INFO.getBytes())
                    .done(device -> log(Log.INFO, "Greetings sent"))
                    .enqueue();

            // To read the result of RPC command execution
            // Because the length of result string is long, we need to read a few times
            read_char_times = 3;
            str_raw_data = "";

            readCharacteristic(mos_rpc_data_char)
                    .with(fluxHandler)
                    .enqueue();

            readCharacteristic(mos_rpc_data_char)
                    .with(fluxHandler)
                    .enqueue();

            readCharacteristic(mos_rpc_data_char)
                    .with(fluxHandler)
                    .enqueue();

            writeCharacteristic(mos_rpc_tx_ctl_char, int_to_four_bytes(MOS_RPC_CMD_SET_WIFI_CONNECTION.length()))
                    .done(device -> log(Log.INFO, "Greetings sent"))
                    .enqueue();

            writeCharacteristic(mos_rpc_data_char, MOS_RPC_CMD_SET_WIFI_CONNECTION.getBytes())
                    .done(device -> log(Log.INFO, "Greetings sent"))
                    .enqueue();

            // Set a callback for your notifications. You may also use waitForNotification(...).
            // Both callbacks will be called when notification is received.
            //setNotificationCallback(firstCharacteristic, callback);
            // If you need to send very long data using Write Without Response, use split()
            // or define your own splitter in split(DataSplitter splitter, WriteProgressCallback cb).
/*
            writeCharacteristic(secondCharacteristic, "Very, very long data that will no fit into MTU")
                    .split()
                    .enqueue();
 */
        }

        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            mos_rpc_tx_ctl_char = null;
            mos_rpc_data_char = null;
        }

        // Define your API.
    };


    private abstract class FluxHandler implements DataReceivedCallback {
        @Override
        public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                onFluxDataReceived(device, data);
        }

        abstract void onFluxDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data);
    }

    /** Initialize time machine. */
/*
    public void enableFluxCapacitor(final int year) {

        waitForNotification(firstCharacteristic)
                .trigger(
                        writeCharacteristic(secondCharacteristic, new FluxJumpRequest(year))
                                .done(device -> log(Log.INDO, "Power on command sent"))
                )
                .with(new FluxHandler() {
                    public void onFluxCapacitorEngaged() {
                        log(Log.WARN, "Flux Capacitor enabled! Going back to the future in 3 seconds!");
                        callbacks.onFluxCapacitorEngaged();

                        sleep(3000).enqueue();
                        write(secondCharacteristic, "Hold on!".getBytes())
                                .done(device -> log(Log.WARN, "It's " + year + "!"))
                                .fail((device, status) -> "Not enough flux? (status: " + status + ")")
                                .enqueue();
                    }
                })
                .enqueue();

    }
 */

    /**
     * Aborts time travel. Call during 3 sec after enabling Flux Capacitor and only if you don't
     * like 2020.
     */
    public void abort() {
        cancelQueue();
    }
}

interface FluxCallbacks extends BleManagerCallbacks {
    void onFluxCapacitorEngaged();
}

public class MainActivity extends AppCompatActivity implements FluxCallbacks, ScanResultAdapter.OnItemClickHandler {

    private static final String TAG = "MainActivity";

    private Button btnWriteDB;
    private TextView textState;

    private Button btnConnectBLE;
    private Button btnDisconnectBLE;
    private Button btnScanBLE;

    private RecyclerView recyclerDevices;

    // Access a Cloud Firestore instance from your Activity
    private FirebaseFirestore db;

    private static final String STR_DB_COLLECTION_NAME = "device-configs";
    private static String str_device_name = "";
    private static Boolean b_is_db_snapshot_listener_enable = false;

    // Firebase variables
    private Map<String, Object> table = new HashMap<>();
    private int fan_button_count = 0;

    // BLE variables
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice bleDevice = null;
    private MyBleManager bleManager = null;
    private BluetoothLeScannerCompat bleScanner = null;

    private Boolean isBleDeviceScanning = false;

    // BLE scanning
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static final int REQUEST_CODE_LOCATION_SETTINGS = 2;

    // BLE devices list structure
    ArrayList<ScanResult> scanResultArrayList = new ArrayList<>();
    ScanResultAdapter scanResultAdapter;

    // Dialog
    private boolean dialogResultValue = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Init UI
        btnWriteDB = (Button) findViewById(R.id.button_write);
        btnWriteDB.setText("FanSpeed");

        textState = (TextView) findViewById(R.id.text_state);
        textState.setText("Please scan devices");

        btnConnectBLE = (Button) findViewById(R.id.button_connect);
        btnConnectBLE.setText("Connect");

        btnDisconnectBLE = (Button) findViewById(R.id.button_disconnect);
        btnDisconnectBLE.setText("Disconnect");

        btnConnectBLE.setEnabled(true);
        btnDisconnectBLE.setEnabled(false);


        btnScanBLE = (Button) findViewById(R.id.button_scan);
        btnScanBLE.setText("Start Scan");

        recyclerDevices = (RecyclerView) findViewById(R.id.recycler_devices);
        recyclerDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerDevices.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Init Variable
        fan_button_count = 0;
        table.put("FanSpeed", "Low");
        table.put("OnOff", false);
        //fan_state_update(table);

        // Init Firestore
        db = FirebaseFirestore.getInstance();
        init_fire_store_snapshot_listener();

        // Init BLE
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Init Nordic BLE manager
        bleManager = new MyBleManager(this);

        // Init Nordic BLE scanner
        bleScanner = BluetoothLeScannerCompat.getScanner();

        // Init BLE devices list
        scanResultAdapter = new ScanResultAdapter(scanResultArrayList, this);
        recyclerDevices.setAdapter(scanResultAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void fan_state_update(Map curTable) {
        table.put("FanSpeed", curTable.get("FanSpeed"));
        table.put("OnOff", curTable.get("OnOff"));

        String fan_speed = (String) table.get("FanSpeed");
        Boolean on_off = (Boolean) table.get("OnOff");

        Log.d(TAG, "FanSpeed: " + fan_speed + ", OnOff: " + on_off);

        if (on_off) {
            if (fan_speed.equals("Low")) {
                textState.setText("Speed: Low");
            } else if (fan_speed.equals("Medium")) {
                textState.setText("Speed: Medium");
            } else if (fan_speed.equals("High")) {
                textState.setText("Speed: High");
            } else {
                Log.d(TAG, "Error occurs, FanSpeed: " + fan_speed);
            }
        } else {
            textState.setText("Power OFF");
        }
    }

    public void set_iot_device_name(String _str_device_name) {
        str_device_name = _str_device_name;
    }

    public String get_iot_device_name() {
        return str_device_name;
    }

    public Boolean is_fire_store_snapshot_listener_enable() {
        return b_is_db_snapshot_listener_enable;
    }

    public void init_fire_store_snapshot_listener() {
        String _str_device_name = get_iot_device_name();

        if (_str_device_name.equals("")) {
            Log.w(TAG, "Init firestore snapshot listener fail because device name is null");
            b_is_db_snapshot_listener_enable = false;
            return;
        }

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
        b_is_db_snapshot_listener_enable = true;
    }

    public void create_fire_store_document() {
        String _str_device_name = get_iot_device_name();
        if (_str_device_name.equals("")) {
            Log.w(TAG, "Create firestore document fail because device name is null");
            return;
        }

        String _str_user_email = "xculadox@gmail.com";
        table.put("E-mail", _str_user_email);

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
    }

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

    private static class OuterHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public OuterHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                throw new RuntimeException();
            }
        }
    }

    public boolean show_alert_dialog_confirm(Context context, String message) {
        final Handler handler = new OuterHandler(this);

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Alert");
        alert.setMessage(message);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialogResultValue = true;
                handler.sendMessage(handler.obtainMessage());
            }
        });

        /*
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                dialogResultValue = false;
                handler.sendMessage(handler.obtainMessage());
            }
        });

         */

        alert.show();

        try {
            Looper.loop();
        } catch (RuntimeException e) {
        }

        return dialogResultValue;
    }

    public void ble_device_connect(String mac_address) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "BT adapter is null");
            return;
        }

        if (bleManager == null) {
            Log.w(TAG, "BLE manager is null");
            return;
        }

        bleDevice = bluetoothAdapter.getRemoteDevice(mac_address);

        if (bleDevice == null) {
            Log.w(TAG, "BLE device is null");
            return;
        }

        textState.setText("Connecting...");

        bleManager.setManagerCallbacks(this);

        bleManager.connect(bleDevice)
                .timeout(100000)
                .retry(3, 100)
                .done(device -> {
                    Log.i(TAG, "Device connected");
                    btnConnectBLE.setEnabled(false);
                    btnDisconnectBLE.setEnabled(true);
                })
                .enqueue();
    }

    public void ble_device_disconnect() {
        if (bleManager == null) {
            Log.w(TAG, "BLE manager is null");
            return;
        }

        bleManager.disconnect()
                .done(device -> {
                    Log.i(TAG, "Device disconnected");
                    btnConnectBLE.setEnabled(true);
                    btnDisconnectBLE.setEnabled(false);
                    textState.setText("Disconnected");
                })
                .enqueue();
    }

    public boolean location_permission_check() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    show_alert_dialog_confirm(this, "Please enable location permission for BLE scanning after Anroid 6.0");
                }

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);

                return false;
            }
        }
        return true;
    }

    public boolean location_setting_check() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    private void location_setting_request() {
        show_alert_dialog_confirm(this, "Please enable location setting to support BLE scanning.");

        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        this.startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {

            scanResultArrayList.clear();
            for (ScanResult r : results) {
                String bleDeviceName = r.getDevice().getName();
                String bleDeviceMAC = r.getDevice().getAddress();

                if (bleDeviceName != null) {
                    Log.i(TAG, "============== Name: " + bleDeviceName + ", MAC: " + bleDeviceMAC + " ==============");
                    scanResultArrayList.add(r);
                }
            }
            scanResultAdapter.notifyDataSetChanged();
        }
    };

    public void ble_device_start_scan() {

        if (!location_setting_check()) {
            location_setting_request();
            return;
        }

        if (!location_permission_check()) {
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(3000)
                .setUseHardwareBatchingIfSupported(true)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        //ParcelUuid deviceUUID = new ParcelUuid(bleManager.SERVICE_UUID);
        //filters.add(new ScanFilter.Builder().setServiceUuid(deviceUUID).build());
        //filters.add(new ScanFilter.Builder().setDeviceName("MyDeviceName_Miles").build());

        bleScanner.startScan(filters, settings, scanCallback);

        isBleDeviceScanning = true;
        btnScanBLE.setText("Stop Scan");
        textState.setText("Scanning...");
    }

    public void ble_device_stop_scan() {

        Log.i(TAG, "ble_device_stop_scan");

        bleScanner.stopScan(scanCallback);

        isBleDeviceScanning = false;
        btnScanBLE.setText("Start Scan");
        textState.setText("Please scan devices");
    }

    public void write_button_on_click(View view) {

        switch (fan_button_count) {
            case 0: // Power on, LOW
                fan_button_count++;
                table.put("FanSpeed", "Low");
                table.put("OnOff", true);
                break;
            case 1: // MEDIUM
                fan_button_count++;
                table.put("FanSpeed", "Medium");
                table.put("OnOff", true);
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

    public void connect_button_on_click(View view) {
        ble_device_connect("A4:CF:12:81:4F:DE");
    }

    public void disconnect_button_on_click(View view) {
        ble_device_disconnect();
    }

    public void scan_button_on_click(View view) {
        if (isBleDeviceScanning) {
            ble_device_stop_scan();
        } else {
            ble_device_start_scan();
        }
    }

    @Override
    public void onFluxCapacitorEngaged() {
        Log.i(TAG, "onFluxCapacitorEngaged");
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        Log.i(TAG, "onDeviceConnected");
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {

    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {

    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onItemClick(int position) {
        if (isBleDeviceScanning) {
            ble_device_stop_scan();
        }
        ble_device_connect(scanResultArrayList.get(position).getDevice().getAddress());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User choose permission allowed
            } else {
                // User choose permission denied
                show_alert_dialog_confirm(this, "Location permission is denied, the ble scanning is not supported.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            if (location_setting_check()) {
            } else {
                show_alert_dialog_confirm(this, "Location setting is disable, the ble scanning is not supported.");
            }
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    final static String GOOGLE_IOT_CORE_DEVICE_ID_FOR_TEST = "TEST"; //"esp32_814FDC";
    final static String GOOGLE_IOT_CORE_PUBLIC_KEY_PATH = "rsa_public.pem";
    final static String GOOGLE_IOT_CORE_PROJECT_ID = "miles-simple-iot";
    final static String GOOGLE_IOT_CORE_CLOUD_REGION = "europe-west1";
    final static String GOOGLE_IOT_CORE_REGISTRY_NAME = "iot-registry";
    final static String APP_NAME = "com.example.myapplication";

    /** Create a device that is authenticated using RS256. */
    public static void createDeviceWithRs256(
            String deviceId,
            String certificateFilePath,
            String projectId,
            String cloudRegion,
            String registryName)
            throws GeneralSecurityException, IOException {

        // Now system will be crashed when calling "createScoped", please check stack-overflow for reason
        // https://stackoverflow.com/questions/60314779/how-to-creating-device-to-google-iot-core-by-android-application

        GoogleCredential credential =
                GoogleCredential.getApplicationDefault().createScoped(CloudIotScopes.all());

        /*
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        HttpRequestInitializer init = new RetryHttpInitializerWrapper(credential);
        final CloudIot service =
                new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
                        .setApplicationName(APP_NAME)
                        .build();

        final String registryPath =
                String.format(
                        "projects/%s/locations/%s/registries/%s", projectId, cloudRegion, registryName);

        PublicKeyCredential publicKeyCredential = new PublicKeyCredential();
        String key = Files.toString(new File(certificateFilePath), Charsets.UTF_8);
        publicKeyCredential.setKey(key);
        publicKeyCredential.setFormat("RSA_X509_PEM");

        DeviceCredential devCredential = new DeviceCredential();
        devCredential.setPublicKey(publicKeyCredential);

        System.out.println("Creating device with id: " + deviceId);
        Device device = new Device();
        device.setId(deviceId);
        device.setCredentials(Arrays.asList(devCredential));
        Device createdDevice =
                service
                        .projects()
                        .locations()
                        .registries()
                        .devices()
                        .create(registryPath, device)
                        .execute();

        System.out.println("Created device: " + createdDevice.toPrettyString());

         */
    }

    public void register_button_on_click(View view) {
        try {
            createDeviceWithRs256(
                    GOOGLE_IOT_CORE_DEVICE_ID_FOR_TEST,
                    GOOGLE_IOT_CORE_PUBLIC_KEY_PATH,
                    GOOGLE_IOT_CORE_PROJECT_ID,
                    GOOGLE_IOT_CORE_CLOUD_REGION,
                    GOOGLE_IOT_CORE_REGISTRY_NAME);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
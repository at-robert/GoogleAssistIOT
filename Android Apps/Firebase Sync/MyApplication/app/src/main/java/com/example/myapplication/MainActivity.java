package com.example.myapplication;

import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button btnWriteDB;
    private TextView textState;

    // Access a Cloud Firestore instance from your Activity
    private FirebaseFirestore db;

    private Map<String, Object> table = new HashMap<>();
    private int fan_button_count = 0;

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

        btnWriteDB = (Button) findViewById(R.id.button_write);
        btnWriteDB.setText("Push");

        textState = (TextView) findViewById(R.id.text_state);
        textState.setText("Connecting...");

        db = FirebaseFirestore.getInstance();

        fan_button_count = 0;
        table.put("FanSpeed", "Low");
        table.put("OnOff", false);
        fan_state_update(table);

        //final DocumentReference docRef = db.collection("cities").document("SF");
        db.collection("device-configs").document("esp8266_FD8185").addSnapshotListener(new EventListener<DocumentSnapshot>() {
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

    public void fan_state_update(Map curTable)
    {
        table.put("FanSpeed", curTable.get("FanSpeed"));
        table.put("OnOff", curTable.get("OnOff"));

        String fan_speed = (String) table.get("FanSpeed");
        Boolean on_off = (Boolean) table.get("OnOff");

        Log.d(TAG, "FanSpeed: " + fan_speed + ", OnOff: " + on_off);

        if(on_off)
        {
            if(fan_speed.equals("Low"))
            {
                textState.setText("Speed: Low");
            }
            else if(fan_speed.equals("Medium"))
            {
                textState.setText("Speed: Medium");
            }
            else if(fan_speed.equals("High"))
            {
                textState.setText("Speed: High");
            }
            else
            {
                Log.d(TAG, "Error occurs, FanSpeed: " + fan_speed);
            }
        }
        else
        {
            textState.setText("Power OFF");
        }
    }

    public void write_button_on_click(View view) {

        switch(fan_button_count)
        {
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

        db.collection("device-configs").document("esp8266_FD8185")
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

package com.example.udp_test3;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class Database {

    private final String TAG = "[Firestore]";

    private MainActivity.DatabaseHandler handler = null;
    //Firestore
    private FirebaseFirestore db;
    private Map<String, Object> value = new HashMap<>();
    private String collection, document;
    DocumentReference dbRef;

    //Constructure
    public Database(String collection,String document,MainActivity.DatabaseHandler handler){
        this.collection = collection;
        this.document = document;
        this.handler = handler;
        value.put("OnOff",false);
        value.put("FanSpeed","Low");
        db = FirebaseFirestore.getInstance();
        dbRef = db.collection(this.collection).document(this.document);
    }

    public Map<String, Object> getValue(){
        return value;
    }
    public void setValue(Map value){
        this.value = value;
    }

    public void initial_firestore(){
        Log.d(TAG,"initial_firestore");
        dbRef.set(value)
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

        dbRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Map<String,Object> curTable = snapshot.getData();
                    Log.d(TAG, "Current data: " + curTable);
                    handler.sendMessage(
                            Message.obtain(handler, MainActivity.DatabaseHandler.UPDATE_STATE, curTable));
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    public void update_firestore(Map<String, Object> data){
        Log.d(TAG,"update_firestore");
        dbRef.update(data)
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

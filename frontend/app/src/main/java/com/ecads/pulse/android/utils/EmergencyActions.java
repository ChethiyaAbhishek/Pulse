package com.ecads.pulse.android.utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.model.Patient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class EmergencyActions {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences preferences;
    private String firestoreEmergencyContact;
    private String firestoreCareGiverContact;
    private final Context context;
    private MapApi mapApi;

    public EmergencyActions(Context context) {
        this.context = context;
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        preferences = context.getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);
        mapApi = new MapApi();
    }

    public void makeCall() {
        String emergencyContact = preferences.getString("emergencyContact", null);

        if (emergencyContact == null) {
            firestore.collection("patients")
                    .document("PA_" + firebaseAuth.getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot != null) {
                                Patient patient = documentSnapshot.toObject(Patient.class);
                                if (patient != null) {
                                    String emerContact = patient.getEmerContact();
                                    Intent callingIntent = new Intent(Intent.ACTION_CALL);
                                    callingIntent.setData(Uri.parse("tel:" + emerContact));
                                    context.startActivity(callingIntent);

                                    firestoreEmergencyContact = patient.getCgiverContact();
                                    firestoreCareGiverContact = patient.getCgiverContact();
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            new AlertDialog.Builder(context)
                                    .setTitle("No Emergency contact found")
                                    .setMessage("Call 1990?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent callingIntent = new Intent(Intent.ACTION_CALL);
                                            callingIntent.setData(Uri.parse("tel:1990"));
                                            context.startActivity(callingIntent);
                                        }
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        }
                    });
        } else {
            Intent callingIntent = new Intent(Intent.ACTION_CALL);
            callingIntent.setData(Uri.parse(emergencyContact));
            context.startActivity(callingIntent);
        }
    }

    public void sendSMS() {

        String careGiverContact = preferences.getString("cgiverContact", null);
        String emergencyContact = preferences.getString("emergencyContact", null);

        if (careGiverContact == null || emergencyContact == null) {
            if (firestoreEmergencyContact == null && firestoreCareGiverContact == null) {
                firestore.collection("patients")
                        .document("PA_" + firebaseAuth.getUid())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot != null) {
                                    Patient patient = documentSnapshot.toObject(Patient.class);
                                    if (patient != null) {
                                        firestoreEmergencyContact = patient.getEmerContact();
                                        firestoreCareGiverContact = patient.getCgiverContact();
                                        forwardSMS();
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "SMS sending failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                forwardSMS();
            }
        } else {
            if (firestoreEmergencyContact == null) {
                firestoreEmergencyContact = emergencyContact;
            }

            if (firestoreCareGiverContact == null) {
                firestoreCareGiverContact = careGiverContact;
            }

            forwardSMS();
        }
    }

    private void forwardSMS() {
        StringBuilder message = new StringBuilder("PULSE: EMERGENCY !!! \n"
                + preferences.getString("fname", "")
                + " needs your help.");

        mapApi.getCurrentLocation(context, latLng -> {

            if (latLng != null) {
                String mapUrl = "https://www.google.com/maps?q="
                        + latLng.latitude + "," + latLng.longitude;
                message.append("\nCurrect Location: \n ");
                message.append(mapUrl);
            }

            Log.i("Emergency", "SMS: " + message);

            List<String> contactList = Arrays.asList(
                    firestoreCareGiverContact,
                    firestoreEmergencyContact);

            SmsManager manager = SmsManager.getDefault();

            for (String contact : contactList) {
                if (contact != null) {
                    manager.sendTextMessage(contact, null, message.toString(), null, null);
                }
            }
        });
    }


}

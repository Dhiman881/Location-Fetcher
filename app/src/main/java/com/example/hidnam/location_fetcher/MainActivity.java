package com.example.hidnam.location_fetcher;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {


    //private  GoogleApiClient mGoogleApiClient;
    //private LocationRequest mLocationRequest;
    // private Button btnStart;
    private Button btnStop;
    private TextView locText;
    private TextView address;
    private LocationManager lm;
    public static final int REQUEST_LOCATION=001;
    private ProgressBar mProgressbar;
    private BroadcastReceiver broadcastReceiver;
    private Button btnSOS;
    private static final String URL_POST = "https://www.Doorhopper.in/JUNK";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //btnStart = (Button) findViewById(R.id.button);
        btnStop = (Button) findViewById(R.id.button2);
        locText = (TextView) findViewById(R.id.textView);
        address =(TextView)findViewById(R.id.textView3);
        btnSOS = (Button)findViewById(R.id.sos_btn);
        mProgressbar = (ProgressBar)findViewById(R.id.progressBar1);
        if (!runtime_permissions())
            enable_buttons();
        lm = (LocationManager)getSystemService(LOCATION_SERVICE);
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            // notify user you are online
        } else {
            // notify user you are not online
            Toast.makeText(this,"Network is not Connected",Toast.LENGTH_SHORT).show();
            mProgressbar.setVisibility(View.INVISIBLE);
            address.setText("Network not available!!");
        }

        btnSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSOS();
            }
        });
    }
    protected void onResume() {
        super.onResume();
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            enableGps();


        }
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getExtras().containsKey("coordinates")) {
                        mProgressbar.setVisibility(View.INVISIBLE);
                        locText.setText(intent.getExtras().get("coordinates").toString());
                        if(intent.getExtras().get("Address") !=null) {
                            String str = intent.getExtras().get("Address").toString();
                            if(!TextUtils.isEmpty(str)){
                                address.setText("Address"+"\n"+str);
                            }
                        }


                    }
                    if(intent.getExtras().containsKey("enable gps"))
                        enableGps();


                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));

    }

    private void sendSOS(){


        JSONObject jsonObject = new JSONObject();
        try {
            // user_id, comment_id,status
            jsonObject.put("token","YOBHATT" );
            jsonObject.put("data","location");
            jsonObject.put("value","SOS");

        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                URL_POST, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        //  YOUR RESPONSE
                        Toast.makeText(MainActivity.this,"SOS Sent",Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"Unable to Send SOS",Toast.LENGTH_SHORT).show();
                error.printStackTrace();

            }
        });
        RequestQueue mRequestQueue = Volley.newRequestQueue(this);
        mRequestQueue.add(jsonObjReq);
    }

    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    private void enable_buttons() {

        Intent i =new Intent(getApplicationContext(),Gp_service.class);
        startService(i);
        mProgressbar.setVisibility(View.VISIBLE);
        Toast.makeText(this,"Starting to Fetch Location",Toast.LENGTH_SHORT).show();
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(1);

                Intent i = new Intent(getApplicationContext(),Gp_service.class);
                stopService(i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(1);

            }
        });

    }
    private boolean runtime_permissions() {



        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

           return true;

        }
        return false;
    }



    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }
    public void enableGps(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);


// ...

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());


        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this,
                                    REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });

    }

}

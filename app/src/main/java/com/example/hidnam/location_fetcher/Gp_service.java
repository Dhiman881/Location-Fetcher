package com.example.hidnam.location_fetcher;

/**
 * Created by hidnam on 7/9/17.
 */

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import javax.net.ssl.HttpsURLConnection;


/**
 * Created by hidnam on 16/8/17.
 */

public class Gp_service extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private LocationListener listener;
    private LocationManager locationManager;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double lat, lon;
    boolean statusOfGPS;
    LocationManager manager;
    private Location mLastLocation;
    //private static Location mLastlocation=null;
    private String city ;
    private String state;
    private String country ;
    private Context context;
    private Geocoder gCoder;
    private String addressDetails;
    boolean isNetAvail=false;
    private static final String URL_POST = "https://www.Doorhopper.in/JUNK";
    private Handler mHandler;
    private Runnable runnableCode;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent,  int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void onCreate() {
        context =Gp_service.this;
        buildGoogleApiClient();
        addressDetails = new String("");
        gCoder = new Geocoder(Gp_service.this);

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
           mHandler = new Handler();
            runnableCode = new Runnable() {
                @Override
                public void run() {

                    manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                    if (activeNetwork != null && activeNetwork.isConnected()) {
                        // notify user you are online
                        isNetAvail=true;
                    } else {
                        // notify user you are not online
                        isNetAvail=false;
                    }
                    if(!statusOfGPS){
                        Intent i = new Intent("location_update");
                        i.putExtra("enable gps",1);
                        sendBroadcast(i);
                        sendAlertNotification();
                    }
                    else if(statusOfGPS && isNetAvail) {
                        Toast.makeText(Gp_service.this,"Starting to Send Data",Toast.LENGTH_SHORT).show();
                        startSendindData(); // Volley Request
                    }
                    // Repeat this the same runnable code block again another 10 seconds
                    mHandler.postDelayed(runnableCode, 10000);
                }
            };
// Start the initial runnable task by posting through the handler
            mHandler.post(runnableCode);



    }

    private void startSendindData() {
        JSONObject locObj = new JSONObject();
        try {
            // user_id, comment_id,status
            locObj.put("latitude", String.valueOf(lat));
            locObj.put("longitude", String.valueOf(lon));
            locObj.put("address",addressDetails);

        } catch (Exception e) {
            e.printStackTrace();
        }

        final String value_String =locObj.toString();
       // Log.v("Location Value",value_String);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_POST, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.v("Response",response);
                Toast.makeText(Gp_service.this,"Data Sent",Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Gp_service.this,"Unable to Send Data",Toast.LENGTH_SHORT).show();
            }
        }){
            protected Map<String,String> getParams() throws AuthFailureError{
                Map<String,String> params = new HashMap<String, String>();
                params.put("token","YOBHATT");
                params.put("data","location");
                params.put("value",value_String);
               // Log.v("Location Value_Param",params.get("value").toString());
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(7000); // Update location every second
        mLocationRequest.setFastestInterval(5000);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            lat = mLastLocation.getLatitude();
            lon = mLastLocation.getLongitude();
            sendAddress();
          //  Toast.makeText(context, String.valueOf(lat)+" "+String.valueOf(lon)+"OnConected",Toast.LENGTH_SHORT).show();

        }

    }

    synchronized void buildGoogleApiClient() {


        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }



    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"Conn Suspended",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Toast.makeText(this,"GPs Disabled",Toast.LENGTH_SHORT).show();
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!statusOfGPS){
            Intent i = new Intent("location_update");
            i.putExtra("enable gps",1);
            sendBroadcast(i);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {


            lat = location.getLatitude();
            lon = location.getLongitude();
            updateLocationNotification(location);
            sendAddress();
           // Toast.makeText(context,String.valueOf(lat)+" "+String.valueOf(lon),Toast.LENGTH_LONG).show();

        }


    }




    public void sendAddress(){
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            // notify user you are online
            isNetAvail=true;
        } else {
            // notify user you are not online
            isNetAvail=false;
        }
        Intent i = new Intent("location_update");

        List<Address> addresses = null;
        try {
                if(isNetAvail)
                    addresses = gCoder.getFromLocation(lat, lon, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {
            addressDetails =addresses.get(0).getAddressLine(0)+","+addresses.get(0).getSubLocality()+","+addresses.get(0).getLocality()+","+ addresses.get(0).getPostalCode() +","+addresses.get(0).getAdminArea()+","+addresses.get(0).getCountryName();
            i.putExtra("Address", addresses.get(0).getAddressLine(0)+","+addresses.get(0).getSubLocality()+","+addresses.get(0).getLocality()+","+ addresses.get(0).getPostalCode() +","+addresses.get(0).getAdminArea()+","+addresses.get(0).getCountryName());
            }
        i.putExtra("coordinates","Latitude : "+lat+"\n"+"Longitude : "+lon);
        i.putExtra("latitude",lat);
        i.putExtra("longitude",lon);
        sendBroadcast(i);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(runnableCode);
        mGoogleApiClient.disconnect();

    }
    private void updateLocationNotification(Location location) {

        CharSequence title = "Location";
        CharSequence message = "Latitude - "+String.valueOf(location.getLatitude())+"\n"+"Longitude - "+String.valueOf(location.getLongitude());

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(false)
                .setContentTitle(title);


        mBuilder.setContentText(message);
        mBuilder.setTicker(message);
        mBuilder.setWhen(System.currentTimeMillis());

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Intent resultIntent = new Intent(this, MainActivity.class);
        //resultIntent.putExtra("Key",new Details());
        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // stackBuilder.addParentStack(MainActivity.class);
        //stackBuilder.addNextIntent(resultIntent);
        // PendingIntent resultPendingIntent =
        //  stackBuilder.getPendingIntent(
        //         0,
        //         PendingIntent.FLAG_UPDATE_CURRENT
        //   );


        // mBuilder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, mBuilder.build());
    }

    private void sendAlertNotification() {

        CharSequence title = "GPS DISABLED";
        CharSequence message = "ENABLE GPS PLEASE";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentTitle(title);

        mBuilder.setContentText(message);
        mBuilder.setTicker(message);
        mBuilder.setWhen(System.currentTimeMillis());

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(this, MainActivity.class);
        //resultIntent.putExtra("Key",new Details());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        mBuilder.setContentIntent(resultPendingIntent);
        notificationManager.notify(2, mBuilder.build());

    }
}

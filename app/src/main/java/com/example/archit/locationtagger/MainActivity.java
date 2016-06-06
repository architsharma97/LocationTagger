package com.example.archit.locationtagger;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*Created by Archit Sharma
 *Last Updated: 5/31/2016
 */

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;

    private static String locationProvider = LocationManager.GPS_PROVIDER;
    private static final int PERMISSION_LOCATION_ACCESS = 1;
    private static final int LOCATION_UPDATE_FREQUENCY = 4000; //Seconds after which location updates are requested; 0 implies asap
    private static final int LOCATION_UPDATE_DISTANCE = 0; //Distance after which location updates are registered; 0 implies asap
    private static final String TAG="MainActivity";
    public static final String PROGRESS_ACTION="com.example.archit.locationtagger.MainActivity";

    Button send;
    ToggleButton car, walk, bike,bus,complete;
    String dateToday;
    EditText editText;
    ProgressDialog progressDialog;

    public void toggleButtons(ToggleButton button){
        car.setChecked(false);car.setActivated(false);
        walk.setChecked(false);walk.setActivated(false);
        bike.setChecked(false);bike.setActivated(false);
        bus.setChecked(false);bus.setActivated(false);
        complete.setChecked(false);complete.setActivated(false);

        button.setChecked(true);button.setActivated(true);
    }

    public String toggleButtonsState(){
        if(car.isActivated()) return car.getTextOn().toString();
        if(walk.isActivated()) return walk.getTextOn().toString();
        if(bike.isActivated()) return bike.getTextOn().toString();
        if(bus.isActivated()) return bus.getTextOn().toString();
        return complete.getTextOn().toString();
    }

    private boolean usernameValid(String username){
        return (!username.matches("") && username.matches(username.toLowerCase()));
    }

    private class ResponseReceiver extends BroadcastReceiver{

        private ResponseReceiver(){}

        @Override
        public void onReceive(Context context, Intent intent) {
            int[] responseCodes=intent.getIntArrayExtra("Files_Status");
            switch(responseCodes[0]){
                case 1:
                    progressDialog.setMessage("New File Detected: Uploading");
                    break;
                case 2:
                    progressDialog.setMessage("File Uploaded Successfully");
                    break;
                case 3:
                    progressDialog.setMessage("Failed to upload file");
                    break;
                case 4:
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Files uploaded: "+responseCodes[1]+" Files failed: "+responseCodes[2],Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences=MainActivity.this.getPreferences(Context.MODE_PRIVATE);

        car=(ToggleButton)findViewById(R.id.toggleCar);
        walk=(ToggleButton)findViewById(R.id.toggleWalk);
        bike=(ToggleButton)findViewById(R.id.toggleBike);
        bus=(ToggleButton)findViewById(R.id.toggleBus);
        complete=(ToggleButton)findViewById(R.id.toggleComplete);
        editText=(EditText)findViewById(R.id.userID);
        send=(Button)findViewById(R.id.send);

        if(!sharedPreferences.getString("userID","").matches("")) {
            editText.setText(sharedPreferences.getString("userID", ""));
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        //default state is complete
        toggleButtons(complete);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //device id
        String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //creating a file
        Calendar currentDate= Calendar.getInstance();
        SimpleDateFormat formatter= new SimpleDateFormat("MM-dd-yyyy");
        dateToday = formatter.format(currentDate.getTime());


        final File locationTagsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Location_Tags_Folder");
        locationTagsDir.mkdirs();
        final File file = new File(locationTagsDir,"Location_tags_" + deviceID + "_"+ dateToday + ".txt");

        ResponseReceiver responseReceiver = new ResponseReceiver();
        IntentFilter intentFilter =new IntentFilter(PROGRESS_ACTION);

        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, intentFilter);

        final LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                Log.v("Status","Inside onLocationChanged");
                String latitude = Double.toString(location.getLatitude());
                String longitude = Double.toString(location.getLongitude());
                String altitude = Double.toString(location.getAltitude());

                //time to standard format
                Date date=new Date(location.getTime());
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.US);
                String time=simpleDateFormat.format(date);

                Log.i("Location Entry","UserID: "+ editText.getText().toString() + "\n" + "Mode: "+toggleButtonsState()+"\nLocation at " + time + "\nLatitude: " + latitude + "\nLongitude: " + longitude + "\nAltitude: " + altitude);

                try {
                    FileOutputStream outputStream=new FileOutputStream(file,true);
                    PrintWriter printWriter = new PrintWriter(outputStream);

                    //userID, Mode, time (MM-dd-yyyy HH:mm:ss), Latitude, Longitude, Altitude
                    printWriter.append((editText.getText().toString()+"," + toggleButtonsState() + "," + time + "," + latitude + "," + longitude + "," + altitude + "\n"));
                    printWriter.flush();
                    printWriter.close();
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                switch(status){
                    case 0:
                        Toast.makeText(getApplicationContext(),"Provider Out of Service",Toast.LENGTH_LONG).show();
                    case 1:
                        Toast.makeText(getApplicationContext(),"Provider Temporarily Unavailable",Toast.LENGTH_LONG).show();
                    case 2:
                        Toast.makeText(getApplicationContext(),"Provider Available",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(getApplicationContext(),"Please enable Location/GPS",Toast.LENGTH_LONG).show();


                /*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getApplicationContext());
                alertDialogBuilder.setMessage("Please enable GPS for this application to collect location tags").setCancelable(true).
                        setPositiveButton("Enable GPS",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent invokeGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(invokeGPSSettingIntent);
                                    }
                                });
                AlertDialog alert = alertDialogBuilder.create();
                alert.show();*/
            }
        };

        car.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    Log.v("Permission_status","Permission not granted");
                    Toast.makeText(getApplicationContext(), "Please accept the request to access location data!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_LOCATION_ACCESS);
                    Log.v("Permission_status","User shown popup for location status");
                }
                Log.v("Status","Starting location data collection: CAR");
                if(usernameValid(editText.getText().toString())) {
                    if (complete.isActivated())
                        locationManager.requestLocationUpdates(locationProvider, LOCATION_UPDATE_FREQUENCY, LOCATION_UPDATE_DISTANCE, locationListener);
                    toggleButtons(car);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please enter a valid username", Toast.LENGTH_SHORT).show();
                    toggleButtons(complete);
                }
            }

        });

        bus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    Log.v("Permission_status","Permission not granted");
                    Toast.makeText(getApplicationContext(), "Please accept the request to access location data!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_LOCATION_ACCESS);
                    Log.v("Permission_status","User shown popup for location status");
                }
                Log.v("Status","Starting location data collection: BUS");
                if(usernameValid(editText.getText().toString())) {
                    if (complete.isActivated())
                        locationManager.requestLocationUpdates(locationProvider, LOCATION_UPDATE_FREQUENCY, LOCATION_UPDATE_DISTANCE, locationListener);
                    toggleButtons(bus);
                }
                else {
                    toggleButtons(complete);
                    Toast.makeText(getApplicationContext(), "Please enter a valid username", Toast.LENGTH_SHORT).show();
                }
            }

        });

        bike.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.v("Permission_status", "Permission not granted");
                    Toast.makeText(getApplicationContext(), "Please accept the request to access location data!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_ACCESS);
                    Log.v("Permission_status", "User shown popup for location status");
                }
                Log.v("Status", "Starting location data collection: BIKE");
                if (usernameValid(editText.getText().toString())) {
                    if (complete.isActivated())
                        locationManager.requestLocationUpdates(locationProvider, LOCATION_UPDATE_FREQUENCY, LOCATION_UPDATE_DISTANCE, locationListener);
                    toggleButtons(bike);
                } else {
                    toggleButtons(complete);
                    Toast.makeText(getApplicationContext(), "Please enter a valid username", Toast.LENGTH_SHORT).show();
                }
            }
        });

        walk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.v("Permission_status", "Permission not granted");
                    Toast.makeText(getApplicationContext(), "Please accept the request to access location data!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_ACCESS);
                    Log.v("Permission_status", "User shown popup for location status");
                }
                Log.v("Status", "Starting location data collection: WALK");
                if (usernameValid(editText.getText().toString())) {
                    if (complete.isActivated())
                        locationManager.requestLocationUpdates(locationProvider, LOCATION_UPDATE_FREQUENCY, LOCATION_UPDATE_DISTANCE, locationListener);
                    toggleButtons(walk);
                }
                else {
                    toggleButtons(complete);
                    Toast.makeText(getApplicationContext(), "Please enter a valid username", Toast.LENGTH_SHORT).show();
                }
            }

        });

        complete.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.v("Status","Complete Pressed: Stopping updates");
                if(!complete.isActivated()) locationManager.removeUpdates(locationListener);
                toggleButtons(complete);
            }

        });

        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiManager wifiMgr = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE);
                if(wifiMgr.isWifiEnabled()) {
                    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                    String ssid = wifiInfo.getSSID();
                    Log.d(TAG, ssid);
                    if (ssid.equals("\"tamulink-wpa\"")) {
                        Log.d(TAG, "Connected to tamulink-wpa");
                        Intent intent = new Intent(MainActivity.this, LocationFilesSendService.class);
                        MainActivity.this.startService(intent);
                        Log.d(TAG, "Location data being sent");
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Sending location data");
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                    }
                    else
                        Toast.makeText(getApplicationContext(),"Please connect to tamulink-wpa to send location data",Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(getApplicationContext(),"Please connect to tamulink-wpa to send location data",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPref=MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPref.edit();
        editor.putString("toggledButton",toggleButtonsState());
        editor.putString("userID",editText.getText().toString());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref=MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        String toggledButton=sharedPref.getString("toggledButton","not_exist");
        if(!toggledButton.equals("not_exist")) {
            if(toggledButton.equals("walk")) toggleButtons(walk);
            else if(toggledButton.equals("car")) toggleButtons(car);
            else if(toggledButton.equals("bus"))toggleButtons(bus);
            else if(toggledButton.equals("bike"))toggleButtons(bike);
            else if(toggledButton.equals("complete")) toggleButtons(complete);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences=MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("userID",editText.getText().toString());
        editor.apply();
    }
}
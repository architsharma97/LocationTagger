package com.example.archit.locationtagger;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class LocationFilesSendService extends IntentService {
    protected static final int USER_LOCATION = 101;
    protected static final String TAG = "LocationFileSendService";
    protected static final String folder = "Location_Tags_Folder";

    private final static String SERVER_IP_ADDRESS = "165.91.209.238";
    private final static String SERVER_PORT_NUMBER = "80";
    private final static String SERVER_PROJECT_NAME = "/SmartCommuter/";
    private final static String SERVER_BASE_URL = "http://" + SERVER_IP_ADDRESS
            + ":" + SERVER_PORT_NUMBER + SERVER_PROJECT_NAME;
    private final static String PAGE_URL = "index.php";
    private final static String FINAL_SERVER_URL = SERVER_BASE_URL + PAGE_URL;
    public static final String PROGRESS_ACTION="com.example.archit.locationtagger.MainActivity";
    LocalBroadcastManager localBroadcastManager;

    public LocationFilesSendService() {
        super("LocationFileSendService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int response=sendLocationFilesToDB();
        Intent returnIntent = new Intent(PROGRESS_ACTION);
        returnIntent.putExtra("Files_Status",response);
        localBroadcastManager=LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(returnIntent);
    }

    private int sendLocationFilesToDB() {
        Log.d(TAG, "Sending Location");

        File folderDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder);
        File fileToOpen;
        Log.d(TAG, "Directory Found");

        String[] files = folderDir.list();
        String[] splitted;
        String xmlUserData, mode, line, userID, latitude, longitude, timestamp, altitude;
        int flag=0;

        Boolean fileDeleted=true, shouldFileBeDeleted=true;

        BufferedReader bufferedReader;
        for (String file : files) {
            fileToOpen = new File(folderDir, file);
            shouldFileBeDeleted = true;
            fileDeleted = false;
            try {
                bufferedReader = new BufferedReader(new FileReader(fileToOpen));
                Log.d(TAG, "File Found" + fileToOpen);
                while ((line = bufferedReader.readLine()) != null) {
                    Log.d(TAG, line);
                    splitted = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    userID = splitted[0];
                    mode = splitted[1];
                    timestamp = splitted[2];
                    latitude = splitted[3];
                    longitude = splitted[4];
                    altitude = splitted[5];

                    xmlUserData = "<request>" + "<operation id=\"1\">"
                            + "<opcode>" + Integer.toString(USER_LOCATION)
                            + "</opcode>" + "<userid>" + userID
                            + "</userid>" + "<latitude>" + latitude
                            + "</latitude>" + "<longitude>" + longitude
                            + "</longitude>" + "<date>" + timestamp
                            + "</date>" + "</operation>" + "</request>";
                    sendLocationEntryToDB(xmlUserData);
                }
                bufferedReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "File not found");
                shouldFileBeDeleted = false;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
                shouldFileBeDeleted = false;
            }

            /*if(shouldFileBeDeleted) fileDeleted = fileToOpen.delete();
            if (fileDeleted) Log.d(TAG, "File Deleted");
            else Log.d(TAG, "File not deleted");*/
        }
        
        //TODO: Delete this statement before packaging
        fileDeleted=true;

        Log.d(TAG, "All Files sent");
        if(!shouldFileBeDeleted) flag=-1;
        if(shouldFileBeDeleted && !fileDeleted) flag=-2;
        return flag;
    }

    private void sendLocationEntryToDB(String userDataEntry) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(FINAL_SERVER_URL);

        List<NameValuePair> parameterList = new ArrayList<NameValuePair>();
        parameterList.add(new BasicNameValuePair("requestParam",userDataEntry));
		//Log.i(TAG, userDataEntry);

        try {
            httppost.setEntity(new UrlEncodedFormEntity(parameterList));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }

        try {
            Log.d(TAG, "Sending request");
            HttpResponse httpresponse = httpclient.execute(httppost);
            InputStream inputStream = httpresponse.getEntity().getContent();
            InputStreamReader inputStreamReader = new InputStreamReader(
                    inputStream);
            BufferedReader bufferedReader = new BufferedReader(
                    inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String bufferedStrChunk = null;
            while ((bufferedStrChunk = bufferedReader.readLine()) != null) {
                stringBuilder.append(bufferedStrChunk);
            }
            Log.d(TAG, stringBuilder.toString());


        } catch (ClientProtocolException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}

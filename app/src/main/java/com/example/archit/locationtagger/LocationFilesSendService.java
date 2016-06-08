package com.example.archit.locationtagger;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

    String upLoadServerUri = "http://165.91.209.238:80/SmartCommuter/index1.php";
    int serverResponseCode=0;
    int countSuccess,countFails;

    LocalBroadcastManager localBroadcastManager;
    Intent returnIntent;

    public LocationFilesSendService() {
        super("LocationFileSendService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        localBroadcastManager=LocalBroadcastManager.getInstance(this);
        sendLocationFilesToDB();
        returnIntent= new Intent(PROGRESS_ACTION);
        int[] stats =new int[3];
        stats[0]=4;
        stats[1]=countSuccess;
        stats[2]=countFails;
        returnIntent.putExtra("Files_Status",stats);
        localBroadcastManager.sendBroadcast(returnIntent);
    }

    private void sendLocationFilesToDB() {
        Log.d(TAG, "Sending Location");

        File folderDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder);
        File fileToOpen;
        Log.d(TAG, "Directory Found");

        String[] files = folderDir.list();
        String[] splitted;
        String xmlUserData, mode, line, userID, latitude, longitude, timestamp, altitude;

        int flag=0,response;

        int[] dataAsArray= new int[1];

        countSuccess=0;countFails=0;

        Boolean fileDeleted=true, shouldFileBeDeleted=true;
        BufferedReader bufferedReader;

        for (String file : files) {
            fileToOpen = new File(folderDir, file);
            shouldFileBeDeleted = false;
            fileDeleted = false;

            dataAsArray[0]=1;
            returnIntent =new Intent(PROGRESS_ACTION);
            returnIntent.putExtra("Files_Status",dataAsArray);
            localBroadcastManager.sendBroadcast(returnIntent);

            //The whole file is sent
            response=uploadFileToServer(fileToOpen,fileToOpen.getAbsolutePath());

            if(response==200){
                shouldFileBeDeleted=true;
                returnIntent = new Intent(PROGRESS_ACTION);
                dataAsArray[0]=2;
                returnIntent.putExtra("Files_Status",dataAsArray);
                localBroadcastManager.sendBroadcast(returnIntent);
                countSuccess++;
            }
            else{
                countFails++;
                returnIntent = new Intent(PROGRESS_ACTION);
                dataAsArray[0]=3;
                returnIntent.putExtra("Files_Status",dataAsArray);
                localBroadcastManager.sendBroadcast(returnIntent);
            }

            /*For each file, one line sent at a time
             *try {
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
            }*/

            /*if(shouldFileBeDeleted) fileDeleted = fileToOpen.delete();
            if (fileDeleted) Log.d(TAG, "File Deleted");
            else Log.d(TAG, "File not deleted");*/
        }
    }

    private int uploadFileToServer(File sourceFile, String sourcePath) {
        if(sourceFile.isFile()){
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;

            try {
                Log.e(TAG, "sourceFile.isFile() : " + sourceFile.isFile());

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourcePath);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;filename='" + sourcePath + "'" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {
                    Log.i("uploadFile","File uploaded");
                }

                //close the streams
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                Log.e(TAG,"Exception:" + e.getMessage());
            }
            return serverResponseCode;
        }
        else{
            Log.e(TAG,"Invalid File Detected");
            return -1;
        }
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

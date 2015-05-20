package com.example.mjmayank.helloworld;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    TextView Xpoint, Ypoint, Zpoint;
    OutputStreamWriter firstFileOSW;
    OutputStreamWriter secondFileOSW;
    OutputStreamWriter wifiFileOSW;
    Double[][] xyzvals;
    Double xOne, yOne, zOne;
    Double xTwo, yTwo, zTwo;
    Double xThree, yThree, zThree;
    long timeOne, timeTwo, timeThree;
    Double xSlope, ySlope, zSlope, xMax, yMax, zMax, xMin, yMin, zMin, xDiff, yDiff, zDiff;
    int counter = 0;
    ArrayList<Double[]> trainedData;
    WifiManager wifiManager;
    WifiReceiver wifiReceiver;
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();
    private static final int NUM_CELLS = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int intervalGroupSize = 3;
        xyzvals = new Double[3][intervalGroupSize];

/*
          try //create the file and open a stream writer to it
          {
              firstFileOSW = new OutputStreamWriter(openFileOutput("logger.txt", Context.MODE_PRIVATE));
              secondFileOSW = new OutputStreamWriter(openFileOutput("calculations.txt", Context.MODE_PRIVATE));

          }
          catch (FileNotFoundException e)
          {
              Log.e("Writing Failure", "Can not open stream writer: " + e.toString());
          }
          */

        try {
            File firstFile = new File("/sdcard/firstFile.txt");
            firstFile.createNewFile();
            File secondFile = new File("/sdcard/secondFile.txt");
            secondFile.createNewFile();
            FileOutputStream fOutOne = new FileOutputStream(firstFile);
            FileOutputStream fOutTwo = new FileOutputStream(secondFile);
            firstFileOSW = new OutputStreamWriter(fOutOne);
            secondFileOSW = new OutputStreamWriter(fOutTwo);
            Toast.makeText(getBaseContext(), "Done writing SD 'fileOne.txt'", Toast.LENGTH_SHORT).show();
            Toast.makeText(getBaseContext(), "Done writing SD 'secondFile.txt'", Toast.LENGTH_SHORT).show();

            File wifiFile = new File("/sdcard/wifiFile.txt");
            wifiFile.createNewFile();
            FileOutputStream fOutWifi = new FileOutputStream(wifiFile);
            wifiFileOSW = new OutputStreamWriter(fOutWifi);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        int numberOfLevels = 100;
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled() == false) {
            wifiManager.setWifiEnabled(true);
        }
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//        wifiManager.startScan();
//        worker.schedule(new Runnable() {
//            @Override
//            public void run() { wifiManager.startScan(); }
//        }, 5, TimeUnit.SECONDS);

//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
//        Toast.makeText(getBaseContext(), level, Toast.LENGTH_SHORT).show();

        trainedData = readFile("trained_data.txt");
//        Toast.makeText(getBaseContext(), filestuff, Toast.LENGTH_SHORT).show();

        Button stop = (Button) findViewById(R.id.stopButton); //Pause your logging
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPause();
            }
        });

        Button done = (Button) findViewById(R.id.doneButton); //Finish your logging
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onFinish();
            }
        });

        Button start = (Button) findViewById(R.id.startButton); //Start your logging
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onResume();
            }
        });

        Button wifi = (Button) findViewById(R.id.wifiButton);
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiManager.startScan();
            }
        });

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Sensor Management
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        Xpoint = (TextView) findViewById(R.id.xCoord); //Change values of textViews
        Ypoint = (TextView) findViewById(R.id.yCoord);
        Zpoint = (TextView) findViewById(R.id.zCoord);

    }

    public void calculatePosition(List<ScanResult> results){
//        ArrayList<Integer> probabilities = new ArrayList<Integer>();
//        for(int i = 0; i<NUM_CELLS; i++){
//            int prob_a_given_b = data[i][results[k].value];
//            int prob_a = NUM_CELLS;
//            int prob_b = 255;
//            probabilities.set(i, prob_a_given_b * prob_a / prob_b);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        int k = 21;
//        Log.d("Test", Arrays.toString(event.values)); //Displaying Values as they change
        Xpoint.setText("X-Coordinate: " + event.values[0]); //Setting textView values to sensor values
        Ypoint.setText("Y-Coordinate: " + event.values[1]);
        Zpoint.setText("Z-Coordinate: " + event.values[2]);
        long time = System.nanoTime();
        try //Write values to the file
        {
            firstFileOSW.write(time + ", " + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + "\n");
        }
        catch (IOException e) {
            Log.e("Writing Failure", "File 1 write failed: " + e.toString());
        }

        if(counter == 0) //assigning first row of xyz **happens once
        {
            timeOne = time;
            xOne = (double)event.values[0];
            yOne = (double)event.values[1];
            zOne = (double)event.values[2];
        }
        else if(counter == 1) //assigning second row of xyz **happens once
        {
            timeTwo = time;
            xTwo = (double)event.values[0];
            yTwo = (double)event.values[1];
            zTwo = (double)event.values[2];
        }
        else if(counter == 2) //assigning third row of xyz **happens once
        {
            timeThree = time;
            xThree = (double)event.values[0];
            yThree = (double)event.values[1];
            zThree = (double)event.values[2];
        }

        if (counter >= 2) //if we have at least 3 lines to analyze to new file
        {
            if(counter > 2) //need to rearrange what 3 lines we are looking at
            {
                xOne = xTwo;
                yOne = yTwo;
                zOne = zTwo;
                timeOne = timeTwo;
                xTwo = xThree;
                yTwo = yThree;
                zTwo = zThree;
                timeTwo = timeThree;
                xThree = (double)event.values[0];
                yThree = (double)event.values[1];
                zThree = (double)event.values[2];
                timeThree = time;
            }
            xSlope = ((xOne - xThree)/(timeThree - timeOne)); //calculate everything
            ySlope = ((yOne - yThree)/(timeThree - timeOne));
            zSlope = ((zOne - zThree)/(timeThree - timeOne));
            xMax = Math.max(Math.max(xOne, xTwo), xThree);
            yMax = Math.max(Math.max(yOne, yTwo), yThree);
            zMax = Math.max(Math.max(zOne, zTwo), zThree);
            xMin = Math.min(Math.min(xOne, xTwo), xThree);
            yMin = Math.min(Math.min(yOne, yTwo), yThree);
            zMin = Math.min(Math.min(zOne, zTwo), zThree);
            xDiff = Math.abs(xMax - xMin);
            yDiff = Math.abs(yMax - yMin);
            zDiff = Math.abs(zMax - zMin);
            try
            {
                secondFileOSW.write(xSlope + ", " + ySlope + ", " + zSlope + ", " + xMax + ", " + yMax + ", " + zMax  + ", " + xMin + ", " + yMin + ", " + zMin  + ", " + xDiff + ", " + yDiff + ", " + zDiff + "\n");
                Double[] dataPoint = {xSlope, ySlope, zSlope, xDiff, yDiff, zDiff};
                DataPoint[] queue = compareToData(dataPoint);
//                Log.d(TAG, Arrays.toString(queue));
                int numVal = 0;
                for(int i = 0; i<k; i++){
                    numVal += queue[i].value;
                }
                if(numVal > k/2.0){
                    //majority value is 1
//                    Toast.makeText(getBaseContext(), "Moving around " + numVal, Toast.LENGTH_SHORT).show();
                }
                else{
                    //majority value is 0
//                    Toast.makeText(getBaseContext(), "Standing Still"  + numVal, Toast.LENGTH_SHORT).show();
                }
            }
            catch (IOException e) {
                Log.e("Writing Failure", "File 2 write failed: " + e.toString());
            }
        }
//
//        if(counter <= 2) //assigning first row of xyz **happens once
//        {
//            timeOne = time;
//            xyzvals[counter][0] = (double)event.values[0];
//            xyzvals[counter][1] = (double)event.values[1];
//            xyzvals[counter][2] = (double)event.values[2];
//        }
        counter ++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {}

    protected void onPause() //When we pause the sensor logging
    {
        super.onPause();
        try
        {
            firstFileOSW.flush(); //need to flush stream before displaying
            secondFileOSW.flush();
            wifiFileOSW.flush();
            //Log.d("First File", readFile("firstFile.txt"));
            //Log.d("Second File", readFile("secondFile.txt"));
        }
        catch (IOException e)
        {
            Log.e("Pause Failure", "Didn't Successfully Pause: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }

    private ArrayList<Double[]> readFile(String fileName) {
        String ret = ""; //start with blank file
        ArrayList<Double[]> finalData = new ArrayList<Double[]>();
        try
        {
            Context context = this;
            AssetManager am = context.getAssets();
            InputStream inputStream = am.open(fileName);
//            Log.e("test", "test");
//            InputStream inputStream = openFileInput(fileName); //input stream
            if (inputStream != null) //make sure the file isn't empty
            {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine())!= null) //go line by line
                {
                    String[] rowData = receiveString.split(",");
                    Double[] doubleData = new Double[rowData.length];
                    for(int i = 0; i<rowData.length; i++){
                        doubleData[i] = Double.parseDouble(rowData[i]);
                    }
                    finalData.add(doubleData);
                    stringBuilder.append(receiveString);
                    //*** Not sure how to add new line here so it's easier to read ***
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e)
        {
            Log.e("Reading Failure", "File not found: " + e.toString());
        }
        catch (IOException e)
        {
            Log.e("Reading Failure", "Can not read file: " + e.toString());
        }

        return finalData;
    }

    protected DataPoint[] compareToData(Double[] point){
//        double [][] trainingData = new double[10][10];
        PriorityQueue<DataPoint> nearestNeighbors = new PriorityQueue<DataPoint>();
        for(Double[] data : trainedData){
            DataPoint dpoint = new DataPoint(data, point);
            nearestNeighbors.add(dpoint);
        }

        return nearestNeighbors.toArray(new DataPoint[0]);
    }

    protected void onResume() { //Restart the logging, adding more sensor data to the file
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onFinish() { //Finish logging and display all the data you logged
        try
        {
            firstFileOSW.close();
            secondFileOSW.close();
            wifiFileOSW.close();
            //Log.d("First File", readFile("firstFile.txt"));
            //Log.d("Second FIle", readFile("secondFile.txt"));
        } catch (IOException e)
        {
            Log.e("Closing Failure", "Can't Close: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }

    protected void onStop(){
        super.onStop();
        unregisterReceiver(wifiReceiver);
    }

    class WifiReceiver extends BroadcastReceiver {
        // An access point scan has completed and results are sent here
        public void onReceive(Context c, Intent intent) {
// Call getScanResults() to obtain the results
            List<ScanResult> results = wifiManager.getScanResults();

            try {
                long time = System.nanoTime();
                for (int n = 0; n < results.size(); n++) {
// SSID contains name of AP and level contains RSSI
                    Log.i("Wifi", "SSID = " + results.get(n).SSID + "; RSSI = " + results.get(n).level);
                    try //Write values to the file
                    {
                        wifiFileOSW.write(time + ", " + ((TextView)findViewById(R.id.cellText)).getText() + ", " + results.get(n).SSID + ", " + results.get(n).level + "\n");
                    }
                    catch (IOException e) {
                        Log.e("Writing Failure", "File 1 write failed: " + e.toString());
                    }
                }
                Toast.makeText(getBaseContext(), "Scan Completed", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e) { }
        }
    } // End of class WifiReceiver
}

class DataPoint implements Comparable<DataPoint>{
    Double[] data;
    Double distance;
    int value;

    public DataPoint(Double[] _data, Double[] mainPoint){
        data = _data;
        value = _data[data.length-1].intValue();
        distance = calculateDistance(mainPoint);
    }

    public Double calculateDistance(Double[] otherPoint){
        Double dist = 0.0;
        for(int i=0; i<otherPoint.length; i++){
            dist += Math.pow((data[i] - otherPoint[i]), 2);
        }
        dist = Math.pow(dist, .5);
        distance = dist;
        return distance;
    }

    public int compareTo(DataPoint other){
        return Double.compare(distance, other.distance);
    }

    @Override public String toString(){
        return distance + ", " + value;
    }
}
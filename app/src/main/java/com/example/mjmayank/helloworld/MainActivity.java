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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    //TextView Xpoint, Ypoint, Zpoint;
    TextView wifiDataT;
    OutputStreamWriter firstFileOSW, calculatedValuesFileOSW, wifiFileOSW;
    ArrayList<Double> xArr, yArr, zArr;
    ArrayList<Long> timeArr;
    int counter = 0;
    ArrayList<Double[]> trainedData;
    HashMap<String, List<Integer>> wifiData;
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
        xArr = new ArrayList<Double>();
        yArr = new ArrayList<Double>();
        zArr = new ArrayList<Double>();
/*
          try //create the file and open a stream writer to it
          {
              firstFileOSW = new OutputStreamWriter(openFileOutput("logger.txt", Context.MODE_PRIVATE));
              calculatedValuesFileOSW = new OutputStreamWriter(openFileOutput("calculations.txt", Context.MODE_PRIVATE));

          }
          catch (FileNotFoundException e)
          {
              Log.e("Writing Failure", "Can not open stream writer: " + e.toString());
          }
          */

        try {
            File firstFile = new File("/sdcard/firstFile.txt");
            firstFile.createNewFile();
            File calculatedValuesFile = new File("/sdcard/calculatedValuesFile.txt");
            calculatedValuesFile.createNewFile();
            FileOutputStream fOutOne = new FileOutputStream(firstFile);
            FileOutputStream fOutTwo = new FileOutputStream(calculatedValuesFile);
            firstFileOSW = new OutputStreamWriter(fOutOne);
            calculatedValuesFileOSW = new OutputStreamWriter(fOutTwo);
            Toast.makeText(getBaseContext(), "Done writing SD 'fileOne.txt'", Toast.LENGTH_SHORT).show();
            Toast.makeText(getBaseContext(), "Done writing SD 'calculatedValuesFile.txt'", Toast.LENGTH_SHORT).show();

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

//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
//        Toast.makeText(getBaseContext(), level, Toast.LENGTH_SHORT).show();

//        trainedData = readFile("trained_data.txt");
//        wifiData = readWifiFile("wifiFile.txt");
//        Toast.makeText(getBaseContext(), filestuff, Toast.LENGTH_SHORT).show();

        //Button stop = (Button) findViewById(R.id.stopButton); //Pause your logging
        //stop.setOnClickListener(new View.OnClickListener() {
         //   public void onClick(View v) {
            //    onPause();
          //  }
        //});

//        Button done = (Button) findViewById(R.id.doneButton); //Finish your logging
//        done.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                onFinish();
//            }
//        });

//        Button start = (Button) findViewById(R.id.startButton); //Start your logging
//        start.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                onResume();
//            }
//        });

        Button wifi = (Button) findViewById(R.id.wifiButton);
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiManager.startScan();
            }
        });

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Sensor Management
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
//        Xpoint = (TextView) findViewById(R.id.xCoord); //Change values of textViews
//        Ypoint = (TextView) findViewById(R.id.yCoord);
//        Zpoint = (TextView) findViewById(R.id.zCoord);
        wifiDataT = (TextView) findViewById(R.id.wifiData);

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

    protected Map<String, double[][]> createDictionary()
    {
        Scanner input = null;
        Map<String, double[][]> map = new HashMap<String, double[][]>();

        try(BufferedReader br = new BufferedReader(new FileReader("./resources/phone_data.txt"))) {
            for(String line; (line = br.readLine()) != null; ) {
                String[] parse = line.split(",");
                if(map.get(parse[0].trim()) == null) //If the mac address doesn't exist
                {
                    double [][] info = new double[256][19];
                    map.put(parse[0].trim(), info);
                }
                if(map.get(parse[0].trim()) != null) //If the mac address does exist
                {
                    int cell = Integer.parseInt(parse[1].trim()); //for each cell put all values
                    for(int value = 2; value < 258; value ++) {
                        double[][] temp = map.get(parse[0].trim());
                        double store = Double.parseDouble(parse[value].trim());
                        temp[value - 2][cell] = store;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    protected int calculateCell(ArrayList<WifiReading> wifiReading, HashMap<String, double[][]> trainedWifiData){
        double[] probs = new double[19];
        Arrays.fill(probs, 1.0 / 17.0);
        Collections.sort(wifiReading);
        int predicted_cell = 0;
        double max_prob = 0.0;

        for(int i=10; i>0; i--){
//            int num_wifis = wifiReading.size();
//            if(i>=num_wifis){
//                for(int j=0; j<19; j++){
//                    probs[j] *= times_dict[j][num_wifis];
//                }
//            }
            if(trainedWifiData.get(wifiReading.get(i)) != null){
                double[][] mac_data = trainedWifiData.get(wifiReading.get(i).mac);
                double[] probs_for_strength = new double[19];
                for(int cell=0; cell<19; cell++){
                    probs_for_strength[cell] = mac_data[cell][wifiReading.get(i).strength];
                    probs[i] *= mac_data[cell][wifiReading.get(i).strength];
                }
                double sum = 0.0;
                for(double j : probs){
                    sum += j;
                }
                for(int j=0; j<probs.length; j++){
                    probs[j] /= sum;
                    if(probs[j] > .8){
                        return j;
                    }
                    if(probs[j] > max_prob){
                        predicted_cell = j;
                    }
                }
            }
        }
        return predicted_cell;
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
        int k = 3;
        Double xSlope, ySlope, zSlope, xMax, yMax, zMax, xMin, yMin, zMin, xDiff, yDiff, zDiff;
//        Log.d("Test", Arrays.toString(event.values)); //Displaying Values as they change
//        Xpoint.setText("X-Coordinate: " + event.values[0]); //Setting textView values to sensor values
//        Ypoint.setText("Y-Coordinate: " + event.values[1]);
//        Zpoint.setText("Z-Coordinate: " + event.values[2]);
        long time = System.nanoTime();
        try //Write values to the file
        {
            firstFileOSW.write(time + ", " + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + "\n");
        }
        catch (IOException e) {
            Log.e("Writing Failure", "File 1 write failed: " + e.toString());
        }

        xArr.add((double)event.values[0]);
        yArr.add((double) event.values[1]);
        zArr.add((double) event.values[2]);
        timeArr.add(time);

        List<Double> xTemp = xArr.subList(counter - k, counter);
        List<Double> yTemp = yArr.subList(counter-k, counter);
        List<Double> zTemp = zArr.subList(counter-k, counter);
        List<Long> timeTemp = timeArr.subList(counter-k, counter);

        if (counter >= 2) //if we have at least 3 lines to analyze to new file
        {
            xSlope = (xTemp.get(k-1) - xTemp.get(0))/(timeTemp.get(k-1) - timeTemp.get(0)); //calculate everything
            ySlope = (yTemp.get(k-1) - yTemp.get(0))/(timeTemp.get(k-1) - timeTemp.get(0));
            zSlope = (zTemp.get(k-1) - zTemp.get(0))/(timeTemp.get(k-1) - timeTemp.get(0));
            xMax = Collections.max(xTemp);
            yMax = Collections.max(yTemp);
            zMax = Collections.max(zTemp);
            xMin = Collections.min(xTemp);
            yMin = Collections.min(yTemp);
            zMin = Collections.min(zTemp);
            xDiff = Math.abs(xMax - xMin);
            yDiff = Math.abs(yMax - yMin);
            zDiff = Math.abs(zMax - zMin);
            try
            {
                calculatedValuesFileOSW.write(xSlope + ", " + ySlope + ", " + zSlope + ", " + xMax + ", " + yMax + ", " + zMax  + ", " + xMin + ", " + yMin + ", " + zMin  + ", " + xDiff + ", " + yDiff + ", " + zDiff + "\n");
                Double[] dataPoint = {xSlope, ySlope, zSlope, xDiff, yDiff, zDiff};

                boolean walking = compareToData(dataPoint);
//                Log.d(TAG, Arrays.toString(queue));
                if(walking){
                    //majority value is 1
                    Toast.makeText(getBaseContext(), "Moving around " + numVal, Toast.LENGTH_SHORT).show();
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
            calculatedValuesFileOSW.flush();
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

    private HashMap<String, List<Integer>> readWifiFile(String fileName) {
        String ret = ""; //start with blank file
        HashMap<String, List<Integer>> finalData = new HashMap<String, List<Integer>>();
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
                    if(finalData.get(rowData[1]) != null){
                        finalData.get(rowData[1]).add(Integer.parseInt(rowData[2]));
                    }
                    else{
                        ArrayList<Integer> list = new ArrayList<Integer>();
                        list.add(Integer.parseInt(rowData[2]));
                        finalData.put(rowData[1], list);
                    }
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

    protected boolean compareToData(Double[] point, int k){
//        double [][] trainingData = new double[10][10];
        PriorityQueue<DataPoint> nearestNeighbors = new PriorityQueue<DataPoint>();
        for(Double[] data : trainedData){
            DataPoint dpoint = new DataPoint(data, point);
            nearestNeighbors.add(dpoint);
        }

        return nearestNeighbors.toArray(new DataPoint[0]);

        int numVal = 0;
        for(int i = 0; i<k; i++){
            numVal += queue[i].value;
        }
        return numVal > k/2.0
    }

    protected void onResume() { //Restart the logging, adding more sensor data to the file
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onFinish() { //Finish logging and display all the data you logged
        try
        {
            firstFileOSW.close();
            calculatedValuesFileOSW.close();
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
                wifiDataT.setText("");
                for (int n = 0; n < results.size(); n++) {
// SSID contains name of AP and level contains RSSI
                    try //Write values to the file
                    {
                        String str = time + ", " + ((TextView)findViewById(R.id.cellText)).getText() + ", " + results.get(n).SSID + ", " + results.get(n).BSSID + ", " + results.get(n).level + "\n";
                        String temp = wifiDataT.getText().toString();
                        Log.d("Wifi", str);
                        wifiDataT.setText(temp + str);
                        wifiFileOSW.write(str);
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

class WifiReading implements Comparable<WifiReading>{
    String mac;
    int strength;

    public WifiReading(String _mac, int _strength){
        mac = _mac;
        strength = _strength;
    }

    public int compareTo(WifiReading other){
        return strength - other.strength;
    }
}
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
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    //TextView Xpoint, Ypoint, Zpoint;
    int still = 0;
    int walking = 0;
    int cont = 0;
    float last_x = 0;
    float last_y = 0;
    float last_z = 0;
    float curr_x = 0;
    float curr_y = 0;
    float curr_z = 0;
    TextView wifiDataT, prob, belief, queueIt;
    int count = 0;
    OutputStreamWriter accelerometerFileOSW, calculatedValuesFileOSW, wifiFileOSW, confMatrixFileOSW;
    ArrayList<Double> xArr, yArr, zArr;
    ArrayList<Long> timeArr;
    int counter = 0;
    ArrayList<Double[]> trainedQueueData;
    ArrayList<double[]> numWifiData;
    WifiManager wifiManager;
    WifiReceiver wifiReceiver;
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();
    private static final int NUM_CELLS = 19;
    HashMap<String, double[][]> globalTrainedWifiData;
    HashMap<String, ArrayList<Integer>> currSessionWifiData;
    int[] currSessionPredictions;
    long endedLine, startedLine;
    LinkedList<Boolean> lastQueue;

    boolean stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int intervalGroupSize = 3;
        prob = (TextView)findViewById(R.id.probability);
        queueIt = (TextView) findViewById(R.id.queueStatus);
        xArr = new ArrayList<Double>();
        yArr = new ArrayList<Double>();
        zArr = new ArrayList<Double>();
        timeArr = new ArrayList<Long>();
        lastQueue = new LinkedList<>();
//        probabilities = new double[19];
        resetProbabilities();

        try {
            File firstFile = new File("/sdcard/accelerometerData.txt");
            firstFile.createNewFile();
            FileOutputStream fOutOne = new FileOutputStream(firstFile);
            accelerometerFileOSW = new OutputStreamWriter(fOutOne);

            File calculatedValuesFile = new File("/sdcard/calculatedValuesFile.txt");
            calculatedValuesFile.createNewFile();
            FileOutputStream fOutTwo = new FileOutputStream(calculatedValuesFile);
            calculatedValuesFileOSW = new OutputStreamWriter(fOutTwo);

            File wifiFile = new File("/sdcard/wifiFile.txt");
            wifiFile.createNewFile();
            FileOutputStream fOutWifi = new FileOutputStream(wifiFile);
            wifiFileOSW = new OutputStreamWriter(fOutWifi);

            File matrixFile = new File("/sdcard/confMatrix.txt");
            matrixFile.createNewFile();
            FileOutputStream fOutMatrix = new FileOutputStream(matrixFile);
            confMatrixFileOSW = new OutputStreamWriter(fOutMatrix);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled() == false) {
            wifiManager.setWifiEnabled(true);
        }
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Button wifi = (Button) findViewById(R.id.locateMe);
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                globalTrainedWifiData = readWifiData();
                numWifiData = readNumWifiData();
                prob.setText("Locating you...");
                stop = false;
                resetProbabilities();
                wifiDataT.setText("Scanning");
                Toast.makeText(getBaseContext(), "scanning", Toast.LENGTH_SHORT).show();
                wifiManager.startScan();
            }
        });

        Button reset = (Button) findViewById(R.id.initialBelief);
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiDataT.setText("Scan stopped");
                stop = true;
                resetProbabilities();
            }
        });

        Button queue = (Button) findViewById(R.id.queue);
        queue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                endedLine = System.currentTimeMillis();
                startedLine = System.currentTimeMillis();
                trainedQueueData = readQueueData();
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

    private void resetProbabilities() {
        currSessionWifiData = new HashMap<>();
        currSessionPredictions = new int[19];
        Arrays.fill(currSessionPredictions, 0);
    }

    class WifiReceiver extends BroadcastReceiver {
        // An access point scan has completed and results are sent here
        public void onReceive(Context c, Intent intent) {
            count++;
            //Log.e("test", Integer.toString(count));
// Call getScanResults() to obtain the results
            List<ScanResult> results = wifiManager.getScanResults();
            ArrayList<WifiReading> readings = new ArrayList<>();
            try {
                long time = System.nanoTime();
                for (int n = 0; n < results.size(); n++) {
// SSID contains name of AP and level contains RSSI

                    try //Write values to the file
                    {
                        String string = time + ", " + ((TextView)findViewById(R.id.cellText)).getText() + ", " + results.get(n).SSID + ", " + results.get(n).BSSID + ", " + results.get(n).level + "\n";
                        wifiFileOSW.write(string);
                    }
                    catch (IOException e) {
                        Log.e("Writing Failure", "File 1 write failed: " + e.toString());
                    }

                    readings.add(new WifiReading(results.get(n).BSSID, results.get(n).level));
                }
                int prediction = calculateCell(readings);
                if(prediction != 0) {
                    currSessionPredictions[prediction]++;
                }
                if(!stop) {
                    int total_pred = calcMode();
                    if(total_pred != 0) {
                        prob.setText("You are in cell " + total_pred);
                        confMatrixFileOSW.write(total_pred + "," + ((TextView) findViewById(R.id.cellText)).getText() + "\n");
                    }
                    Toast.makeText(getBaseContext(), "Cell " + prediction, Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                }
            }
            catch (Exception e) { }
        }
    } // End of class WifiReceiver

    protected int calculateCell(ArrayList<WifiReading> wifiReading){
        HashMap<String, double[][]> trainedWifiData = globalTrainedWifiData;
        Collections.sort(wifiReading);
        int predicted_cell = 0;
        double max_prob = 0.0;
        double[] probabilities = new double[19];
        for(int i=0; i<probabilities.length; i++){
            probabilities[i] = 1.0/18.0;
        }

        for(int i=10; i>0; i--){
            int num_wifis = wifiReading.size();
            if(i>=wifiReading.size()){
                i = wifiReading.size()-1;
                for(int j=0; j<19; j++){
                    probabilities[j] *= numWifiData.get(j)[num_wifis];
                }
                continue;
            }
            String mac = wifiReading.get(i).mac;
            if(trainedWifiData.get(mac) != null){
                if(currSessionWifiData.get(mac) == null){
                    ArrayList<Integer> array = new ArrayList<>();
                    array.add(wifiReading.get(i).strength);
                    currSessionWifiData.put(mac, array);
                }
                int average = 0;
                ArrayList<Integer> array = currSessionWifiData.get(mac);
                for(int val : array){
                    average += val;
                }
                average = average/array.size();

                double[][] mac_data = trainedWifiData.get(mac);
                for(int cell=0; cell<NUM_CELLS; cell++){
                    probabilities[cell] = probabilities[cell] * mac_data[average][cell];
                }
                double sum = 0.0;
                for(double j : probabilities){
                    sum += j;
                }
                for(int j=0; j<probabilities.length; j++){
                    probabilities[j] += .000000001;
                    probabilities[j] /= sum + (.000000001 * 19);
                    if(probabilities[j] > .8){
                        System.out.println("over 80%");
                        return j;
                    }
                    if(probabilities[j] > max_prob){
                        max_prob = probabilities[j];
                        predicted_cell = j;
                    }
                }
            }
            else{
                System.out.println("Couldn't find MAC");
            }
        }
        return predicted_cell;
    }

    public int calcMode(){
        int sum = 0;
        int maxVal = 0;
        int maxIndex = 0;
        for(int i=0; i<currSessionPredictions.length;i++){
            if(currSessionPredictions[i] > maxVal){
                maxVal = currSessionPredictions[i];
                maxIndex = i;
            }
            sum += currSessionPredictions[i];
        }
        if(sum < 2){
            return 0;
        }
        else if(sum == 2){
            if(maxVal == 2){
                return maxIndex;
            }
            return 0;
        }
        else{
            if(maxVal > 1)
                return maxIndex;
            return 0;
        }
    }

    /*
    *
    *
    *
    *
    *
    END OF WIFI READING CODE - BEGINNING OF QUEUE CODE
    *
    *
    *
    *
    *
    */

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
            curr_x = event.values[0];
            curr_y = event.values[1];
            curr_z = event.values[2];
            accelerometerFileOSW.write(time + ", " + curr_x + ", " + curr_y + ", " + curr_z + "\n");
            //See how current values change with past ones to see if movement or not
            if(curr_x > 0.1 && curr_y > 0.1 && curr_z > 0.1)
            {
                //moving

            }
        }
        catch (IOException e) {
            Log.e("Writing Failure", "File 1 write failed: " + e.toString());
        }

//       System.out.println(event.values[1]);

        xArr.add((double)event.values[0]);
        yArr.add((double)event.values[1]);
        zArr.add((double)event.values[2]);
        timeArr.add(time);



        if (counter >= k) //if we have at least 3 lines to analyze to new file
        {
            List<Double> xTemp = new ArrayList<Double>();
            List<Double> yTemp = new ArrayList<Double>();
            List<Double> zTemp = new ArrayList<Double>();
            List<Long> timeTemp = new ArrayList<Long>();

            //Log.e("counter - 1", String.valueOf(counter-1));
            //Log.e("counter - k", String.valueOf(counter-k));

            xTemp = xArr.subList(counter - k, counter-1);
            yTemp = yArr.subList(counter-k, counter-1);
            zTemp = zArr.subList(counter-k, counter-1);

            xSlope = (xArr.get(counter-1) - xArr.get(counter - k))/(timeArr.get(counter-1) - timeArr.get(counter-k)); //calculate everything
            ySlope = (yArr.get(counter-1) - yArr.get(counter-k))/(timeArr.get(counter-1) - timeArr.get(counter-k));
            zSlope = (zArr.get(counter-1) - zArr.get(counter-k))/(timeArr.get(counter-1) - timeArr.get(counter-k));

            xMax = Collections.max(xTemp);
            yMax = Collections.max(yTemp);
            zMax = Collections.max(zTemp);
            xMin = Collections.min(xTemp);
            yMin = Collections.min(yTemp);
            zMin = Collections.min(zTemp);
            xDiff = Math.abs(xMax - xMin);
            yDiff = Math.abs(yMax - yMin);
            zDiff = Math.abs(zMax - zMin);
                //ONLY XYZ VALUES TO FILE
//                calculatedValuesFileOSW.write(xSlope + ", " + ySlope + ", " + zSlope + ", " + xMax + ", " + yMax + ", " + zMax  + ", " + xMin + ", " + yMin + ", " + zMin  + ", " + xDiff + ", " + yDiff + ", " + zDiff + "\n");
            if(trainedQueueData != null) {
                Double[] dataPoint = {xSlope, ySlope, zSlope, xDiff, yDiff, zDiff};
                boolean walking = compareToQueueData(dataPoint, 3);
//                Log.d(TAG, Arrays.toString(queue));
                if (walking) {
                    //majority value is 1
                    Toast.makeText(getBaseContext(), "Moving around", Toast.LENGTH_SHORT).show();
                    if(System.currentTimeMillis() - endedLine > 100){
                        long timeInLine = endedLine - startedLine;
                    }
                } else {
                    //majority value is 0
                    Toast.makeText(getBaseContext(), "Standing Still", Toast.LENGTH_SHORT).show();
                    endedLine = System.currentTimeMillis();
                }
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
            accelerometerFileOSW.flush(); //need to flush stream before displaying
            calculatedValuesFileOSW.flush();
            wifiFileOSW.flush();
            confMatrixFileOSW.flush();
            //Log.d("First File", readFile("firstFile.txt"));
            //Log.d("Second File", readFile("secondFile.txt"));
        }
        catch (IOException e)
        {
            Log.e("Pause Failure", "Didn't Successfully Pause: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }

    protected Double[] normalize(Double[] point){

        return point;
    }

    protected boolean compareToQueueData(Double[] point, int k){
//        double [][] trainingData = new double[10][10];
        PriorityQueue<DataPoint> nearestNeighbors = new PriorityQueue<DataPoint>();
        for(Double[] data : trainedQueueData){
            DataPoint dpoint = new DataPoint(data, normalize(point));
            nearestNeighbors.add(dpoint);
        }

        int numVal = 0;
        for(int i = 0; i<k; i++){
            numVal += nearestNeighbors.poll().value;
        }
        return numVal > k/2.0;
    }

    protected void onResume() { //Restart the logging, adding more sensor data to the file
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onFinish() { //Finish logging and display all the data you logged
        try
        {
            accelerometerFileOSW.close();
            calculatedValuesFileOSW.close();
            wifiFileOSW.close();
            confMatrixFileOSW.close();
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

    protected ArrayList<double[]> readNumWifiData(){
        Scanner input = null;
        ArrayList<double[]> map = new ArrayList<>();

        try
        {
            Context context = this;
            AssetManager am = getAssets();
            InputStream inputStream = am.open("numWifiData.txt");
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
                    String[] parse = receiveString.split(",");
                    double[] probs = new double[parse.length];
                    for(int i=0; i<parse.length; i++){
                        probs[i] = Double.parseDouble(parse[i]);
                    }
                    map.add(probs);
                }
                inputStream.close();
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
        System.out.println(map);
        return map;
    }

    protected HashMap<String, double[][]> readWifiData()
    {
        Scanner input = null;
        HashMap<String, double[][]> map = new HashMap<String, double[][]>();

        try
        {
            Context context = this;
            AssetManager am = getAssets();
            InputStream inputStream = am.open("wifiData.txt");
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
                    String[] parse = receiveString.split(",");
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
                inputStream.close();
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
        System.out.println(map);
        return map;
    }

    private ArrayList<Double[]> readQueueData() {
        ArrayList<Double[]> finalData = new ArrayList<Double[]>();
        try
        {
            Context context = this;
            AssetManager am = context.getAssets();
            InputStream inputStream = am.open("queueData.txt");
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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
        strength = _strength * -1;
    }

    public String toString(){
        return mac + ": " + strength;
    }

    public int compareTo(WifiReading other){
        return strength - other.strength;
    }
}
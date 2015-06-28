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
import android.view.MotionEvent;
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
    TextView wifiDataT, probTextView, belief, queueTextView;
    int count = 0;
    OutputStreamWriter accelerometerFileOSW, calculatedValuesFileOSW, wifiFileOSW, confMatrixFileOSW;
    ArrayList<Double> xArr, yArr, zArr;
    ArrayList<Long> timeArr;
    int counter = 0;
//    ArrayList<Double[]> trainedQueueData;
    KDTree<Integer> trainedQueueData;
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
    double[] queueMeans, queueStandardDeviations;
    int isWalking;

    boolean stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int intervalGroupSize = 3;
        isWalking = 2;
        probTextView = (TextView)findViewById(R.id.probability);
        queueTextView = (TextView) findViewById(R.id.queueStatus);
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
                probTextView.setText("Locating you...");
                stop = false;
                resetProbabilities();
                wifiDataT.setText("Scanning");
                Toast.makeText(getBaseContext(), "scanning", Toast.LENGTH_SHORT).show();
                wifiManager.startScan();
            }
        });

        Button walking = (Button) findViewById(R.id.walking);
        walking.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(isWalking>0){
                    isWalking = 0;
                }
                else{
                    isWalking = 1;
                }
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

        queueTextView.setText("Back of the queue");

        Button queue = (Button) findViewById(R.id.queue);
        queue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                queueTextView.setText("Starting back of the queue");
                endedLine = System.currentTimeMillis();
                startedLine = System.currentTimeMillis();
                ArrayList<double []> tempQData = readQueueData();
                trainedQueueData = new KDTree.Euclidean<>(tempQData.get(0).length-1);
                for(double[] temp : tempQData){
                    trainedQueueData.addPoint(Arrays.copyOfRange(temp, 0, temp.length - 1), (int) temp[temp.length - 1]);
                }
                System.out.println(trainedQueueData);
            }
        });

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Sensor Management
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , 1000);

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
                long time = System.currentTimeMillis();
                for (int n = 0; n < results.size(); n++) {
// SSID contains name of AP and level contains RSSI

                    try //Write values to the file
                    {
                        if(isWalking != 2) {
                            String string = time + ", " + ((TextView) findViewById(R.id.cellText)).getText() + ", " + results.get(n).SSID + ", " + results.get(n).BSSID + ", " + results.get(n).level + "\n";
                            wifiFileOSW.write(string);
                        }
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
                        probTextView.setText("You are in cell " + total_pred);
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
        System.out.println(predicted_cell);
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
        int firstPassk = 9;
        double xSlope, ySlope, zSlope, xVar, yVar, zVar, xDiff, yDiff, zDiff;

        long time = System.nanoTime();
        try //Write values to the file
        {
            float curr_x;
            float curr_y;
            float curr_z;
            curr_x = event.values[0];
            curr_y = event.values[1];
            curr_z = event.values[2];
            accelerometerFileOSW.write(time + ", " + curr_x + ", " + curr_y + ", " + curr_z + ", " + isWalking + "\n");
        }
        catch (IOException e) {
            Log.e("Writing Failure", "File 1 write failed: " + e.toString());
        }

//       System.out.println(event.values[1]);

        xArr.add((double)event.values[0]);
        yArr.add((double)event.values[1]);
        zArr.add((double)event.values[2]);
        timeArr.add(time);

        if (counter >= firstPassk && (counter % firstPassk) == 0) //if we have at least 3 lines to analyze to new file
        {

            xSlope = (xArr.get(firstPassk-1) - xArr.get(0))/(timeArr.get(firstPassk-1) - timeArr.get(0)); //calculate everything
            ySlope = (yArr.get(firstPassk-1) - yArr.get(0))/(timeArr.get(firstPassk-1) - timeArr.get(0));
            zSlope = (zArr.get(firstPassk-1) - zArr.get(0))/(timeArr.get(firstPassk-1) - timeArr.get(0));

            xVar = calcVar(xArr);
            yVar = calcVar(yArr);
            zVar = calcVar(zArr);

            xDiff = Math.abs(Collections.max(xArr) - Collections.min(xArr));
            yDiff = Math.abs(Collections.max(yArr) - Collections.min(yArr));
            zDiff = Math.abs(Collections.max(zArr) - Collections.min(zArr));

            if(trainedQueueData != null) {
                int secondPassk = 5;
                int knn = 5;
                double[] dataPoint = {xSlope, ySlope, zSlope, xVar, yVar, zVar, xDiff, yDiff, zDiff};
                if(lastQueue.size() >= secondPassk) {
                    lastQueue.pollLast();
                }
                Boolean temp = compareToQueueData(dataPoint, knn);
                lastQueue.push(temp);
                Boolean[] array = lastQueue.toArray(new Boolean[lastQueue.size()]);
                int count = 0;
                for(Boolean bool : array){
                    System.out.print(bool);
                    if(bool)
                        count++;
                }
                System.out.println(array.length/2.0);
                boolean walking = count > array.length/2.0;
                if (walking) {
                    //majority value is 1
                    System.out.println("walking");
//                    Toast.makeText(getBaseContext(), "Moving around", Toast.LENGTH_SHORT).show();
                    queueTextView.setText("Moving");
                    if(System.currentTimeMillis() - endedLine > 10000){
                        long timeInLine = endedLine - startedLine;
                    }
                } else {
                    System.out.println("standing");
                    //majority value is 0
//                    Toast.makeText(getBaseContext(), "Standing Still", Toast.LENGTH_SHORT).show();
                    queueTextView.setText("Standing Still");
                    endedLine = System.currentTimeMillis();
                }
            }
            xArr = new ArrayList<>();
            yArr = new ArrayList<>();
            zArr = new ArrayList<>();
        }
        counter ++;
    }

    protected double[] normalize(double[] point, double[] mean, double[] standardDeviation){
        double[] values = new double[point.length];
        for (int i = 0; i < point.length; i++) {
            values[i] = Math.abs((point[i] - mean[i])/standardDeviation[i]);
        }
        return values;
    }

    protected boolean compareToQueueData(double[] point, int k){
        ArrayList<KDTree.SearchResult<Integer>> results = trainedQueueData.nearestNeighbours(normalize(point, queueMeans, queueStandardDeviations), k);

        Integer numVal = 0;
        for(KDTree.SearchResult<Integer> i : results) {
            numVal += i.payload;
        }
//        System.out.println(numVal);
//        System.out.println(k/2.0);
//        System.out.println(numVal > k/2.0);
        return numVal > k/2.0;
    }

    protected double calcVar(ArrayList<Double> array){
        double sum = 0.0;
        for(Double val : array){
            sum += val;
        }
        double mean = sum/array.size();
        double variance = 0.0;
        for(Double val : array){
            variance += Math.pow((val-mean), 2);
        }
        return variance;
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

    protected void onResume() { //Restart the logging, adding more sensor data to the file
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, 10);

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

    private ArrayList<double[]> readQueueData() {
        ArrayList<double[]> finalData = new ArrayList<double[]>();
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
                    double[] doubleData = new double[rowData.length];
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
        queueMeans = finalData.get(0);
        queueStandardDeviations = finalData.get(1);
        finalData.remove(1);
        finalData.remove(0);
        System.out.println("Finished reading");
        Toast.makeText(getBaseContext(), "Finished reading", Toast.LENGTH_SHORT).show();
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
        data = Arrays.copyOfRange(_data, 0, _data.length-1);
        System.out.println(data.length);
        value = _data[data.length-1].intValue();
//        distance = calculateDistance(mainPoint);
        distance = 0.0;
    }

    public Double calculateDistance(Double[] otherPoint){
        System.out.println(data.length);
        System.out.println(otherPoint.length);
        Double dist = 0.0;
        for(int i=0; i<otherPoint.length; i++){
            dist += Math.pow((data[i] - otherPoint[i]), 2);
        }
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
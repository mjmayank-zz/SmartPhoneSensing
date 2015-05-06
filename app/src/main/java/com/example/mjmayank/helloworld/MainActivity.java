package com.example.mjmayank.helloworld;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.util.Arrays;
import java.util.PriorityQueue;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    TextView Xpoint, Ypoint, Zpoint;
    OutputStreamWriter firstFileOSW;
    OutputStreamWriter secondFileOSW;
    float xOne, yOne, zOne;
    float xTwo, yTwo, zTwo;
    float xThree, yThree, zThree;
    float timeOne, timeTwo, timeThree;
    float xSlope, ySlope, zSlope, xMax, yMax, zMax, xMin, yMin, zMin, xDiff, yDiff, zDiff;
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }


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

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Sensor Management
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        Xpoint = (TextView) findViewById(R.id.xCoord); //Change values of textViews
        Ypoint = (TextView) findViewById(R.id.yCoord);
        Zpoint = (TextView) findViewById(R.id.zCoord);

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
        Log.d("Test", Arrays.toString(event.values)); //Displaying Values as they change
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
                xThree = event.values[0];
                yThree = event.values[1];
                zThree = event.values[2];
                timeThree = time;
            }
            xSlope = Math.abs((xOne - xThree)/(timeThree - timeOne)); //calculate everything
            ySlope = Math.abs((yOne - yThree)/(timeThree - timeOne));
            zSlope = Math.abs((zOne - zThree)/(timeThree - timeOne));
            xMax = Math.max(Math.max(xOne, xTwo), xThree);
            yMax = Math.max(Math.max(yOne, yTwo), yThree);
            zMax = Math.max(Math.max(zOne, zTwo), zThree);
            xMin = Math.min(Math.min(xOne, xTwo), xThree);
            yMin = Math.min(Math.min(yOne, yTwo), yThree);
            xMin = Math.min(Math.min(zOne, zTwo), zThree);
            xDiff = xMax - xMin;
            yDiff = yMax - yMin;
            zDiff = zMax - zMin;
            try
            {
                secondFileOSW.write(xSlope + ", " + ySlope + ", " + zSlope + ", " + xMax + ", " + yMax + ", " + zMax  + ", " + xMin + ", " + yMin + ", " + zMin  + ", " + xDiff + ", " + yDiff + ", " + zDiff + "\n");
            }
            catch (IOException e) {
                Log.e("Writing Failure", "File 2 write failed: " + e.toString());
            }
        }
        else if(counter == 0) //assigning first row of xyz **happens once
        {
            timeOne = time;
            xOne = event.values[0];
            yOne = event.values[1];
            zOne = event.values[2];
        }
        else if(counter == 1) //assigning second row of xyz **happens once
        {
            timeTwo = time;
            xTwo = event.values[0];
            yTwo = event.values[1];
            zTwo = event.values[2];
        }
        else if(counter == 2) //assigning third row of xyz **happens once
        {
            timeThree = time;
            xThree = event.values[0];
            yThree = event.values[1];
            zThree = event.values[2];
        }
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
            //Log.d("First File", readFile("firstFile.txt"));
            //Log.d("Second File", readFile("secondFile.txt"));
        }
        catch (IOException e)
        {
            Log.e("Pause Failure", "Didn't Successfully Pause: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }

    private String readFile(String fileName) {

        String ret = ""; //start with blank file
        try
        {
            InputStream inputStream = openFileInput(fileName); //input stream
            if (inputStream != null) //make sure the file isn't empty
            {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine())!= null) //go line by line
                {
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

        return ret;
    }

    protected DataPoint[] compareToData(int[] point, int k){
        int [][] trainingData = new int[10][10];
        PriorityQueue<DataPoint> nearestNeighbors = new PriorityQueue<DataPoint>();
        for(int[] data : trainingData){
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
            //Log.d("First File", readFile("firstFile.txt"));
            //Log.d("Second FIle", readFile("secondFile.txt"));
        } catch (IOException e)
        {
            Log.e("Closing Failure", "Can't Close: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }
}

class DataPoint implements Comparable<DataPoint>{
    int[] data;
    double distance;

    public DataPoint(int[] _data, int[] mainPoint){
        data = _data;
        distance = calculateDistance(mainPoint);
    }

    public double calculateDistance(int[] otherPoint){
        double dist = 0;
        for(int i=0; i<otherPoint.length; i++){
            dist += Math.pow((data[i] - otherPoint[i]), 2);
        }
        dist = Math.pow(dist, .5);
        return distance;
    }

    public int compareTo(DataPoint other){
        return Double.compare(distance, other.distance);
    }
}
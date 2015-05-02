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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    TextView Xpoint, Ypoint, Zpoint;
    OutputStreamWriter outputStreamWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try //create the file and open a stream writer to it
        {
            outputStreamWriter = new OutputStreamWriter(openFileOutput("logger.txt", Context.MODE_PRIVATE));
        }
        catch (FileNotFoundException e)
        {
            Log.e("Writing Failure", "Can not open stream writer: " + e.toString());
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
        try //Write values to the file
        {
            outputStreamWriter.write("X-Coordinate: " + event.values[0] + ", " + "Y-Coordinate: " + event.values[1] + ", " + "Z-Coordinate: " + event.values[2]);
        }
        catch (IOException e) {
            Log.e("Writing Failure", "File write failed: " + e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {}

    protected void onPause() //When we pause the sensor logging
    {
        super.onPause();
        try
        {
            outputStreamWriter.flush(); //need to flush stream before displaying
            Log.d("Full File", readFile());
        }
        catch (IOException e)
        {
            Log.e("Pause Failure", "Didn't Successfully Pause: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }

    private String readFile() {

        String ret = ""; //start with blank file
        try
        {
            InputStream inputStream = openFileInput("logger.txt"); //input stream
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

    protected void onResume() { //Restart the logging, adding more sensor data to the file
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onFinish() { //Finish logging and display all the data you logged
        try
        {
            outputStreamWriter.close();
            Log.d("Full File", readFile());
        } catch (IOException e)
        {
            Log.e("Closing Failure", "Can't Close: " + e.toString());
        }
        senSensorManager.unregisterListener(this);
    }
}
package r.psy.phy.soundstop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    long starttime=0L,timeMilli=0L,timeSwap=0L,updateTime=0L;
    private static volatile boolean running=false;
    public static boolean ispermit=false;
    private long lastPress;
    TextView timeView;
    TextView startBtn;

    Handler customHandler =new Handler();
    Runnable updatetimerThread= new Runnable() {
        @Override
        public void run() {

            timeMilli=SystemClock.uptimeMillis()-starttime;
            updateTime=timeSwap+timeMilli;
            int secs=(int)(updateTime/1000);
            int mins=secs/60;
            secs%=60;
            int msecs=(int) (updateTime%1000);
            timeView.setText(String
                    .format(Locale.getDefault(),
                            "%02d:%02d:%03d",mins, secs, msecs));
            customHandler.postDelayed(this,0);

        }
    };

    Handler soundHandler = new Handler();
    final Runnable recordThread = new Runnable() {
        @Override
        public void run() {

            try {
                startRecord();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ProgressBar progressBar = findViewById(R.id.progressBar);

            SeekBar seekBar =findViewById(R.id.seekBar);

            double soundLevel=getAmplitude();
            double decibel= 10*Math.log10(soundLevel/20);
            int soundLevelint=(int) decibel;

            progressBar.setProgress(soundLevelint);

            //level.setText(Integer.toString(soundLevelint));

            if (soundLevelint>seekBar.getProgress() && running){
                pauseCount();
            }

            soundHandler.postDelayed(this,0);
        }
    };





    @Override
    public void onBackPressed(){
        long currentTime = System.currentTimeMillis();
        if(currentTime - lastPress > 2000){
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
            lastPress = currentTime;
        }else{
            super.onBackPressed();
        }
    }
    //===============================================================================

    private MediaRecorder mRecorder = null;

    public void startRecord() throws IOException {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            mRecorder.prepare();
            mRecorder.start();
        }
    }

    public void stopRecord() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  mRecorder.getMaxAmplitude();
        else
            return 0;

    }

    public void startCount(){
        starttime = SystemClock.uptimeMillis();
        running=true;
        customHandler.postDelayed(updatetimerThread, 0);
        startBtn.setText("\u25a0");
    }


    public void pauseCount(){

        running=false;
        timeSwap+=timeMilli;
        customHandler.removeCallbacks(updatetimerThread);
        startBtn.setText("\u25b6");
    }



    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { permission },
                    requestCode);
        }
        else {
            ispermit=true;
            soundHandler.postDelayed(recordThread,0);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                        "Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
                ispermit=true;
                soundHandler.postDelayed(recordThread,0);
            }
            else {
                checkPermission(Manifest.permission.RECORD_AUDIO,1);
                ispermit=false;
            }
        }
    }

    //===================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ispermit){
            soundHandler.postDelayed(recordThread,0);
        }


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        checkPermission(Manifest.permission.RECORD_AUDIO,1);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView infoBtn =findViewById(R.id.infoBtn);

        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,InfoActivity.class);
                startActivity(intent);

            }
        });

        timeView =findViewById(R.id.time_view);
        startBtn =findViewById(R.id.startBtn);
        TextView resetBtn =findViewById(R.id.resetBtn);



        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!running) {
                    startCount();
                }
                else{
                    pauseCount();

                }

            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int t=0;

                running=false;
                customHandler.removeCallbacks(updatetimerThread);
                timeView.setText(String
                        .format(Locale.getDefault(),
                                "%02d:%02d:%03d",t, t, t));
                starttime=0L;
                timeMilli=0L;
                timeSwap=0L;
                updateTime=0L;
                startBtn.setText("\u25b6");
            }
        });


    }


    @Override
    protected void onPause()
    {
        super.onPause();
        stopRecord();
       // wasRunning = running;
        //running = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRecord();
    }



}
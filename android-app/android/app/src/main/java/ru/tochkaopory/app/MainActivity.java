package ru.tochkaopory.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends BridgeActivity {
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;
    private static final String VERSION_URL = "https://himikmetan-wq.github.io/tochka-opory/version.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getBridge().getWebView().addJavascriptInterface(new AndroidBridge(), "TochkaAndroid");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("463727981974-a201ad105cc2e51e7999b7.apps.googleusercontent.com")
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        checkForUpdate();
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void googleSignIn() {
            runOnUiThread(() -> startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN));
        }

        @JavascriptInterface
        public void openQuickLaunchSettings() {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this,
                    "Включите «Точка опоры — быстрый запуск»",
                    Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String token = account.getIdToken();
                if (token != null) {
                    String quoted = JSONObject.quote(token);
                    getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript("window.tochkaFirebaseSignIn(" + quoted + ")", null));
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Не удалось войти через Google (" + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkForUpdate() {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(VERSION_URL + "?t=" + System.currentTimeMillis()).openConnection();
                c.setConnectTimeout(5000); c.setReadTimeout(5000);
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder b = new StringBuilder(); String line;
                while ((line = r.readLine()) != null) b.append(line);
                r.close();
                JSONObject j = new JSONObject(b.toString());
                int latest = j.getInt("versionCode");
                String versionName = j.optString("versionName", "");
                String apkUrl = j.getString("apkUrl");
                long currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
                if (latest > currentVersion) {
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

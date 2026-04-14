package com.ghostshadow.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.KeyEvent;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ImageView wallpaperView;
    private SharedPreferences prefs;
    private boolean isIncognito = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("GhostBrowserSettings", Context.MODE_PRIVATE);
        webView = findViewById(R.id.webView);
        wallpaperView = findViewById(R.id.wallpaperView);
        EditText addressBar = findViewById(R.id.addressBar);

        setupBrowser();

        // 1. Wallpaper Logic
        findViewById(R.id.btnChangeBg).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        });

        // 2. Incognito Logic with Password
        findViewById(R.id.btnIncognito).setOnClickListener(v -> verifyIncognitoAccess());

        // 3. Address Bar Logic
        addressBar.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String url = addressBar.getText().toString();
                if (!url.startsWith("http")) url = "https://www.google.com/search?q=" + url;
                webView.loadUrl(url);
                return true;
            }
            return false;
        });
    }

    private void setupBrowser() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); 
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        
        // Universal Downloader Implementation
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();
        });

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://www.google.com");
    }

    private void verifyIncognitoAccess() {
        String savedPass = prefs.getString("incog_pwd", null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(savedPass == null ? "Set Incognito Password" : "Enter Password");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Access", (dialog, which) -> {
            String pass = input.getText().toString();
            if (savedPass == null) {
                if(!pass.isEmpty()){
                   prefs.edit().putString("incog_pwd", pass).apply();
                   toggleIncognito(true);
                }
            } else if (pass.equals(savedPass)) {
                toggleIncognito(true);
            } else {
                Toast.makeText(this, "Wrong Password!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void toggleIncognito(boolean enable) {
        isIncognito = enable;
        if (enable) {
            CookieManager.getInstance().setAcceptCookie(false);
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.clearHistory();
            findViewById(R.id.topBar).setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"));
            Toast.makeText(this, "Incognito Mode Active", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            wallpaperView.setImageURI(data.getData());
        }
    }
}

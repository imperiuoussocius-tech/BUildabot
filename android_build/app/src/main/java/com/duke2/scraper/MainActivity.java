package com.duke2.scraper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.*;

public class MainActivity extends Activity {
    
    private static final int REQUEST_STORAGE = 100;
    private TextView tvStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Main layout
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(0xFF0A0A0F);
        
        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🕷️ Web Scraper");
        tvTitle.setTextSize(28);
        tvTitle.setTextColor(0xFFE94560);
        tvTitle.setPadding(0, 0, 0, 20);
        layout.addView(tvTitle);
        
        // Status
        tvStatus = new TextView(this);
        tvStatus.setText("Status: Ready");
        tvStatus.setTextColor(0xFF888888);
        tvStatus.setPadding(0, 0, 0, 30);
        layout.addView(tvStatus);
        
        // Buttons
        addButton(layout, "▶️ Start Scraper", v -> startScraper());
        addButton(layout, "🖼️ Open Gallery", v -> openGallery());
        addButton(layout, "⚙️ Proxy Settings", v -> showProxySettings());
        addButton(layout, "🛡️ Cloudflare Bypass", v -> showBypassInfo());
        addButton(layout, "📖 Quick Tutorial", v -> showTutorial());
        addButton(layout, "📘 User Guide", v -> showUserGuide());
        addButton(layout, "📁 Open Download Folder", v -> openDownloadFolder());
        
        scrollView.addView(layout);
        setContentView(scrollView);
        
        checkPermissions();
    }
    
    private void addButton(LinearLayout layout, String text, android.view.View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(0xFFE94560);
        btn.setTextColor(0xFFFFFFFF);
        btn.setPadding(20, 30, 20, 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);
        layout.addView(btn);
    }
    
    private void checkPermissions() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.INTERNET
            }, REQUEST_STORAGE);
        }
    }
    
    private void startScraper() {
        Toast.makeText(this, "Run in Termux: python Duke2_Enhanced.py", Toast.LENGTH_LONG).show();
        
        // Try to launch Termux
        try {
            Intent intent = new Intent();
            intent.setClassName("com.termux", "com.termux.app.TermuxActivity");
            startActivity(intent);
        } catch (Exception e) {
            showTermuxDialog();
        }
    }
    
    private void openGallery() {
        File galleryDir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        File galleryFile = new File(galleryDir, "gallery.html");
        
        if (galleryFile.exists()) {
            Uri uri = FileProvider.getUriForFile(this, "com.duke2.scraper.fileprovider", galleryFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/html");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No gallery found. Run scraper first!", Toast.LENGTH_LONG).show();
        }
    }
    
    private void openDownloadFolder() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        if (!dir.exists()) dir.mkdirs();
        
        Uri uri = FileProvider.getUriForFile(this, "com.duke2.scraper.fileprovider", dir);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fallback - try file manager
            Intent fileIntent = new Intent(Intent.ACTION_VIEW);
            fileIntent.setDataAndType(Uri.parse(dir.getAbsolutePath()), "*/*");
            try {
                startActivity(fileIntent);
            } catch (Exception e2) {
                Toast.makeText(this, "Download folder: " + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void showProxySettings() {
        new AlertDialog.Builder(this)
            .setTitle("⚙️ Proxy Settings")
            .setMessage("Configure proxy in Duke2_Enhanced.py:\n\n"
                + "1. HTTP Proxy: http://host:port\n"
                + "2. SOCKS5: socks5://user:pass@host:port\n"
                + "3. Rotating proxy supported\n\n"
                + "Enter proxy URL when prompted during scraper startup.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showBypassInfo() {
        new AlertDialog.Builder(this)
            .setTitle("🛡️ Cloudflare Bypass Engines")
            .setMessage("Available bypass engines:\n\n"
                + "1. curl_cffi - TLS fingerprint impersonation (recommended)\n"
                + "2. cloudscraper - JavaScript challenge solver\n"
                + "3. Standard requests (limited)\n\n"
                + "Install: pip install curl-cffi cloudscraper")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTutorial() {
        new AlertDialog.Builder(this)
            .setTitle("📖 Quick Tutorial")
            .setMessage("1. Install Termux from F-Droid\n"
                + "2. Run: pkg install python\n"
                + "3. Run: pip install requests bs4 curl-cffi cloudscraper\n"
                + "4. Run: python Duke2_Enhanced.py\n"
                + "5. Enter URL and configure options\n"
                + "6. View results in gallery.html\n\n"
                + "For full tutorial, use the User Guide button.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showUserGuide() {
        try {
            java.io.InputStream is = getAssets().open("TUTORIAL.md");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            is.close();

            TextView tvGuide = new TextView(this);
            tvGuide.setText(builder.toString());
            tvGuide.setTextColor(0xFFE0E0E0);
            tvGuide.setPadding(20, 20, 20, 20);

            ScrollView scroll = new ScrollView(this);
            scroll.addView(tvGuide);

            new AlertDialog.Builder(this)
                .setTitle("📘 Web Scraper User Guide")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to load user guide.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void showTermuxDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Termux Required")
            .setMessage("Web Scraper requires Termux to run.\n\n"
                + "Install Termux from F-Droid, then:\n"
                + "1. pkg install python\n"
                + "2. pip install requests bs4 curl-cffi\n"
                + "3. python Duke2_Enhanced.py")
            .setPositiveButton("Get Termux", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://f-droid.org/packages/com.termux/"));
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}

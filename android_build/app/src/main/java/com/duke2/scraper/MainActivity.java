package com.duke2.scraper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.*;

public class MainActivity extends Activity {
    
    private static final int REQUEST_STORAGE = 100;
    private TextView tvStatus;
    private int pendingDepth = 1;
    private int pendingMaxFiles = 50;
    private String pendingBypass = "standard";
    
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
        // Create dialog to get URL and options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Web Scraper Settings");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        // URL input
        TextView tvUrl = new TextView(this);
        tvUrl.setText("URL to Scrape:");
        tvUrl.setTextColor(0xFFE94560);
        tvUrl.setPadding(0, 0, 0, 10);
        layout.addView(tvUrl);
        
        EditText etUrl = new EditText(this);
        etUrl.setHint("https://example.com");
        etUrl.setTextColor(0xFFE0E0E0);
        etUrl.setHintTextColor(0xFF666666);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        urlParams.setMargins(0, 0, 0, 20);
        etUrl.setLayoutParams(urlParams);
        layout.addView(etUrl);
        
        // Depth selection
        TextView tvDepth = new TextView(this);
        tvDepth.setText("Crawl Depth: 1");
        tvDepth.setTextColor(0xFFE94560);
        tvDepth.setPadding(0, 0, 0, 10);
        layout.addView(tvDepth);
        
        SeekBar sbDepth = new SeekBar(this);
        sbDepth.setMax(3);
        sbDepth.setProgress(1);
        sbDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean b) { tvDepth.setText("Crawl Depth: " + (p + 1)); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        LinearLayout.LayoutParams depthParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        depthParams.setMargins(0, 0, 0, 20);
        sbDepth.setLayoutParams(depthParams);
        layout.addView(sbDepth);
        
        // Max files selection
        TextView tvFiles = new TextView(this);
        tvFiles.setText("Max Files: 50");
        tvFiles.setTextColor(0xFFE94560);
        tvFiles.setPadding(0, 0, 0, 10);
        layout.addView(tvFiles);
        
        SeekBar sbFiles = new SeekBar(this);
        sbFiles.setMax(9);
        sbFiles.setProgress(4);
        sbFiles.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean b) { tvFiles.setText("Max Files: " + ((p + 1) * 10)); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        sbFiles.setLayoutParams(depthParams);
        layout.addView(sbFiles);
        
        // Bypass mode selection
        TextView tvBypass = new TextView(this);
        tvBypass.setText("Bypass Mode: standard");
        tvBypass.setTextColor(0xFFE94560);
        tvBypass.setPadding(0, 0, 0, 10);
        layout.addView(tvBypass);
        
        SeekBar sbBypass = new SeekBar(this);
        sbBypass.setMax(2);
        sbBypass.setProgress(0);
        sbBypass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean b) { 
                String[] modes = {"standard", "stealth", "cf"};
                tvBypass.setText("Bypass Mode: " + modes[p]);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        sbBypass.setLayoutParams(depthParams);
        layout.addView(sbBypass);
        
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        
        builder.setView(scroll);
        builder.setPositiveButton("Start Scraping", (d, w) -> {
            String url = etUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            } else {
                int depth = sbDepth.getProgress() + 1;
                int maxFiles = (sbFiles.getProgress() + 1) * 10;
                String[] bypassModes = {"standard", "stealth", "cf"};
                String bypass = bypassModes[sbBypass.getProgress()];
                performScraping(url, depth, maxFiles, bypass);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void performScraping(String url, int depth, int maxFiles, String bypass) {
        // If Cloudflare mode selected, use WebView bypass to capture rendered HTML first
        if ("cf".equals(bypass)) {
            pendingDepth = depth;
            pendingMaxFiles = maxFiles;
            pendingBypass = bypass;
            Intent i = new Intent(this, WebViewBypassActivity.class);
            i.putExtra(WebViewBypassActivity.EXTRA_URL, url);
            startActivityForResult(i, 200);
            return;
        }
        // Show progress dialog
        AlertDialog.Builder progressBuilder = new AlertDialog.Builder(this);
        progressBuilder.setTitle("Scraping in Progress...");
        
        TextView tvProgress = new TextView(this);
        tvProgress.setText("Initializing scraper...");
        tvProgress.setTextColor(0xFFE0E0E0);
        tvProgress.setPadding(20, 20, 20, 20);
        
        progressBuilder.setView(tvProgress);
        progressBuilder.setCancelable(false);
        AlertDialog progressDialog = progressBuilder.show();
        
        // Create and start scraper engine with bypass mode
        ScraperEngine scraper = new ScraperEngine(new ScraperEngine.ScraperCallback() {
            @Override
            public void onProgress(String message) {
                tvProgress.setText(message);
            }
            
            @Override
            public void onComplete(String galleryPath) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Scraping complete! Gallery saved.", Toast.LENGTH_LONG).show();
                openGallery();
            }
            
            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }, bypass);
        
        scraper.scrape(url, depth, maxFiles);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            String fileUri = data.getStringExtra(WebViewBypassActivity.EXTRA_OUTPUT);
            if (fileUri != null) {
                // Start scraping using the captured local file
                performScrapingWithFile(fileUri, pendingDepth, pendingMaxFiles, pendingBypass);
            } else {
                Toast.makeText(this, "Bypass failed or returned no content.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void performScrapingWithFile(String fileUri, int depth, int maxFiles, String bypass) {
        AlertDialog.Builder progressBuilder = new AlertDialog.Builder(this);
        progressBuilder.setTitle("Scraping in Progress...");
        TextView tvProgress = new TextView(this);
        tvProgress.setText("Initializing scraper (bypass)...");
        tvProgress.setTextColor(0xFFE0E0E0);
        tvProgress.setPadding(20, 20, 20, 20);
        progressBuilder.setView(tvProgress);
        progressBuilder.setCancelable(false);
        AlertDialog progressDialog = progressBuilder.show();

        ScraperEngine scraper = new ScraperEngine(new ScraperEngine.ScraperCallback() {
            @Override
            public void onProgress(String message) { tvProgress.setText(message); }
            @Override
            public void onComplete(String galleryPath) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Scraping complete! Gallery saved.", Toast.LENGTH_LONG).show();
                openGallery();
            }
            @Override
            public void onError(String error) { progressDialog.dismiss(); Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show(); }
        }, bypass);

        scraper.scrape(fileUri, depth, maxFiles);
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
            .setMessage("Proxy Configuration:\n\n"
                + "Proxy support coming in future version.\n\n"
                + "For now, the scraper:\n"
                + "• Uses standard HTTP connections\n"
                + "• Auto-detects Cloudflare\n"
                + "• Respects robots.txt\n"
                + "• Rotates User-Agent headers\n\n"
                + "Check settings for VPN options.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showBypassInfo() {
        new AlertDialog.Builder(this)
            .setTitle("🛡️ Bypass Modes")
            .setMessage("Web Scraper v3.0.2 Bypass Features:\n\n"
                + "🔹 STANDARD MODE\n"
                + "  • Basic HTTP requests\n"
                + "  • Best for: Unprotected sites\n"
                + "  • Speed: ⚡⚡⚡ Fastest\n\n"
                + "🔹 STEALTH MODE\n"
                + "  • User-Agent rotation\n"
                + "  • Realistic browser headers\n"
                + "  • Best for: Sites with IP blocking\n"
                + "  • Speed: ⚡⚡ Moderate\n\n"
                + "🔹 CF MODE (Cloudflare)\n"
                + "  • Retry logic for 403 errors\n"
                + "  • Aggressive header rotation\n"
                + "  • Best for: Cloudflare protected sites\n"
                + "  • Speed: ⚡ Slower\n\n"
                + "💡 Tips: Start with STANDARD, upgrade if blocked")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTutorial() {
        new AlertDialog.Builder(this)
            .setTitle("📖 Quick Tutorial")
            .setMessage("Web Scraper Guide:\n\n"
                + "1. Tap 'Start Scraper'\n"
                + "2. Enter a website URL (https://...)\n"
                + "3. Set crawl depth (1-4)\n"
                + "4. Set max files to download (10-100)\n"
                + "5. App will extract images, videos, audio\n"
                + "6. View results in gallery.html\n\n"
                + "⚙️ Proxy Settings: Configure in settings\n"
                + "🛡️ Bypass: Auto-detects Cloudflare\n"
                + "📁 Downloads: All files saved to Download/Duke2\n\n"
                + "For full guide, use the User Guide button.")
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
}

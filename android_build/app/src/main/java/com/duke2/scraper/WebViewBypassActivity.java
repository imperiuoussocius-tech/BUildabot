package com.duke2.scraper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.File;
import java.io.FileOutputStream;

public class WebViewBypassActivity extends Activity {
    public static final String EXTRA_URL = "bypass_url";
    public static final String EXTRA_OUTPUT = "bypass_output";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            finish();
            return;
        }

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String finishedUrl) {
                // Extract HTML
                view.evaluateJavascript("(function(){return document.documentElement.outerHTML;})();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String html) {
                        try {
                            // html is a quoted string; remove surrounding quotes
                            if (html != null && html.length() > 1 && html.startsWith("\"") && html.endsWith("\"")) {
                                html = html.substring(1, html.length()-1).replaceAll("\\\\u003C", "<");
                                html = html.replaceAll("\\\\n", "\n").replaceAll("\\\\\"", "\"");
                            }

                            File dir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
                            if (!dir.exists()) dir.mkdirs();
                            File out = new File(dir, "bypass.html");
                            try (FileOutputStream fos = new FileOutputStream(out)) {
                                fos.write(html.getBytes("UTF-8"));
                            }

                            Intent result = new Intent();
                            result.putExtra(EXTRA_OUTPUT, "file://" + out.getAbsolutePath());
                            setResult(RESULT_OK, result);
                            finish();
                        } catch (Exception e) {
                            new AlertDialog.Builder(WebViewBypassActivity.this)
                                .setTitle("Error")
                                .setMessage("Failed to capture page: " + e.getMessage())
                                .setPositiveButton("OK", (d, w) -> finish())
                                .show();
                        }
                    }
                });
            }
        });

        webView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

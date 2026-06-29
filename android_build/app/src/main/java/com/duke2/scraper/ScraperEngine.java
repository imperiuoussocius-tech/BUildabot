package com.duke2.scraper;

import android.os.Environment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScraperEngine {

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp", ".ico", ".tiff", ".avif"
    );

    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
            ".mp4", ".webm", ".mkv", ".flv", ".avi", ".mov", ".m4v", ".3gp", ".ogv"
    );

    private static final List<String> AUDIO_EXTENSIONS = Arrays.asList(
            ".mp3", ".ogg", ".wav", ".flac", ".aac", ".m4a", ".opus", ".wma"
    );

    public interface ScraperCallback {
        void onProgress(String message);
        void onComplete(String galleryPath);
        void onError(String error);
    }

    private OkHttpClient httpClient;
    private Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    private List<String> downloadedMedia = Collections.synchronizedList(new ArrayList<>());
    private File downloadDir;
    private ScraperCallback callback;
    private volatile boolean isRunning = false;

    public ScraperEngine(ScraperCallback callback) {
        this.callback = callback;
        this.httpClient = new OkHttpClient.Builder().build();
        this.downloadDir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        if (!downloadDir.exists()) downloadDir.mkdirs();
    }

    public boolean isRunning() { return isRunning; }
    public void stop() { isRunning = false; }

    public void scrape(String startUrl, int maxDepth, int maxFiles) {
        new Thread(() -> {
            try {
                isRunning = true;
                callback.onProgress("Starting scraper...");
                downloadedMedia.clear();
                visitedUrls.clear();
                scrapeUrl(startUrl, maxDepth, maxFiles);
                callback.onProgress("Generating gallery...");
                String gallery = generateGallery();
                if (gallery != null) callback.onComplete(gallery);
                else callback.onError("Failed to generate gallery");
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            } finally {
                isRunning = false;
            }
        }).start();
    }

    private void scrapeUrl(String url, int depth, int maxFiles) {
        if (!isRunning || depth <= 0 || downloadedMedia.size() >= maxFiles) return;
        if (visitedUrls.contains(url)) return;
        visitedUrls.add(url);
        try {
            callback.onProgress("Crawling: " + url + " (Depth: " + depth + ")");
            Document doc = fetchDocument(url);
            if (doc == null) return;

            Elements images = doc.select("img[src], img[data-src]");
            for (Element img : images) {
                if (downloadedMedia.size() >= maxFiles) break;
                String src = img.hasAttr("src") ? img.attr("src") : img.attr("data-src");
                if (src == null || src.isEmpty()) continue;
                String mediaUrl = resolveUrl(url, src);
                downloadMedia(mediaUrl);
            }

            // Follow simple same-domain links
            if (depth > 1) {
                Elements links = doc.select("a[href]");
                String domain = extractDomain(url);
                for (Element a : links) {
                    if (downloadedMedia.size() >= maxFiles) break;
                    String href = a.attr("href");
                    String next = resolveUrl(url, href);
                    if (next != null && next.startsWith("http") && domain.equals(extractDomain(next)) && !visitedUrls.contains(next)) {
                        scrapeUrl(next, depth - 1, maxFiles);
                    }
                }
            }
        } catch (Exception e) {
            // ignore individual page errors
        }
    }

    private Document fetchDocument(String url) {
        try {
            if (url != null && url.startsWith("file://")) {
                String path = url.substring("file://".length());
                byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path));
                String html = new String(data, StandardCharsets.UTF_8);
                return Jsoup.parse(html, "");
            }
            Request req = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                String body = resp.body().string();
                return Jsoup.parse(body, url);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void downloadMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;
        try {
            Request req = new Request.Builder().url(mediaUrl).header("User-Agent", "Mozilla/5.0").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return;
                String filename = new java.io.File(new java.net.URL(mediaUrl).getPath()).getName();
                if (filename == null || filename.isEmpty()) filename = "media_" + System.currentTimeMillis();
                File out = new File(downloadDir, filename);
                try (InputStream in = resp.body().byteStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192]; int r;
                    while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
                }
                downloadedMedia.add(out.getAbsolutePath());
                callback.onProgress("Saved: " + out.getAbsolutePath());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private String generateGallery() {
        try {
            File gallery = new File(downloadDir, "gallery.html");
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset=\"utf-8\"><title>Gallery</title></head><body>\n");
            for (String p : downloadedMedia) {
                String rel = p;
                if (p.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)$")) {
                    sb.append("<div><img src=\"file://" + rel + "\" style=\"max-width:300px\"><p>" + rel + "</p></div>\n");
                } else {
                    sb.append("<div><a href=\"file://" + rel + "\">" + rel + "</a></div>\n");
                }
            }
            sb.append("</body></html>");
            java.nio.file.Files.write(gallery.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            return gallery.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private String resolveUrl(String base, String rel) {
        try {
            if (rel == null) return base;
            java.net.URI baseUri = new java.net.URI(base);
            java.net.URI resolved = baseUri.resolve(rel);
            return resolved.toString();
        } catch (Exception e) {
            return rel;
        }
    }

    private String extractDomain(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private String generateFilename(String url) {
        try {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            if (filename.contains("?")) filename = filename.substring(0, filename.indexOf('?'));
            if (filename.isEmpty() || filename.length() > 120) filename = "media_" + System.currentTimeMillis();
            return filename;
        } catch (Exception e) {
            return "media_" + System.currentTimeMillis();
        }
    }

}

import android.os.Environment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScraperEngine {

    public interface ScraperCallback {
        void onProgress(String message);
        void onComplete(String galleryPath);
        void onError(String error);
    }

    private OkHttpClient httpClient;
    private Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    private List<String> downloadedMedia = Collections.synchronizedList(new ArrayList<>());
    private File downloadDir;
    private ScraperCallback callback;
    private volatile boolean isRunning = false;

    public ScraperEngine(ScraperCallback callback) {
        this.callback = callback;
        this.httpClient = new OkHttpClient.Builder().build();
        this.downloadDir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        if (!downloadDir.exists()) downloadDir.mkdirs();
    }

    public boolean isRunning() { return isRunning; }
    public void stop() { isRunning = false; }

    public void scrape(String startUrl, int maxDepth, int maxFiles) {
        new Thread(() -> {
            try {
                isRunning = true;
                callback.onProgress("Starting scraper...");
                downloadedMedia.clear();
                visitedUrls.clear();
                scrapeUrl(startUrl, maxDepth, maxFiles);
                callback.onProgress("Generating gallery...");
                String gallery = generateGallery();
                if (gallery != null) callback.onComplete(gallery);
                else callback.onError("Failed to generate gallery");
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            } finally {
                isRunning = false;
            }
        }).start();
    }

    private void scrapeUrl(String url, int depth, int maxFiles) {
        if (!isRunning || depth <= 0 || downloadedMedia.size() >= maxFiles) return;
        if (visitedUrls.contains(url)) return;
        visitedUrls.add(url);
        try {
            callback.onProgress("Crawling: " + url + " (Depth: " + depth + ")");
            Document doc = fetchDocument(url);
            if (doc == null) return;

            Elements images = doc.select("img");
            for (Element img : images) {
                if (downloadedMedia.size() >= maxFiles) break;
                String src = img.hasAttr("src") ? img.attr("src") : img.attr("data-src");
                if (src == null || src.isEmpty()) continue;
                String mediaUrl = resolveUrl(url, src);
                downloadMedia(mediaUrl);
            }

            // Follow simple links for crawling
            Elements links = doc.select("a[href]");
            for (Element a : links) {
                if (downloadedMedia.size() >= maxFiles) break;
                String href = a.attr("href");
                String next = resolveUrl(url, href);
                if (next != null && !visitedUrls.contains(next)) {
                    scrapeUrl(next, depth - 1, maxFiles);
                }
            }
        } catch (Exception e) {
            // continue on errors
        }
    }

    private Document fetchDocument(String url) {
        try {
            if (url.startsWith("file://")) {
                String path = url.substring("file://".length());
                byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path));
                String html = new String(data, StandardCharsets.UTF_8);
                return Jsoup.parse(html);
            }
            Request req = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return null;
                String body = resp.body() != null ? resp.body().string() : "";
                return Jsoup.parse(body, url);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void downloadMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;
        try {
            Request req = new Request.Builder().url(mediaUrl).header("User-Agent", "Mozilla/5.0").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return;
                String filename = new java.io.File(new java.net.URL(mediaUrl).getPath()).getName();
                if (filename == null || filename.isEmpty()) filename = "media_" + System.currentTimeMillis();
                File out = new File(downloadDir, filename);
                try (InputStream in = resp.body().byteStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192]; int r;
                    while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
                }
                downloadedMedia.add(out.getAbsolutePath());
                callback.onProgress("Saved: " + out.getAbsolutePath());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private String generateGallery() {
        try {
            File gallery = new File(downloadDir, "gallery.html");
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset=\"utf-8\"><title>Gallery</title></head><body>\n");
            for (String p : downloadedMedia) {
                String rel = p;
                if (p.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)$")) {
                    sb.append("<div><img src=\"file://" + rel + "\" style=\"max-width:300px\"><p>" + rel + "</p></div>\n");
                } else {
                    sb.append("<div><a href=\"file://" + rel + "\">" + rel + "</a></div>\n");
                }
            }
            sb.append("</body></html>");
            java.nio.file.Files.write(gallery.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            return gallery.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private String resolveUrl(String base, String rel) {
        try {
            java.net.URI baseUri = new java.net.URI(base);
            java.net.URI resolved = baseUri.resolve(rel);
            return resolved.toString();
        } catch (Exception e) {
            return rel;
        }
    }
}
            
            // Follow links (if depth > 1)
            if (depth > 1 && visitedUrls.size() < 20) {
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    if (downloadedMedia.size() >= maxFiles) break;
                    
                    String href = link.attr("href");
                    if (!href.isEmpty() && href.startsWith("http")) {
                        String domain = extractDomain(url);
                        String linkDomain = extractDomain(href);
                        
                        if (domain.equals(linkDomain) && !visitedUrls.contains(href)) {
                            scrapeUrl(href, depth - 1, maxFiles);
                        }
                    }
                }
            }
        } catch (Exception e) {
            callback.onProgress("Error scraping " + url + ": " + e.getMessage());
        }
    }
    
    private Document fetchDocument(String url) {
        try {
            // Support local file fetch via file:// URI for WebView bypass paths
            if (url != null && url.startsWith("file://")) {
                String path = url.replaceFirst("file://", "");
                File f = new File(path);
                if (f.exists()) {
                    return Jsoup.parse(f, "UTF-8", f.getParentFile() != null ? f.getParentFile().toURI().toString() : "");
                }
                return null;
            }

            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String html = response.body().string();
                response.close();
                return Jsoup.parse(html, url);
            }
            response.close();
        } catch (Exception e) {
            callback.onProgress("Failed to fetch: " + url + " (" + e.getMessage() + ")");
        }
        return null;
    }
    
    private void downloadMedia(String mediaUrl, String type) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;
        
        new Thread(() -> {
            try {
                callback.onProgress("Downloading " + type + ": " + mediaUrl);
                
                Request request = new Request.Builder()
                    .url(mediaUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
                
                Response response = httpClient.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    response.close();
                    return;
                }
                
                // Generate filename
                String filename = generateFilename(mediaUrl);
                File file = new File(downloadDir, filename);
                
                // Save file
                byte[] bytes = response.body().bytes();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(bytes);
                }
                response.close();
                
                if (file.exists() && file.length() > 0) {
                    String mediaPath = "Download/Duke2/" + filename;
                    downloadedMedia.add(mediaPath);
                    callback.onProgress("Saved: " + filename + " (" + bytes.length + " bytes)");
                }
            } catch (Exception e) {
                callback.onProgress("Failed to download: " + mediaUrl);
            }
        }).start();
    }
    
    private String generateGallery() {
        try {
            // Wait a bit for downloads to complete
            Thread.sleep(2000);
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n");
            html.append("<head>\n");
            html.append("<meta charset='UTF-8'>\n");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            html.append("<title>Web Scraper Gallery</title>\n");
            html.append("<style>\n");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
            html.append("body { background: #0a0a0f; color: #e0e0e0; font-family: Arial; padding: 20px; }\n");
            html.append("h1 { color: #e94560; margin-bottom: 20px; }\n");
            html.append(".stats { background: #1a1a25; padding: 15px; margin-bottom: 20px; border-radius: 5px; }\n");
            html.append(".gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px; }\n");
            html.append(".item { background: #1a1a25; border-radius: 5px; overflow: hidden; text-align: center; }\n");
            html.append(".item img { width: 100%; height: 150px; object-fit: cover; display: block; }\n");
            html.append(".item video { width: 100%; height: 150px; object-fit: cover; }\n");
            html.append(".item audio { width: 100%; }\n");
            html.append(".info { padding: 10px; font-size: 12px; word-break: break-all; }\n");
            html.append("a { color: #e94560; text-decoration: none; }\n");
            html.append("a:hover { text-decoration: underline; }\n");
            html.append("</style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("<h1>🕷️ Web Scraper Gallery</h1>\n");
            html.append("<div class='stats'>\n");
            html.append("<p>Total Media Found: <strong>").append(downloadedMedia.size()).append("</strong></p>\n");
            html.append("<p>Scan Time: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</p>\n");
            html.append("</div>\n");
            html.append("<div class='gallery'>\n");
            
            for (String media : downloadedMedia) {
                String filename = new File(media).getName();
                String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
                
                html.append("<div class='item'>\n");
                
                if (IMAGE_EXTENSIONS.contains(extension)) {
                    html.append("<a href='").append(media).append("' target='_blank'>");
                    html.append("<img src='").append(media).append("' alt='").append(filename).append("'>");
                    html.append("</a>\n");
                } else if (VIDEO_EXTENSIONS.contains(extension)) {
                    html.append("<video controls><source src='").append(media).append("'></video>\n");
                } else if (AUDIO_EXTENSIONS.contains(extension)) {
                    html.append("<audio controls><source src='").append(media).append("'></audio>\n");
                } else {
                    html.append("<div style='height: 150px; display: flex; align-items: center; justify-content: center;'>\n");
                    html.append("<span>📄</span>\n");
                    html.append("</div>\n");
                }
                
                html.append("<div class='info'>\n");
                html.append("<a href='").append(media).append("' target='_blank'>").append(filename).append("</a>\n");
                html.append("</div>\n");
                html.append("</div>\n");
            }
            
            html.append("</div>\n");
            html.append("</body>\n");
            html.append("</html>\n");
            
            File galleryFile = new File(downloadDir, "gallery.html");
            try (FileWriter writer = new FileWriter(galleryFile)) {
                writer.write(html.toString());
            }
            
            return galleryFile.getAbsolutePath();
        } catch (Exception e) {
            callback.onProgress("Error generating gallery: " + e.getMessage());
            return null;
        }
    }
    
    private String resolveUrl(String baseUrl, String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }
            if (url.startsWith("//")) {
                String protocol = baseUrl.split("://")[0];
                return protocol + ":" + url;
            }
            if (url.startsWith("/")) {
                URL base = new URL(baseUrl);
                return base.getProtocol() + "://" + base.getHost() + url;
            }
            return new URL(new URL(baseUrl), url).toString();
        } catch (Exception e) {
            return url;
        }
    }
    
    private String extractDomain(String url) {
        try {
            URL u = new URL(url);
            return u.getHost();
        } catch (Exception e) {
            return "";
        }
    }
    
    private String generateFilename(String url) {
        try {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf('?'));
            }
            if (filename.isEmpty() || filename.length() > 100) {
                filename = "media_" + System.currentTimeMillis() + ".tmp";
            }
            return filename;
        } catch (Exception e) {
            return "media_" + System.currentTimeMillis() + ".tmp";
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void stop() {
        isRunning = false;
    }
}

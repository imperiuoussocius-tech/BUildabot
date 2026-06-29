# Web Scraper Desktop - Windows GUI Application

## 🎉 Features

- **Standalone Desktop Application** - No command line or terminal required
- **Graphical User Interface** - Easy-to-use desktop app
- **Multi-threaded Downloads** - Fast concurrent media extraction
- **Cloudflare Bypass** - Multiple bypass engines for protected websites:
  - **curl_cffi** (recommended) - TLS fingerprint impersonation
  - **cloudscraper** - JavaScript challenge solver
  - **standard** - Basic requests library
- **Media Extraction** - Images, Videos, Audio, Documents
- **Auto Gallery** - HTML gallery generation with preview
- **Deep Crawling** - Multi-page site traversal
- **Lazy Load Support** - Detects data-src and other lazy attributes
- **Progress Tracking** - Real-time download status

## 📥 Installation

### Option 1: Pre-Built Executable (Recommended)
1. Download `WebScraper.exe` from GitHub releases
2. Double-click to run (no installation needed)
3. No dependencies to install

### Option 2: Build from Source
1. Install Python 3.8+ from https://www.python.org/
2. Clone this repository
3. Open terminal in `windows_scraper` folder
4. Run: `build.bat`
5. Find executable in `dist\WebScraper\WebScraper.exe`

### Option 3: Run Python Script Directly
```bash
pip install -r requirements.txt
python web_scraper_gui.py
```

## 🚀 Usage

### Basic Usage
1. **Enter URL** - Copy website URL into "Target URL" field
2. **Configure** - Set crawl depth and max files to download
3. **Select Bypass** - Choose Cloudflare bypass method (curl_cffi recommended)
4. **Choose Folder** - Select where to save downloaded files
5. **Click Start** - Begin scraping

### Settings Explained

| Setting | Range | Description |
|---------|-------|-------------|
| **Crawl Depth** | 1-4 | How many page levels to traverse |
| **Max Files** | 10-100 | Maximum media files to download |
| **Cloudflare Bypass** | curl_cffi / cloudscraper / standard | Protection bypass method |
| **Download Location** | Any folder | Where files and gallery.html are saved |

### Example Scenarios

**Light Scrape (Quick & Small)**
- URL: https://example.com
- Depth: 1
- Max Files: 20

**Thorough Scrape (Deep & Complete)**
- URL: https://example.com
- Depth: 3
- Max Files: 100

**Protected Site (Cloudflare/WAF)**
- Bypass: curl_cffi (recommended)
- Depth: 2
- Max Files: 50

## 📊 Output

### Downloaded Files
- **Images** - .jpg, .png, .gif, .webp, .svg, .webp, .bmp, .ico, .tiff
- **Videos** - .mp4, .webm, .mkv, .flv, .avi, .mov
- **Audio** - .mp3, .ogg, .wav, .flac, .aac, .m4a
- **Documents** - .pdf, .docx, .txt, .epub, .doc
- **Archives** - .zip, .rar, .7z, .tar, .gz

### Gallery File
- `gallery.html` - Opens in any web browser
- Responsive grid layout
- Preview for images/videos
- Direct download links
- File information (size, type)

## 🛡️ Cloudflare Bypass Explanation

### curl_cffi (Recommended)
- **How it works:** Impersonates TLS fingerprints of real browsers
- **Best for:** Sites using Cloudflare with TLS fingerprinting
- **Speed:** Very fast
- **Requires:** curl_cffi library

### cloudscraper
- **How it works:** Solves Cloudflare JavaScript challenges
- **Best for:** JavaScript-protected pages
- **Speed:** Slower than curl_cffi
- **Requires:** cloudscraper library

### Standard (Requests)
- **How it works:** Standard HTTP requests
- **Best for:** Unprotected sites
- **Speed:** Fastest
- **Requires:** Nothing extra

## ⚠️ Legal Notice

**Respect robots.txt** - Check website's robots.txt before scraping
**Review Terms of Service** - Ensure scraping complies with ToS
**Respect Rate Limits** - Don't overload servers with requests
**Content Usage** - Verify you have right to download/use content

## 🐛 Troubleshooting

### "Module not found" Error
**Solution:** Run `pip install -r requirements.txt`

### "Failed to download" Messages
**Possible causes:**
- Website blocks connections → Use VPN
- File no longer exists → Try different site
- Protected content → Increase bypass strength

### App Crashes on Launch
**Solution:** 
1. Uninstall: `pip uninstall PySimpleGUI`
2. Reinstall: `pip install PySimpleGUI`

### No Files Downloaded
**Check:**
1. URL is correct and accessible
2. Website has media (images/videos)
3. Bypass method working (check logs)
4. Download folder exists and is writable

## 📖 Advanced Tips

### Optimize for Specific Sites
```
Instagram → Depth: 1, Max: 50, Bypass: curl_cffi
YouTube → Depth: 1, Max: 20, Bypass: cloudscraper
Blog → Depth: 2, Max: 100, Bypass: standard
```

### Command Line (Advanced)
```python
from web_scraper_gui import WebScraperEngine

engine = WebScraperEngine(bypass_engine="curl_cffi")
engine.scrape("https://example.com", depth=2, max_files=50, 
              download_dir="C:/Downloads/scraped")
```

## 📋 System Requirements

- **OS:** Windows 7 or later
- **RAM:** 2 GB minimum (4 GB recommended)
- **Disk:** 500 MB for application + media space
- **Internet:** Stable connection required
- **Python:** 3.8+ (only for source build)

## 🔄 Updates

Check GitHub releases for:
- New bypass methods
- Performance improvements
- Bug fixes
- New file type support

## 📝 License

Open source - Free to use and modify

## 🤝 Support

For issues:
1. Check Troubleshooting section
2. Review output log for error messages
3. Try different bypass method
4. Report bug on GitHub

## 🚀 Version History

### v3.0.2
- Initial Windows GUI release
- curl_cffi integration
- Multi-threaded downloads
- HTML gallery generation

---

**Made with ❤️ for web enthusiasts**  
No terminal. No installation hassles. Just click and scrape!

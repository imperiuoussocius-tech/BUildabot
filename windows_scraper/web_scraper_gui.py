#!/usr/bin/env python3
"""
Web Scraper Desktop - Windows GUI Application
Standalone web scraper with Cloudflare bypass support
No terminal or external dependencies required
"""

import os
import sys
import threading
import json
from pathlib import Path
from typing import List, Dict
from urllib.parse import urljoin, urlparse
from queue import Queue

# Try to import PySimpleGUI, fallback to tkinter
try:
    import PySimpleGUI as sg
    USE_PSGUI = True
except ImportError:
    USE_PSGUI = False
    import tkinter as tk
    from tkinter import ttk, filedialog, messagebox, scrolledtext
    from tkinter import simpledialog

import requests
from bs4 import BeautifulSoup
from pathlib import Path

# Cloudflare Bypass Engines
try:
    from curl_cffi import requests as cf_requests
    BYPASS_ENGINE = "curl_cffi"
    print("[+] curl_cffi loaded - TLS fingerprint impersonation ACTIVE")
except ImportError:
    BYPASS_ENGINE = "requests"
    try:
        import cloudscraper
        BYPASS_ENGINE = "cloudscraper"
        print("[+] cloudscraper loaded - Cloudflare JS challenge bypass ACTIVE")
    except ImportError:
        print("[!] Standard requests mode - limited Cloudflare bypass")

# ─────────────────────────────────────────────────────────────────────────
# SCRAPER ENGINE
# ─────────────────────────────────────────────────────────────────────────

MEDIA_EXTENSIONS = {
    'images': ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp', '.ico', '.tiff', '.avif'],
    'videos': ['.mp4', '.webm', '.mkv', '.flv', '.avi', '.mov', '.m4v', '.3gp', '.ogv'],
    'audio': ['.mp3', '.ogg', '.wav', '.flac', '.aac', '.m4a', '.opus', '.wma'],
    'documents': ['.pdf', '.epub', '.docx', '.txt', '.doc', '.rtf', '.odt'],
    'archives': ['.zip', '.rar', '.7z', '.tar', '.gz', '.bz2', '.xz'],
}

LAZY_ATTRIBUTES = [
    'data-src', 'data-lazy-src', 'data-original', 'data-url', 'data-full',
    'data-large', 'data-hd', 'data-source', 'data-poster', 'data-video',
    'data-audio', 'data-file', 'data-thumb', 'data-preview', 'data-media',
    'ng-src', 'v-lazy', 'srcset'
]


class WebScraperEngine:
    def __init__(self, progress_callback=None, bypass_engine="curl_cffi"):
        self.progress_callback = progress_callback
        self.bypass_engine = bypass_engine
        self.visited_urls = set()
        self.downloaded_media = []
        self.session = self._create_session()
        self.is_running = False

    def _create_session(self):
        """Create HTTP session with appropriate bypass engine"""
        if self.bypass_engine == "curl_cffi":
            try:
                return cf_requests.Session()
            except:
                pass
        
        if self.bypass_engine == "cloudscraper":
            try:
                return cloudscraper.create_scraper()
            except:
                pass
        
        session = requests.Session()
        session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        return session

    def log(self, message):
        if self.progress_callback:
            self.progress_callback(message)
        print(f"[*] {message}")

    def scrape(self, start_url: str, max_depth: int, max_files: int, download_dir: str):
        """Start scraping"""
        self.is_running = True
        self.visited_urls.clear()
        self.downloaded_media.clear()
        
        try:
            Path(download_dir).mkdir(parents=True, exist_ok=True)
            self.log(f"Starting scrape: {start_url}")
            self._crawl(start_url, max_depth, max_files, download_dir)
            self.log(f"Found {len(self.downloaded_media)} media files")
            self._generate_gallery(download_dir)
            self.log("✓ Scraping complete!")
            return True
        except Exception as e:
            self.log(f"✗ Error: {str(e)}")
            return False

    def _crawl(self, url: str, depth: int, max_files: int, download_dir: str):
        """Recursive URL crawler"""
        if not self.is_running or depth <= 0 or len(self.downloaded_media) >= max_files:
            return
        
        if url in self.visited_urls:
            return
        
        self.visited_urls.add(url)
        
        try:
            self.log(f"Crawling: {url} (depth: {depth})")
            
            response = self.session.get(url, timeout=10)
            response.encoding = response.apparent_encoding or 'utf-8'
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # Extract images
            for img in soup.find_all('img'):
                if len(self.downloaded_media) >= max_files:
                    break
                src = img.get('src', '') or img.get('data-src', '') or img.get('data-lazy-src', '')
                if src:
                    media_url = urljoin(url, src)
                    self._download_media(media_url, 'image', download_dir)
            
            # Extract videos
            for video in soup.find_all(['video', 'source']):
                if len(self.downloaded_media) >= max_files:
                    break
                src = video.get('src', '') or video.get('data-video', '')
                if src:
                    media_url = urljoin(url, src)
                    self._download_media(media_url, 'video', download_dir)
            
            # Extract audio
            for audio in soup.find_all(['audio', 'source']):
                if len(self.downloaded_media) >= max_files:
                    break
                src = audio.get('src', '') or audio.get('data-audio', '')
                if src:
                    media_url = urljoin(url, src)
                    self._download_media(media_url, 'audio', download_dir)
            
            # Follow links
            if depth > 1:
                base_domain = urlparse(url).netloc
                for link in soup.find_all('a', href=True):
                    if len(self.downloaded_media) >= max_files or len(self.visited_urls) > 30:
                        break
                    
                    href = link['href']
                    if href.startswith('http'):
                        link_domain = urlparse(href).netloc
                        if base_domain in link_domain and href not in self.visited_urls:
                            self._crawl(href, depth - 1, max_files, download_dir)
        
        except Exception as e:
            self.log(f"Error crawling {url}: {str(e)}")

    def _download_media(self, url: str, media_type: str, download_dir: str):
        """Download media file"""
        try:
            self.log(f"Downloading {media_type}: {url}")
            response = self.session.get(url, timeout=10)
            
            if response.status_code == 200:
                filename = os.path.basename(urlparse(url).path)
                if not filename or len(filename) > 100:
                    filename = f"media_{len(self.downloaded_media)}{self._get_extension(url)}"
                
                filepath = os.path.join(download_dir, filename)
                with open(filepath, 'wb') as f:
                    f.write(response.content)
                
                self.downloaded_media.append({
                    'path': filepath,
                    'type': media_type,
                    'size': len(response.content),
                    'url': url
                })
                self.log(f"✓ Saved: {filename}")
        except Exception as e:
            self.log(f"Failed to download {url}: {str(e)}")

    def _get_extension(self, url: str) -> str:
        """Get file extension from URL"""
        path = urlparse(url).path
        if '.' in path:
            return path[path.rfind('.'):]
        return '.bin'

    def _generate_gallery(self, download_dir: str):
        """Generate HTML gallery"""
        html = '''<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Web Scraper Gallery</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            background: #0a0a0f; 
            color: #e0e0e0; 
            font-family: 'Segoe UI', Arial, sans-serif; 
            padding: 30px;
        }
        .container { max-width: 1400px; margin: 0 auto; }
        h1 { color: #e94560; margin-bottom: 10px; font-size: 2.5em; }
        .stats {
            background: #1a1a25; 
            padding: 20px; 
            margin-bottom: 30px; 
            border-radius: 8px;
            border-left: 4px solid #e94560;
        }
        .stats p { margin: 5px 0; }
        .gallery {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 20px;
        }
        .item {
            background: #1a1a25;
            border-radius: 8px;
            overflow: hidden;
            text-align: center;
            transition: transform 0.2s;
        }
        .item:hover { transform: scale(1.05); }
        .item img, .item video { 
            width: 100%; 
            height: 180px; 
            object-fit: cover; 
            display: block; 
        }
        .item audio { width: 100%; }
        .info {
            padding: 15px;
            font-size: 12px;
            word-break: break-all;
            background: #0f0f15;
        }
        a { color: #e94560; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .type-badge {
            display: inline-block;
            background: #e94560;
            color: white;
            padding: 3px 8px;
            border-radius: 3px;
            font-size: 10px;
            margin-bottom: 5px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🕷️ Web Scraper Gallery</h1>
        <div class="stats">
            <p><strong>Total Media:</strong> {total} files</p>
            <p><strong>Generated:</strong> {timestamp}</p>
            <p><strong>Location:</strong> {location}</p>
        </div>
        <div class="gallery">
'''
        
        for media in self.downloaded_media:
            filename = os.path.basename(media['path'])
            rel_path = os.path.relpath(media['path'], download_dir)
            
            html += '<div class="item">\n'
            
            if media['type'] == 'image':
                html += f'<a href="{rel_path}" target="_blank"><img src="{rel_path}" alt="{filename}"></a>\n'
            elif media['type'] == 'video':
                html += f'<video controls><source src="{rel_path}"></video>\n'
            elif media['type'] == 'audio':
                html += f'<audio controls><source src="{rel_path}"></audio>\n'
            
            html += '<div class="info">\n'
            html += f'<div class="type-badge">{media["type"].upper()}</div>\n'
            html += f'<a href="{rel_path}" target="_blank">{filename}</a>\n'
            html += f'<div style="font-size:10px;color:#888">Size: {media["size"]/1024:.1f} KB</div>\n'
            html += '</div></div>\n'
        
        html += '''        </div>
    </div>
</body>
</html>'''
        
        from datetime import datetime
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        html = html.format(
            total=len(self.downloaded_media),
            timestamp=timestamp,
            location=download_dir
        )
        
        gallery_path = os.path.join(download_dir, 'gallery.html')
        with open(gallery_path, 'w', encoding='utf-8') as f:
            f.write(html)

    def stop(self):
        self.is_running = False


# ─────────────────────────────────────────────────────────────────────────
# PYSGUI INTERFACE
# ─────────────────────────────────────────────────────────────────────────

def create_psgui():
    """Create PySimpleGUI interface"""
    sg.theme('DarkGrey13')
    
    layout = [
        [sg.Text('🕷️ Web Scraper Desktop', font=('Arial', 20, 'bold'), text_color='#e94560')],
        [sg.Text('Standalone web scraper - No terminal required', font=('Arial', 10), text_color='#888888')],
        [sg.Separator()],
        [sg.Text('Target URL:', font=('Arial', 10, 'bold'))],
        [sg.InputText('https://example.com', key='url', size=(50, 1), font=('Arial', 10))],
        [sg.Separator()],
        [sg.Text('Configuration:', font=('Arial', 10, 'bold'))],
        [sg.Text('Crawl Depth:'), sg.Slider(range=(1, 4), default_value=1, orientation='h', size=(20, 15), key='depth')],
        [sg.Text('Max Files:'), sg.Slider(range=(10, 100), default_value=50, orientation='h', size=(20, 15), key='max_files', resolution=10)],
        [sg.Text('Cloudflare Bypass:'), sg.Combo(['curl_cffi (recommended)', 'cloudscraper', 'standard'], 
                                                   default_value='curl_cffi (recommended)', key='bypass')],
        [sg.Separator()],
        [sg.Text('Download Location:', font=('Arial', 10, 'bold'))],
        [sg.InputText(str(Path.home() / 'Downloads' / 'WebScraper'), key='download_dir', size=(40, 1)),
         sg.FolderBrowse()],
        [sg.Separator()],
        [sg.Multiline(size=(60, 15), key='output', disabled=True, font=('Courier', 9))],
        [sg.Button('▶️ Start Scraping', size=(15, 2), font=('Arial', 11, 'bold'), button_color=('#ffffff', '#e94560')),
         sg.Button('⏹️ Stop', size=(15, 2), font=('Arial', 11, 'bold')),
         sg.Button('📁 Open Folder', size=(15, 2), font=('Arial', 11, 'bold')),
         sg.Button('Exit', size=(15, 2), font=('Arial', 11, 'bold'))],
    ]
    
    window = sg.Window('Web Scraper Desktop v3.0.2', layout, finalize=True)
    
    scraper = None
    scraper_thread = None
    
    def log_message(message):
        current = window['output'].get()
        window['output'].update(current + message + '\n')
        window['output'].see(tk.END)
    
    while True:
        event, values = window.read(timeout=100)
        
        if event == sg.WINDOW_CLOSED or event == 'Exit':
            if scraper:
                scraper.stop()
            break
        
        if event == '▶️ Start Scraping':
            url = values['url'].strip()
            if not url:
                sg.popup_error('Please enter a URL')
                continue
            
            depth = int(values['depth'])
            max_files = int(values['max_files'])
            download_dir = values['download_dir']
            bypass = values['bypass'].split(' ')[0]
            
            window['output'].update('')
            log_message(f"Starting scraper with {bypass} bypass engine...\n")
            
            scraper = WebScraperEngine(log_message, bypass)
            
            def run_scraper():
                scraper.scrape(url, depth, max_files, download_dir)
            
            scraper_thread = threading.Thread(target=run_scraper, daemon=True)
            scraper_thread.start()
        
        if event == '⏹️ Stop':
            if scraper:
                scraper.stop()
                log_message('Stopped by user.')
        
        if event == '📁 Open Folder':
            folder = values['download_dir']
            if os.path.exists(folder):
                os.startfile(folder) if sys.platform == 'win32' else os.system(f'xdg-open "{folder}"')
    
    window.close()


# ─────────────────────────────────────────────────────────────────────────
# TKINTER INTERFACE (Fallback)
# ─────────────────────────────────────────────────────────────────────────

class WebScraperGUI:
    def __init__(self, root):
        self.root = root
        self.root.title('Web Scraper Desktop v3.0.2')
        self.root.geometry('700x900')
        self.root.configure(bg='#0a0a0f')
        
        self.scraper = None
        self.scraper_thread = None
        
        # Title
        title = tk.Label(root, text='🕷️ Web Scraper Desktop', font=('Arial', 18, 'bold'),
                        bg='#0a0a0f', fg='#e94560')
        title.pack(pady=10)
        
        subtitle = tk.Label(root, text='Standalone web scraper - No terminal required',
                           font=('Arial', 9), bg='#0a0a0f', fg='#888888')
        subtitle.pack()
        
        # URL input
        tk.Label(root, text='Target URL:', font=('Arial', 10, 'bold'), bg='#0a0a0f', fg='#e94560').pack(pady=(10, 0))
        self.url_var = tk.StringVar(value='https://example.com')
        url_entry = tk.Entry(root, textvariable=self.url_var, font=('Arial', 10), width=60)
        url_entry.pack(pady=5)
        
        # Configuration
        tk.Label(root, text='Configuration:', font=('Arial', 10, 'bold'), bg='#0a0a0f', fg='#e94560').pack(pady=(10, 0))
        
        # Depth
        tk.Label(root, text='Crawl Depth: 1', font=('Arial', 9), bg='#0a0a0f', fg='#e0e0e0').pack()
        self.depth_var = tk.IntVar(value=1)
        depth_scale = tk.Scale(root, from_=1, to=4, orient=tk.HORIZONTAL, variable=self.depth_var,
                              bg='#1a1a25', fg='#e94560', length=400)
        depth_scale.pack(padx=20)
        
        # Max files
        tk.Label(root, text='Max Files: 50', font=('Arial', 9), bg='#0a0a0f', fg='#e0e0e0').pack()
        self.max_files_var = tk.IntVar(value=50)
        files_scale = tk.Scale(root, from_=10, to=100, orient=tk.HORIZONTAL, variable=self.max_files_var,
                              bg='#1a1a25', fg='#e94560', length=400)
        files_scale.pack(padx=20)
        
        # Bypass engine
        tk.Label(root, text='Cloudflare Bypass:', font=('Arial', 9), bg='#0a0a0f', fg='#e0e0e0').pack()
        self.bypass_var = tk.StringVar(value='curl_cffi')
        bypass_combo = ttk.Combobox(root, textvariable=self.bypass_var,
                                    values=['curl_cffi', 'cloudscraper', 'standard'],
                                    state='readonly', width=40)
        bypass_combo.pack(pady=5)
        
        # Download directory
        tk.Label(root, text='Download Location:', font=('Arial', 10, 'bold'), bg='#0a0a0f', fg='#e94560').pack(pady=(10, 0))
        self.download_var = tk.StringVar(value=str(Path.home() / 'Downloads' / 'WebScraper'))
        tk.Entry(root, textvariable=self.download_var, font=('Arial', 9), width=60).pack(pady=5)
        
        # Output
        tk.Label(root, text='Output Log:', font=('Arial', 10, 'bold'), bg='#0a0a0f', fg='#e94560').pack(pady=(10, 0))
        self.output = scrolledtext.ScrolledText(root, height=15, width=85, font=('Courier', 8),
                                               bg='#1a1a25', fg='#00ff00')
        self.output.pack(pady=5, padx=10)
        
        # Buttons
        button_frame = tk.Frame(root, bg='#0a0a0f')
        button_frame.pack(pady=10)
        
        tk.Button(button_frame, text='▶️ Start Scraping', font=('Arial', 10, 'bold'),
                 bg='#e94560', fg='white', command=self.start_scrape, width=18).pack(side=tk.LEFT, padx=5)
        tk.Button(button_frame, text='⏹️ Stop', font=('Arial', 10, 'bold'),
                 bg='#666666', fg='white', command=self.stop_scrape, width=15).pack(side=tk.LEFT, padx=5)
        tk.Button(button_frame, text='Exit', font=('Arial', 10, 'bold'),
                 bg='#333333', fg='white', command=root.quit, width=15).pack(side=tk.LEFT, padx=5)

    def log(self, message):
        self.output.insert(tk.END, message + '\n')
        self.output.see(tk.END)
        self.root.update()

    def start_scrape(self):
        url = self.url_var.get().strip()
        if not url:
            messagebox.showerror('Error', 'Please enter a URL')
            return
        
        depth = self.depth_var.get()
        max_files = self.max_files_var.get()
        download_dir = self.download_var.get()
        bypass = self.bypass_var.get()
        
        self.output.delete('1.0', tk.END)
        self.log(f'Starting scraper with {bypass} bypass engine...\n')
        
        self.scraper = WebScraperEngine(self.log, bypass)
        
        def run():
            self.scraper.scrape(url, depth, max_files, download_dir)
        
        self.scraper_thread = threading.Thread(target=run, daemon=True)
        self.scraper_thread.start()

    def stop_scrape(self):
        if self.scraper:
            self.scraper.stop()
            self.log('Stopped by user.')


# ─────────────────────────────────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    if USE_PSGUI:
        create_psgui()
    else:
        root = tk.Tk()
        gui = WebScraperGUI(root)
        root.mainloop()

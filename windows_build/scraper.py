import os
import threading
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse

USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
]

class Scraper:
    def __init__(self, callback=None, bypass_mode='standard'):
        self.callback = callback
        self.session = requests.Session()
        self.bypass_mode = bypass_mode
        self.visited = set()
        self.downloads = []

    def log(self, msg):
        if self.callback:
            self.callback(msg)
        else:
            print(msg)

    def fetch(self, url):
        headers = {'User-Agent': USER_AGENTS[0]}
        try:
            r = self.session.get(url, headers=headers, timeout=15)
            if r.status_code == 403 and self.bypass_mode == 'cf':
                # try cloudscraper fallback
                try:
                    import cloudscraper
                    self.log('Using cloudscraper fallback')
                    scr = cloudscraper.create_scraper()
                    r = scr.get(url, timeout=20)
                except Exception as e:
                    self.log('cloudscraper failed: %s' % e)
            return r.text if r.status_code == 200 else None
        except Exception as e:
            self.log('fetch error: %s' % e)
            return None

    def scrape(self, start_url, depth=1, max_files=50):
        self.log('Starting scraper (%s mode)...' % self.bypass_mode)
        self._scrape_url(start_url, depth, max_files)
        self.log('Generating gallery...')
        return self.generate_gallery()

    def _scrape_url(self, url, depth, max_files):
        if depth <= 0 or len(self.downloads) >= max_files or url in self.visited:
            return
        self.visited.add(url)
        self.log('Crawling: %s' % url)
        html = self.fetch(url)
        if not html:
            return
        soup = BeautifulSoup(html, 'html.parser')
        imgs = soup.find_all('img')
        for img in imgs:
            if len(self.downloads) >= max_files: break
            src = img.get('src') or img.get('data-src')
            if src:
                link = urljoin(url, src)
                self._download(link)
        if depth > 1:
            links = soup.find_all('a', href=True)
            for a in links:
                href = a['href']
                if href.startswith('http') and urlparse(href).netloc == urlparse(url).netloc:
                    self._scrape_url(href, depth-1, max_files)

    def _download(self, url):
        try:
            self.log('Downloading: %s' % url)
            r = self.session.get(url, stream=True, timeout=20)
            if r.status_code == 200:
                fname = url.split('/')[-1].split('?')[0] or ('media_%d' % len(self.downloads))
                outdir = os.path.join(os.getcwd(), 'dist_files')
                os.makedirs(outdir, exist_ok=True)
                path = os.path.join(outdir, fname)
                with open(path, 'wb') as f:
                    for chunk in r.iter_content(8192):
                        f.write(chunk)
                self.downloads.append(path)
                self.log('Saved: %s' % path)
        except Exception as e:
            self.log('download failed: %s' % e)

    def generate_gallery(self):
        outdir = os.path.join(os.getcwd(), 'dist_files')
        html = ['<!doctype html>','<html>','<head><meta charset="utf-8"><title>Gallery</title></head>','<body>','<h1>Gallery</h1>','<div>']
        for p in self.downloads:
            rel = os.path.relpath(p, outdir)
            if any(p.lower().endswith(ext) for ext in ['.jpg','.jpeg','.png','.gif','.webp']):
                html.append(f"<div><img src=\"{rel}\" style=\"max-width:300px\"><p>{os.path.basename(p)}</p></div>")
            else:
                html.append(f"<div><a href=\"{rel}\">{os.path.basename(p)}</a></div>")
        html.extend(['</div>','</body>','</html>'])
        gallery = os.path.join(outdir, 'gallery.html')
        with open(gallery,'w',encoding='utf-8') as f:
            f.write('\n'.join(html))
        self.log('Gallery written: %s' % gallery)
        return gallery

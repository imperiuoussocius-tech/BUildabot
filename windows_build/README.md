# Web Scraper - Windows EXE

This folder contains a standalone Windows GUI version of the Web Scraper.

Prerequisites (for building):
- Python 3.10+
- Install requirements:

```
python -m pip install -r requirements.txt
```

Run locally:

```
python gui.py
```

Build EXE with PyInstaller:

```
pyinstaller --onefile --windowed gui.py --add-data "dist_files;dist_files"
```

Notes:
- The EXE includes `cloudscraper` for Cloudflare bypass when `cf` mode is selected.
- Gallery files are written to `dist_files` next to the executable.

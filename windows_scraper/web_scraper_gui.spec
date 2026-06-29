# PyInstaller spec file for Web Scraper Desktop
# Usage: pyinstaller web_scraper_gui.spec

block_cipher = None

a = Analysis(
    ['web_scraper_gui.py'],
    pathex=[],
    binaries=[],
    datas=[],
    hiddenimports=[
        'PySimpleGUI',
        'requests',
        'bs4',
        'curl_cffi',
        'cloudscraper',
    ],
    hookspath=[],
    runtime_hooks=[],
    excludedimports=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='WebScraper',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,
    icon=None,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='WebScraper'
)

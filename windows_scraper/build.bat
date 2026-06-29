@echo off
REM Web Scraper Desktop - Build Script for Windows
REM Creates standalone executable using PyInstaller

echo.
echo ====================================================================
echo  Web Scraper Desktop - PyInstaller Build Script
echo ====================================================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.8+ from https://www.python.org/
    pause
    exit /b 1
)

echo [1] Installing dependencies...
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python -m pip install pyinstaller

echo.
echo [2] Building executable...
pyinstaller web_scraper_gui.spec --distpath ./dist --buildpath ./build

if exist "dist\WebScraper\WebScraper.exe" (
    echo.
    echo ====================================================================
    echo SUCCESS! Executable created at:
    echo   dist\WebScraper\WebScraper.exe
    echo.
    echo To run:
    echo   1. Double-click WebScraper.exe
    echo   2. Or run: python -m web_scraper_gui.py
    echo ====================================================================
    echo.
    pause
) else (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

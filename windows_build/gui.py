import PySimpleGUI as sg
from scraper import Scraper
import threading
import os

layout = [
    [sg.Text('Web Scraper - Windows')],
    [sg.Text('URL', size=(8,1)), sg.Input(key='-URL-')],
    [sg.Text('Depth', size=(8,1)), sg.Slider(range=(1,4), orientation='h', size=(20,15), default_value=1, key='-DEP-')],
    [sg.Text('Max Files', size=(8,1)), sg.Slider(range=(10,100), resolution=10, orientation='h', size=(20,15), default_value=50, key='-MAX-')],
    [sg.Text('Bypass Mode', size=(8,1)), sg.Combo(['standard','cf'], default_value='standard', key='-BYP-')],
    [sg.Multiline(size=(60,15), key='-OUT-')],
    [sg.Button('Start'), sg.Button('Open Gallery'), sg.Button('Exit')]
]

window = sg.Window('Web Scraper', layout)

scr = None

def log(msg):
    window['-OUT-'].update(msg + '\n', append=True)

while True:
    event, values = window.read(timeout=100)
    if event == sg.WIN_CLOSED or event == 'Exit':
        break
    if event == 'Start':
        url = values['-URL-']
        depth = int(values['-DEP-'])
        maxf = int(values['-MAX-'])
        bypass = values['-BYP-']
        window['-OUT-'].update('')
        def run():
            global scr
            scr = Scraper(callback=log, bypass_mode=bypass)
            gallery = scr.scrape(url, depth, maxf)
            log('Done. Gallery: %s' % gallery)
        threading.Thread(target=run, daemon=True).start()
    if event == 'Open Gallery':
        path = os.path.join(os.getcwd(),'dist_files','gallery.html')
        if os.path.exists(path):
            os.startfile(path)
        else:
            log('No gallery found')

window.close()

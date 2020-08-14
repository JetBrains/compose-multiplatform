#!/usr/bin/env python3

"""
Script to download Android vector drawables from fonts.google.com into the raw-icons directory.
Usage: python3 download_material_icons.py
"""

import urllib.request
import json
import pathlib
import os
import os.path

# Mapping from font themes to our preferred theme names
THEME_MAPPING = {
    'materialicons' : 'filled',
    'materialiconsoutlined' : 'outlined',
    'materialiconsround' : 'rounded',
    'materialiconstwotone' : 'twotone',
    'materialiconssharp' : 'sharp'
}

METADATA_URL = 'http://fonts.google.com/metadata/icons'

ICONS_DIR = 'raw-icons'

def get_metadata():
    """Returns the metadata file that contains all the available icons"""
    with urllib.request.urlopen(METADATA_URL) as response:
        # The first line contains )]}' used for protection against XSS attacks
        sanitized_response = b'\n'.join(response.readlines()[1:])
        return json.loads(sanitized_response)

def download_icon(name, url, theme):
    """Downloads and saves this url to the correct icon file for the given theme"""
    with urllib.request.urlopen(url) as response:
        icon = response.read()
        # E.g raw-icons/filled/menu.xml
        filepath = ICONS_DIR + '/' + theme + '/' + name + '.xml'
        print("Writing to: ", filepath)
        # Ensure all parent directories exist
        pathlib.Path(filepath).parent.mkdir(parents=True, exist_ok=True)
        with open(filepath, 'wb') as output:
            output.write(icon)

def get_icon_info(family_name, icons, host, asset_url_pattern):
    """Returns a list containing tuples of icon names and their URLs"""
    icon_info = []
    for icon in icons:
        if family_name not in icon['unsupported_families']:
            name = icon['name']
            url_params = {
                'family' : family_name,
                'icon' : name,
                'version' : icon['version'],
                'asset' : '24px.xml'
            }
            info = (name, 'http://' + host + asset_url_pattern.format(**url_params))
            icon_info.append(info)
    return icon_info

def main():
    """Do the thing"""
    metadata = get_metadata()
    asset_url_pattern = metadata['asset_url_pattern']
    host = metadata['host']
    icons = metadata['icons']
    for family in metadata['families']:
        family_name = family.lower().replace(" ", "")
        icon_info = get_icon_info(family_name, icons, host, asset_url_pattern)
        theme = THEME_MAPPING[family_name]
        for info in icon_info:
            name = info[0]
            url = info[1]
            download_icon(name, url, theme)

if __name__ == '__main__':
    # Change into script directory
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    main()

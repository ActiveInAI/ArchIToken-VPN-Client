#!/usr/bin/env bash
set -euo pipefail

python -m pip install --upgrade pip
python -m pip install -e . pyinstaller
python -m PyInstaller --noconfirm --onefile --name ArchIToken-VPN-Client \
  --collect-submodules tkinter \
  packaging/pyinstaller_entry.py

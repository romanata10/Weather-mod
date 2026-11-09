#!/usr/bin/env python3
"""Capture a complete Xvfb framebuffer atomically (diagnostic, not visual approval)."""
from PIL import ImageGrab
from pathlib import Path
import os
import sys

path = Path(sys.argv[1] if len(sys.argv) > 1 else 'client-screen.png')
temporary = path.with_name(path.stem + '.tmp.png')
ImageGrab.grab(xdisplay=os.environ['DISPLAY']).save(temporary)
os.replace(temporary, path)

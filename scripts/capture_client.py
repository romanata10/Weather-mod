#!/usr/bin/env python3
"""Capture Xvfb client framebuffer in CI; image is diagnostic, not visual approval."""
from PIL import ImageGrab
import os
# Publish atomically: the workflow must not open a PNG while Pillow is writing it.
ImageGrab.grab(xdisplay=os.environ['DISPLAY']).save('client-screen.tmp.png')
os.replace('client-screen.tmp.png','client-screen.png')

#!/usr/bin/env python3
"""Capture Xvfb client framebuffer in CI; image is diagnostic, not visual approval."""
from PIL import ImageGrab
import os
ImageGrab.grab(xdisplay=os.environ['DISPLAY']).save('client-screen.png')

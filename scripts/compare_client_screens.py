#!/usr/bin/env python3
"""Diagnostic only: summarize world-frame pixel changes, not an acceptance test."""
from PIL import Image, ImageChops, ImageStat
from pathlib import Path

baseline, active = map(Path, ('client-baseline.png', 'client-screen.png'))
if not baseline.is_file() or not active.is_file():
    print('::warning::Missing before/after client framebuffer pair')
    raise SystemExit(0)
with Image.open(baseline) as before, Image.open(active) as after:
    if before.size != after.size:
        print('::warning::Client framebuffer dimensions changed between captures')
        raise SystemExit(0)
    width, height = before.size
    # Central gameplay area: exclude the hotbar and top-right first-join toast.
    area = (int(width * .32), int(height * .27), int(width * .67), int(height * .69))
    delta = ImageChops.difference(before.convert('RGB').crop(area),
                                  after.convert('RGB').crop(area))
    changed = sum(max(pixel) > 32 for pixel in delta.getdata())
    total = delta.width * delta.height
    mean = sum(ImageStat.Stat(delta).mean) / 3
    print(f'::notice::Central framebuffer change: {changed}/{total} pixels '
          f'({100*changed/total:.1f}%), mean RGB delta {mean:.1f}. '
          'Includes moving PMWeather clouds and camera motion; NOT proof of funnel shape.')

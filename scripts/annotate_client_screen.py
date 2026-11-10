#!/usr/bin/env python3
"""Lossy CI screenshot preview in bounded check annotations (not a release asset)."""
import base64
import sys
from io import BytesIO
from pathlib import Path
from PIL import Image
path=Path(sys.argv[1] if len(sys.argv)>1 else 'client-screen.png')
prefix=('ClientBaseline' if path.name=='client-baseline.png' else
        'CycloneScreen' if path.name=='cyclone-screen.png' else
        'SpriteScreen' if path.name=='sprite-screen.png' else 'ClientScreen')
if not path.is_file():
    raise SystemExit(0)
img=Image.open(path).convert('RGB')
img.thumbnail((480,270))
compressed=BytesIO()
img.save(compressed,format='JPEG',quality=34,optimize=True)
encoded=base64.b64encode(compressed.getvalue()).decode('ascii')
chunks=[encoded[i:i+3500] for i in range(0,len(encoded),3500)]
if len(chunks)>5:
    print('::warning::Screen preview too large for CI annotations')
else:
    for index,part in enumerate(chunks,1):
        print(f'::notice title={prefix}_{index}_of_{len(chunks)}::{part}')

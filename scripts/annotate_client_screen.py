#!/usr/bin/env python3
"""Lossy CI screenshot preview in bounded check annotations (not a release asset)."""
import base64
from io import BytesIO
from pathlib import Path
from PIL import Image
path=Path('client-screen.png')
if not path.is_file():
    raise SystemExit(0)
img=Image.open(path).convert('RGB')
img.thumbnail((640,360))
compressed=BytesIO()
img.save(compressed,format='JPEG',quality=58,optimize=True)
encoded=base64.b64encode(compressed.getvalue()).decode('ascii')
chunks=[encoded[i:i+12000] for i in range(0,len(encoded),12000)]
if len(chunks)>8:
    print('::warning::Screen preview too large for CI annotations')
else:
    for index,part in enumerate(chunks,1):
        print(f'::notice title=ClientScreen_{index}_of_{len(chunks)}::{part}')

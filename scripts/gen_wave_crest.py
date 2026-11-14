#!/usr/bin/env python3
"""Generate the original transparent foam/spray sprite (no borrowed texture)."""
from pathlib import Path
import math
import struct
import zlib
import binascii

size=32
pixels=[]
for y in range(size):
    row=bytearray([0])
    for x in range(size):
        dx=(x-15.5)/15.5
        # Whitecaps: irregular crests above a low, wider foam belt.
        ridge=17-3*math.sin(x*.52)-1.6*math.sin(x*1.21)
        belt=math.exp(-((y-ridge)/4.0)**2)*max(0,1-abs(dx)**3)
        spray=max(0,math.sin(x*.75+y*.4))*.35*math.exp(-((y-ridge+6)/3.2)**2)
        alpha=int(min(220,215*belt+115*spray))
        row.extend((220,243,255,alpha))
    pixels.append(bytes(row))

def chunk(tag,data):
    return struct.pack('>I',len(data))+tag+data+struct.pack('>I',binascii.crc32(tag+data)&0xffffffff)

path=Path(__file__).resolve().parents[1]/'src/main/resources/assets/extremeweather/textures/particle/wave_crest.png'
path.parent.mkdir(parents=True,exist_ok=True)
path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',size,size,8,6,0,0,0))+
                 chunk(b'IDAT',zlib.compress(b''.join(pixels),9))+chunk(b'IEND',b''))
print(path)

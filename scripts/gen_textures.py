#!/usr/bin/env python3
"""Original 32x32 instrument pixel art; no third-party textures or Pillow."""
from pathlib import Path
import math
import random
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/extremeweather'
TEX = ROOT / 'textures/block'
MODELS = ROOT / 'models/block'
random.seed(421)


def chunk(tag, data):
    import binascii
    return struct.pack('>I',len(data))+tag+data+struct.pack('>I',binascii.crc32(tag+data)&0xffffffff)


def png(name, pixels):
    rows=b''.join(b'\0'+b''.join(bytes((*p,255)) for p in row) for row in pixels)
    size=len(pixels)
    (TEX/f'{name}.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(rows,9))+chunk(b'IEND',b''))


def shade(c, amount):
    return tuple(max(0,min(255,x+amount)) for x in c)


def panel(kind, face):
    base={'weather_radar':(37,57,68),'anemometer':(82,104,111),'barometer':(103,75,54),'storm_siren':(82,43,49)}[kind]
    accent={'weather_radar':(65,237,178),'anemometer':(197,231,232),'barometer':(233,183,100),'storm_siren':(242,80,74)}[kind]
    size=32
    out=[]
    for y in range(size):
        row=[]
        for x in range(size):
            edge=min(x,y,31-x,31-y)
            c=shade(base,(-36 if edge<2 else 20 if edge<4 else -8 if (x+y)//4%2 else 0))
            if edge in (2,3) and (x+y)%5!=0:c=shade(base,32)
            if x in (4,27) and y in (4,27):c=(184,185,171)
            if face=='top':
                dx=x-15.5;dy=y-15.5;r=math.hypot(dx,dy);theta=math.atan2(dy,dx)
                if kind=='weather_radar':
                    if 4.6<r<5.4 or 9.6<r<10.4 or 12.5<r<13.3:c=shade(accent,-60)
                    if r<12 and (-1.4<theta<-1.1 or abs(dy)<.9 and dx>0):c=accent
                    if r<2.5:c=(187,250,213)
                elif kind=='anemometer':
                    if abs(dx)<1.5 or abs(dy)<1.5:c=accent
                    if r<4:c=(41,72,85)
                    if (x-7)**2+(y-15)**2<9 or (x-23)**2+(y-15)**2<9 or (x-15)**2+(y-7)**2<9:c=shade(accent,-25)
                elif kind=='barometer':
                    if 11<r<13:c=accent
                    if r<10:c=(27,43,55)
                    if abs(dy+dx*.65)<1 and -5<dx<6:c=accent
                    if r<2:c=(240,233,200)
                    if 7<r<9 and abs((theta*8)%3.14)<.3:c=accent
                else:
                    if r<10:c=shade(accent,-15 if r<6 else -45)
                    if r<4:c=(255,197,144)
                    if abs(dx)<2 and r<11:c=shade(accent,25)
            elif face=='side':
                if kind=='weather_radar' and 7<x<24 and 10<y<23:
                    c=(20,74,68)
                    if (x+y)%5<2:c=shade(accent,-22)
                elif kind=='anemometer' and 14<x<18 and 7<y<25:c=accent
                elif kind=='barometer' and 10<x<22 and 9<y<23:
                    c=(33,52,59)
                    if (x+y)%7<2:c=shade(accent,-25)
                elif kind=='storm_siren' and 7<x<25 and 9<y<22:
                    c=shade(accent,-30)
                    if (x+y)%6==0:c=shade(accent,22)
                if 25<y<29:c=shade(base,30)
            row.append(c)
        out.append(row)
    return out


def terrain(kind):
    out=[]
    palette={'scorched_earth':((39,34,31),(71,54,44),(112,77,53)),
             'charred_grass':((36,43,32),(55,58,42),(88,76,51)),
             'ash':((81,82,75),(118,121,112),(172,165,147))}[kind]
    for y in range(32):
        row=[]
        for x in range(32):
            noise=(x*71+y*37+x*y*13)%101
            c=palette[0] if noise<35 else palette[1] if noise<80 else palette[2]
            if kind=='scorched_earth' and (x+2*y)%17==0:c=(20,21,21)
            if kind=='ash' and (x*13+y*23)%29<2:c=(197,191,174)
            if kind=='charred_grass' and y<6 and noise<80:c=(26,33,24)
            row.append(c)
        out.append(row)
    return out

if __name__=='__main__':
    import json
    for kind in ('weather_radar','anemometer','barometer','storm_siren'):
        png(kind+'_top',panel(kind,'top'))
        png(kind+'_side',panel(kind,'side'))
        png(kind+'_bottom',panel(kind,'bottom'))
        # Name of the item model stays the same; block model uses six proper faces.
        with (MODELS/f'{kind}.json').open('w') as f:
            json.dump({'parent':'minecraft:block/cube','textures':{
                'particle':f'extremeweather:block/{kind}_side',
                'up':f'extremeweather:block/{kind}_top',
                'down':f'extremeweather:block/{kind}_bottom',
                'north':f'extremeweather:block/{kind}_side',
                'south':f'extremeweather:block/{kind}_side',
                'west':f'extremeweather:block/{kind}_side',
                'east':f'extremeweather:block/{kind}_side'}},f,indent=2)
        (TEX/f'{kind}.png').unlink(missing_ok=True)
    for kind in ('scorched_earth','charred_grass','ash'):
        png(kind,terrain(kind))

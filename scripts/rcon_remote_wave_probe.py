#!/usr/bin/env python3
"""A forceloaded ocean cod must be hit even after every player leaves the front."""
import re
import socket
import struct
import sys
import time


def exact(sock,n):
    out=b''
    while len(out)<n:
        part=sock.recv(n-len(out))
        if not part:raise ConnectionError('RCON disconnected')
        out+=part
    return out


def command(sock,index,kind,text):
    data=struct.pack('<ii',index,kind)+text.encode()+b'\0\0'
    sock.sendall(struct.pack('<i',len(data))+data)
    while True:
        packet=exact(sock,struct.unpack('<i',exact(sock,4))[0])
        if struct.unpack('<i',packet[:4])[0]==index:
            return packet[8:-2].decode(errors='replace')


with socket.create_connection(('127.0.0.1',25576),timeout=8) as sock:
    sock.settimeout(12)
    command(sock,1,3,sys.argv[1])
    command(sock,2,2,'ew clear all')
    command(sock,54,2,'kill @e[type=minecraft:cod]')  # exclude the preceding visual probe's cod
    location=command(sock,3,2,'data get entity @a[limit=1] Pos')
    match=re.search(r'\[([^]]+)\]',location)
    if not match:raise AssertionError('Could not locate joined player: '+location)
    x,_,z=[float(v.rstrip('dDfF')) for v in match.group(1).split(', ')]
    x=int(x//1);z=int(z//1)
    print('remote wave staging:',x,z,flush=True)
    command(sock,4,2,f'fillbiome {x-4} 60 {z-4} {x+4} 85 {z+4} minecraft:ocean')
    command(sock,5,2,f'fill {x-26} 62 {z-26} {x+26} 62 {z+26} minecraft:stone')
    command(sock,6,2,f'fill {x-26} 63 {z-26} {x+26} 63 {z+26} minecraft:water')
    # Force-load the origin and remote fish before moving the only player away.
    for index,xx in ((7,x),(8,x+18)):
        reply=command(sock,index,2,f'forceload add {xx} {z}')
        if 'force' not in reply.lower():raise AssertionError('Forceload failed: '+reply)
    fish=command(sock,9,2,f'summon minecraft:cod {x+18} 63 {z} {{PersistenceRequired:1b}}')
    if 'Summoned' not in fish:raise AssertionError('Remote fish missing: '+fish)
    command(sock,10,2,'execute as @a at @s run tp @s ~200 90 ~200')
    started=command(sock,11,2,f'ew disaster tsunami {x} 64 {z}')
    if 'Started tsunami' not in started:raise AssertionError('Remote wave missing: '+started)
    for i in range(30):
        time.sleep(.5)
        status=command(sock,12+i,2,f'execute positioned {x} 64 {z} run ew status')
        match=re.search(r'wave entity pushes: (\d+)',status)
        if match and int(match.group(1))>0:
            print('no-player-near-front cod impulse:',status,flush=True)
            break
    else:raise AssertionError('Forceloaded cod was not hit without a player near the front: '+status)
    command(sock,50,2,'ew clear all')
    command(sock,51,2,f'forceload remove {x} {z}')
    command(sock,52,2,f'forceload remove {x+18} {z}')
    command(sock,53,2,f'kill @e[type=minecraft:cod,x={x},y=63,z={z},distance=..50]')

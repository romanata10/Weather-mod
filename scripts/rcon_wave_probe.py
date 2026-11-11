#!/usr/bin/env python3
"""Joined-client ocean probe: a real cod must receive the tsunami front impulse."""
import re
import socket
import struct
import sys
import time


def exact(sock, n):
    data=b''
    while len(data)<n:
        part=sock.recv(n-len(data))
        if not part:raise ConnectionError('RCON disconnected')
        data+=part
    return data


def command(sock, index, kind, text):
    payload=struct.pack('<ii',index,kind)+text.encode()+b'\0\0'
    sock.sendall(struct.pack('<i',len(payload))+payload)
    while True:
        packet=exact(sock,struct.unpack('<i',exact(sock,4))[0])
        if struct.unpack('<i',packet[:4])[0]==index:
            return packet[8:-2].decode(errors='replace')


with socket.create_connection(('127.0.0.1',25576),timeout=8) as sock:
    sock.settimeout(12)
    command(sock,1,3,sys.argv[1])
    command(sock,2,2,'ew clear all')
    command(sock,3,2,'time set noon')
    command(sock,4,2,'weather clear 100000')
    command(sock,5,2,'gamemode spectator @a')
    command(sock,6,2,'execute as @a at @s run tp @s ~ 66 ~ 0 55')
    time.sleep(.4)
    at='execute as @a at @s run '
    biome=command(sock,7,2,at+'fillbiome ~-4 ~-4 ~-24 ~4 ~4 ~4 minecraft:ocean')
    print('wave ocean biome:',biome,flush=True)
    checked=command(sock,8,2,at+'execute if biome ~ ~ ~ minecraft:ocean')
    if 'passed' not in checked.lower():raise AssertionError('Viewer is not in staged ocean: '+checked)
    floor=command(sock,9,2,at+'fill ~-12 62 ~-12 ~12 62 ~12 minecraft:stone')
    water=command(sock,10,2,at+'fill ~-12 63 ~-12 ~12 63 ~12 minecraft:water')
    print('wave basin:',floor,water,flush=True)
    cod=command(sock,11,2,at+'summon minecraft:cod ~3 63 ~')
    print('wave cod:',cod,flush=True)
    if 'Summoned' not in cod:raise AssertionError('Could not stage water mob: '+cod)
    started=command(sock,12,2,at+'execute positioned ~ ~ ~-20 run ew disaster tsunami')
    print('ocean wave:',started,flush=True)
    if 'Started tsunami' not in started:raise AssertionError('Wave failed to start: '+started)
    for attempt in range(20):
        time.sleep(.5)
        status=command(sock,13+attempt,2,'ew status')
        match=re.search(r'wave entity pushes: (\d+)',status)
        if match and int(match.group(1))>0:
            print('joined-client wave physical state:',status,flush=True)
            break
    else:
        raise AssertionError('Wave front never pushed a mob in the ocean basin: '+status)

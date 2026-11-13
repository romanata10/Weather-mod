#!/usr/bin/env python3
"""Joined-client preview of a weak waterspout under ONE ordinary PM cloud."""
import re
import socket
import struct
import sys
import time


def exact(sock, n):
    out=b''
    while len(out)<n:
        part=sock.recv(n-len(out))
        if not part:raise ConnectionError('RCON disconnected')
        out+=part
    return out


def command(sock, index, kind, text):
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
    command(sock,3,2,'time set noon')
    command(sock,4,2,'weather clear 100000')
    command(sock,5,2,'gamemode spectator @a')
    command(sock,6,2,'execute as @a at @s run tp @s ~ 104 ~ 0 -5')
    time.sleep(.5)
    at='execute as @a at @s anchored eyes positioned ^ ^-8 ^20 run '
    floor=command(sock,7,2,at+'fill ~-8 ~-2 ~-8 ~8 ~-2 ~8 minecraft:stone')
    water=command(sock,8,2,at+'fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 minecraft:water')
    print('waterspout shallow pool:',floor,water,flush=True)
    cloud=command(sock,9,2,at+'execute positioned ~ ~80 ~ run ew storm supercell 70')
    print('single ordinary PM supercell:',cloud,flush=True)
    if 'Spawned PMWeather storm' not in cloud:raise AssertionError('Could not stage parent thundercloud: '+cloud)
    time.sleep(.6)
    started=command(sock,10,2,at+'ew disaster waterspout')
    print('cloud-anchored waterspout:',started,flush=True)
    if 'Started waterspout' not in started:raise AssertionError('Waterspout refused parent cloud/water: '+started)
    for attempt in range(8):
        time.sleep(.25)
        status=command(sock,11+attempt,2,'ew status')
        if 'active small vortices: 1' in status:break
    else:raise AssertionError('Waterspout disappeared before client capture: '+status)
    print('joined-client waterspout state:',status,flush=True)
    # Allow several particle lifetimes to populate the column before capture.
    time.sleep(2)

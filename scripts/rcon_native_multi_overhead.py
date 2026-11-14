#!/usr/bin/env python3
"""Second camera of the SAME PMWeather multi-vortex, without spawning another storm."""
import re
import socket
import struct
import sys
import time


def exact(sock, n):
    buf=b''
    while len(buf)<n:
        part=sock.recv(n-len(buf))
        if not part:raise ConnectionError('RCON disconnected')
        buf+=part
    return buf


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
    status=command(sock,2,2,'ew status')
    print('same native storm before camera change:',status,flush=True)
    satellites=re.search(r'native sub-vortices: (\d+)',status)
    if not satellites or int(satellites.group(1))<4:
        raise AssertionError('Native satellites expired before alternate angle: '+status)
    # Stage from above, looking down to the original funnel about 75 blocks
    # in front of the camera. Only the spectator player moves; no storm added.
    teleport=command(sock,3,2,'execute as @a at @s run tp @s ~ 180 ~ 0 55')
    print('overhead camera:',teleport,flush=True)
    if teleport and 'teleported' not in teleport.lower():
        raise AssertionError('Spectator camera not moved: '+teleport)
    time.sleep(1.2)

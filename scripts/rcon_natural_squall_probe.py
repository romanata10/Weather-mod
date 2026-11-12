#!/usr/bin/env python3
"""Joined-client probe of addon adoption of an existing PMWeather squall.

The RCON storm command only stages a PMWeather squall; it does not create an
addon SquallField. The addon must notice the same storm on its ordinary poll.
"""
import re
import socket
import struct
import sys
import time


def exact(sock, n):
    out=b''
    while len(out)<n:
        piece=sock.recv(n-len(out))
        if not piece:raise ConnectionError('RCON disconnected')
        out+=piece
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
    joined=command(sock,3,2,'list')
    print('joined players:',joined,flush=True)
    if 'There are 1 of a max' not in joined:
        raise AssertionError('Natural squall adoption requires a joined player')
    created=command(sock,4,2,'execute as @a at @s positioned ~ ~80 ~ run ew storm squall 70')
    print('one PMWeather squall:',created,flush=True)
    if 'Spawned PMWeather storm' not in created:
        raise AssertionError('Could not stage a PMWeather-owned squall: '+created)
    for attempt in range(24):
        time.sleep(.25)
        status=command(sock,5+attempt,2,'ew status')
        active=re.search(r'active squall gusts: (\d+)',status)
        if active and int(active.group(1))==1:
            print('adopted original PMWeather squall:',status,flush=True)
            break
    else:
        raise AssertionError('Existing PMWeather squall was not adopted: '+status)
    command(sock,30,2,'ew clear all')

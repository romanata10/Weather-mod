#!/usr/bin/env python3
"""CI-only: create ONE native PMWeather cyclone before a second client frame."""
import socket,sys,time
import struct

def exact(sock,count):
    data=b''
    while len(data)<count:
        data+=sock.recv(count-len(data))
    return data

def command(sock,request_id,kind,text):
    payload=struct.pack('<ii',request_id,kind)+text.encode()+b'\0\0'
    sock.sendall(struct.pack('<i',len(payload))+payload)
    while True:
        packet=exact(sock,struct.unpack('<i',exact(sock,4))[0])
        if struct.unpack('<i',packet[:4])[0]==request_id:
            return packet[8:-2].decode(errors='replace')

with socket.create_connection(('127.0.0.1',25576),timeout=8) as sock:
    sock.settimeout(12)
    command(sock,1,3,sys.argv[1])
    print('before cyclone:',command(sock,2,2,'ew status'),flush=True)
    command(sock,3,2,'ew clear all')
    # Above the storm deck, looking down from a stationary spectator camera.
    # A ground-level screenshot shows only rain/fog and cannot judge rotation.
    command(sock,6,2,'gamemode spectator @a')
    command(sock,7,2,'execute as @a at @s run tp @s ~ 240 ~ 0 65')
    time.sleep(1)
    result=command(sock,4,2,
        'execute as @a at @s anchored eyes positioned ^ ^0 ^170 run ew disaster hurricane')
    print('native cyclone:',result,flush=True)
    if 'Started hurricane' not in result:
        raise AssertionError('PMWeather native cyclone failed to spawn')
    time.sleep(5)
    status=command(sock,5,2,'ew status')
    print('after cyclone:',status,flush=True)
    if 'active cyclone systems: 1' not in status:
        raise AssertionError('Native cyclone no longer tracked during client capture')


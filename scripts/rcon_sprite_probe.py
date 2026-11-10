#!/usr/bin/env python3
"""CI-only: place a transient red sprite above one native thunderstorm."""
import socket,struct,sys,time

def exact(sock,count):
    data=b''
    while len(data)<count:
        part=sock.recv(count-len(data))
        if not part:raise ConnectionError('RCON closed')
        data+=part
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
    command(sock,2,2,'ew clear all')
    command(sock,3,2,'time set midnight')
    command(sock,4,2,'execute as @a at @s run tp @s ~ 245 ~ 0 0')
    time.sleep(.6)
    place='execute as @a at @s anchored eyes positioned ^ ^-85 ^100 run '
    storm=command(sock,5,2,place+'ew storm supercell 70')
    print('thunderstorm:',storm,flush=True)
    if 'Spawned PMWeather storm' not in storm:
        raise AssertionError('No native thunderstorm for sprite')
    sprite=command(sock,6,2,place+'ew disaster sprite')
    print('sprite:',sprite,flush=True)
    if 'Started sprite' not in sprite:
        raise AssertionError('Sprite rejected despite native storm and night')
    status=command(sock,7,2,'ew status')
    print('sprite server state:',status,flush=True)
    if 'active sprites: 1' not in status:
        raise AssertionError('Sprite not active when client capture requested')

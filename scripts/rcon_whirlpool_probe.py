#!/usr/bin/env python3
"""CI-only: build a temporary water pool and inspect the addon whirlpool."""
import socket,struct,sys,time,re

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
    command(sock,3,2,'time set noon')
    command(sock,4,2,'weather clear 100000')
    command(sock,5,2,'execute as @a at @s run tp @s ~ 100 ~ 0 55')
    time.sleep(.4)
    at='execute as @a at @s anchored eyes positioned ^ ^-10 ^20 run '
    print('water floor:',command(sock,6,2,at+'fill ~-10 ~-2 ~-10 ~10 ~-2 ~10 minecraft:stone'),flush=True)
    print('water pool:',command(sock,7,2,at+'fill ~-10 ~-1 ~-10 ~10 ~-1 ~10 minecraft:water'),flush=True)
    whirl=command(sock,8,2,at+'ew disaster whirlpool')
    print('whirlpool:',whirl,flush=True)
    if 'Started whirlpool' not in whirl:
        raise AssertionError('Failed to start whirlpool over temporary water pool')
    print('submerged cod:',command(sock,10,2,at+'summon minecraft:cod ~3 ~-1 ~'),flush=True)
    time.sleep(1.7)
    status=command(sock,9,2,'ew status')
    print('whirlpool state:',status,flush=True)
    if 'active whirlpools: 1' not in status:
        raise AssertionError('Whirlpool died before graphical screenshot')
    match=re.search(r'whirlpool pulls: (\d+)',status)
    if not match or int(match.group(1))<1:
        raise AssertionError('No submerged cod was pulled in the joined-client whirlpool world')

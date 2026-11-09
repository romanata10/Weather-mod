#!/usr/bin/env python3
"""CI-only: connect a real client, trigger a dust devil, inspect the client render log."""
import socket,struct,sys,time

def exact(sock,count):
    buf=b''
    while len(buf)<count:
        part=sock.recv(count-len(buf))
        if not part:raise ConnectionError('RCON closed')
        buf+=part
    return buf

def command(sock,request_id,kind,text):
    payload=struct.pack('<ii',request_id,kind)+text.encode()+b'\0\0'
    sock.sendall(struct.pack('<i',len(payload))+payload)
    while True:
        size=struct.unpack('<i',exact(sock,4))[0]
        packet=exact(sock,size)
        ident,typ=struct.unpack('<ii',packet[:8])
        if ident==request_id:return packet[8:-2].decode(errors='replace')

for attempt in range(70):
    try:
        with socket.create_connection(('127.0.0.1',25576),timeout=3) as sock:
            sock.settimeout(5)
            if command(sock,1,3,sys.argv[1]) is None:raise ConnectionError('RCON auth failed')
            listing=command(sock,2,2,'list')
            if attempt%12==0:print('server player list:',listing,flush=True)
            if 'There are 1 of a max' in listing or 'There are 2 of a max' in listing:
                print('joined client:',listing,flush=True)
                # Put the camera above terrain, facing a clear horizontal view.
                # Otherwise an arbitrary spawn may hide the vortex behind foliage.
                command(sock,90,2,'effect give @a minecraft:slow_falling 120 10 true')
                command(sock,91,2,'execute as @a at @s run tp @s ~ 145 ~ 0 0')
                time.sleep(2)
                particle=command(sock,3,2,'execute as @a at @s anchored eyes positioned ^ ^-1 ^16 run particle extremeweather:dust_volume ~ ~ ~ 0 0 0 0 1 force @a')
                print('direct volume spawn:',particle,flush=True)
                disaster=command(sock,4,2,'execute as @a at @s anchored eyes positioned ^ ^-1 ^16 run ew disaster dust_devil')
                print('dust devil command:',disaster,flush=True)
                if 'Started dust_devil' not in disaster:raise AssertionError('dust devil refused client position')
                # Joining at the server can precede dismissing the client's
                # Loading terrain screen. Keep emitting the short-lived volume
                # until the client actually draws it with the world GUI visible.
                for visual_attempt in range(35):
                    if visual_attempt%2==0:
                        command(sock,5+visual_attempt,2,'execute as @a at @s anchored eyes positioned ^ ^-1 ^16 run particle extremeweather:dust_volume ~ ~ ~ 0 0 0 0 1 force @a')
                    try:
                        if 'ExtremeWeather dust volume rendered with world GUI visible' in open('client-smoke.log',errors='ignore').read():
                            command(sock,80,2,'execute as @a at @s anchored eyes positioned ^ ^-1 ^16 run ew disaster dust_devil')
                            print('world GUI visible and volume renderer ran',flush=True)
                            break
                    except OSError:
                        pass
                    time.sleep(1)
                else:
                    raise AssertionError('client never rendered the volume with its world GUI visible')
                break
    except (OSError,ConnectionError) as error:
        if attempt%15==0:print('waiting for connected client:',error,flush=True)
    time.sleep(2)
else:
    raise AssertionError('No client joined the dedicated server within 140 seconds')

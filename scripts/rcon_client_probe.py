#!/usr/bin/env python3
"""CI-only: connect a real client, trigger a dust devil, inspect the client render log."""
import socket,struct,sys,time
from pathlib import Path

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
                # Place camera above a flat diagnostic world and below the cloud layer.
                # Otherwise an arbitrary spawn may hide the vortex behind foliage.
                command(sock,87,2,'gamerule doWeatherCycle false')
                command(sock,88,2,'weather clear 100000')
                command(sock,86,2,'ew clear all')
                command(sock,89,2,'time set noon')
                command(sock,90,2,'effect give @a minecraft:slow_falling 120 10 true')
                command(sock,91,2,'execute as @a at @s run tp @s ~ 85 ~ 0 0')
                # RCON sees the connection before the render thread dismisses
                # Loading terrain. A baseline taken that early is meaningless.
                for _ in range(80):
                    try:
                        if 'ExtremeWeather client gameplay ready for baseline capture' in Path('client-smoke.log').read_text(errors='ignore'):
                            break
                    except OSError:pass
                    time.sleep(.5)
                else:raise AssertionError('Client did not reach settled gameplay before baseline')
                # Frame from the same joined client before any addon particle.
                Path('baseline-now').touch()
                for _ in range(20):
                    if Path('client-baseline.png').is_file():break
                    time.sleep(.5)
                else:raise AssertionError('No pre-vortex framebuffer from joined client')
                particle=command(sock,3,2,'execute as @a at @s anchored eyes positioned ^ ^-8 ^18 run particle extremeweather:dust_volume ~ ~ ~ 0 0 0 0 1 force @a')
                print('direct volume spawn:',particle,flush=True)
                disaster=command(sock,4,2,'execute as @a at @s anchored eyes positioned ^ ^-8 ^18 run ew disaster dust_devil')
                print('dust devil command:',disaster,flush=True)
                if 'Started dust_devil' not in disaster:raise AssertionError('dust devil refused client position')
                # Joining at the server can precede dismissing the client's
                # Loading terrain screen. Keep emitting the short-lived volume
                # until the client actually draws it with the world GUI visible.
                for visual_attempt in range(35):
                    if visual_attempt%2==0:
                        command(sock,5+visual_attempt,2,'execute as @a at @s anchored eyes positioned ^ ^-8 ^18 run particle extremeweather:dust_volume ~ ~ ~ 0 0 0 0 1 force @a')
                    try:
                        if 'ExtremeWeather dust volume rendered with world GUI visible' in open('client-smoke.log',errors='ignore').read():
                            command(sock,92,2,'execute as @a at @s run tp @s ~ 85 ~ 0 45')
                            command(sock,80,2,'execute as @a at @s anchored eyes positioned ^ ^-8 ^18 run ew disaster dust_devil')
                            # count=0 encodes the raymarch volume scale in the
                            # particle's x velocity. Verify all three bounds
                            # actually reach and render on the joined client.
                            for index,scale in enumerate((.65,1,1.55),100):
                                shown=command(sock,index,2,
                                    f'execute as @a at @s anchored eyes positioned ^ ^-8 ^18 run particle extremeweather:dust_volume ~ ~ ~ {scale} 0 0 1 0 force @a')
                                if 'Displaying particle' not in shown:
                                    raise AssertionError(f'Dust size {scale} was not sent: {shown}')
                            for _ in range(20):
                                time.sleep(.2)
                                log=Path('client-smoke.log').read_text(errors='ignore')
                                if all(f'ExtremeWeather dust size rendered: {size}' in log
                                       for size in ('small','medium','large')):break
                            else:raise AssertionError('Joined client did not render all three dust volume bounds')
                            print('joined client rendered small, medium and large dust volumes',flush=True)
                            # Let all readiness-test particles expire before
                            # capturing ONE fresh raymarched billboard. Otherwise
                            # eight stacked quads fake a denser funnel than users see.
                            time.sleep(1.5)
                            command(sock,94,2,'weather clear 100000')
                            command(sock,95,2,'execute as @a at @s run tp @s ~ 85 ~ 0 0')
                            time.sleep(.4)
                            solo=command(sock,93,2,'execute as @a at @s anchored eyes positioned ^ ^-8 ^18 run particle extremeweather:dust_volume ~ ~ ~ 0 0 0 0 1 force @a')
                            print('single diagnostic volume:',solo,flush=True)
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

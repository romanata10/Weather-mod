#!/usr/bin/env python3
"""Stage ONE native PMWeather funnel in front of a joined client for visual review.

A captured framebuffer is evidence to inspect, not proof of a realistic shape.
"""
import re
import socket
import struct
import sys
import time


def exact(sock, count):
    result = b''
    while len(result) < count:
        part = sock.recv(count - len(result))
        if not part:
            raise ConnectionError('RCON closed')
        result += part
    return result


def command(sock, request_id, kind, text):
    payload = struct.pack('<ii', request_id, kind) + text.encode() + b'\0\0'
    sock.sendall(struct.pack('<i', len(payload)) + payload)
    while True:
        size = struct.unpack('<i', exact(sock, 4))[0]
        packet = exact(sock, size)
        if struct.unpack('<i', packet[:4])[0] == request_id:
            return packet[8:-2].decode(errors='replace')


shape = sys.argv[2] if len(sys.argv) > 2 else 'wedge'
if shape not in ('wedge', 'rope', 'multi_vortex'):
    raise ValueError('Only wedge, rope and multi_vortex diagnostic presets are supported')
# Compare rope and wedge with the same EF strength; otherwise their different
# winds/storm maturity confound a visual comparison of requested morphology.
ef = 4
distance = {'wedge': 90, 'rope': 45, 'multi_vortex': 75}[shape]

with socket.create_connection(('127.0.0.1', 25576), timeout=8) as sock:
    sock.settimeout(12)
    command(sock, 1, 3, sys.argv[1])
    command(sock, 2, 2, 'ew clear all')
    command(sock, 3, 2, 'weather clear 100000')
    command(sock, 4, 2, 'time set noon')
    mode = command(sock, 5, 2, 'gamemode spectator @a')
    print('spectator camera:', mode, flush=True)
    # Vanilla emits an empty RCON reply when the player was already in this
    # mode (the preceding wedge probe switched the same connected client).
    if mode and 'spectator' not in mode.lower() and 'already' not in mode.lower():
        raise AssertionError('Could not switch diagnostic camera: ' + mode)
    # Pitch the spectator down toward the funnel rather than staring across
    # the cloud deck at a level horizon. Spawn on the actual camera ray.
    teleport = command(sock, 6, 2, 'execute as @a at @s run tp @s ~ 90 ~ 0 12')
    print('camera teleport:', teleport, flush=True)
    if teleport and 'teleported' not in teleport.lower():
        raise AssertionError('Could not position diagnostic camera: ' + teleport)
    time.sleep(1)
    result = command(sock, 7, 2,
        f'execute as @a at @s anchored eyes positioned ^ ^0 ^{distance} run ew vortex {shape} {ef}')
    print(f'native {shape} command:', result, flush=True)
    if 'One native PMWeather supercell' not in result:
        raise AssertionError('Native funnel did not spawn: ' + result)
    # The first frame still contains the spectator-mode toast and the native
    # storm has not had time to advance. Capture after server/client ticks.
    time.sleep(10)
    status = command(sock, 8, 2, 'ew status')
    print(f'native {shape} state:', status, flush=True)
    if shape == 'multi_vortex':
        satellites = re.search(r'native sub-vortices: (\d+)', status)
        if not satellites or int(satellites.group(1)) < 4:
            raise AssertionError('Four native PMWeather satellites did not survive: ' + status)
    count = re.search(r'PMWeather storms nearby: (\d+)', status)
    # PMWeather may naturally spawn another storm during the ten-second wait;
    # the command itself adds only one. Do not mistake ambient weather for a
    # second funnel created by the addon.
    if not count or int(count.group(1)) < 1:
        raise AssertionError('Native PMWeather storm disappeared: ' + status)
    width = re.search(r'widest native supercell width: (\d+)', status)
    wind = re.search(r'strongest native supercell wind: (\d+)', status)
    if not width or int(width.group(1)) < (20 if shape == 'wedge' else 8):
        raise AssertionError('Native PMWeather supercell lost its funnel width: ' + status)
    if not wind or int(wind.group(1)) < 1:
        raise AssertionError('Native PMWeather storm has no tornadic wind: ' + status)

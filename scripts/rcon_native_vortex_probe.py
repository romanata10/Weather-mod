#!/usr/bin/env python3
"""Stage ONE native PMWeather funnel in front of a joined client for visual review.

A captured framebuffer is evidence to inspect, not proof of a realistic shape.
"""
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


with socket.create_connection(('127.0.0.1', 25576), timeout=8) as sock:
    sock.settimeout(12)
    command(sock, 1, 3, sys.argv[1])
    command(sock, 2, 2, 'ew clear all')
    command(sock, 3, 2, 'weather clear 100000')
    command(sock, 4, 2, 'time set noon')
    mode = command(sock, 5, 2, 'gamemode spectator @a')
    print('spectator camera:', mode, flush=True)
    if 'spectator' not in mode.lower():
        raise AssertionError('Could not switch diagnostic camera: ' + mode)
    # Pitch the spectator down toward the funnel rather than staring across
    # the cloud deck at a level horizon. Spawn on the actual camera ray.
    teleport = command(sock, 6, 2, 'execute as @a at @s run tp @s ~ 90 ~ 0 12')
    print('camera teleport:', teleport, flush=True)
    if 'teleported' not in teleport.lower():
        raise AssertionError('Could not position diagnostic camera: ' + teleport)
    time.sleep(1)
    result = command(sock, 7, 2,
        'execute as @a at @s anchored eyes positioned ^ ^0 ^90 run ew vortex wedge 4')
    print('native wedge command:', result, flush=True)
    if 'One native PMWeather supercell' not in result:
        raise AssertionError('Native funnel did not spawn: ' + result)
    time.sleep(3)
    status = command(sock, 8, 2, 'ew status')
    print('native wedge state:', status, flush=True)
    if 'PMWeather storms nearby: 1' not in status:
        raise AssertionError('Expected one native PMWeather storm: ' + status)

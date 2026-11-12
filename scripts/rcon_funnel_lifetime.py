#!/usr/bin/env python3
"""Dedicated-server probe: a live PM tornado must retain its preset past 1200 ticks."""
import re
import socket
import struct
import sys
import time


def exact(sock, n):
    data = b''
    while len(data) < n:
        chunk = sock.recv(n-len(data))
        if not chunk: raise ConnectionError('RCON disconnected')
        data += chunk
    return data


def command(sock, index, kind, text):
    payload = struct.pack('<ii', index, kind) + text.encode() + b'\0\0'
    sock.sendall(struct.pack('<i', len(payload)) + payload)
    while True:
        packet = exact(sock, struct.unpack('<i', exact(sock, 4))[0])
        if struct.unpack('<i', packet[:4])[0] == index:
            return packet[8:-2].decode(errors='replace')


with socket.create_connection(('127.0.0.1', 25575), timeout=8) as sock:
    sock.settimeout(12)
    command(sock, 1, 3, sys.argv[1])
    command(sock, 2, 2, 'ew clear all')
    command(sock, 3, 2, 'forceload add 0 0')
    spawned = command(sock, 4, 2, 'execute positioned 0 100 0 run ew vortex wedge 4')
    print('one native wedge:', spawned, flush=True)
    if 'One native PMWeather supercell' not in spawned:
        raise AssertionError('Could not stage native tornado: '+spawned)
    before = command(sock, 5, 2, 'execute positioned 0 100 0 run ew status')
    print('initial native geometry:', before, flush=True)
    if 'WEDGE target=16.00' not in before:
        raise AssertionError('Native preset was not tracked at spawn: '+before)
    start = int(re.search(r'\d+', command(sock, 6, 2, 'time query gametime')).group())
    # Poll game time, not wall time. An empty server can tick slowly or pause;
    # a 60-second sleep alone does not establish the old 1200-tick boundary.
    for attempt in range(165):
        time.sleep(.5)
        now_text = command(sock, 7+attempt, 2, 'time query gametime')
        now = int(re.search(r'\d+', now_text).group())
        if now-start > 1230:
            break
    else:
        raise AssertionError(f'Server did not advance 1230 ticks: {start} -> {now_text}')
    after = command(sock, 180, 2, 'execute positioned 0 100 0 run ew status')
    print('after 1230+ server ticks:', after, flush=True)
    if 'WEDGE target=16.00' not in after:
        raise AssertionError('Living native preset expired or PM storm died: '+after)
    width = re.search(r'widest native supercell width: (\d+)', after)
    if not width or int(width.group(1)) < 90:
        raise AssertionError('Native wedge geometry did not survive 1230+ ticks: '+after)
    command(sock, 181, 2, 'ew clear all')
    print('Native wedge retained beyond the former 1200-tick timeout', flush=True)

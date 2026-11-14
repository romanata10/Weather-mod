#!/usr/bin/env python3
"""Two-phase RCON probe for temporary flood-source SavedData across a clean restart."""
import socket
import struct
import sys
import time


def exact(sock, count):
    out = b''
    while len(out) < count:
        part = sock.recv(count-len(out))
        if not part:
            raise ConnectionError('RCON disconnected')
        out += part
    return out


def command(sock, index, kind, text):
    payload = struct.pack('<ii', index, kind) + text.encode() + b'\0\0'
    sock.sendall(struct.pack('<i', len(payload)) + payload)
    while True:
        packet = exact(sock, struct.unpack('<i', exact(sock, 4))[0])
        if struct.unpack('<i', packet[:4])[0] == index:
            return packet[8:-2].decode(errors='replace')


phase, password = sys.argv[1:3]
with socket.create_connection(('127.0.0.1', 25575), timeout=8) as sock:
    sock.settimeout(12)
    command(sock, 1, 3, password)
    def check(index, text, expected):
        result = command(sock, index, 2, text)
        print(text, repr(result), flush=True)
        if expected.lower() not in result.lower():
            raise AssertionError(f'{text}: expected {expected!r}, got {result!r}')
    if phase == 'stage':
        check(2, 'forceload add 8008 8', 'force')
        check(3, 'setblock 8008 63 8 minecraft:water', 'block')
        command(sock, 4, 2, 'setblock 8008 64 8 minecraft:air')
        check(5, 'ew disaster flood 8008 64 8', 'Started flood')
        check(6, 'execute if block 8008 64 8 minecraft:water', 'passed')
        check(7, 'save-all flush', 'Saved the game')
        # Minecraft closes RCON during /stop; the prior flushed save is what
        # matters, not whether the stop command returns a response packet.
        try:
            print('stop:', command(sock, 8, 2, 'stop'), flush=True)
        except (OSError, ConnectionError):
            print('RCON closed on clean server stop', flush=True)
    elif phase == 'verify':
        # The persisted forceload ticket should load this remote chunk again.
        check(9, 'forceload query 8008 8', 'force')
        for attempt in range(20):
            result = command(sock, 10, 2, 'execute if block 8008 64 8 minecraft:water')
            if 'passed' in result.lower():
                print('saved crest survived restart:', result, flush=True)
                break
            time.sleep(.5)
        else:
            raise AssertionError('Saved temporary source did not survive restart: '+result)
        check(11, 'ew clear all', 'PMWeather storms and addon weather cleared')
        time.sleep(1.5)
        check(12, 'execute if block 8008 64 8 minecraft:air', 'passed')
        print('saved crest removed after restart and /ew clear all', flush=True)
    else:
        raise ValueError('phase must be stage or verify')

#!/usr/bin/env python3
"""Run operator commands against the dedicated test server using vanilla RCON."""
import socket
import struct
import sys
import time


def read_exact(sock, count):
    data = b""
    while len(data) < count:
        part = sock.recv(count - len(data))
        if not part:
            raise ConnectionError("RCON disconnected")
        data += part
    return data


def send(sock, request_id, packet_type, value):
    payload = struct.pack("<ii", request_id, packet_type) + value.encode() + b"\0\0"
    sock.sendall(struct.pack("<i", len(payload)) + payload)
    while True:
        size = struct.unpack("<i", read_exact(sock, 4))[0]
        if size < 10 or size > 65536:
            raise ValueError("invalid RCON response size")
        packet = read_exact(sock, size)
        response_id, response_type = struct.unpack("<ii", packet[:8])
        if response_id != request_id:
            raise RuntimeError(f"RCON request rejected: {response_id}")
        if packet_type == 3 and response_type != 2:
            continue  # vanilla may emit an empty response before auth acknowledgement
        return packet[8:-2].decode(errors="replace")


connection = None
for _ in range(20):
    try:
        connection = socket.create_connection(("127.0.0.1", 25575), timeout=2)
        break
    except OSError:
        time.sleep(1)
if connection is None:
    raise ConnectionError("RCON port 25575 never opened; check the dedicated server log")

with connection as conn:
    conn.settimeout(12)
    send(conn, 7, 3, sys.argv[1])
    for index, (command, expected) in enumerate(
        [
            ("ew help", "/ew storm"),
            ("forceload add 0 0", "force"),
            ("execute in minecraft:overworld positioned 0 100 0 run ew storm supercell 50", "Spawned PMWeather storm"),
            ("execute in minecraft:overworld positioned 0 100 0 run ew vortex rope 1", "Native PMWeather supercell"),
        ], start=8
    ):
        reply = send(conn, index, 2, command)
        print(f"{command}: {reply}")
        if expected.lower() not in reply.lower():
            raise AssertionError(f"expected {expected!r} in RCON reply {reply!r}")
print("PMWeather integration command smoke tests passed")

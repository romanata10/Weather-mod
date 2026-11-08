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
    def check(index, command, expected):
        reply = send(conn, index, 2, command)
        print(f"{command}: {reply}", flush=True)
        if expected.lower() not in reply.lower():
            raise AssertionError(f"expected {expected!r} in RCON reply {reply!r}")

    check(8, "ew help", "/ew storm")
    check(9, "forceload add 0 0", "force")
    check(10, "weather clear", "clear")
    check(11, "setblock 0 70 0 minecraft:grass_block", "Changed")
    send(conn, 12, 2, "setblock 0 71 0 minecraft:air")  # air may already exist
    check(13, "ew ignite 0 70 0", "Wildfire ignition requested")
    # Give the server 10+ seconds to tick 200 simulated fire ticks.
    time.sleep(26)
    burned = False
    for i, block in enumerate(("extremeweather:charred_grass", "extremeweather:scorched_earth", "minecraft:grass_block", "minecraft:dirt", "minecraft:air"), start=14):
        reply = send(conn, i, 2, f"execute if block 0 70 0 {block} run say wildfire_passed")
        print(f"block probe {block}: {reply!r}", flush=True)
        if "wildfire_passed" in reply and block.startswith("extremeweather:"):
            burned = True
    if not burned:
        raise AssertionError("Wildfire did not replace grass/dirt with a scorched block after 26 seconds")
    check(21, "execute in minecraft:overworld positioned 0 100 0 run ew storm supercell 50", "Spawned PMWeather storm")
    check(22, "execute in minecraft:overworld positioned 0 100 0 run ew vortex rope 1", "Native PMWeather supercell")
print("PMWeather storm/tornado and scorched-ground integration tests passed")

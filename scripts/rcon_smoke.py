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
    print("prepare wildfire fuel:",send(conn,11,2,"setblock 0 70 0 minecraft:grass_block"),flush=True)
    check(54, "execute if block 0 70 0 minecraft:grass_block", "passed")
    send(conn, 12, 2, "setblock 0 71 0 minecraft:air")  # air may already exist
    check(13, "ew ignite 0 70 0", "Wildfire started")
    print("fire status after ignition:",send(conn, 23, 2, "ew firestatus 0 70 0"),flush=True)
    start_time = send(conn, 24, 2, "time query gametime")
    print("start time:",start_time,flush=True)
    # Give the server enough time for multiple fire spread and conversion passes.
    time.sleep(26)
    print("fire status after 26s:",send(conn, 25, 2, "ew firestatus 0 70 0"),flush=True)
    print("end time:",send(conn, 26, 2, "time query gametime"),flush=True)
    burned = False
    for i, block in enumerate(("extremeweather:charred_grass", "extremeweather:scorched_earth", "minecraft:grass_block", "minecraft:dirt", "minecraft:air"), start=14):
        reply = send(conn, i, 2, f"execute if block 0 70 0 {block}")
        print(f"block probe {block}: {reply!r}", flush=True)
        if "passed" in reply.lower() and block.startswith("extremeweather:"):
            burned = True
    if not burned:
        raise AssertionError("Wildfire did not replace grass/dirt with a scorched block after 26 seconds")
    check(21, "execute in minecraft:overworld positioned 0 100 0 run ew storm supercell 50", "Spawned PMWeather storm")
    check(22, "execute in minecraft:overworld positioned 0 100 0 run ew vortex rope 1", "Native PMWeather supercell")
    check(44, "execute in minecraft:overworld positioned 0 100 0 run ew vortex multi_vortex 2", "Native PMWeather supercell")
    reply=send(conn,45,2,"execute in minecraft:overworld positioned 0 100 0 run ew status")
    print("native vortex state:",reply,flush=True)
    import re
    matched=re.search(r"native sub-vortices: (\d+)",reply)
    if not matched or int(matched.group(1))<4:
        raise AssertionError("PMWeather storm did not retain four native satellites")
    check(27, "execute in minecraft:overworld positioned 0 100 0 run ew status", "PMWeather storms nearby:")
    check(28, "ew clear all", "PMWeather storms and addon weather cleared")
    check(29, "ew disaster hail 0 70 0", "Started hail")
    check(30, "ew disaster squall 0 100 0", "Started squall")
    check(46, "execute positioned 0 100 0 run ew status", "active squall gusts: 1")
    check(47, "summon minecraft:pig 0 74 0 {Invulnerable:1b,PersistenceRequired:1b}", "Summoned")
    import re
    def pig_position(index):
        reply=send(conn,index,2,"data get entity @e[type=minecraft:pig,limit=1,sort=nearest] Pos")
        print("squall pig position:",reply,flush=True)
        data=re.search(r"\[([^]]+)\]",reply)
        if not data:raise AssertionError("Cannot inspect squall test entity")
        return [float(value.rstrip('df')) for value in data.group(1).split(', ')]
    before=pig_position(48)
    time.sleep(2)
    after=pig_position(49)
    reply=send(conn,53,2,"data get entity @e[type=minecraft:pig,limit=1] Motion")
    print("pig velocity:",reply,flush=True)
    vector=re.search(r"\[([^]]+)\]",reply)
    if not vector:raise AssertionError("Cannot inspect squall entity velocity")
    velocity=[float(value.rstrip('df')) for value in vector.group(1).split(', ')]
    status=send(conn,52,2,"execute positioned 0 100 0 run ew status")
    print("squall physics status:",status,flush=True)
    pushed=re.search(r"squall pushes: (\d+)",status)
    if not pushed or int(pushed.group(1))<1:
        raise AssertionError("Squall never applied a gust to the test entity")
    # A dedicated server with zero players may suspend entity movement even in a
    # forced chunk. Verify horizontal velocity, not apparent displacement.
    # Sampled Motion may be zero between gusts; previous/current positions
    # also prove the gust displaced the mob while the server was ticking it.
    horizontal_motion=abs(velocity[0])+abs(velocity[2])
    horizontal_displacement=abs(before[0]-after[0])+abs(before[2]-after[2])
    if horizontal_motion<.01 and horizontal_displacement<.01:
        raise AssertionError("Squall neither imparted horizontal velocity nor moved the mob")
    send(conn,50,2,"kill @e[type=minecraft:pig]")
    check(41, "time set midnight", "Set the time")
    print("storm status before sprite:",send(conn,43,2,"execute positioned 0 100 0 run ew status"),flush=True)
    check(42, "ew disaster sprite 0 100 0", "Started sprite")
    check(62, "ew disaster dust_devil 0 71 0", "Started dust_devil")
    check(63, "ew status", "active small vortices: 1")
    check(31, "ew clear all", "PMWeather storms and addon weather cleared")
    check(32, "ew disaster hurricane 0 100 0", "Started hurricane")
    check(61, "execute positioned 0 100 0 run ew status", "active cyclone systems: 1")
    check(33, "ew clear all", "PMWeather storms and addon weather cleared")
    print("prepare water:",send(conn,34,2,"setblock 2 63 0 minecraft:water"),flush=True)
    check(55, "execute if block 2 63 0 minecraft:water", "passed")
    check(35, "ew disaster waterspout 2 64 0", "Started waterspout")
    check(56, "ew status", "active small vortices: 1")
    print("remove water under waterspout:",send(conn,57,2,"setblock 2 63 0 minecraft:air"),flush=True)
    time.sleep(.8)
    check(58, "ew status", "active small vortices: 0")
    print("restore water:",send(conn,59,2,"setblock 2 63 0 minecraft:water"),flush=True)
    check(60, "execute if block 2 63 0 minecraft:water", "passed")
    check(40, "ew disaster whirlpool 2 64 0", "Started whirlpool")
    print("clear crest target:",send(conn,39,2,"setblock 2 64 0 minecraft:air"),flush=True)
    check(36, "ew disaster flood 2 64 0", "Started flood")
    check(37, "execute if block 2 64 0 minecraft:water", "passed")
    check(38, "ew clear all", "PMWeather storms and addon weather cleared")
print("PMWeather storm/tornado and scorched-ground integration tests passed")

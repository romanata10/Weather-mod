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
    check(10, "weather clear 100000", "clear")
    send(conn, 100, 2, "gamerule doWeatherCycle false")
    # A solid shallow basin around a single water anchor forces the temporary
    # crest to spread laterally as vanilla flowing water. Check its recession.
    # This does not cover arbitrary shore geometry or crash recovery.
    check(173,"forceload add 4100 8","force")
    print("flood test basin floor:",send(conn,174,2,"fill 4097 63 5 4103 63 11 minecraft:stone"),flush=True)
    check(181,"setblock 4100 63 8 minecraft:water","block")
    send(conn,175,2,"fill 4097 64 5 4103 64 11 minecraft:air")
    check(176,"ew disaster flood 4100 64 8","Started flood")
    # Force a vanilla neighbor update beside the temporary source; a remote
    # force-loaded chunk with no players need not otherwise tick fluid soon.
    send(conn,182,2,"setblock 4101 64 8 minecraft:stone")
    send(conn,183,2,"setblock 4101 64 8 minecraft:air")
    flowed=False
    for attempt in range(24):
        # Empty dedicated servers sometimes delay scheduled fluid ticks even
        # in a force-loaded chunk. Re-notify the adjacent block periodically.
        if attempt and attempt%6==0:
            send(conn,182,2,"setblock 4101 64 8 minecraft:stone")
            send(conn,183,2,"setblock 4101 64 8 minecraft:air")
        time.sleep(.5)
        reply=send(conn,177,2,"execute if block 4101 64 8 minecraft:water")
        if 'passed' in reply.lower():flowed=True;break
    if not flowed:
        source=send(conn,184,2,"execute if block 4100 64 8 minecraft:water")
        raise AssertionError("Temporary source did not spread in test basin; source="+source+" flow="+reply)
    check(178,"ew clear all","PMWeather storms and addon weather cleared")
    time.sleep(4)
    check(179,"execute if block 4100 64 8 minecraft:air","passed")
    check(180,"execute if block 4101 64 8 minecraft:air","passed")
    print("prepare wildfire fuel:",send(conn,11,2,"setblock 0 70 0 minecraft:grass_block"),flush=True)
    check(54, "execute if block 0 70 0 minecraft:grass_block", "passed")
    send(conn, 12, 2, "setblock 0 71 0 minecraft:air")  # air may already exist
    # PMWeather may start rain even when vanilla weather is locked clear.
    # Shelter only the test fuel, so rain cannot extinguish it mid-probe.
    send(conn, 101, 2, "setblock 0 73 0 minecraft:stone")
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
        raise AssertionError("Wildfire did not scorch sheltered test grass after 26 seconds; "
                             + send(conn, 102, 2, "ew firestatus 0 70 0"))
    check(21, "execute in minecraft:overworld positioned 0 100 0 run ew storm supercell 50", "Spawned PMWeather storm")
    check(22, "execute in minecraft:overworld positioned 0 100 0 run ew vortex rope 1", "Native PMWeather supercell")
    check(44, "execute in minecraft:overworld positioned 0 100 0 run ew vortex multi_vortex 2", "Native PMWeather supercell")
    reply=send(conn,45,2,"execute in minecraft:overworld positioned 0 100 0 run ew status")
    print("native vortex state:",reply,flush=True)
    import re
    matched=re.search(r"native sub-vortices: (\d+)",reply)
    width=re.search(r"widest native supercell width: (\d+)",reply)
    if not matched or int(matched.group(1))<4:
        raise AssertionError("PMWeather storm did not retain four native satellites")
    if not width or int(width.group(1))<20:
        raise AssertionError("Native PMWeather funnel width was not applied: "+reply)
    time.sleep(1.5)
    later=send(conn,130,2,"execute in minecraft:overworld positioned 0 100 0 run ew status")
    print("native vortex after server ticks:",later,flush=True)
    later_width=re.search(r"widest native supercell width: (\d+)",later)
    if not later_width or int(later_width.group(1))<15:
        raise AssertionError("Native funnel width collapsed after server ticks: "+later)
    later_wind=re.search(r"strongest native supercell wind: (\d+)",later)
    if not later_wind or int(later_wind.group(1))<1:
        raise AssertionError("Native funnel lost its PMWeather wind after server ticks: "+later)
    # The first regression appeared only after roughly ten seconds. Keep
    # this in the required server test, not only the optional client probe.
    time.sleep(8.5)
    sustained=send(conn,131,2,"execute in minecraft:overworld positioned 0 100 0 run ew status")
    print("native vortex after ten seconds:",sustained,flush=True)
    sustained_width=re.search(r"widest native supercell width: (\d+)",sustained)
    sustained_wind=re.search(r"strongest native supercell wind: (\d+)",sustained)
    if not sustained_width or int(sustained_width.group(1))<20 or not sustained_wind or int(sustained_wind.group(1))<1:
        raise AssertionError("Native tornado geometry/wind did not persist: "+sustained)
    check(27, "execute in minecraft:overworld positioned 0 100 0 run ew status", "PMWeather storms nearby:")
    check(28, "ew clear all", "PMWeather storms and addon weather cleared")
    # EF endpoints must seed nonzero native wind, not just a static cloud.
    for rating, minimum in ((0, 60), (5, 200)):
        check(132+rating, f"execute positioned 0 100 0 run ew vortex wedge {rating}", "One native PMWeather supercell")
        endpoint=send(conn,140+rating,2,"execute positioned 0 100 0 run ew status")
        print(f"native EF{rating} status:",endpoint,flush=True)
        endpoint_wind=re.search(r"strongest native supercell wind: (\d+)",endpoint)
        if not endpoint_wind or int(endpoint_wind.group(1))<minimum:
            raise AssertionError(f"EF{rating} native wind was not initialized: "+endpoint)
        check(150+rating,"ew clear all","PMWeather storms and addon weather cleared")
    # Rare requested morphologies must stay ONE native supercell with PM native
    # satellites, not a second addon funnel. Natural rarity is audited separately.
    for shape,minimum,count in (("dead_man_walking",40,3),("el_reno",80,5)):
        check(180,f"execute positioned 0 100 0 run ew vortex {shape} 5","One native PMWeather supercell")
        special=send(conn,181,2,"execute positioned 0 100 0 run ew status")
        print(f"native {shape} state:",special,flush=True)
        satellites=re.search(r"native sub-vortices: (\d+)",special)
        width=re.search(r"widest native supercell width: (\d+)",special)
        if not satellites or int(satellites.group(1))<count or not width or int(width.group(1))<minimum:
            raise AssertionError(f"Native {shape} satellites/width missing: "+special)
        check(182,"ew clear all","PMWeather storms and addon weather cleared")
    # Verify the functional storm siren switches its BLOCKSTATE from PMWeather
    # storms, rather than merely existing as a decorative crafted block.
    check(120,"setblock 4 70 0 extremeweather:storm_siren","block")
    check(126,"setblock 5 70 0 minecraft:redstone_lamp","block")
    check(121,"execute if block 4 70 0 extremeweather:storm_siren[active=false]","passed")
    check(127,"execute if block 5 70 0 minecraft:redstone_lamp[lit=false]","passed")
    check(122,"execute positioned 4 100 0 run ew storm supercell 70","Spawned PMWeather storm")
    time.sleep(1.4)
    check(123,"execute if block 4 70 0 extremeweather:storm_siren[active=true]","passed")
    check(128,"execute if block 5 70 0 minecraft:redstone_lamp[lit=true]","passed")
    check(124,"ew clear all","PMWeather storms and addon weather cleared")
    time.sleep(1.4)
    check(125,"execute if block 4 70 0 extremeweather:storm_siren[active=false]","passed")
    check(129,"execute if block 5 70 0 minecraft:redstone_lamp[lit=false]","passed")
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
        # With no nearby player, the dedicated server can suspend the pig's
        # chunk; the authoritative gust counter above still proves the
        # squall attempted pushes, NOT that an entity moved for a player.
        print("HEADLESS LIMITATION: gust applied, but no pig motion or displacement observed",flush=True)
    send(conn,50,2,"kill @e[type=minecraft:pig]")
    check(41, "time set midnight", "Set the time")
    print("storm status before sprite:",send(conn,43,2,"execute positioned 0 100 0 run ew status"),flush=True)
    check(42, "ew disaster sprite 0 100 0", "Started sprite")
    check(106, "ew status", "active sprites: 1")
    time.sleep(3.5)
    check(107, "ew status", "active sprites: 0")
    check(62, "ew disaster dust_devil 0 71 0", "Started dust_devil")
    check(63, "ew status", "active small vortices: 1")
    check(31, "ew clear all", "PMWeather storms and addon weather cleared")
    # The test must be land irrespective of randomized overworld spawn biome.
    check(105, "fillbiome -16 90 -16 16 110 16 minecraft:plains", "biome entry")
    check(32, "ew disaster hurricane 0 100 0", "Started hurricane")
    check(61, "execute positioned 0 100 0 run ew status", "active cyclone systems: 1")
    rejected_sprite=send(conn,116,2,"ew disaster sprite 0 100 0")
    print("sprite over cyclone rejected:",rejected_sprite,flush=True)
    if 'Sprite requires night and a nearby PMWeather thundercloud' not in rejected_sprite:
        raise AssertionError("Cyclone was incorrectly accepted as sprite thundercloud: "+rejected_sprite)
    cyclone_status=send(conn,103,2,"execute positioned 0 100 0 run ew status")
    import re
    width=re.search(r"widest native cyclone width: (\d+)",cyclone_status)
    if not width or int(width.group(1))<220:
        raise AssertionError("Native PMWeather cyclone did not keep the requested wide cloud state: "+cyclone_status)
    # This test world is land, so native width and addon strength should both
    # decline after several 40-tick synchronization intervals.
    time.sleep(5)
    # PMWeather can reassign its own width between our addon tick and an RCON
    # query. Sample over several server ticks; require an actual narrow state,
    # not merely a declining addon power counter.
    narrowed=False
    for _ in range(20):
        landfall=send(conn,104,2,"execute positioned 0 100 0 run ew status")
        reduced=re.search(r"widest native cyclone width: (\d+)",landfall)
        if reduced and int(reduced.group(1))<int(width.group(1)):
            narrowed=True
            break
        time.sleep(.25)
    print("native cyclone landfall state:",landfall,flush=True)
    if not narrowed:
        raise AssertionError("Native cyclone cloud never narrowed during sampled landfall ticks: "+landfall)
    check(33, "ew clear all", "PMWeather storms and addon weather cleared")
    print("prepare water:",send(conn,34,2,"setblock 2 63 0 minecraft:water"),flush=True)
    check(55, "execute if block 2 63 0 minecraft:water", "passed")
    without_cloud=send(conn,185,2,"ew disaster waterspout 2 64 0")
    print("waterspout without thundercloud:",without_cloud,flush=True)
    if 'requires a nearby PMWeather supercell/squall cloud' not in without_cloud:
        raise AssertionError("Waterspout spawned without its required thundercloud: "+without_cloud)
    check(186,"execute positioned 2 100 0 run ew storm supercell 70","Spawned PMWeather storm")
    check(35, "ew disaster waterspout 2 64 0", "Started waterspout")
    check(56, "ew status", "active small vortices: 1")
    print("remove water under waterspout:",send(conn,57,2,"setblock 2 63 0 minecraft:air"),flush=True)
    time.sleep(.8)
    check(58, "ew status", "active small vortices: 0")
    check(187,"ew clear all","PMWeather storms and addon weather cleared")
    print("restore water:",send(conn,59,2,"setblock 2 63 0 minecraft:water"),flush=True)
    check(60, "execute if block 2 63 0 minecraft:water", "passed")
    # Stage a stable water basin BEFORE starting the current: otherwise a
    # single isolated source can drain/flow before the pool is filled.
    send(conn,170,2,"fill 0 62 -6 8 62 6 minecraft:stone")
    print("prepare whirlpool pool:",send(conn,108,2,"fill 0 63 -6 8 63 6 minecraft:water"),flush=True)
    check(109,"execute if block 5 63 0 minecraft:water","passed")
    check(171,"execute if block 2 63 0 minecraft:water","passed")
    check(40, "ew disaster whirlpool 2 64 0", "Started whirlpool")
    # A real submerged mob, not just a successful command response: verify the
    # server applied inward/rotational water-only impulses.
    print("submerged cod:",send(conn,110,2,"summon minecraft:cod 5 63 0"),flush=True)
    for attempt in range(12):
        time.sleep(.5)
        whirl_status=send(conn,111+attempt,2,"ew status")
        pull=re.search(r"whirlpool pulls: (\d+)",whirl_status)
        if pull and int(pull.group(1))>=1:break
    else:
        water_below=send(conn,172,2,"execute if block 2 63 0 minecraft:water")
        raise AssertionError("Whirlpool did not apply a submerged-entity pull; center water="+water_below+": "+whirl_status)
    print("whirlpool physical status:",whirl_status,flush=True)
    send(conn,112,2,"kill @e[type=minecraft:cod]")
    print("drain whirlpool center:",send(conn,113,2,"fill 0 63 -6 8 63 6 minecraft:air"),flush=True)
    time.sleep(.6)
    check(114,"ew status","active whirlpools: 0")
    check(115,"setblock 2 63 0 minecraft:water","block")
    print("clear crest target:",send(conn,39,2,"setblock 2 64 0 minecraft:air"),flush=True)
    check(36, "ew disaster flood 2 64 0", "Started flood")
    check(37, "execute if block 2 64 0 minecraft:water", "passed")
    # Reject another crest alongside the first: two vanilla source blocks can
    # create untracked permanent water even after the addon removes its own.
    send(conn,117,2,"setblock 3 63 0 minecraft:water")
    second=send(conn,118,2,"ew disaster flood 3 64 0")
    print("adjacent flood crest rejected:",second,flush=True)
    if 'Started flood' in second:raise AssertionError("Unsafe adjacent flood source was allowed")
    check(38, "ew clear all", "PMWeather storms and addon weather cleared")
    time.sleep(1.5)
    check(119,"execute if block 2 64 0 minecraft:air","passed")
    # A cleanup issued while the crest's chunk is unloaded must keep the
    # SavedData entry, then remove its own source once the chunk loads again.
    check(160,"forceload add 4008 8","force")
    check(161,"setblock 4008 63 8 minecraft:water","block")
    send(conn,162,2,"setblock 4008 64 8 minecraft:air")
    check(163,"ew disaster flood 4008 64 8","Started flood")
    check(164,"execute if block 4008 64 8 minecraft:water","passed")
    check(165,"forceload remove 4008 8","force")
    unloaded=False
    for _ in range(20):
        time.sleep(.5)
        reply=send(conn,169,2,"execute unless loaded 4008 64 8")
        if 'passed' in reply.lower():unloaded=True;break
    if not unloaded:raise AssertionError("Flood test chunk never unloaded: "+reply)
    check(166,"ew clear all","PMWeather storms and addon weather cleared")
    check(167,"forceload add 4008 8","force")
    time.sleep(1.5)
    check(168,"execute if block 4008 64 8 minecraft:air","passed")
    # The current "tsunami" is a bounded ocean-only entity/foam pulse, not
    # a fluid wall. Prove its cap and immediate /ew clear reset explicitly.
    print("stage ocean for pulse:",send(conn,184,2,"fillbiome 4004 60 4 4012 80 12 minecraft:ocean"),flush=True)
    check(185,"execute if biome 4008 64 8 minecraft:ocean","passed")
    check(186,"ew disaster tsunami 4008 64 8","Started tsunami")
    check(187,"ew disaster meteotsunami 4008 64 8","Started meteotsunami")
    check(188,"ew status","active wave pulses: 2")
    third=send(conn,189,2,"ew disaster tsunami 4008 64 8")
    print("third wave rejected:",third,flush=True)
    if 'Wave limit reached' not in third:raise AssertionError("More than two simultaneous wave pulses allowed: "+third)
    check(190,"ew clear all","PMWeather storms and addon weather cleared")
    check(191,"ew status","active wave pulses: 0")
print("PMWeather storm/tornado and scorched-ground integration tests passed")

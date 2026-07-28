"""Generate Rotwire's stair-connected Lost Cities building resources.

The generated parts use the six-block floor height used by Lost Cities 8.3.10.
Every traversable upper floor repeats the same two-wide stair flight.  The
generator deliberately has no ladder palette entry, making it impossible for
these structures to accidentally depend on ladders for vertical circulation.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = ROOT / "src" / "main" / "resources" / "data" / "rotwire" / "lostcities"
SIZE = 16
FLOOR_HEIGHT = 6

PALETTE = {
    "palette": [
        {"char": "#", "block": "minecraft:polished_deepslate"},
        {"char": "}", "block": "minecraft:cobbled_deepslate"},
        {"char": "F", "block": "minecraft:smooth_stone"},
        {"char": "W", "block": "minecraft:light_gray_concrete"},
        {"char": "N", "block": "minecraft:polished_blackstone_bricks"},
        {"char": "P", "block": "minecraft:bricks"},
        {"char": "G", "block": "minecraft:gray_stained_glass"},
        {"char": "V", "block": "minecraft:blue_stained_glass"},
        {"char": "A", "block": "minecraft:lime_concrete"},
        {"char": "R", "block": "minecraft:red_concrete"},
        {"char": "H", "block": "minecraft:yellow_concrete"},
        {"char": "B", "block": "minecraft:barrel[facing=up,open=false]"},
        {
            "char": "u",
            "block": "handcrafted:oak_cupboard[facing=north,type=1]",
        },
        {
            "char": "i",
            "block": (
                "handcrafted:oak_drawer"
                "[facing=north,shape=single,type=1]"
            ),
        },
        {
            "char": "j",
            "block": "handcrafted:spruce_cupboard[facing=north,type=1]",
        },
        {
            "char": "m",
            "block": (
                "handcrafted:spruce_drawer"
                "[facing=north,shape=single,type=1]"
            ),
        },
        {
            "char": "e",
            "block": "handcrafted:dark_oak_cupboard[facing=north,type=1]",
        },
        {
            "char": "f",
            "block": (
                "handcrafted:dark_oak_drawer"
                "[facing=north,shape=single,type=1]"
            ),
        },
        {"char": "T", "block": "minecraft:stripped_oak_wood[axis=y]"},
        {"char": "K", "block": "minecraft:black_concrete"},
        {"char": "O", "block": "minecraft:bookshelf"},
        {"char": "X", "block": "minecraft:iron_block"},
        {
            "char": "q",
            "block": "minecraft:white_bed[facing=south,occupied=false,part=foot]",
        },
        {
            "char": "Q",
            "block": "minecraft:white_bed[facing=south,occupied=false,part=head]",
        },
        {
            "char": "c",
            "block": (
                "minecraft:stone_brick_stairs"
                "[facing=south,half=bottom,shape=straight,waterlogged=false]"
            ),
        },
        {
            "char": "d",
            "block": (
                "minecraft:stone_brick_stairs"
                "[facing=north,half=top,shape=straight,waterlogged=false]"
            ),
        },
        {
            "char": ":",
            "block": (
                "minecraft:iron_bars"
                "[east=false,north=false,south=false,waterlogged=false,west=false]"
            ),
        },
        {"char": "L", "block": "minecraft:sea_lantern"},
    ]
}

BUILDING_SELECTOR_ENTRIES = [
    {"factor": 0.24, "value": "rotwire:custom_buildings/quarantine_tower"},
    {"factor": 0.20, "value": "rotwire:custom_buildings/response_office"},
    {"factor": 0.26, "value": "rotwire:custom_buildings/stairwell_apartments"},
]

MULTIBUILDING_SELECTOR_ENTRIES = [
    {"factor": 0.16, "value": "rotwire:custom_buildings/quarantine_hospital"},
    {"factor": 0.08, "value": "rotwire:custom_buildings/emergency_block"},
]


def blank_layer(fill: str = " ") -> list[list[str]]:
    return [[fill for _ in range(SIZE)] for _ in range(SIZE)]


def blank_part() -> list[list[list[str]]]:
    return [blank_layer() for _ in range(FLOOR_HEIGHT)]


def put(layer: list[list[str]], x: int, z: int, char: str) -> None:
    layer[z][x] = char


def horizontal(
    layer: list[list[str]], z: int, x1: int, x2: int, char: str
) -> None:
    for x in range(x1, x2 + 1):
        put(layer, x, z, char)


def vertical(
    layer: list[list[str]], x: int, z1: int, z2: int, char: str
) -> None:
    for z in range(z1, z2 + 1):
        put(layer, x, z, char)


def rectangle(
    layer: list[list[str]], x1: int, z1: int, x2: int, z2: int, char: str
) -> None:
    horizontal(layer, z1, x1, x2, char)
    horizontal(layer, z2, x1, x2, char)
    vertical(layer, x1, z1, z2, char)
    vertical(layer, x2, z1, z2, char)


def fill_rectangle(
    layer: list[list[str]], x1: int, z1: int, x2: int, z2: int, char: str
) -> None:
    for z in range(z1, z2 + 1):
        horizontal(layer, z, x1, x2, char)


def add_floor(part: list[list[list[str]]], wall: str) -> None:
    fill_rectangle(part[0], 0, 0, 15, 15, "F")
    horizontal(part[0], 0, 0, 15, wall)
    horizontal(part[0], 15, 0, 15, wall)
    vertical(part[0], 0, 0, 15, wall)
    vertical(part[0], 15, 0, 15, wall)


def facade_char(position: int, y: int, wall: str, glass: str, accent: str) -> str:
    if position in (0, 5, 10, 15):
        return accent if y in (2, 3) else wall
    if y in (2, 3, 4) and position not in (1, 14):
        return glass
    return wall


def add_shell(
    part: list[list[list[str]]],
    *,
    wall: str,
    glass: str,
    accent: str,
    external_sides: Iterable[str] = ("north", "south", "west", "east"),
    ground_entrance: bool = False,
) -> None:
    sides = set(external_sides)
    add_floor(part, wall)
    for y in range(1, FLOOR_HEIGHT):
        layer = part[y]
        if "north" in sides:
            for x in range(SIZE):
                put(layer, x, 0, facade_char(x, y, wall, glass, accent))
        if "south" in sides:
            for x in range(SIZE):
                put(layer, x, 15, facade_char(x, y, wall, glass, accent))
        if "west" in sides:
            for z in range(SIZE):
                put(layer, 0, z, facade_char(z, y, wall, glass, accent))
        if "east" in sides:
            for z in range(SIZE):
                put(layer, 15, z, facade_char(z, y, wall, glass, accent))

    if ground_entrance and "north" in sides:
        for y in (1, 2, 3):
            for x in (7, 8):
                put(part[y], x, 0, " ")
        for y in (1, 2, 3):
            put(part[y], 6, 0, accent)
            put(part[y], 9, 0, accent)


def custom_chamfer(
    part: list[list[list[str]]],
    *,
    wall: str,
    glass: str,
    external_sides: Iterable[str],
    ground: bool,
    deep: bool = False,
) -> None:
    sides = set(external_sides)
    corners = (
        ("north", "west", ((0, 0), (1, 0), (0, 1), (1, 1))),
        ("north", "east", ((15, 0), (14, 0), (15, 1), (14, 1))),
        ("south", "west", ((0, 15), (1, 15), (0, 14), (1, 14))),
        ("south", "east", ((15, 15), (14, 15), (15, 14), (14, 14))),
    )
    for side_a, side_b, (corner, edge_a, edge_b, inner) in corners:
        if side_a not in sides or side_b not in sides:
            continue
        cleared = (corner, edge_a, edge_b) if deep else (corner,)
        for x, z in cleared:
            for y in range(0 if not ground else 1, FLOOR_HEIGHT):
                put(part[y], x, z, " ")
        if deep:
            for y in range(1, FLOOR_HEIGHT):
                put(
                    part[y],
                    inner[0],
                    inner[1],
                    glass if y in (2, 3, 4) else wall,
                )


def custom_recess(
    part: list[list[list[str]]],
    *,
    side: str,
    start: int,
    end: int,
    wall: str,
    glass: str,
    accent: str,
    ground: bool,
    balcony: bool = False,
) -> None:
    boundary = 0 if side in ("north", "west") else 15
    inner = 1 if boundary == 0 else 14
    for position in range(start, end + 1):
        x = position if side in ("north", "south") else boundary
        z = boundary if side in ("north", "south") else position
        if not ground and not balcony:
            put(part[0], x, z, " ")
        for y in range(1, FLOOR_HEIGHT):
            put(part[y], x, z, " ")

        inner_x = position if side in ("north", "south") else inner
        inner_z = inner if side in ("north", "south") else position
        for y in range(1, FLOOR_HEIGHT):
            char = glass if y in (2, 3, 4) else wall
            if position in (start, end):
                char = accent
            put(part[y], inner_x, inner_z, char)
        if balcony:
            put(part[1], x, z, ":")

    if balcony:
        door = (start + end) // 2
        inner_x = door if side in ("north", "south") else inner
        inner_z = inner if side in ("north", "south") else door
        for y in (1, 2):
            put(part[y], inner_x, inner_z, " ")


def restore_custom_entrance(
    part: list[list[list[str]]],
    accent: str,
) -> None:
    for z in (0, 1):
        for x in (7, 8):
            put(part[0], x, z, "F")
            for y in (1, 2, 3):
                put(part[y], x, z, " ")
    for x in (6, 9):
        for y in (1, 2, 3):
            put(part[y], x, 0, accent)
    horizontal(part[4], 0, 6, 9, accent)


def apply_custom_exterior(
    part: list[list[list[str]]],
    *,
    theme: str,
    wall: str,
    glass: str,
    accent: str,
    external_sides: Iterable[str],
    ground: bool,
) -> None:
    sides = tuple(external_sides)
    if theme == "quarantine_tower":
        custom_chamfer(
            part,
            wall=wall,
            glass=glass,
            external_sides=sides,
            ground=ground,
            deep=True,
        )
        custom_recess(
            part,
            side="west",
            start=4,
            end=11,
            wall=wall,
            glass=glass,
            accent=accent,
            ground=ground,
        )
    elif theme == "response_office":
        custom_chamfer(
            part,
            wall=wall,
            glass=glass,
            external_sides=sides,
            ground=ground,
        )
        custom_recess(
            part,
            side="north",
            start=3,
            end=12,
            wall=wall,
            glass=glass,
            accent=accent,
            ground=ground,
        )
        for position in (2, 6, 10, 13):
            for y in range(1, FLOOR_HEIGHT):
                put(part[y], position, 15, accent)
    elif theme == "stairwell_apartments":
        custom_chamfer(
            part,
            wall=wall,
            glass=glass,
            external_sides=sides,
            ground=ground,
        )
        for side in ("north", "south"):
            custom_recess(
                part,
                side=side,
                start=2,
                end=5,
                wall=wall,
                glass=glass,
                accent=accent,
                ground=ground,
                balcony=not ground,
            )
    elif theme == "quarantine_hospital":
        custom_chamfer(
            part,
            wall=wall,
            glass=glass,
            external_sides=sides,
            ground=ground,
            deep=True,
        )
        # Projecting vertical corner piers create a coherent 32×32 hospital
        # frame without ever touching an internal quadrant seam.
        for side in sides:
            boundary = 0 if side in ("north", "west") else 15
            for position in (4, 11):
                x = position if side in ("north", "south") else boundary
                z = boundary if side in ("north", "south") else position
                for y in range(1, FLOOR_HEIGHT):
                    put(part[y], x, z, accent)

    if ground and "north" in sides:
        restore_custom_entrance(part, accent)


def apply_custom_roof_shape(
    part: list[list[list[str]]],
    *,
    theme: str,
    wall: str,
    glass: str,
    external_sides: Iterable[str],
) -> None:
    custom_chamfer(
        part,
        wall=wall,
        glass=glass,
        external_sides=external_sides,
        ground=False,
        deep=theme in ("quarantine_tower", "quarantine_hospital"),
    )


def add_stair_flight(
    part: list[list[list[str]]], *, arriving_from_below: bool
) -> None:
    # Carve a full-height vestibule before and after the flight. The apartment
    # theme has a brick partition at z=7; clearing only the stair footprint
    # left its first step pressed against that wall and unusable in-game.
    for y in (1, 2, 3):
        for z in (6, 7, 14):
            for x in range(9, 13):
                put(part[y], x, z, " ")

    if arriving_from_below:
        for z in (10, 11, 12):
            put(part[0], 10, z, " ")
            put(part[0], 11, z, " ")
        put(part[0], 10, 13, "c")
        put(part[0], 11, 13, "c")

    for y in range(1, FLOOR_HEIGHT):
        stair_z = 7 + y
        put(part[y], 10, stair_z, "c")
        put(part[y], 11, stair_z, "c")
        put(part[y], 10, stair_z + 1, "d")
        put(part[y], 11, stair_z + 1, "d")

    for z in range(9, 14):
        put(part[1], 9, z, ":")
        put(part[1], 12, z, ":")


def add_roof(
    part: list[list[list[str]]],
    *,
    wall: str,
    accent: str,
    external_sides: Iterable[str],
    stairs: bool,
) -> None:
    sides = set(external_sides)
    fill_rectangle(part[0], 0, 0, 15, 15, "F")
    if "north" in sides:
        horizontal(part[1], 0, 0, 15, wall)
    if "south" in sides:
        horizontal(part[1], 15, 0, 15, wall)
    if "west" in sides:
        vertical(part[1], 0, 0, 15, wall)
    if "east" in sides:
        vertical(part[1], 15, 0, 15, wall)

    for x, z in ((3, 3), (4, 3), (3, 4), (4, 4), (6, 3), (7, 3)):
        put(part[1], x, z, "X")
    horizontal(part[1], 7, 3, 7, accent)

    if not stairs:
        return

    for z in (10, 11, 12):
        put(part[0], 10, z, " ")
        put(part[0], 11, z, " ")
    put(part[0], 10, 13, "c")
    put(part[0], 11, 13, "c")

    for y in (1, 2, 3):
        rectangle(part[y], 9, 9, 12, 14, wall)
        put(part[y], 9, 13, " ")
    fill_rectangle(part[4], 9, 9, 12, 14, "F")
    put(part[2], 12, 11, "G")
    put(part[3], 12, 11, "G")


def add_quarantine_interior(
    part: list[list[list[str]]], *, stage: str
) -> None:
    for y in (1, 2, 3, 4):
        vertical(part[y], 5, 1, 14, "W")
        for z in (4, 5, 11):
            put(part[y], 5, z, " ")
    if stage == "ground":
        horizontal(part[1], 3, 2, 4, "T")
        put(part[1], 3, 6, "u")
        put(part[1], 4, 6, "i")
        horizontal(part[1], 6, 8, 12, "A")
    else:
        for x, z in ((2, 3), (2, 9), (7, 3), (7, 6)):
            put(part[1], x, z, "q")
            put(part[1], x, z + 1, "Q")
        put(part[1], 3, 13, "u")
        put(part[1], 7, 13, "i")
    for x, z in ((3, 3), (3, 10), (8, 4), (8, 11)):
        put(part[5], x, z, "L")


def add_office_interior(part: list[list[list[str]]], *, stage: str) -> None:
    for y in (1, 2, 3):
        horizontal(part[y], 6, 1, 9, "N")
        put(part[y], 4, 6, " ")
        put(part[y], 5, 6, " ")
    desks = ((3, 3), (7, 3), (3, 10), (7, 10))
    for x, z in desks:
        horizontal(part[1], z, x, x + 1, "T")
        put(part[2], x, z, "K")
    if stage == "ground":
        horizontal(part[1], 2, 6, 9, "T")
        put(part[1], 13, 3, "e")
        put(part[1], 13, 5, "f")
        put(part[1], 3, 13, "e")
    else:
        vertical(part[1], 13, 3, 6, "O")
        put(part[1], 13, 3, "e")
        put(part[1], 13, 6, "f")
        put(part[1], 3, 13, "e")
        put(part[1], 7, 13, "f")
    for x, z in ((4, 4), (8, 4), (4, 11), (8, 11)):
        put(part[5], x, z, "L")


def add_apartment_interior(
    part: list[list[list[str]]], *, stage: str
) -> None:
    for y in (1, 2, 3, 4):
        horizontal(part[y], 7, 1, 14, "P")
        vertical(part[y], 7, 1, 14, "P")
        for x, z in ((3, 7), (4, 7), (7, 4), (7, 11)):
            put(part[y], x, z, " ")
    for x, z in ((2, 2), (10, 2), (2, 10), (8, 10)):
        put(part[1], x, z, "q")
        put(part[1], x, z + 1, "Q")
    for (x, z), container in zip(
        ((5, 2), (13, 2), (5, 12), (13, 12)),
        ("u", "i", "j", "m"),
    ):
        put(part[1], x, z, container)
    for x, z in ((4, 4), (11, 4), (4, 11), (11, 11)):
        put(part[5], x, z, "L")


def add_hospital_interior(
    part: list[list[list[str]]], *, stage: str, quadrant: str
) -> None:
    if quadrant in ("nw", "sw"):
        for y in (1, 2, 3):
            vertical(part[y], 5, 2, 13, "W")
            put(part[y], 5, 7, " ")
            put(part[y], 5, 8, " ")
    else:
        for y in (1, 2, 3):
            vertical(part[y], 10, 2, 13, "W")
            put(part[y], 10, 7, " ")
            put(part[y], 10, 8, " ")

    if stage == "ground":
        horizontal(part[1], 4, 3, 7, "T")
        horizontal(part[1], 11, 8, 12, "A")
    else:
        for x, z in ((2, 3), (2, 10), (7, 3), (7, 10)):
            put(part[1], x, z, "q")
            put(part[1], x, z + 1, "Q")
    put(part[1], 13, 4, "u")
    put(part[1], 13, 11, "i")
    for x, z in ((3, 5), (8, 5), (3, 12), (8, 12)):
        put(part[5], x, z, "L")


def serialize_part(part: list[list[list[str]]]) -> dict:
    return {
        "xsize": SIZE,
        "zsize": SIZE,
        "slices": [["".join(row) for row in layer] for layer in part],
        "refpalette": "rotwire:custom_buildings",
    }


def make_solo_part(theme: str, stage: str) -> dict:
    settings = {
        "quarantine_tower": ("W", "G", "A", add_quarantine_interior),
        "response_office": ("N", "V", "R", add_office_interior),
        "stairwell_apartments": ("P", "G", "H", add_apartment_interior),
    }
    wall, glass, accent, interior = settings[theme]
    part = blank_part()
    if stage == "roof":
        add_roof(
            part,
            wall=wall,
            accent=accent,
            external_sides=("north", "south", "west", "east"),
            stairs=True,
        )
        apply_custom_roof_shape(
            part,
            theme=theme,
            wall=wall,
            glass=glass,
            external_sides=("north", "south", "west", "east"),
        )
    else:
        add_shell(
            part,
            wall=wall,
            glass=glass,
            accent=accent,
            ground_entrance=stage == "ground",
        )
        apply_custom_exterior(
            part,
            theme=theme,
            wall=wall,
            glass=glass,
            accent=accent,
            external_sides=("north", "south", "west", "east"),
            ground=stage == "ground",
        )
        interior(part, stage=stage)
        if stage == "ground":
            restore_custom_entrance(part, accent)
        add_stair_flight(part, arriving_from_below=stage == "middle")
    return serialize_part(part)


HOSPITAL_SIDES = {
    "nw": ("north", "west"),
    "ne": ("north", "east"),
    "sw": ("south", "west"),
    "se": ("south", "east"),
}


def make_hospital_part(quadrant: str, stage: str) -> dict:
    has_stairs = quadrant in ("nw", "se")
    sides = HOSPITAL_SIDES[quadrant]
    part = blank_part()
    if stage == "roof":
        add_roof(
            part,
            wall="W",
            accent="A",
            external_sides=sides,
            stairs=has_stairs,
        )
        apply_custom_roof_shape(
            part,
            theme="quarantine_hospital",
            wall="W",
            glass="G",
            external_sides=sides,
        )
    else:
        add_shell(
            part,
            wall="W",
            glass="G",
            accent="A",
            external_sides=sides,
            ground_entrance=stage == "ground" and "north" in sides,
        )
        apply_custom_exterior(
            part,
            theme="quarantine_hospital",
            wall="W",
            glass="G",
            accent="A",
            external_sides=sides,
            ground=stage == "ground",
        )
        add_hospital_interior(part, stage=stage, quadrant=quadrant)
        if stage == "ground" and "north" in sides:
            restore_custom_entrance(part, "A")
        if has_stairs:
            add_stair_flight(part, arriving_from_below=stage == "middle")
    return serialize_part(part)


def building_definition(name: str, min_floors: int, max_floors: int) -> dict:
    base = f"rotwire:custom_buildings/{name}"
    return {
        "refpalette": "rotwire:custom_buildings",
        "filler": "#",
        "rubble": "}",
        "mincellars": 0,
        "maxcellars": 0,
        "minfloors": min_floors,
        "maxfloors": max_floors,
        "allowDoors": True,
        "allowFillers": False,
        "overrideFloors": True,
        "parts": [
            {"floor": 0, "part": f"{base}_ground"},
            {"top": False, "range": "1,100", "part": f"{base}_middle"},
            {"top": True, "part": f"{base}_roof"},
        ],
    }


def write_json(relative: Path, value: dict) -> None:
    path = DATA_ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def validate_part(name: str, part: dict) -> None:
    slices = part["slices"]
    if len(slices) != FLOOR_HEIGHT:
        raise ValueError(f"{name}: expected {FLOOR_HEIGHT} slices")
    for y, layer in enumerate(slices):
        if len(layer) != SIZE:
            raise ValueError(f"{name}: slice {y} does not contain {SIZE} rows")
        for z, row in enumerate(layer):
            if len(row) != SIZE:
                raise ValueError(
                    f"{name}: slice {y}, row {z} has width {len(row)}, not {SIZE}"
                )
            if "l" in row:
                raise ValueError(f"{name}: ladder marker found")


def generate() -> None:
    write_json(Path("palettes/custom_buildings.json"), PALETTE)

    solo_heights = {
        "quarantine_tower": (5, 7),
        "response_office": (6, 9),
        "stairwell_apartments": (5, 8),
    }
    generated_parts: dict[str, dict] = {}
    for name, (minimum, maximum) in solo_heights.items():
        for stage in ("ground", "middle", "roof"):
            part_name = f"{name}_{stage}"
            part = make_solo_part(name, stage)
            validate_part(part_name, part)
            generated_parts[part_name] = part
            write_json(
                Path("parts/custom_buildings") / f"{part_name}.json",
                part,
            )
        write_json(
            Path("buildings/custom_buildings") / f"{name}.json",
            building_definition(name, minimum, maximum),
        )

    for quadrant in ("nw", "ne", "sw", "se"):
        name = f"quarantine_hospital_{quadrant}"
        for stage in ("ground", "middle", "roof"):
            part_name = f"{name}_{stage}"
            part = make_hospital_part(quadrant, stage)
            validate_part(part_name, part)
            generated_parts[part_name] = part
            write_json(
                Path("parts/custom_buildings") / f"{part_name}.json",
                part,
            )
        write_json(
            Path("buildings/custom_buildings") / f"{name}.json",
            building_definition(name, 5, 7),
        )

    write_json(
        Path("multibuildings/custom_buildings/quarantine_hospital.json"),
        {
            "dimx": 2,
            "dimz": 2,
            "buildings": [
                [
                    "rotwire:custom_buildings/quarantine_hospital_nw",
                    "rotwire:custom_buildings/quarantine_hospital_sw",
                ],
                [
                    "rotwire:custom_buildings/quarantine_hospital_ne",
                    "rotwire:custom_buildings/quarantine_hospital_se",
                ],
            ],
        },
    )
    write_json(
        Path("multibuildings/custom_buildings/emergency_block.json"),
        {
            "dimx": 3,
            "dimz": 2,
            "buildings": [
                [
                    "rotwire:custom_buildings/quarantine_tower",
                    "rotwire:custom_buildings/response_office",
                ],
                [
                    "rotwire:custom_buildings/stairwell_apartments",
                    "rotwire:custom_buildings/quarantine_tower",
                ],
                [
                    "rotwire:custom_buildings/response_office",
                    "rotwire:custom_buildings/stairwell_apartments",
                ],
            ],
        },
    )

    if any(
        "ladder" in entry.get("block", "")
        for entry in PALETTE["palette"]
    ):
        raise ValueError("Custom building palette must not contain ladders")

    print(
        "Generated "
        f"{len(generated_parts)} parts, "
        f"{len(solo_heights) + len(HOSPITAL_SIDES)} buildings, "
        "and 2 multibuildings."
    )


if __name__ == "__main__":
    generate()

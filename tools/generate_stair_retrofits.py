"""Replace Lost Cities' ladder tower families with zombie-traversable stairs.

The Rotwire pack already decorates the LCMT versions of building families
1-8 with Handcrafted furniture. Families 1, 2, 3, 5, 6, and 8 still inherited
the original vertical ladder shaft. This generator keeps those decorated floor
plans, removes every ladder marker, and installs the same proven two-wide stair
flight used by Rotwire's custom buildings.

Both the base Lost Cities and LCMT building definitions are overridden. This
also fixes inherited city-style and multibuilding selections without replacing
their IDs or requiring a world-profile change.
"""

from __future__ import annotations

import copy
import json
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = ROOT / "src" / "main" / "resources"
ROTWIRE_ROOT = (
    RESOURCE_ROOT / "data" / "rotwire" / "lostcities"
)
SIZE = 16
FLOOR_HEIGHT = 6


@dataclass(frozen=True)
class Family:
    part_count: int
    stair_x: int
    stair_z: int
    min_floors: int
    max_floors: int
    lore: str
    exterior: str


# These placements were scored across every decorated variant. They preserve
# all Handcrafted symbols and avoid every vanilla loot-chest marker.
FAMILIES = {
    "building1": Family(
        9, 10, 7, 5, 8, "quarantine apartments", "quarantine"
    ),
    "building2": Family(
        4, 11, 5, 5, 7, "urgent-care annex", "clinical"
    ),
    "building3": Family(
        4, 11, 6, 5, 8, "civil-defense offices", "civic"
    ),
    "building4": Family(
        4, 10, 8, 5, 7, "emergency research annex", "research"
    ),
    "building5": Family(
        4, 4, 3, 5, 7, "municipal maintenance depot", "industrial"
    ),
    "building6": Family(
        4, 5, 3, 5, 7, "evacuation shelter", "bunker"
    ),
    "building7": Family(
        5, 10, 8, 5, 8, "public housing tower", "housing"
    ),
    "building8": Family(
        8, 12, 7, 6, 9, "mixed-use relief center", "mixed_use"
    ),
}


@dataclass(frozen=True)
class Exterior:
    wall: str
    accent: str
    glass: str
    shape: str


# A dedicated composite palette lets the inherited plans retain every
# Handcrafted marker while adding fixed, lore-specific façade materials. The
# Greek symbols are intentionally outside Lost Cities' ordinary ASCII palette,
# so they cannot collide with a city-style palette entry.
FACADE_BLOCKS = {
    "α": "minecraft:polished_andesite",
    "β": "minecraft:light_gray_concrete",
    "γ": "minecraft:polished_deepslate",
    "δ": "minecraft:bricks",
    "ε": "minecraft:oxidized_cut_copper",
    "ζ": "minecraft:stripped_spruce_wood",
    "η": "minecraft:gray_stained_glass",
    "θ": "minecraft:light_blue_stained_glass",
    "ι": "minecraft:yellow_concrete",
    "κ": "minecraft:lime_concrete",
    "λ": "minecraft:red_concrete",
    "μ": "minecraft:smooth_quartz",
    "ν": "minecraft:iron_block",
    "ξ": "minecraft:dark_prismarine",
    "ο": "minecraft:calcite",
    "π": "minecraft:brown_terracotta",
    "ρ": (
        "minecraft:iron_bars"
        "[east=false,north=false,south=false,waterlogged=false,west=false]"
    ),
}
FACADE_SYMBOLS = frozenset(FACADE_BLOCKS)
HANDCRAFTED_SYMBOLS = frozenset(
    entry["char"]
    for entry in json.loads(
        (
            ROTWIRE_ROOT
            / "palettes"
            / "handcrafted_furnishings.json"
        ).read_text(encoding="utf-8")
    )["palette"]
)

EXTERIORS = {
    "quarantine": Exterior("δ", "ι", "η", "chamfer_balcony"),
    "clinical": Exterior("β", "μ", "θ", "recessed"),
    "civic": Exterior("γ", "α", "η", "ribbed"),
    "research": Exterior("ο", "ε", "θ", "stepped"),
    "industrial": Exterior("δ", "α", "η", "industrial"),
    "bunker": Exterior("γ", "ν", "η", "bunker"),
    "housing": Exterior("π", "ζ", "θ", "balconies"),
    "mixed_use": Exterior("α", "ξ", "η", "mixed_use"),
}

STORAGE_SYMBOLS = {
    "⑥",
    "⑦",
    "⑪",
    "⑫",
    "⑬",
    "⑭",
    "⑮",
    "⑯",
    "⑲",
    "⑳",
    "ⓑ",
    "ⓒ",
    "ⓓ",
    "ⓖ",
    "ⓗ",
}

# (back wall offset, front/access offset, cupboard, drawer). The selected
# symbol faces away from its supporting wall so players can open it.
ORIENTED_STORAGE = (
    ((0, 1), (0, -1), "⑥", "⑦"),
    ((0, -1), (0, 1), "⑮", "⑫"),
    ((-1, 0), (1, 0), "⑭", "⑪"),
    ((1, 0), (-1, 0), "⑯", "⑬"),
)

# Furnishings displaced during early stair-core prototyping. Restoring these
# source markers before every pass keeps the generator lossless and repeatable.
PRESERVED_FURNISHINGS = {
    ("building1", 3): ((9, 1, 14, "ⓛ"),),
    ("building1", 4): (
        (9, 1, 14, "ⓑ"),
        (10, 1, 14, "ⓑ"),
    ),
    ("building1", 6): (
        (9, 1, 14, "⑥"),
        (10, 1, 14, "⑦"),
    ),
    ("building1", 7): (
        (11, 1, 14, "ⓗ"),
        (12, 1, 14, "ⓖ"),
    ),
    ("building5", 4): ((6, 1, 1, "ⓐ"),),
}
EXPECTED_HANDCRAFTED_TOTALS = {
    "building1": 76,
    "building2": 34,
    "building3": 37,
    "building4": 32,
    "building5": 49,
    "building6": 55,
    "building7": 32,
    "building8": 82,
}
EXPECTED_LOOT_COUNTS = {
    "building1": (0, 0, 0, 0, 1, 0, 0, 0, 1),
    "building2": (0, 1, 1, 0),
    "building3": (0, 0, 0, 1),
    "building4": (1, 1, 0, 0),
    "building5": (0, 0, 0, 0),
    "building6": (1, 0, 0, 1),
    "building7": (1, 0, 1, 0, 1),
    "building8": (0, 1, 0, 1, 2, 0, 1, 2),
}


def load_part(family: str, index: int) -> dict:
    path = (
        ROTWIRE_ROOT
        / "parts"
        / family
        / f"{family}_{index}.json"
    )
    return json.loads(path.read_text(encoding="utf-8"))


def mutable_slices(part: dict) -> list[list[list[str]]]:
    return [
        [list(row) for row in layer]
        for layer in part["slices"]
    ]


def set_cell(
    slices: list[list[list[str]]],
    y: int,
    x: int,
    z: int,
    symbol: str,
) -> None:
    slices[y][z][x] = symbol


def facade_symbol(
    profile: Exterior,
    position: int,
    y: int,
    *,
    narrow_windows: bool = False,
) -> str:
    if position in (0, 1, 5, 10, 14, 15):
        return profile.accent
    if y in (2, 3, 4):
        if narrow_windows and position % 4 != 2:
            return profile.wall
        return profile.glass
    return profile.wall


def paint_perimeter(
    slices: list[list[list[str]]],
    profile: Exterior,
    *,
    narrow_windows: bool = False,
) -> None:
    for y in range(1, FLOOR_HEIGHT):
        for position in range(SIZE):
            north_south = facade_symbol(
                profile, position, y, narrow_windows=narrow_windows
            )
            east_west = facade_symbol(
                profile, position, y, narrow_windows=narrow_windows
            )
            set_cell(slices, y, position, 0, north_south)
            set_cell(slices, y, position, 15, north_south)
            set_cell(slices, y, 0, position, east_west)
            set_cell(slices, y, 15, position, east_west)


def chamfer_corners(
    slices: list[list[list[str]]],
    profile: Exterior,
    *,
    ground: bool,
    deep: bool = False,
) -> None:
    corners = (
        ((0, 0), (1, 0), (0, 1), (1, 1)),
        ((15, 0), (14, 0), (15, 1), (14, 1)),
        ((0, 15), (1, 15), (0, 14), (1, 14)),
        ((15, 15), (14, 15), (15, 14), (14, 14)),
    )
    for corner, edge_a, edge_b, inner in corners:
        cleared = (corner, edge_a, edge_b) if deep else (corner,)
        for x, z in cleared:
            for y in range(0 if not ground else 1, FLOOR_HEIGHT):
                set_cell(slices, y, x, z, " ")
        if deep:
            for y in range(1, FLOOR_HEIGHT):
                set_cell(
                    slices,
                    y,
                    inner[0],
                    inner[1],
                    profile.accent if y in (1, 5) else profile.glass,
                )


def recess_face(
    slices: list[list[list[str]]],
    profile: Exterior,
    side: str,
    start: int,
    end: int,
    *,
    ground: bool,
    balcony: bool = False,
) -> None:
    if side not in ("north", "south", "west", "east"):
        raise ValueError(f"Unknown façade side {side}")
    boundary = 0 if side in ("north", "west") else 15
    inner = 1 if boundary == 0 else 14

    for position in range(start, end + 1):
        x = position if side in ("north", "south") else boundary
        z = boundary if side in ("north", "south") else position
        if not ground and not balcony:
            set_cell(slices, 0, x, z, " ")
        for y in range(1, FLOOR_HEIGHT):
            set_cell(slices, y, x, z, " ")

        inner_x = position if side in ("north", "south") else inner
        inner_z = inner if side in ("north", "south") else position
        for y in range(1, FLOOR_HEIGHT):
            set_cell(
                slices,
                y,
                inner_x,
                inner_z,
                facade_symbol(profile, position, y),
            )

        if balcony:
            set_cell(slices, 1, x, z, "ρ")

    # A paired opening makes the recessed strip usable as a balcony or arcade.
    if balcony:
        doorway = (start + end) // 2
        for position in (doorway, min(doorway + 1, end)):
            inner_x = position if side in ("north", "south") else inner
            inner_z = inner if side in ("north", "south") else position
            for y in (1, 2):
                set_cell(slices, y, inner_x, inner_z, " ")


def add_facade_fins(
    slices: list[list[list[str]]],
    profile: Exterior,
    side: str,
    positions: tuple[int, ...],
) -> None:
    boundary = 0 if side in ("north", "west") else 15
    for position in positions:
        x = position if side in ("north", "south") else boundary
        z = boundary if side in ("north", "south") else position
        for y in range(1, FLOOR_HEIGHT):
            set_cell(slices, y, x, z, profile.accent)


def add_ground_entrance(
    slices: list[list[list[str]]],
    profile: Exterior,
) -> None:
    # Clear both the normal and recessed north wall positions. This stays
    # traversable regardless of the family's façade depth.
    for z in (0, 1):
        for x in (7, 8):
            set_cell(slices, 0, x, z, "#")
            for y in (1, 2, 3):
                set_cell(slices, y, x, z, " ")
    for x in (6, 9):
        for y in (1, 2, 3):
            set_cell(slices, y, x, 0, profile.accent)
    for x in range(6, 10):
        set_cell(slices, 4, x, 0, profile.accent)


def apply_exterior(
    slices: list[list[list[str]]],
    family: Family,
    *,
    ground: bool,
) -> None:
    profile = EXTERIORS[family.exterior]
    narrow = profile.shape in ("industrial", "bunker")
    paint_perimeter(slices, profile, narrow_windows=narrow)

    if profile.shape == "chamfer_balcony":
        chamfer_corners(slices, profile, ground=ground)
        recess_face(
            slices, profile, "south", 3, 7, ground=ground, balcony=not ground
        )
    elif profile.shape == "recessed":
        recess_face(slices, profile, "north", 4, 11, ground=ground)
        recess_face(slices, profile, "south", 4, 11, ground=ground)
    elif profile.shape == "ribbed":
        chamfer_corners(slices, profile, ground=ground)
        add_facade_fins(slices, profile, "north", (2, 5, 10, 13))
        add_facade_fins(slices, profile, "south", (2, 5, 10, 13))
    elif profile.shape == "stepped":
        chamfer_corners(slices, profile, ground=ground, deep=True)
        recess_face(slices, profile, "east", 5, 10, ground=ground)
    elif profile.shape == "industrial":
        recess_face(slices, profile, "north", 3, 6, ground=ground)
        recess_face(slices, profile, "north", 9, 12, ground=ground)
        add_facade_fins(slices, profile, "south", (3, 7, 11))
    elif profile.shape == "bunker":
        chamfer_corners(slices, profile, ground=ground, deep=True)
        add_facade_fins(slices, profile, "north", (2, 6, 10, 13))
        add_facade_fins(slices, profile, "south", (2, 6, 10, 13))
    elif profile.shape == "balconies":
        chamfer_corners(slices, profile, ground=ground)
        recess_face(
            slices, profile, "north", 2, 5, ground=ground, balcony=not ground
        )
        recess_face(
            slices, profile, "south", 10, 13, ground=ground, balcony=not ground
        )
    elif profile.shape == "mixed_use":
        chamfer_corners(slices, profile, ground=ground, deep=True)
        recess_face(slices, profile, "west", 4, 11, ground=ground)
        add_facade_fins(slices, profile, "east", (3, 8, 12))

    if ground:
        add_ground_entrance(slices, profile)


def remove_ladders(slices: list[list[list[str]]]) -> None:
    for y, layer in enumerate(slices):
        for row in layer:
            for x, symbol in enumerate(row):
                if symbol == "l":
                    # Close abandoned floor openings, but leave the old ladder
                    # shaft unobstructed at head height.
                    row[x] = "#" if y == 0 else " "


def restore_preserved_furnishings(
    name: str,
    index: int,
    slices: list[list[list[str]]],
) -> None:
    for x, y, z, symbol in PRESERVED_FURNISHINGS.get((name, index), ()):
        set_cell(slices, y, x, z, symbol)


def clear_stair_core(
    slices: list[list[list[str]]],
    stair_x: int,
    stair_z: int,
) -> None:
    end_z = stair_z + 5
    for y in range(1, FLOOR_HEIGHT):
        for z in range(stair_z, end_z + 1):
            for x in (stair_x, stair_x + 1):
                set_cell(slices, y, x, z, " ")

    # Four-wide vestibules immediately before and after the flight prevent an
    # inherited room partition from leaving it unreachable. Keeping the carve
    # to the adjacent row avoids erasing useful furniture farther into a room.
    for y in (1, 2, 3):
        for z in (stair_z - 1, end_z + 1):
            for x in range(stair_x - 1, stair_x + 3):
                set_cell(slices, y, x, z, " ")
            egress_x = (
                stair_x + 3
                if stair_x + 3 < SIZE - 1
                else stair_x - 2
            )
            set_cell(slices, y, egress_x, z, " ")

    # The two southern industrial families start close to the north wall.
    # Their recessed façades need one additional interior approach row.
    if stair_z == 3:
        for y in (1, 2, 3):
            for x in range(stair_x - 1, stair_x + 3):
                set_cell(slices, y, x, 1, " ")


def add_stair_flight(
    slices: list[list[list[str]]],
    *,
    stair_x: int,
    stair_z: int,
    arriving_from_below: bool,
) -> None:
    end_z = stair_z + 5
    clear_stair_core(slices, stair_x, stair_z)

    # Normalize the footprint so rerunning the generator is idempotent.
    for z in range(stair_z, end_z + 1):
        for x in (stair_x, stair_x + 1):
            set_cell(slices, 0, x, z, "#")

    if arriving_from_below:
        for z in range(stair_z + 2, end_z):
            for x in (stair_x, stair_x + 1):
                set_cell(slices, 0, x, z, " ")
        for x in (stair_x, stair_x + 1):
            set_cell(slices, 0, x, end_z, "c")

    for y in range(1, FLOOR_HEIGHT):
        lower_z = stair_z + y - 1
        upper_z = lower_z + 1
        for x in (stair_x, stair_x + 1):
            set_cell(slices, y, x, lower_z, "c")
            set_cell(slices, y, x, upper_z, "d")

    # Low iron-bar balustrades keep the industrial stairwell readable while
    # leaving its two landings open to the surrounding floor.
    for z in range(stair_z + 1, end_z + 1):
        set_cell(slices, 1, stair_x - 1, z, ":")
        set_cell(slices, 1, stair_x + 2, z, ":")


def serialize_part(template: dict, slices: list[list[list[str]]]) -> dict:
    part = copy.deepcopy(template)
    part["slices"] = [
        ["".join(row) for row in layer]
        for layer in slices
    ]
    part["refpalette"] = "rotwire:furnished_facades"
    return part


def storage_count(slices: list[list[list[str]]]) -> int:
    return sum(
        symbol in STORAGE_SYMBOLS
        for layer in slices
        for row in layer
        for symbol in row
    )


def projected_stair_cells(
    slices: list[list[list[str]]],
) -> set[tuple[int, int]]:
    cells = set()
    for layer in slices:
        for z, row in enumerate(layer):
            for x, symbol in enumerate(row):
                if symbol in ("c", "d"):
                    cells.add((x, z))
    return cells


def near_stair(
    x: int,
    z: int,
    stairs: set[tuple[int, int]],
) -> bool:
    return any(
        abs(x - stair_x) + abs(z - stair_z) <= 2
        for stair_x, stair_z in stairs
    )


def has_clear_body_space(
    slices: list[list[list[str]]],
    x: int,
    z: int,
) -> bool:
    return (
        slices[0][z][x] != " "
        and slices[1][z][x] == " "
        and slices[2][z][x] == " "
    )


def ensure_storage_layout(
    slices: list[list[list[str]]],
    *,
    minimum: int = 3,
    reserved: frozenset[tuple[int, int]] = frozenset(),
) -> None:
    """Add accessible wall-backed Handcrafted storage to sparse floors."""

    stairs = projected_stair_cells(slices)
    storage_index = storage_count(slices)
    if storage_index >= minimum:
        return

    for z in range(1, SIZE - 1):
        for x in range(1, SIZE - 1):
            if storage_index >= minimum:
                return
            if (
                not has_clear_body_space(slices, x, z)
                or near_stair(x, z, stairs)
                or (x, z) in reserved
            ):
                continue

            for (
                (back_x, back_z),
                (front_x, front_z),
                cupboard,
                drawer,
            ) in ORIENTED_STORAGE:
                bx, bz = x + back_x, z + back_z
                fx, fz = x + front_x, z + front_z
                if (
                    slices[1][bz][bx] == " "
                    or not has_clear_body_space(slices, fx, fz)
                    or near_stair(fx, fz, stairs)
                    or (fx, fz) in reserved
                ):
                    continue
                set_cell(
                    slices,
                    1,
                    x,
                    z,
                    cupboard if storage_index % 2 == 0 else drawer,
                )
                storage_index += 1
                break

    if storage_index < minimum:
        raise ValueError(
            f"Could only place {storage_index} accessible storage markers"
        )


def ensure_loot_layout(
    slices: list[list[list[str]]],
    *,
    expected: int,
    reserved: frozenset[tuple[int, int]],
) -> None:
    loot_count = sum(
        symbol == "C"
        for layer in slices
        for row in layer
        for symbol in row
    )
    if loot_count > expected:
        raise ValueError(
            f"Expected at most {expected} loot markers, found {loot_count}"
        )
    if loot_count == expected:
        return

    stairs = projected_stair_cells(slices)
    for z in range(1, SIZE - 1):
        for x in range(1, SIZE - 1):
            if loot_count >= expected:
                return
            if (
                (x, z) in reserved
                or near_stair(x, z, stairs)
                or not has_clear_body_space(slices, x, z)
            ):
                continue
            set_cell(slices, 1, x, z, "C")
            loot_count += 1

    if loot_count < expected:
        raise ValueError(
            f"Could only place {loot_count} of {expected} loot markers"
        )


def handcrafted_counts(
    slices: list[list[list[str]]],
) -> dict[str, int]:
    counts: dict[str, int] = {}
    for layer in slices:
        for row in layer:
            for symbol in row:
                if symbol in HANDCRAFTED_SYMBOLS:
                    counts[symbol] = counts.get(symbol, 0) + 1
    return counts


def relocate_displaced_furnishings(
    before: dict[str, int],
    slices: list[list[list[str]]],
    *,
    reserved: frozenset[tuple[int, int]] = frozenset(),
) -> None:
    """Move furnishings sacrificed by the stair carve into usable room space."""

    after = handcrafted_counts(slices)
    stairs = projected_stair_cells(slices)
    missing = [
        symbol
        for symbol, expected in before.items()
        for _ in range(max(0, expected - after.get(symbol, 0)))
    ]

    for symbol in missing:
        placed = False
        for z in range(1, SIZE - 1):
            for x in range(1, SIZE - 1):
                if (
                    not has_clear_body_space(slices, x, z)
                    or near_stair(x, z, stairs)
                    or (x, z) in reserved
                ):
                    continue
                neighbors = (
                    (x - 1, z),
                    (x + 1, z),
                    (x, z - 1),
                    (x, z + 1),
                )
                if not any(
                    has_clear_body_space(slices, nx, nz)
                    and not near_stair(nx, nz, stairs)
                    for nx, nz in neighbors
                ):
                    continue
                set_cell(slices, 1, x, z, symbol)
                placed = True
                break
            if placed:
                break
        if not placed:
            raise ValueError(
                f"Could not relocate displaced Handcrafted marker {symbol}"
            )


def retrofit_floor(
    template: dict,
    family: Family,
    *,
    name: str,
    index: int,
    arriving_from_below: bool,
    ground: bool,
) -> dict:
    slices = mutable_slices(template)
    if template.get("refpalette") != "rotwire:furnished_facades":
        restore_preserved_furnishings(name, index, slices)
    furnishings_before = handcrafted_counts(slices)
    reserved = (
        frozenset((x, z) for x in (7, 8) for z in (0, 1))
        if ground
        else frozenset()
    )
    apply_exterior(slices, family, ground=ground)
    remove_ladders(slices)
    add_stair_flight(
        slices,
        stair_x=family.stair_x,
        stair_z=family.stair_z,
        arriving_from_below=arriving_from_below,
    )
    ensure_loot_layout(
        slices,
        expected=EXPECTED_LOOT_COUNTS[name][index - 1],
        reserved=reserved,
    )
    relocate_displaced_furnishings(
        furnishings_before, slices, reserved=reserved
    )
    ensure_storage_layout(slices, reserved=reserved)
    if ground:
        add_ground_entrance(slices, EXTERIORS[family.exterior])
    return serialize_part(template, slices)


def blank_slices() -> list[list[list[str]]]:
    return [
        [[" " for _ in range(SIZE)] for _ in range(SIZE)]
        for _ in range(FLOOR_HEIGHT)
    ]


def horizontal(
    slices: list[list[list[str]]],
    y: int,
    z: int,
    x1: int,
    x2: int,
    symbol: str,
) -> None:
    for x in range(x1, x2 + 1):
        set_cell(slices, y, x, z, symbol)


def vertical(
    slices: list[list[list[str]]],
    y: int,
    x: int,
    z1: int,
    z2: int,
    symbol: str,
) -> None:
    for z in range(z1, z2 + 1):
        set_cell(slices, y, x, z, symbol)


def rectangle(
    slices: list[list[list[str]]],
    y: int,
    x1: int,
    z1: int,
    x2: int,
    z2: int,
    symbol: str,
) -> None:
    horizontal(slices, y, z1, x1, x2, symbol)
    horizontal(slices, y, z2, x1, x2, symbol)
    vertical(slices, y, x1, z1, z2, symbol)
    vertical(slices, y, x2, z1, z2, symbol)


def fill_rectangle(
    slices: list[list[list[str]]],
    y: int,
    x1: int,
    z1: int,
    x2: int,
    z2: int,
    symbol: str,
) -> None:
    for z in range(z1, z2 + 1):
        horizontal(slices, y, z, x1, x2, symbol)


def make_roof(family: Family) -> dict:
    slices = blank_slices()
    stair_x = family.stair_x
    stair_z = family.stair_z
    end_z = stair_z + 5
    profile = EXTERIORS[family.exterior]

    fill_rectangle(slices, 0, 0, 0, 15, 15, "#")
    rectangle(slices, 1, 0, 0, 15, 15, profile.wall)

    if profile.shape in (
        "chamfer_balcony",
        "ribbed",
        "stepped",
        "bunker",
        "balconies",
        "mixed_use",
    ):
        chamfer_corners(
            slices,
            profile,
            ground=False,
            deep=profile.shape in ("stepped", "bunker", "mixed_use"),
        )
    if profile.shape in ("recessed", "industrial"):
        for x in range(4, 12):
            set_cell(slices, 0, x, 0, " ")
            set_cell(slices, 1, x, 0, " ")
            set_cell(slices, 1, x, 1, profile.accent)

    for z in range(stair_z + 2, end_z):
        for x in (stair_x, stair_x + 1):
            set_cell(slices, 0, x, z, " ")
    for x in (stair_x, stair_x + 1):
        set_cell(slices, 0, x, end_z, "c")

    # A weatherproof roof bulkhead replaces the usual exposed ladder hatch.
    for y in (1, 2, 3):
        rectangle(
            slices,
            y,
            stair_x - 1,
            stair_z + 1,
            stair_x + 2,
            end_z + 1,
            profile.wall,
        )
        set_cell(slices, y, stair_x - 1, end_z, " ")
    fill_rectangle(
        slices,
        4,
        stair_x - 1,
        stair_z + 1,
        stair_x + 2,
        end_z + 1,
        profile.accent,
    )
    set_cell(slices, 2, stair_x + 2, stair_z + 2, ":")
    set_cell(slices, 3, stair_x + 2, stair_z + 2, ":")

    # Municipal HVAC pads and an emergency-radio mast support the pack's
    # quarantine/evacuation narrative without introducing new palette symbols.
    for x, z in ((2, 3), (3, 3), (2, 4), (3, 4), (5, 3), (6, 3)):
        set_cell(slices, 1, x, z, profile.accent)
    for y in (1, 2, 3, 4):
        set_cell(slices, y, 6, 5, ":")

    return {
        "xsize": SIZE,
        "zsize": SIZE,
        "slices": [
            ["".join(row) for row in layer]
            for layer in slices
        ],
        "refpalette": "rotwire:furnished_facades",
    }


def building_definition(name: str, family: Family) -> dict:
    normal_parts = [
        {
            "top": False,
            "range": "1,100",
            "part": f"rotwire:{name}/{name}_{index}",
        }
        for index in range(1, family.part_count + 1)
    ]
    return {
        "filler": "#",
        "rubble": "}",
        "mincellars": 0,
        "maxcellars": 0,
        "minfloors": family.min_floors,
        "maxfloors": family.max_floors,
        "allowDoors": True,
        "allowFillers": False,
        "overrideFloors": True,
        "parts": [
            {
                "floor": 0,
                "part": f"rotwire:stair_retrofits/{name}_ground",
            },
            *normal_parts,
            {
                "top": True,
                "part": f"rotwire:stair_retrofits/{name}_roof",
            },
        ],
    }


def validate_part(name: str, part: dict, expected_stairs: int) -> None:
    slices = part.get("slices", [])
    if len(slices) != FLOOR_HEIGHT:
        raise ValueError(f"{name}: expected {FLOOR_HEIGHT} slices")
    stair_count = 0
    for y, layer in enumerate(slices):
        if len(layer) != SIZE:
            raise ValueError(f"{name}: slice {y} has {len(layer)} rows")
        for z, row in enumerate(layer):
            if len(row) != SIZE:
                raise ValueError(
                    f"{name}: slice {y}, row {z} has width {len(row)}"
                )
            if "l" in row:
                raise ValueError(f"{name}: forbidden ladder marker")
            stair_count += row.count("c") + row.count("d")
    if stair_count != expected_stairs:
        raise ValueError(
            f"{name}: expected {expected_stairs} stairs, got {stair_count}"
        )


def count_handcrafted_symbols(parts: list[dict]) -> int:
    return sum(
        1
        for part in parts
        for layer in part["slices"]
        for row in layer
        for symbol in row
        if symbol in HANDCRAFTED_SYMBOLS
    )


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def write_furnished_facade_palette() -> None:
    source = (
        ROTWIRE_ROOT
        / "palettes"
        / "handcrafted_furnishings.json"
    )
    palette = json.loads(source.read_text(encoding="utf-8"))
    used = {
        entry["char"]
        for entry in palette["palette"]
    }
    for symbol, block in FACADE_BLOCKS.items():
        if symbol in used:
            raise ValueError(f"Duplicate façade palette symbol {symbol}")
        palette["palette"].append({"char": symbol, "block": block})
    write_json(
        ROTWIRE_ROOT / "palettes" / "furnished_facades.json",
        palette,
    )


def generate() -> None:
    write_furnished_facade_palette()
    generated_floors = 0
    for name, family in FAMILIES.items():
        templates = [
            load_part(name, index)
            for index in range(1, family.part_count + 1)
        ]
        ground = retrofit_floor(
            templates[0],
            family,
            name=name,
            index=1,
            arriving_from_below=False,
            ground=True,
        )
        validate_part(f"{name}_ground", ground, 20)
        write_json(
            ROTWIRE_ROOT
            / "parts"
            / "stair_retrofits"
            / f"{name}_ground.json",
            ground,
        )

        retrofitted = []
        for index, template in enumerate(templates, start=1):
            part = retrofit_floor(
                template,
                family,
                name=name,
                index=index,
                arriving_from_below=True,
                ground=False,
            )
            validate_part(f"{name}_{index}", part, 22)
            retrofitted.append(part)
            write_json(
                ROTWIRE_ROOT
                / "parts"
                / name
                / f"{name}_{index}.json",
                part,
            )
            generated_floors += 1

        furniture_after = count_handcrafted_symbols(retrofitted)
        expected_furniture = EXPECTED_HANDCRAFTED_TOTALS[name]
        if furniture_after < expected_furniture:
            raise ValueError(
                f"{name}: expected at least {expected_furniture} "
                f"Handcrafted markers, found {furniture_after}"
            )
        for index, part in enumerate(retrofitted, start=1):
            if storage_count(mutable_slices(part)) < 3:
                raise ValueError(
                    f"{name}_{index}: fewer than three storage markers"
                )

        roof = make_roof(family)
        validate_part(f"{name}_roof", roof, 2)
        write_json(
            ROTWIRE_ROOT
            / "parts"
            / "stair_retrofits"
            / f"{name}_roof.json",
            roof,
        )

        definition = building_definition(name, family)
        for namespace in ("lostcities", "lcmt"):
            write_json(
                RESOURCE_ROOT
                / "data"
                / namespace
                / "lostcities"
                / "buildings"
                / f"{name}.json",
                definition,
            )

        print(
            f"{name}: {family.lore}, "
            f"{family.part_count} furnished floors, "
            f"{furniture_after} Handcrafted markers retained"
        )

    print(
        f"Generated {generated_floors} retrofitted floors, "
        f"{len(FAMILIES)} ground floors, {len(FAMILIES)} roof exits, "
        f"{len(FAMILIES) * 2} building overrides, and checked storage "
        f"across {generated_floors} furnished floors."
    )


if __name__ == "__main__":
    generate()

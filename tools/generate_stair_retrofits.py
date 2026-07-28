"""Replace Lost Cities' ladder tower families with zombie-traversable stairs.

The Biohazard pack already decorates the LCMT versions of building families
1-8 with Handcrafted furniture. Families 1, 2, 3, 5, 6, and 8 still inherited
the original vertical ladder shaft. This generator keeps those decorated floor
plans, removes every ladder marker, and installs the same proven two-wide stair
flight used by Biohazard's custom buildings.

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
BIOHAZARD_ROOT = (
    RESOURCE_ROOT / "data" / "biohazard" / "lostcities"
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


# These placements were scored across every decorated variant. They preserve
# all Handcrafted symbols and avoid every vanilla loot-chest marker.
FAMILIES = {
    "building1": Family(9, 10, 7, 5, 8, "quarantine apartments"),
    "building2": Family(4, 11, 5, 5, 7, "urgent-care annex"),
    "building3": Family(4, 11, 6, 5, 8, "civil-defense offices"),
    "building4": Family(4, 10, 8, 5, 7, "emergency research annex"),
    "building5": Family(4, 4, 3, 5, 7, "municipal maintenance depot"),
    "building6": Family(4, 5, 3, 5, 7, "evacuation shelter"),
    "building7": Family(5, 10, 8, 5, 8, "public housing tower"),
    "building8": Family(8, 12, 7, 6, 9, "mixed-use relief center"),
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


def load_part(family: str, index: int) -> dict:
    path = (
        BIOHAZARD_ROOT
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
    part["refpalette"] = "biohazard:handcrafted_furnishings"
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


def handcrafted_counts(
    slices: list[list[list[str]]],
) -> dict[str, int]:
    counts: dict[str, int] = {}
    for layer in slices:
        for row in layer:
            for symbol in row:
                if ord(symbol) > 127:
                    counts[symbol] = counts.get(symbol, 0) + 1
    return counts


def relocate_displaced_furnishings(
    before: dict[str, int],
    slices: list[list[list[str]]],
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
) -> dict:
    slices = mutable_slices(template)
    restore_preserved_furnishings(name, index, slices)
    furnishings_before = handcrafted_counts(slices)
    remove_ladders(slices)
    add_stair_flight(
        slices,
        stair_x=family.stair_x,
        stair_z=family.stair_z,
        arriving_from_below=arriving_from_below,
    )
    relocate_displaced_furnishings(furnishings_before, slices)
    ensure_storage_layout(slices)
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

    fill_rectangle(slices, 0, 0, 0, 15, 15, "#")
    rectangle(slices, 1, 0, 0, 15, 15, "#")

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
            "#",
        )
        set_cell(slices, y, stair_x - 1, end_z, " ")
    fill_rectangle(
        slices,
        4,
        stair_x - 1,
        stair_z + 1,
        stair_x + 2,
        end_z + 1,
        "S",
    )
    set_cell(slices, 2, stair_x + 2, stair_z + 2, ":")
    set_cell(slices, 3, stair_x + 2, stair_z + 2, ":")

    # Municipal HVAC pads and an emergency-radio mast support the pack's
    # quarantine/evacuation narrative without introducing new palette symbols.
    for x, z in ((2, 3), (3, 3), (2, 4), (3, 4), (5, 3), (6, 3)):
        set_cell(slices, 1, x, z, "S")
    for y in (1, 2, 3, 4):
        set_cell(slices, y, 6, 5, ":")

    return {
        "xsize": SIZE,
        "zsize": SIZE,
        "slices": [
            ["".join(row) for row in layer]
            for layer in slices
        ],
        "refpalette": "biohazard:handcrafted_furnishings",
    }


def building_definition(name: str, family: Family) -> dict:
    normal_parts = [
        {
            "top": False,
            "range": "1,100",
            "part": f"biohazard:{name}/{name}_{index}",
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
                "part": f"biohazard:stair_retrofits/{name}_ground",
            },
            *normal_parts,
            {
                "top": True,
                "part": f"biohazard:stair_retrofits/{name}_roof",
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
        if ord(symbol) > 127
    )


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def generate() -> None:
    generated_floors = 0
    for name, family in FAMILIES.items():
        templates = [
            load_part(name, index)
            for index in range(1, family.part_count + 1)
        ]
        furniture_before = count_handcrafted_symbols(templates)

        ground = retrofit_floor(
            templates[0],
            family,
            name=name,
            index=1,
            arriving_from_below=False,
        )
        validate_part(f"{name}_ground", ground, 20)
        write_json(
            BIOHAZARD_ROOT
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
            )
            validate_part(f"{name}_{index}", part, 22)
            retrofitted.append(part)
            write_json(
                BIOHAZARD_ROOT
                / "parts"
                / name
                / f"{name}_{index}.json",
                part,
            )
            generated_floors += 1

        furniture_after = count_handcrafted_symbols(retrofitted)
        if furniture_after < furniture_before:
            raise ValueError(
                f"{name}: Handcrafted markers decreased from "
                f"{furniture_before} to {furniture_after}"
            )
        for index, part in enumerate(retrofitted, start=1):
            if storage_count(mutable_slices(part)) < 3:
                raise ValueError(
                    f"{name}_{index}: fewer than three storage markers"
                )

        roof = make_roof(family)
        validate_part(f"{name}_roof", roof, 2)
        write_json(
            BIOHAZARD_ROOT
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

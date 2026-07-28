"""Audit candidate two-wide stair cores in the decorated LCMT floor parts."""

from __future__ import annotations

import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PART_ROOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "rotwire"
    / "lostcities"
    / "parts"
)
FAMILIES = {
    "building1": 9,
    "building2": 4,
    "building3": 4,
    "building5": 4,
    "building6": 4,
    "building8": 8,
}
LOW_COST = set(" #a@`SG")


def affected_cells(anchor_x: int, start_z: int) -> set[tuple[int, int, int]]:
    cells: set[tuple[int, int, int]] = set()
    end_z = start_z + 5
    for z in range(start_z + 2, end_z):
        for x in (anchor_x, anchor_x + 1):
            cells.add((0, x, z))
    for x in (anchor_x, anchor_x + 1):
        cells.add((0, x, end_z))
    for y in range(1, 6):
        for z in range(start_z, end_z + 1):
            for x in (anchor_x, anchor_x + 1):
                cells.add((y, x, z))
    for z in range(start_z + 1, end_z + 1):
        for x in (anchor_x - 1, anchor_x + 2):
            cells.add((1, x, z))
    # Keep both ends open so the flight cannot terminate in a sealed room
    # partition. Include the side-adjacent cells in the cost as well.
    for y in (1, 2, 3):
        for x in (anchor_x, anchor_x + 1):
            cells.add((y, x, start_z - 1))
            cells.add((y, x, end_z + 1))
        for x in (anchor_x - 1, anchor_x + 2):
            cells.add((y, x, start_z - 1))
            cells.add((y, x, end_z + 1))
    return cells


def score(parts: list[dict], anchor_x: int, start_z: int) -> tuple:
    chars: Counter[str] = Counter()
    for part in parts:
        slices = part["slices"]
        for y, x, z in affected_cells(anchor_x, start_z):
            chars[slices[y][z][x]] += 1
    furniture = sum(
        count for char, count in chars.items() if ord(char) > 127
    )
    ladders = chars["l"]
    expensive_ascii = sum(
        count
        for char, count in chars.items()
        if ord(char) <= 127 and char not in LOW_COST and char != "l"
    )
    structure = sum(chars[char] for char in LOW_COST if char != " ")
    total = furniture * 100 + expensive_ascii * 15 + structure
    return total, furniture, expensive_ascii, structure, -ladders, chars


def main() -> None:
    for family, count in FAMILIES.items():
        parts = [
            json.loads(
                (PART_ROOT / family / f"{family}_{index}.json").read_text(
                    encoding="utf-8"
                )
            )
            for index in range(1, count + 1)
        ]
        candidates = []
        for anchor_x in range(2, 13):
            for start_z in range(2, 9):
                candidates.append(
                    (
                        score(parts, anchor_x, start_z),
                        anchor_x,
                        start_z,
                    )
                )
        candidates.sort(key=lambda candidate: candidate[0][:-1])
        print(f"\n{family}")
        for result, anchor_x, start_z in candidates[:8]:
            total, furniture, expensive, structure, negative_ladders, chars = result
            print(
                f"  x={anchor_x:2d} z={start_z:2d}"
                f" score={total:5d}"
                f" furniture={furniture:2d}"
                f" decor/loot={expensive:3d}"
                f" structure={structure:3d}"
                f" ladders={-negative_ladders:2d}"
                f" chars={dict(chars)}"
            )


if __name__ == "__main__":
    main()

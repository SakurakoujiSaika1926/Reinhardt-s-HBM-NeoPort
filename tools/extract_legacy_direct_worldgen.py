from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OLD_DUNGEON = ROOT.parents[1] / "HBM_1.7.10" / "src" / "main" / "java" / "com" / "hbm" / "world" / "dungeon"
OUT = ROOT / "src" / "main" / "resources" / "data" / "reinhardtshbm" / "legacy_worldgen"


STRUCTURES = {
    "desert_atom": ["DesertAtom001.java", "DesertAtom002.java", "DesertAtom003.java"],
    "library_dungeon": ["LibraryDungeon.java"],
    "spaceship": ["Spaceship.java", "Spaceship2.java"],
    "waste_tank": ["Barrel.java"],
}

VANILLA_ALIASES = {
    "air": "air",
    "bedrock": "bedrock",
    "chest": "chest",
    "double_wooden_slab": "double_wooden_slab",
    "fence": "oak_fence",
    "flower_pot": "flower_pot",
    "gravel": "gravel",
    "iron_door": "iron_door",
    "ladder": "ladder",
    "lever": "lever",
    "mob_spawner": "spawner",
    "oak_stairs": "oak_stairs",
    "redstone_wire": "redstone_wire",
    "stone_brick_stairs": "stone_brick_stairs",
    "stone_stairs": "stone_stairs",
    "stonebrick": "stonebrick",
    "unpowered_repeater": "repeater",
    "vine": "vine",
    "wall_sign": "oak_wall_sign",
    "water": "water",
    "web": "web",
    "wooden_door": "oak_door",
    "wooden_slab": "wooden_slab",
}

HBM_ALIASES = {
    # The 1.7.10 field is registered with block name "crashed_bomb" and its
    # metadata selects the balefire/conventional/nuke/salted variant.
    "crashed_balefire": "crashed_bomb",
}

LOOT = {
    "desert_atom": [
        (9, -4, 14, "POOL_NUKE_MISC", 10),
        (36, 0, 12, "POOL_GENERIC", 8),
        (22, 0, 13, "POOL_GENERIC", 8),
        (24, 0, 26, "POOL_NUKE_TRASH", 8),
        (18, 1, 16, "POOL_EXPENSIVE", 12),
        (36, 4, 9, "POOL_NUKE_MISC", 12),
    ],
    "spaceship": [
        (5, -2, 25, "POOL_SPACESHIP", 12),
        (8, -2, 25, "POOL_SPACESHIP", 12),
        (5, -2, 26, "POOL_SPACESHIP", 12),
        (8, -2, 26, "POOL_SPACESHIP", 12),
        (8, -2, 38, "POOL_EXPENSIVE", 12),
    ],
    "waste_tank": [
        (2, 1, 2, "POOL_EXPENSIVE", 16),
    ],
}

DUNGEON_LOOT = {
    "library_dungeon": [
        (1, 1, 5),
        (1, 1, 8),
        (1, 2, 3),
        (1, 4, 5),
        (2, 4, 9),
        (1, 5, 7),
        (6, 6, 1),
        (7, 6, 4),
    ],
}

SPAWNERS = {
    "library_dungeon": [
        (6, 1, 2),
        (6, 1, 8),
        (3, 4, 5),
        (5, 6, 4),
    ],
}


SET_BLOCK_RE = re.compile(
    r"world\.setBlock\(\s*"
    r"x\s*(?:\+\s*)?([-+]?\d+)\s*,\s*"
    r"y\s*(?:\+\s*)?([-+]?\d+)\s*,\s*"
    r"z\s*(?:\+\s*)?([-+]?\d+)\s*,\s*"
    r"([^,]+?)\s*,\s*"
    r"([^,]+?)\s*,\s*"
    r"\d+\s*\)"
)
DOOR_RE = re.compile(
    r"ItemDoor\.placeDoorBlock\(\s*world\s*,\s*"
    r"x\s*(?:\+\s*)?([-+]?\d+)\s*,\s*"
    r"y\s*(?:\+\s*)?([-+]?\d+)\s*,\s*"
    r"z\s*(?:\+\s*)?([-+]?\d+)\s*,\s*"
    r"([-+]?\d+)\s*,\s*"
    r"(Blocks\.[A-Za-z0-9_]+)\s*\)"
)
VAR_RE = re.compile(r"\bBlock\s+(\w+)\s*=\s*(ModBlocks|Blocks)\.([A-Za-z0-9_]+)\s*;")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//.*?$", "", text, flags=re.M)
    return text


def signed_int(value: str) -> int:
    return int(value.replace("+", ""))


def hbm_id(name: str) -> str:
    return f"reinhardtshbm:{HBM_ALIASES.get(name, name)}"


def vanilla_id(name: str) -> str:
    mapped = VANILLA_ALIASES.get(name)
    if mapped is None:
        raise ValueError(f"Missing vanilla mapping for Blocks.{name}")
    return f"minecraft:{mapped}"


def resolve_expr(expr: str, variables: dict[str, str]) -> tuple[str, str]:
    expr = expr.strip()
    if expr == "Library.getRandomConcrete()":
        return ("S", "random_concrete")
    if expr == "getBrick(rand)":
        return ("S", "library_brick")
    if expr == "getShelf(rand)":
        return ("S", "library_shelf")
    if expr in variables:
        return ("B", variables[expr])
    if expr == "sellafield":
        return ("B", hbm_id("sellafield"))
    if expr.startswith("ModBlocks."):
        return ("B", hbm_id(expr.split(".", 1)[1]))
    if expr.startswith("Blocks."):
        return ("B", vanilla_id(expr.split(".", 1)[1]))
    raise ValueError(f"Unhandled block expression: {expr}")


def extract_file(path: Path) -> list[tuple[int, str]]:
    text = strip_comments(path.read_text(encoding="utf-8"))
    variables: dict[str, str] = {}
    for match in VAR_RE.finditer(text):
        var, owner, name = match.groups()
        variables[var] = hbm_id(name) if owner == "ModBlocks" else vanilla_id(name)

    events: list[tuple[int, str]] = []
    for match in SET_BLOCK_RE.finditer(text):
        lx, ly, lz = (signed_int(match.group(i)) for i in range(1, 4))
        expr = match.group(4).strip()
        meta = match.group(5).strip()
        kind, value = resolve_expr(expr, variables)
        if kind == "B" and meta == "getBrick(rand)":
            line = f"S\t{lx}\t{ly}\t{lz}\tlibrary_brick\t0"
        elif kind == "B":
            line = f"B\t{lx}\t{ly}\t{lz}\t{value}\t{meta}"
        else:
            line = f"S\t{lx}\t{ly}\t{lz}\t{value}\t{meta}"
        events.append((match.start(), line))

    for match in DOOR_RE.finditer(text):
        lx, ly, lz = (signed_int(match.group(i)) for i in range(1, 4))
        facing = signed_int(match.group(4))
        block = vanilla_id(match.group(5).split(".", 1)[1])
        events.append((match.start(), f"D\t{lx}\t{ly}\t{lz}\t{block}\t{facing}"))

    events.sort(key=lambda event: event[0])
    return events


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, files in STRUCTURES.items():
        lines = [
            "# Generated from HBM 1.7.10 world/dungeon Java sources by tools/extract_legacy_direct_worldgen.py",
            "# kind, x, y, z, value, meta/count",
        ]
        block_count = 0
        for file_name in files:
            events = extract_file(OLD_DUNGEON / file_name)
            lines.append(f"# source {file_name}")
            for _, line in events:
                lines.append(line)
                block_count += 1
        for lx, ly, lz, pool, count in LOOT.get(name, []):
            lines.append(f"L\t{lx}\t{ly}\t{lz}\t{pool}\t{count}")
        for lx, ly, lz in DUNGEON_LOOT.get(name, []):
            lines.append(f"V\t{lx}\t{ly}\t{lz}\tminecraft:simple_dungeon\t0")
        for lx, ly, lz in SPAWNERS.get(name, []):
            lines.append(f"M\t{lx}\t{ly}\t{lz}\tlegacy_dungeon_mob\t0")
        (OUT / f"{name}.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"{name}: {block_count} source block/door events -> {OUT / (name + '.tsv')}")


if __name__ == "__main__":
    main()

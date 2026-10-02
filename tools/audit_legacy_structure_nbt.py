#!/usr/bin/env python3
"""Read-only audit utility for HBM's compressed legacy structure templates.

The shipped templates use the 1.7.10-era numeric metadata format rather than
Minecraft's modern structure palette.  Keeping this parser dependency-free
makes the audit usable from Gradle and from a clean checkout.
"""

from __future__ import annotations

import argparse
import json
import struct
import zlib
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, BinaryIO


class NbtError(RuntimeError):
    pass


def _read_exact(stream: BinaryIO, length: int) -> bytes:
    data = stream.read(length)
    if len(data) != length:
        raise NbtError(f"Unexpected end of NBT data: expected {length} bytes, got {len(data)}")
    return data


def _unpack(stream: BinaryIO, fmt: str) -> Any:
    return struct.unpack(">" + fmt, _read_exact(stream, struct.calcsize(">" + fmt)))[0]


def _string(stream: BinaryIO) -> str:
    length = _unpack(stream, "H")
    return _read_exact(stream, length).decode("utf-8")


def _payload(stream: BinaryIO, tag_type: int) -> Any:
    if tag_type == 1:
        return _unpack(stream, "b")
    if tag_type == 2:
        return _unpack(stream, "h")
    if tag_type == 3:
        return _unpack(stream, "i")
    if tag_type == 4:
        return _unpack(stream, "q")
    if tag_type == 5:
        return _unpack(stream, "f")
    if tag_type == 6:
        return _unpack(stream, "d")
    if tag_type == 7:
        length = _unpack(stream, "i")
        if length < 0:
            raise NbtError(f"Negative byte-array length {length}")
        return _read_exact(stream, length)
    if tag_type == 8:
        return _string(stream)
    if tag_type == 9:
        child_type = _unpack(stream, "B")
        length = _unpack(stream, "i")
        if length < 0:
            raise NbtError(f"Negative list length {length}")
        return [_payload(stream, child_type) for _ in range(length)]
    if tag_type == 10:
        result: dict[str, Any] = {}
        while True:
            child_type = _unpack(stream, "B")
            if child_type == 0:
                return result
            name = _string(stream)
            result[name] = _payload(stream, child_type)
    if tag_type == 11:
        length = _unpack(stream, "i")
        if length < 0:
            raise NbtError(f"Negative int-array length {length}")
        return [_unpack(stream, "i") for _ in range(length)]
    if tag_type == 12:
        length = _unpack(stream, "i")
        if length < 0:
            raise NbtError(f"Negative long-array length {length}")
        return [_unpack(stream, "q") for _ in range(length)]
    raise NbtError(f"Unsupported NBT tag type {tag_type}")


def read_nbt(path: Path) -> dict[str, Any]:
    raw = path.read_bytes()
    if raw.startswith(b"\x1f\x8b"):
        # A few upstream 1.7.10 assets have non-gzip padding after the first
        # member. Java's GZIPInputStream accepts them, while gzip.decompress()
        # attempts to parse the padding as another member and rejects it.
        raw = zlib.decompress(raw, 16 + zlib.MAX_WBITS)
    from io import BytesIO

    stream = BytesIO(raw)
    root_type = _unpack(stream, "B")
    if root_type != 10:
        raise NbtError(f"Root tag in {path} is {root_type}, expected compound (10)")
    _string(stream)  # Root name is intentionally unused.
    root = _payload(stream, root_type)
    trailing = stream.read()
    if trailing:
        raise NbtError(f"{path} has {len(trailing)} trailing bytes after its root tag")
    return root


def _item_palette(root: dict[str, Any]) -> dict[int, str]:
    result: dict[int, str] = {}
    for entry in root.get("itemPalette", []):
        if not isinstance(entry, dict):
            raise NbtError("itemPalette contains a non-compound entry")
        numeric_id = entry.get("ID")
        name = entry.get("Name")
        if not isinstance(numeric_id, int) or not isinstance(name, str):
            raise NbtError(f"Malformed itemPalette entry: {entry!r}")
        if numeric_id in result:
            raise NbtError(f"Duplicate itemPalette id {numeric_id}")
        result[numeric_id] = name
    return result


def _block_palette(root: dict[str, Any]) -> dict[int, tuple[str, int]]:
    result: dict[int, tuple[str, int]] = {}
    for index, entry in enumerate(root.get("palette", [])):
        if not isinstance(entry, dict) or not isinstance(entry.get("Name"), str):
            raise NbtError(f"Malformed palette entry at index {index}: {entry!r}")
        properties = entry.get("Properties", {})
        if not isinstance(properties, dict):
            raise NbtError(f"Malformed palette Properties at index {index}: {properties!r}")
        raw_meta = properties.get("meta", "0")
        try:
            meta = int(raw_meta)
        except (TypeError, ValueError) as error:
            raise NbtError(f"Invalid legacy metadata at palette index {index}: {raw_meta!r}") from error
        result[index] = (entry["Name"], meta)
    return result


def summarize(path: Path, root_dir: Path) -> dict[str, Any]:
    root = read_nbt(path)
    block_palette = _block_palette(root)
    item_palette = _item_palette(root)
    size = root.get("size")
    if not isinstance(size, list) or len(size) != 3 or not all(isinstance(v, int) and v > 0 for v in size):
        raise NbtError(f"Malformed structure size {size!r}")
    blocks = root.get("blocks", [])
    entities = root.get("entities", [])
    if not isinstance(entities, list):
        raise NbtError("entities is not a list")
    for index, entity in enumerate(entities):
        if not isinstance(entity, dict):
            raise NbtError(f"entities[{index}] is not a compound")
    counts: Counter[str] = Counter()
    metadata: dict[str, Counter[int]] = defaultdict(Counter)
    block_entities: Counter[str] = Counter()
    unresolved: list[dict[str, Any]] = []
    positions: dict[tuple[int, int, int], dict[str, Any]] = {}

    if not isinstance(blocks, list):
        raise NbtError("blocks is not a list")
    for index, block in enumerate(blocks):
        if not isinstance(block, dict):
            raise NbtError(f"blocks[{index}] is not a compound")
        pos_raw = block.get("pos")
        state = block.get("state")
        if not isinstance(pos_raw, list) or len(pos_raw) != 3 or not all(isinstance(v, int) for v in pos_raw):
            raise NbtError(f"blocks[{index}] has malformed pos {pos_raw!r}")
        if not isinstance(state, int):
            raise NbtError(f"blocks[{index}] has malformed state")
        pos = tuple(pos_raw)
        if any(coordinate < 0 or coordinate >= size[axis] for axis, coordinate in enumerate(pos)):
            raise NbtError(f"blocks[{index}] position {pos} is outside structure size {tuple(size)}")
        if pos in positions:
            raise NbtError(f"Duplicate block position {pos}")
        palette_entry = block_palette.get(state)
        if palette_entry is None:
            unresolved.append({"index": index, "pos": pos, "state": state})
            name, meta = f"<missing-palette:{state}>", 0
        else:
            name, meta = palette_entry
        counts[name] += 1
        metadata[name][meta] += 1
        nbt = block.get("nbt")
        if isinstance(nbt, dict):
            block_entities[name] += 1
        positions[pos] = {"name": name, "meta": meta, "nbt": nbt, "index": index}

    return {
        "file": path.relative_to(root_dir).as_posix(),
        "root_keys": sorted(root),
        "size": size,
        "blocks": len(blocks),
        "entities": len(entities),
        "block_palette": block_palette,
        "item_palette": item_palette,
        "counts": dict(sorted(counts.items())),
        "metadata": {name: dict(sorted(values.items())) for name, values in sorted(metadata.items())},
        "block_entities": dict(sorted(block_entities.items())),
        "unresolved": unresolved,
        "positions": positions,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path, help="Directory containing structure .nbt files")
    parser.add_argument("--json", type=Path, help="Write the complete audit data as JSON")
    parser.add_argument("--details", action="store_true", help="Print per-template palette and metadata summaries")
    args = parser.parse_args()

    root_dir = args.root.resolve()
    paths = sorted(root_dir.rglob("*.nbt"))
    if not paths:
        raise SystemExit(f"No .nbt files under {root_dir}")

    summaries = [summarize(path, root_dir) for path in paths]
    errors: list[str] = []
    for summary in summaries:
        for unresolved in summary["unresolved"]:
            errors.append(f"{summary['file']}: block references missing palette id {unresolved['state']} at {unresolved['pos']}")

    print(
        f"Parsed {len(summaries)} templates with {sum(s['blocks'] for s in summaries)} block entries "
        f"and {sum(s['entities'] for s in summaries)} entities"
    )
    if args.details:
        for summary in summaries:
            print(
                f"\n[{summary['file']}] size={summary['size']} blocks={summary['blocks']} "
                f"entities={summary['entities']}"
            )
            for name, metas in summary["metadata"].items():
                be_count = summary["block_entities"].get(name, 0)
                print(f"  {name}: {summary['counts'][name]} meta={metas} blockEntities={be_count}")
    if errors:
        print("\nERRORS")
        for error in errors:
            print(f"  {error}")

    if args.json:
        serializable = []
        for summary in summaries:
            copy = dict(summary)
            copy["positions"] = [
                {"pos": list(pos), **entry} for pos, entry in sorted(summary["positions"].items())
            ]
            serializable.append(copy)
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(serializable, ensure_ascii=False, indent=2), encoding="utf-8")

    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())

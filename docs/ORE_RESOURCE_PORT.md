# Ore Resource Port

This pass starts the HBM 1.12.2 ore/resource migration for Minecraft 1.21.1 NeoForge.

## Core Ores

The following legacy overworld ore ids are now core block registrations instead of generic placeholders:

- `ore_uranium`, `ore_uranium_scorched`
- `ore_thorium`, `ore_titanium`, `ore_tungsten`
- `ore_aluminium`, `ore_lead`, `ore_beryllium`
- `ore_sulfur`, `ore_niter`, `ore_fluorite`, `ore_lignite`
- `ore_asbestos`, `ore_rare`, `ore_cinnabar`, `ore_cobalt`, `ore_coltan`
- `ore_schrabidium`
- `cluster_copper`

Every core `ore_*` entry in this list has a generated `ore_deepslate_*` counterpart.
`ore_copper` is intentionally not promoted because Minecraft 1.21.1 already has vanilla copper ore and deepslate copper ore; the legacy id remains a placeholder for compatibility.

## Generated Resources

`tools/generate_ore_resources.ps1` regenerates the first ore resource batch:

- Deepslate ore textures derived from the legacy ore texture plus vanilla stone/deepslate textures.
- Blockstates, block models, and item models for normal and deepslate ores.
- Loot tables for the selected normal/deepslate ores.
- Common `c:ores/*` block/item tags and generic `c:ores` rollup tags.
- Pickaxe and tool-tier tags.
- Furnace/blast furnace recipes for smeltable ores.
- Data-driven overworld ore configured/placed features and NeoForge biome modifiers.
- Copper ore cluster worldgen, dropping `crystal_copper`.

## Current Simplifications

- Fortune and silk-touch behavior is not fully equivalent to 1.12.2 yet.
- Old metadata-backed drops such as rare-earth `chunk_ore` and aluminium cryolite are represented by single modern items for now.
- `ore_schrabidium` and scorched uranium variants are registered and textured, but they are not normal overworld worldgen entries in this pass.
- Radiation, outgassing, fallout conversion, and special structure/deposit behavior still need real 1.21.1 block logic.

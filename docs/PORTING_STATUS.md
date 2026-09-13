# Reinhardt's HBM Porting Status

This project is the Minecraft 1.21.1 NeoForge staging port for HBM 1.12.2.

## Current Scope

- NeoForge 21.1.250 Gradle workspace with Java 21.
- Main mod id: `reinhardtshbm`.
- First functional hand-written registrations:
  - 29 material/tool items.
  - 43 core ore/construction blocks.
  - 5 creative tabs, including broad legacy placeholder tabs.
- Ore/resource foundation:
  - 18 legacy overworld ore ids have graduated from placeholders to core block registrations.
  - 18 matching `ore_deepslate_*` variants were generated from the legacy ore textures.
  - `cluster_copper` is ported as the copper-specific HBM resource block; vanilla copper ore handles normal copper ore generation.
  - Basic ore tags, mining-tier tags, drops, smelting/blasting recipes, and overworld ore features are present for the first resource pass.
- Radiation foundation:
  - Living entity radiation attachment with NBT serialization and network sync.
  - Geiger counter and dosimeter right-click readout.
  - `/rhbm radiation get|set|add|clear` command.
  - Lightweight inventory radiation accumulation for uranium, plutonium, and schrabidium ingots.
- Legacy registration preservation:
  - 1843 legacy item ids from `HBM_1.12.2/src/main/java/com/hbm/items/ModItems.java`.
  - 1073 legacy block ids from `HBM_1.12.2/src/main/java/com/hbm/blocks/ModBlocks.java`.
  - Legacy ids are registered as placeholders so commands, resource ids, and creative inventory entries exist while behavior is ported.
- Asset migration:
  - 1527 legacy items have original 1.12.2 item textures copied into the new namespace.
  - 507 legacy blocks have original 1.12.2 block textures copied into the new namespace.
  - Remaining legacy entries use a visible placeholder texture until their real model/renderer is ported.
- Translations:
  - English and Simplified Chinese names were converted from legacy `.lang` files where available.

## Build Verification

`gradle build --no-daemon` completed successfully with the locally cached Gradle 8.14 installation.

A short `runClient` smoke test reached mod loading, resource reload, sound engine startup, and texture atlas creation without a mod loading crash.

Output jar:

`build/libs/reinhardtshbm-0.1.0-alpha.jar`

## Important Notes

Most legacy entries are intentionally placeholders. This is not a behavior-complete port yet. The point of this first pass is to lock down ids, resources, and a NeoForge-safe registration architecture before replacing placeholder classes with real 1.21.1 implementations.

Recommended next behavior targets:

1. Radiation damage/effects and protection rules.
2. Chunk/world radiation storage and environmental sources.
3. Accurate ore fortune/silk-touch behavior and rare/meta item replacement for old 1.12.2 drops.
4. Schrabidium/scorched uranium special generation and radiation/outgas behavior.
5. Simple machine/block entity foundation before porting RBMK and missile systems.

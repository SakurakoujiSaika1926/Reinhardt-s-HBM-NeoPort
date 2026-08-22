# Next Porting Targets

## Radiation Foundation

Port these concepts first:

- Player radiation and digamma values.
- Chunk/environment radiation values.
- Basic radiation tick handler.
- Geiger counter and dosimeter readout.
- Data attachments or saved data equivalents for 1.21.1.

Do not start by copying 1.12.2 capabilities directly. NeoForge 1.21.1 attachment APIs are the right replacement.

## Material Foundation

The first registered materials are already available:

- Uranium, U-235, U-238.
- Plutonium and Schrabidium.
- Steel, lead, boron, beryllium, graphite.
- Thorium-232, titanium, tungsten, aluminium, copper, cobalt, asbestos.
- Sulfur, niter, fluorite, lignite, cinnabar, cobalt/coltan fragments, generic ore chunk.
- Advanced alloy.

The first ore pass is also available:

- Core registrations for the main legacy overworld ores.
- Matching deepslate ore variants and generated textures.
- Copper ore uses vanilla 1.21.1 ore blocks; HBM's `cluster_copper` is the ported copper-specific resource block.
- Basic common tags, drops, smelting/blasting recipes, and overworld ore generation.

Next steps:

- Add recipes for ingots, nuggets, blocks, and dusts once those entries graduate from placeholders.
- Replace simplified rare/aluminium/coltan drops with proper modern item variants.
- Add fortune/silk-touch behavior and radiation/outgas behavior for ore blocks.

## Machine Foundation

Before porting RBMK, missiles, or factories, create a minimal block entity foundation:

- Common ticking block entity base.
- Energy/fluid interfaces.
- Menu and screen registration pattern.
- A tiny one-block test machine.

## Avoid For Now

These systems are too wide for the first phase:

- Full RBMK simulation.
- Complete missile guidance and entities.
- Old network packet set.
- Custom renderers and OBJ model loaders.
- Legacy config migration.

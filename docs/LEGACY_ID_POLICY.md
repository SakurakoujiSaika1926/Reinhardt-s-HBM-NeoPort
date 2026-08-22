# Legacy ID Policy

The 1.12.2 source uses static field initialization and old Forge lifecycle hooks. Those classes cannot be copied directly into 1.21.1 without a large rewrite, but their registry names are valuable.

This port keeps old ids by loading generated text lists from:

- `src/main/resources/legacy/reinhardtshbm/items.txt`
- `src/main/resources/legacy/reinhardtshbm/blocks.txt`

The ids are registered through `LegacyHbmContent` as placeholder items and blocks. When real behavior is ported, remove that id from the placeholder path by adding it to `CORE_ITEMS` or `CORE_BLOCKS`, then register a real class in `HbmItems` or `HbmBlocks`.

Use the same registry path unless there is a strong compatibility reason not to. For example, old `ingot_uranium` remains `reinhardtshbm:ingot_uranium`.

The namespace changes from legacy `hbm` to `reinhardtshbm` for this project. Cross-mod compatibility aliases can be added later through tags, recipes, or optional remapping logic.

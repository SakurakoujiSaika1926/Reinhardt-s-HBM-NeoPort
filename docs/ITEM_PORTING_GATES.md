# Item Porting Gates

An item is not considered ported merely because it is registered. Before a
legacy item is accepted into the 1.21.1 port, all four gates below must be
checked against the 1.7.10 source and against the current runtime.

1. **Recipe chain**
   - Verify every production recipe and every recipe that consumes the item.
   - Verify intermediate materials, tags, counts, fluids, duration, power, and
     machine tier/recipe visibility.
   - Run the JSON, semantic, runtime recipe, and item-acquisition audits.
2. **Behavior**
   - Verify the item's actual state, capabilities, values, slot acceptance,
     automation, persistence, tooltip, and client rendering.
   - Test both a newly crafted stack and any meaningful charged, damaged, full,
     empty, or configured state.
3. **Loot**
   - Search the legacy item pools, crates, structures, starter kits, and entity
     drops for the exact item and metadata value.
   - Port every applicable source, and verify that flattening did not silently
     redirect a legacy metadata stack to the base item.
4. **Flattening**
   - Decide explicitly whether the item remains a true stateful family or is
     registered as an independent item.
   - Ordinary material/component variants must use independent registry IDs;
     metadata/custom-data variants are reserved for genuinely stateful special
     families.
   - Update recipes, loot, tags, models, translations, creative tabs, JEI, and
     all Java consumers together. Do not retain a hidden compatibility variant.

Every item-specific parity task should guard these decisions where practical so
that a later refactor cannot restore a partial port.

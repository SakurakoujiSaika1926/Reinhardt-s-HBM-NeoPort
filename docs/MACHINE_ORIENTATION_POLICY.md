# Machine orientation policy

Machine orientation is part of each legacy machine's compatibility contract. It must not be inferred from another machine, even when the current numerical mapping happens to be identical.

Required rules:

- Every machine and every machine kind owns an explicit facing map verified against its exact 1.7.10 implementation.
- Do not introduce shared cardinal, yaw, rotation, pose, footprint, collision, port, or capability-orientation helpers across machines or kinds.
- Base classes must not supply a default placement-facing, `rotate`, or `mirror` implementation for concrete machines. Every concrete machine class owns those mappings explicitly, even when the bodies are currently identical; inheriting those mappings from a Minecraft framework superclass is not an exception.
- A multi-kind block or renderer must dispatch to kind-specific methods. Numerically identical kinds remain separate.
- Render bounds must apply the same kind-specific `PoseStack` used by the real render path. A separately copied transform or a guessed envelope is not acceptable.
- Structure dummies, occupied positions, collisions, interaction roots, connectors, fluid/power ports, runtime effects, and oriented item models follow the same rule.
- Continuous animation rotations may share low-level math only when they do not encode block facing or machine orientation.
- Changes require direct legacy-source or exact-model evidence. Estimation and generic fallback mappings are prohibited.
- This prohibition is permanent: code deduplication is never a valid reason to reintroduce a shared machine-orientation path.

`validateRendererCullingBounds` contains executable guards for the audited renderer contracts. Extend those guards whenever another shared orientation path is removed.

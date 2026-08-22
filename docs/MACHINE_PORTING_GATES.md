# Machine Porting Gates

These rules are mandatory for every ported machine, especially OBJ/TESR/BER machines from HBM 1.7.10.

## Build-Time Gates

- `gradlew build` runs `validateObjModels`.
- Every `LargeMachineBlock` subclass must call a superclass constructor with an explicit `RotationBasis`.
- Use `RotationBasis.MODERN_NORTH` only when the footprint was authored for the current 1.21 renderer/port logic.
- Use `RotationBasis.HBM_LEGACY_SOUTH` when the footprint is copied from 1.7.10 `BlockDummyable#getDimensions`, `getOffset`, or `fillSpace` without renderer-side compensation.
- A BER machine that renders OBJ or standalone models in Java must not also render an OBJ or normal block model from its blockstate.
- High-risk BER OBJ machines must use `reinhardtshbm:block/empty` in blockstate JSON.
- OBJ model JSONs must reference existing OBJ and MTL files.
- OBJ files with faces must have material bindings.
- MTL files must use atlas-safe texture paths, not raw `textures/...png` paths.

## Porting Evidence

Every newly ported machine must have old-version evidence before it is marked complete:

- Collision footprint: cite the 1.7.10 class and its `getDimensions`, `getOffset`, and any `fillSpace` calls.
- Rotation basis: cite whether the footprint is `MODERN_NORTH` or `HBM_LEGACY_SOUTH`, and why.
- Rotation: cite the 1.7.10 renderer or block metadata direction switch.
- Renderer: if a BER renders the world model, the blockstate must be empty or particle-only.
- Item model: use the same OBJ asset path and verify scale/translation against the old item renderer or an approved auto-fit item renderer.
- Interfaces: power, fluid, item, redstone, and tool ports must be derived from old side logic, not from model appearance.
- Recipes: use old recipe sources. Do not add guessed fallback recipes.

## No Temporary Ports

Do not mark a machine as complete if any of these are missing:

- world model
- item model
- texture bindings
- collision footprint
- rotation alignment
- interface side map
- GUI or documented no-GUI behavior
- work recipes and recursive ingredient items
- JEI category/background where applicable
- Chinese localization

If an exception is required, it must be named in `validateObjModels` with a reason. Silent exceptions are not allowed.

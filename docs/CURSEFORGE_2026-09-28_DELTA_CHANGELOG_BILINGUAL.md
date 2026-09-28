# Reinhardt's HBM NeoForgePort — 2026-09-28

## 2026 年 9 月 21 日至 9 月 28 日更新 / September 21–28, 2026 Update

Minecraft 1.21.1 · NeoForge 21.1.250+

本说明严格覆盖仓库提交 `5d2dc7e`（2026-09-21）之后至本次提交之间已经落地的源码变化，不重复此前已经发布的完整移植日志。最新版旧版行为参考为 HBM 1.7.10 提交 `e674541d`（标签 `1.0.27_X5808`）。

This document strictly covers implemented source changes after repository commit `5d2dc7e` (September 21, 2026) through this update. It does not repeat the previously published full porting history. The current legacy behavior reference is HBM 1.7.10 commit `e674541d` (tag `1.0.27_X5808`).

---

## 中文更新说明

### 同步最新版 HBM 1.7.10 内容

- 拉取并审计最新版 HBM 1.7.10 源码，以其补充缺失的机器、物品、配方、战利品、行为和权威数值；NeoForge 移植版现有的现代化能源、流体、网络、事件驱动机器和持久化框架保持不变。
- 完成最新版上游新增内容审计：本次范围内的目标均已登记、汉化、配置资源并补齐获取路径；仅明确排除依赖 OpenComputers 的 `rtty_card`，HBM 自身的 RTTY/RoR 系统仍完整保留。
- 新增并完整实现新版联盟号发射台、Lambda 发射台、研磨机、卫星地面站、超级计算机、磁带驱动器、芝加哥堆、气动仓储进出口、钢制红色电塔和货舱门。
- 芝加哥堆包含堆芯、砖体、装料器、通风口、控制器、九种独立燃料棒以及对应 PUREX 和燃料循环行为。
- 新增 11 种独立数据驱动器、Lambda 导弹、RTTY 指示器、永恒煤炭，以及间谍、扫描、雷达、采矿、精确激光、死光、共振、转发、探测和科研等完整卫星系列。
- 新增 15 种扁平化金属板建筑方块，并补齐独立注册、模型、连接外观、配方、掉落和双语文本。
- 补齐自定义机器、起重机、太空装配、卫星数据处理、轨道激光和死光炮所需的服务端逻辑、GUI、网络包、实体、音效与配方。
- 移除最新版上游已经废弃的旧联盟号“投影核心—逐块搭建—成型机器”实现，只保留可直接放置、拥有精确 2181 个占位单元和完整能力端口的新版联盟号发射台。

### 机器、物流与交互

- 完成超级计算机、磁带驱动器、卫星地面站的数据链路；超级计算机拥有 18 个工作配方，原始飞行与轨道数据可按新版流程处理。
- 完成研磨机的九个工作配方、机器 GUI、配方选择器和 JEI 分类；JEI 页面按实际槽位绘制，不再擅自添加额外机器背景。
- 恢复输送带相关设备、移动包裹实体和缺失制造配方，并补齐起重机抓取器、提取器和插入器的生存制造路径。
- 恢复铜电容及其他电容的旧版行为、充放电状态和物品表现；同一种电容不再同时显示一个无电量条方块物品和一个有电量条纯物品。
- 恢复功率计的正确模型、材质、朝向和测量功能。
- 恢复储罐普通右键交互、GUI、识别码设定、流体能力和管道连接；空储罐可以先指定流体类型，再建立流体网络。
- 修复催化裂化塔端口无法连接、客户端管道外观间歇断开，以及其他大型设备能力端口没有转发到核心的问题。
- 修复水泵生成管道无法破坏的问题，并恢复防辐射管道的创造栏/JEI 可见性、制造配方和安全壳穿墙用途。
- 修复高炉各层端口：底部只接受燃料并输出成品，上部两个端口分别服务两个冶炼原料槽。
- 大型设备远端 Dummy 现在可以稳定打开核心 GUI，不会因距离核心过远而立即关闭。
- 原版式双层门改为原子化同步更新，上下两部分不再出现肉眼可见的先后切换过程。
- 经验包恢复实际使用效果和生存获取途径；宝藏陨石恢复旧版战利品与交互用途。

### PWR、核燃料与材料处理

- 重做 PWR 的搭建、结构验证、投影指引、成型方块、控制器 GUI、启动条件、端口、燃料装填和运行状态，使整套反应堆可以按最新版旧版规则实际投入使用。
- 修复 PWR 控制器 GUI 严重错位和燃料池 JEI 页面错位；补齐大量 PWR 燃料棒的制造、热态、乏燃料、冷却及后处理配方。
- 补全 RBMK、ZIRNOX 和 PWR 的完整燃料循环，覆盖燃料制造、装填、卸出、拆解、冷却、空壳回收、燃料池、SILEX 与 PUREX 后处理。
- 修复部分 RBMK 燃料棒和结构柱体在 JEI 中无配方、实际无法制造的问题，并加入运行时燃料循环完整性检查。
- 补齐锇、镉、锝、朱砂和一瓶水银等材料的辐射属性与处理链路，恢复其冶炼、结晶、化工、SILEX、PUREX 或组合炉用途。
- 将 19 种铸造模具彻底扁平化为独立物品，移除重复的基底变体，并逐项补齐对应铸造配方和机器识别。
- 将 `scraps` 系列彻底扁平化，修复紫黑贴图，同时补齐生成、拆分、合金产出、后续加工、模型、语言和 JEI 链路。
- 将旧 `plant_flower`、`plant_tall` 变体拆分为独立作物与方块，补回大麻、烟草、毛地黄、颠茄、芥子柳等作物的种植、掉落、加工和生存获取。
- 修复 Desh 等钻头、炮铜机构、机构铸造模具、炮塔、卤素泛光灯、RBMK 柱体及多种大型设备的制造断层。
- 炮塔弹药统一增加“[自动炮塔用]”前缀，避免与 TACZ 等枪械模组的弹药命名混淆；炮塔射程芯片也已恢复。
- 补齐 HBM 与沉浸工程之间英式 `aluminium` 和美式 `aluminum` 的锭、板标签兼容，并继续通过标签互通煤焦炭等材料。

### 核爆、导弹与武器效果

- 核爆最大即时伤害提高到 5000，并使用独立真实伤害路径绕过普通护甲、护甲韧性、抗性和外部魔法减伤；HBM 自身的专用防护体系和特殊分支仍可按设计生效。
- 核爆继续使用旧版作用范围与衰减公式，同时修复近距离生物毫发无伤的问题。
- 核爆地形处理会清除爆炸范围内的流体源，避免爆炸后留下悬空水柱或岩浆柱。
- 核爆与融毁的后台工作改为按存档隔离、可取消且分批回写；创建或切换世界时会终止旧世界任务，避免新存档卡死。
- 恢复核爆的强光、放射状光束及其他旧版视觉阶段，并补齐导弹爆炸音效。
- 恢复联盟号死光炮从卫星、数据处理、发射到轨道攻击的完整玩法链，并优化发射后的服务器开销。
- 修复火焰喷射炮塔粒子数量与颜色，使其重新具有清晰可见的火焰效果。
- 修复各类哑弹方块实体模型消失，并审计同类方块实体注册，防止同一原因影响其他设备。
- 各类核弹套件的说明文本和其他遗失的中文本地化已恢复。

### 世界生成、实体与环境

- 修复 `oil_bubble` 世界生成跨越当前生成区域写入远端区块的问题，避免进入世界后地形长时间延迟和连续 `setBlock in a far chunk` 警告。
- 下界矿石改用维度类型和群系标签判定，不再写死原版下界 ID，可在采用下界模板的第三方维度正常生成。
- 异虫生成会严格检查完整碰撞体、实体占位和液体，不再容易生成进墙体后窒息；实体模型朝向也随实际运动更新。
- 塑料袋实体恢复正确材质和模型高度，不再显示紫黑纹理或半身陷入地面。
- 混凝土砖楼梯的内外 90 度拐角模型和面剔除已修复，不再出现透明缺面。
- 恢复多方块设备放置核心后的结构指引，并按各设备自身的精确结构数据生成投影。
- 为外部能力访问增加安全边界，第三方模组返回异常能力、失效对象或卸载状态时不再直接使游戏崩溃。

### Create、Sable、TACZ 与其他兼容

- 玩家搭建的大型多方块设备增加物理化复制防护，反复物理化与解除物理化不再复制核心、Dummy 或整套结构方块。
- 为避免 90 度翻转后模型、碰撞和端口失配，大型 HBM 设备现在会拒绝在 Create 列车、动力轴承和其他动态结构上组装；行为与 Create 自身不可移动结构一致。
- Sable 兼容只处理真正需要的装配状态，气体方块不再接入无意义的物理化流程，并减少不必要的区块/邻居更新。
- 武器补给箱继续保留 HBM 原有战利品；安装 TACZ 时额外生成枪械和配件，但不再强制附带子弹，避免破坏第三方复杂弹药体系。
- TACZ 枪械生成时拥有有效默认开火模式，同时不再让 TACZ 内容覆盖整个原有战利品池。
- 所有外部模组联动继续保持软兼容，未安装对应模组时不会加载其类或注册无效占位内容。

### 渲染、模型、GUI 与本地化

- 永久取消大型设备的通用旋转推导：每台机器分别维护经旧版代码和实际模型验证的专用朝向、位移、碰撞体、端口和包围盒。
- 修复大型设备距离核心数个区块后完全消失、镜头转动时闪烁、模型偏移、碰撞体旋转 90 度以及 Dummy 重复渲染等问题。
- 聚变环静态结构、输电塔端点匹配和电线几何使用缓存；只有资源重载、接线或朝向变化时才重建。
- 大型输电塔和变电站恢复正确塔体、电线端点和世界坐标，不再消失、悬空或渲染到远处。
- 修复定日镜、紧凑发射台、大型发射台、联盟号平台、储罐、辅助冷却塔及其他大型机器的裁剪、贴图闪烁、碰撞和交互问题。
- 修复雷达显示屏、IBM 300PL、CRT、定时炸弹、RTG、裁决剑等物品在 GUI、第一人称和第三人称下的专用朝向与位置。
- 修复雷达显示屏破坏粒子紫黑、QE/AE 门物品模型、微波炉对线位置、手雷及其他 OBJ 物品模型偏移。
- 修复等离子锻炉、PWR、燃料池和研磨机的 JEI/GUI 槽位错位；JEI 页面不再擅自叠加与原 GUI 冲突的背景。
- 雷达、大型雷达、指示器、PWR、核弹套件和相关设备的中文 GUI 与提示文本已补齐。
- 具有搜索框的机器界面不会再因输入 `E` 关闭 GUI；流体标识符及通用配方选择界面支持中文拼音搜索。
- 超级计算机罐体经过纹理哈希、UV、透明度、材质和 1132 个模型面的逐项核验；其深黑区域来自最新版旧版原始纹理，并非贴图丢失，因此未擅自修改原资产。

### 性能、稳定性与发布校验

- 方块实体和实体渲染器使用按真实模型、动态效果或碰撞范围验证的有限包围盒，移除无限范围、固定 256 格距离和无条件离屏渲染。
- 多方块渲染包装仅作用于明确登记的大型设备，普通方块实体不再承担额外的全局判断。
- 气体方块、输电塔、聚变环、核爆、卫星死光和大型设备渲染路径均完成针对性性能优化。
- 新增机器朝向政策和物品移植门禁：每次移植必须同时检查配方链路、行为、战利品与是否需要扁平化，禁止隐藏兼容变体和未经证据支持的通用旋转。
- 最新上游机器配方审计执行 3145 项语义断言，结果为 0 问题。
- 机器制造与工作配方审计覆盖 503 个方块、47 个机器配方族，结果为 0 问题。
- 普通模式与 528 模式均通过运行时配方装载、燃料循环和物品生存获取审计，未发现无法获取的配方原料组。
- 成品 JAR 通过注册表、语言、OBJ、伤害类型、实体/方块实体包围盒、导弹配方、热力、电力、端口与压缩包完整性检查。

### 注意事项

- 本次删除了已经被最新版 HBM 1.7.10 废弃的旧联盟号核心、投影器和旧成型发射台 ID。请勿用本版本直接加载仍依赖这些旧方块的测试存档。
- 这是 Alpha 版本。更新前仍建议备份世界，并确保服务器与所有客户端使用完全相同的 JAR。
- OpenComputers 专用 `rtty_card` 不在本次移植范围内；HBM 自身的 RTTY/RoR 功能不受影响。

---

## English Changelog

### Latest HBM 1.7.10 Upstream Sync

- Pulled and audited the latest HBM 1.7.10 source to restore missing machines, items, recipes, loot, behavior, and authoritative values. The NeoForge port keeps its modern energy, fluid, networking, event-driven machine, and persistence frameworks.
- Completed the latest-upstream content audit. Every in-scope target now has registration, assets, localization, and an acquisition path. Only the OpenComputers-only `rtty_card` is deliberately excluded; HBM's native RTTY/RoR system remains fully supported.
- Added complete implementations for the new Soyuz launchpad, Lambda launchpad, rock mill, satellite ground station, supercomputer, tape drive, Chicago pile, pneumatic storage import/export blocks, steel red pylon, and cargo door.
- The Chicago pile includes its core, bricks, loader, vent, controller, nine independent rod items, and the corresponding PUREX and fuel-cycle behavior.
- Added 11 independent data drives, the Lambda missile, RTTY designator, eternal coal, and the complete spy, scanner, radar, mining, precision-laser, death-ray, resonator, relay, detector, and science satellite families.
- Added 15 flattened plate-metal building blocks with independent registration, models, connected appearance, recipes, drops, and bilingual localization.
- Completed the server logic, GUIs, packets, entities, sounds, and recipes required by custom machines, cranes, space assembly, satellite data processing, orbital lasers, and death-ray gameplay.
- Removed the deprecated Soyuz projector/core/build-and-form implementation from the latest upstream. The port now keeps only the direct-place Soyuz launchpad with its exact 2,181 occupied cells and complete capability ports.

### Machines, Logistics & Interaction

- Completed the supercomputer, tape-drive, and satellite-ground-station data chain. The supercomputer now exposes 18 working recipes and processes raw flight and orbital data through the current upstream workflow.
- Completed the rock mill with nine working recipes, its machine screen, recipe selector, and JEI category. JEI now draws the real slot layout without an invented machine background.
- Restored missing conveyor equipment, moving-package entities, and manufacturing recipes, including survival acquisition for crane grabbers, extractors, and inserters.
- Restored legacy behavior, charging state, and item presentation for copper and other capacitors. A capacitor no longer appears twice as separate block and item entries with inconsistent power bars.
- Restored the power meter's correct model, texture, orientation, and measurement behavior.
- Restored normal tank interaction, GUI access, identifier configuration, fluid capability, and pipe connectivity. Empty tanks can be assigned a fluid type before joining a fluid network.
- Fixed catalytic-cracker ports that rejected pipes, intermittent client-side pipe disconnection visuals, and capability ports on other large machines that failed to route to their controller.
- Fixed unbreakable pump-generated pipes and restored radiation-sealed pipes to Creative search, JEI, survival crafting, and containment-wall use.
- Corrected blast-furnace automation: the bottom port only accepts fuel and outputs products, while the two upper ports feed the two smelting-input slots.
- Remote dummy blocks on large machines now open the controller GUI reliably instead of closing immediately because the controller is too far away.
- Vanilla-style two-block doors now update both halves atomically, removing the visibly staggered upper/lower transition.
- Restored actual use behavior and survival acquisition for experience bags, and restored the legacy purpose and loot behavior of treasure meteorites.

### PWR, Nuclear Fuel & Material Processing

- Reworked PWR construction, validation, build guidance, formed block, controller GUI, startup conditions, ports, fuel loading, and runtime state so the reactor can be operated according to the current legacy implementation.
- Fixed the severely misaligned PWR controller screen and fuel-pool JEI layout, and restored missing manufacturing, hot-fuel, spent-fuel, cooling, and reprocessing recipes for many PWR rods.
- Completed the RBMK, ZIRNOX, and PWR fuel cycles, including manufacturing, loading, unloading, disassembly, cooling, casing recovery, fuel-pool handling, SILEX, and PUREX processing.
- Fixed RBMK rods and structural columns that had no visible or usable recipe, and added runtime fuel-cycle completeness validation.
- Restored radiation properties and processing chains for osmium, cadmium, technetium, cinnabar, bottled mercury, and related materials across smelting, crystallization, chemistry, SILEX, PUREX, and combination-oven routes.
- Fully flattened all 19 casting molds into independent items, removed the duplicate base-variant family, and restored every corresponding casting recipe and machine lookup.
- Fully flattened the `scraps` family, fixed its missing texture, and restored production, splitting, alloy output, downstream processing, models, localization, and JEI integration.
- Split the old `plant_flower` and `plant_tall` variants into independent crops and blocks, restoring hemp, tobacco, foxglove, nightshade, mustard willow, and their planting, drops, processing, and survival acquisition.
- Restored broken manufacturing chains for Desh and other drill bits, gun mechanisms, mechanism casting molds, turrets, halogen floodlights, RBMK columns, and multiple large machines.
- Added the `[Auto Turret]` prefix to turret ammunition to avoid ambiguity with TACZ and other firearm mods, and restored turret range-upgrade chips.
- Added both British `aluminium` and American `aluminum` ingot/plate tags for HBM and Immersive Engineering while retaining tag-based coke interoperability.

### Nuclear Explosions, Missiles & Weapon Effects

- Raised maximum immediate nuclear-blast damage to 5,000 and moved it to a dedicated true-damage path that bypasses normal armor, toughness, resistance effects, and external magic mitigation. HBM's own purpose-built protection and special branches remain effective by design.
- Preserved the legacy blast radius and falloff formula while fixing nearby entities surviving without any damage.
- Nuclear terrain destruction now removes fluid source blocks inside the blast volume, preventing suspended water and lava columns after an explosion.
- Nuclear and meltdown background work is now save-scoped, cancellable, and applied in controlled batches. Creating or switching worlds cancels stale work so a new world no longer hangs behind an old explosion task.
- Restored the intense flash, radial light rays, and other legacy visual stages of nuclear explosions, and restored missing missile explosion sounds.
- Restored the complete Soyuz death-ray progression from satellites and data processing through launch and orbital attack, while reducing its post-launch server cost.
- Restored visible flame density and legacy color behavior for flamethrower turrets.
- Fixed missing block-entity models for dud bombs and audited related registrations so the same failure cannot silently affect other machines.
- Restored missing Chinese descriptions for nuclear weapon kits and related equipment.

### World Generation, Entities & Environment

- Fixed `oil_bubble` world generation writing into distant chunks outside the active generation area, eliminating prolonged terrain stalls and repeated `setBlock in a far chunk` warnings on world entry.
- Nether ore generation now uses dimension-type and biome tags instead of a hard-coded vanilla Nether ID, allowing generation in modded dimensions based on Nether templates.
- Glyphid spawning now validates full block collision, entity obstruction, and liquids, preventing frequent suffocation inside terrain. Entity models also follow actual movement orientation.
- Restored the plastic-bag entity texture and vertical placement so it no longer renders purple/black or half buried in the ground.
- Fixed inner and outer 90-degree concrete-brick stair corners and face culling so they no longer render transparent holes.
- Restored multiblock construction guidance after placing a controller, using the exact structure data owned by each machine.
- Added safety boundaries around external capability access so invalid capabilities, stale objects, or unloaded third-party state cannot directly crash the game.

### Create, Sable, TACZ & Other Compatibility

- Added duplication protection for player-built multiblocks. Repeated physicalization and disassembly can no longer duplicate controllers, dummy blocks, or entire structures.
- To prevent 90-degree transforms from separating models, collision, and ports, large HBM machines now refuse assembly on Create trains, mechanical bearings, and other moving contraptions, matching Create's own immovable-structure behavior.
- Sable compatibility now handles only meaningful assembly states. Gas blocks are excluded from pointless physicalization work, with fewer unnecessary chunk and neighbor updates.
- Weapon supply crates retain their original HBM loot. With TACZ installed they additionally roll guns and attachments, but no longer force ammunition into the crate, preserving third-party complex-ammunition systems.
- Generated TACZ guns receive a valid default fire mode without replacing the original HBM loot pool.
- All third-party integrations remain optional and avoid loading dependency classes or registering inert placeholders when the corresponding mod is absent.

### Rendering, Models, GUIs & Localization

- Permanently removed generic orientation inference for large machines. Every machine owns a dedicated, legacy-verified facing map, translation, collision shape, port layout, and render bound.
- Fixed large machines disappearing only a few chunks from their controller, flickering when the camera turns, rendering at displaced coordinates, rotating collision by 90 degrees, or drawing their controller model from every dummy.
- Static fusion-ring geometry, pylon endpoint matching, and wire meshes are cached and rebuilt only on resource reload, rewiring, or an actual orientation change.
- Restored correct tower bodies, wire endpoints, and world coordinates for large pylons and substations; they no longer disappear, float, or render far away.
- Fixed culling, texture flicker, collision, and interaction on heliostats, compact launchers, large launchers, the Soyuz platform, tanks, auxiliary cooling towers, and other large machines.
- Corrected dedicated GUI, first-person, and third-person transforms for radar displays, IBM 300PL computers, CRTs, timer bombs, RTGs, the Judgment sword, and related OBJ items.
- Fixed purple/black radar-display break particles, AE/QE door item models, microwave cable alignment, grenade offsets, and other OBJ item transforms.
- Fixed slot placement in plasma-forge, PWR, fuel-pool, and rock-mill JEI/GUI layouts. JEI pages no longer add arbitrary backgrounds that conflict with the real interface.
- Restored Chinese GUI and tooltip localization for radar, large radar, indicators, PWR, nuclear kits, and related equipment.
- Typing `E` in searchable machine screens no longer closes the GUI. Fluid identifiers and general recipe selectors now support Chinese Pinyin search.
- The supercomputer tank was verified face-by-face against the latest legacy asset: texture hashes, UVs, alpha, material binding, and all 1,132 model faces are valid. Its very dark panels are part of the original texture, so the asset was intentionally left unchanged.

### Performance, Stability & Release Validation

- Block-entity and entity renderers now use finite bounds verified from their actual model, dynamic effect, or collision extent, replacing infinite bounds, fixed 256-block ranges, and unconditional off-screen rendering.
- Multiblock render wrapping is limited to explicitly registered large machines, removing the extra global check from ordinary block entities.
- Gas blocks, pylons, fusion rings, nuclear explosions, satellite death rays, and large-machine render paths received targeted performance work.
- Added permanent machine-orientation and item-porting gates: every port must review recipes, behavior, loot, and flattening, while hidden compatibility variants and unsupported generic rotations are rejected.
- The latest-upstream machine recipe audit now executes 3,145 semantic assertions with zero reported problems.
- Manufacturing and working-recipe coverage now audits 503 blocks and 47 machine recipe families with zero reported problems.
- Both normal and 528 modes pass runtime recipe loading, fuel-cycle, and survival item-acquisition audits with no unresolved ingredient groups.
- The final JAR passes registry, localization, OBJ, damage-type, entity/block-entity render-bound, rocket/missile recipe, thermal, power, capability-port, and archive-integrity validation.

### Notes

- This update removes the old Soyuz controller, projector, and formed-launcher IDs that are deprecated by the latest HBM 1.7.10 source. Do not load a test world that still depends on those retired blocks without making a backup.
- This remains an Alpha release. Back up worlds before updating and ensure that the server and every client use the exact same JAR.
- The OpenComputers-only `rtty_card` is outside this porting scope. HBM's native RTTY/RoR functionality is unaffected.

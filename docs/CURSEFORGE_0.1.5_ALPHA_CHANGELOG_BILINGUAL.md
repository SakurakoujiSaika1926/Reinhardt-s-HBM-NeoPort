# Reinhardt's HBM NeoforgePort 0.1.5-Alpha

Minecraft 1.21.1 · NeoForge 21.1.250+

本说明仅覆盖已发布的 `0.1.3`（仓库基线 `a431ee82`）之后完成的内容，不重复此前版本的完整移植记录。

This changelog covers only work completed after the published `0.1.3` baseline (`a431ee82`). It does not repeat the full porting history from earlier releases.

---

## 中文更新说明

### 性能重点：面向大型工业基地的架构优化

性能是 Reinhardt's HBM NeoforgePort 的核心优势之一。本移植并非只把旧版内容原样搬到高版本，而是针对大型工业基地中“多数机器长期待机、少数生产线按订单运行”的真实负载重构执行路径。与仍依赖大范围逐 Tick 轮询、逐帧重复提交完整模型的直接移植方式相比，本项目把无状态变化时的工作尽可能从主线程和渲染线程移除；实际提升会随硬件、整合包和基地布局变化，但优化目标明确覆盖平均帧率、1% Low、TPS 与长时间运行稳定性。

- 已改造的生产设备采用事件/脏状态驱动：库存、流体、电力或配方状态变化时才重新判定，待机机器不再持续执行完整配方与供能扫描。
- 100 个经过逐项核验、几何不随帧变化的机器渲染器使用独立静态 VBO；多类复杂设备还缓存几何、物品和连接快照，避免每帧重复解析 OBJ、查询配方或提交全部静态顶点。
- 大型设备使用按实际模型和动画范围计算的有限包围盒，并接入原版区段可见性图进行保守遮挡剔除；只有整个模型覆盖区都不可见时才跳过绘制，不以重新引入模型消失为性能代价。
- 清理多条渲染热路径中的逐帧临时数组、矩阵、向量与排序副本，并对联盟号等重型模型拆分静态/动态图层，重点降低大型基地中的 GC 抖动和 1% Low 波动。
- 流体网络、电力连接、红石采样、辐射与火焰同步改为缓存或仅脏状态更新；核爆破坏与流体清理由受预算限制的分批任务执行，避免一次性占满服务器主线程。
- 上述策略由静态 VBO 覆盖、精确包围盒、破坏粒子、网络、机器运行、配方模式和最终 JAR 完整性等自动校验保护，防止后续优化再次换来模型消失、配方失效或服务器卡死。

### 玩法模式、蓝图与内置手册

- 重做蓝图权限：普通模式下所有普通机器配方直接开放，不再用蓝图折磨玩家；启用 528 模式后，`alt`、`discover` 和 `528` 配方池才会完整启用蓝图权限。
- 补齐 26 个有效蓝图池及其 148 项配方引用，修复蓝图册消耗后产物被空物品覆盖的问题，并清理已经失效的秘密蓝图、秘密蓝图册及相关资源。
- 完整补入 528 模式运行规则，包括 ReaSim 锅炉、下界燃烧、加压化工输入、爆炸性 AE2 交互、机器地基、波黑地雷以及 RTG 衰变/输出规则。
- 加入可独立配置、且与 528 模式互斥的 Less Bullshit Mode，覆盖简化护甲、工具、合金、化工、离心、普通合成、焊接、药物、砧等级、核废料衰变和施拉比矿转化率。
- 恢复 Cataclysm 模式：启用后，每个真实生物死亡事件都会从高空生成燃烧中的 FOEQ，并按旧版规则坠落和爆炸。
- 移植最新版旧版的 Quick Manual and Wiki 系统以及 264 个资源驱动手册页面。在物品界面将鼠标悬停到受支持物品上并按 `F1`，即可打开对应帮助页面；页面支持链接、前进和后退导航。

### 全局玩法与生存内容

- 恢复工业肥料的 3×3×3 生长效果，并补回通用手雷、普通炸药、捕鱼炸药和工业肥料的发射器行为。
- 恢复机器消音器升级；22 类循环机器音效和方块位置音效会按旧版规则衰减，而不是继续以完整音量播放。
- 恢复旧版稀有生物掉落、氰化物/红色药丸食物中毒、Bobmazon 隐藏目录、玩家声望以及售货机购买来源。
- 恢复 MKU 的随机合成、三小时感染状态、传播、伤害、呕吐、污染物品与隔离防护流程。
- 恢复 TOM 的落体、爆炸完成回调、持久化尘埃与火灾阶段、地表灼烧、生物影响以及对后续世界生成的旧版干涉规则。
- 恢复流星护符和展示台广播对流星选择、抑制与减伤的影响。
- 恢复危险掉落物行为：反物质容器、反施拉比容器、奇点、黑洞与双伽马粒子不再只是普通掉落物；对应危险效果均受独立配置控制。
- 补回伺服、强化伺服、铁包覆、电池等护甲模块行为，并修复磁铁同步、移动/攻击修正、击退抗性和额外冲刺次数。
- 恢复铋、火山岩、叶绿矿和 Mese 镐的专用采掘与战斗能力，同时移除已经随旧深层岩石系统废弃的“深层岩石专用工具”逻辑。

### 机器、物流与交互

- 完整恢复双层售货机玩法：两个外观变体、代币消费、准确奖池、旧版音效、上下层同步、变体掉落和 Bobmazon 获取途径均已实现。
- 重做工业搅拌机的双流体罐路由。两个罐体不再串液；配方的流体 1/流体 2 可以按正常顺序或交换罐位输入，机器会缓存已确认的布局并从正确罐体扣除流体。
- 修复小型雷达和大型雷达的旋转插值、360° 回绕和跳帧抽搐；两者现在使用各自旧版 OBJ 中经过核验的静态底座与旋转组件。
- 恢复雷达显示屏的数据保持与刷新逻辑，并补入陨石等雷达目标，使显示屏不再是无功能装饰。
- 修正 FEnSU 的精确 9×5×10 占用方向，模型、Dummy 与碰撞体不再相差 90°；大型机器仍坚持逐台专用朝向，不引入通用旋转。
- 加入旧版机器地基规则及可配置的重力检查，并恢复传送带拥堵上限和超时破坏行为。
- 根治普通储罐无法右键、无法连接管道或无法存储流体的问题。正常储罐可稳定打开 GUI、切换模式并通过四向专用端口输入输出；受损储罐会明确提示原因，并可用有燃料的喷灯和 6 块钢板维修。
- 修复高炉无法工作的回归，恢复旧版燃料兼容、24 份煤当量容量、空气鼓风速度曲线、烟气产量、地基判定和四个朝向各自独立的自动化端口映射。
- 修复回转炉及炮铜链路；实际放置的机器现可通过 Dummy 能力端口完成进料、蒸汽供应、处理和熔融炮铜输出，而不再只是 JEI 中存在一张无法执行的配方。
- 修复聚变反应容器和等离子锻炉无法工作的运行逻辑，并校正等离子锻炉机器 GUI 的槽位、能量栏、流体栏与文本位置；同时核验微波炉的熔炉配方入口和实际执行路径。
- 删除最新版旧版已经淘汰的单方块汽轮机，不再保留重复且无意义的机器、菜单、模型和配方。
- 删除未完成且没有继续移植价值的旧列车、矿车及专属轨道玩法；结构生成所依赖的窄轨和直轨模板仍然保留。

### ICF、聚变与星流浆

- 补齐最新版 HBM 1.7.10 的 ICF 玩法。控制器会验证激光结构、端口、燃料室、发射器、电容器与涡轮增压器，并按实际结构容量接收能量、发射激光和驱动反应核心。
- 恢复 ICF 靶丸反应、热量演化、钠冷却回路、结构端口和 GUI 数据同步，并加入 ICF 结构搭建与工具操作的 JEI 指引。
- 完整恢复星流浆链路：ICF 核心能够生成并从端口输出星流浆；流体包按 32,000 mB 打包/解包；聚变配方满速消耗 5 mB/t；六类等离子锻炉配方重新接入该流体。
- 补齐聚变、ICF、发射台、大型机器、粒子加速器、PWR、RBMK、WATZ 与精密装配升级回收等制造配方，并恢复普通与昂贵材料分支。

### 世界生成、结构与矿石体系

- 新增并接入自然生成的水泵站、小型废弃雷达天线、塔基、辐射避难所住宅、废弃北极站和军事基地；每种结构都有独立生成权重和可用的 `/locate structure` 注册。
- 大幅扩展旧结构 NBT 转换器：恢复原版旧元数据、HBM 扁平化方块、连接形状、楼梯、门、重型门、多方块核心、Dummy、方块实体数据和战利品。
- 修复结构中重复生成多个机器核心、对面留下虚空 Dummy、门体缺失、连接模型错误以及进入存档时因旧 ID/元数据转换失败而崩溃的问题。
- 向废弃矿井、地牢、沙漠神殿、丛林神庙、出生奖励箱和村庄铁匠类箱子注入旧版稀有战利品；注入不会替换原版或其他模组的战利品池。
- 石油与油砂气泡改为确定性的分区生成：完整保留最大 48 格半径，不会跨当前世界生成区域直接写入远端区块，也不会因区块生成顺序不同而被裁切或重复生成。
- 根除旧“深层岩石/深层矿石”体系：删除重复基底、旧深层建筑方块、旧深层矿簇和失效配方，统一使用原版深板岩层与明确的普通/深板岩/下界矿石注册。
- 补齐硼砂、锆、紫翠玉、下界钕、深板岩硼砂、深板岩锆及深板岩铁/钛/钨矿簇的生成、掉落、标签、模型、战利品与处理链。
- 朱砂统一为唯一规范注册，清理 `cinnebar` 拼写和重复矿石/晶体别名，并补齐采矿、粗矿/粉末、结晶、冶炼、染料与下游配方。
- 补全镉链路，包括芥子柳来源、橡胶替代路线、PVC 压力模式和冶炼路线；相关材料链均加入构建期强校验。

### 核爆、特殊灾害与环境

- 核爆会清理实际爆炸边界相连的流体源，解决爆坑中悬空水柱和岩浆柱；完整密封且未被破坏的容器内流体不会被无条件穿墙清除。
- 爆炸球体内的含水方块会连同方块本体一起删除，不再只蒸发水而留下悬空或失真的固体外壳。
- 流体清理使用分区位图、方块调色板预筛选和受预算限制的分批扫描，避免为修复悬空流体重新引入主线程长时间卡顿。
- 新增燃烧 FOEQ、下落核弹、反物质闪光和驱散罐等旧版实体及对应模型/效果，为 Cataclysm、TOM 和危险物品行为提供完整实体链路。

### 渲染、物品模型与雷达修复

- 售货机、三个烤面包机变体和灰色 RTG 改为各自基于旧版矩阵与实际 OBJ 边界的专用物品变换，不再使用通用缩放或推测偏移。
- 修复小型雷达模型扭曲、旋转动画丢失，以及大型雷达旋转跳帧；大小雷达保持独立模型与独立动画组件。
- 修复大小雷达破坏粒子紫黑。雷达现在使用只提供正确粒子贴图、不会重复绘制机器模型的专用空模型。
- 同步修复 C4、Semtex、ICF 部件、火山覆盖层、木结构、聚光灯光束等同类粒子模型缺口。
- 增加强制破坏粒子校验：递归检查所有方块状态、模型父链、贴图别名、PNG 可读性和图集收录；任何缺失或无法解析的粒子都会直接使 `build`、`check` 和部署任务失败。
- 将 100 个逐项审计且静态几何不随帧变化的机器渲染器改为独立静态 VBO 缓存；资源重载、重新烘焙和退出世界时会正确重建或释放缓存。
- 所有方块实体渲染器重新通过有限包围盒审计。大型设备继续使用各自模型、动画和结构范围推导出的专用包围盒，不采用通用旋转、固定 256 格距离、无限包围盒或估算尺寸。
- 大型设备接入原版区段可见性图：只有设备完整包围盒覆盖的所有区段均不可见时才会跳过渲染；镜头在包围盒内或快照不可用时保守渲染，避免再次出现距离核心数个区块后模型消失。
- 新增客户端选项 `enableLargeMachineOcclusionCulling`，默认启用；遇到第三方渲染兼容问题时可单独关闭墙体遮挡剔除，而不影响精确包围盒与 VBO 缓存。
- 清理每四边形数组、法线向量、模型视图矩阵、装配机械臂数组、储罐 OBJ 法线、核爆云排序副本及多类广告牌向量等帧内临时分配，降低工业基地内的 GC 抖动与低帧波动。
- 装配设备、发射台、聚变设备、玩偶、摇头娃娃、雪球和展示设备使用资源重载时生成的几何/物品快照，渲染帧内不再重复查询世界状态、配方管理器或烘焙模型。
- 修正联盟号 OBJ 三角面批处理、静态/动态图层拆分和缓存生命周期，避免贴图扭曲、顶点纠缠或缓存跨世界残留。

### 配方、稳定性与发布校验

- 所有电池类配方原料现在只校验所需电池型号并忽略当前电量；空电、部分充电和满电状态均可用于装配机、普通合成及其他制造配方，不同电池型号仍不能互相替代。
- 将旧版昂贵模式从 528 模式中彻底拆分为独立配置。普通、528、昂贵、528+昂贵四种组合分别加载自己的机器配方，装配机客户端选择与服务器执行使用同一过滤规则，不再混入互斥的普通/昂贵材料表。
- 当前配方基线包含 5,269 个文件；普通、528、昂贵、528+昂贵四种配置分别完成运行时装载审计，当前活动 HBM 配方数依次为 5,003、5,012、4,981 和 4,990，模式矩阵未发现串配方或互斥分支同时启用。
- 按最新版旧版的可见性规则集中隐藏无创造标签的内部物品、技术状态、关闭态灯具、结构占位和兼容中间件；JEI 使用独立的更窄规则，不会因物品不在创造栏就连带隐藏正常配方或合法原料。
- 优化流体网络端点发现与轮转、电池插座连接点及红石采样、放射/火焰状态脏同步和 Sable 电力空间缓存，减少无状态变化时的临时对象、查询与同步。
- 机器审计覆盖 503 个方块和 47 个工作配方族；最新版旧版机器配方导出继续通过 3,145 项语义断言。
- 新增蓝图模式、现代矿石地质、镉/朱砂处理、售货机、工业搅拌机罐位、LBSM/Cataclysm、雷达动画、旧版箱子战利品、列车移除、分区油田、破坏粒子、四模式配方矩阵、回转炉炮铜、高炉运行、ICF/星流浆与静态 VBO 的发布门禁。
- 继续保留注册表、双语语言、OBJ、伤害类型、方块实体/实体包围盒、核爆、热力、电力、端口、制造配方、工作配方和最终 JAR 完整性检查。

### 兼容性与已知边界

- 这是 Alpha 版本。更新前请备份世界，并确保服务器与所有客户端使用完全相同的 JAR。
- 本版本主动移除了旧深层岩石体系、旧单方块汽轮机和旧列车玩法的注册 ID；不保证仍引用这些废弃 ID 的开发期旧存档可直接加载。
- 按既定计划，铀矿石在破坏、邻接更新和随机刻时的旧版氡气逸散暂未移植。
- MS-ES 1.1 脚本扩展暂未同步；依赖 OpenComputers 的 `rtty_card` 仍明确排除，HBM 自身的 RTTY/RoR 功能不受影响。

---

## English Changelog

### Performance Focus: Architecture Built for Large Industrial Bases

Performance is one of the core advantages of Reinhardt's HBM NeoforgePort. This port does more than reproduce legacy content on a newer version: its execution paths are rebuilt around the real load pattern of large industrial bases, where most machines remain idle while a smaller set of production lines runs on demand. Compared with direct ports that retain broad per-tick polling and resubmit complete models every frame, this project removes as much unchanged work as possible from both the main and render threads. Exact gains still depend on hardware, modpack composition, and base layout, but the architecture explicitly targets average FPS, 1% lows, TPS, and long-session stability.

- Converted production paths use event- and dirty-state-driven reevaluation. Inventory, fluid, power, or recipe changes wake the relevant logic, while idle machines avoid repeated full recipe and supply scans.
- One hundred individually audited renderers with frame-invariant geometry now use dedicated static VBOs. Complex machines additionally cache geometry, item, and connection snapshots instead of reparsing OBJ data, querying recipe state, or resubmitting all static vertices every frame.
- Large machines use finite bounds derived from their real models and animation ranges, together with conservative vanilla visible-section occlusion. Rendering is skipped only when every section touched by the complete model is invisible, so performance is not purchased by reintroducing disappearing machinery.
- Per-frame temporary arrays, matrices, vectors, and sorting copies were removed from multiple render hot paths. Heavy models such as the Soyuz launcher separate static and dynamic layers to reduce garbage-collection stutter and improve 1% lows in dense bases.
- Fluid networks, power connections, redstone sampling, radiation, and fire synchronization now rely on caching or dirty-only updates. Nuclear destruction and fluid cleanup run as budgeted incremental tasks rather than monopolizing the server thread in one pass.
- Automated gates cover static-VBO adoption, exact render bounds, destruction particles, networks, machine runtimes, recipe modes, and final-JAR integrity, preventing later optimizations from silently trading performance for missing models, broken recipes, or server stalls.

### Gameplay Modes, Blueprints & Built-in Manual

- Reworked blueprint permissions. Normal mode now exposes all ordinary machine recipes without forcing players through blueprint gating; the complete `alt`, `discover`, and `528` permission layer activates only when 528 mode is enabled.
- Restored 26 valid blueprint pools and their 148 recipe references, fixed generated blueprints being overwritten by the consumed empty folder stack, and removed obsolete secret blueprints, secret folders, and their dead assets.
- Completed the 528 runtime rules, including ReaSim boilers, Nether burning, pressurized chemical inputs, explosive AE2 interaction, machine foundations, Bosnia mines, and RTG decay/output rules.
- Added a separately configurable Less Bullshit Mode, mutually exclusive with 528 mode, covering simplified armor, tools, alloys, chemistry, centrifuging, ordinary crafting, soldering, medicine, anvil levels, waste decay, and Schrabidium-ore conversion rates.
- Restored Cataclysm mode. When enabled, every real mob death spawns a burning FOEQ high above the death position, followed by the legacy fall and explosion behavior.
- Ported the current legacy Quick Manual and Wiki system together with 264 resource-driven manual pages. Hover a supported item in an inventory and press `F1` to open its help page; links and back/forward navigation are supported.

### Global Gameplay & Survival Content

- Restored the industrial fertilizer's 3×3×3 growth effect and dispenser behavior for universal grenades, ordinary dynamite, fishing dynamite, and industrial fertilizer.
- Restored machine muffler upgrades. Twenty-two looping machine-sound controllers and positional block sounds now apply the legacy attenuation instead of continuing at full volume.
- Restored legacy rare mob drops, cyanide/red-pill food poisoning, the hidden Bobmazon catalog, persistent player reputation, and vending-machine acquisition.
- Restored the complete MKU random-crafting and three-hour contagion flow, including transmission, damage, vomiting, contaminated items, and isolation protection.
- Restored TOM falling behavior, crater-completion callback, persistent dust/fire phases, surface scorching, entity effects, and legacy interference with later world generation.
- Restored meteor charms and pedestal broadcasts used for meteor selection, suppression, and damage reduction.
- Restored hazardous dropped-item behavior. Antimatter containers, anti-Schrabidium containers, singularities, black holes, and Digamma particles are no longer inert dropped stacks; each dangerous effect has its own configuration control.
- Restored servo, reinforced-servo, iron-cladding, battery, and related armor-module behavior, including magnet synchronization, movement/attack modifiers, knockback resistance, and additional dash charges.
- Restored the dedicated mining and combat abilities of bismuth, volcanic, chlorophyte, and Mese pickaxes while removing the obsolete deep-rock-only tool rule together with the retired deep-rock system.

### Machines, Logistics & Interaction

- Fully restored the two-block vending-machine gameplay: both visual variants, token consumption, exact reward pools, legacy sound, synchronized upper/lower halves, variant-aware drops, and Bobmazon acquisition.
- Reworked industrial-mixer fluid routing. The two input tanks no longer cross-fill; recipe fluid 1 and fluid 2 can be supplied in direct or swapped tank order, with the resolved layout cached and the correct physical tanks drained on completion.
- Fixed rotation interpolation, 360-degree wrapping, and animation stutter for both small and large radars. Each radar now uses the audited static and rotating groups from its own legacy OBJ.
- Restored radar-screen feed retention and refresh behavior and added missing detectable targets such as meteorites, so the display is no longer an inert decoration.
- Corrected the FEnSU's exact 9×5×10 occupied orientation. Its model, dummy footprint, and collision are no longer rotated 90 degrees apart; large machines continue to use per-machine transforms instead of generic rotation.
- Added legacy machine-foundation rules with configurable gravity checks and restored conveyor crowding limits and timeout destruction behavior.
- Fixed the remaining tank regression that prevented right-click interaction, pipe connections, and fluid storage. Intact tanks now open reliably, change modes, and transfer through audited four-facing ports; damaged tanks explain their state and can be repaired with a fueled blowtorch and six steel plates.
- Fixed the blast-furnace runtime regression, restoring legacy fuel compatibility, its 24-coal-equivalent capacity, air-blast speed curve, flue-gas production, foundation checks, and independent automation-port maps for all four facings.
- Fixed the rotary furnace and gunmetal chain. A placed machine now completes input, steam delivery, processing, and molten-gunmetal output through its real dummy capability ports instead of exposing a JEI-only recipe.
- Fixed nonfunctional fusion reaction vessels and plasma forges, corrected the plasma-forge machine GUI slot, power-bar, fluid-bar, and text layout, and audited the microwave's furnace-recipe lookup and execution path.
- Removed the obsolete single-block steam turbine from the current legacy upstream, including its duplicate machine, menu, model, and recipe.
- Removed the incomplete legacy train/minecart gameplay and its dedicated rail families. The narrow rails and straight templates still required by generated structures remain available.

### ICF, Fusion & Stellar Flux

- Completed the current HBM 1.7.10 ICF gameplay. Controllers now validate laser structures, ports, cells, emitters, capacitors, and turbochargers, then use the real assembled capacity to accept power, fire the laser, and drive the reaction core.
- Restored ICF pellet reactions, heat evolution, the sodium cooling loop, structure ports, and GUI synchronization, with dedicated JEI construction and tooling guidance.
- Restored the complete Stellar Flux chain. ICF cores generate and output Stellar Flux; fluid packs move 32,000 mB per pack; the fusion recipe consumes 5 mB/t at full speed; and six plasma-forge consumers use the fluid again.
- Completed manufacturing recipes for fusion, ICF, launchpads, large machines, particle accelerators, PWR, RBMK, WATZ, and precision-assembler upgrade recycling, including normal and expensive material branches.

### World Generation, Structures & Ore Geology

- Added natural generation for the water-pump station, small dead radar dish, tower base, fallout-shelter house, abandoned Arctic station, and military base. Each structure has an independent spawn weight and a valid `/locate structure` registration.
- Greatly expanded legacy NBT structure conversion, including old vanilla metadata, flattened HBM blocks, connected shapes, stairs, doors, heavy doors, multiblock controllers, dummies, block-entity data, and loot.
- Fixed structures producing duplicate machine controllers, orphaned air dummies, missing door sections, invalid connected models, or save-entry crashes caused by unresolved legacy IDs and metadata.
- Injected exact legacy rare loot into abandoned mineshafts, dungeons, desert pyramids, jungle temples, spawn bonus chests, and village smith chests without replacing vanilla or other mods' loot pools.
- Replaced oil and oil-sand bubble placement with deterministic chunk-sliced generation. The complete radius of up to 48 blocks is preserved without directly writing into distant generation chunks, and results no longer depend on chunk generation order.
- Retired the duplicate deep-rock/depth-ore system completely. Obsolete host blocks, deep building blocks, duplicate depth clusters, and stale recipes were removed in favor of vanilla deepslate and explicit stone/deepslate/Nether ore registrations.
- Completed generation, drops, tags, models, loot, and processing for borax, zirconium, alexandrite, Nether neodymium, deepslate borax, deepslate zirconium, and deepslate iron/titanium/tungsten clusters.
- Canonicalized cinnabar to one registry family, removed the misspelled `cinnebar` and duplicate ore/crystal aliases, and restored mining, raw/dust, crystallizer, smelting, dye, and downstream recipe routes.
- Completed cadmium processing through mustard willow, the rubber fallback, PVC pressure variants, and smelting. Both material chains are now protected by build-time parity checks.

### Nuclear Blasts, Special Disasters & Environment

- Nuclear blasts now remove fluid sources connected to the actually destroyed blast boundary, eliminating suspended water and lava columns while leaving fluids behind intact sealed containment untouched.
- Waterlogged blocks inside the exact blast sphere are removed as complete blocks instead of evaporating only their water and leaving invalid floating shells behind.
- Fluid cleanup uses section masks, palette pre-filtering, and budgeted incremental scans so the visual fix does not reintroduce long main-thread stalls.
- Added the legacy burning FOEQ, falling nuke, antimatter flash, and disperser-canister entities and their render/effect paths for Cataclysm, TOM, and hazardous-item gameplay.

### Rendering, Item Models & Radar Repairs

- Vending machines, all three toaster variants, and the grey RTG now use dedicated item transforms derived from their legacy matrices and actual OBJ bounds instead of shared scaling or guessed offsets.
- Fixed the small radar's distorted model and missing rotation, and the large radar's animation stutter. The two machines retain independent models and independent animated groups.
- Fixed purple-black destruction particles for both radar sizes. Each radar now uses an empty particle-only model with the correct texture and cannot redraw the machine during block breaking.
- Fixed the same particle-chain gaps for C4, Semtex, ICF components, volcano overlays, wooden structures, spotlight beams, and related models.
- Added a fail-closed destruction-particle validator covering every blockstate, recursive parent chain, texture alias, readable PNG, and block-atlas entry. Any unresolved particle now fails `build`, `check`, and deployment tasks.
- Moved 100 individually audited renderers with frame-invariant geometry to dedicated static VBO caches. Their complete model vertices are no longer resubmitted every frame, and caches are rebuilt or released after resource reloads, model baking, and logout.
- Re-audited finite bounds for every block-entity renderer. Large machines retain per-machine bounds derived from their actual models, animation ranges, and structures—never generic rotation, a fixed 256-block distance, infinite boxes, or estimated dimensions.
- Reconnected large machines to Minecraft's visible-section graph. Rendering is skipped only when every section touched by the complete bounds is invisible; cameras inside the bounds and unavailable snapshots always fall back to rendering, preventing several-chunks-from-core disappearance.
- Added the `enableLargeMachineOcclusionCulling` client option, enabled by default. Wall occlusion can be disabled independently for third-party renderer compatibility without disabling exact bounds or VBO caching.
- Removed per-frame temporary allocations for quad arrays, normals, model-view matrices, assembly arms, tank OBJ fallback normals, nuclear-cloud sorting copies, and several billboard paths, reducing garbage-collection stutter and low-frame spikes in dense bases.
- Assembly equipment, launchers, fusion machines, plushies, bobbleheads, snowglobes, and display blocks now use geometry/item snapshots built during resource reload instead of querying world state, recipe managers, or baked models during a frame.
- Corrected Soyuz OBJ triangle batching, static/dynamic layer separation, and cache lifetime to prevent distorted textures, tangled vertices, or render data leaking between worlds.

### Recipes, Stability & Release Validation

- Battery recipe inputs now match the required battery model while ignoring its current charge. Empty, partially charged, and full batteries are accepted by assembly, ordinary crafting, and other manufacturing recipes, while different battery models remain non-interchangeable.
- Fully separated legacy Expensive Mode from 528 Mode. Normal, 528, Expensive, and 528+Expensive configurations load independent machine-recipe sets, with the same filtering used by client selection and server execution so mutually exclusive normal/expensive costs cannot mix.
- The current baseline contains 5,269 recipe files. Normal, 528, Expensive, and 528+Expensive runtime audits load 5,003, 5,012, 4,981, and 4,990 active HBM recipes respectively, with no leaked recipes or simultaneously active exclusive branches.
- Applied current legacy visibility rules centrally to no-tab internal items, technical states, disabled-light variants, structure placeholders, and compatibility intermediates. JEI uses a separate narrower policy so legitimate recipes and ingredients remain discoverable.
- Reduced allocation and redundant work in fluid-network endpoint rotation, battery-socket connector/redstone sampling, dirty-only radiation/fire synchronization, and Sable power-space caches.
- Machine coverage audits include 503 blocks and 47 working-recipe families, while current legacy machine exports continue to pass 3,145 semantic assertions.
- Added release gates for blueprint modes, modern ore geology, cadmium/cinnabar processing, vending machines, mixer tank routing, LBSM/Cataclysm, both radar animations, legacy chest loot, intentional train removal, chunk-sliced oil fields, destruction particles, the four-mode recipe matrix, rotary-furnace gunmetal, blast-furnace runtime, ICF/Stellar Flux, and static VBO coverage.
- Existing registry, bilingual language, OBJ, damage-type, block-entity/entity bounds, nuclear, thermal, power, port, manufacturing recipe, working recipe, and final-JAR integrity checks remain enabled.

### Compatibility & Known Boundaries

- This is an Alpha release. Back up important worlds before updating and make sure every client and server uses the exact same JAR.
- This release intentionally removes registry IDs for the old deep-rock system, the obsolete single-block turbine, and legacy train gameplay. Development saves that still reference those retired IDs are not guaranteed to load directly.
- Legacy uranium-ore radon release on breaking, neighboring updates, and random ticks remains deliberately deferred.
- The MS-ES 1.1 scripting extension is still deferred. The OpenComputers-only `rtty_card` remains deliberately excluded; HBM's native RTTY/RoR functionality is unaffected.

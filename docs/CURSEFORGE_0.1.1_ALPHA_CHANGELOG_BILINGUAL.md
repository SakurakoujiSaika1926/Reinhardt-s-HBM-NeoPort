# Reinhardt's HBM 0.1.1-alpha

## 综合兼容、玩法、配方与渲染更新 / Combined Compatibility, Gameplay, Recipe & Rendering Update

Minecraft 1.21.1 · NeoForge 21.1.250+

本说明合并了 2026 年 9 月 14 日、9 月 19 日、9 月 21 日三次未成功发布的内容，以及截至 2026 年 9 月 24 日的后续更新。9 月 14 日批次本身还包含 8 月 23 日至 9 月 13 日累计完成的 159 个前置移植与修复提交，下文已一并展开。

This changelog combines the unpublished release batches from September 14, September 19, and September 21, 2026, together with all follow-up changes through September 24, 2026. The September 14 batch itself also contains 159 porting and repair commits completed between August 23 and September 13; those changes are detailed below as well.

---

## 中文更新说明

### 9 月 14 日前的完整移植基础（8 月 23 日至 9 月 13 日）

- 9 月 14 日的首个发布候选并非单次小更新，而是建立在此前 159 个提交之上，覆盖从初始工程迁移到完整内容登记、资源迁移、运行逻辑和旧版行为校正的连续工作。
- 建立了 Reinhardt's HBM 在 Minecraft 1.21.1 / NeoForge 上的完整移植基础，覆盖方块、物品、流体、实体、机器、方块实体、菜单、GUI、网络同步、配方、战利品、标签、语言和资源。
- 9 月 14 日基线源码已包含 4,020 个配方资源、920 个战利品表、182 个世界生成资源和 74 个结构资源；这些是实际仓库文件统计，而非功能数量估算。
- 对旧版 131 个 `machine_` ID 完成登记审计：17 个旧版已废弃项目被明确排除，其余 113 个活跃机器 ID 均已在 1.21.1 注册；具体运行行为仍由独立测试清单校验。
- 移植了装配、精密装配、压弹、锻压、焊接、粉碎、混合、固化、液化、热解、回转、连续铸造、熔炉、高炉和锯木等主要制造设备。
- 移植了木燃、柴油、蒸汽、斯特林、涡扇、燃气轮机、工业汽轮机、锅炉、冷凝器、冷却塔、地热换热和高架火炬等能源与热工设备。
- 移植了 FEnSU、储能设备、HE/RF 转换器、变频器、功率检测器、RTG、辐射裂解室和相关电网组件。
- 移植了储罐、排液管、流体管道、钻油塔、石油钻机、压裂塔、炼油、分馏、裂化、重整、加氢、焦化、内燃机和化工设备。
- 移植了采矿钻机、采矿激光、基岩矿处理、结晶、离心、气体离心、电解、氘提取、FEL、SILEX、回旋加速器、辐照舱和 PUREX 等矿物与同位素处理设备。
- 建立了 RBMK、PWR、ZIRNOX、Watz、增殖反应堆、研究反应堆、聚变设备、DFC 组件、燃料池及核废料处理等核工业基础。
- 移植了导弹装配、联盟号发射系统、火箭与导弹、炸弹与核武器、手雷、炮弹、集束子弹药、电磁脉冲及相关爆炸实体。
- 移植了输送带、组合漏斗、自动工作台、结构工具、遥控与无线电、雷达、卫星管理、传送机、力场和其他自动化或辅助设备。
- 建立了辐射、污染、核爆、核沉降、异常区域、装备防护、盖革检测和辐射抗性等环境与生存系统。
- 移植了 Glyphid、辐射生物、无人机、士兵、特殊苦力怕、飞行器、列车、船只、陨石、投射物及其他旧版实体基础。
- 移植了矿脉、矿层、石油与油砂气泡、基岩矿、钶钽铁矿床、氯气喷口、板岩层、深层钕矿、辐射热点、陨石坑、Glyphid 巢穴、古墓、地牢、遗迹散布结构和核沉降地形效果等世界生成内容。
- 恢复了盔甲、喷气背包、电池包、桶、储存鼓、战利品堆、装饰方块、音效、粒子、OBJ 模型及大量旧版物品姿态。
- 移植了进度、独立伤害类型、唱片曲目和完整创造分类等外围内容，使移植不局限于机器本体。
- 建立了大量机器菜单、同步数据、客户端界面、状态提示、配方序列化器和服务器到客户端网络包。
- 建立了广泛的 JEI 机器配方分类，并加入 Create、沉浸工程、Jade 与 Sable 的首批兼容基础。

### 9 月 14 日前完成的专项对齐与修复

- 移植了灯笼巨构、旧版烟花发射阵列和 Glyphid 巢穴基础，并补充 Glyphid 实体、生成、模型材质、朝向、高度及不会在日光下错误燃烧的行为。
- 修复流体管道加载递归、重复流体桶注册和重复客户端流体扩展注册，避免区块加载或客户端初始化异常。
- 完成创造物品栏搜索、索引和提示文本搜索修复，使大规模物品目录可以正常检索。
- 恢复大型化工厂通过冷却代理输出乏蒸汽的行为。
- 将高炉 GUI 对齐旧版布局，分离状态提示，并移除重复的退休高炉实现。
- 修复重型门方块实体注册以及 Digamma 的死亡与复活状态，避免设备或实体因生命周期映射错误而失效。
- 修复 FEnSU 在电网中的功率透传，使相邻网络能够按设计传递能量。
- 将涡扇发动机的工作循环、流体端口、Dummy 端口、识别码交互和同步行为对齐旧版，同时修复内燃机端口。
- 恢复方形与箱式流体管道的轴向、面绕序、交互范围和物品模型表现。
- 恢复无线电、接收机、广播器、Radiobox、红石无线电、Telex 的模型、材质、交互和完整制造链。
- 修复红铜线缆、箱式铜缆、可染色铜缆、经典线缆、充电器线缆、盖革计数器和组合漏斗的碰撞、UV、朝向与物品渲染。
- 系统性恢复 RBMK 面板、ZIRNOX、Watz、DFC、聚变设备、研究反应堆、燃油设备、燃气轮机、斯特林设备、FEnSU、热工设备、Foundry 和大型齿轮的旧版物品姿态。
- 恢复旧版 GUI 背景、流体储罐纹理、熔炉状态提示、喷气背包提示、动态桶、填充桶、填充桶装物与储存鼓的显示。
- 恢复武器补给箱旧版行为、输送带搬运与渲染、Foundry 物品模型、结构魔杖交互以及结构中的旧版铁轨朝向。
- 恢复耐火砖、排气管、Telex、回旋加速器水银处理、联盟号核心装配等配方，并修正精密装配机回收配方门槛与空原料显示。
- 对齐联盟号发射器方块实体变换和碰撞体积，补回火箭与导弹创造分类，并修复炸弹、核弹和其他核武器物品资源路径与模型。
- 恢复 RBMK 无线面板、显示面板、流体加热器的分类与物品方向，并修复 ICF 支架组件、组装激光器和聚变加热柱等核工业部件。
- 对齐化工排气口、燃油设备、排液管、油机和各种流体容器的端口方向、材质绑定和专用物品变换。
- 将辐射模拟限制在已加载区块，避免每次发射重建辐射快照，并改为事件驱动状态更新，同时降低抗辐射效果的逐 tick 开销。
- 增加 Jade 对大型机器 Dummy 方块的核心设备提示，使玩家查看占位结构时仍能识别实际机器。
- 增加移动结构中的 HBM 电力与流体兼容，使受支持的移动设备可以保存并恢复网络状态。

### 机器、自动化与性能

- 将装配机、弹药装配机、精密装配机、焊接台、电弧焊机、火力锻压机和大型装配厂等设备改为状态变化驱动，空闲机器不再每 tick 全量扫描配方与内容物。
- 修复漏斗、Create 漏斗和 AE2 样板供应器逐件输入时反复重置加工进度的问题；补充来电唤醒、输出解除阻塞和配方状态失效后的重新检查。
- 修复新放置且未接线的机器虚空充电、电量倒转、满电后停止接收自动化输入，以及满电空闲设备仍持续从电网取电的问题。
- 优化电网拓扑和端点更新，减少空闲网络及微小功率差异造成的无效计算。
- 重新设计火力锻压机的自动化端口：上层只输入冲压模板，中层输入原料并输出成品，下层只输入燃料；兼容原版漏斗、Create 和 AE2 自动化。
- 校正高炉端口：底部只允许输入燃料和输出成品，上部两个接口分别对应两个冶炼原料槽。
- 修复大型设备远端 Dummy 方块打开 GUI 后立刻关闭的问题，交互会稳定转发到实际核心。
- 恢复储罐普通右键 GUI、识别码设定、流体存储与管道能力；空储罐也能先设定流体类型并建立连接。
- 修复催化裂化塔和催化重整塔的端口归属、管道连接和客户端连接外观不同步问题。
- 恢复完整方块流体管道及辐射密封管道，使封闭安全壳内的反应堆仍可正常连接外部系统。
- 修复钢门红石控制，并调整玩家放置钢墙时的朝向，使墙体生成在远离玩家的一侧。
- 修复浇注盆、高速钢板批量铸造、模板保留、连续铸造机及其他铸造链路。
- 修复大型设备被 Create 物理装配、移动和解除装配时复制核心或遗留错误结构的问题。
- 优化压裂塔、辐射系统、核爆和 RBMK 融毁等高开销逻辑，减少主线程阻塞和 TPS 峰值。

### 石油、流体与能源联动

- 通过通用流体标签兼容 HBM 与 Create Diesel Generators 的原油、汽油和柴油，不再把第三方油田产物强制替换成写死的 HBM 流体。
- HBM 设备可接受带有对应通用标签的燃料和原油；HBM 汽油、柴油也可供第三方内燃机及管道系统使用。
- 钻油塔、石油钻机和压裂塔会在首次具备工作条件时识别浅层油田、深层油田与基岩油田，并缓存选择，避免每次加工重复扫描。
- 未安装 Create Diesel Generators 时直接使用 HBM 浅层模式；同时存在深浅油田时优先开采浅层，枯竭后自动切换到深层，无需拆除机器。
- 深层模式要求抽油管实际接触基岩后才能工作，保留实体钻探过程，而不是直接进行虚空抽取。
- 压裂塔按浅层、深层、基岩油田的顺序切换，并可开采以机器为中心、曼哈顿半径 3 的 25 区块菱形范围，排列为 1/3/5/7/5/3/1。
- 浅层油田首次供电时生成并持久化任务快照；机器按队列消费油岩，避免每次工作遍历区域。若目标被玩家提前挖除，本次不产出并继续处理下一任务。
- 深层油田的储量、群系和反射调用结果已缓存，在保持多机器一致性的同时显著降低重复查询成本。
- 石油探测仪可分别报告浅层油田、当前区块深层油田、压裂可开采储量和基岩油田，并提供“附近存在 / 预估储量 / 未发现”的本地化文本。
- 沉浸工程柴油发电机、HV 导线和 HV 接口获得可配置的高功率默认值；仅在对应模组存在且兼容开关启用时生效。
- Create Crafts & Additions 电动马达的内部储能下限提高到 16,384 FE，并保证与传输速率匹配，减少高负载下间歇停机。

### 核工业、辐射与爆炸

- 辐射场按旧版玩法改为二维区块列传播，不再使用 Y 轴距离；辐射数据继续通过世界 SavedData 持久化，重启游戏不会清空。
- 辐射更新改为事件驱动并限制在已加载区域，减少无效扫描；盖革计数、环境辐射、实体暴露和爆心附近辐射已统一校正。
- 核爆会正确施加即时伤害、环境辐射和生物辐射影响，不再出现爆心读数过低而生物完全不受影响的情况。
- 核爆破坏与 RBMK 融毁采用分批、异步化的世界修改流程，在提高破坏应用速度的同时控制单 tick 开销。
- RBMK 融毁重新对齐旧版行为：残骸不会作为可拾取机器掉落，残骸运动、熔融物与爆炸效果得到校正。
- RBMK 安全壳会削弱融毁破坏和辐射外泄；铅层具有更高的屏蔽能力，完整铅夹层可按高版本玩法阻断外泄辐射。
- RBMK 诊断界面的温度和耐久等浮点数据显示限制为三位小数。
- 补全 PWR、ZIRNOX 和 RBMK 燃料棒的制造、使用、冷却、拆解及后处理链路，并增加运行时燃料循环完整性检查。
- PWR 燃料支持装填、热燃料、燃料池冷却、乏燃料及 PUREX 后处理；ZIRNOX 支持卸棒、热废料、空壳回收、冷却及 PUREX 后处理。
- RBMK 各燃料棒采用独立物品和配方；使用后的燃料棒可拆出燃料芯块与空壳，各芯块状态均补齐 SILEX 后处理。
- 恢复核燃料池、结晶器、焊接台及相关核工业配方的 JEI 可见性和实际可制造性。
- 恢复水银、铅等放射性或有毒物品的携带中毒效果。
- 防毒面具在安装 Curios 时可作为饰品使用；未安装 Curios 时仍保持原有头盔槽行为。
- 车万女仆会受到携带放射性物品带来的辐射和呕吐效果，玩家去污器也可清除女仆体内的辐射。

### 配方、物品与生存制造链路

- 将旧式 material 变体中的线材、板材、铸造板、壳体及其他普通材料改为独立物品注册，并同步更新配方、标签、模型、语言、战利品和世界生成引用。
- 清理已经移除的旧 HBM 枪械系统所遗留的枪托、枪管及无效制造链路；核燃料等确有状态需求的特殊物品继续保留专用状态设计。
- 严格修复电路板、连续铸造机、焦化装置、化工厂、坩埚、装配设备和大量机器本体的生存合成断层。
- 恢复化工厂六氟化铀配方在机器配方选择器中的可见性。
- 恢复 RBMK 结构柱、空白柱体、相关反应堆部件和卤素泛光灯的制造配方。
- 将十种钻头扁平化为独立注册，并补齐钢、高速钢、Desh、碳化物合金和铀铁钻头及其钻石涂层版本的装配配方。
- 恢复粉碎机刀片维修、高速钢板铸造、一瓶水银及其后处理等缺失链路。
- 修复 `crate_` 箱体变体只能被 Create 搅拌配方识别的问题，使标准制造设备也能正常使用。
- 补全炮塔制造链路：炮铜机构、机构铸造模具、炮塔本体、哨戒炮及 528 模式蓝图均有可用的生存获取路径。
- 为 49 种炮塔专用弹药增加“[自动炮塔用]”前缀，避免与 TACZ 等枪械模组的弹药混淆；迷你核弹系列保持原名。
- 弹药装配机使用独立且合理的制造与配方体系，不再依赖 TACZ 枪械工作台。
- 修复 AE/QE 推拉门、微波炉、手雷、输电塔、储罐、联盟号舱体、化工厂、炮塔和多种核工业部件的物品模型。
- 补全物品获取路径审计，覆盖普通模式和 528 模式，降低“创造模式存在但生存无法获得”的风险。

### 跨模组兼容

- TACZ 采用软兼容：安装后，武器补给箱会从 TACZ API 提供的全部枪械与配件池中随机生成内容，并支持第三方枪包。
- 武器补给箱不再掉落 TACZ 子弹，避免与会改写弹药机制的其他模组冲突；生成的枪械会带有有效的默认开火模式，同时保留原有 HBM 战利品。
- Curios 采用可选兼容：安装后，普通物品栏旁会显示带背景的便携箱槽位和“打开”按钮；该槽位映射到隐藏的 Curios 槽，不会污染饰品界面。
- 带有 `reinhardtshbm:portable_crates` 标签的箱子均可作为便携箱；玩家背包已满时，新拾取物会自动尝试进入便携箱。
- AE2 输入/输出总线和 Create 自动化可正确与更新后的机器端口、配方缓存和渐进式进料逻辑配合。
- HBM 与沉浸工程的煤焦炭通过通用标签互通。
- 同时补充英式 `aluminium` 与美式 `aluminum` 的锭、板等标签，使 HBM 和沉浸工程材料可双向通用。
- 增加 AE2 石英材料兼容，并修复相关配方只接受单一命名空间物品的问题。
- 安装 JECharacters 时，砧、装配机、化工厂和坩埚的配方选择界面支持中文拼音搜索。
- 增加 Create Big Cannons 软兼容：其流体炮弹可装填 HBM 芥子气，并在命中后生成对应毒雾与污染效果。
- 改进 Create 结构移动兼容，大型 HBM 设备、Dummy 链和相关流体/电力状态能在受支持的移动结构中正确保存与恢复。

### 世界生成与矿产

- 下界矿石不再写死原版下界维度 ID，而是依据下界群系标签和超热维度类型判断，可在符合条件的模组下界模板维度生成。
- 移除深层岩石作为现代世界生成基底的用途，以原版深板岩体系承接高版本深层矿石，避免重复定位和高度断层。
- 补全稀土、深层稀土、铝土矿、赤铁矿、孔雀石及其资源层与矿簇生成。
- 校正普通矿石和深层矿石的生成高度、基底、掉落、时运行为及矿物标签，使其符合 1.21.1 的粗矿石玩法。
- 补齐深层矿簇及矿石变体的标签和 JEI 可见性，修复创造物品栏可见但配方检索不到的问题。
- 保留不同矿石变体的独立外观，同时根治不再需要的旧基底方块和失效配方引用。

### 渲染、模型与界面

- 取消大型设备统一通用旋转，恢复并固定每台机器专用的朝向、位移、碰撞体积和物品姿态，避免 90 度错位及碰撞箱与模型不一致。
- 为方块实体和实体渲染器建立有限且可验证的精确包围盒；清除无限包围盒、固定 256 格距离和无条件离屏渲染策略。
- 修复大型设备距离核心约数个区块后消失、镜头转动时闪烁、模型渲染到错误位置，以及 Dummy 重复绘制核心模型的问题。
- 为聚变环、输电塔电线及其他重型静态几何增加缓存；模型重载时会使缓存失效，避免继续使用旧资源。
- 大型输电塔按塔型缓存端点、排列与电线几何，仅在接线或朝向改变时重建；修复塔体消失、电线悬空、错接和远端偏移。
- 修复变电站、定日镜、辅助冷却塔、聚变设备、RBMK、Watz、发射台及其他大型设备的模型定位和裁剪范围。
- 修复紧凑发射台和大型发射台的底座贴图闪烁、缺失贴图、无法交互、GUI 错位及模型偏移；联盟号专用表现保持独立。
- 修复电力泛光灯、卤素泛光灯的发光面表现，避免已供电设备看起来仍未点亮。
- 修复 QE 两种推拉门的物品缩放、专用姿态及外观区分。
- 校正微波炉为完整方块尺寸，并使用专用世界与物品变换精确对齐电线接口。
- 修复手雷、摇头娃娃、玩偶、雪球和部分炮塔的偏移、无限包围盒与过度渲染问题。
- 修复聚变反应堆 GUI 文本重叠、JEI 位置错乱，以及多种机器 GUI 控件和槽位无法交互的问题。
- 移除大型设备无意显示的绿色结构网格和调试包围框。
- 修复开裂混凝土楼梯等方块的透明穿透、层级或渲染类型错误。

### 稳定性与发布校验

- 增加注册表、语言文件、OBJ 模型、损伤类型、资源路径和成品 JAR 完整性检查。
- 增加 JSON 配方结构、配方语义、历史配方基线、Create 压片/切割一致性以及导弹和火箭配方检查。
- 增加普通模式与 528 模式的运行时配方装载审计和物品生存获取审计。
- 增加燃料循环、炮塔制造链、扁平化钻头、热力行为、电力行为、机器端口及关键回归问题的专项校验。
- 增加方块实体和实体渲染包围盒审计，阻止无限范围、未经验证的估算范围和通用旋转重新进入发布构建。
- 所有跨模组内容均保持软兼容：只有检测到对应模组时才注册或启用相关功能。

### 注意事项

- 这是 Alpha 版本，建议更新前备份世界，并确保服务器与客户端使用同一版本。
- Curios、TACZ、Create Diesel Generators、沉浸工程、AE2、JECharacters、Create Big Cannons 和车万女仆等联动只会在对应模组已安装时启用。

---

## English Changelog

### Complete Port Foundation Before September 14 (August 23–September 13)

- The first September 14 release candidate was not a small standalone update. It was built on 159 earlier commits covering the continuous migration from the initial project to complete content registration, resource migration, runtime logic, and legacy-behavior alignment.
- Established the full Minecraft 1.21.1 / NeoForge port foundation for Reinhardt's HBM, covering blocks, items, fluids, entities, machines, block entities, menus, GUIs, networking, recipes, loot, tags, localization, and assets.
- The September 14 source baseline already contained 4,020 recipe resources, 920 loot tables, 182 world-generation resources, and 74 structure resources. These are direct repository file counts, not estimated feature counts.
- Audited all 131 legacy `machine_` IDs: 17 entries already deprecated in the legacy version were explicitly excluded, while all remaining 113 active machine IDs were registered for 1.21.1. Runtime behavior remained subject to the separate machine test checklist.
- Ported the major manufacturing machines for assembly, precision assembly, ammunition pressing, forging, welding, shredding, mixing, solidification, liquefaction, pyrolysis, rotary processing, continuous casting, furnaces, blast furnaces, and sawmilling.
- Ported power and thermal machinery including wood, diesel, steam, Stirling, turbofan, gas-turbine and industrial-turbine generation, boilers, condensers, cooling towers, geothermal exchange, and gas flares.
- Ported FEnSU, energy storage, HE/RF converters, frequency conversion, power detectors, RTGs, radiolysis, and associated grid components.
- Ported tanks, drains, fluid ducts, oil derricks, pumpjacks, fracking towers, refineries, fractionation, cracking, reforming, hydrotreating, coking, combustion engines, and chemical machinery.
- Ported excavators, mining lasers, bedrock-ore treatment, crystallization, centrifuges, gas centrifuges, electrolysis, deuterium extraction, FEL, SILEX, cyclotrons, exposure chambers, and PUREX mineral and isotope processing.
- Established the nuclear-industry foundation for RBMK, PWR, ZIRNOX, Watz, breeder and research reactors, fusion machinery, DFC components, fuel pools, and nuclear-waste processing.
- Ported missile assembly, the Soyuz launch system, rockets and missiles, bombs and nuclear weapons, grenades, artillery shells, cluster submunitions, EMP effects, and associated explosive entities.
- Ported conveyors, combination funnels, autocrafting, structure tools, remote and radio systems, radar, satellite management, teleporters, force fields, and other automation and utility equipment.
- Established the environmental and survival foundations for radiation, pollution, nuclear explosions, fallout, anomalous areas, protective equipment, Geiger detection, and radiation resistance.
- Ported the foundations for Glyphids, radioactive creatures, drones, soldiers, special creepers, aircraft, trains, boats, meteors, projectiles, and other legacy entities.
- Ported world generation for ore deposits, resource layers, oil and oil-sand bubbles, bedrock ores, coltan deposits, chlorine geysers, schist strata, deep neodymium, radiation hotspots, meteorites, Glyphid hives, ancient tombs, dungeons, scattered ruins, and nuclear-fallout terrain effects.
- Restored armor, jetpacks, battery packs, barrels, storage drums, loot piles, decorative blocks, sounds, particles, OBJ models, and a large collection of legacy item poses.
- Ported advancements, dedicated damage types, jukebox tracks, and the complete Creative-tab structure so the migration extends beyond machine blocks themselves.
- Established the machine menus, synchronized data, client screens, status displays, recipe serializers, and server-to-client packets required by the port.
- Established broad JEI machine recipe coverage together with the first compatibility foundations for Create, Immersive Engineering, Jade, and Sable.

### Dedicated Alignment and Repairs Completed Before September 14

- Ported lantern megastructures, the legacy fireworks battery, and the Glyphid-hive foundation, then restored Glyphid entities, spawning, model materials, orientation, height, and correct resistance to erroneous daylight burning.
- Fixed fluid-pipe load recursion, duplicate fluid-bucket registration, and duplicate client fluid-extension registration to prevent chunk-loading and client-initialization failures.
- Completed Creative inventory search, indexing, and tooltip-search fixes so the large item catalog can be searched normally.
- Restored spent-steam output from the large chemical factory through its coolant proxies.
- Aligned the blast-furnace GUI with the legacy layout, separated status tooltips, and removed the duplicate retired blast-furnace implementation.
- Fixed heavy-door block-entity registration and Digamma death/revival state so lifecycle mapping errors no longer disable the block or entity.
- Fixed FEnSU power passthrough so adjacent grids can transfer energy as designed.
- Aligned turbofan operation, fluid ports, Dummy ports, identifier interaction, and synchronization with the legacy version, while also fixing combustion-engine ports.
- Restored axis selection, face winding, interaction bounds, and item rendering for square and boxed fluid ducts.
- Restored the models, materials, interactions, and full crafting chain for radios, receivers, broadcasters, Radiobox, wireless redstone, and Telex equipment.
- Fixed collision, UV mapping, orientation, and item rendering for red-copper cables, boxed copper cables, paintable copper cables, classic cables, charger cables, Geiger counters, and combination funnels.
- Systematically restored legacy item poses for RBMK panels, ZIRNOX, Watz, DFC, fusion machinery, research reactors, oil machinery, gas turbines, Stirling equipment, FEnSU, thermal equipment, foundry equipment, and large gears.
- Restored legacy GUI backgrounds, fluid-tank textures, furnace status tooltips, jetpack tooltips, dynamic buckets, filled buckets, filled barrels, and storage-drum presentation.
- Restored legacy weapon-supply-crate behavior, conveyor transport and rendering, foundry item models, structure-wand interaction, and legacy rail orientation inside structures.
- Restored recipes for firebricks, exhaust ducts, Telex equipment, cyclotron mercury processing, and Soyuz core assembly, while correcting precision-assembler recycling gates and empty-ingredient display.
- Aligned Soyuz launcher block-entity transforms and collision, restored the rockets-and-missiles Creative category, and fixed resource paths and models for bombs, nuclear bombs, and other nuclear weapons.
- Restored RBMK wireless-panel and display-panel orientation, RBMK fluid-heater categorization, and nuclear components including ICF scaffold parts, the assembled laser, and the fusion-heater pillar.
- Aligned port direction, material binding, and dedicated item transforms for chemical exhausts, refueling equipment, drains, oil machinery, and multiple fluid containers.
- Limited radiation simulation to loaded chunks, stopped rebuilding radiation snapshots for every emission, moved radiation state to event-driven updates, and reduced per-tick radiation-resistance overhead.
- Added Jade controller tooltips for large-machine Dummy blocks so occupied structure blocks still identify their real machine.
- Added HBM power and fluid compatibility for moving structures so supported mobile machinery can preserve and restore network state.

### Machines, Automation & Performance

- Converted the assembler, ammunition assembler, precision assembler, welding table, arc welder, fire-powered press, large assembly factory, and related machines to state-change-driven processing so idle machines no longer perform full recipe and inventory scans every tick.
- Fixed processing progress being repeatedly reset when hoppers, Create funnels, or AE2 pattern providers insert ingredients one item at a time; added power wake-up, output-unblocked recovery, and recipe-state invalidation handling.
- Fixed newly placed and disconnected machines charging from nowhere, power values running backward, full buffers refusing further automation input, and idle full machines continuing to draw power from the grid.
- Optimized power-grid topology and endpoint updates to reduce redundant work caused by idle networks and insignificant power differences.
- Redesigned fire-powered press automation: the upper section accepts only press templates, the middle section accepts ingredients and outputs products, and the lower section accepts only fuel; vanilla hoppers, Create, and AE2 automation are supported.
- Corrected blast furnace ports: lower ports now accept fuel and output products only, while the two upper ports map to the two smelting input slots.
- Fixed GUIs opened from distant Dummy blocks on large machines closing immediately; interaction is now forwarded reliably to the real controller.
- Restored normal right-click tank GUIs, identifier configuration, fluid storage, and pipe capabilities; empty tanks can be assigned a fluid type before a connection is established.
- Fixed catalytic cracking and catalytic reforming tower port ownership, pipe connections, and intermittent client-side disconnected visuals.
- Restored the full-block fluid duct and radiation-sealed duct so reactors inside sealed containment buildings can still connect to external systems.
- Fixed steel-door redstone control and changed player-placed steel walls to occupy the side farther away from the player.
- Fixed casting basins, bulk high-speed-steel plate casting, mold retention, continuous casting machines, and other casting chains.
- Fixed core duplication and invalid leftover structures when large machines are assembled, moved, and disassembled through supported Create contraption mechanics.
- Optimized fracking towers, radiation processing, nuclear explosions, and RBMK meltdowns to reduce main-thread stalls and TPS spikes.

### Oil, Fluid & Power Integration

- Added common fluid-tag compatibility between HBM and Create Diesel Generators for crude oil, gasoline, and diesel, instead of forcibly replacing third-party oil-field output with a hard-coded HBM fluid.
- HBM machines now accept correctly tagged fuels and crude oils, while HBM gasoline and diesel can be used by compatible third-party engines and pipe systems.
- Oil derricks, pumpjacks, and fracking towers identify shallow, deep, and bedrock oil sources when they first become able to work, then cache the selection to avoid repeated scans on every operation.
- When Create Diesel Generators is absent, machines directly use HBM shallow mode. If both shallow and deep fields exist, shallow oil is consumed first and the machine automatically switches to deep oil when depleted, without being broken and replaced.
- Deep mode requires the drill pipe to physically touch bedrock before operation, preserving a real drilling process instead of allowing immediate void extraction.
- Fracking towers switch through shallow, deep, and bedrock fields in that order and can consume deep fields in a 25-chunk Manhattan-radius-3 diamond around the tower, using a 1/3/5/7/5/3/1 layout.
- Shallow deposits create and persist a task snapshot on first power-up. Machines consume queued oil-rock targets instead of traversing the region for every operation; if a player has removed a target, that operation produces nothing and advances to the next task.
- Deep-field amount, biome, and reflective API results are cached, substantially reducing repeated query cost while keeping multiple machines consistent.
- The oil detector now reports shallow fields, deep fields in the current chunk, frackable reserves, and bedrock fields independently, with localized “nearby / estimated reserve / not found” output.
- Immersive Engineering diesel generators, HV wires, and HV connectors receive configurable high-power defaults that apply only when the mod is present and the compatibility option is enabled.
- Create Crafts & Additions electric motors now have a minimum internal capacity of 16,384 FE and capacity appropriate to their transfer rate, reducing intermittent stalls under heavy load.

### Nuclear Industry, Radiation & Explosions

- Radiation fields now follow the legacy two-dimensional chunk-column model and no longer use Y-axis distance. Radiation remains persistent through world SavedData and is not cleared by restarting the game.
- Radiation updates are event-driven and limited to loaded areas. Geiger readings, environmental radiation, entity exposure, and ground-zero radiation have been brought into agreement.
- Nuclear detonations now apply immediate damage, environmental radiation, and radiation exposure to mobs correctly, preventing ground zero from showing negligible readings while nearby entities remain unaffected.
- Nuclear terrain destruction and RBMK meltdowns use batched, asynchronous world updates to apply destruction faster while controlling per-tick cost.
- RBMK meltdown behavior has been realigned with the legacy version: wreckage no longer drops as collectible machine items, and wreckage motion, corium, and explosive effects have been corrected.
- RBMK containment structures reduce meltdown damage and radiation leakage. Lead layers provide stronger shielding, and a complete lead interlayer can block escaping radiation under the modernized gameplay rules.
- RBMK diagnostic floating-point values such as temperature and durability are limited to three decimal places.
- Restored manufacturing, operation, cooling, disassembly, and reprocessing chains for PWR, ZIRNOX, and RBMK fuel rods, with runtime fuel-cycle completeness checks.
- PWR fuel supports assembly, hot fuel, fuel-pool cooling, depleted fuel, and PUREX reprocessing. ZIRNOX supports rod unloading, hot waste, empty-casing recovery, cooling, and PUREX reprocessing.
- Each RBMK fuel rod is now a dedicated item with its own recipe. Used rods can be disassembled into fuel pellets and an empty casing, and every pellet state has the required SILEX reprocessing path.
- Restored JEI visibility and actual craftability for nuclear fuel-pool, crystallizer, soldering-table, and related nuclear-industry recipes.
- Restored carried-item poisoning for mercury, lead, and other radioactive or toxic materials.
- Gas masks can be equipped as accessories when Curios is installed; without Curios, the original helmet-slot behavior remains available.
- Touhou Little Maid maids are affected by radioactive inventory contents, radiation sickness, and vomiting, and player decontaminators can remove radiation from maids.

### Recipes, Items & Survival Progression

- Replaced legacy material variants for wires, plates, cast plates, casings, and other ordinary materials with dedicated item registrations, updating recipes, tags, models, localization, loot, and world-generation references accordingly.
- Removed obsolete stocks, barrels, and invalid production paths left behind by the removed legacy HBM firearm system. Specialized items that genuinely require state, such as nuclear fuels, retain dedicated state handling.
- Strictly repaired survival-progression gaps for circuit boards, continuous casting machines, coking units, chemical plants, crucibles, assembly equipment, and many machine blocks themselves.
- Restored uranium-hexafluoride recipe visibility in the chemical plant's in-machine recipe selector.
- Restored crafting recipes for RBMK structural columns, blank columns, related reactor components, and halogen floodlights.
- Flattened ten drill bits into dedicated registrations and restored assembly recipes for steel, high-speed steel, Desh, carbide-alloy, and ferrouranium drill bits, including all diamond-coated versions.
- Restored missing chains for shredder-blade repair, high-speed-steel plate casting, bottled mercury and its downstream processing, and related content.
- Fixed `crate_` container variants being recognized only by Create mixing recipes so standard manufacturing machines can use them as intended.
- Completed the turret manufacturing chain, including gun mechanisms, the mechanism casting mold, turret blocks, the sentry gun, and usable survival paths for 528-mode blueprints.
- Added the “[Automated Turret]” prefix to 49 turret-only ammunition items to avoid confusion with ammunition from TACZ and other firearm mods; mini-nuke variants retain their existing names.
- The ammunition assembler now uses its own appropriate manufacturing and recipe path instead of depending on the TACZ gun workbench.
- Fixed item models for AE/QE sliding doors, the microwave, grenades, power pylons, tanks, the Soyuz capsule, chemical plants, turrets, and multiple nuclear-industry components.
- Added item-acquisition audits for both normal and 528 modes to reduce cases where content exists in Creative mode but cannot be obtained in Survival.

### Mod Compatibility

- TACZ support is optional. When installed, weapon supply crates draw from all guns and attachments exposed by the TACZ API, including compatible third-party gun packs.
- Weapon supply crates no longer drop TACZ ammunition, avoiding conflicts with mods that replace or extend ammunition mechanics. Generated guns receive a valid default fire mode, and original HBM loot remains in the pool.
- Curios support is optional. When installed, the normal inventory screen shows a portable-crate slot with a background panel and an “Open” button; it mirrors a hidden Curios slot and does not clutter the accessories screen.
- Any container tagged `reinhardtshbm:portable_crates` can serve as a portable crate. When the player's main inventory is full, newly collected items automatically attempt to enter the equipped crate.
- AE2 import/export buses and Create automation now work correctly with the updated machine ports, recipe caches, and incremental ingredient insertion logic.
- HBM and Immersive Engineering coal coke interoperate through common tags.
- Both British `aluminium` and American `aluminum` tags are provided for ingots, plates, and related forms so HBM and Immersive Engineering materials work in both directions.
- Added AE2 quartz compatibility and fixed recipes that previously accepted material from only one namespace.
- With JECharacters installed, recipe selectors for the anvil, assembler, chemical plant, and crucible support Chinese pinyin search.
- Added optional Create Big Cannons support: its fluid shells can carry HBM mustard gas and create the matching toxic mist and pollution effects on impact.
- Improved Create structure-movement compatibility so large HBM machines, Dummy chains, and related fluid and power state can be preserved and restored by supported moving structures.

### World Generation & Ores

- Nether ores no longer depend on the vanilla Nether dimension ID. Nether biome tags and ultra-warm dimension types are used instead, allowing generation in qualifying modded Nether-template dimensions.
- Removed deep stone from modern world-generation duties and moved deep ore generation onto the vanilla deepslate system, avoiding duplicated roles and vertical generation gaps.
- Completed rare-earth, deepslate rare-earth, bauxite, hematite, malachite, resource-layer, and ore-cluster generation.
- Corrected generation height, host block, drops, Fortune behavior, and ore tags for regular and deep ores to match the Minecraft 1.21.1 raw-ore progression.
- Completed tags and JEI visibility for deep clusters and ore variants, fixing cases where entries were visible in Creative mode but absent from recipe searches.
- Preserved the distinct appearance of ore variants while removing obsolete host blocks and invalid recipe references that were no longer needed.

### Rendering, Models & UI

- Removed generic rotation from large machines and restored dedicated orientation, translation, collision geometry, and item poses for each machine, preventing 90-degree errors and model/collision mismatches.
- Added finite, verifiable, geometry-derived bounds for block-entity and entity renderers, removing infinite bounds, fixed 256-block ranges, and blanket off-screen rendering.
- Fixed large machines disappearing only a few chunks away from their controller, flickering as the camera moves, rendering at incorrect positions, and being redrawn once for every Dummy block.
- Added caches for the fusion ring, pylon wires, and other heavy static geometry. Cached data is invalidated when models reload so stale resources are not reused.
- Large power pylons now cache endpoints, permutations, and wire geometry by tower type, rebuilding only when wiring or orientation changes; tower disappearance, floating wires, incorrect matching, and distant offsets have been fixed.
- Fixed model placement and culling ranges for substations, heliostats, auxiliary cooling towers, fusion machinery, RBMK, Watz, launchpads, and other large structures.
- Fixed compact and large launchpad base Z-fighting, missing textures, broken interaction, GUI misalignment, and model offsets while keeping Soyuz-specific presentation separate.
- Fixed the emissive appearance of electric and halogen floodlights so powered lights no longer appear unlit.
- Fixed dedicated item scaling, poses, and visual distinction for both QE sliding-door variants.
- Corrected the microwave to full-block scale and added precise dedicated world and item transforms aligned with its cable connector.
- Fixed offsets, infinite bounds, and excessive rendering for grenades, bobbleheads, plushies, snow globes, and selected turrets.
- Fixed fusion-reactor GUI text overlap, JEI layout displacement, and non-interactive or misaligned controls and slots in several machine screens.
- Removed unintended green structure grids and debug bounds from large machines.
- Fixed transparency bleed, layer selection, and render-type issues on blocks such as cracked concrete stairs.

### Stability & Release Validation

- Added validation for registries, language files, OBJ models, damage types, resource paths, and final JAR integrity.
- Added checks for recipe JSON structure, recipe semantics, historical recipe baselines, Create pressing/cutting parity, and missile and rocket recipes.
- Added runtime recipe-loading audits and Survival item-acquisition audits for both normal and 528 modes.
- Added targeted checks for fuel cycles, turret production, flattened drill bits, thermal behavior, power behavior, machine ports, and critical regression fixes.
- Added block-entity and entity render-bound audits that prevent infinite ranges, unverified estimated bounds, and generic rotations from returning to release builds.
- All cross-mod features remain soft integrations and register or activate only when the corresponding mod is detected.

### Notes

- This is an Alpha release. Back up important worlds before updating and ensure that clients and servers use the same version.
- Curios, TACZ, Create Diesel Generators, Immersive Engineering, AE2, JECharacters, Create Big Cannons, Touhou Little Maid, and other integrations activate only when their corresponding mods are installed.

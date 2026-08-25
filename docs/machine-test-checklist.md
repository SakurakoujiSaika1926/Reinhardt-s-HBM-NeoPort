# machine_ 测试清单

当前需要手动测试的新版独立 `machine_` 设备：**111 个**。

每一项至少检查：创造栏/命令获取、物品模型、放置模型、四向朝向、碰撞体积、交互与对应的物品/流体/能量接口。带 GUI 的设备再检查中文、布局、JEI、自动化、配方和工作循环。

旧版审计的 `machine_boiler` 与 `machine_boiler_off` 已合并为同一设备，因此只列一次。电炉与砖砌炉的 `_on` 方块属于工作状态，不单列，工作时一并检查。

## 制造与加工

- [ ] 压弹机 - `machine_ammo_press`
- [ ] 焚毁炉 - `machine_annihilator`
- [ ] 大型电弧炉 - `machine_arc_furnace`
- [ ] 电弧焊机 - `machine_arc_welder`
- [ ] 装甲改装台 - `machine_armor_table`
- [ ] 储灰槽 - `machine_ashpit`
- [ ] 大型装配厂 - `machine_assembly_factory`
- [ ] 装配机 - `machine_assembly_machine`
- [ ] 自动工作台 - `machine_autocrafter`
- [ ] 自动嗡嗡锯 - `machine_autosaw`
- [ ] 压缩机 - `machine_compressor`
- [ ] 紧凑型压缩机 - `machine_compressor_compact`
- [ ] 自动脱粒机 - `machine_thresher`
- [ ] 高炉 - `machine_blast_furnace`
- [ ] 坩埚 - `machine_crucible`
- [ ] 火力锻压机 - `machine_press`
- [ ] 电动锻压机 - `machine_epress`
- [ ] 粉碎机 - `machine_shredder`
- [ ] 工业搅拌机 - `machine_mixer`
- [ ] 工业固化机 - `machine_solidifier`
- [ ] 工业液化机 - `machine_liquefactor`
- [ ] 连续铸造机 - `machine_strand_caster`
- [ ] 热解炉 - `machine_pyrooven`
- [ ] 回转炉 - `machine_rotary_furnace`
- [ ] 焊接台 - `machine_soldering_station`
- [ ] 砖砌炉（含工作状态） - `machine_furnace_brick_off`
- [ ] 电炉（含工作状态） - `machine_electric_furnace_off`
- [ ] 微波炉 - `machine_microwave`
- [ ] 斯特林锯木机 - `machine_sawmill`

## 能源与热工

- [ ] 火力发电机 - `machine_wood_burner`
- [ ] 柴油发电机 - `machine_diesel`
- [ ] 蒸汽机 - `machine_steam_engine`
- [ ] 斯特林发电机 - `machine_stirling`
- [ ] 重型斯特林发电机 - `machine_stirling_steel`
- [ ] 创造斯特林发动机 - `machine_stirling_creative`
- [ ] 涡扇发动机 - `machine_turbofan`
- [ ] 联合循环燃气轮机 - `machine_turbine_gas`
- [ ] 汽轮机 - `machine_turbine`
- [ ] 工业汽轮机 - `machine_industrial_turbine`
- [ ] 利维坦巨型汽轮机 - `machine_chungus`
- [ ] 太阳能锅炉 - `machine_solar_boiler`
- [ ] 锅炉 - `machine_boiler_off`
- [ ] 小型电锅炉 - `machine_boiler_electric_off`
- [ ] 工业锅炉 - `machine_industrial_boiler`
- [ ] 蒸汽冷凝器 - `machine_condenser`
- [ ] 大功率蒸汽冷凝器 - `machine_condenser_powered`
- [ ] 辅助冷却塔 - `machine_tower_small`
- [ ] 冷却塔 - `machine_tower_large`
- [ ] 地热换热器 - `machine_hephaestus`
- [ ] 空气压缩机（当前显示名：进气口） - `machine_intake`
- [ ] 高架火炬 - `machine_flare`

## 电网与储能

- [ ] FEnSU - `machine_battery_redd`
- [ ] 电池座 - `machine_battery_socket`
- [ ] 重型磁约束储罐 - `machine_orbus`（已移植，待游戏内验证）
- [ ] HE->RF 转换器 - `machine_converter_he_rf`
- [ ] RF->HE 转换器 - `machine_converter_rf_he`
- [ ] 功率检测器 - `machine_detector`
- [ ] 10k-20Hz 变频器 - `machine_transformer`
- [ ] 放射性同位素发电机 - `machine_rtg_grey`
- [ ] 放射性同位素热电机和辐射裂解室 - `machine_radiolysis`
- [ ] 辐射能量发电机 - `machine_radgen`

## 流体与石油

- [ ] 储罐 - `machine_fluidtank`
- [ ] 六氟化铀储罐 - `machine_uf6_tank`
- [ ] 六氟化钚储罐 - `machine_puf6_tank`
- [ ] 巨尻-9000 储罐 - `machine_bat9000`
- [ ] 排液管 - `machine_drain`
- [ ] 钻油塔 - `machine_well`
- [ ] 石油钻机 - `machine_pumpjack`
- [ ] 水力压裂塔 - `machine_fracking_tower`
- [ ] 炼油厂 - `machine_refinery`
- [ ] 真空炼油厂 - `machine_vacuum_distill`
- [ ] 分馏塔 - `machine_fraction_tower`
- [ ] 催化裂化塔 - `machine_catalytic_cracker`
- [ ] 催化重整器 - `machine_catalytic_reformer`
- [ ] 加氢装置 - `machine_hydrotreater`
- [ ] 焦化装置 - `machine_coker`
- [ ] 工业内燃机 - `machine_combustion_engine`
- [ ] 化工厂 - `machine_chemical_plant`
- [ ] 大型化工厂 - `machine_chemical_factory`

## 矿物与分离

- [ ] 大型采矿钻机 - `machine_excavator`
- [ ] 采矿激光 - `machine_mining_laser`
- [ ] 基岩矿石处理机 - `machine_ore_slopper`
- [ ] 矿物酸化器 - `machine_crystallizer`
- [ ] 离心机 - `machine_centrifuge`
- [ ] 气体离心机 - `machine_gascent`
- [ ] 电解机 - `machine_electrolyser`
- [ ] 氘提取器 - `machine_deuterium_extractor`
- [ ] 氘萃取塔 - `machine_deuterium_tower`
- [ ] FEL 自由电子激光器 - `machine_fel`
- [ ] SILEX 激光同位素分离室 - `machine_silex`
- [ ] 回旋加速器 - `machine_cyclotron`
- [ ] 辐照舱 - `machine_exposure_chamber`
- [ ] ICF 燃料靶丸制造器 - `machine_icf_press`
- [ ] 钚铀还原提取设备（PUREX） - `machine_purex`

## 核工业

- [ ] 增殖反应堆 - `machine_reactor_breeding`
- [ ] 反应堆遥控模块 - `machine_controller`
- [ ] 核废料处理桶 - `machine_storage_drum`
- [ ] 乏燃料池 - `machine_waste_drum`

## 自动化与工具

- [ ] 输送带锻压机 - `machine_conveyor_press`
- [x] 火力锻压机 - `machine_press`
- [ ] 组合漏斗 - `machine_funnel`
- [x] 精密装配机 - `machine_precass`
- [ ] 导弹装配台 - `machine_missile_assembly`
- [ ] 锁匠桌 - `machine_keyforge`
- [ ] 传送机 - `machine_teleporter`
- [ ] 卫星 ID 管理器![alt text](7541080d744802150185b20a4e8cef5c.jpeg) - `machine_satlinker`
- [ ] 力场发生器 - `machine_forcefield`

## 监测与其他

- [ ] 雷达 - `machine_radar`
- [ ] 大型雷达 - `machine_radar_large`
- [ ] 警报器 - `machine_siren`
- [ ] LPW-2 空间站推进器 - `machine_lpw2`
- [ ] 武器改装台 - `machine_weapon_table`

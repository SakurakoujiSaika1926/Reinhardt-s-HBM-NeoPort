package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.TeslaArmorModItemRenderer;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Exact 1.7.10 ItemModTesla target selection and damage calculation. */
public final class LegacyTeslaArmorModItem extends ArmorModItem {
    private static final double RANGE = 5.0D;

    public LegacyTeslaArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final TeslaArmorModItemRenderer renderer = new TeslaArmorModItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.level().isClientSide || !(armor.getItem() instanceof PoweredArmorFSBItem powered)
                || !powered.isArmorEnabled(armor) || !ArmorFSBItem.hasFSBArmor(player)) {
            return;
        }

        zap(player, armor);
    }

    private static void zap(Player source, ItemStack armor) {
        Level level = source.level();
        Vec3 origin = new Vec3(source.getX(), source.getY() + 1.25D, source.getZ());
        // The old implementation divides by every queried entity, including
        // entities subsequently rejected by range or line-of-sight checks.
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(origin, origin).inflate(RANGE)
        );
        if (candidates.isEmpty()) {
            return;
        }

        boolean hit = false;
        for (LivingEntity target : candidates) {
            if (target == source || target instanceof Cat || !target.isAlive()) {
                continue;
            }

            Vec3 targetPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            if (targetPoint.distanceTo(origin) > RANGE || isObstructed(level, origin, targetPoint, source)) {
                continue;
            }

            float damage = (float) Math.clamp(target.getMaxHealth() * 0.5D, 3.0D, 20.0D) / candidates.size();
            if (target.hurt(level.damageSources().source(HbmDamageTypes.ELECTRICITY), damage)) {
                level.playSound(null, target.blockPosition(), HbmSoundEvents.WEAPON_TESLA.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                hit = true;
            }
        }

        if (hit && source.getRandom().nextInt(5) == 0) {
            armor.hurtAndBreak(1, source, source.getEquipmentSlotForItem(armor));
        }
    }

    private static boolean isObstructed(Level level, Vec3 origin, Vec3 target, Player source) {
        HitResult result = level.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return result.getType() != HitResult.Type.MISS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.reinhardtshbm.back_tesla")
                .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

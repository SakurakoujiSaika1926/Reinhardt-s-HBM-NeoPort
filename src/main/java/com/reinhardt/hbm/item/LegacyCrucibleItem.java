package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.render.CrucibleItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.function.Consumer;

/** The charge-gated three-hit 1.7.10 Crucible sword. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class LegacyCrucibleItem extends Item {
    private static final int MAX_CHARGE = 3;
    private static final ItemAttributeModifiers ACTIVE_ATTRIBUTES = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5_000.0D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(ReinhardtsHBM.id("crucible_movement"), 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
            .build();

    public LegacyCrucibleItem(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_CHARGE).component(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ACTIVE_ATTRIBUTES));
    }

    public static boolean isCharged(ItemStack stack) {
        return !stack.isEmpty() && stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final CrucibleItemRenderer renderer = new CrucibleItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!isCharged(stack)) {
            return;
        }

        Level level = attacker.level();
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.75F + target.getRandom().nextFloat() * 0.2F);
        if (!level.isClientSide && !target.isAlive() && level instanceof ServerLevel serverLevel) {
            int count = Math.min((int) Math.ceil(target.getMaxHealth() / 3.0D), 250) * 4;
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), count,
                    0.0D, 0.0D, 0.0D, 0.1D);
        }
        stack.hurtAndBreak(1, attacker, attacker.getEquipmentSlotForItem(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        StringBuilder charge = new StringBuilder("Charge [");
        for (int i = MAX_CHARGE - 1; i >= 0; i--) {
            charge.append(stack.getDamageValue() <= i ? "||||||" : "   ");
        }
        tooltip.add(Component.literal(charge.append(']').toString()).withStyle(ChatFormatting.RED));
    }

    @SubscribeEvent
    public static void cancelEmptyAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof LegacyCrucibleItem) || isCharged(held)) {
            return;
        }
        event.setCanceled(true);
        if (attacker instanceof Player player && !player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.crucible.no_energy").withStyle(ChatFormatting.RED), true);
        }
    }
}

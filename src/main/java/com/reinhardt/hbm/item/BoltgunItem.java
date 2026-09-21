package com.reinhardt.hbm.item;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Construction boltgun retained because legacy multiblock assembly requires it. */
public final class BoltgunItem extends Item {
    private static final ResourceLocation BOLT_SPIKE = ResourceLocation.fromNamespaceAndPath("reinhardtshbm", "bolt_spike");

    public BoltgunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!ToolConversion.isConvertible(context.getLevel(), context.getClickedPos(), ToolConversion.Tool.BOLT)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return ToolConversion.convert(context.getLevel(), context.getClickedPos(), context.getPlayer(), ToolConversion.Tool.BOLT)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !target.isAlive()) {
            return false;
        }
        if (target.level().isClientSide) {
            return hasBolt(player);
        }
        if (!consumeBolt(player)) {
            return false;
        }

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                HbmSoundEvents.BOLT_GUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        int oldInvulnerability = target.invulnerableTime;
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(player.damageSources().playerAttack(player), 10.0F);
        if (!hurt) {
            target.invulnerableTime = oldInvulnerability;
        }
        if (!target.isAlive() && target instanceof Player victim) {
            HbmAdvancements.award(victim, "go_fish");
        }
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    private static boolean hasBolt(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isBolt(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeBolt(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isBolt(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean isBolt(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BOLT_SPIKE.equals(id)
                || pathEquals(id, "bolt_steel")
                || stack.is(HbmItems.BOLT_TUNGSTEN.get())
                || stack.is(HbmItems.BOLT_DURA_STEEL.get())) {
            return true;
        }
        return false;
    }

    private static boolean pathEquals(ResourceLocation id, String path) {
        return id != null && id.getNamespace().equals("reinhardtshbm") && id.getPath().equals(path);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}

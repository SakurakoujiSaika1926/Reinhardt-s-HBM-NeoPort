package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.GlyphidEntity;
import com.reinhardt.hbm.entity.LegacyGlyphidVariantEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A fixed-variant Glyphid spawn egg.  The old 1.7.10 mob mappings exposed
 * each concrete Glyphid class as a separate spawn-egg entry; the modern
 * entity stores that class identity in GlyphidEntity.Variant instead.
 */
public final class GlyphidSpawnEggItem extends Item {
    private final GlyphidEntity.Variant variant;
    private final int backgroundColor;
    private final int highlightColor;

    public GlyphidSpawnEggItem(Properties properties, GlyphidEntity.Variant variant,
                               int backgroundColor, int highlightColor) {
        super(properties.stacksTo(64));
        this.variant = variant;
        this.backgroundColor = backgroundColor;
        this.highlightColor = highlightColor;
    }

    public GlyphidEntity.Variant variant() {
        return variant;
    }

    public int color(int tintIndex) {
        return tintIndex == 0 ? backgroundColor : highlightColor;
    }

    public static int tint(ItemStack stack, int tintIndex) {
        return stack.getItem() instanceof GlyphidSpawnEggItem egg
                ? 0xFF000000 | egg.color(tintIndex)
                : 0xFFFFFFFF;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.mayUseItemAt(
                context.getClickedPos(), context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos spawnPos = clicked.relative(context.getClickedFace());
        double verticalOffset = context.getClickedFace().getAxis().isVertical()
                && context.getClickedFace().getStepY() > 0
                && context.getLevel().getBlockState(clicked).getBlock() instanceof SnowLayerBlock ? 0.5D : 0.0D;
        return spawn(context.getLevel(), player, context.getItemInHand(), spawnPos, verticalOffset);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof LiquidBlock)
                || !level.mayInteract(player, blockHit.getBlockPos())
                || !player.mayUseItemAt(blockHit.getBlockPos(), blockHit.getDirection(), stack)) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = spawn(level, player, stack, blockHit.getBlockPos(), 0.0D);
        return new InteractionResultHolder<>(result, stack);
    }

    private InteractionResult spawn(Level level, Player player, ItemStack stack, BlockPos pos, double verticalOffset) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        GlyphidEntity glyphid = switch (variant) {
            case BRAWLER -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_BRAWLER.get());
            case BEHEMOTH -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_BEHEMOTH.get());
            case BRENDA -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_BRENDA.get());
            case BOMBARDIER -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_BOMBARDIER.get());
            case BLASTER -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_BLASTER.get());
            case SCOUT -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_SCOUT.get());
            case NUCLEAR -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_NUCLEAR.get());
            case DIGGER -> fixedVariant(serverLevel, HbmEntityTypes.GLYPHID_DIGGER.get());
            default -> new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), serverLevel);
        };
        glyphid.setVariant(variant);
        glyphid.moveTo(pos.getX() + 0.5D, pos.getY() + verticalOffset, pos.getZ() + 0.5D,
                Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
        glyphid.setYHeadRot(glyphid.getYRot());
        glyphid.setYBodyRot(glyphid.getYRot());
        EventHooks.finalizeMobSpawn(glyphid, serverLevel,
                level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            glyphid.setCustomName(stack.getHoverName());
        }
        level.addFreshEntity(glyphid);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private LegacyGlyphidVariantEntity fixedVariant(ServerLevel level,
                                                     net.minecraft.world.entity.EntityType<LegacyGlyphidVariantEntity> type) {
        return new LegacyGlyphidVariantEntity(type, level, variant);
    }
}

package com.reinhardt.hbm.item;

import com.reinhardt.hbm.integration.createdieselgenerators.HbmCreateDieselGeneratorsOilCompat;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class OilDetectorItem extends Item {
    private static final int DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS = 1;
    private static final int FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS = 3;
    private static final int FRACKING_DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS = 4;
    private static final int SHALLOW_OIL_SCAN_NODE_LIMIT = 1_024;
    private static final long SHALLOW_OIL_ESTIMATED_MB_PER_DEPOSIT = 10_000L;
    private static final int[][] NEARBY_SAMPLE_OFFSETS = {
            {5, 0},
            {-5, 0},
            {0, 5},
            {0, -5},
            {10, 0},
            {-10, 0},
            {0, 10},
            {0, -10},
            {5, 5},
            {-5, 5},
            {5, -5},
            {-5, -5}
    };

    public OilDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            for (Component message : scanMessages(level, player.blockPosition())) {
                player.displayClientMessage(message, false);
            }
        }

        player.swing(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.reinhardtshbm.oil_detector.desc1"));
        tooltipComponents.add(Component.translatable("item.reinhardtshbm.oil_detector.desc2"));
        tooltipComponents.add(Component.translatable("item.reinhardtshbm.oil_detector.desc3"));
    }

    private static List<Component> scanMessages(Level level, BlockPos base) {
        ShallowOilScan shallow = scanShallowOil(level, base);
        CreateDieselGeneratorsOilScan deep = scanCreateDieselGeneratorsOil(level, base);
        FrackingDeepOilScan frackingDeep = scanFrackingDeepOil(level, base);
        BedrockOilScan bedrock = scanBedrockOil(level, base);

        List<Component> messages = new ArrayList<>();
        messages.add(Component.empty());
        messages.add(Component.translatable("item.reinhardtshbm.oil_detector.report.shallow", shallowStatus(shallow)));
        if (HbmCreateDieselGeneratorsOilCompat.isLoaded()) {
            messages.add(Component.translatable("item.reinhardtshbm.oil_detector.report.deep", deepStatus(deep)));
            messages.add(Component.translatable("item.reinhardtshbm.oil_detector.report.fracking", frackingDeepStatus(frackingDeep)));
        }
        messages.add(Component.translatable("item.reinhardtshbm.oil_detector.report.bedrock", bedrockStatus(bedrock)));
        messages.add(Component.empty());
        return messages;
    }

    private static ShallowOilScan scanShallowOil(Level level, BlockPos base) {
        BlockPos direct = findColumn(level, base, 0, 0, OilDetectorItem::isShallowOilDeposit);
        if (direct != null) {
            ShallowOilEstimate estimate = estimateShallowOil(level, direct);
            return new ShallowOilScan(true, false, estimate.estimatedAmount(), estimate.limited());
        }

        for (int[] offset : NEARBY_SAMPLE_OFFSETS) {
            if (findColumn(level, base, offset[0], offset[1], OilDetectorItem::isShallowOilDeposit) != null) {
                return new ShallowOilScan(false, true, 0L, false);
            }
        }
        return ShallowOilScan.NONE;
    }

    private static CreateDieselGeneratorsOilScan scanCreateDieselGeneratorsOil(Level level, BlockPos base) {
        if (!HbmCreateDieselGeneratorsOilCompat.isLoaded() || !(level instanceof ServerLevel serverLevel)) {
            return CreateDieselGeneratorsOilScan.NONE;
        }

        ChunkPos center = new ChunkPos(base);
        int directAmount = HbmCreateDieselGeneratorsOilCompat.getChunkOilAmount(serverLevel, center);
        if (directAmount > 0) {
            return new CreateDieselGeneratorsOilScan(true, false, directAmount);
        }

        for (int chunkX = -DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkX <= DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkZ <= DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkZ++) {
                if (chunkX == 0 && chunkZ == 0) {
                    continue;
                }
                ChunkPos sample = new ChunkPos(center.x + chunkX, center.z + chunkZ);
                int amount = HbmCreateDieselGeneratorsOilCompat.getChunkOilAmount(serverLevel, sample);
                if (amount > 0) {
                    return new CreateDieselGeneratorsOilScan(false, true, amount);
                }
            }
        }
        return CreateDieselGeneratorsOilScan.NONE;
    }

    private static FrackingDeepOilScan scanFrackingDeepOil(Level level, BlockPos base) {
        if (!HbmCreateDieselGeneratorsOilCompat.isLoaded() || !(level instanceof ServerLevel serverLevel)) {
            return FrackingDeepOilScan.NONE;
        }

        ChunkPos center = new ChunkPos(base);
        long totalAmount = 0L;
        for (int chunkX = -FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS; chunkX <= FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS; chunkZ <= FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS; chunkZ++) {
                if (Math.abs(chunkX) + Math.abs(chunkZ) > FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS) {
                    continue;
                }
                ChunkPos sample = new ChunkPos(center.x + chunkX, center.z + chunkZ);
                int amount = HbmCreateDieselGeneratorsOilCompat.getChunkOilAmount(serverLevel, sample);
                if (amount == Integer.MAX_VALUE) {
                    return new FrackingDeepOilScan(true, false, Long.MAX_VALUE);
                }
                if (amount > 0) {
                    totalAmount += amount;
                }
            }
        }
        if (totalAmount > 0L) {
            return new FrackingDeepOilScan(true, false, totalAmount);
        }

        for (int chunkX = -FRACKING_DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkX <= FRACKING_DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -FRACKING_DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkZ <= FRACKING_DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS; chunkZ++) {
                int distance = Math.abs(chunkX) + Math.abs(chunkZ);
                if (distance <= FRACKING_DEEP_RESERVOIR_CHUNK_RADIUS
                        || distance > FRACKING_DEEP_RESERVOIR_NEARBY_CHUNK_RADIUS) {
                    continue;
                }
                ChunkPos sample = new ChunkPos(center.x + chunkX, center.z + chunkZ);
                if (HbmCreateDieselGeneratorsOilCompat.getChunkOilAmount(serverLevel, sample) > 0) {
                    return new FrackingDeepOilScan(false, true, 0L);
                }
            }
        }
        return FrackingDeepOilScan.NONE;
    }

    private static BedrockOilScan scanBedrockOil(Level level, BlockPos base) {
        if (findColumn(level, base, 0, 0, OilDetectorItem::isBedrockOilDeposit) != null) {
            return new BedrockOilScan(true, false);
        }
        for (int[] offset : NEARBY_SAMPLE_OFFSETS) {
            if (findColumn(level, base, offset[0], offset[1], OilDetectorItem::isBedrockOilDeposit) != null) {
                return new BedrockOilScan(false, true);
            }
        }
        return BedrockOilScan.NONE;
    }

    private static Component shallowStatus(ShallowOilScan scan) {
        if (scan.direct()) {
            Component amount = formatOilAmount(scan.estimatedAmount());
            return Component.translatable(
                            scan.limited()
                                    ? "item.reinhardtshbm.oil_detector.status.estimated_at_least"
                                    : "item.reinhardtshbm.oil_detector.status.estimated",
                            amount
                    )
                    .withStyle(ChatFormatting.DARK_GREEN);
        }
        if (scan.nearby()) {
            return Component.translatable("item.reinhardtshbm.oil_detector.status.nearby")
                    .withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("item.reinhardtshbm.oil_detector.status.not_found")
                .withStyle(ChatFormatting.RED);
    }

    private static Component deepStatus(CreateDieselGeneratorsOilScan scan) {
        if (scan.direct()) {
            return Component.translatable(
                            "item.reinhardtshbm.oil_detector.status.estimated",
                            formatOilAmount(scan.amount())
                    )
                    .withStyle(ChatFormatting.AQUA);
        }
        if (scan.nearby()) {
            return Component.translatable("item.reinhardtshbm.oil_detector.status.nearby")
                    .withStyle(ChatFormatting.BLUE);
        }
        return Component.translatable("item.reinhardtshbm.oil_detector.status.not_found")
                .withStyle(ChatFormatting.RED);
    }

    private static Component frackingDeepStatus(FrackingDeepOilScan scan) {
        if (scan.direct()) {
            return Component.translatable(
                            "item.reinhardtshbm.oil_detector.status.estimated",
                            formatOilAmount(scan.amount())
                    )
                    .withStyle(ChatFormatting.AQUA);
        }
        if (scan.nearby()) {
            return Component.translatable("item.reinhardtshbm.oil_detector.status.nearby")
                    .withStyle(ChatFormatting.BLUE);
        }
        return Component.translatable("item.reinhardtshbm.oil_detector.status.not_found")
                .withStyle(ChatFormatting.RED);
    }


    private static Component bedrockStatus(BedrockOilScan scan) {
        if (scan.direct()) {
            return Component.translatable("item.reinhardtshbm.oil_detector.status.exists")
                    .withStyle(ChatFormatting.DARK_GREEN);
        }
        if (scan.nearby()) {
            return Component.translatable("item.reinhardtshbm.oil_detector.status.nearby")
                    .withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("item.reinhardtshbm.oil_detector.status.not_found")
                .withStyle(ChatFormatting.RED);
    }

    private static Component formatOilAmount(long amount) {
        if (amount == Integer.MAX_VALUE || amount == Long.MAX_VALUE) {
            return Component.literal("∞");
        }
        return Component.literal(String.format(Locale.ROOT, "%,d", amount));
    }

    private static BlockPos findColumn(Level level, BlockPos base, int offsetX, int offsetZ, Predicate<BlockState> predicate) {
        int x = base.getX() + offsetX;
        int z = base.getZ() + offsetZ;
        BlockPos chunkProbe = new BlockPos(x, base.getY(), z);
        if (!level.isLoaded(chunkProbe)) {
            return null;
        }

        int topY = Math.min(level.getMaxBuildHeight() - 1, base.getY() + 15);
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = topY; y >= minY; y--) {
            cursor.set(x, y, z);
            if (predicate.test(level.getBlockState(cursor))) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static ShallowOilEstimate estimateShallowOil(Level level, BlockPos start) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.offer(start.immutable());
        visited.add(start.immutable());

        long oilBlocks = 0L;
        boolean limited = false;
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current)) {
                continue;
            }

            BlockState state = level.getBlockState(current);
            if (!isShallowSearchBlock(state)) {
                continue;
            }
            if (isShallowOilDeposit(state)) {
                oilBlocks++;
            }

            if (visited.size() >= SHALLOW_OIL_SCAN_NODE_LIMIT) {
                limited = true;
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (neighbor.getY() < level.getMinBuildHeight()
                        || neighbor.getY() >= level.getMaxBuildHeight()
                        || visited.contains(neighbor)
                        || !level.isLoaded(neighbor)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighbor);
                if (isShallowSearchBlock(neighborState)) {
                    BlockPos immutableNeighbor = neighbor.immutable();
                    visited.add(immutableNeighbor);
                    queue.offer(immutableNeighbor);
                }
            }
        }
        return new ShallowOilEstimate(oilBlocks * SHALLOW_OIL_ESTIMATED_MB_PER_DEPOSIT, limited);
    }

    private static boolean isShallowOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_OIL.get()) || state.is(HbmBlocks.ORE_DEEPSLATE_OIL.get());
    }

    private static boolean isShallowSearchBlock(BlockState state) {
        return isShallowOilDeposit(state)
                || state.is(HbmBlocks.ORE_OIL_EMPTY.get())
                || state.is(HbmBlocks.ORE_DEEPSLATE_OIL_EMPTY.get());
    }

    private static boolean isBedrockOilDeposit(BlockState state) {
        return state.is(HbmBlocks.ORE_BEDROCK_OIL.get());
    }

    private record ShallowOilScan(boolean direct, boolean nearby, long estimatedAmount, boolean limited) {
        private static final ShallowOilScan NONE = new ShallowOilScan(false, false, 0L, false);
    }

    private record ShallowOilEstimate(long estimatedAmount, boolean limited) {
    }

    private record CreateDieselGeneratorsOilScan(boolean direct, boolean nearby, long amount) {
        private static final CreateDieselGeneratorsOilScan NONE =
                new CreateDieselGeneratorsOilScan(false, false, 0L);
    }

    private record FrackingDeepOilScan(boolean direct, boolean nearby, long amount) {
        private static final FrackingDeepOilScan NONE =
                new FrackingDeepOilScan(false, false, 0L);
    }

    private record BedrockOilScan(boolean direct, boolean nearby) {
        private static final BedrockOilScan NONE = new BedrockOilScan(false, false);
    }
}

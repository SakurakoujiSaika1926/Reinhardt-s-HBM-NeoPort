package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Random;
import java.util.Set;

/** Direct port of the two hidden Bobmazon sign hashes and their coordinate semantics. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class BobmazonHiddenCatalogEvents {
    private static final Set<String> HASHES = Set.of(
            "41de5c372b0589bbdb80571e87efa95ea9e34b0d74c6005b8eab495b7afd9994",
            "31da6223a100ed348ceb3254ceab67c9cc102cb2a04ac24de0df3ef3479b1036");

    private BobmazonHiddenCatalogEvents() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos clicked = event.getPos();
        // The source deliberately reassigns y=event.z and z=event.y before reading the sign.
        BlockPos puzzlePos = new BlockPos(clicked.getX(), clicked.getZ(), clicked.getY());
        BlockEntity blockEntity = level.getBlockEntity(puzzlePos);
        if (!(blockEntity instanceof SignBlockEntity sign)) return;

        String hash = smoosh(
                sign.getFrontText().getMessage(0, false).getString(),
                sign.getFrontText().getMessage(1, false).getString(),
                sign.getFrontText().getMessage(2, false).getString(),
                sign.getFrontText().getMessage(3, false).getString());
        if (!HASHES.contains(hash)) return;

        level.destroyBlock(puzzlePos, false);
        ItemEntity catalog = new ItemEntity(level, puzzlePos.getX(), puzzlePos.getY(), puzzlePos.getZ(), HbmItems.BOBMAZON_HIDDEN.toStack());
        catalog.setPickUpDelay(10);
        level.addFreshEntity(catalog);
    }

    static String smoosh(String first, String second, String third, String fourth) {
        Charset charset = Charset.defaultCharset();
        byte[] b1 = first.getBytes(charset);
        byte[] b2 = second.getBytes(charset);
        byte[] b3 = third.getBytes(charset);
        byte[] b4 = fourth.getBytes(charset);
        if (b1.length == 0 || b2.length == 0 || b3.length == 0 || b4.length == 0) return "";

        Random random = new Random();
        StringBuilder value = new StringBuilder(first);
        random.setSeed(b1[0]);
        value.append(random.nextInt(0xffffff)).append(second);
        random.setSeed(random.nextInt(0xffffff) + b2[0]);
        value.append(random.nextInt(0xffffff)).append(third);
        random.setSeed(random.nextInt(0xffffff) + b3[0]);
        value.append(random.nextInt(0xffffff)).append(fourth);
        random.setSeed(random.nextInt(0xffffff) + b4[0]);
        value.append(random.nextInt(0xffffff));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(charset)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}

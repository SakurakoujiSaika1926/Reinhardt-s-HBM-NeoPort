package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

public class BedrockOreBaseItem extends Item {
    private static final String PREFIX = "ore_";

    public BedrockOreBaseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            double amount = getOreAmount(stack, type);
            tooltip.add(Component.translatable(
                    "tooltip.reinhardtshbm.bedrock_ore_base.amount",
                    Component.translatable("item.reinhardtshbm.bedrock_ore_new.type." + type.id() + ".name"),
                    formatAmount(amount),
                    Component.translatable(densityKey(amount)).withStyle(densityColor(amount))
            ));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        ItemStack stack = new ItemStack(this);
        setOreAmount(stack, 0, 0, 1.0D);
        output.accept(stack);
    }

    public static ItemStack stackFor(Supplier<? extends Item> item, int x, int z, double multiplier) {
        ItemStack stack = new ItemStack(item.get());
        setOreAmount(stack, x, z, multiplier);
        return stack;
    }

    public static void setOreAmount(ItemStack stack, int x, int z, double multiplier) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            tag.putDouble(key(type), getOreLevel(x, z, type) * multiplier);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static double getOreAmount(ItemStack stack, BedrockOreItem.Type type) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getDouble(key(type));
    }

    public static double getAverageOreLevel(int x, int z) {
        double total = 0.0D;
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            total += getOreLevel(x, z, type);
        }
        return total / BedrockOreItem.Type.values().length;
    }

    public static double getOreLevel(int x, int z, BedrockOreItem.Type type) {
        double scale = 0.01D;
        double common = CommonNoiseHolder.LEVEL.noise(x * scale, z * scale);
        double ore = CommonNoiseHolder.ORE[type.ordinal()].noise(x * scale, z * scale);
        return Mth.clamp(Math.abs(common * ore) * 0.05D, 0.0D, 2.0D);
    }

    private static String key(BedrockOreItem.Type type) {
        return PREFIX + type.id();
    }

    private static String formatAmount(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private static String densityKey(double amount) {
        if (amount > 1.5D) {
            return "tooltip.reinhardtshbm.bedrock_ore_base.density.very_high";
        }
        if (amount > 1.0D) {
            return "tooltip.reinhardtshbm.bedrock_ore_base.density.high";
        }
        if (amount > 0.75D) {
            return "tooltip.reinhardtshbm.bedrock_ore_base.density.medium";
        }
        if (amount > 0.25D) {
            return "tooltip.reinhardtshbm.bedrock_ore_base.density.low";
        }
        return "tooltip.reinhardtshbm.bedrock_ore_base.density.trace";
    }

    private static ChatFormatting densityColor(double amount) {
        if (amount > 1.5D) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        if (amount > 1.0D) {
            return ChatFormatting.GREEN;
        }
        if (amount > 0.75D) {
            return ChatFormatting.YELLOW;
        }
        if (amount > 0.25D) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.DARK_GRAY;
    }

    private static final class CommonNoiseHolder {
        private static final LegacyPerlin LEVEL = new LegacyPerlin(new Random(2114043L), 4);
        private static final LegacyPerlin[] ORE = oreNoise();

        private static LegacyPerlin[] oreNoise() {
            LegacyPerlin[] noise = new LegacyPerlin[BedrockOreItem.Type.values().length];
            for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
                noise[type.ordinal()] = new LegacyPerlin(new Random(2082127L + type.ordinal()), 4);
            }
            return noise;
        }
    }

    /**
     * Equivalent of Minecraft 1.7.10 NoiseGeneratorPerlin#func_151601_a.
     * HBM 1.7.10 uses this exact two-dimensional Perlin field for bedrock ore density.
     */
    private static final class LegacyPerlin {
        private final LegacyImprovedNoise[] generators;
        private final int octaves;

        private LegacyPerlin(Random random, int octaves) {
            this.octaves = octaves;
            this.generators = new LegacyImprovedNoise[octaves];
            for (int i = 0; i < octaves; i++) {
                this.generators[i] = new LegacyImprovedNoise(random);
            }
        }

        private double noise(double x, double z) {
            double result = 0.0D;
            double divisor = 1.0D;
            for (int i = 0; i < this.octaves; i++) {
                result += this.generators[i].noise(x * divisor, z * divisor) / divisor;
                divisor /= 2.0D;
            }
            return result;
        }
    }

    /**
     * Equivalent of Minecraft 1.7.10 NoiseGeneratorImproved 2D sampling.
     */
    private static final class LegacyImprovedNoise {
        private final int[] permutations = new int[512];
        private final double xCoord;
        private final double yCoord;
        private final double zCoord;

        private LegacyImprovedNoise(Random random) {
            this.xCoord = random.nextDouble() * 256.0D;
            this.yCoord = random.nextDouble() * 256.0D;
            this.zCoord = random.nextDouble() * 256.0D;
            for (int i = 0; i < 256; this.permutations[i] = i++) {
            }
            for (int i = 0; i < 256; i++) {
                int j = random.nextInt(256 - i) + i;
                int value = this.permutations[i];
                this.permutations[i] = this.permutations[j];
                this.permutations[j] = value;
                this.permutations[i + 256] = this.permutations[i];
            }
        }

        private double noise(double x, double z) {
            double sampleX = x + this.xCoord;
            double sampleY = this.yCoord;
            double sampleZ = z + this.zCoord;
            int floorX = Mth.floor(sampleX);
            int floorY = Mth.floor(sampleY);
            int floorZ = Mth.floor(sampleZ);
            sampleX -= floorX;
            sampleY -= floorY;
            sampleZ -= floorZ;
            floorX &= 255;
            floorY &= 255;
            floorZ &= 255;
            double fadeX = fade(sampleX);
            double fadeY = fade(sampleY);
            double fadeZ = fade(sampleZ);
            int a = this.permutations[floorX] + floorY;
            int aa = this.permutations[a] + floorZ;
            int ab = this.permutations[a + 1] + floorZ;
            int b = this.permutations[floorX + 1] + floorY;
            int ba = this.permutations[b] + floorZ;
            int bb = this.permutations[b + 1] + floorZ;
            return lerp(fadeZ,
                    lerp(fadeY,
                            lerp(fadeX, grad(this.permutations[aa], sampleX, sampleY, sampleZ),
                                    grad(this.permutations[ba], sampleX - 1.0D, sampleY, sampleZ)),
                            lerp(fadeX, grad(this.permutations[ab], sampleX, sampleY - 1.0D, sampleZ),
                                    grad(this.permutations[bb], sampleX - 1.0D, sampleY - 1.0D, sampleZ))),
                    lerp(fadeY,
                            lerp(fadeX, grad(this.permutations[aa + 1], sampleX, sampleY, sampleZ - 1.0D),
                                    grad(this.permutations[ba + 1], sampleX - 1.0D, sampleY, sampleZ - 1.0D)),
                            lerp(fadeX, grad(this.permutations[ab + 1], sampleX, sampleY - 1.0D, sampleZ - 1.0D),
                                    grad(this.permutations[bb + 1], sampleX - 1.0D, sampleY - 1.0D, sampleZ - 1.0D))));
        }

        private static double fade(double value) {
            return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
        }

        private static double lerp(double delta, double start, double end) {
            return start + delta * (end - start);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : h != 12 && h != 14 ? z : x;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}

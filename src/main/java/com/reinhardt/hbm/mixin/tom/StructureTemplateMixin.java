package com.reinhardt.hbm.mixin.tom;

import com.reinhardt.hbm.world.TomImpactWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ListIterator;

/** Rewrites only processed blocks belonging to a post-impact village. */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @Inject(
            method = "processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;",
            at = @At("RETURN")
    )
    private static void reinhardtshbm$applyTomVillagePalette(
            ServerLevelAccessor level,
            BlockPos offset,
            BlockPos pos,
            StructurePlaceSettings settings,
            List<StructureTemplate.StructureBlockInfo> original,
            @Nullable StructureTemplate template,
            CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> callback
    ) {
        ListIterator<StructureTemplate.StructureBlockInfo> iterator = callback.getReturnValue().listIterator();
        while (iterator.hasNext()) {
            StructureTemplate.StructureBlockInfo current = iterator.next();
            StructureTemplate.StructureBlockInfo transformed = TomImpactWorldgen.transformVillageBlock(current);
            if (transformed != current) {
                iterator.set(transformed);
            }
        }
    }
}

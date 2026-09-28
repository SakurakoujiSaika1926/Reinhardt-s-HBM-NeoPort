package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.RbmkDebrisEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.EnumMap;
import java.util.Map;

public class RbmkDebrisEntityRenderer extends EntityRenderer<RbmkDebrisEntity> {
    // OpenGL's legacy glRotatef(angle, 1, 1, 1) normalizes the axis internally.
    // JOML's AxisAngle4f keeps the supplied axis, so use the exact normalized
    // diagonal axis to preserve the old rotation without introducing scale.
    private static final float LEGACY_DIAGONAL_AXIS = 0.5773502691896258F;

    private static final ModelResourceLocation BLANK = MachineModelRenderer.standalone("entity/rbmk_debris_blank");
    private static final ModelResourceLocation ELEMENT = MachineModelRenderer.standalone("entity/rbmk_debris_element");
    private static final ModelResourceLocation FUEL = MachineModelRenderer.standalone("entity/rbmk_debris_fuel");
    private static final ModelResourceLocation GRAPHITE = MachineModelRenderer.standalone("entity/rbmk_debris_graphite");
    private static final ModelResourceLocation LID = MachineModelRenderer.standalone("entity/rbmk_debris_lid");
    private static final ModelResourceLocation ROD = MachineModelRenderer.standalone("entity/rbmk_debris_rod");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final Map<RbmkDebrisEntity.DebrisType, LegacyEntityObjMesh> BOUNDS_MODELS =
            boundsModels();
    private static final Map<RbmkDebrisEntity.DebrisType, Double> VISUAL_RADII = visualRadii();

    public RbmkDebrisEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BLANK);
        event.register(ELEMENT);
        event.register(FUEL);
        event.register(GRAPHITE);
        event.register(LID);
        event.register(ROD);
    }

    @Override
    public void render(RbmkDebrisEntity debris, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D, 0.0D);
        poseStack.mulPose(yaw(debris.getId() % 360));
        float rot = debris.lastRot + (debris.rot - debris.lastRot) * partialTick;
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(rot),
                LEGACY_DIAGONAL_AXIS,
                LEGACY_DIAGONAL_AXIS,
                LEGACY_DIAGONAL_AXIS)));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model(debris.debrisType())),
                poseStack,
                bufferSource,
                RENDER_STATE,
                packedLight,
                0
        );
        poseStack.popPose();
        super.render(debris, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(RbmkDebrisEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = VISUAL_RADII.get(entity.debrisType());
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(RbmkDebrisEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static ModelResourceLocation model(RbmkDebrisEntity.DebrisType type) {
        return switch (type) {
            case BLANK -> BLANK;
            case ELEMENT -> ELEMENT;
            case FUEL -> FUEL;
            case GRAPHITE -> GRAPHITE;
            case LID -> LID;
            case ROD -> ROD;
        };
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static Map<RbmkDebrisEntity.DebrisType, LegacyEntityObjMesh> boundsModels() {
        Map<RbmkDebrisEntity.DebrisType, LegacyEntityObjMesh> models =
                new EnumMap<>(RbmkDebrisEntity.DebrisType.class);
        models.put(RbmkDebrisEntity.DebrisType.BLANK, mesh("deb_blank"));
        models.put(RbmkDebrisEntity.DebrisType.ELEMENT, mesh("deb_element"));
        models.put(RbmkDebrisEntity.DebrisType.FUEL, mesh("deb_fuel"));
        models.put(RbmkDebrisEntity.DebrisType.GRAPHITE, mesh("deb_graphite"));
        models.put(RbmkDebrisEntity.DebrisType.LID, mesh("deb_lid"));
        models.put(RbmkDebrisEntity.DebrisType.ROD, mesh("deb_rod"));
        return Map.copyOf(models);
    }

    private static LegacyEntityObjMesh mesh(String name) {
        return LegacyEntityObjMesh.load(com.reinhardt.hbm.ReinhardtsHBM.id(
                "models/obj/projectiles/" + name + ".obj"));
    }

    private static Map<RbmkDebrisEntity.DebrisType, Double> visualRadii() {
        Map<RbmkDebrisEntity.DebrisType, Double> radii =
                new EnumMap<>(RbmkDebrisEntity.DebrisType.class);
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        BOUNDS_MODELS.forEach((type, mesh) -> {
            // Both authored rotations are unit quaternions. Only the fixed
            // +0.125 Y translation can increase distance from entity origin.
            double radius = mesh.maximumFaceRadius(0.0D, 0.0D, 0.0D) + 0.125D;
            radii.put(type, Math.nextUp(radius * (1.0D + gamma64)));
        });
        return Map.copyOf(radii);
    }
}

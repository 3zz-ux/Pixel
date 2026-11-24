package com.backpackmod.client;

import com.backpackmod.BreakActions;
import com.backpackmod.HammerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Renders a white outline around every block that will be broken when sneaking (holding Shift) with a hammer.
 */
public final class HammerOutlineRenderer {
    private record OutlineBlock(BlockPos pos, VoxelShape shape) {}

    private static final RenderStateDataKey<List<OutlineBlock>> HAMMER_OUTLINE_KEY =
            RenderStateDataKey.create(() -> "backpackmod:hammer_outline");
    private static final int WHITE_ARGB = 0xFFFFFFFF;

    public static void register() {
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hitResult) -> {
            BlockOutlineRenderState outlineState = context.levelState().blockOutlineRenderState;
            if (outlineState == null) return;
            if (!(hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) return;

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || !player.isShiftKeyDown()) return;

            ItemStack tool = player.getMainHandItem();
            if (!(tool.getItem() instanceof HammerItem hammer) || hammer.size() <= 1) return;

            ClientLevel level = context.level();
            List<BlockPos> targets = BreakActions.previewHammerTargets(
                    level, player, blockHit.getBlockPos(), blockHit.getDirection(), tool);
            if (targets.isEmpty()) return;

            CollisionContext collisionContext = CollisionContext.of(context.camera().entity());
            List<OutlineBlock> entries = new ArrayList<>(targets.size());
            for (BlockPos targetPos : targets) {
                VoxelShape shape = level.getBlockState(targetPos).getShape(level, targetPos, collisionContext);
                if (!shape.isEmpty()) {
                    entries.add(new OutlineBlock(targetPos.immutable(), shape));
                }
            }
            if (!entries.isEmpty()) {
                outlineState.setData(HAMMER_OUTLINE_KEY, List.copyOf(entries));
            }
        });

        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) -> {
            if (outlineRenderState == null) return true;
            List<OutlineBlock> blocks = outlineRenderState.getData(HAMMER_OUTLINE_KEY);
            if (blocks == null || blocks.isEmpty()) return true;

            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();
            Vec3 camPos = context.levelState().cameraRenderState.pos;
            float lineWidth = Math.max(2.5F,
                    context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth);
            RenderType renderType = RenderTypes.lines();
            boolean isTranslucent = outlineRenderState.isTranslucent();

            for (OutlineBlock block : blocks) {
                poseStack.pushPose();
                poseStack.translate(
                        block.pos().getX() - camPos.x,
                        block.pos().getY() - camPos.y,
                        block.pos().getZ() - camPos.z
                );
                collector.submitShapeOutline(poseStack, block.shape(), renderType, WHITE_ARGB, lineWidth, isTranslucent);
                poseStack.popPose();
            }
            return false;
        });
    }

    private HammerOutlineRenderer() {}
}

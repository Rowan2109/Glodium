package com.glodblock.github.glodium.client.render.highlight;

import com.glodblock.github.glodium.Glodium;
import com.glodblock.github.glodium.client.render.ColorData;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

public class HighlightRender {

    public static final HighlightRender INSTANCE = new HighlightRender();
    private final RenderPipeline BLOCK_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Glodium.id(Glodium.MODID, "pipeline/block_highlight"))
            .withColorTargetState(new ColorTargetState(BlendFunction.GLINT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    private final RenderType BLOCK_HIGHLIGHT_LINE = RenderType.create(
            "glodium:block_highlight_line",
            RenderSetup.builder(BLOCK_HIGHLIGHT_PIPELINE)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    public static void hook(SubmitCustomGeometryEvent event) {
        HighlightRender.INSTANCE.tick(event.getPoseStack(), event.getSubmitNodeCollector(), event.getLevelRenderState().cameraRenderState.pos);
    }

    public void tick(PoseStack stack, SubmitNodeCollector collector, Vec3 camera) {
        var world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        this.invalidate();
        var drawList = HighlightHandler.getBlockData();
        if (drawList.isEmpty()) {
            return;
        }
        for (var block : drawList) {
            if (block.checkDim(world.dimension()) && block.allowRender()) {
                drawBlockOutline(block.box(), block.color(), stack, camera, collector);
            }
        }
    }

    private void invalidate() {
        while (HighlightHandler.getFirst() != null) {
            var info = HighlightHandler.getFirst();
            if (System.currentTimeMillis() > info.time()) {
                HighlightHandler.expire();
            } else {
                break;
            }
        }
    }

    private void drawBlockOutline(AABB box, ColorData color, PoseStack stack, Vec3 camera, SubmitNodeCollector collector) {
        var r = color.getRf();
        var g = color.getGf();
        var b = color.getBf();
        var a = color.getAf();
        AABB aabb = box.move(camera.reverse());
        collector.submitCustomGeometry(stack, BLOCK_HIGHLIGHT_LINE, (pose, buf) -> {
            var topRight = new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ);
            var bottomRight = new Vec3(aabb.maxX, aabb.minY, aabb.maxZ);
            var bottomLeft = new Vec3(aabb.minX, aabb.minY, aabb.maxZ);
            var topLeft = new Vec3(aabb.minX, aabb.maxY, aabb.maxZ);
            var topRight2 = new Vec3(aabb.maxX, aabb.maxY, aabb.minZ);
            var bottomRight2 = new Vec3(aabb.maxX, aabb.minY, aabb.minZ);
            var bottomLeft2 = new Vec3(aabb.minX, aabb.minY, aabb.minZ);
            var topLeft2 = new Vec3(aabb.minX, aabb.maxY, aabb.minZ);
            renderBox(buf, pose, topLeft, bottomLeft, topRight, bottomRight, r, g, b, a);
            renderBox(buf, pose, topLeft2, bottomLeft2, topRight2, bottomRight2, r, g, b, a);
            renderLine(buf, pose, topRight, topRight2, r, g, b, a);
            renderLine(buf, pose, bottomRight, bottomRight2, r, g, b, a);
            renderLine(buf, pose, bottomLeft, bottomLeft2, r, g, b, a);
            renderLine(buf, pose, topLeft, topLeft2, r, g, b, a);
        });
    }

    private void renderBox(VertexConsumer buf, PoseStack.Pose stack, Vec3 topLeft, Vec3 bottomLeft, Vec3 topRight, Vec3 bottomRight, float r, float g, float b, float a) {
        renderLine(buf, stack, topLeft, bottomLeft, r, g, b, a);
        renderLine(buf, stack, topLeft, topRight, r, g, b, a);
        renderLine(buf, stack, bottomRight, bottomLeft, r, g, b, a);
        renderLine(buf, stack, bottomRight, topRight, r, g, b, a);
    }

    private void renderLine(VertexConsumer buf, PoseStack.Pose pose, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        var mat = pose.pose();
        var normal = from.subtract(to);
        buf.addVertex(mat, (float) from.x, (float) from.y, (float) from.z).setColor(r, g, b, a).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z).setLineWidth(3);
        buf.addVertex(mat, (float) to.x, (float) to.y, (float) to.z).setColor(r, g, b, a).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z).setLineWidth(3);
    }

}

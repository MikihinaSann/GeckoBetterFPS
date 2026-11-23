package com.moepus.gbf.mixin;

import com.moepus.gbf.renderer.IrisCubeRenderer;
import com.moepus.gbf.renderer.IrisEntityVertex;
import com.moepus.gbf.renderer.SodiumCubeRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

@Mixin(
   value = {GeoRenderer.class},
   remap = false
)
public interface GeoRendererMixin {
   @Unique
   default void gbf$createVerticesOfQuad(
      GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay, int colour
   ) {
      Vector3f transformed = new Vector3f();

      for (GeoVertex vertex : quad.vertices()) {
         vertex.position().mulPosition(poseState, transformed);
         buffer.addVertex(
            transformed.x(),
            transformed.y(),
            transformed.z(),
            colour,
            vertex.texU(),
            vertex.texV(),
            packedOverlay,
            packedLight,
            normal.x(),
            normal.y(),
            normal.z()
         );
      }
   }

   @Overwrite(
      remap = false
   )
   default void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
      Vec3 rotation = cube.rotation();
      if (!rotation.equals(Vec3.ZERO)) {
         Vec3 pivot = cube.pivot();
         poseStack.rotateAround(
            new Quaternionf().rotationZYX((float)rotation.z(), (float)rotation.y(), (float)rotation.x()),
            (float)(pivot.x() / 16.0),
            (float)(pivot.y() / 16.0),
            (float)(pivot.z() / 16.0)
         );
      }

      VertexBufferWriter writer = VertexBufferWriter.tryOf(buffer);
      if (writer != null) {
         int sodiumColour = ColorARGB.toABGR(colour);
         if (buffer instanceof BufferBuilder bb) {
            if (bb.format == DefaultVertexFormat.NEW_ENTITY) {
               SodiumCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, sodiumColour);
               return;
            }

            if (bb.format == IrisEntityVertex.FORMAT) {
               IrisCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, sodiumColour);
               return;
            }
         }
      }

      Matrix3f normalisedPoseState = poseStack.last().normal();
      Matrix4f poseState = poseStack.last().pose();
      Vector3f normal = new Vector3f();

      for (GeoQuad quad : cube.quads()) {
         if (quad != null) {
            quad.normal().mul(normalisedPoseState, normal);
            RenderUtil.fixInvertedFlatCube(cube, normal);
            this.gbf$createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, colour);
         }
      }
   }
}

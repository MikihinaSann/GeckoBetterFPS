package com.moepus.gbf.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

public class SodiumCubeRenderer {
   private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64L, 144L);

   private static void createVerticesOfQuad(
      GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexBufferWriter writer, int packedLight, int packedOverlay, int color
   ) {
      if (quad.vertices().length == 4) {
         Vector3f transformed = new Vector3f();
         long buffer = SCRATCH_BUFFER;
         long ptr = buffer;
         int inormal = NormI8.pack(normal);

         for (GeoVertex vertex : quad.vertices()) {
            vertex.position().mulPosition(poseState, transformed);
            EntityVertex.write(ptr, transformed.x(), transformed.y(), transformed.z(), color, vertex.texU(), vertex.texV(), packedOverlay, packedLight, inormal);
            ptr += 36L;
         }

         MemoryStack stack = MemoryStack.stackPush();

         try {
            writer.push(stack, buffer, quad.vertices().length, EntityVertex.FORMAT);
         } catch (Throwable var18) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var17) {
                  var18.addSuppressed(var17);
               }
            }

            throw var18;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   public static void renderCube(PoseStack poseStack, GeoCube cube, VertexBufferWriter writer, int packedLight, int packedOverlay, int color) {
      Matrix3f normalisedPoseState = poseStack.last().normal();
      Matrix4f poseState = poseStack.last().pose();
      Vector3f normal = new Vector3f();

      for (GeoQuad quad : cube.quads()) {
         if (quad != null) {
            quad.normal().mul(normalisedPoseState, normal);
            RenderUtil.fixInvertedFlatCube(cube, normal);
            createVerticesOfQuad(quad, poseState, normal, writer, packedLight, packedOverlay, color);
         }
      }
   }
}

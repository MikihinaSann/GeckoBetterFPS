package com.moepus.gbf.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.irisshaders.iris.vertices.NormalHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

public class IrisCubeRenderer {
   private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64L, 4 * IrisEntityVertex.STRIDE);

   private static void createVerticesOfQuad(
      GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexBufferWriter writer, int packedLight, int packedOverlay, int color
   ) {
      GeoVertex[] vertices = quad.vertices();
      if (vertices.length == 4) {
         Vector3f pos0 = new Vector3f();
         Vector3f pos1 = new Vector3f();
         Vector3f pos2 = new Vector3f();
         Vector3f pos3 = new Vector3f();
         vertices[0].position().mulPosition(poseState, pos0);
         vertices[1].position().mulPosition(poseState, pos1);
         vertices[2].position().mulPosition(poseState, pos2);
         vertices[3].position().mulPosition(poseState, pos3);
         int tangent = NormalHelper.computeTangent(
            normal.x(),
            normal.y(),
            normal.z(),
            pos0.x(),
            pos0.y(),
            pos0.z(),
            vertices[0].texU(),
            vertices[0].texV(),
            pos1.x(),
            pos1.y(),
            pos1.z(),
            vertices[1].texU(),
            vertices[1].texV(),
            pos2.x(),
            pos2.y(),
            pos2.z(),
            vertices[2].texU(),
            vertices[2].texV()
         );
         long buffer = SCRATCH_BUFFER;
         int inormal = NormI8.pack(normal);
         float mid_u = (vertices[0].texU() + vertices[1].texU() + vertices[2].texU() + vertices[3].texU()) / 4.0F;
         float mid_v = (vertices[0].texV() + vertices[1].texV() + vertices[2].texV() + vertices[3].texV()) / 4.0F;
         IrisEntityVertex.write(
            buffer, pos0.x(), pos0.y(), pos0.z(), color, vertices[0].texU(), vertices[0].texV(), mid_u, mid_v, packedOverlay, packedLight, inormal, tangent
         );
         long ptr = buffer + IrisEntityVertex.STRIDE;
         IrisEntityVertex.write(
            ptr, pos1.x(), pos1.y(), pos1.z(), color, vertices[1].texU(), vertices[1].texV(), mid_u, mid_v, packedOverlay, packedLight, inormal, tangent
         );
         ptr += IrisEntityVertex.STRIDE;
         IrisEntityVertex.write(
            ptr, pos2.x(), pos2.y(), pos2.z(), color, vertices[2].texU(), vertices[2].texV(), mid_u, mid_v, packedOverlay, packedLight, inormal, tangent
         );
         ptr += IrisEntityVertex.STRIDE;
         IrisEntityVertex.write(
            ptr, pos3.x(), pos3.y(), pos3.z(), color, vertices[3].texU(), vertices[3].texV(), mid_u, mid_v, packedOverlay, packedLight, inormal, tangent
         );
         MemoryStack stack = MemoryStack.stackPush();

         try {
            writer.push(stack, buffer, 4, IrisEntityVertex.FORMAT);
         } catch (Throwable var24) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var23) {
                  var24.addSuppressed(var23);
               }
            }

            throw var24;
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

package com.moepus.gbf.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

@Mixin(
   value = {RenderUtil.class},
   remap = false
)
public abstract class RenderUtilsMixin {
   @Overwrite(
      remap = false
   )
   public static void rotateMatrixAroundBone(PoseStack poseStack, GeoBone bone) {
      if (bone.getRotZ() != 0.0F || bone.getRotY() != 0.0F || bone.getRotX() != 0.0F) {
         poseStack.mulPose(new Quaternionf().rotationZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX()));
      }
   }

   @Overwrite(
      remap = false
   )
   public static Matrix4f translateMatrix(Matrix4f matrix, Vector3f vector) {
      return matrix.m30(matrix.m30() + vector.x).m31(matrix.m31() + vector.y).m32(matrix.m32() + vector.z);
   }
}

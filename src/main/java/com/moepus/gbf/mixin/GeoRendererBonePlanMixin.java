package com.moepus.gbf.mixin;

import com.moepus.gbf.renderer.plan.BoneRenderPlan;
import com.moepus.gbf.renderer.plan.BoneRenderPlanProvider;
import com.moepus.gbf.renderer.plan.BoneVisit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

@Mixin(
   value = {GeoRenderer.class},
   remap = false
)
public interface GeoRendererBonePlanMixin<T extends GeoAnimatable> extends BoneRenderPlanProvider {
   @Overwrite(
      remap = false
   )
   default void actuallyRender(
      PoseStack poseStack,
      T animatable,
      BakedGeoModel model,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int colour
   ) {
      GeoRenderer<T> renderer = (GeoRenderer<T>)this;
      if (buffer == null) {
         if (renderType == null) {
            return;
         }

         buffer = bufferSource.getBuffer(renderType);
      }

      BoneRenderPlan plan = this.gbf$getBoneRenderPlan();
      List<GeoBone> bones = model.topLevelBones();
      plan.prepare(model, !renderer.getRenderLayers().isEmpty());
      renderer.updateAnimatedTextureFrame(animatable);
      int i = 0;

      for (int size = bones.size(); i < size; i++) {
         GeoBone bone = bones.get(i);
         if (plan.classify(bone) != BoneVisit.SKIP_SUBTREE) {
            renderer.renderRecursively(
               poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour
            );
         }
      }
   }

   @Overwrite(
      remap = false
   )
   default void renderRecursively(
      PoseStack poseStack,
      T animatable,
      GeoBone bone,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int colour
   ) {
      GeoRenderer<T> renderer = (GeoRenderer<T>)this;
      BoneVisit visit = this.gbf$getBoneRenderPlan().classify(bone);
      if (visit != BoneVisit.SKIP_SUBTREE) {
         poseStack.pushPose();
         RenderUtil.prepMatrixForBone(poseStack, bone);
         buffer = renderer.checkAndRefreshBuffer(isReRender, buffer, bufferSource, renderType);
         if (visit == BoneVisit.RENDER) {
            renderer.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
            if (!isReRender) {
               renderer.applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
         }

         renderer.renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
         poseStack.popPose();
      }
   }

   @Overwrite(
      remap = false
   )
   default void renderChildBones(
      PoseStack poseStack,
      T animatable,
      GeoBone bone,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int colour
   ) {
      GeoRenderer<T> renderer = (GeoRenderer<T>)this;
      BoneRenderPlan plan = this.gbf$getBoneRenderPlan();
      if (!bone.isHidingChildren()) {
         List<GeoBone> children = bone.getChildBones();
         int i = 0;

         for (int size = children.size(); i < size; i++) {
            GeoBone child = children.get(i);
            if (plan.classify(child) != BoneVisit.SKIP_SUBTREE) {
               renderer.renderRecursively(
                  poseStack, animatable, child, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour
               );
            }
         }
      }
   }
}

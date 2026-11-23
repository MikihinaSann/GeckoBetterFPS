package com.moepus.gbf.mixin;

import com.moepus.gbf.animation.AnimationControllerExtension;
import com.moepus.gbf.animation.AnimationTickOptimizer;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

@Mixin(
   value = {AnimationProcessor.class},
   remap = false
)
public abstract class AnimationProcessorMixin<T extends GeoAnimatable> {
   @Shadow
   @Final
   private Map<String, GeoBone> bones;
   @Shadow
   public boolean reloadAnimations;
   @Unique
   private final AnimationTickOptimizer gbf$optimizer = new AnimationTickOptimizer();

   @Overwrite(
      remap = false
   )
   public void tickAnimation(
      T animatable, GeoModel<T> model, AnimatableManager<T> animatableManager, double animTime, AnimationState<T> state, boolean crashWhenCantFindBone
   ) {
      Map<String, BoneSnapshot> snapshots = this.gbf$optimizer.updateBoneSnapshots(this.bones.values(), animatableManager.getBoneSnapshotCollection());

      for (AnimationController<T> controller : animatableManager.getAnimationControllers().values()) {
         AnimationControllerExtension extension = (AnimationControllerExtension)controller;
         if (this.reloadAnimations) {
            controller.forceAnimationReset();
            controller.getBoneAnimationQueues().clear();
         }

         extension.gbf$beginFrame();
         extension.gbf$setJustStarting(animatableManager.isFirstTick());
         state.withController(controller);
         controller.process(model, state, this.bones, snapshots, animTime, crashWhenCantFindBone);
         this.gbf$optimizer.applyActiveQueues(controller, snapshots);
      }

      this.reloadAnimations = false;
      this.gbf$optimizer.configureReset(animTime, animatable.getBoneResetTime());
      this.gbf$optimizer.resetBones(this.bones.values(), snapshots);
      ((AnimatableManagerAccessor)animatableManager).gbf$finishFirstTick();
   }
}

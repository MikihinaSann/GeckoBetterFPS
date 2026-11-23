package com.moepus.gbf.mixin;

import com.moepus.gbf.animation.BoneAnimationQueueExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animation.keyframe.AnimationPointQueue;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;

@Mixin(
   value = {BoneAnimationQueue.class},
   remap = false
)
public abstract class BoneAnimationQueueMixin implements BoneAnimationQueueExtension {
   @Unique
   private ObjectArrayList<BoneAnimationQueue> gbf$activeQueues;
   @Unique
   private boolean gbf$active;

   @Shadow
   public abstract AnimationPointQueue rotationXQueue();

   @Shadow
   public abstract AnimationPointQueue rotationYQueue();

   @Shadow
   public abstract AnimationPointQueue rotationZQueue();

   @Shadow
   public abstract AnimationPointQueue positionXQueue();

   @Shadow
   public abstract AnimationPointQueue positionYQueue();

   @Shadow
   public abstract AnimationPointQueue positionZQueue();

   @Shadow
   public abstract AnimationPointQueue scaleXQueue();

   @Shadow
   public abstract AnimationPointQueue scaleYQueue();

   @Shadow
   public abstract AnimationPointQueue scaleZQueue();

   @Override
   public void gbf$reset(ObjectArrayList<BoneAnimationQueue> activeQueues) {
      this.rotationXQueue().clear();
      this.rotationYQueue().clear();
      this.rotationZQueue().clear();
      this.positionXQueue().clear();
      this.positionYQueue().clear();
      this.positionZQueue().clear();
      this.scaleXQueue().clear();
      this.scaleYQueue().clear();
      this.scaleZQueue().clear();
      this.gbf$activeQueues = activeQueues;
      this.gbf$active = false;
   }

   @Override
   public void gbf$activate() {
      if (!this.gbf$active && this.gbf$activeQueues != null) {
         this.gbf$active = true;
         this.gbf$activeQueues.add((BoneAnimationQueue)(Object)this);
      }
   }
}

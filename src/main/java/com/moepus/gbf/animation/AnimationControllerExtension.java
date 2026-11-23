package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.animation.EasingType;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;

public interface AnimationControllerExtension {
   void gbf$beginFrame();

   ObjectArrayList<BoneAnimationQueue> gbf$getActiveQueues();

   void gbf$setJustStarting(boolean var1);

   EasingType gbf$getEasingType();
}

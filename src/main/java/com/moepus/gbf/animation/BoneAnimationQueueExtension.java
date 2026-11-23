package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;

public interface BoneAnimationQueueExtension {
   void gbf$reset(ObjectArrayList<BoneAnimationQueue> var1);

   void gbf$activate();
}

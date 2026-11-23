package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.Map;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.cache.object.GeoBone;

public final class AnimationQueueManager {
   private final ObjectArrayList<BoneAnimationQueue> activeQueues = new ObjectArrayList();

   public void beginFrame() {
      this.activeQueues.clear();
   }

   public void prepare(Map<String, BoneAnimationQueue> queues, Collection<GeoBone> bones) {
      this.beginFrame();
      if (!hasSameBoneLayout(queues, bones)) {
         this.rebuild(queues, bones);
      } else {
         for (GeoBone bone : bones) {
            this.reset(queues.get(bone.getName()));
         }
      }
   }

   public ObjectArrayList<BoneAnimationQueue> activeQueues() {
      return this.activeQueues;
   }

   public void activate(BoneAnimationQueue queue) {
      ((BoneAnimationQueueExtension)(Object)queue).gbf$activate();
   }

   static boolean hasSameBoneLayout(Map<String, BoneAnimationQueue> queues, Collection<GeoBone> bones) {
      if (queues.size() != bones.size()) {
         return false;
      } else {
         for (GeoBone bone : bones) {
            BoneAnimationQueue queue = queues.get(bone.getName());
            if (queue == null || queue.bone() != bone) {
               return false;
            }
         }

         return true;
      }
   }

   private void rebuild(Map<String, BoneAnimationQueue> queues, Collection<GeoBone> bones) {
      queues.clear();

      for (GeoBone bone : bones) {
         BoneAnimationQueue queue = new BoneAnimationQueue(bone);
         this.reset(queue);
         queues.put(bone.getName(), queue);
      }
   }

   private void reset(BoneAnimationQueue queue) {
      ((BoneAnimationQueueExtension)(Object)queue).gbf$reset(this.activeQueues);
   }
}

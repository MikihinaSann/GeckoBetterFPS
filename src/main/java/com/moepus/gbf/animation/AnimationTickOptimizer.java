package com.moepus.gbf.animation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.Map;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.EasingType;
import software.bernie.geckolib.animation.keyframe.AnimationPoint;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;

public final class AnimationTickOptimizer {
   private double animTime;
   private double resetTickLength;
   private double inverseResetTickLength;

   public Map<String, BoneSnapshot> updateBoneSnapshots(Collection<GeoBone> bones, Map<String, BoneSnapshot> snapshots) {
      for (GeoBone bone : bones) {
         if (!snapshots.containsKey(bone.getName())) {
            snapshots.put(bone.getName(), BoneSnapshot.copy(bone.getInitialSnapshot()));
         }
      }

      return snapshots;
   }

   public void applyActiveQueues(AnimationController<?> controller, Map<String, BoneSnapshot> snapshots) {
      AnimationControllerExtension extension = (AnimationControllerExtension)controller;
      ObjectArrayList<BoneAnimationQueue> queues = extension.gbf$getActiveQueues();
      int i = 0;

      for (int size = queues.size(); i < size; i++) {
         BoneAnimationQueue queue = (BoneAnimationQueue)queues.get(i);
         this.applyQueue(queue, snapshots.get(queue.bone().getName()), extension.gbf$getEasingType());
      }
   }

   public void configureReset(double animTime, double resetTickLength) {
      this.animTime = animTime;
      this.resetTickLength = resetTickLength;
      this.inverseResetTickLength = resetTickLength == 0.0 ? 0.0 : 1.0 / resetTickLength;
   }

   public void resetBones(Collection<GeoBone> bones, Map<String, BoneSnapshot> snapshots) {
      for (GeoBone bone : bones) {
         boolean resetRotation = !bone.hasRotationChanged();
         boolean resetPosition = !bone.hasPositionChanged();
         boolean resetScale = !bone.hasScaleChanged();
         if (resetRotation || resetPosition || resetScale) {
            BoneSnapshot saved = snapshots.get(bone.getName());
            BoneSnapshot initial = bone.getInitialSnapshot();
            if (resetRotation) {
               this.resetRotation(bone, initial, saved);
            }

            if (resetPosition) {
               this.resetPosition(bone, initial, saved);
            }

            if (resetScale) {
               this.resetScale(bone, initial, saved);
            }
         }

         bone.resetStateChanges();
      }
   }

   private void applyQueue(BoneAnimationQueue queue, BoneSnapshot snapshot, EasingType easingType) {
      GeoBone bone = queue.bone();
      BoneSnapshot initial = bone.getInitialSnapshot();
      AnimationPoint rotX = (AnimationPoint)queue.rotationXQueue().poll();
      AnimationPoint rotY = (AnimationPoint)queue.rotationYQueue().poll();
      AnimationPoint rotZ = (AnimationPoint)queue.rotationZQueue().poll();
      AnimationPoint posX = (AnimationPoint)queue.positionXQueue().poll();
      AnimationPoint posY = (AnimationPoint)queue.positionYQueue().poll();
      AnimationPoint posZ = (AnimationPoint)queue.positionZQueue().poll();
      AnimationPoint scaleX = (AnimationPoint)queue.scaleXQueue().poll();
      AnimationPoint scaleY = (AnimationPoint)queue.scaleYQueue().poll();
      AnimationPoint scaleZ = (AnimationPoint)queue.scaleZQueue().poll();
      if (rotX != null && rotY != null && rotZ != null) {
         bone.setRotX((float)EasingType.lerpWithOverride(rotX, easingType) + initial.getRotX());
         bone.setRotY((float)EasingType.lerpWithOverride(rotY, easingType) + initial.getRotY());
         bone.setRotZ((float)EasingType.lerpWithOverride(rotZ, easingType) + initial.getRotZ());
         snapshot.updateRotation(bone.getRotX(), bone.getRotY(), bone.getRotZ());
         snapshot.startRotAnim();
         bone.markRotationAsChanged();
      }

      if (posX != null && posY != null && posZ != null) {
         bone.setPosX((float)EasingType.lerpWithOverride(posX, easingType));
         bone.setPosY((float)EasingType.lerpWithOverride(posY, easingType));
         bone.setPosZ((float)EasingType.lerpWithOverride(posZ, easingType));
         snapshot.updateOffset(bone.getPosX(), bone.getPosY(), bone.getPosZ());
         snapshot.startPosAnim();
         bone.markPositionAsChanged();
      }

      if (scaleX != null && scaleY != null && scaleZ != null) {
         bone.setScaleX((float)EasingType.lerpWithOverride(scaleX, easingType));
         bone.setScaleY((float)EasingType.lerpWithOverride(scaleY, easingType));
         bone.setScaleZ((float)EasingType.lerpWithOverride(scaleZ, easingType));
         snapshot.updateScale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
         snapshot.startScaleAnim();
         bone.markScaleAsChanged();
      }
   }

   private void resetRotation(GeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
      if (!isRotationSettled(bone, initial, saved)) {
         if (saved.isRotAnimInProgress()) {
            saved.stopRotAnim(this.animTime);
         }

         double percentage = this.resetPercentage(saved.getLastResetRotationTick());
         float initialX = initial.getRotX();
         float initialY = initial.getRotY();
         float initialZ = initial.getRotZ();
         float lastX = saved.getRotX();
         float lastY = saved.getRotY();
         float lastZ = saved.getRotZ();
         if (percentage == 0.0) {
            if (lastX != initialX && isSuspectedCompletedRotation(lastX)) {
               lastX = initialX;
               percentage = 1.0;
            }

            if (lastY != initialY && isSuspectedCompletedRotation(lastY)) {
               lastY = initialY;
               percentage = 1.0;
            }

            if (lastZ != initialZ && isSuspectedCompletedRotation(lastZ)) {
               lastZ = initialZ;
               percentage = 1.0;
            }
         }

         if (percentage >= 1.0) {
            bone.setRotX(initialX);
            bone.setRotY(initialY);
            bone.setRotZ(initialZ);
            saved.updateRotation(initialX, initialY, initialZ);
         } else {
            bone.setRotX((float)lerp(lastX, initialX, percentage));
            bone.setRotY((float)lerp(lastY, initialY, percentage));
            bone.setRotZ((float)lerp(lastZ, initialZ, percentage));
         }
      }
   }

   private void resetPosition(GeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
      if (!isPositionSettled(bone, initial, saved)) {
         if (saved.isPosAnimInProgress()) {
            saved.stopPosAnim(this.animTime);
         }

         double percentage = this.resetPercentage(saved.getLastResetPositionTick());
         float initialX = initial.getOffsetX();
         float initialY = initial.getOffsetY();
         float initialZ = initial.getOffsetZ();
         if (percentage >= 1.0) {
            bone.setPosX(initialX);
            bone.setPosY(initialY);
            bone.setPosZ(initialZ);
            saved.updateOffset(initialX, initialY, initialZ);
         } else {
            bone.setPosX((float)lerp(saved.getOffsetX(), initialX, percentage));
            bone.setPosY((float)lerp(saved.getOffsetY(), initialY, percentage));
            bone.setPosZ((float)lerp(saved.getOffsetZ(), initialZ, percentage));
         }
      }
   }

   private void resetScale(GeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
      if (!isScaleSettled(bone, initial, saved)) {
         if (saved.isScaleAnimInProgress()) {
            saved.stopScaleAnim(this.animTime);
         }

         double percentage = this.resetPercentage(saved.getLastResetScaleTick());
         float initialX = initial.getScaleX();
         float initialY = initial.getScaleY();
         float initialZ = initial.getScaleZ();
         if (percentage >= 1.0) {
            bone.setScaleX(initialX);
            bone.setScaleY(initialY);
            bone.setScaleZ(initialZ);
            saved.updateScale(initialX, initialY, initialZ);
         } else {
            bone.setScaleX((float)lerp(saved.getScaleX(), initialX, percentage));
            bone.setScaleY((float)lerp(saved.getScaleY(), initialY, percentage));
            bone.setScaleZ((float)lerp(saved.getScaleZ(), initialZ, percentage));
         }
      }
   }

   private double resetPercentage(double lastResetTick) {
      return this.resetTickLength == 0.0 ? 1.0 : Math.min((this.animTime - lastResetTick) * this.inverseResetTickLength, 1.0);
   }

   private static boolean isRotationSettled(GeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
      return !saved.isRotAnimInProgress()
         && sameFloat(saved.getRotX(), initial.getRotX())
         && sameFloat(saved.getRotY(), initial.getRotY())
         && sameFloat(saved.getRotZ(), initial.getRotZ())
         && sameFloat(bone.getRotX(), initial.getRotX())
         && sameFloat(bone.getRotY(), initial.getRotY())
         && sameFloat(bone.getRotZ(), initial.getRotZ());
   }

   private static boolean isPositionSettled(GeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
      return !saved.isPosAnimInProgress()
         && sameFloat(saved.getOffsetX(), initial.getOffsetX())
         && sameFloat(saved.getOffsetY(), initial.getOffsetY())
         && sameFloat(saved.getOffsetZ(), initial.getOffsetZ())
         && sameFloat(bone.getPosX(), initial.getOffsetX())
         && sameFloat(bone.getPosY(), initial.getOffsetY())
         && sameFloat(bone.getPosZ(), initial.getOffsetZ());
   }

   private static boolean isScaleSettled(GeoBone bone, BoneSnapshot initial, BoneSnapshot saved) {
      return !saved.isScaleAnimInProgress()
         && sameFloat(saved.getScaleX(), initial.getScaleX())
         && sameFloat(saved.getScaleY(), initial.getScaleY())
         && sameFloat(saved.getScaleZ(), initial.getScaleZ())
         && sameFloat(bone.getScaleX(), initial.getScaleX())
         && sameFloat(bone.getScaleY(), initial.getScaleY())
         && sameFloat(bone.getScaleZ(), initial.getScaleZ());
   }

   private static boolean sameFloat(float left, float right) {
      return Float.floatToRawIntBits(left) == Float.floatToRawIntBits(right);
   }

   private static double lerp(double start, double end, double delta) {
      return start + (end - start) * delta;
   }

   private static boolean isSuspectedCompletedRotation(float lastRotation) {
      float rotations = Math.abs(lastRotation / (float) (Math.PI * 2));
      float partialRotation = 1.0F - (rotations - (int)rotations);
      return partialRotation == 1.0F || partialRotation < 0.026 * rotations;
   }
}

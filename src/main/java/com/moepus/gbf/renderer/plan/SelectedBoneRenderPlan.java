package com.moepus.gbf.renderer.plan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public final class SelectedBoneRenderPlan implements BoneRenderPlan {
   private final ObjectArrayList<GeoBone> selectedRoots = new ObjectArrayList(3);
   private final ObjectArrayList<GeoBone> preservedRoots = new ObjectArrayList(3);
   private final boolean preserveVisibleGeometry;
   private boolean enabled;
   private boolean fallbackToFull;

   public SelectedBoneRenderPlan() {
      this(false);
   }

   public SelectedBoneRenderPlan(boolean preserveVisibleGeometry) {
      this.preserveVisibleGeometry = preserveVisibleGeometry;
   }

   public void disable() {
      this.selectedRoots.clear();
      this.preservedRoots.clear();
      this.enabled = false;
      this.fallbackToFull = false;
   }

   public void selectOnly(GeoBone bone) {
      this.disable();
      this.include(bone);
   }

   public void include(GeoBone bone) {
      if (bone != null && !this.containsIdentity(bone)) {
         this.selectedRoots.add(bone);
         this.enabled = true;
      }
   }

   @Override
   public void prepare(BakedGeoModel model, boolean hasPerBoneLayers) {
      this.preservedRoots.clear();
      this.fallbackToFull = false;
      if (this.enabled && this.preserveVisibleGeometry) {
         if (hasPerBoneLayers) {
            this.fallbackToFull = true;
         } else {
            List<GeoBone> topLevelBones = model.topLevelBones();
            int i = 0;

            for (int size = topLevelBones.size(); i < size; i++) {
               this.collectVisibleGeometry(topLevelBones.get(i));
            }
         }
      }
   }

   @Override
   public BoneVisit classify(GeoBone bone) {
      if (this.enabled && !this.fallbackToFull) {
         BoneVisit selectedVisit = this.classifyAgainst(this.selectedRoots, bone);
         if (selectedVisit == BoneVisit.RENDER) {
            return BoneVisit.RENDER;
         } else {
            BoneVisit preservedVisit = this.classifyAgainst(this.preservedRoots, bone);
            return preservedVisit.ordinal() > selectedVisit.ordinal() ? preservedVisit : selectedVisit;
         }
      } else {
         return BoneVisit.RENDER;
      }
   }

   private BoneVisit classifyAgainst(ObjectArrayList<GeoBone> roots, GeoBone bone) {
      BoneVisit result = BoneVisit.SKIP_SUBTREE;
      int i = 0;

      for (int size = roots.size(); i < size; i++) {
         GeoBone selectedRoot = (GeoBone)roots.get(i);
         if (isDescendantOrSelf(bone, selectedRoot)) {
            return BoneVisit.RENDER;
         }

         if (isDescendantOrSelf(selectedRoot, bone)) {
            result = BoneVisit.TRANSFORM_ONLY;
         }
      }

      return result;
   }

   private void collectVisibleGeometry(GeoBone bone) {
      if (this.classifyAgainst(this.selectedRoots, bone) != BoneVisit.RENDER) {
         if (!bone.isTrackingMatrices() && (bone.isHidden() || bone.getCubes().isEmpty())) {
            if (!bone.isHidingChildren()) {
               List<GeoBone> children = bone.getChildBones();
               int i = 0;

               for (int size = children.size(); i < size; i++) {
                  this.collectVisibleGeometry(children.get(i));
               }
            }
         } else {
            this.preservedRoots.add(bone);
         }
      }
   }

   private boolean containsIdentity(GeoBone bone) {
      int i = 0;

      for (int size = this.selectedRoots.size(); i < size; i++) {
         if (this.selectedRoots.get(i) == bone) {
            return true;
         }
      }

      return false;
   }

   private static boolean isDescendantOrSelf(GeoBone bone, GeoBone ancestor) {
      for (GeoBone current = bone; current != null; current = current.getParent()) {
         if (current == ancestor) {
            return true;
         }
      }

      return false;
   }
}

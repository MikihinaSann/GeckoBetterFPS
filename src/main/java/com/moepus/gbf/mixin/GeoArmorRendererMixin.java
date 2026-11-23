package com.moepus.gbf.mixin;

import com.moepus.gbf.renderer.plan.BoneRenderPlan;
import com.moepus.gbf.renderer.plan.BoneRenderPlanProvider;
import com.moepus.gbf.renderer.plan.SelectedBoneRenderPlan;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.mixinlite.injector.InjectLite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

@Mixin(
   value = {GeoArmorRenderer.class},
   remap = false
)
public abstract class GeoArmorRendererMixin<T extends Item & GeoItem> implements BoneRenderPlanProvider {
   @Shadow
   protected GeoBone head;
   @Shadow
   protected GeoBone body;
   @Shadow
   protected GeoBone rightArm;
   @Shadow
   protected GeoBone leftArm;
   @Shadow
   protected GeoBone rightLeg;
   @Shadow
   protected GeoBone leftLeg;
   @Shadow
   protected GeoBone rightBoot;
   @Shadow
   protected GeoBone leftBoot;
   @Unique
   private final SelectedBoneRenderPlan gbf$boneRenderPlan = new SelectedBoneRenderPlan(true);

   @Shadow
   protected abstract void setAllBonesVisible(boolean var1);

   @Shadow
   protected abstract void setBoneVisible(GeoBone var1, boolean var2);

   @Override
   public BoneRenderPlan gbf$getBoneRenderPlan() {
      return this.gbf$boneRenderPlan;
   }

   @Overwrite(
      remap = false
   )
   protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
      HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
      this.gbf$boneRenderPlan.disable();
      this.setAllBonesVisible(false);
      switch (currentSlot) {
         case HEAD:
            this.gbf$selectVisible(this.head, model.head.visible);
            break;
         case CHEST:
            this.gbf$selectVisible(this.body, model.body.visible);
            this.gbf$selectVisible(this.rightArm, model.rightArm.visible);
            this.gbf$selectVisible(this.leftArm, model.leftArm.visible);
            break;
         case LEGS:
            this.gbf$selectVisible(this.rightLeg, model.rightLeg.visible);
            this.gbf$selectVisible(this.leftLeg, model.leftLeg.visible);
            break;
         case FEET:
            this.gbf$selectVisible(this.rightBoot, model.rightLeg.visible);
            this.gbf$selectVisible(this.leftBoot, model.leftLeg.visible);
      }
   }

   @Overwrite(
      remap = false
   )
   public void applyBoneVisibilityByPart(EquipmentSlot currentSlot, ModelPart currentPart, HumanoidModel<?> model) {
      ((HumanoidModel<?>)(Object)this).setAllVisible(false);
      currentPart.visible = true;
      GeoBone bone = this.gbf$findBone(currentSlot, currentPart, model);
      if (bone == null) {
         this.gbf$boneRenderPlan.disable();
      } else {
         bone.setHidden(false);
         this.gbf$boneRenderPlan.selectOnly(bone);
      }
   }

   @InjectLite(
      method = {"doPostRenderCleanup"},
      at = {@At("HEAD")},
      remap = false
   )
   private void gbf$disableBoneRenderPlan() {
      this.gbf$boneRenderPlan.disable();
   }

   @Unique
   private void gbf$selectVisible(GeoBone bone, boolean visible) {
      this.setBoneVisible(bone, visible);
      if (visible) {
         this.gbf$boneRenderPlan.include(bone);
      }
   }

   @Unique
   private GeoBone gbf$findBone(EquipmentSlot slot, ModelPart part, HumanoidModel<?> model) {
      if (part == model.hat || part == model.head) {
         return this.head;
      } else if (part == model.body) {
         return this.body;
      } else if (part == model.leftArm) {
         return this.leftArm;
      } else if (part == model.rightArm) {
         return this.rightArm;
      } else if (part == model.leftLeg) {
         return slot == EquipmentSlot.FEET ? this.leftBoot : this.leftLeg;
      } else if (part == model.rightLeg) {
         return slot == EquipmentSlot.FEET ? this.rightBoot : this.rightLeg;
      } else {
         return null;
      }
   }
}

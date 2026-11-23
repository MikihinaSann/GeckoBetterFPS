package com.moepus.gbf;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class MixinPlugin implements IMixinConfigPlugin {
   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      Config config = ConfigParser.getConfig();

      return switch (mixinClassName) {
         case "com.moepus.gbf.mixin.MolangParserMixin" -> config.enableMolangCompiler;
         case "com.moepus.gbf.mixin.GeoArmorRendererMixin", "com.moepus.gbf.mixin.GeoRendererBonePlanMixin" -> config.enableBoneRenderPlan;
         default -> true;
      };
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}

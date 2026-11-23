package com.moepus.gbf.mixin;

import com.moepus.gbf.molang.compiler.MolangCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MathValue;

@Mixin(
   value = {MathParser.class},
   remap = false
)
public abstract class MolangParserMixin {
   @Inject(
      method = {"parseJson"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private static void gbf$compileParsedValue(CallbackInfoReturnable<MathValue> callback) {
      callback.setReturnValue(MolangCompiler.compile((MathValue)callback.getReturnValue()));
   }
}

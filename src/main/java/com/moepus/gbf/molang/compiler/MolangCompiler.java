package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.math.value.Constant;

public final class MolangCompiler {
   private MolangCompiler() {
   }

   public static MathValue compile(MathValue value) {
      try {
         CompiledAst converted = MathValueToAst.convert(value);
         AstNode optimized = AstOptimizer.optimize(converted.root());
         return (MathValue)(optimized instanceof ConstantAstNode constant
            ? new Constant(constant.value())
            : BytecodeGen.compile(new CompiledAst(optimized, converted.references())));
      } catch (LinkageError | Exception var4) {
         return value;
      }
   }
}

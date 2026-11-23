package com.moepus.gbf.molang.compiler;

import java.util.List;
import software.bernie.geckolib.loading.math.MathValue;

record CompiledAst(AstNode root, List<MathValue> references) {
}

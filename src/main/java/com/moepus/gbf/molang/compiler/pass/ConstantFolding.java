package com.moepus.gbf.molang.compiler.pass;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.DelegateAstNode;
import com.moepus.gbf.molang.compiler.node.TernaryAstNode;
import com.moepus.gbf.molang.compiler.node.UnaryAstNode;

public final class ConstantFolding {
   private ConstantFolding() {
   }

   public static AstNode optimize(AstNode node) {
      AstNode optimized = AstPassSupport.mapChildren(node, ConstantFolding::optimize);
      if (optimized instanceof BinaryAstNode binary && binary.left() instanceof ConstantAstNode && binary.right() instanceof ConstantAstNode) {
         return new ConstantAstNode(binary.eval());
      } else if (optimized instanceof BinaryAstNode binary) {
         return foldConstantMultiplier(binary);
      } else if (optimized instanceof UnaryAstNode unary && unary.operand() instanceof ConstantAstNode) {
         return new ConstantAstNode(unary.eval());
      } else if (optimized instanceof CachedAstNode cached && cached.operand() instanceof ConstantAstNode constant) {
         return constant;
      } else if (optimized instanceof DelegateAstNode delegate && delegate.isConstant()) {
         return new ConstantAstNode(delegate.eval());
      } else if (optimized instanceof TernaryAstNode ternary && ternary.condition() instanceof ConstantAstNode) {
         return ternary.condition().eval() != 0.0 ? ternary.ifTrue() : ternary.ifFalse();
      } else {
         return optimized;
      }
   }

   private static AstNode foldConstantMultiplier(BinaryAstNode binary) {
      if (binary.op() != BinaryAstNode.BinaryOp.MUL
         || !(
            binary.right() instanceof ConstantAstNode outerConstant && binary.left() instanceof BinaryAstNode inner && inner.op() == BinaryAstNode.BinaryOp.MUL
         )) {
         return binary;
      } else if (inner.right() instanceof ConstantAstNode innerConstant) {
         return multipliedBy(inner.left(), innerConstant.value() * outerConstant.value());
      } else {
         return inner.left() instanceof ConstantAstNode innerConstant ? multipliedBy(inner.right(), innerConstant.value() * outerConstant.value()) : binary;
      }
   }

   private static BinaryAstNode multipliedBy(AstNode operand, double multiplier) {
      return new BinaryAstNode(BinaryAstNode.BinaryOp.MUL, operand, new ConstantAstNode(multiplier));
   }
}

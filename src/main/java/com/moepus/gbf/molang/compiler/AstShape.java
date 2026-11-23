package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.DelegateAstNode;
import com.moepus.gbf.molang.compiler.node.ReferenceAstNode;
import com.moepus.gbf.molang.compiler.node.TernaryAstNode;
import com.moepus.gbf.molang.compiler.node.UnaryAstNode;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

sealed interface AstShape
   permits AstShape.Constant,
   AstShape.Reference,
   AstShape.Binary,
   AstShape.Unary,
   AstShape.Ternary,
   AstShape.Delegate,
   AstShape.CachedDefinition,
   AstShape.CachedReference {
   static AstShape of(AstNode root) {
      return new AstShape.Factory().shape(root);
   }

   public record Binary(BinaryAstNode.BinaryOp op, AstShape left, AstShape right) implements AstShape {
   }

   public record CachedDefinition(int id, AstShape operand) implements AstShape {
   }

   public record CachedReference(int id) implements AstShape {
   }

   public record Constant(long bits) implements AstShape {
   }

   public record Delegate(DelegateAstNode.DelegateOp op, List<AstShape> arguments) implements AstShape {
      public Delegate(DelegateAstNode.DelegateOp op, List<AstShape> arguments) {
         arguments = List.copyOf(arguments);
         this.op = op;
         this.arguments = arguments;
      }
   }

   public static final class Factory {
      private final Map<CachedAstNode, Integer> cachedNodes = new IdentityHashMap<>();

      private AstShape shape(AstNode node) {
         if (node instanceof ConstantAstNode constant) {
            return new AstShape.Constant(Double.doubleToLongBits(constant.value()));
         } else if (node instanceof ReferenceAstNode reference) {
            return new AstShape.Reference(reference.referenceIndex());
         } else if (node instanceof BinaryAstNode binary) {
            return new AstShape.Binary(binary.op(), this.shape(binary.left()), this.shape(binary.right()));
         } else if (node instanceof UnaryAstNode unary) {
            return new AstShape.Unary(unary.op(), this.shape(unary.operand()));
         } else if (node instanceof TernaryAstNode ternary) {
            return new AstShape.Ternary(this.shape(ternary.condition()), this.shape(ternary.ifTrue()), this.shape(ternary.ifFalse()));
         } else if (node instanceof DelegateAstNode delegate) {
            return new AstShape.Delegate(delegate.op(), Arrays.stream(delegate.args()).map(this::shape).toList());
         } else if (node instanceof CachedAstNode cached) {
            return this.cachedShape(cached);
         } else {
            throw new IllegalArgumentException("Unknown AST node: " + node.getClass().getName());
         }
      }

      private AstShape cachedShape(CachedAstNode node) {
         Integer existing = this.cachedNodes.get(node);
         if (existing != null) {
            return new AstShape.CachedReference(existing);
         } else {
            int id = this.cachedNodes.size();
            this.cachedNodes.put(node, id);
            return new AstShape.CachedDefinition(id, this.shape(node.operand()));
         }
      }
   }

   public record Reference(int index) implements AstShape {
   }

   public record Ternary(AstShape condition, AstShape ifTrue, AstShape ifFalse) implements AstShape {
   }

   public record Unary(UnaryAstNode.UnaryOp op, AstShape operand) implements AstShape {
   }
}

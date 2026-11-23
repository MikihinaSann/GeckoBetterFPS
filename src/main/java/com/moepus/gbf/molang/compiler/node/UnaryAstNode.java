package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public record UnaryAstNode(UnaryAstNode.UnaryOp op, AstNode operand) implements AstNode {
   @Override
   public double eval() {
      return this.op.eval(this.operand.eval());
   }

   @Override
   public AstNode[] children() {
      return new AstNode[]{this.operand};
   }

   @Override
   public void emit(BytecodeGen.Context context, InstructionAdapter method) {
      this.operand.emit(context, method);
      this.op.emit(method);
   }

   public UnaryAstNode withOperand(AstNode operand) {
      return operand == this.operand ? this : new UnaryAstNode(this.op, operand);
   }

   public static enum UnaryOp {
      NEGATE {
         @Override
         public double eval(double value) {
            return -value;
         }

         @Override
         void emit(InstructionAdapter method) {
            method.neg(Type.DOUBLE_TYPE);
         }
      },
      BOOLEAN_NEGATE {
         @Override
         public double eval(double value) {
            return value == 0.0 ? 1.0 : 0.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            Label trueLabel = new Label();
            Label end = new Label();
            method.dconst(0.0);
            method.cmpl(Type.DOUBLE_TYPE);
            method.visitJumpInsn(153, trueLabel);
            method.dconst(0.0);
            method.goTo(end);
            method.visitLabel(trueLabel);
            method.dconst(1.0);
            method.visitLabel(end);
         }
      };

      public abstract double eval(double var1);

      abstract void emit(InstructionAdapter var1);
   }
}

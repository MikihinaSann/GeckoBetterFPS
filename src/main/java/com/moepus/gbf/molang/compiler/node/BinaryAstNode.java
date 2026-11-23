package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public record BinaryAstNode(BinaryAstNode.BinaryOp op, AstNode left, AstNode right) implements AstNode {
   private static final double EQUALITY_EPSILON = 1.0E-5;

   @Override
   public double eval() {
      return this.op.eval(this.left.eval(), this.right.eval());
   }

   @Override
   public AstNode[] children() {
      return new AstNode[]{this.left, this.right};
   }

   @Override
   public void emit(BytecodeGen.Context context, InstructionAdapter method) {
      this.left.emit(context, method);
      this.right.emit(context, method);
      this.op.emit(method);
   }

   public BinaryAstNode withChildren(AstNode left, AstNode right) {
      return left == this.left && right == this.right ? this : new BinaryAstNode(this.op, left, right);
   }

   public static double divide(double a, double b) {
      return a / (b == 0.0 ? 1.0 : b);
   }

   public static double logicalAnd(double a, double b) {
      return a != 0.0 && b != 0.0 ? 1.0 : 0.0;
   }

   public static double modulus(double a, double b) {
      return b == 0.0 ? a : a % b;
   }

   public static double logicalOr(double a, double b) {
      return a == 0.0 && b == 0.0 ? 0.0 : 1.0;
   }

   private static boolean approximatelyEqual(double a, double b) {
      return Math.abs(a - b) < 1.0E-5;
   }

   public static enum BinaryOp {
      ADD {
         @Override
         public double eval(double a, double b) {
            return a + b;
         }

         @Override
         void emit(InstructionAdapter method) {
            method.add(Type.DOUBLE_TYPE);
         }
      },
      SUB {
         @Override
         public double eval(double a, double b) {
            return a - b;
         }

         @Override
         void emit(InstructionAdapter method) {
            method.sub(Type.DOUBLE_TYPE);
         }
      },
      MUL {
         @Override
         public double eval(double a, double b) {
            return a * b;
         }

         @Override
         void emit(InstructionAdapter method) {
            method.mul(Type.DOUBLE_TYPE);
         }
      },
      DIV {
         @Override
         public double eval(double a, double b) {
            return BinaryAstNode.divide(a, b);
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitBinaryHelper(method, "divide");
         }
      },
      MOD {
         @Override
         public double eval(double a, double b) {
            return BinaryAstNode.modulus(a, b);
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitBinaryHelper(method, "modulus");
         }
      },
      POW {
         @Override
         public double eval(double a, double b) {
            return Math.pow(a, b);
         }

         @Override
         void emit(InstructionAdapter method) {
            method.invokestatic(Type.getInternalName(Math.class), "pow", BinaryAstNode.BinaryOp.binaryDoubleDescriptor(), false);
         }
      },
      AND {
         @Override
         public double eval(double a, double b) {
            return BinaryAstNode.logicalAnd(a, b);
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitBinaryHelper(method, "logicalAnd");
         }
      },
      OR {
         @Override
         public double eval(double a, double b) {
            return BinaryAstNode.logicalOr(a, b);
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitBinaryHelper(method, "logicalOr");
         }
      },
      LT {
         @Override
         public double eval(double a, double b) {
            return a < b ? 1.0 : 0.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitComparison(method, 155, true);
         }
      },
      LTE {
         @Override
         public double eval(double a, double b) {
            return a <= b ? 1.0 : 0.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitComparison(method, 158, true);
         }
      },
      GT {
         @Override
         public double eval(double a, double b) {
            return a > b ? 1.0 : 0.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitComparison(method, 157, false);
         }
      },
      GTE {
         @Override
         public double eval(double a, double b) {
            return a >= b ? 1.0 : 0.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitComparison(method, 156, false);
         }
      },
      EQ {
         @Override
         public double eval(double a, double b) {
            return BinaryAstNode.approximatelyEqual(a, b) ? 1.0 : 0.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitEqualityOperands(method);
            BinaryAstNode.BinaryOp.emitComparison(method, 155, true);
         }
      },
      NE {
         @Override
         public double eval(double a, double b) {
            return BinaryAstNode.approximatelyEqual(a, b) ? 0.0 : 1.0;
         }

         @Override
         void emit(InstructionAdapter method) {
            BinaryAstNode.BinaryOp.emitEqualityOperands(method);
            BinaryAstNode.BinaryOp.emitComparison(method, 156, true);
         }
      };

      public abstract double eval(double var1, double var3);

      abstract void emit(InstructionAdapter var1);

      private static void emitBinaryHelper(InstructionAdapter method, String name) {
         method.invokestatic(Type.getInternalName(BinaryAstNode.class), name, binaryDoubleDescriptor(), false);
      }

      private static String binaryDoubleDescriptor() {
         return Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE});
      }

      private static void emitEqualityOperands(InstructionAdapter method) {
         method.sub(Type.DOUBLE_TYPE);
         method.invokestatic(Type.getInternalName(Math.class), "abs", Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE}), false);
         method.dconst(1.0E-5);
      }

      private static void emitComparison(InstructionAdapter method, int opcode, boolean nanIsGreater) {
         Label trueLabel = new Label();
         Label end = new Label();
         if (nanIsGreater) {
            method.cmpg(Type.DOUBLE_TYPE);
         } else {
            method.cmpl(Type.DOUBLE_TYPE);
         }

         method.visitJumpInsn(opcode, trueLabel);
         method.dconst(0.0);
         method.goTo(end);
         method.visitLabel(trueLabel);
         method.dconst(1.0);
         method.visitLabel(end);
      }
   }
}

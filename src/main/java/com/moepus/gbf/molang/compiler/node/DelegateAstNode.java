package com.moepus.gbf.molang.compiler.node;

import com.moepus.gbf.molang.compiler.AstNode;
import com.moepus.gbf.molang.compiler.BytecodeGen;
import java.util.Arrays;
import com.moepus.gbf.molang.compiler.MolangMath;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public record DelegateAstNode(DelegateAstNode.DelegateOp op, AstNode[] args) implements AstNode {
   @Override
   public double eval() {
      double[] values = Arrays.stream(this.args).mapToDouble(AstNode::eval).toArray();
      return this.op.eval(values);
   }

   public boolean isConstant() {
      for (AstNode arg : this.args) {
         if (!(arg instanceof ConstantAstNode)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public AstNode[] children() {
      return this.args;
   }

   @Override
   public void emit(BytecodeGen.Context context, InstructionAdapter method) {
      this.op.emit(context, method, this.args);
   }

   public DelegateAstNode withArgs(AstNode[] args) {
      return Arrays.equals((Object[])this.args, (Object[])args) ? this : new DelegateAstNode(this.op, args);
   }

   public static enum DelegateOp {
      SIN("sin", 1) {
         @Override
         double eval(double[] args) {
            return Math.sin(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            DelegateAstNode.DelegateOp.emitMath1(context, method, args, "sin");
         }
      },
      COS("cos", 1) {
         @Override
         double eval(double[] args) {
            return Math.cos(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            DelegateAstNode.DelegateOp.emitMath1(context, method, args, "cos");
         }
      },
      ABS("abs", 1) {
         @Override
         double eval(double[] args) {
            return Math.abs(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            DelegateAstNode.DelegateOp.emitMath1(context, method, args, "abs");
         }
      },
      MIN("min", 2) {
         @Override
         double eval(double[] args) {
            return Math.min(args[0], args[1]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            args[0].emit(context, method);
            args[1].emit(context, method);
            method.invokestatic(
               Type.getInternalName(Math.class), "min", Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}), false
            );
         }
      },
      MAX("max", 2) {
         @Override
         double eval(double[] args) {
            return Math.max(args[0], args[1]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            args[0].emit(context, method);
            args[1].emit(context, method);
            method.invokestatic(
               Type.getInternalName(Math.class), "max", Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}), false
            );
         }
      },
      CLAMP("clamp", 3) {
         @Override
         double eval(double[] args) {
            return MolangMath.clamp(args[0], args[1], args[2]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            args[0].emit(context, method);
            args[1].emit(context, method);
            args[2].emit(context, method);
            method.invokestatic(
               Type.getInternalName(MolangMath.class),
               "clamp",
               Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}),
               false
            );
         }
      },
      LERP("lerp", 3) {
         @Override
         double eval(double[] args) {
            return MolangMath.lerp(args[2], args[0], args[1]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            args[2].emit(context, method);
            args[0].emit(context, method);
            args[1].emit(context, method);
            method.invokestatic(
               Type.getInternalName(MolangMath.class),
               "lerp",
               Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}),
               false
            );
         }
      },
      FLOOR("floor", 1) {
         @Override
         double eval(double[] args) {
            return Math.floor(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            DelegateAstNode.DelegateOp.emitMath1(context, method, args, "floor");
         }
      },
      CEIL("ceil", 1) {
         @Override
         double eval(double[] args) {
            return Math.ceil(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            DelegateAstNode.DelegateOp.emitMath1(context, method, args, "ceil");
         }
      },
      ROUND("round", 1) {
         @Override
         double eval(double[] args) {
            return Math.round(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            args[0].emit(context, method);
            method.invokestatic(Type.getInternalName(Math.class), "round", Type.getMethodDescriptor(Type.LONG_TYPE, new Type[]{Type.DOUBLE_TYPE}), false);
            method.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
         }
      },
      POW("pow", 2) {
         @Override
         double eval(double[] args) {
            return Math.pow(args[0], args[1]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            args[0].emit(context, method);
            args[1].emit(context, method);
            method.invokestatic(
               Type.getInternalName(Math.class), "pow", Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}), false
            );
         }
      },
      SQRT("sqrt", 1) {
         @Override
         double eval(double[] args) {
            return Math.sqrt(args[0]);
         }

         @Override
         void emit(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args) {
            DelegateAstNode.DelegateOp.emitMath1(context, method, args, "sqrt");
         }
      };

      private final String name;
      private final int minArgs;

      private DelegateOp(String name, int minArgs) {
         this.name = name;
         this.minArgs = minArgs;
      }

      public static DelegateAstNode.DelegateOp byName(String name) {
         String normalized = name.startsWith("math.") ? name.substring("math.".length()) : name;

         for (DelegateAstNode.DelegateOp op : values()) {
            if (op.name.equals(normalized)) {
               return op;
            }
         }

         return null;
      }

      public int minArgs() {
         return this.minArgs;
      }

      abstract double eval(double[] var1);

      abstract void emit(BytecodeGen.Context var1, InstructionAdapter var2, AstNode[] var3);

      private static void emitMath1(BytecodeGen.Context context, InstructionAdapter method, AstNode[] args, String methodName) {
         args[0].emit(context, method);
         method.invokestatic(Type.getInternalName(Math.class), methodName, Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[]{Type.DOUBLE_TYPE}), false);
      }
   }
}

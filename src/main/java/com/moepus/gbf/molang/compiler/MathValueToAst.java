package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.DelegateAstNode;
import com.moepus.gbf.molang.compiler.node.ReferenceAstNode;
import com.moepus.gbf.molang.compiler.node.TernaryAstNode;
import com.moepus.gbf.molang.compiler.node.UnaryAstNode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.math.function.MathFunction;
import software.bernie.geckolib.loading.math.value.BooleanNegate;
import software.bernie.geckolib.loading.math.value.Calculation;
import software.bernie.geckolib.loading.math.value.Constant;
import software.bernie.geckolib.loading.math.value.Group;
import software.bernie.geckolib.loading.math.value.Negative;
import software.bernie.geckolib.loading.math.value.Ternary;
import software.bernie.geckolib.loading.math.value.Variable;

final class MathValueToAst {
   private static final Map<String, BinaryAstNode.BinaryOp> BINARY_OPERATIONS = Map.ofEntries(
      Map.entry("+", BinaryAstNode.BinaryOp.ADD),
      Map.entry("-", BinaryAstNode.BinaryOp.SUB),
      Map.entry("*", BinaryAstNode.BinaryOp.MUL),
      Map.entry("/", BinaryAstNode.BinaryOp.DIV),
      Map.entry("%", BinaryAstNode.BinaryOp.MOD),
      Map.entry("^", BinaryAstNode.BinaryOp.POW),
      Map.entry("&&", BinaryAstNode.BinaryOp.AND),
      Map.entry("||", BinaryAstNode.BinaryOp.OR),
      Map.entry("<", BinaryAstNode.BinaryOp.LT),
      Map.entry("<=", BinaryAstNode.BinaryOp.LTE),
      Map.entry(">", BinaryAstNode.BinaryOp.GT),
      Map.entry(">=", BinaryAstNode.BinaryOp.GTE),
      Map.entry("==", BinaryAstNode.BinaryOp.EQ),
      Map.entry("!=", BinaryAstNode.BinaryOp.NE)
   );
   private final Map<MathValue, AstNode> nodes = new IdentityHashMap<>();
   private final Map<MathValue, Integer> referenceIndexes = new IdentityHashMap<>();
   private final List<MathValue> references = new ArrayList<>();

   static CompiledAst convert(MathValue value) {
      MathValueToAst converter = new MathValueToAst();
      return new CompiledAst(converter.nodeFor(value), List.copyOf(converter.references));
   }

   private AstNode nodeFor(MathValue value) {
      return this.nodes.computeIfAbsent(value, this::createNode);
   }

   private AstNode createNode(MathValue value) {
      if (value instanceof Constant constant) {
         return new ConstantAstNode(constant.value());
      } else if (value instanceof Variable) {
         return new ReferenceAstNode(this.reference(value));
      } else if (value instanceof Calculation calculation) {
         return this.calculationNode(calculation);
      } else if (value instanceof Negative negative) {
         return new UnaryAstNode(UnaryAstNode.UnaryOp.NEGATE, this.nodeFor(negative.value()));
      } else if (value instanceof BooleanNegate negate) {
         return new UnaryAstNode(UnaryAstNode.UnaryOp.BOOLEAN_NEGATE, this.nodeFor(negate.value()));
      } else if (value instanceof Ternary ternary) {
         return new TernaryAstNode(this.nodeFor(ternary.condition()), this.nodeFor(ternary.trueValue()), this.nodeFor(ternary.falseValue()));
      } else if (value instanceof Group group) {
         return this.nodeFor(group.contents());
      } else if (value instanceof MathFunction function && !function.isMutable()) {
         return new ConstantAstNode(function.get());
      } else {
         return (AstNode)(value instanceof MathFunction function ? this.functionNode(value, function) : new ReferenceAstNode(this.reference(value)));
      }
   }

   private AstNode calculationNode(Calculation calculation) {
      BinaryAstNode.BinaryOp operation = BINARY_OPERATIONS.get(calculation.operator().symbol());
      return (AstNode)(operation == null
         ? new ReferenceAstNode(this.reference(calculation))
         : new BinaryAstNode(operation, this.nodeFor(calculation.argA()), this.nodeFor(calculation.argB())));
   }

   private AstNode functionNode(MathValue value, MathFunction function) {
      DelegateAstNode.DelegateOp operation = DelegateAstNode.DelegateOp.byName(function.getName());
      MathValue[] arguments = function.getArgs();
      if (operation != null && arguments.length >= operation.minArgs()) {
         AstNode[] compiledArguments = new AstNode[operation.minArgs()];

         for (int i = 0; i < compiledArguments.length; i++) {
            compiledArguments[i] = this.nodeFor(arguments[i]);
         }

         if (operation == DelegateAstNode.DelegateOp.SIN || operation == DelegateAstNode.DelegateOp.COS) {
            compiledArguments[0] = radiansNode(compiledArguments[0]);
         }

         return new DelegateAstNode(operation, compiledArguments);
      } else {
         return new ReferenceAstNode(this.reference(value));
      }
   }

   private static AstNode radiansNode(AstNode degrees) {
      return new BinaryAstNode(BinaryAstNode.BinaryOp.MUL, degrees, new ConstantAstNode((float) (Math.PI / 180.0)));
   }

   private int reference(MathValue value) {
      return this.referenceIndexes.computeIfAbsent(value, ignored -> {
         this.references.add(value);
         return this.references.size() - 1;
      });
   }
}

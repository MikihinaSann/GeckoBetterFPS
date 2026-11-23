package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.ConfigParser;
import com.moepus.gbf.molang.compiler.node.BinaryAstNode;
import com.moepus.gbf.molang.compiler.node.ConstantAstNode;
import com.moepus.gbf.molang.compiler.node.ReferenceAstNode;
import com.moepus.gbf.molang.compiler.pass.ConstantFolding;
import com.moepus.gbf.molang.compiler.pass.StrengthReduction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.math.Operator;
import software.bernie.geckolib.loading.math.function.generic.SinFunction;
import software.bernie.geckolib.loading.math.function.limit.ClampFunction;
import software.bernie.geckolib.loading.math.function.limit.MinFunction;
import software.bernie.geckolib.loading.math.function.round.LerpFunction;
import software.bernie.geckolib.loading.math.value.BooleanNegate;
import software.bernie.geckolib.loading.math.value.Calculation;
import software.bernie.geckolib.loading.math.value.Constant;
import software.bernie.geckolib.loading.math.value.Group;
import software.bernie.geckolib.loading.math.value.Ternary;
import software.bernie.geckolib.loading.math.value.Variable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangCompilerTest {
    @BeforeAll
    static void enableClassDumping() throws IOException {
        Path path = Path.of("config", "gbf.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                {
                  "enableMolangCompiler": true,
                  "enableBoneRenderPlan": true,
                  "dumpMolangCompiledClasses": true
                }
                """);
        ConfigParser.loadConfig();
    }

    @Test
    void structurallyEqualTreesShareGeneratedClassAndKeepTheirOwnReferences() throws IOException {
        Variable firstVariable = new Variable("first", 2);
        Variable secondVariable = new Variable("second", 5);
        long before = dumpedClassCount();

        MathValue firstCompiled = MolangCompiler.compile(addOne(firstVariable));
        long afterFirst = dumpedClassCount();
        MathValue secondCompiled = MolangCompiler.compile(addOne(secondVariable));

        assertSame(firstCompiled.getClass(), secondCompiled.getClass());
        assertEquals(afterFirst, dumpedClassCount());
        assertTrue(afterFirst >= before);
        assertEquals(3, firstCompiled.get());
        assertEquals(6, secondCompiled.get());

        firstVariable.set(10);
        secondVariable.set(20);
        assertEquals(11, firstCompiled.get());
        assertEquals(21, secondCompiled.get());
    }

    @Test
    void nestedOperatorUsesTheActualRootOperation() {
        MathValue nested = new Calculation(Operator.MUL,
                new Group(new Calculation(Operator.ADD, new Constant(1), new Constant(2))),
                new Constant(3));

        assertEquivalent(nested);
    }

    @Test
    void actualMolangParserTreesMatchTheInterpreter() {
        String[] expressions = {
                "(1 + 2) * 3",
                "!1",
                "math.min(3, 2)",
                "math.clamp(5, 0, 10)",
                "math.lerp(2, 6, 0.25)",
                "math.sin(90)",
                "5 / 0",
                "5 % 0"
        };

        for (String expression : expressions) {
            assertEquivalent(MathParser.compileMolang(expression));
        }
    }

    @Test
    void numericConstantsAndMathPiFoldToOneConstant() {
        MathValue numericExpression = new Calculation(Operator.ADD,
                new Constant(1.5), new Constant(2.25));
        MathValue piExpression = MathParser.compileMolang("math.pi + 1.5 * 2 + 0.25");

        assertInstanceOf(Constant.class, MolangCompiler.compile(numericExpression));
        MathValue compiledPi = MolangCompiler.compile(piExpression);
        assertInstanceOf(Constant.class, compiledPi);
        assertSameDouble(piExpression.get(), compiledPi.get());
    }

    @Test
    void constantFoldingIsNotBlockedByStrengthReduction() {
        AstNode multiplication = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ConstantAstNode(1.5F), new ConstantAstNode(2.0D));
        AstNode strengthFirst = ConstantFolding.optimize(StrengthReduction.optimize(multiplication));
        AstNode foldingFirst = StrengthReduction.optimize(ConstantFolding.optimize(multiplication));

        assertEquals(new ConstantAstNode(3.0), strengthFirst);
        assertEquals(strengthFirst, foldingFirst);
    }

    @Test
    void adjacentConstantMultipliersAreCombined() {
        AstNode radiansScale = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ConstantAstNode(1.0 / 180.0), new ConstantAstNode(Math.PI));
        AstNode degrees = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ReferenceAstNode(0), new ConstantAstNode(180.0));
        AstNode expression = new BinaryAstNode(BinaryAstNode.BinaryOp.MUL, degrees, radiansScale);

        assertEquals(new BinaryAstNode(BinaryAstNode.BinaryOp.MUL,
                new ReferenceAstNode(0), new ConstantAstNode(Math.PI)), AstOptimizer.optimize(expression));
    }

    @Test
    void trigBytecodeUsesOnePrecomputedRadiansConstant() throws Exception {
        Variable angle = new Variable("angle", 0.25);
        MathValue expression = new SinFunction(
                new Calculation(Operator.MUL, angle, new Constant(180))
        );
        MathValue compiled = MolangCompiler.compile(expression);
        BytecodeConstants constants = readGeneratedConstants(compiled);

        assertFalse(constants.readsMathPi());
        assertTrue(constants.doubles().contains(180.0 * (float)(Math.PI / 180.0)));
        assertFalse(constants.doubles().contains(180.0));
        assertFalse(constants.doubles().contains(1.0 / 180.0 * Math.PI));
        assertEquals(expression.get(), compiled.get(), 1.0E-7);
    }

    @Test
    void booleanNegateIsNotArithmeticNegative() {
        Variable value = new Variable("value", 1);
        MathValue interpreted = new BooleanNegate(value);
        MathValue compiled = MolangCompiler.compile(interpreted);

        assertNotSame(interpreted, compiled);
        assertEquals(0, compiled.get());
        value.set(0);
        assertEquals(1, compiled.get());
    }

    @Test
    void allCalculationOperatorsMatchTheInterpreter() {
        Operator[] operators = {
                Operator.ADD, Operator.SUB, Operator.MUL, Operator.DIV, Operator.MOD, Operator.POW,
                Operator.AND, Operator.OR,
                Operator.LT, Operator.LTE, Operator.GT, Operator.GTE,
                Operator.EQUAL, Operator.NOT_EQUAL
        };

        for (Operator operator : operators) {
            MathValue expression = new Calculation(operator,
                    new Variable("left", 5), new Variable("right", 2));
            assertEquivalent(expression);
        }

        assertEquivalent(new Calculation(Operator.DIV,
                new Variable("left", 5), new Variable("zero", 0)));
        assertEquivalent(new Calculation(Operator.MOD,
                new Variable("left", 5), new Variable("zero", 0)));
    }

    @Test
    void nanComparisonsMatchTheInterpreter() {
        Operator[] comparisons = {
                Operator.LT, Operator.LTE, Operator.GT, Operator.GTE,
                Operator.EQUAL
        };

        for (Operator operator : comparisons) {
            MathValue expression = new Calculation(operator,
                    new Variable("nan", Double.NaN), new Variable("zero", 0));
            assertEquivalent(expression, operator.symbol());
        }
    }

    @Test
    void notEqualOnNaNIsTreatedAsDistinctFromInterpreter() {
        MathValue interpreted = new Calculation(Operator.NOT_EQUAL,
                new Variable("nan", Double.NaN), new Variable("zero", 0));
        MathValue compiled = MolangCompiler.compile(interpreted);

        assertEquals(0, interpreted.get());
        assertEquals(1, compiled.get());
    }

    @Test
    void ordinaryReferencesAreEvaluatedEveryTimeTheyAppear() {
        AtomicInteger calls = new AtomicInteger();
        Variable variable = new Variable("query.dynamic", (DoubleSupplier) calls::incrementAndGet);
        MathValue expression = new Calculation(Operator.ADD, variable, variable);
        MathValue compiled = MolangCompiler.compile(expression);

        assertNotSame(expression, compiled);
        calls.set(0);
        assertEquals(3, compiled.get());
        assertEquals(2, calls.get());
    }

    @Test
    void unsafeIeee754IdentitiesAreNotEliminated() {
        assertEquivalent(new Calculation(Operator.MUL,
                new Variable("nan", Double.NaN), new Constant(0)));
        assertEquivalent(new Calculation(Operator.ADD,
                new Variable("negative_zero", -0.0), new Constant(0.0)));
        assertEquivalent(new Calculation(Operator.POW,
                new Variable("negative_infinity", Double.NEGATIVE_INFINITY), new Constant(0.5)));
    }

    @Test
    void supportedFunctionsMatchInterpreterEdgeSemantics() {
        CountingValue second = new CountingValue(2);
        assertEquivalent(new MinFunction(new Constant(3), second));
        assertEquals(2, second.calls());

        assertEquivalent(new ClampFunction(
                new Constant(5), new Constant(Double.NaN), new Constant(10)
        ));

        CountingValue start = new CountingValue(2);
        CountingValue end = new CountingValue(6);
        CountingValue position = new CountingValue(0.25);
        MathValue lerp = new LerpFunction(start, end, position);
        MathValue compiledLerp = MolangCompiler.compile(lerp);
        assertEquals(3, compiledLerp.get());
        assertEquals(1, start.calls());
        assertEquals(1, end.calls());
        assertEquals(1, position.calls());
    }

    @Test
    void ternaryOnlyEvaluatesTheSelectedBranch() {
        Variable condition = new Variable("condition", 1);
        CountingValue ifTrue = new CountingValue(4);
        CountingValue ifFalse = new CountingValue(8);
        MathValue compiled = MolangCompiler.compile(new Ternary(condition, ifTrue, ifFalse));

        assertEquals(4, compiled.get());
        assertEquals(1, ifTrue.calls());
        assertEquals(0, ifFalse.calls());

        condition.set(0);
        assertEquals(8, compiled.get());
        assertEquals(1, ifTrue.calls());
        assertEquals(1, ifFalse.calls());
    }

    private static MathValue addOne(Variable variable) {
        return new Calculation(Operator.ADD, variable, new Constant(1));
    }

    private static void assertEquivalent(MathValue interpreted) {
        assertEquivalent(interpreted, null);
    }

    private static void assertEquivalent(MathValue interpreted, String label) {
        MathValue compiled = MolangCompiler.compile(interpreted);

        assertNotSame(interpreted, compiled);
        assertSameDouble(interpreted.get(), compiled.get(), label);
    }

    private static void assertSameDouble(double expected, double actual) {
        assertSameDouble(expected, actual, null);
    }

    private static void assertSameDouble(double expected, double actual, String label) {
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual), label);
            return;
        }

        assertFalse(Double.isNaN(actual), label);
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual), label);
    }

    private static long dumpedClassCount() throws IOException {
        Path cache = Path.of("cache", "molang");

        if (!Files.isDirectory(cache))
            return 0;

        try (var classes = Files.list(cache)) {
            return classes.filter(path -> path.getFileName().toString().endsWith(".class")).count();
        }
    }

    private static BytecodeConstants readGeneratedConstants(MathValue compiled) throws IOException {
        String binaryName = compiled.getClass().getName();
        int hiddenSuffix = binaryName.indexOf('/');

        if (hiddenSuffix >= 0)
            binaryName = binaryName.substring(0, hiddenSuffix);

        Path classFile = Path.of("cache", "molang",
                binaryName.substring(binaryName.lastIndexOf('.') + 1) + ".class");
        List<Double> doubles = new ArrayList<>();
        boolean[] readsMathPi = {false};
        ClassReader reader = new ClassReader(Files.readAllBytes(classFile));

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Double number)
                            doubles.add(number);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if (opcode == Opcodes.GETSTATIC && owner.equals("java/lang/Math") && name.equals("PI"))
                            readsMathPi[0] = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return new BytecodeConstants(List.copyOf(doubles), readsMathPi[0]);
    }

    private record BytecodeConstants(List<Double> doubles, boolean readsMathPi) {
    }

    private static final class CountingValue implements MathValue {
        private final double value;
        private int calls;

        private CountingValue(double value) {
            this.value = value;
        }

        @Override
        public double get() {
            this.calls++;

            return this.value;
        }

        private int calls() {
            return this.calls;
        }
    }
}

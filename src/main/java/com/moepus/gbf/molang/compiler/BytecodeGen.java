package com.moepus.gbf.molang.compiler;

import com.moepus.gbf.ConfigParser;
import com.moepus.gbf.molang.compiler.node.CachedAstNode;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import software.bernie.geckolib.loading.math.MathValue;

public final class BytecodeGen {
   private static final Path MOLANG_CACHE_DIR = Path.of("cache", "molang");
   private static final String GENERATED_CLASS_PREFIX = BytecodeGen.class.getPackageName().replace('.', '/') + "/MolangCompiled_";
   private static final ConcurrentMap<AstShape, BytecodeGen.CompiledFactory> COMPILED_CLASSES = new ConcurrentHashMap<>();
   private static final AtomicLong ORDINAL = new AtomicLong();
   private static final boolean DUMP_CLASSES = ConfigParser.getConfig().dumpMolangCompiledClasses;

   private BytecodeGen() {
   }

   static MathValue compile(CompiledAst ast) throws Exception {
      BytecodeGen.CompiledFactory factory = factoryFor(ast.root());
      return factory.bind(ast.references());
   }

   private static BytecodeGen.CompiledFactory factoryFor(AstNode root) throws Exception {
      AstShape shape = AstShape.of(root);

      try {
         return COMPILED_CLASSES.computeIfAbsent(shape, ignored -> generateUnchecked(root));
      } catch (BytecodeGen.CompilationFailure var3) {
         throw var3.compilationCause;
      }
   }

   private static BytecodeGen.CompiledFactory generateUnchecked(AstNode root) {
      try {
         return generateFactory(root);
      } catch (Exception var2) {
         throw new BytecodeGen.CompilationFailure(var2);
      }
   }

   private static BytecodeGen.CompiledFactory generateFactory(AstNode root) throws Exception {
      String className = GENERATED_CLASS_PREFIX + ORDINAL.getAndIncrement();
      BytecodeGen.Context context = new BytecodeGen.Context(className, Type.getType(MathValue.class));
      ClassWriter writer = new ClassWriter(3);
      writer.visit(65, 17, className, null, Type.getInternalName(Object.class), new String[]{Type.getInternalName(MathValue.class)});
      writer.visitField(18, "references", Type.getDescriptor(Object[].class), null, null).visitEnd();
      generateConstructor(writer, context);
      generateGet(writer, context, root);
      writer.visitEnd();
      byte[] bytes = writer.toByteArray();
      if (DUMP_CLASSES) {
         dumpClass(className, bytes);
      }

      Class<?> generatedClass = MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass();
      Constructor<?> constructor = generatedClass.getConstructor(Object[].class);
      return references -> (MathValue)constructor.newInstance((Object)references.toArray());
   }

   private static void dumpClass(String className, byte[] bytes) {
      try {
         Files.createDirectories(MOLANG_CACHE_DIR);
         Files.write(MOLANG_CACHE_DIR.resolve(className.substring(className.lastIndexOf(47) + 1) + ".class"), bytes);
      } catch (IOException var3) {
      }
   }

   private static void clearCacheDir() {
      if (Files.exists(MOLANG_CACHE_DIR)) {
         try (Stream<Path> paths = Files.walk(MOLANG_CACHE_DIR)) {
            paths.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(MOLANG_CACHE_DIR)).forEach(BytecodeGen::deleteCacheEntry);
         } catch (IOException var5) {
         }
      }
   }

   private static void deleteCacheEntry(Path path) {
      try {
         Files.deleteIfExists(path);
      } catch (IOException var2) {
      }
   }

   private static void generateConstructor(ClassWriter writer, BytecodeGen.Context context) {
      InstructionAdapter method = new InstructionAdapter(
         writer.visitMethod(1, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.getType(Object[].class)}), null, null)
      );
      Label start = new Label();
      Label end = new Label();
      method.visitCode();
      method.visitLabel(start);
      method.load(0, InstructionAdapter.OBJECT_TYPE);
      method.invokespecial(Type.getInternalName(Object.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, new Type[0]), false);
      method.load(0, InstructionAdapter.OBJECT_TYPE);
      method.load(1, InstructionAdapter.OBJECT_TYPE);
      method.putfield(context.className(), "references", Type.getDescriptor(Object[].class));
      method.areturn(Type.VOID_TYPE);
      method.visitLabel(end);
      method.visitLocalVariable("this", "L" + context.className() + ";", null, start, end, 0);
      method.visitLocalVariable("references", Type.getDescriptor(Object[].class), null, start, end, 1);
      method.visitMaxs(0, 0);
      method.visitEnd();
   }

   private static void generateGet(ClassWriter writer, BytecodeGen.Context context, AstNode root) {
      InstructionAdapter method = new InstructionAdapter(writer.visitMethod(17, "get", Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[0]), null, null));
      Label start = new Label();
      Label end = new Label();
      context.resetLocals(1);
      method.visitCode();
      method.visitLabel(start);
      root.emitReturn(context, method);
      method.visitLabel(end);
      method.visitLocalVariable("this", "L" + context.className() + ";", null, start, end, 0);
      method.visitMaxs(0, 0);
      method.visitEnd();
   }

   static {
      if (DUMP_CLASSES) {
         clearCacheDir();
      }
   }

   private static final class CompilationFailure extends RuntimeException {
      private final Exception compilationCause;

      private CompilationFailure(Exception cause) {
         super(cause);
         this.compilationCause = cause;
      }
   }

   @FunctionalInterface
   private interface CompiledFactory {
      MathValue bind(List<MathValue> var1) throws Exception;
   }

   public static final class Context {
      private final String className;
      private final Type mathValueType;
      private final Map<CachedAstNode, Integer> cachedLocals = new IdentityHashMap<>();
      private int nextLocal = 1;

      private Context(String className, Type mathValueType) {
         this.className = className;
         this.mathValueType = mathValueType;
      }

      public String className() {
         return this.className;
      }

      public BytecodeGen.Context childScope() {
         BytecodeGen.Context child = new BytecodeGen.Context(this.className, this.mathValueType);
         child.nextLocal = this.nextLocal;
         return child;
      }

      public void mergeLocals(BytecodeGen.Context child) {
         this.nextLocal = Math.max(this.nextLocal, child.nextLocal);
      }

      public void emitReference(int referenceIndex, InstructionAdapter method) {
         this.emitReferenceLoad(referenceIndex, method);
      }

      public int cachedLocal(CachedAstNode node, InstructionAdapter method) {
         Integer existing = this.cachedLocals.get(node);
         if (existing != null) {
            return existing;
         } else {
            int local = this.nextLocal;
            this.nextLocal = this.nextLocal + Type.DOUBLE_TYPE.getSize();
            this.cachedLocals.put(node, local);
            node.operand().emit(this, method);
            method.store(local, Type.DOUBLE_TYPE);
            return local;
         }
      }

      private void resetLocals(int nextLocal) {
         this.cachedLocals.clear();
         this.nextLocal = nextLocal;
      }

      private void emitReferenceLoad(int referenceIndex, InstructionAdapter method) {
         method.load(0, InstructionAdapter.OBJECT_TYPE);
         method.getfield(this.className, "references", Type.getDescriptor(Object[].class));
         method.iconst(referenceIndex);
         method.aload(InstructionAdapter.OBJECT_TYPE);
         method.checkcast(this.mathValueType);
         method.invokeinterface(this.mathValueType.getInternalName(), "get", Type.getMethodDescriptor(Type.DOUBLE_TYPE, new Type[0]));
      }
   }
}

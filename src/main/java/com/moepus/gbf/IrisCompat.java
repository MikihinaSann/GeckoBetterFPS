package com.moepus.gbf;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;

public class IrisCompat {
	public static final boolean IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");

	private IrisCompat() {
	}

	public static VertexFormat getEntityVertexFormat() {
		try {
			Class<?> formats = Class.forName("net.irisshaders.iris.vertices.IrisVertexFormats");
			Field field = formats.getDeclaredField("ENTITY");
			return (VertexFormat)field.get(null);
		} catch (LinkageError | ReflectiveOperationException var2) {
			return null;
		}
	}
}

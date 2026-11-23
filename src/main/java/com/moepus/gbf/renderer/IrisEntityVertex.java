package com.moepus.gbf.renderer;

import com.moepus.gbf.IrisCompat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.OverlayAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;

public class IrisEntityVertex {
   public static final VertexFormat FORMAT = IrisCompat.IS_IRIS_INSTALLED ? IrisCompat.getEntityVertexFormat() : null;
   public static final int STRIDE;
   private static final long ENTITY_ID_OFFSET;
   private static final long BLOCK_ENTITY_ID_OFFSET;
   private static final long ITEM_ID_OFFSET;
   private static final long MID_U_OFFSET;
   private static final long MID_V_OFFSET;
   private static final long TANGENT_OFFSET;

   public static void write(
      long ptr, float x, float y, float z, int color, float u, float v, float mid_u, float mid_v, int overlay, int light, int normal, int tangent
   ) {
      PositionAttribute.put(ptr + 0L, x, y, z);
      ColorAttribute.set(ptr + 12L, color);
      TextureAttribute.put(ptr + 16L, u, v);
      OverlayAttribute.set(ptr + 24L, overlay);
      LightAttribute.set(ptr + 28L, light);
      NormalAttribute.set(ptr + 32L, normal);
      MemoryUtil.memPutShort(ptr + ENTITY_ID_OFFSET, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
      MemoryUtil.memPutShort(ptr + BLOCK_ENTITY_ID_OFFSET, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
      MemoryUtil.memPutShort(ptr + ITEM_ID_OFFSET, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
      MemoryUtil.memPutFloat(ptr + MID_U_OFFSET, mid_u);
      MemoryUtil.memPutFloat(ptr + MID_V_OFFSET, mid_v);
      MemoryUtil.memPutInt(ptr + TANGENT_OFFSET, tangent);
   }

   static {
      if (FORMAT == null) {
         STRIDE = 54;
         ENTITY_ID_OFFSET = 36L;
         BLOCK_ENTITY_ID_OFFSET = 38L;
         ITEM_ID_OFFSET = 40L;
         MID_U_OFFSET = 42L;
         MID_V_OFFSET = 46L;
         TANGENT_OFFSET = 50L;
      } else {
         STRIDE = FORMAT.getVertexSize();
         ENTITY_ID_OFFSET = FORMAT.getOffset(IrisVertexFormats.ENTITY_ID_ELEMENT);
         BLOCK_ENTITY_ID_OFFSET = ENTITY_ID_OFFSET + 2L;
         ITEM_ID_OFFSET = BLOCK_ENTITY_ID_OFFSET + 2L;
         MID_U_OFFSET = FORMAT.getOffset(IrisVertexFormats.MID_TEXTURE_ELEMENT);
         MID_V_OFFSET = MID_U_OFFSET + 4L;
         TANGENT_OFFSET = FORMAT.getOffset(IrisVertexFormats.TANGENT_ELEMENT);
      }
   }
}

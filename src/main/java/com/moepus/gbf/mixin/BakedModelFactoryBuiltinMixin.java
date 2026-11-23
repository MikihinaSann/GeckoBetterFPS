package com.moepus.gbf.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.loading.json.raw.Cube;
import software.bernie.geckolib.loading.json.raw.ModelProperties;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.BakedModelFactory.Builtin;
import software.bernie.geckolib.loading.object.BakedModelFactory.VertexSet;
import software.bernie.geckolib.util.RenderUtil;

@Mixin(
   value = {Builtin.class},
   remap = false
)
public abstract class BakedModelFactoryBuiltinMixin implements BakedModelFactory {
   @Overwrite(
      remap = false
   )
   public GeoCube constructCube(Cube cube, ModelProperties properties, GeoBone bone) {
      boolean mirror = cube.mirror() == Boolean.TRUE;
      double inflate = cube.inflate() != null ? cube.inflate() / 16.0 : (bone.getInflate() == null ? 0.0 : bone.getInflate() / 16.0);
      Vec3 size = RenderUtil.arrayToVec(cube.size());
      Vec3 origin = RenderUtil.arrayToVec(cube.origin());
      Vec3 rotation = RenderUtil.arrayToVec(cube.rotation());
      Vec3 pivot = RenderUtil.arrayToVec(cube.pivot());
      origin = new Vec3(-(origin.x + size.x) / 16.0, origin.y / 16.0, origin.z / 16.0);
      Vec3 vertexSize = size.multiply(0.0625, 0.0625, 0.0625);
      pivot = pivot.multiply(-1.0, 1.0, 1.0);
      GeoQuad[] quads = this.buildQuads(
         cube.uv(), new VertexSet(origin, vertexSize, inflate), cube, (float)properties.textureWidth(), (float)properties.textureHeight(), mirror
      );
      if (rotation.distanceToSqr(Vec3.ZERO) > 1.0E-5) {
         rotation = new Vec3(Math.toRadians(-rotation.x), Math.toRadians(-rotation.y), Math.toRadians(rotation.z));
      } else {
         rotation = Vec3.ZERO;
      }

      return new GeoCube(quads, pivot, rotation, size, inflate, mirror);
   }
}

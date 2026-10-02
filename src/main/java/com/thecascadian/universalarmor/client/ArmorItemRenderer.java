package com.thecascadian.universalarmor.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import com.thecascadian.universalarmor.registry.ModRegistries;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;

public class ArmorItemRenderer extends BlockEntityWithoutLevelRenderer {

  private static ArmorItemRenderer instance;
  private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

  public static ArmorItemRenderer getInstance() {
    if (instance == null) {
      instance = new ArmorItemRenderer(Minecraft.getInstance());
    }
    return instance;
  }

  private ArmorItemRenderer(Minecraft mc) {
    super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
  }

  @Override
  public void renderByItem(
    ItemStack stack,
    ItemDisplayContext ctx,
    PoseStack poseStack,
    MultiBufferSource buffer,
    int packedLight,
    int packedOverlay
  ) {
    ArmorPartsComponent comp = stack.get(
      ModRegistries.ARMOR_PARTS_COMPONENT.get()
    );
    if (!(stack.getItem() instanceof ArmorItem armorItem)) return;
    if (comp == null) comp = ArmorPartsComponent.defaultParts();

    ResourceLocation composite = getOrCreate(comp, armorItem.getType());

    VertexConsumer vc = buffer.getBuffer(
      RenderType.entityTranslucentCull(composite)
    );
    Matrix4f mat = poseStack.last().pose();

    // With the JSON files set to "gui_light": "front", we safely use standard 2D Z-normals!
    float nx = 0,
      ny = 0,
      nz = 1;
    float bnx = 0,
      bny = 0,
      bnz = -1;

    vc
      .addVertex(mat, 0F, 1F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(0F, 0F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(nx, ny, nz);
    vc
      .addVertex(mat, 0F, 0F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(0F, 1F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(nx, ny, nz);
    vc
      .addVertex(mat, 1F, 0F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(1F, 1F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(nx, ny, nz);
    vc
      .addVertex(mat, 1F, 1F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(1F, 0F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(nx, ny, nz);

    vc
      .addVertex(mat, 1F, 1F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(1F, 0F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(bnx, bny, bnz);
    vc
      .addVertex(mat, 1F, 0F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(1F, 1F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(bnx, bny, bnz);
    vc
      .addVertex(mat, 0F, 0F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(0F, 1F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(bnx, bny, bnz);
    vc
      .addVertex(mat, 0F, 1F, 0.5F)
      .setColor(255, 255, 255, 255)
      .setUv(0F, 0F)
      .setOverlay(packedOverlay)
      .setLight(packedLight)
      .setNormal(bnx, bny, bnz);
  }

  private ResourceLocation getOrCreate(
    ArmorPartsComponent parts,
    ArmorItem.Type type
  ) {
    String key = buildCacheKey(parts, type);
    return CACHE.computeIfAbsent(key, k -> generate(parts, type));
  }

  private String buildCacheKey(ArmorPartsComponent parts, ArmorItem.Type type) {
    return (
      type.getName() +
      "|" +
      BuiltInRegistries.ITEM.getKey(parts.frame().value()) +
      "|" +
      BuiltInRegistries.ITEM.getKey(parts.padding().value()) +
      "|" +
      BuiltInRegistries.ITEM.getKey(parts.plating().value()) +
      "|" +
      BuiltInRegistries.ITEM.getKey(parts.attachment().value()) +
      "|" +
      parts.shape()
    );
  }

  private ResourceLocation generate(
    ArmorPartsComponent parts,
    ArmorItem.Type type
  ) {
    Minecraft mc = Minecraft.getInstance();
    NativeImage canvas = null;

    Item[] materials = {
      parts.frame().value(),
      parts.padding().value(),
      parts.plating().value(),
      parts.attachment().value(),
    };

    String[] components = { "frame", "padding", "plating", "attachment" };

    try {
      for (int i = 0; i < materials.length; i++) {
        NativeImage src = loadMaterialTexture(mc, materials[i]);
        NativeImage mask = loadItemMask(
          mc,
          type.getName(),
          components[i],
          parts.shape()
        );

        if (src != null && mask != null) {
          if (canvas == null) {
            canvas = new NativeImage(mask.getWidth(), mask.getHeight(), true);
          }
          applyLayer(canvas, src, mask);
        }

        if (src != null) src.close();
        if (mask != null) mask.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (canvas == null) {
      return ResourceLocation.fromNamespaceAndPath(
        "universalarmor",
        "textures/item/" + type.getName() + "_overlay.png"
      );
    }

    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
      "universalarmor",
      "dyn_item_" +
        type.getName() +
        "_" +
        Integer.toHexString(buildCacheKey(parts, type).hashCode())
    );
    mc.getTextureManager().register(loc, new DynamicTexture(canvas));
    return loc;
  }

  private NativeImage loadItemMask(
    Minecraft mc,
    String piece,
    String component,
    String shape
  ) {
    String[] pathsToTry;
    if (component.equals("plating")) {
      pathsToTry = new String[] {
        shape + "_" + piece + "_" + component + "_overlay.png",
        piece + "_" + component + "_overlay.png",
        shape + "_" + piece + "_overlay.png",
        piece + "_overlay.png",
      };
    } else {
      pathsToTry = new String[] {
        shape + "_" + piece + "_" + component + "_overlay.png",
        piece + "_" + component + "_overlay.png",
      };
    }

    for (String p : pathsToTry) {
      ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
        "universalarmor",
        "textures/item/" + p
      );
      Optional<Resource> res = mc.getResourceManager().getResource(loc);
      if (res.isPresent()) {
        try {
          return NativeImage.read(res.get().open());
        } catch (Exception e) {
          // ignore and try next
        }
      }
    }
    return null;
  }

  private NativeImage loadMaterialTexture(Minecraft mc, Item material) {
    try {
      var model = mc
        .getItemRenderer()
        .getItemModelShaper()
        .getItemModel(material.getDefaultInstance());
      var sprite = model.getParticleIcon(
        net.neoforged.neoforge.client.model.data.ModelData.EMPTY
      );
      ResourceLocation spriteLoc = sprite.contents().name();
      ResourceLocation sourcePath = ResourceLocation.fromNamespaceAndPath(
        spriteLoc.getNamespace(),
        "textures/" + spriteLoc.getPath() + ".png"
      );

      Optional<Resource> sourceRes = mc
        .getResourceManager()
        .getResource(sourcePath);
      if (sourceRes.isPresent()) {
        try (InputStream stream = sourceRes.get().open()) {
          return NativeImage.read(stream);
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  private void applyLayer(
    NativeImage canvas,
    NativeImage source,
    NativeImage mask
  ) {
    int w = mask.getWidth();
    int h = mask.getHeight();

    int minX = source.getWidth(),
      minY = source.getHeight(),
      maxX = 0,
      maxY = 0;
    boolean foundOpaque = false;
    for (int sx = 0; sx < source.getWidth(); sx++) {
      for (int sy = 0; sy < source.getHeight(); sy++) {
        if (((source.getPixelRGBA(sx, sy) >> 24) & 0xFF) > 0) {
          minX = Math.min(minX, sx);
          minY = Math.min(minY, sy);
          maxX = Math.max(maxX, sx);
          maxY = Math.max(maxY, sy);
          foundOpaque = true;
        }
      }
    }
    if (!foundOpaque) {
      minX = 0;
      minY = 0;
      maxX = source.getWidth() - 1;
      maxY = source.getHeight() - 1;
    }
    int boxW = Math.max(1, maxX - minX + 1);
    int boxH = Math.max(1, maxY - minY + 1);

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        int maskColor = mask.getPixelRGBA(x, y);
        int maskAlpha = (maskColor >> 24) & 0xFF;

        if (maskAlpha > 0) {
          int srcPx = getOpaquePixel(source, x, y, minX, minY, boxW, boxH);
          float shadeMultiplier = (maskColor & 0xFF) / 255.0f;

          int r = (int) (((srcPx) & 0xFF) * shadeMultiplier);
          int g = (int) (((srcPx >> 8) & 0xFF) * shadeMultiplier);
          int b = (int) (((srcPx >> 16) & 0xFF) * shadeMultiplier);

          int existingColor = canvas.getPixelRGBA(x, y);
          int existingAlpha = (existingColor >> 24) & 0xFF;

          if (existingAlpha == 0) {
            canvas.setPixelRGBA(
              x,
              y,
              (maskAlpha << 24) | (b << 16) | (g << 8) | r
            );
          } else {
            float alpha = maskAlpha / 255.0f;
            float invAlpha = 1.0f - alpha;
            float extAlpha = existingAlpha / 255.0f;

            float outAlpha = alpha + extAlpha * invAlpha;
            int nA = (int) (outAlpha * 255);
            if (nA > 255) nA = 255;

            if (nA > 0) {
              int eR = existingColor & 0xFF;
              int eG = (existingColor >> 8) & 0xFF;
              int eB = (existingColor >> 16) & 0xFF;

              int nR = (int) ((r * alpha + eR * extAlpha * invAlpha) /
                outAlpha);
              int nG = (int) ((g * alpha + eG * extAlpha * invAlpha) /
                outAlpha);
              int nB = (int) ((b * alpha + eB * extAlpha * invAlpha) /
                outAlpha);

              canvas.setPixelRGBA(
                x,
                y,
                (nA << 24) | (nB << 16) | (nG << 8) | nR
              );
            }
          }
        }
      }
    }
  }

  private int getOpaquePixel(
    NativeImage src,
    int x,
    int y,
    int minX,
    int minY,
    int boxW,
    int boxH
  ) {
    int srcX = minX + (x % boxW);
    int srcY = minY + (y % boxH);
    int px = src.getPixelRGBA(srcX, srcY);
    if (((px >> 24) & 0xFF) > 0) return px;

    for (int r = 1; r <= Math.max(boxW, boxH); r++) {
      for (int i = -r; i <= r; i++) {
        for (int j = -r; j <= r; j++) {
          if (Math.abs(i) == r || Math.abs(j) == r) {
            int cx = srcX + i;
            int cy = srcY + j;
            if (
              cx >= minX && cx < minX + boxW && cy >= minY && cy < minY + boxH
            ) {
              int cpx = src.getPixelRGBA(cx, cy);
              if (((cpx >> 24) & 0xFF) > 0) return cpx;
            }
          }
        }
      }
    }
    return 0xFFFFFFFF;
  }
}

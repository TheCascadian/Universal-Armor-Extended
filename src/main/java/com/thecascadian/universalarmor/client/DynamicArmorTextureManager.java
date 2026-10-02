package com.thecascadian.universalarmor.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;

public class DynamicArmorTextureManager {

  private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

  private static final String[] COMPONENT_ORDER = {
    "frame",
    "padding",
    "plating",
    "attachment",
  };

  public static ResourceLocation getOrCreateCompiledTexture(
    ArmorPartsComponent parts,
    boolean innerModel
  ) {
    String cacheKey = buildCacheKey(parts, innerModel);
    return CACHE.computeIfAbsent(cacheKey, key ->
      generateCompiledTexture(parts, innerModel)
    );
  }

  private static String buildCacheKey(
    ArmorPartsComponent parts,
    boolean innerModel
  ) {
    return (
      sanitize(parts.frame().value()) +
      "|" +
      sanitize(parts.padding().value()) +
      "|" +
      sanitize(parts.plating().value()) +
      "|" +
      sanitize(parts.attachment().value()) +
      "|" +
      parts.shape() +
      (innerModel ? "_inner" : "_outer")
    );
  }

  private static String sanitize(Item item) {
    ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
    return loc.getNamespace() + "_" + loc.getPath();
  }

  private static ResourceLocation generateCompiledTexture(
    ArmorPartsComponent parts,
    boolean innerModel
  ) {
    Minecraft mc = Minecraft.getInstance();

    Item[] materials = {
      parts.frame().value(),
      parts.padding().value(),
      parts.plating().value(),
      parts.attachment().value(),
    };

    NativeImage canvas = null;

    try {
      for (int i = 0; i < materials.length; i++) {
        NativeImage source = loadMaterialTexture(mc, materials[i]);
        NativeImage mask = loadMask(
          mc,
          COMPONENT_ORDER[i],
          innerModel,
          parts.shape()
        );

        if (source == null || mask == null) {
          if (source != null) source.close();
          if (mask != null) mask.close();
          continue;
        }

        if (canvas == null) {
          canvas = new NativeImage(mask.getWidth(), mask.getHeight(), true);
        }

        applyLayer(canvas, source, mask);
        source.close();
        mask.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (canvas == null) return fallbackTexture(innerModel, parts.shape());

    DynamicTexture dynamicTexture = new DynamicTexture(canvas);
    String texName =
      "universalarmor_compiled_" +
      Integer.toHexString(buildCacheKey(parts, innerModel).hashCode());

    return mc.getTextureManager().register(texName, dynamicTexture);
  }

  private static void applyLayer(
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
        if (maskAlpha <= 0) continue;

        float shade = (maskColor & 0xFF) / 255.0f;
        int srcColor = getOpaquePixel(source, x, y, minX, minY, boxW, boxH);

        int r = (int) (((srcColor) & 0xFF) * shade);
        int g = (int) (((srcColor >> 8) & 0xFF) * shade);
        int b = (int) (((srcColor >> 16) & 0xFF) * shade);

        canvas.setPixelRGBA(x, y, (maskAlpha << 24) | (b << 16) | (g << 8) | r);
      }
    }
  }

  private static int getOpaquePixel(
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

  private static NativeImage loadMaterialTexture(Minecraft mc, Item material) {
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
      if (sourceRes.isEmpty()) return null;

      try (InputStream stream = sourceRes.get().open()) {
        return NativeImage.read(stream);
      }
    } catch (Exception e) {
      return null;
    }
  }

  private static NativeImage loadMask(
    Minecraft mc,
    String component,
    boolean innerModel,
    String shape
  ) {
    ResourceLocation maskPath = ResourceLocation.fromNamespaceAndPath(
      "universalarmor",
      "textures/models/armor/" +
        shape +
        "_" +
        component +
        "_layer_" +
        (innerModel ? "2" : "1") +
        ".png"
    );

    Optional<Resource> maskRes = mc.getResourceManager().getResource(maskPath);
    if (maskRes.isEmpty() && !shape.equals("standard")) {
      maskRes = mc
        .getResourceManager()
        .getResource(
          ResourceLocation.fromNamespaceAndPath(
            "universalarmor",
            "textures/models/armor/standard_" +
              component +
              "_layer_" +
              (innerModel ? "2" : "1") +
              ".png"
          )
        );
    }

    if (maskRes.isEmpty()) return deriveMask(mc, component, innerModel, shape);

    try (InputStream stream = maskRes.get().open()) {
      return NativeImage.read(stream);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * No dedicated per-component mask exists, so split the shape's universal
   * layer by shade: darkest pixels become padding, brightest become the
   * attachment, upper-mid becomes plating and the rest is the frame.
   */
  private static NativeImage deriveMask(
    Minecraft mc,
    String component,
    boolean innerModel,
    String shape
  ) {
    Optional<Resource> res = mc
      .getResourceManager()
      .getResource(fallbackTexture(innerModel, shape));
    if (res.isEmpty()) res = mc
      .getResourceManager()
      .getResource(fallbackTexture(innerModel, "standard"));
    if (res.isEmpty()) return null;

    try (InputStream stream = res.get().open()) {
      NativeImage base = NativeImage.read(stream);
      int w = base.getWidth();
      int h = base.getHeight();

      int[] hist = new int[256];
      int total = 0;
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
          int px = base.getPixelRGBA(x, y);
          if (((px >> 24) & 0xFF) == 0) continue;
          hist[px & 0xFF]++;
          total++;
        }
      }
      int lowCut = quantile(hist, total, 0.25);
      int midCut = quantile(hist, total, 0.65);
      int highCut = quantile(hist, total, 0.90);

      NativeImage mask = new NativeImage(w, h, true);
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
          int px = base.getPixelRGBA(x, y);
          if (((px >> 24) & 0xFF) == 0) continue;
          int shade = px & 0xFF;
          String part;
          if (shade < lowCut) part = "padding";
          else if (shade > highCut) part = "attachment";
          else if (shade > midCut) part = "plating";
          else part = "frame";
          if (part.equals(component)) mask.setPixelRGBA(x, y, px);
        }
      }
      base.close();
      return mask;
    } catch (Exception e) {
      return null;
    }
  }

  private static int quantile(int[] hist, int total, double q) {
    int target = (int) (total * q);
    int acc = 0;
    for (int v = 0; v < 256; v++) {
      acc += hist[v];
      if (acc > target) return v;
    }
    return 255;
  }

  private static ResourceLocation fallbackTexture(
    boolean innerModel,
    String shape
  ) {
    return ResourceLocation.fromNamespaceAndPath(
      "universalarmor",
      "textures/models/armor/" +
        shape +
        "_universal_layer_" +
        (innerModel ? "2" : "1") +
        ".png"
    );
  }
}

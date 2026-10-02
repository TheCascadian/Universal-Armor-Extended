package com.thecascadian.universalarmor.client.screen;

import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import com.thecascadian.universalarmor.inventory.ArmoryResultSlot;
import com.thecascadian.universalarmor.inventory.ArmoryStationMenu;
import com.thecascadian.universalarmor.network.ChangeShapePacket;
import com.thecascadian.universalarmor.util.DynamicStatsCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ArmoryStationScreen
  extends AbstractContainerScreen<ArmoryStationMenu>
{

  private static final int PANEL_X = 84;
  private static final int PANEL_Y = 20;
  private static final int PANEL_W = 66;
  private static final int PANEL_H = 90;
  private static final int STATS_X = 154;
  private static final int STATS_W = 54;

  private static final int[] PART_COLORS = {
    0xFFB0623A,
    0xFF4F8F5C,
    0xFF4A78B8,
    0xFFC9A93A,
  };

  private static final ArmorItem.Type[] TYPES = {
    ArmorItem.Type.HELMET,
    ArmorItem.Type.CHESTPLATE,
    ArmorItem.Type.LEGGINGS,
    ArmorItem.Type.BOOTS,
  };
  private static final EquipmentSlot[] EQUIP = {
    EquipmentSlot.HEAD,
    EquipmentSlot.CHEST,
    EquipmentSlot.LEGS,
    EquipmentSlot.FEET,
  };

  private ArmorStand previewStand;
  private ArmorPartsComponent lastParts;
  private float dragYaw = 0;
  private boolean dragging = false;
  private boolean autoSpin = true;

  public ArmoryStationScreen(
    ArmoryStationMenu menu,
    Inventory playerInventory,
    Component title
  ) {
    super(menu, playerInventory, title);
    this.imageWidth = 240;
    this.imageHeight = 232;
    this.inventoryLabelX = 40;
    this.inventoryLabelY = 138;
    this.titleLabelX = 10;
    this.titleLabelY = 6;
  }

  @Override
  protected void init() {
    super.init();

    this.addRenderableWidget(
      new ArrowButton(
        this.leftPos + PANEL_X,
        this.topPos + 114,
        true,
        () -> sendShapeChange(-1)
      )
    );
    this.addRenderableWidget(
      new ArrowButton(
        this.leftPos + STATS_X + STATS_W - 14,
        this.topPos + 114,
        false,
        () -> sendShapeChange(1)
      )
    );
    this.addRenderableWidget(
      new SpinButton(
        this.leftPos + PANEL_X + 3,
        this.topPos + PANEL_Y + 3,
        () -> autoSpin,
        () -> autoSpin = !autoSpin
      )
    );
    this.addRenderableWidget(
      Button.builder(Component.literal("?"), b ->
        this.minecraft.setScreen(new ArmoryGuideScreen(this))
      )
        .bounds(this.leftPos + this.imageWidth - 22, this.topPos + 4, 14, 14)
        .tooltip(
          net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.universalarmor.guide.button")
          )
        )
        .build()
    );

    // init() also runs when returning from the guide; re-dress the new stand
    lastParts = null;
    if (this.minecraft.level != null) {
      previewStand = new ArmorStand(
        EntityType.ARMOR_STAND,
        this.minecraft.level
      );
      previewStand.setInvisible(true);
      previewStand.setNoBasePlate(true);
    }
  }

  private void sendShapeChange(int delta) {
    int n = ArmoryStationMenu.SHAPES.length;
    int newIndex = (this.menu.getShapeIndex() + delta + n) % n;
    PacketDistributor.sendToServer(new ChangeShapePacket(newIndex));
  }

  // ---------------------------------------------------------------- drawing

  private static void bevel(
    GuiGraphics g,
    int x1,
    int y1,
    int x2,
    int y2,
    boolean raised
  ) {
    int light = raised ? 0xFFFFFFFF : 0xFF555555;
    int dark = raised ? 0xFF555555 : 0xFFFFFFFF;
    g.fill(x1, y1, x2, y2, 0xFFC6C6C6);
    g.fill(x1, y1, x2 - 1, y1 + 1, light);
    g.fill(x1, y1, x1 + 1, y2 - 1, light);
    g.fill(x1 + 1, y2 - 1, x2, y2, dark);
    g.fill(x2 - 1, y1 + 1, x2, y2, dark);
  }

  @Override
  protected void renderBg(
    GuiGraphics g,
    float partialTick,
    int mouseX,
    int mouseY
  ) {
    int x = this.leftPos;
    int y = this.topPos;

    // window
    g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFF000000);
    bevel(g, x, y, x + imageWidth, y + imageHeight, true);

    // preview panel
    g.fill(
      x + PANEL_X - 1,
      y + PANEL_Y - 1,
      x + PANEL_X + PANEL_W + 1,
      y + PANEL_Y + PANEL_H + 1,
      0xFF373737
    );
    g.fill(
      x + PANEL_X,
      y + PANEL_Y,
      x + PANEL_X + PANEL_W,
      y + PANEL_Y + PANEL_H,
      0xFF1A1C20
    );
    g.fill(
      x + PANEL_X + 8,
      y + PANEL_Y + PANEL_H - 10,
      x + PANEL_X + PANEL_W - 8,
      y + PANEL_Y + PANEL_H - 8,
      0x33FFFFFF
    );

    // stats panel
    g.fill(
      x + STATS_X - 1,
      y + PANEL_Y - 1,
      x + STATS_X + STATS_W + 1,
      y + PANEL_Y + PANEL_H + 1,
      0xFF373737
    );
    g.fill(
      x + STATS_X,
      y + PANEL_Y,
      x + STATS_X + STATS_W,
      y + PANEL_Y + PANEL_H,
      0xFF8B8B8B
    );

    // slot frames; inputs get a colour key matching their label
    for (Slot slot : this.menu.slots) {
      int sx = x + slot.x;
      int sy = y + slot.y;
      g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
      g.fill(sx, sy, sx + 17, sy + 17, 0xFFFFFFFF);
      g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
      if (slot.index < ArmoryStationMenu.INPUTS) {
        g.fill(sx - 4, sy - 1, sx - 2, sy + 17, PART_COLORS[slot.index]);
      }
    }

    // input labels
    for (int i = 0; i < ArmoryStationMenu.INPUTS; i++) {
      String part = ArmoryStationMenu.PART_NAMES[i];
      g.pose().pushPose();
      g.pose().translate(x + 31, y + 26 + i * 20, 0);
      g.pose().scale(0.85f, 0.85f, 1f);
      g.drawString(
        this.font,
        Component.translatable("gui.universalarmor.part." + part),
        0,
        0,
        0x404040,
        false
      );
      g.pose().popPose();
      g.pose().pushPose();
      g.pose().translate(x + 31, y + 36 + i * 20, 0);
      g.pose().scale(0.7f, 0.7f, 1f);
      g.drawString(
        this.font,
        Component.translatable("gui.universalarmor.part." + part + ".short"),
        0,
        0,
        0x6A6A6A,
        false
      );
      g.pose().popPose();
    }

    renderPreview(g, partialTick, mouseX, mouseY);
    renderStats(g);

    // shape name
    Component shape = Component.translatable(
      "shape.universalarmor." +
        ArmoryStationMenu.SHAPES[this.menu.getShapeIndex()]
    );
    g.drawCenteredString(
      this.font,
      shape,
      x + (PANEL_X + 14 + STATS_X + STATS_W - 14) / 2,
      y + 118,
      0x303030
    );

    // status line
    g.drawCenteredString(
      this.font,
      statusLine(),
      x + imageWidth / 2,
      y + 132,
      0x404040
    );
  }

  private Component statusLine() {
    if (this.menu.resolveSources() == null) return Component.translatable(
      "gui.universalarmor.status.empty"
    ).withStyle(ChatFormatting.DARK_GRAY);
    if (this.menu.hasOutput()) return Component.translatable(
      "gui.universalarmor.status.ready"
    ).withStyle(ChatFormatting.DARK_GREEN);
    return Component.translatable("gui.universalarmor.status.short")
      .withStyle(ChatFormatting.DARK_RED);
  }

  private void renderPreview(
    GuiGraphics g,
    float partialTick,
    int mouseX,
    int mouseY
  ) {
    int px = this.leftPos + PANEL_X;
    int py = this.topPos + PANEL_Y;
    ArmorPartsComponent parts = this.menu.getPreviewParts();

    if (parts == null || previewStand == null) {
      var lines = this.font.split(
        Component.translatable("gui.universalarmor.preview.empty"),
        PANEL_W - 8
      );
      int ly = py + PANEL_H / 2 - lines.size() * 5;
      for (var line : lines) {
        g.drawString(
          this.font,
          line,
          px + (PANEL_W - this.font.width(line)) / 2,
          ly,
          0x8A8F99,
          false
        );
        ly += 10;
      }
      return;
    }

    if (!Objects.equals(parts, lastParts)) {
      lastParts = parts;
      for (int i = 0; i < 4; i++) {
        previewStand.setItemSlot(
          EQUIP[i],
          ArmoryStationMenu.createArmor(i, parts)
        );
      }
    }

    if (autoSpin && !dragging) dragYaw += partialTick * 1.5f;
    float yaw = 180f + dragYaw;
    previewStand.yBodyRot = yaw;
    previewStand.yBodyRotO = yaw;
    previewStand.yHeadRot = yaw;
    previewStand.yHeadRotO = yaw;
    previewStand.setYRot(yaw);
    previewStand.yRotO = yaw;

    g.enableScissor(px, py, px + PANEL_W, py + PANEL_H);
    InventoryScreen.renderEntityInInventory(
      g,
      px + PANEL_W / 2f,
      py + PANEL_H / 2f + 4,
      40,
      new Vector3f(0, previewStand.getBbHeight() / 2f + 0.0625f, 0),
      new Quaternionf().rotateZ((float) Math.PI),
      null,
      previewStand
    );
    g.disableScissor();
  }

  private void renderStats(GuiGraphics g) {
    int x = this.leftPos + STATS_X + 4;
    int y = this.topPos + PANEL_Y + 5;
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().scale(0.85f, 0.85f, 1f);
    g.drawString(
      this.font,
      Component.translatable("gui.universalarmor.stats.title"),
      0,
      0,
      0xFFFFFF,
      true
    );
    g.pose().popPose();

    ArmorPartsComponent parts = this.menu.getPreviewParts();
    if (parts == null) {
      g.drawString(this.font, "--", x, y + 14, 0x404040, false);
      return;
    }

    int armor = 0;
    for (ArmorItem.Type t : TYPES) armor += DynamicStatsCalculator.getCompoundedDefense(
      parts,
      t
    );
    float tough = DynamicStatsCalculator.getCompoundedToughness(parts) * 4;
    int dur = DynamicStatsCalculator.getCompoundedDurability(
      parts,
      ArmorItem.Type.CHESTPLATE
    );
    int speed = switch (parts.shape()) {
      case "heavy" -> -12;
      case "light" -> 12;
      default -> 0;
    };

    statRow(g, x, y + 14, "armor", String.valueOf(armor), 0x1F3FBF);
    statRow(g, x, y + 31, "toughness", fmt(tough), 0x1F3FBF);
    statRow(g, x, y + 48, "durability", String.valueOf(dur), 0x2B6B2B);
    statRow(
      g,
      x,
      y + 65,
      "speed",
      (speed > 0 ? "+" : "") + speed + "%",
      speed < 0 ? 0xA02020 : speed > 0 ? 0x2B6B2B : 0x404040
    );
  }

  private static String fmt(float v) {
    return v == (int) v ? String.valueOf((int) v) : String.format("%.1f", v);
  }

  private void statRow(
    GuiGraphics g,
    int x,
    int y,
    String key,
    String value,
    int color
  ) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().scale(0.75f, 0.75f, 1f);
    g.drawString(
      this.font,
      Component.translatable("gui.universalarmor.stats." + key),
      0,
      0,
      0x303030,
      false
    );
    g.pose().popPose();
    g.drawString(this.font, value, x + 2, y + 8, color, false);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    g.drawString(this.font, this.title, titleLabelX, titleLabelY, 0x404040, false);
  }

  // ---------------------------------------------------------------- input

  private boolean overPreview(double mx, double my) {
    int px = this.leftPos + PANEL_X;
    int py = this.topPos + PANEL_Y;
    return mx >= px && mx < px + PANEL_W && my >= py && my < py + PANEL_H;
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0 && overPreview(mx, my)) dragging = true;
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    dragging = false;
    return super.mouseReleased(mx, my, button);
  }

  @Override
  public boolean mouseDragged(
    double mx,
    double my,
    int button,
    double dx,
    double dy
  ) {
    if (dragging) {
      dragYaw -= (float) dx * 1.5f;
      return true;
    }
    return super.mouseDragged(mx, my, button, dx, dy);
  }

  // -------------------------------------------------------------- tooltips

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    super.render(g, mouseX, mouseY, partialTick);

    Slot hovered = this.hoveredSlot;
    if (hovered != null && !hovered.hasItem()) {
      List<Component> tip = null;
      if (hovered.index < ArmoryStationMenu.INPUTS) {
        tip = inputTooltip(hovered.index);
      } else if (hovered instanceof ArmoryResultSlot) {
        tip = outputTooltip(hovered.index - ArmoryStationMenu.OUTPUT_START);
      }
      if (tip != null) g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
    }
    this.renderTooltip(g, mouseX, mouseY);
  }

  private List<Component> inputTooltip(int i) {
    String part = ArmoryStationMenu.PART_NAMES[i];
    List<Component> tip = new ArrayList<>();
    tip.add(
      Component.translatable("gui.universalarmor.part." + part).withStyle(
        ChatFormatting.WHITE
      )
    );
    tip.add(
      Component.translatable("gui.universalarmor.part." + part + ".tip")
        .withStyle(ChatFormatting.GRAY)
    );
    if (i > 0) tip.add(
      Component.translatable("gui.universalarmor.part.optional").withStyle(
        ChatFormatting.DARK_GRAY
      )
    );
    return tip;
  }

  private List<Component> outputTooltip(int piece) {
    List<Component> tip = new ArrayList<>();
    tip.add(
      Component.translatable(
        "item.universalarmor.universal_" +
          ArmoryStationMenu.PIECE_NAMES[piece] +
          ".generic"
      ).withStyle(ChatFormatting.WHITE)
    );
    appendCosts(tip, piece);
    return tip;
  }

  private void appendCosts(List<Component> tip, int piece) {
    tip.add(
      Component.translatable("gui.universalarmor.cost.header").withStyle(
        ChatFormatting.GRAY
      )
    );
    int[] costs = ArmoryStationMenu.costsFor(piece);
    int[] src = this.menu.resolveSources();
    for (int c = 0; c < 4; c++) {
      String part = ArmoryStationMenu.PART_NAMES[c];
      Component line = Component.literal(" " + costs[c] + "× ").append(
        Component.translatable("gui.universalarmor.part." + part)
      );
      ChatFormatting color = ChatFormatting.DARK_GRAY;
      if (src != null) {
        int[] req = this.menu.requirements(piece);
        boolean ok =
          this.menu.getInputStack(src[c]).getCount() >= req[src[c]];
        color = ok ? ChatFormatting.GREEN : ChatFormatting.RED;
      }
      tip.add(line.copy().withStyle(color));
    }
  }

  @Override
  protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
    List<Component> tip = super.getTooltipFromContainerItem(stack);
    if (
      this.hoveredSlot instanceof ArmoryResultSlot &&
      this.hoveredSlot.hasItem()
    ) {
      tip = new ArrayList<>(tip);
      tip.add(Component.empty());
      appendCosts(
        tip,
        this.hoveredSlot.index - ArmoryStationMenu.OUTPUT_START
      );
    }
    return tip;
  }

  // -------------------------------------------------------------- widgets

  private static class SpinButton extends Button {

    private final java.util.function.BooleanSupplier state;

    SpinButton(
      int x,
      int y,
      java.util.function.BooleanSupplier state,
      Runnable action
    ) {
      super(
        x,
        y,
        24,
        11,
        Component.translatable("gui.universalarmor.spin"),
        b -> action.run(),
        DEFAULT_NARRATION
      );
      this.state = state;
      setTooltip(
        net.minecraft.client.gui.components.Tooltip.create(
          Component.translatable("gui.universalarmor.spin.tip")
        )
      );
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
      int x = getX();
      int y = getY();
      boolean on = state.getAsBoolean();
      g.fill(x, y, x + width, y + height, isHoveredOrFocused() ? 0xFF4A4F5A : 0xFF30343C);
      g.fill(x, y, x + width, y + 1, on ? 0xFF55CC55 : 0xFF666B75);
      g.pose().pushPose();
      g.pose().translate(x + 3, y + 3, 0);
      g.pose().scale(0.7f, 0.7f, 1f);
      g.drawString(
        Minecraft.getInstance().font,
        getMessage(),
        0,
        0,
        on ? 0xFF8CE68C : 0xFF9AA0AA,
        false
      );
      g.pose().popPose();
    }
  }

  private static class ArrowButton extends Button {

    private final boolean left;

    ArrowButton(int x, int y, boolean left, Runnable action) {
      super(
        x,
        y,
        14,
        14,
        Component.literal(left ? "<" : ">"),
        b -> action.run(),
        DEFAULT_NARRATION
      );
      this.left = left;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
      int x = getX();
      int y = getY();
      bevel(g, x, y, x + width, y + height, !isHoveredOrFocused());
      g.fill(x, y, x + 1, y + 1, 0xFFC6C6C6);
      // chevron
      int cx = x + width / 2 + (left ? -1 : 1);
      int cy = y + height / 2;
      int color = isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFF303030;
      for (int i = 0; i < 4; i++) {
        int dx = left ? -i : i;
        g.fill(cx + dx, cy - (3 - i), cx + dx + 1, cy + (3 - i) + 1, color);
      }
    }
  }
}

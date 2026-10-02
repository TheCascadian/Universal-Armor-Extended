package com.thecascadian.universalarmor.client.screen;

import com.thecascadian.universalarmor.guide.GuideContent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The guide book: a chapter list on the left, the chosen chapter laid out into
 * pages on the right, with page buttons and mouse wheel paging. Same layout as
 * the Universal Ore Processing guide.
 */
public class ArmoryGuideScreen extends Screen {

  private static final int WIDTH = 300;
  private static final int HEIGHT = 196;
  private static final int LIST_WIDTH = 104;
  private static final int LINE_HEIGHT = 10;
  private static final int CELL = 18;
  private static final int GAP = 4;
  private static final int PAGE_HEIGHT = HEIGHT - 34;

  private static final int PAPER = 0xFFE9DCBC;
  private static final int PAPER_EDGE = 0xFF8C6B3F;
  private static final int LIST_BG = 0xFF5B4327;
  private static final int TEXT = 0xFF2B2015;

  private sealed interface Line permits TextLine, StackLine, Gap {
    int height();
  }

  private record TextLine(FormattedCharSequence text) implements Line {
    public int height() {
      return LINE_HEIGHT;
    }
  }

  private record StackLine(List<ItemStack> stacks) implements Line {
    public int height() {
      return CELL;
    }
  }

  private record Gap() implements Line {
    public int height() {
      return GAP;
    }
  }

  @Nullable
  private final Screen parent;

  private final List<GuideContent.Chapter> chapters = GuideContent.build();
  private List<List<Line>> pages = List.of();
  private int chapter;
  private int page;
  private Button previous;
  private Button next;

  public ArmoryGuideScreen(@Nullable Screen parent) {
    super(Component.translatable("item.universalarmor.armory_guide"));
    this.parent = parent;
  }

  private int left() {
    return (width - WIDTH) / 2;
  }

  private int top() {
    return (height - HEIGHT) / 2;
  }

  @Override
  protected void init() {
    int x = left() + 6;
    int buttonHeight = Math.min(
      16,
      (HEIGHT - 12) / Math.max(1, chapters.size())
    );
    for (int i = 0; i < chapters.size(); i++) {
      int index = i;
      addRenderableWidget(
        Button.builder(chapters.get(i).title(), b -> select(index))
          .bounds(x, top() + 6 + i * buttonHeight, LIST_WIDTH - 10, buttonHeight - 1)
          .build()
      );
    }
    int bottom = top() + HEIGHT - 22;
    int pageX = left() + LIST_WIDTH + 6;
    previous = addRenderableWidget(
      Button.builder(Component.literal("<"), b -> turn(-1))
        .bounds(pageX, bottom, 22, 16)
        .build()
    );
    next = addRenderableWidget(
      Button.builder(Component.literal(">"), b -> turn(1))
        .bounds(left() + WIDTH - 28, bottom, 22, 16)
        .build()
    );
    select(chapter);
  }

  private void select(int index) {
    chapter = Math.max(0, Math.min(index, chapters.size() - 1));
    page = 0;
    paginate();
    updateButtons();
  }

  private void paginate() {
    int textWidth = WIDTH - LIST_WIDTH - 24;
    List<List<Line>> result = new ArrayList<>();
    List<Line> current = new ArrayList<>();
    int used = 0;

    for (GuideContent.Element element : chapters.get(chapter).elements()) {
      List<Line> block = new ArrayList<>();
      boolean whole = true;
      switch (element) {
        case GuideContent.Text text -> {
          for (FormattedCharSequence line : font.split(text.text(), textWidth)) block.add(
            new TextLine(line)
          );
          whole = false;
        }
        case GuideContent.Flow flow -> block.add(new StackLine(flow.stacks()));
      }
      block.add(new Gap());

      int blockHeight = block.stream().mapToInt(Line::height).sum();
      if (whole && used > 0 && used + blockHeight > PAGE_HEIGHT) {
        result.add(current);
        current = new ArrayList<>();
        used = 0;
      }
      for (Line line : block) {
        if (!whole && used > 0 && used + line.height() > PAGE_HEIGHT) {
          result.add(current);
          current = new ArrayList<>();
          used = 0;
        }
        if (line instanceof Gap && used == 0) continue;
        current.add(line);
        used += line.height();
      }
    }
    if (!current.isEmpty() || result.isEmpty()) result.add(current);
    pages = result;
  }

  private void turn(int delta) {
    int target = Math.max(0, Math.min(page + delta, pages.size() - 1));
    if (target != page) {
      page = target;
      updateButtons();
    }
  }

  private void updateButtons() {
    if (previous != null) previous.active = page > 0;
    if (next != null) next.active = page < pages.size() - 1;
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    turn(sy < 0 ? 1 : -1);
    return true;
  }

  @Override
  public void onClose() {
    this.minecraft.setScreen(parent);
  }

  /** No vanilla blur: a plain dim over the world. The parent is not drawn, as its items and 3D preview would draw over the guide. */
  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    g.fill(0, 0, width, height, 0xA0000000);
    int left = left();
    int top = top();
    g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, PAPER_EDGE);
    g.fill(left, top, left + LIST_WIDTH, top + HEIGHT, LIST_BG);
    g.fill(left + LIST_WIDTH, top, left + WIDTH, top + HEIGHT, PAPER);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    g.pose().pushPose();
    g.pose().translate(0, 0, 0);
    this.renderBackground(g, mouseX, mouseY, partialTick);
    g.pose().popPose();

    g.pose().pushPose();
    g.pose().translate(0, 0, 200);
    super.render(g, mouseX, mouseY, partialTick);

    int left = left();
    int top = top();
    int textX = left + LIST_WIDTH + 8;
    int y = top + 8;
    ItemStack hovered = ItemStack.EMPTY;

    for (Line line : pages.get(page)) {
      switch (line) {
        case TextLine text -> g.drawString(font, text.text(), textX, y, TEXT, false);
        case StackLine row -> {
          for (int i = 0; i < row.stacks().size(); i++) {
            ItemStack stack = row.stacks().get(i);
            int x = textX + i * CELL;
            if (stack.isEmpty()) {
              g.drawString(font, "→", x + 5, y + 5, TEXT, false);
              continue;
            }
            g.renderItem(stack, x + 1, y + 1);
            if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) hovered = stack;
          }
        }
        case Gap gap -> {}
      }
      y += line.height();
    }

    Component counter = Component.literal((page + 1) + " / " + pages.size());
    g.drawString(
      font,
      counter,
      left + LIST_WIDTH + (WIDTH - LIST_WIDTH) / 2 - font.width(counter) / 2,
      top + HEIGHT - 18,
      TEXT,
      false
    );
    if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
    g.pose().popPose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}

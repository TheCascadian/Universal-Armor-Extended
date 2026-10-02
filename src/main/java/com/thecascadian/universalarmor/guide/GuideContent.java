package com.thecascadian.universalarmor.guide;

import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import com.thecascadian.universalarmor.inventory.ArmoryStationMenu;
import com.thecascadian.universalarmor.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Chapters of the in-game guide: lang text paragraphs plus rows of example items. */
public final class GuideContent {

  /** One piece of a chapter: wrapped text or a row of items. */
  public sealed interface Element permits Text, Flow {}

  public record Text(Component text) implements Element {}

  /** Items read left to right; an empty stack is drawn as an arrow. */
  public record Flow(List<ItemStack> stacks) implements Element {}

  public record Chapter(Component title, List<Element> elements) {}

  private static final String KEY = "guide.universalarmor.";
  private static final ItemStack ARROW = ItemStack.EMPTY;

  private GuideContent() {}

  public static List<Chapter> build() {
    List<ItemStack> pieces = new ArrayList<>();
    ArmorPartsComponent iron = ArmorPartsComponent.defaultParts();
    for (int i = 0; i < 4; i++) pieces.add(
      ArmoryStationMenu.createArmor(i, iron)
    );

    ItemStack station = new ItemStack(ModRegistries.ARMORY_STATION_ITEM.get());
    ItemStack iron4 = new ItemStack(Items.IRON_INGOT);

    List<Chapter> chapters = new ArrayList<>();
    chapters.add(
      chapter(
        "welcome",
        3,
        new Flow(List.of(station, ARROW, iron4, ARROW, pieces.get(1)))
      )
    );
    chapters.add(
      chapter(
        "parts",
        5,
        new Flow(
          List.of(
            new ItemStack(Items.OAK_LOG),
            new ItemStack(Items.LEATHER),
            new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.GOLD_INGOT)
          )
        )
      )
    );
    chapters.add(chapter("shapes", 4, null));
    chapters.add(chapter("costs", 6, new Flow(pieces)));
    chapters.add(chapter("tips", 5, null));
    return chapters;
  }

  private static Chapter chapter(String id, int paragraphs, Flow flow) {
    List<Element> elements = new ArrayList<>();
    for (int i = 1; i <= paragraphs; i++) {
      elements.add(
        new Text(Component.translatable(KEY + id + "." + i))
      );
      // the picture sits after the first paragraph
      if (i == 1 && flow != null) elements.add(flow);
    }
    return new Chapter(Component.translatable(KEY + id), elements);
  }
}

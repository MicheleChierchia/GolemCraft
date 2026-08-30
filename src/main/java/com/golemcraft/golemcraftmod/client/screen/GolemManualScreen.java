package com.golemcraft.golemcraftmod.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;

public class GolemManualScreen extends BookViewScreen {

    public GolemManualScreen() {
        super(new BookViewScreen.BookAccess(buildPages()));
    }

    @Override
    protected void createMenuControls() {
        int centerX = this.width / 2;
        int top = this.menuControlsTop();

        // Index button on the left
        this.addRenderableWidget(
            Button.builder(Component.translatable("book.golemcraft.index_button"), btn -> this.setPage(0))
                .pos(centerX - 102, top)
                .width(98)
                .build()
        );

        // Done button on the right
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                .pos(centerX + 4, top)
                .width(98)
                .build()
        );
    }

    private static List<Component> buildPages() {
        List<Component> pages = new ArrayList<>();

        // Page 1: Cover & Clickable Table of Contents
        MutableComponent page1 = Component.empty();
        page1.append(Component.translatable("book.golemcraft.page1.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        page1.append(Component.literal("\n"));
        page1.append(Component.translatable("book.golemcraft.page1.subtitle").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        page1.append(Component.literal("\n\n"));
        page1.append(Component.translatable("book.golemcraft.page1.intro").withStyle(ChatFormatting.BLACK));
        page1.append(Component.literal("\n\n"));
        page1.append(Component.translatable("book.golemcraft.page1.index_header").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        page1.append(Component.literal("\n"));

        page1.append(createIndexLink("book.golemcraft.page1.toc1", 2));
        page1.append(Component.literal("\n"));
        page1.append(createIndexLink("book.golemcraft.page1.toc2", 3));
        page1.append(Component.literal("\n"));
        page1.append(createIndexLink("book.golemcraft.page1.toc3", 4));
        page1.append(Component.literal("\n"));
        page1.append(createIndexLink("book.golemcraft.page1.toc4", 5));
        page1.append(Component.literal("\n"));
        page1.append(createIndexLink("book.golemcraft.page1.toc5", 12));
        page1.append(Component.literal("\n"));
        page1.append(createIndexLink("book.golemcraft.page1.toc6", 13));
        page1.append(Component.literal("\n"));
        page1.append(createIndexLink("book.golemcraft.page1.toc7", 15));
        pages.add(page1);

        // Pages 2 through 15
        for (int i = 2; i <= 15; i++) {
            MutableComponent page = Component.empty();
            page.append(Component.translatable("book.golemcraft.page" + i + ".title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            page.append(Component.literal("\n\n"));
            page.append(Component.translatable("book.golemcraft.page" + i + ".content").withStyle(ChatFormatting.BLACK));
            pages.add(page);
        }

        return pages;
    }

    private static Component createIndexLink(String translationKey, int targetPage1Indexed) {
        return Component.translatable(translationKey)
                .withStyle(style -> style
                        .withColor(ChatFormatting.DARK_BLUE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.ChangePage(targetPage1Indexed)));
    }
}

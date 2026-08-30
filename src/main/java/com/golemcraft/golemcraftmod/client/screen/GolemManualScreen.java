package com.golemcraft.golemcraftmod.client.screen;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.*;
import com.golemcraft.golemcraftmod.registry.ModBlocks;
import com.golemcraft.golemcraftmod.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class GolemManualScreen extends Screen {

    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/gui/golem_manual_gui.png");

    private static final int BOOK_WIDTH = 280;
    private static final int BOOK_HEIGHT = 180;

    private int currentPage = 0;
    private final List<ManualPage> pages = new ArrayList<>();
    private final Map<String, LivingEntity> entityCache = new HashMap<>();

    // Category Tabs: (NameKey, TargetPage, IconItem)
    private static final CategoryTab[] TABS = new CategoryTab[] {
        new CategoryTab("book.golemcraft.tab.basics", 0, new ItemStack(ModBlocks.BASE_GOLEM_ITEM.get())),
        new CategoryTab("book.golemcraft.tab.professions", 4, new ItemStack(Items.IRON_HOE)),
        new CategoryTab("book.golemcraft.tab.beacon", 11, new ItemStack(ModBlocks.GOLEM_BEACON_ITEM.get())),
        new CategoryTab("book.golemcraft.tab.recipes", 13, new ItemStack(Blocks.CRAFTING_TABLE)),
        new CategoryTab("book.golemcraft.tab.automation", 16, new ItemStack(ModBlocks.GOLEM_COMPASS_ITEM.get()))
    };

    private static int dummyEntityIdCounter = -10000;

    public GolemManualScreen() {
        super(Component.translatable("item.golemcraft.golem_manual"));
        this.initPages();
    }

    private void initPages() {
        pages.clear();

        // 0: Introduzione
        pages.add(new ManualPage(PageType.ENTITY, 0,
                "book.golemcraft.page1.title", "book.golemcraft.page1.subtitle", "book.golemcraft.page1.content",
                "entity.base", ModEntities.BASE_GOLEM.get(), ItemStack.EMPTY, 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.guide"));

        // 1: Il Base Golem
        pages.add(new ManualPage(PageType.ENTITY, 0,
                "book.golemcraft.page2.title", "book.golemcraft.page2.subtitle", "book.golemcraft.page2.content",
                "entity.base_clean", ModEntities.BASE_GOLEM.get(), new ItemStack(Items.COPPER_INGOT), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.companion"));

        // 2: Ossidazione & Cera
        pages.add(new ManualPage(PageType.ENTITY, 0,
                "book.golemcraft.page3.title", "book.golemcraft.page3.subtitle", "book.golemcraft.page3.content",
                "entity.base_weathered", ModEntities.BASE_GOLEM.get(), new ItemStack(Items.HONEYCOMB), 2, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.maintenance"));

        // 3: Statue & Sovraccarico
        pages.add(new ManualPage(PageType.ENTITY, 0,
                "book.golemcraft.page4.title", "book.golemcraft.page4.subtitle", "book.golemcraft.page4.content",
                "entity.base_charged", ModEntities.BASE_GOLEM.get(), ItemStack.EMPTY, 0, true, null, null, ItemStack.EMPTY, "book.golemcraft.badge.electric"));

        // 4: Golem Fiorista
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page5.title", "book.golemcraft.page5.subtitle", "book.golemcraft.page5.content",
                "entity.flower", ModEntities.FLOWER_GOLEM.get(), new ItemStack(Blocks.POPPY), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.gardener"));

        // 5: Golem Contadino
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page6.title", "book.golemcraft.page6.subtitle", "book.golemcraft.page6.content",
                "entity.farmer", ModEntities.FARMER_GOLEM.get(), new ItemStack(Items.IRON_HOE), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.farmer"));

        // 6: Golem Taglialegna
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page7.title", "book.golemcraft.page7.subtitle", "book.golemcraft.page7.content",
                "entity.lumberjack", ModEntities.LUMBERJACK_GOLEM.get(), new ItemStack(Items.IRON_AXE), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.lumberjack"));

        // 7: Golem Pescatore
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page8.title", "book.golemcraft.page8.subtitle", "book.golemcraft.page8.content",
                "entity.fisherman", ModEntities.FISHERMAN_GOLEM.get(), new ItemStack(Items.FISHING_ROD), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.fisherman"));

        // 8: Golem Soldato
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page9.title", "book.golemcraft.page9.subtitle", "book.golemcraft.page9.content",
                "entity.soldier", ModEntities.SOLDIER_GOLEM.get(), new ItemStack(Items.IRON_SWORD), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.soldier"));

        // 9: Golem degli Abissi
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page10.title", "book.golemcraft.page10.subtitle", "book.golemcraft.page10.content",
                "entity.depth", ModEntities.DEPTH_GOLEM.get(), new ItemStack(Items.ECHO_SHARD), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.warden"));

        // 10: Golem Esploratore
        pages.add(new ManualPage(PageType.ENTITY, 1,
                "book.golemcraft.page11.title", "book.golemcraft.page11.subtitle", "book.golemcraft.page11.content",
                "entity.explorer", ModEntities.EXPLORER_GOLEM.get(), new ItemStack(Items.RECOVERY_COMPASS), 0, false, null, null, ItemStack.EMPTY, "book.golemcraft.badge.explorer"));

        // 11: Il Faro dei Golem
        pages.add(new ManualPage(PageType.BLOCK_ITEM, 2,
                "book.golemcraft.page12.title", "book.golemcraft.page12.subtitle", "book.golemcraft.page12.content",
                null, null, ItemStack.EMPTY, 0, false, new ItemStack(ModBlocks.GOLEM_BEACON_ITEM.get()), null, ItemStack.EMPTY, "book.golemcraft.badge.beacon"));

        // 12: Poteri del Faro
        pages.add(new ManualPage(PageType.BLOCK_ITEM, 2,
                "book.golemcraft.page13.title", "book.golemcraft.page13.subtitle", "book.golemcraft.page13.content",
                null, null, ItemStack.EMPTY, 0, false, new ItemStack(Items.COPPER_BLOCK.weathering().unaffected()), null, ItemStack.EMPTY, "book.golemcraft.badge.powers"));

        // 13: Crafting Faro dei Golem
        ItemStack[] beaconGrid = new ItemStack[] {
                new ItemStack(Blocks.GLASS), new ItemStack(Blocks.GLASS), new ItemStack(Blocks.GLASS),
                new ItemStack(Blocks.GLASS), new ItemStack(Items.NETHER_STAR), new ItemStack(Blocks.GLASS),
                new ItemStack(Blocks.COPPER_BLOCK.weathering().unaffected()), new ItemStack(Blocks.COPPER_BLOCK.weathering().unaffected()), new ItemStack(Blocks.COPPER_BLOCK.weathering().unaffected())
        };
        pages.add(new ManualPage(PageType.CRAFTING, 3,
                "book.golemcraft.page14.title", "book.golemcraft.page14.subtitle", "book.golemcraft.page14.content",
                null, null, ItemStack.EMPTY, 0, false, null, beaconGrid, new ItemStack(ModBlocks.GOLEM_BEACON_ITEM.get()), "book.golemcraft.badge.crafting"));

        // 14: Crafting Manuale dei Golem
        ItemStack[] manualGrid = new ItemStack[] {
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.BOOK), new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        };
        pages.add(new ManualPage(PageType.CRAFTING, 3,
                "book.golemcraft.page15.title", "book.golemcraft.page15.subtitle", "book.golemcraft.page15.content",
                null, null, ItemStack.EMPTY, 0, false, null, manualGrid, new ItemStack(ModBlocks.GOLEM_MANUAL_ITEM.get()), "book.golemcraft.badge.crafting"));

        // 15: Crafting Bussola dei Golem
        ItemStack[] compassGrid = new ItemStack[] {
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COMPASS), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT)
        };
        pages.add(new ManualPage(PageType.CRAFTING, 3,
                "book.golemcraft.page16.title", "book.golemcraft.page16.subtitle", "book.golemcraft.page16.content",
                null, null, ItemStack.EMPTY, 0, false, null, compassGrid, new ItemStack(ModBlocks.GOLEM_COMPASS_ITEM.get()), "book.golemcraft.badge.crafting"));

        // 16: Bussola & Deposito Casse
        pages.add(new ManualPage(PageType.BLOCK_ITEM, 4,
                "book.golemcraft.page17.title", "book.golemcraft.page17.subtitle", "book.golemcraft.page17.content",
                null, null, ItemStack.EMPTY, 0, false, new ItemStack(Blocks.CHEST), null, ItemStack.EMPTY, "book.golemcraft.badge.automation"));

        // 17: Il Pennello & Consigli
        pages.add(new ManualPage(PageType.BLOCK_ITEM, 4,
                "book.golemcraft.page18.title", "book.golemcraft.page18.subtitle", "book.golemcraft.page18.content",
                null, null, ItemStack.EMPTY, 0, false, new ItemStack(Items.BRUSH), null, ItemStack.EMPTY, "book.golemcraft.badge.reset"));
    }

    private LivingEntity getOrCreateEntity(String cacheKey, net.minecraft.world.entity.EntityType<? extends LivingEntity> entityType, ItemStack heldItem, int oxidation, boolean charged) {
        return entityCache.computeIfAbsent(cacheKey, k -> {
            if (this.minecraft == null || this.minecraft.level == null) return null;
            LivingEntity entity = entityType.create(this.minecraft.level, EntitySpawnReason.LOAD);
            if (entity != null) {
                entity.setId(dummyEntityIdCounter--);
                if (entity instanceof BaseGolemEntity golem) {
                    golem.setOxidationLevel(oxidation);
                    golem.setCharged(charged);
                    if (!heldItem.isEmpty()) {
                        golem.setItemSlot(EquipmentSlot.MAINHAND, heldItem.copy());
                    }
                }
            }
            return entity;
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - BOOK_WIDTH) / 2;
        int top = (this.height - BOOK_HEIGHT) / 2;

        // 1. Draw Category Tabs
        ManualPage currentPageObj = pages.get(this.currentPage);
        Component hoveredTabTooltip = null;
        for (int i = 0; i < TABS.length; i++) {
            CategoryTab tab = TABS[i];
            int tabX = left + 20 + i * 48;
            int tabY = top - 17;
            boolean isHovered = mouseX >= tabX && mouseX < tabX + 40 && mouseY >= tabY && mouseY < tabY + 22;
            boolean isActive = currentPageObj.categoryIndex == i;

            int u = (isActive || isHovered) ? 50 : 0;
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, tabX, tabY, (float) u, 250.0F, 40, 22, 512, 512);

            // Tab Icon
            graphics.item(tab.icon, tabX + 12, tabY + 3);
            graphics.itemDecorations(this.font, tab.icon, tabX + 12, tabY + 3);

            if (isHovered) {
                hoveredTabTooltip = Component.translatable(tab.nameKey);
            }
        }

        // 2. Draw Main Book GUI Background
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, left, top, 0.0F, 0.0F, BOOK_WIDTH, BOOK_HEIGHT, 512, 512);

        ItemStack hoveredTooltipStack = null;

        // 3. Render Left Page Content
        int leftPageCenter = left + 73;
        if (currentPageObj.type == PageType.ENTITY) {
            // Draw Pedestal
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, leftPageCenter - 28, top + 124, 100.0F, 200.0F, 56, 22, 512, 512);

            // Badge Title above entity
            if (currentPageObj.badgeKey != null) {
                Component badge = Component.translatable(currentPageObj.badgeKey).withStyle(ChatFormatting.BOLD);
                graphics.centeredText(this.font, badge, leftPageCenter, top + 16, 0xFF7A1C00);
            }

            // Render 3D Entity
            LivingEntity entity = getOrCreateEntity(currentPageObj.entityCacheKey, currentPageObj.entityType, currentPageObj.heldItem, currentPageObj.oxidationLevel, currentPageObj.charged);
            if (entity != null) {
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        graphics,
                        left + 25, top + 34, left + 121, top + 138,
                        40,
                        0.0625F,
                        (float) mouseX, (float) mouseY,
                        entity
                );
            }

            // Sub-badge under pedestal
            if (!currentPageObj.heldItem.isEmpty()) {
                graphics.item(currentPageObj.heldItem, leftPageCenter - 8, top + 148);
                graphics.itemDecorations(this.font, currentPageObj.heldItem, leftPageCenter - 8, top + 148);
                if (mouseX >= leftPageCenter - 8 && mouseX < leftPageCenter + 8 && mouseY >= top + 148 && mouseY < top + 164) {
                    hoveredTooltipStack = currentPageObj.heldItem;
                }
            }

        } else if (currentPageObj.type == PageType.CRAFTING) {
            // Badge
            if (currentPageObj.badgeKey != null) {
                Component badge = Component.translatable(currentPageObj.badgeKey).withStyle(ChatFormatting.BOLD);
                graphics.centeredText(this.font, badge, leftPageCenter, top + 16, 0xFF125412);
            }

            int gridX = left + 14;
            int gridY = top + 46;

            // Render 3x3 Slots
            if (currentPageObj.craftingGrid != null) {
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        int slotX = gridX + c * 18;
                        int slotY = gridY + r * 18;
                        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, slotX, slotY, 0.0F, 200.0F, 18, 18, 512, 512);

                        int index = r * 3 + c;
                        if (index < currentPageObj.craftingGrid.length) {
                            ItemStack stack = currentPageObj.craftingGrid[index];
                            if (!stack.isEmpty()) {
                                graphics.item(stack, slotX + 1, slotY + 1);
                                graphics.itemDecorations(this.font, stack, slotX + 1, slotY + 1);

                                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                                    hoveredTooltipStack = stack;
                                }
                            }
                        }
                    }
                }
            }

            // Crafting Arrow
            int arrowX = gridX + 56;
            int arrowY = gridY + 19;
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, arrowX, arrowY, 65.0F, 200.0F, 22, 15, 512, 512);

            // Output Slot
            int outX = gridX + 80;
            int outY = gridY + 14;
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, outX, outY, 30.0F, 200.0F, 26, 26, 512, 512);

            if (!currentPageObj.craftingResult.isEmpty()) {
                graphics.item(currentPageObj.craftingResult, outX + 5, outY + 5);
                graphics.itemDecorations(this.font, currentPageObj.craftingResult, outX + 5, outY + 5);

                if (mouseX >= outX && mouseX < outX + 26 && mouseY >= outY && mouseY < outY + 26) {
                    hoveredTooltipStack = currentPageObj.craftingResult;
                }
            }

        } else if (currentPageObj.type == PageType.BLOCK_ITEM) {
            // Draw Pedestal
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, leftPageCenter - 28, top + 115, 100.0F, 200.0F, 56, 22, 512, 512);

            // Badge
            if (currentPageObj.badgeKey != null) {
                Component badge = Component.translatable(currentPageObj.badgeKey).withStyle(ChatFormatting.BOLD);
                graphics.centeredText(this.font, badge, leftPageCenter, top + 16, 0xFF7A3D00);
            }

            // 3D Item / Block rendered in slot area
            if (currentPageObj.displayItem != null && !currentPageObj.displayItem.isEmpty()) {
                graphics.item(currentPageObj.displayItem, leftPageCenter - 8, top + 65);
                graphics.itemDecorations(this.font, currentPageObj.displayItem, leftPageCenter - 8, top + 65);

                if (mouseX >= leftPageCenter - 16 && mouseX < leftPageCenter + 16 && mouseY >= top + 55 && mouseY < top + 95) {
                    hoveredTooltipStack = currentPageObj.displayItem;
                }
            }
        }

        // 4. Render Right Page Content
        int rightTextX = left + 148;
        int rightTextY = top + 15;
        int maxTextWidth = 114;

        // Title
        Component title = Component.translatable(currentPageObj.titleKey).withStyle(ChatFormatting.BOLD);
        graphics.text(this.font, title, rightTextX, rightTextY, 0xFF2A1202, false);

        // Subtitle
        Component subtitle = Component.translatable(currentPageObj.subtitleKey).withStyle(ChatFormatting.ITALIC);
        graphics.text(this.font, subtitle, rightTextX, rightTextY + 12, 0xFF5A2A0A, false);

        // Content
        FormattedText contentFormatted = Component.translatable(currentPageObj.contentKey);
        List<FormattedCharSequence> lines = this.font.split(contentFormatted, maxTextWidth);
        int textStartY = rightTextY + 26;
        for (int i = 0; i < Math.min(lines.size(), 11); i++) {
            graphics.text(this.font, lines.get(i), rightTextX, textStartY + i * 10, 0xFF111111, false);
        }

        // 5. Navigation Controls (< and > and Page Count)
        // Prev Button
        if (this.currentPage > 0) {
            boolean prevHover = mouseX >= left + 16 && mouseX < left + 34 && mouseY >= top + 156 && mouseY < top + 168;
            float prevU = prevHover ? 20.0F : 0.0F;
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, left + 16, top + 156, prevU, 230.0F, 18, 12, 512, 512);
        }

        // Next Button
        if (this.currentPage < pages.size() - 1) {
            boolean nextHover = mouseX >= left + 248 && mouseX < left + 266 && mouseY >= top + 156 && mouseY < top + 168;
            float nextU = nextHover ? 60.0F : 40.0F;
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, left + 248, top + 156, nextU, 230.0F, 18, 12, 512, 512);
        }

        // Page Indicator
        String pageNumberStr = (this.currentPage + 1) + " / " + pages.size();
        graphics.centeredText(this.font, Component.literal(pageNumberStr), rightTextX + maxTextWidth / 2, top + 158, 0xFF554433);

        // 6. Draw Item Tooltips & Tab Tooltips
        if (hoveredTooltipStack != null && !hoveredTooltipStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hoveredTooltipStack, mouseX, mouseY);
        } else if (hoveredTabTooltip != null) {
            graphics.setTooltipForNextFrame(this.font, hoveredTabTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDouble) {
        if (event.button() == 0) {
            int left = (this.width - BOOK_WIDTH) / 2;
            int top = (this.height - BOOK_HEIGHT) / 2;
            double mouseX = event.x();
            double mouseY = event.y();

            // 1. Check Category Tabs
            for (int i = 0; i < TABS.length; i++) {
                int tabX = left + 20 + i * 48;
                int tabY = top - 17;
                if (mouseX >= tabX && mouseX < tabX + 40 && mouseY >= tabY && mouseY < tabY + 22) {
                    this.setPage(TABS[i].targetPage);
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
                    }
                    return true;
                }
            }

            // 2. Check Prev Button
            if (this.currentPage > 0 && mouseX >= left + 16 && mouseX < left + 34 && mouseY >= top + 156 && mouseY < top + 168) {
                this.setPage(this.currentPage - 1);
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
                }
                return true;
            }

            // 3. Check Next Button
            if (this.currentPage < pages.size() - 1 && mouseX >= left + 248 && mouseX < left + 266 && mouseY >= top + 156 && mouseY < top + 168) {
                this.setPage(this.currentPage + 1);
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
                }
                return true;
            }
        }

        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        // Left Arrow
        if (event.key() == 263 && this.currentPage > 0) {
            this.setPage(this.currentPage - 1);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
            return true;
        }
        // Right Arrow
        if (event.key() == 262 && this.currentPage < pages.size() - 1) {
            this.setPage(this.currentPage + 1);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }

    public void setPage(int page) {
        this.currentPage = Math.max(0, Math.min(page, pages.size() - 1));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum PageType {
        ENTITY,
        CRAFTING,
        BLOCK_ITEM,
        OVERVIEW
    }

    public record CategoryTab(String nameKey, int targetPage, ItemStack icon) {}

    public record ManualPage(
            PageType type,
            int categoryIndex,
            String titleKey,
            String subtitleKey,
            String contentKey,
            String entityCacheKey,
            net.minecraft.world.entity.EntityType<? extends LivingEntity> entityType,
            ItemStack heldItem,
            int oxidationLevel,
            boolean charged,
            ItemStack displayItem,
            ItemStack[] craftingGrid,
            ItemStack craftingResult,
            String badgeKey
    ) {}
}
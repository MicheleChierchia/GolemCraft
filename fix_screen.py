import re

file_path = "/home/miky/IdeaProjects/GolemCraft/src/main/java/com/golemcraft/golemcraftmod/client/screen/GolemBeaconScreen.java"
with open(file_path, "r") as f:
    content = f.read()

# Add import for Hud
content = content.replace("import net.minecraft.client.Minecraft;", "import net.minecraft.client.Minecraft;\nimport net.minecraft.client.gui.Hud;")

# Replace renderVanillaEffectIcon body
old_method = """    private void renderVanillaEffectIcon(GuiGraphicsExtractor graphics,
                                          net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                          int x, int y) {
        // Hud è in net.minecraft.client.gui.components.AbstractWidget.Hud oppure in net.minecraft.client.gui.components.AbstractWidget
        // Non avendolo per certo, uso una fallback leggendo direttamente MobEffectTextureManager
        var sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.contents().name(), x, y, 16, 16);
    }"""

new_method = """    private void renderVanillaEffectIcon(GuiGraphicsExtractor graphics,
                                          net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                          int x, int y) {
        Identifier sprite = Hud.getMobEffectSprite(effect);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 18, 18);
    }"""

content = content.replace(old_method, new_method)

with open(file_path, "w") as f:
    f.write(content)

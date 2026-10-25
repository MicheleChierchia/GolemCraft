package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.TotemGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class TotemGolemRenderer extends BaseGolemRenderer {

    private static final Identifier TOTEM_TEXTURE = Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/totem_golem.png");

    public TotemGolemRenderer(EntityRendererProvider.Context context) {
        super(context,
              new com.golemcraft.golemcraftmod.client.model.BaseGolemModel(
                  context.bakeLayer(com.golemcraft.golemcraftmod.events.ClientEvents.TOTEM_GOLEM_LAYER)));
        this.addLayer(new TotemGolemEyesLayer(this));
    }

    @Override
    public void extractRenderState(com.golemcraft.golemcraftmod.entity.BaseGolemEntity entity,
                                   BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof TotemGolemEntity totem) {
            state.isWaiting = totem.isStaying();
            state.isGuarding = totem.isStaying();
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return TOTEM_TEXTURE;
    }
}

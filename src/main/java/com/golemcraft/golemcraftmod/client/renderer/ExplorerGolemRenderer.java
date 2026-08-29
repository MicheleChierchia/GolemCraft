package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.ExplorerGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class ExplorerGolemRenderer extends BaseGolemRenderer {

    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/explorer_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_explorer_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_explorer_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_explorer_golem.png")
    };

    public ExplorerGolemRenderer(EntityRendererProvider.Context context) {
        super(context,
              new com.golemcraft.golemcraftmod.client.model.BaseGolemModel(
                  context.bakeLayer(com.golemcraft.golemcraftmod.events.ClientEvents.EXPLORER_GOLEM_LAYER)));
        this.addLayer(new ExplorerGolemEyesLayer(this));
    }

    @Override
    public void extractRenderState(com.golemcraft.golemcraftmod.entity.BaseGolemEntity entity,
                                   BaseGolemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isExplorerGolem = true;
        if (entity instanceof ExplorerGolemEntity explorer) {
            state.isWaiting = explorer.isWaiting();
            state.isGuarding = explorer.isStaying();
        }
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}

package z1gned.goetyneorevelation.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.client.layer.WitherServantArmorLayer;
import z1gned.goetyneorevelation.client.model.WitherServantModel;
import z1gned.goetyneorevelation.entity.WitherServant;

public class WitherServantRenderer extends MobRenderer<WitherServant, WitherServantModel<WitherServant>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(GoetyNeoRevelation.LEGACY_NAMESPACE, "wither_servant"), "main");

    private static final ResourceLocation WITHER_INVULNERABLE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final ResourceLocation WITHER_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/wither/wither.png");

    public WitherServantRenderer(EntityRendererProvider.Context arg0) {
        super(arg0, new WitherServantModel<>(arg0.bakeLayer(LAYER)), 1.0F);
        this.addLayer(new WitherServantArmorLayer(this, arg0.getModelSet()));
    }

    protected int getBlockLightLevel(WitherServant arg0, BlockPos arg1) {
        return 15;
    }

    public ResourceLocation getTextureLocation(WitherServant arg0) {
        int i = arg0.getInvulnerableTicks();
        return i <= 0 || i <= 80 && i / 5 % 2 == 1 ? WITHER_LOCATION : WITHER_INVULNERABLE_LOCATION;
    }

    protected void scale(WitherServant arg0, PoseStack arg1, float arg2) {
        float f = 2.0F;
        int i = arg0.getInvulnerableTicks();
        if (i > 0) {
            f -= ((float) i - arg2) / 220.0F * 0.5F;
        }

        arg1.scale(f, f, f);
    }
}

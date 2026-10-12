package z1gned.goetyneorevelation.client.layer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import z1gned.goetyneorevelation.client.model.WitherServantModel;
import z1gned.goetyneorevelation.entity.WitherServant;

/**
 * 半血（powered）时的能量护甲叠加层，复用原版 {@link ModelLayers#WITHER_ARMOR} 模型。
 */
public class WitherServantArmorLayer extends EnergySwirlLayer<WitherServant, WitherServantModel<WitherServant>> {
    private static final ResourceLocation WITHER_ARMOR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    private final WitherServantModel<WitherServant> model;

    public WitherServantArmorLayer(RenderLayerParent<WitherServant, WitherServantModel<WitherServant>> arg0, EntityModelSet arg1) {
        super(arg0);
        this.model = new WitherServantModel<>(arg1.bakeLayer(ModelLayers.WITHER_ARMOR));
    }

    protected float xOffset(float arg0) {
        return Mth.cos(arg0 * 0.02F) * 3.0F;
    }

    protected ResourceLocation getTextureLocation() {
        return WITHER_ARMOR_LOCATION;
    }

    protected EntityModel<WitherServant> model() {
        return this.model;
    }
}

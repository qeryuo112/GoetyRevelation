package z1gned.goetyneorevelation.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import z1gned.goetyneorevelation.entity.WitherServant;

public class WitherServantModel<T extends WitherServant> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart centerHead;
    private final ModelPart rightHead;
    private final ModelPart leftHead;
    private final ModelPart ribcage;
    private final ModelPart tail;

    public WitherServantModel(ModelPart arg0) {
        this.root = arg0;
        this.ribcage = arg0.getChild("ribcage");
        this.tail = arg0.getChild("tail");
        this.centerHead = arg0.getChild("center_head");
        this.rightHead = arg0.getChild("right_head");
        this.leftHead = arg0.getChild("left_head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "shoulders",
            CubeListBuilder.create().texOffs(0, 16).addBox(-10.0F, 3.9F, -0.5F, 20.0F, 3.0F, 3.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        float f = 0.20420352F;
        partdefinition.addOrReplaceChild(
            "ribcage",
            CubeListBuilder.create()
                .texOffs(0, 22)
                .addBox(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(24, 22)
                .addBox(-4.0F, 1.5F, 0.5F, 11.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(24, 22)
                .addBox(-4.0F, 4.0F, 0.5F, 11.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(24, 22)
                .addBox(-4.0F, 6.5F, 0.5F, 11.0F, 2.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-2.0F, 6.9F, -0.5F, 0.20420352F, 0.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(12, 22).addBox(0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-2.0F, 6.9F + Mth.cos(0.20420352F) * 10.0F, -0.5F + Mth.sin(0.20420352F) * 10.0F, 0.83252203F, 0.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "center_head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        CubeListBuilder cubelistbuilder = CubeListBuilder.create()
            .texOffs(32, 0)
            .addBox(-4.0F, -4.0F, -4.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE);
        partdefinition.addOrReplaceChild("right_head", cubelistbuilder, PartPose.offset(-8.0F, 4.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_head", cubelistbuilder, PartPose.offset(10.0F, 4.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public ModelPart root() {
        return this.root;
    }

    public void setupAnim(T arg0, float arg1, float arg2, float arg3, float arg4, float arg5) {
        float f = Mth.cos(arg3 * 0.1F);
        this.ribcage.xRot = (0.065F + 0.05F * f) * 3.1415927F;
        this.tail.setPos(-2.0F, 6.9F + Mth.cos(this.ribcage.xRot) * 10.0F, -0.5F + Mth.sin(this.ribcage.xRot) * 10.0F);
        this.tail.xRot = (0.265F + 0.1F * f) * 3.1415927F;
        this.centerHead.yRot = arg4 * 0.017453292F;
        this.centerHead.xRot = arg5 * 0.017453292F;
    }

    public void prepareMobModel(T arg0, float arg1, float arg2, float arg3) {
        setupHeadRotation(arg0, this.rightHead, 0);
        setupHeadRotation(arg0, this.leftHead, 1);
    }

    private static <T extends WitherServant> void setupHeadRotation(T arg0, ModelPart arg1, int arg2) {
        arg1.yRot = (arg0.getHeadYRot(arg2) - arg0.yBodyRot) * 0.017453292F;
        arg1.xRot = arg0.getHeadXRot(arg2) * 0.017453292F;
    }
}

package com.thecascadian.universalarmor.client.model;

import com.thecascadian.universalarmor.UniversalArmorMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Heavy-shape armor: vanilla humanoid mesh plus crest, pauldrons, keel, toe caps and tassets. */
public class HeavyArmorModel extends HumanoidModel<LivingEntity> {

  public static final ModelLayerLocation OUTER = layer("heavy_armor_outer");
  public static final ModelLayerLocation INNER = layer("heavy_armor_inner");

  private static ModelLayerLocation layer(String name) {
    return new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath(UniversalArmorMod.MOD_ID, name),
      "main"
    );
  }

  public HeavyArmorModel(ModelPart root) {
    super(root);
  }

  public static LayerDefinition createOuter() {
    MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F);
    PartDefinition root = mesh.getRoot();

    PartDefinition head = root.getChild("head");
    head.addOrReplaceChild(
      "crest",
      CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, -11F, -5F, 1, 2, 10),
      PartPose.ZERO
    );
    head.addOrReplaceChild(
      "brow",
      CubeListBuilder.create().texOffs(96, 36).addBox(-5.5F, -6F, -6F, 11, 1, 1),
      PartPose.ZERO
    );

    root.getChild("body").addOrReplaceChild(
      "keel",
      CubeListBuilder.create().texOffs(96, 16).addBox(-1F, 1F, -4F, 2, 6, 1),
      PartPose.ZERO
    );

    addPauldrons(root.getChild("right_arm"), false);
    addPauldrons(root.getChild("left_arm"), true);

    root.getChild("right_leg").addOrReplaceChild(
      "toe",
      CubeListBuilder.create().texOffs(96, 28).addBox(-2.5F, 10F, -5F, 5, 3, 2),
      PartPose.ZERO
    );
    root.getChild("left_leg").addOrReplaceChild(
      "toe",
      CubeListBuilder.create().texOffs(96, 28).mirror().addBox(-2.5F, 10F, -5F, 5, 3, 2),
      PartPose.ZERO
    );
    return LayerDefinition.create(mesh, 128, 64);
  }

  private static void addPauldrons(PartDefinition arm, boolean left) {
    float x1 = left ? -2.5F : -4.5F;
    float x2 = left ? -3F : -5F;
    CubeListBuilder t1 = CubeListBuilder.create().texOffs(64, 16);
    CubeListBuilder t2 = CubeListBuilder.create().texOffs(64, 28);
    if (left) {
      t1.mirror();
      t2.mirror();
    }
    arm.addOrReplaceChild("paul1", t1.addBox(x1, -3.5F, -3.5F, 7, 3, 7), PartPose.ZERO);
    arm.addOrReplaceChild("paul2", t2.addBox(x2, -0.5F, -4F, 8, 2, 8), PartPose.ZERO);
  }

  public static LayerDefinition createInner() {
    MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0F);
    PartDefinition body = mesh.getRoot().getChild("body");
    body.addOrReplaceChild(
      "tasset_front",
      CubeListBuilder.create().texOffs(64, 48).addBox(-4.5F, 10.5F, -3.5F, 9, 4, 1),
      PartPose.ZERO
    );
    body.addOrReplaceChild(
      "tasset_back",
      CubeListBuilder.create().texOffs(64, 48).addBox(-4.5F, 10.5F, 2.5F, 9, 4, 1),
      PartPose.ZERO
    );
    return LayerDefinition.create(mesh, 128, 64);
  }
}

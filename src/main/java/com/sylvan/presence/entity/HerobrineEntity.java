package com.sylvan.presence.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.world.World;

public class HerobrineEntity {
	private World world;
	private ArmorStandEntity headArmsEntity;
	private ArmorStandEntity eyesEntity;
	private ArmorStandEntity bodyEntity;
	private ArmorStandEntity legsEntity;

	private static NbtCompound headBodyCompound = new NbtCompound();
	private static NbtCompound legsCompound = new NbtCompound();

	public static void initEntity() {
		NbtList armPoseValues = new NbtList();
		armPoseValues.add(NbtFloat.of(0.0f));
		armPoseValues.add(NbtFloat.of(0.0f));
		armPoseValues.add(NbtFloat.of(0.0f));

		NbtCompound armLegPoseCompound = new NbtCompound();
		armLegPoseCompound.put("LeftArm", armPoseValues);
		armLegPoseCompound.put("RightArm", armPoseValues);

		headBodyCompound.putBoolean("Invisible", true);
		headBodyCompound.putBoolean("Invulnerable", true);
		headBodyCompound.putBoolean("NoBasePlate", true);
		headBodyCompound.putBoolean("ShowArms", true);
		headBodyCompound.putInt("DisabledSlots", 2039583);
		headBodyCompound.put("Pose", armLegPoseCompound);

		legsCompound.putBoolean("Invisible", true);
		legsCompound.putBoolean("Invulnerable", true);
		legsCompound.putBoolean("NoBasePlate", true);
		legsCompound.putBoolean("ShowArms", true);
		legsCompound.putInt("DisabledSlots", 2039583);
		legsCompound.putBoolean("Small", true);
		legsCompound.put("Pose", armLegPoseCompound);
	}

	public HerobrineEntity(final World world, final BlockPos blockPos, final float yaw, final float pitch) {
		this.world = world;
		EulerAngle headRotation = new EulerAngle(pitch, yaw, 0.1f);

		ItemStack head = newModelItem(1350146);
		ItemStack eyes = newModelItem(1350147);
		ItemStack body = newModelItem(1350148);
		ItemStack leftArm = newModelItem(1350149);
		ItemStack rightArm = newModelItem(1350150);
		ItemStack leftLeg = newModelItem(1350151);
		ItemStack rightLeg = newModelItem(1350152);

		this.headArmsEntity = EntityType.ARMOR_STAND.create(world);
		headArmsEntity.readCustomDataFromNbt(headBodyCompound);
		headArmsEntity.equipStack(EquipmentSlot.HEAD, head);
		headArmsEntity.equipStack(EquipmentSlot.MAINHAND, rightArm);
		headArmsEntity.equipStack(EquipmentSlot.OFFHAND, leftArm);
		headArmsEntity.setNoGravity(true);
		headArmsEntity.refreshPositionAndAngles(blockPos, yaw, pitch);
		headArmsEntity.setHeadRotation(headRotation);

		this.eyesEntity = EntityType.ARMOR_STAND.create(world);
		eyesEntity.readCustomDataFromNbt(headBodyCompound);
		eyesEntity.equipStack(EquipmentSlot.HEAD, eyes);
		eyesEntity.setNoGravity(true);
		eyesEntity.refreshPositionAndAngles(blockPos, yaw, pitch);
		eyesEntity.setHeadRotation(headRotation);

		this.bodyEntity = EntityType.ARMOR_STAND.create(world);
		bodyEntity.readCustomDataFromNbt(headBodyCompound);
		bodyEntity.equipStack(EquipmentSlot.HEAD, body);
		bodyEntity.setNoGravity(true);
		bodyEntity.refreshPositionAndAngles(blockPos, yaw, pitch);

		this.legsEntity = EntityType.ARMOR_STAND.create(world);
		legsEntity.readCustomDataFromNbt(legsCompound);
		legsEntity.equipStack(EquipmentSlot.MAINHAND, rightLeg);
		legsEntity.equipStack(EquipmentSlot.OFFHAND, leftLeg);
		legsEntity.setNoGravity(true);
		legsEntity.refreshPositionAndAngles(blockPos, yaw, pitch);
	}

	private static ItemStack newModelItem(final int modelValue) {
		ItemStack itemStack = new ItemStack(Items.DISC_FRAGMENT_5);
		NbtCompound tag = itemStack.hasNbt() ? itemStack.getNbt() : new NbtCompound();
		tag.putInt("CustomModelData", modelValue);
		itemStack.setNbt(tag);
		return itemStack;
	}

	public void summon() {
		world.spawnEntity(headArmsEntity);
		world.spawnEntity(eyesEntity);
		world.spawnEntity(bodyEntity);
		world.spawnEntity(legsEntity);
	}

	public void remove() {
		headArmsEntity.remove(RemovalReason.DISCARDED);
		eyesEntity.remove(RemovalReason.DISCARDED);
		bodyEntity.remove(RemovalReason.DISCARDED);
		legsEntity.remove(RemovalReason.DISCARDED);
	}
}

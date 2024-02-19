package com.sylvan.presence.entity;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HerobrineEntity {
	private World world;
	private ArmorStandEntity headEntity;
	private ArmorStandEntity bodyEntity;
	private ArmorStandEntity armsEntity;
	private ArmorStandEntity legsEntity;

	private static final NbtCompound headBodyCompound = new NbtCompound();
	private static final NbtCompound legsCompound = new NbtCompound();
	public static final Map<String, Integer> skins = new HashMap<>();

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

		skins.put("classic", 100);
		skins.put("smile", 200);
	}

	private static ItemStack newModelItem(final int skinValue) {
		ItemStack itemStack = new ItemStack(Items.DISC_FRAGMENT_5);
		NbtCompound tag = itemStack.hasNbt() ? itemStack.getNbt() : new NbtCompound();
		tag.putInt("CustomModelData", skinValue);
		itemStack.setNbt(tag);
		return itemStack;
	}

	public HerobrineEntity(final World world, final String skin) {
		this.world = world;

		int skinId = skins.get("classic");
		if (skins.containsKey(skin)) {
			skinId = skins.get(skin);
		}

		ItemStack head = newModelItem(skinId);
		ItemStack body = newModelItem(skinId + 1);
		ItemStack leftArm = newModelItem(skinId + 2);
		ItemStack rightArm = newModelItem(skinId + 3);
		ItemStack leftLeg = newModelItem(skinId + 4);
		ItemStack rightLeg = newModelItem(skinId + 5);

		this.headEntity = EntityType.ARMOR_STAND.create(world);
		headEntity.readCustomDataFromNbt(headBodyCompound);
		headEntity.equipStack(EquipmentSlot.HEAD, head);
		headEntity.setNoGravity(true);

		this.bodyEntity = EntityType.ARMOR_STAND.create(world);
		bodyEntity.readCustomDataFromNbt(headBodyCompound);
		bodyEntity.equipStack(EquipmentSlot.HEAD, body);
		bodyEntity.setNoGravity(true);

		this.armsEntity = EntityType.ARMOR_STAND.create(world);
		armsEntity.readCustomDataFromNbt(headBodyCompound);
		armsEntity.equipStack(EquipmentSlot.MAINHAND, rightArm);
		armsEntity.equipStack(EquipmentSlot.OFFHAND, leftArm);
		armsEntity.setNoGravity(true);

		this.legsEntity = EntityType.ARMOR_STAND.create(world);
		legsEntity.readCustomDataFromNbt(legsCompound);
		legsEntity.equipStack(EquipmentSlot.MAINHAND, rightLeg);
		legsEntity.equipStack(EquipmentSlot.OFFHAND, leftLeg);
		legsEntity.setNoGravity(true);
	}

	public void summon() {
		world.spawnEntity(headEntity);
		world.spawnEntity(bodyEntity);
		world.spawnEntity(armsEntity);
		world.spawnEntity(legsEntity);
	}

	public void remove() {
		headEntity.remove(RemovalReason.DISCARDED);
		bodyEntity.remove(RemovalReason.DISCARDED);
		armsEntity.remove(RemovalReason.DISCARDED);
		legsEntity.remove(RemovalReason.DISCARDED);
	}

	public void setHeadRotation(final float pitch, final float yaw, final float roll) {
		EulerAngle headRotation = new EulerAngle(pitch, yaw, roll);
		headEntity.setHeadRotation(headRotation);
	}

	public void setBodyRotation(final float yaw) {
		EulerAngle bodyRotation = new EulerAngle(0.0f, yaw, 0.0f);
		bodyEntity.setHeadRotation(bodyRotation);
		armsEntity.setYaw(yaw);
		legsEntity.setYaw(yaw);
	}

	public void setPosition(final Vec3d pos) {
		headEntity.setPosition(pos);
		bodyEntity.setPosition(pos);
		armsEntity.setPosition(pos);
		legsEntity.setPosition(pos);
	}
}

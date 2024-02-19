package com.sylvan.presence.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HerobrineEntity {
	World world;
	ArmorStandEntity headArms;
	ArmorStandEntity eyes;
	ArmorStandEntity bodyLegs;

	public HerobrineEntity(final World world, final BlockPos blockPos, final float yaw, final float pitch) {
		this.world = world;

		ItemStack head = newModelItem(1350146);
		ItemStack eye = newModelItem(1350147);
		ItemStack body = newModelItem(1350148);
		ItemStack leftArm = newModelItem(1350149);
		ItemStack rightArm = newModelItem(1350150);
		ItemStack leftLeg = newModelItem(1350151);
		ItemStack rightLeg = newModelItem(1350152);

		this.headArms = EntityType.ARMOR_STAND.create(world);
		headArms.equipStack(EquipmentSlot.HEAD, head);
		headArms.equipStack(EquipmentSlot.MAINHAND, rightArm);
		headArms.equipStack(EquipmentSlot.OFFHAND, leftArm);
		headArms.refreshPositionAndAngles(blockPos, yaw, pitch);

		this.eyes = EntityType.ARMOR_STAND.create(world);
		eyes.equipStack(EquipmentSlot.HEAD, eye);
		eyes.refreshPositionAndAngles(blockPos, yaw, pitch);

		this.bodyLegs = EntityType.ARMOR_STAND.create(world);
		bodyLegs.equipStack(EquipmentSlot.CHEST, body);
		bodyLegs.equipStack(EquipmentSlot.MAINHAND, rightLeg);
		bodyLegs.equipStack(EquipmentSlot.OFFHAND, leftLeg);
		bodyLegs.refreshPositionAndAngles(blockPos, yaw, pitch);
	}

	private static ItemStack newModelItem(final int modelValue) {
		ItemStack itemStack = new ItemStack(Items.DISC_FRAGMENT_5);
		NbtCompound tag = itemStack.hasNbt() ? itemStack.getNbt() : new NbtCompound();
		tag.putInt("CustomModelData", modelValue);
		itemStack.setNbt(tag);
		return itemStack;
	}

	public void summon() {
		world.spawnEntity(headArms);
		world.spawnEntity(eyes);
		world.spawnEntity(bodyLegs);
	}

	public void remove() {
		headArms.remove(RemovalReason.DISCARDED);
		eyes.remove(RemovalReason.DISCARDED);
		bodyLegs.remove(RemovalReason.DISCARDED);
	}
}

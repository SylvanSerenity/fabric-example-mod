package com.sylvan.presence.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HerobrineEntity extends ArmorStandEntity {

	public HerobrineEntity(EntityType<? extends ArmorStandEntity> entityType, World world) {
		super(entityType, world);
		ItemStack head = new ItemStack(Items.PLAYER_HEAD);
		setCustomEquipment(head, head, head, head, head, head);
	}

	public void setCustomEquipment(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, ItemStack mainHand, ItemStack offHand) {
		this.equipStack(EquipmentSlot.HEAD, head);
		this.equipStack(EquipmentSlot.CHEST, chest);
		this.equipStack(EquipmentSlot.LEGS, legs);
		this.equipStack(EquipmentSlot.FEET, feet);
		this.equipStack(EquipmentSlot.MAINHAND, mainHand);
		this.equipStack(EquipmentSlot.OFFHAND, offHand);
	}

	public static ArmorStandEntity summon(final World world, final BlockPos blockPos, final float yaw, final float pitch) {
		ArmorStandEntity armorStand = EntityType.ARMOR_STAND.create(world);
		armorStand.refreshPositionAndAngles(blockPos, yaw, pitch);
		world.spawnEntity(armorStand);
		return armorStand;
	}
}

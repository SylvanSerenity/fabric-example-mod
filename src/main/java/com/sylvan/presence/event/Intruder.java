package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.TimeUnit;

public class Intruder {
    // Config
    public static boolean intruderEnabled = true;		    // Whether the intruder event is active
    private static float intruderHauntLevelMin = 1.75f;     // The minimum haunt level to play event
    private static int intruderDelayMin = 60 * 45;		    // The minimum delay between intruder events
    private static int intruderDelayMax = 60 * 60 * 3;	    // The maximum delay between intruder events
    private static int intruderRetryDelay = 60;	            // The delay between tries to execute intruder event
    private static int intruderBangFrequencyMs = 2000;      // The frequency of door bangs
    private static int intruderBangFrequencyVariance = 500; // The frequency of door bangs

    public static void loadConfig() {
        try {
            intruderEnabled = Presence.config.getOrSetValue("intruderEnabled", intruderEnabled).getAsBoolean();
            intruderHauntLevelMin = Presence.config.getOrSetValue("intruderHauntLevelMin", intruderHauntLevelMin).getAsFloat();
            intruderDelayMin = Presence.config.getOrSetValue("intruderDelayMin", intruderDelayMin).getAsInt();
            intruderDelayMax = Presence.config.getOrSetValue("intruderDelayMax", intruderDelayMax).getAsInt();
            intruderRetryDelay = Presence.config.getOrSetValue("intruderRetryDelay", intruderRetryDelay).getAsInt();
            intruderBangFrequencyMs = Presence.config.getOrSetValue("intruderBangFrequencyMs", intruderBangFrequencyMs).getAsInt();
            intruderBangFrequencyVariance = Presence.config.getOrSetValue("intruderBangFrequencyVariance", intruderBangFrequencyVariance).getAsInt();
        } catch (UnsupportedOperationException e) {
            Presence.LOGGER.error("Configuration issue for Intruder.java. Wiping and using default values.", e);
            Presence.config.wipe();
            Presence.initConfig();
        }
    }

    public static void scheduleEvent(final PlayerEntity player) {
        final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
        scheduleEventWithDelay(
                player,
                Algorithms.RANDOM.nextBetween(
                        Algorithms.divideByFloat(intruderDelayMin, hauntLevel),
                        Algorithms.divideByFloat(intruderDelayMax, hauntLevel)
                )
        );
    }

    public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
        final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
        Events.scheduler.schedule(
                () -> {
                    if (player.isRemoved()) return;
                    if (intrude(player, false)) {
                        scheduleEventWithDelay(
                                player,
                                Algorithms.RANDOM.nextBetween(
                                        Algorithms.divideByFloat(intruderDelayMin, hauntLevel),
                                        Algorithms.divideByFloat(intruderDelayMax, hauntLevel)
                                )
                        );
                    } else {
                        // Retry if it is a bad time
                        scheduleEventWithDelay(player, intruderRetryDelay);
                    }
                },
                delay, TimeUnit.SECONDS
        );
    }

    public static boolean intrude(final PlayerEntity player, final boolean overrideHauntLevel) {
        // Stop when player leaves or is no longer AFK
        if (player.isRemoved() || !PlayerData.getPlayerData(player).isAFK()) return false;
        if (!overrideHauntLevel) {
            final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
            if (hauntLevel < intruderHauntLevelMin) return true; // Reset event as if it passed
        }


        // Get nearest door position
        BlockPos nearestDoorPos = Algorithms.getNearestBlockToEntity(player, OpenDoor.doorBlocks, OpenDoor.openDoorSearchRadius);
        if (nearestDoorPos == null) return false;

        // Get bang sound
        final SoundEvent bangSound =
                (player.getEntityWorld().getBlockState(nearestDoorPos).getSoundGroup() == BlockSoundGroup.METAL) ?
                SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR :
                SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR
        ;

        playBangSound(player, nearestDoorPos, bangSound);
        return true;
    }

    private static void playBangSound(final PlayerEntity player, final BlockPos soundPos, final SoundEvent bangSound) {
        // Stop when player leaves or is no longer AFK
        if (player.isRemoved() || !PlayerData.getPlayerData(player).isAFK()) return;

        // Play the door bang sound
        player.getEntityWorld().playSound(null, soundPos, bangSound, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Continue while player is AFK
        Events.scheduler.schedule(
                () -> playBangSound(player, soundPos, bangSound),
                Algorithms.RANDOM.nextBetween(
                        intruderBangFrequencyMs - intruderBangFrequencyVariance,
                        intruderBangFrequencyMs + intruderBangFrequencyVariance
                ),
                TimeUnit.MILLISECONDS
        );
    }
}

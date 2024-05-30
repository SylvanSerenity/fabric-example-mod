package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.concurrent.TimeUnit;

public class Lightning {
    public static boolean lightningEnabled = true;		    	// Whether the lightning event is active
    private static float lightningHauntLevelMin = 1.5f; 		// The minimum haunt level to play event
    private static int lightningDelayMin = 60 * 60 * 2;		    // The minimum delay between lightning events
    private static int lightningDelayMax = 60 * 60 * 5;	    	// The maximum delay between lightning events
    private static int lightningRetryDelay = 180;	    		// The delay between retrying lightning event if the previous attempt failed
    private static int lightningDistanceMin = 0;    			// The minimum distance of the lightning strike from the player
    private static int lightningDistanceMax = 10;			    // The maximum distance of the lightning strike from the player
    private static boolean lightningStormConstraint = true;     // Whether there must be a thunderstorm for the lightning event to pass
    private static boolean lightningOutsideConstraint = true;   // Whether the player must be outside for the lightning event to pass

    public static void loadConfig() {
        try {
            lightningEnabled = Presence.config.getOrSetValue("lightningEnabled", lightningEnabled).getAsBoolean();
            lightningHauntLevelMin = Presence.config.getOrSetValue("lightningHauntLevelMin", lightningHauntLevelMin).getAsFloat();
            lightningDelayMin = Presence.config.getOrSetValue("lightningDelayMin", lightningDelayMin).getAsInt();
            lightningDelayMax = Presence.config.getOrSetValue("lightningDelayMax", lightningDelayMax).getAsInt();
            lightningRetryDelay = Presence.config.getOrSetValue("lightningRetryDelay", lightningRetryDelay).getAsInt();
            lightningDistanceMin = Presence.config.getOrSetValue("lightningDistanceMin", lightningDistanceMin).getAsInt();
            lightningDistanceMax = Presence.config.getOrSetValue("lightningDistanceMax", lightningDistanceMax).getAsInt();
            lightningStormConstraint = Presence.config.getOrSetValue("lightningStormConstraint", lightningStormConstraint).getAsBoolean();
            lightningOutsideConstraint = Presence.config.getOrSetValue("lightningOutsideConstraint", lightningOutsideConstraint).getAsBoolean();
        } catch (UnsupportedOperationException e) {
            Presence.LOGGER.error("Configuration issue for Lightning.java. Wiping and using default values.", e);
            Presence.config.wipe();
            Presence.initConfig();
        }
    }

    public static void scheduleEvent(final PlayerEntity player) {
        final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
        scheduleEventWithDelay(
                player,
                Algorithms.RANDOM.nextBetween(
                        Algorithms.divideByFloat(lightningDelayMin, hauntLevel),
                        Algorithms.divideByFloat(lightningDelayMax, hauntLevel)
                )
        );
    }

    public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
        final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
        Events.scheduler.schedule(
                () -> {
                    if (player.isRemoved()) return;
                    if (strike(player, false)) {
                        scheduleEventWithDelay(
                                player,
                                Algorithms.RANDOM.nextBetween(
                                        Algorithms.divideByFloat(lightningDelayMin, hauntLevel),
                                        Algorithms.divideByFloat(lightningDelayMax, hauntLevel)
                                )
                        );
                    } else {
                        // Retry if it is a bad time
                        scheduleEventWithDelay(player, lightningRetryDelay);
                    }
                },
                delay, TimeUnit.SECONDS
        );
    }

    public static boolean strike(final PlayerEntity player, final boolean overrideHauntLevel) {
        if (player.isRemoved()) return false;
        if (!overrideHauntLevel) {
            final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
            if (hauntLevel < lightningHauntLevelMin) return true; // Reset event as if it passed
        }

        final World world = player.getEntityWorld();
        if (
                (lightningStormConstraint && !world.isThundering()) ||    // Must be thundering
                (lightningOutsideConstraint && !Algorithms.isEntityOutside(player)) // Player must be outside
        ) return false;

        LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        final Vec3d startPos = Algorithms.getRandomPosNearEntity(player, lightningDistanceMin, lightningDistanceMax, false);
        final BlockPos spawnPos = Algorithms.getTopBlock(world, Algorithms.getBlockPosFromVec3d(startPos), true);
        bolt.refreshPositionAfterTeleport(spawnPos.toCenterPos());
        world.spawnEntity(bolt);
        return true;
    }
}

package com.sylvan.presence.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AFKSounds {
    private static final Map<SoundEvent, Float> afkSounds = new HashMap<>();

    // Config
    public static boolean afkSoundsEnabled = true;			// Whether the AFK sounds event is active
    private static float afkSoundsHauntLevelMin = 1.25f;	// The minimum haunt level to play event
    private static int afkSoundsDelayMin = 60 * 60;			// The minimum delay between AFK sounds
    private static int afkSoundsDelayMax = 60 * 60 * 3;		// The maximum delay between AFK sounds
    private static int afkSoundsRetryDelay = 60;			// The delay between tries to play AFK sound
    private static boolean afkSoundsRepeat = true;          // Whether to repeat the sound
    private static int afkSoundsFrequencyMs = 3000;			// The maximum sound pitch
    private static JsonObject afkSoundsSoundWeights = new JsonObject();	// A set of sound ID keys with weight values to play during the event

    public static void loadConfig() {
        afkSoundsSoundWeights.addProperty(SoundEvents.ENTITY_WARDEN_HEARTBEAT.getId().toString(), 50.0f);
        afkSoundsSoundWeights.addProperty(SoundEvents.BLOCK_BELL_USE.getId().toString(), 25.0f);
        afkSoundsSoundWeights.addProperty(SoundEvents.ENTITY_CAT_PURR.getId().toString(), 25.0f);

        try {
            afkSoundsEnabled = Presence.config.getOrSetValue("afkSoundsEnabled", afkSoundsEnabled).getAsBoolean();
            afkSoundsHauntLevelMin = Presence.config.getOrSetValue("afkSoundsHauntLevelMin", afkSoundsHauntLevelMin).getAsFloat();
            afkSoundsDelayMin = Presence.config.getOrSetValue("afkSoundsDelayMin", afkSoundsDelayMin).getAsInt();
            afkSoundsDelayMax = Presence.config.getOrSetValue("afkSoundsDelayMax", afkSoundsDelayMax).getAsInt();
            afkSoundsRetryDelay = Presence.config.getOrSetValue("afkSoundsRetryDelay", afkSoundsRetryDelay).getAsInt();
            afkSoundsRepeat = Presence.config.getOrSetValue("afkSoundsRepeat", afkSoundsRepeat).getAsBoolean();
            afkSoundsFrequencyMs = Presence.config.getOrSetValue("afkSoundsFrequencyMs", afkSoundsFrequencyMs).getAsInt();
            afkSoundsSoundWeights = Presence.config.getOrSetValue("afkSoundsSoundWeights", afkSoundsSoundWeights).getAsJsonObject();
        } catch (UnsupportedOperationException e) {
            Presence.LOGGER.error("Configuration issue for AFKSounds.java. Wiping and using default values.", e);
            Presence.config.wipe();
            Presence.initConfig();
        }
    }

    public static void initEvent() {
        try {
            String key;
            for (Map.Entry<String, JsonElement> entry : afkSoundsSoundWeights.entrySet()) {
                key = entry.getKey();
                final Identifier soundId = Algorithms.getIdentifierFromString(key);
                final SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
                if (sound == null) {
                    Presence.LOGGER.warn("Could not find sound \"" + key + "\" in AFKSounds.java.");
                    continue;
                }
                afkSounds.put(sound, entry.getValue().getAsFloat());
            }
        } catch (UnsupportedOperationException e) {
            Presence.LOGGER.error("Configuration issue for AFKSounds.java. Wiping and using default values.", e);
            Presence.config.wipe();
            Presence.initConfig();
            Events.initEvents();
        }
    }

    public static void scheduleEvent(final PlayerEntity player) {
        final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
        scheduleEventWithDelay(
                player,
                Algorithms.RANDOM.nextBetween(
                        Algorithms.divideByFloat(afkSoundsDelayMin, hauntLevel),
                        Algorithms.divideByFloat(afkSoundsDelayMax, hauntLevel)
                )
        );
    }

    public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
        final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
        Events.scheduler.schedule(
                () -> {
                    if (player.isRemoved()) return;
                    if (playAFKSound(player, false)) {
                        scheduleEventWithDelay(
                            player,
                            Algorithms.RANDOM.nextBetween(
                                    Algorithms.divideByFloat(afkSoundsDelayMin, hauntLevel),
                                    Algorithms.divideByFloat(afkSoundsDelayMax, hauntLevel)
                            )
                        );
                    } else {
                        // Retry if it is a bad time
                        scheduleEventWithDelay(player, afkSoundsRetryDelay);
                    }
                },
                delay, TimeUnit.SECONDS
        );
    }

    public static boolean playAFKSound(final PlayerEntity player, final boolean overrideHauntLevel) {
        // Stop when player leaves or is no longer AFK
        if (player.isRemoved() || !PlayerData.getPlayerData(player).isAFK()) return false;
        if (!overrideHauntLevel) {
            final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
            if (hauntLevel < afkSoundsHauntLevelMin) return true; // Reset event as if it passed
        }

        final SoundEvent sound = Algorithms.randomKeyFromWeightMap(afkSounds);
        continueAFKSounds(player, sound);
        return true;
    }

    private static void continueAFKSounds(final PlayerEntity player, final SoundEvent sound) {
        // Stop when player leaves or is no longer AFK
        if (player.isRemoved() || !PlayerData.getPlayerData(player).isAFK()) return;

        // Play the sound
        player.playSoundToPlayer(sound, SoundCategory.PLAYERS, 128.0f, 0.5f);

        // Continue while player is AFK
        Events.scheduler.schedule(
                () -> continueAFKSounds(player, sound),
                afkSoundsFrequencyMs,
                TimeUnit.MILLISECONDS
        );
    }
}

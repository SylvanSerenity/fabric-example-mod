package com.sylvan.presence.entity;

import com.sylvan.presence.event.Mine;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class MiningEntity {
    enum MiningState {
        MINING,
        NEW_BLOCK
    };

    private final PlayerEntity trackedPlayer;
    private final World world;
    private BlockPos breakPos;
    private int breakProgress = 1;
    private int ticksSinceBreakProgress = 0;
    private int ticksSinceStartBreaking = 0;
    private int breakCount;
    private boolean shouldRemove = false;
    private MiningState miningState = MiningState.MINING;


    public MiningEntity(final PlayerEntity trackedPlayer, final BlockPos startPos, final int breakCount) {
        this.trackedPlayer = trackedPlayer;
        this.breakPos = startPos;
        this.breakCount = breakCount;
        world = trackedPlayer.getEntityWorld();
    }

    public void tick() {
        if (shouldRemove) return;
        if (
                (breakCount <= 0) ||
                !Algorithms.isEntityInCave(trackedPlayer) ||
                !Algorithms.isCaveBlockSound(world.getBlockState(breakPos).getSoundGroup()) ||
                Algorithms.couldBlockBeSeenByPlayers(world.getPlayers(), breakPos)
        ) {
            world.setBlockBreakingInfo(0, breakPos, 0);
            shouldRemove = true;
            return;
        }

        switch (miningState) {
            case MINING -> mineBlock();
            case NEW_BLOCK -> newBlock();
        }
    }

    private void breakBlock() {
        world.setBlockBreakingInfo(0, breakPos, 0);
        if (Mine.mineBreakBlock) world.breakBlock(breakPos, Mine.mineLootBlock);
        else {
            world.playSound(null, breakPos, world.getBlockState(breakPos).getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
        ticksSinceStartBreaking = 0;
        breakProgress = 1;
        --breakCount;
        miningState = MiningState.NEW_BLOCK;
    }

    private void mineBlock() {
        if (ticksSinceBreakProgress >= Mine.mineTicksPerBreakProgress) {
            ticksSinceBreakProgress = 0;
            if (breakProgress > 10) {
                breakBlock();
                return;
            }

            // Make break progress
            world.setBlockBreakingInfo(0, breakPos, breakProgress);
            ++breakProgress;
        } else {
            // Wait
            ++ticksSinceBreakProgress;
        }

        ++ticksSinceStartBreaking;
        if (ticksSinceStartBreaking % 4 == 0) {
            world.playSound(null, breakPos, world.getBlockState(breakPos).getSoundGroup().getHitSound(), SoundCategory.BLOCKS, 1.0f, 0.5f);
        }
    }

    private void newBlock() {
        // Update breakPos and begin breaking next block
        final Direction towardsPlayer = Algorithms.getBlockDirectionFromEntity(trackedPlayer, breakPos).getOpposite();
        breakPos = breakPos.offset(towardsPlayer);
        miningState = MiningState.MINING;
    }

    public PlayerEntity getTrackedPlayer() {
        return trackedPlayer;
    }

    public World getWorld() { return world; }

    public boolean shouldRemove() { return shouldRemove; }
}

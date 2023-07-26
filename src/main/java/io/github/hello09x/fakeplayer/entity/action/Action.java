package io.github.hello09x.fakeplayer.entity.action;

import io.github.hello09x.fakeplayer.util.Tracer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.*;

public enum Action {

    USE {
        @Override
        @SuppressWarnings("resource")
        public boolean tick(@NotNull ActionPack ap) {
            if (ap.itemUseFreeze > 0) {
                ap.itemUseFreeze--;
                return false;
            }

            var player = ap.player;
            if (player.isUsingItem()) {
                return true;
            }

            var hit = getTarget(player);
            for (var hand : InteractionHand.values()) {
                switch (hit.getType()) {
                    case BLOCK -> {
                        player.resetLastActionTime();
                        var world = player.serverLevel();
                        var blockHit = (BlockHitResult) hit;
                        var pos = blockHit.getBlockPos();
                        var side = blockHit.getDirection();
                        if (pos.getY() < player.level().getMaxBuildHeight() - (side == Direction.UP ? 1 : 0) && world.mayInteract(player, pos)) {
                            var result = player.gameMode.useItemOn(player, world, player.getItemInHand(hand), hand, blockHit);
                            if (result.consumesAction()) {
                                player.swing(hand);
                                ap.itemUseFreeze = 3;
                                return true;
                            }
                        }
                    }
                    case ENTITY -> {
                        player.resetLastActionTime();
                        var entityHit = (EntityHitResult) hit;
                        var entity = entityHit.getEntity();
                        boolean handWasEmpty = player.getItemInHand(hand).isEmpty();
                        boolean itemFrameEmpty = (entity instanceof ItemFrame) && ((ItemFrame) entity).getItem().isEmpty();
                        var pos = entityHit.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
                        if (entity.interactAt(player, pos, hand).consumesAction()) {
                            ap.itemUseFreeze = 3;
                            return true;
                        }
                        if (player.interactOn(entity, hand).consumesAction() && !(handWasEmpty && itemFrameEmpty)) {
                            ap.itemUseFreeze = 3;
                            return true;
                        }
                    }
                }
                var handItem = player.getItemInHand(hand);
                if (player.gameMode.useItem(player, player.level(), handItem, hand).consumesAction()) {
                    ap.itemUseFreeze = 3;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void stop(@NotNull ActionPack ap) {
            ap.itemUseFreeze = 0;
            ap.player.releaseUsingItem();
        }
    },

    ATTACK {
        @Override
        @SuppressWarnings("resource")
        public boolean tick(@NotNull ActionPack ap) {
            var player = ap.player;
            var hit = getTarget(player);
            switch (hit.getType()) {
                case ENTITY -> {
                    var entityHit = (EntityHitResult) hit;
                    player.attack(entityHit.getEntity());
                    player.swing(InteractionHand.MAIN_HAND);
                    player.resetAttackStrengthTicker();
                    player.resetLastActionTime();
                    return true;
                }
                case BLOCK -> {
                    if (ap.blockHitFreeze > 0) {
                        ap.blockHitFreeze--;
                        return false;
                    }

                    var blockHit = (BlockHitResult) hit;
                    var pos = blockHit.getBlockPos();
                    var side = blockHit.getDirection();

                    if (player.blockActionRestricted(player.level(), pos, player.gameMode.getGameModeForPlayer())) {
                        return false;
                    }

                    if (ap.curBlockPos != null && player.level().getBlockState(ap.curBlockPos).isAir()) {
                        ap.curBlockPos = null;
                        return false;
                    }

                    var state = player.level().getBlockState(pos);
                    var broken = false;
                    if (player.gameMode.getGameModeForPlayer().isCreative()) {
                        player.gameMode.handleBlockBreakAction(
                                pos,
                                START_DESTROY_BLOCK,
                                side,
                                player.level().getMaxBuildHeight(),
                                -1
                        );
                        ap.blockHitFreeze = 5;
                    } else if (ap.curBlockPos == null || !ap.curBlockPos.equals(pos)) {
                        if (ap.curBlockPos != null) {
                            player.gameMode.handleBlockBreakAction(
                                    ap.curBlockPos,
                                    ABORT_DESTROY_BLOCK,
                                    side,
                                    player.level().getMaxBuildHeight(),
                                    -1
                            );
                        }

                        player.gameMode.handleBlockBreakAction(
                                pos,
                                START_DESTROY_BLOCK,
                                side,
                                player.level().getMaxBuildHeight(),
                                -1
                        );

                        if (!state.isAir() && ap.curBlockPgs == 0) {
                            state.attack(player.level(), pos, player);
                        }

                        if (!state.isAir() && state.getDestroyProgress(player, player.level(), pos) >= 1) {
                            ap.curBlockPos = null;
                            broken = true;
                        } else {
                            ap.curBlockPos = pos;
                            ap.curBlockPgs = 0;
                        }
                    } else {
                        ap.curBlockPgs += state.getDestroyProgress(player, player.level(), pos);
                        if (ap.curBlockPgs >= 1) {
                            player.gameMode.handleBlockBreakAction(
                                    pos,
                                    STOP_DESTROY_BLOCK,
                                    side,
                                    player.level().getMaxBuildHeight(),
                                    -1
                            );
                            ap.curBlockPos = null;
                            ap.blockHitFreeze = 5;
                            broken = true;
                        }
                        player.level().destroyBlockProgress(-1, pos, (int) (ap.curBlockPgs * 10));
                    }

                    player.resetLastActionTime();
                    player.swing(InteractionHand.MAIN_HAND);
                    return broken;
                }
            }
            return false;
        }

        @Override
        public void stop(@NotNull ActionPack ap) {
            if (ap.curBlockPos == null) {
                return;
            }

            var player = ap.player;
            player.level().destroyBlockProgress(-1, ap.curBlockPos, -1);
            player.gameMode.handleBlockBreakAction(
                    ap.curBlockPos,
                    ABORT_DESTROY_BLOCK,
                    Direction.DOWN,
                    player.level().getMaxBuildHeight(),
                    -1
            );
            ap.curBlockPos = null;
            ap.blockHitFreeze = 0;
            ap.curBlockPgs = 0;
        }
    },


    DROP_ITEM {
        @Override
        public boolean tick(@NotNull ActionPack ap) {
            var player = ap.player;
            player.resetLastActionTime();
            player.drop(false);
            return true;
        }
    },

    DROP_STACK {
        @Override
        public boolean tick(@NotNull ActionPack ap) {
            var player = ap.player;
            player.resetLastActionTime();
            player.drop(true);
            return true;
        }
    },

    DROP_INVENTORY {
        @Override
        public boolean tick(@NotNull ActionPack ap) {
            var player = ap.player;
            dropInventory(player);
            return true;
        }
    };


    static HitResult getTarget(ServerPlayer player) {
        double reach = player.gameMode.isCreative() ? 5 : 4.5f;
        return Tracer.rayTrace(player, 1, reach, false);
    }

    public static void dropInventory(@NotNull ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = inventory.getContainerSize(); i >= 0; i--) {
            player.drop(inventory.removeItem(i, inventory.getItem(i).getCount()), false, true);
        }
    }

    public abstract boolean tick(@NotNull ActionPack ap);

    public void stop(@NotNull ActionPack ap) {}

    public void inactiveTick(@NotNull ActionPack ap) {
        this.stop(ap);
    }

    public static class ActionPack {

        public final ServerPlayer player;

        // attack
        public BlockPos curBlockPos;
        public float curBlockPgs;
        public int blockHitFreeze;


        // use
        public int itemUseFreeze;

        public ActionPack(ServerPlayer player) {
            this.player = player;
        }

    }


}

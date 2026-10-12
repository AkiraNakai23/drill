package dev.example.industrialdrill;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Simple rideable drill vehicle. Uses vanilla entity riding controls:
 * W/S move, A/D steer. Mining is server-side and only happens while a player
 * is riding and the vehicle is moving forward.
 */
public class DrillEntity extends Entity {
    private int mineCooldown = 0;
    private float throttle = 0;

    public DrillEntity(EntityType<? extends DrillEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        mineCooldown = tag.getInt("MineCooldown");
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("MineCooldown", mineCooldown);
    }

    @Override
    public void tick() {
        super.tick();
        if (mineCooldown > 0) mineCooldown--;
        if (level().isClientSide()) return;

        Entity rider = getFirstPassenger();
        if (rider instanceof Player player) {
            float forward = player.zza;
            float sideways = player.xxa;
            if (forward < 0) forward *= 0.45F;
            setYRot(player.getYRot());
            setXRot(0);
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            double speed = forward > 0 ? 0.28 : 0.12;
            Vec3 movement = look.scale(forward * speed);
            setDeltaMovement(movement.x, getDeltaMovement().y, movement.z);
            if (sideways != 0) setYRot(getYRot() - sideways * 2.5F);
            if (forward > 0 && mineCooldown == 0) {
                mineAhead((ServerLevel) level(), player);
                mineCooldown = 4;
            }
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.7, 1, 0.7));
        }
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        if (onGround()) setDeltaMovement(getDeltaMovement().x, 0, getDeltaMovement().z);
    }

    private void mineAhead(ServerLevel world, Player player) {
        Vec3 front = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.8));
        BlockPos center = BlockPos.containing(front.x, front.y + 0.4, front.z);
        List<BlockPos> targets = new ArrayList<>();
        // 3-wide by 2-high tunnel face, one block deep per mining pulse.
        Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
        for (int w = -1; w <= 1; w++) {
            for (int h = 0; h <= 1; h++) {
                BlockPos pos = BlockPos.containing(
                    center.getX() + right.x * w,
                    center.getY() + h,
                    center.getZ() + right.z * w
                );
                if (!targets.contains(pos)) targets.add(pos);
            }
        }
        for (BlockPos pos : targets) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(world, pos) < 0) continue;
            if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL) ||
                state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.NETHER_PORTAL) ||
                state.is(Blocks.BARRIER) || state.is(Blocks.REINFORCED_DEEPSLATE)) continue;
            if (!world.mayInteract(player, pos)) continue;
            if (!world.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) continue;
            // Respect the block's normal loot table; drops are spawned into the world.
            if (!world.destroyBlock(pos, true, player)) continue;
        }
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(IndustrialDrillMod.DRILL_KIT);
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected boolean canRide(Entity vehicle) { return false; }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (hasPassenger(passenger)) {
            double x = getX();
            double y = getY() + 1.25;
            double z = getZ();
            moveFunction.accept(passenger, x, y, z);
        }
    }

    public static void tickWorld(Level level) {
        // Reserved for future chunk/world maintenance.
    }
}

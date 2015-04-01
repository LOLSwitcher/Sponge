/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.world;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Optional;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.mod.SpongeMod;

public class SpongeTeleportHelper implements TeleportHelper {

    /** The default height radius to scan for safe locations */
    static int DEFAULT_HEIGHT = 3;
    /** The default width radius to scan for safe locations */
    static int DEFAULT_WIDTH = 9;

    @Override
    public Optional<Location> getSafeLocation(Location location) {
        return getSafeLocation(location, DEFAULT_HEIGHT, DEFAULT_WIDTH);
    }

    @Override
    public Optional<Location> getSafeLocation(Location location, final int height, final int width) {
        // Check around the player first in a configurable radius:
        // TODO: Make this configurable
        Optional<Location> safe = checkAboveAndBelowLocation(location, height, width);
        if (safe.isPresent()) {
            SpongeMod.instance.getLogger().info("Found a safe location: " + safe.get().getBlockPosition()); // TODO plugin.getLocationManipulation().strCoordsRaw(safe));
            World world = (World) location.getExtent();
            BlockType block = world.getBlockType(safe.get().getBlockPosition());
            BlockType blockAbove = world.getBlockType(safe.get().getBlockPosition().add(Vector3i.UP));
            BlockType blockBelow = world.getBlockType(safe.get().getBlockPosition().sub(0, 1, 0));
            System.out.println("Safe blocks : [CENTER: " + block.getId() + "][UP: " + blockAbove.getId() + "][DOWN: " + blockBelow.getId());
            return Optional.of(getBlockCenteredCoordinates(safe.get()));
        } else {
            SpongeMod.instance.getLogger().info("Uh oh! No safe location found!");
            return null;
        }
    }

    private static final double BLOCK_CENTER = .5D;

    private Location getBlockCenteredCoordinates(Location location) {
        return new Location(location.getExtent(), new Vector3d(location.getBlockX() + BLOCK_CENTER, location.getBlockY(), location.getBlockZ()
                + BLOCK_CENTER));
    }

    private Optional<Location> checkAboveAndBelowLocation(Location location, final int height, final int width) {
        SpongeMod.instance.getLogger().debug("Given Location of: " + location.getBlockPosition()); // TODO plugin.getLocationManipulation().strCoordsRaw(l));
        SpongeMod.instance.getLogger().debug("Checking +-%" + height + " with a radius of " + width);
        // For now this will just do a straight up block.
        // Check the main level
        Optional<Location> safe = checkAroundLocation(location, width);

        if (safe.isPresent()) {
            return safe;
        }

        // We've already checked zero right above this.
        for (int currentLevel = 1; currentLevel <= height; currentLevel++) {
            // Check above
            safe = checkAroundLocation(new Location(location.getExtent(), location.getPosition().add(0, currentLevel, 0)), width);
            if (safe.isPresent()) {
                return safe;
            }

            // Check below
            safe = checkAroundLocation(new Location(location.getExtent(), location.getPosition().sub(0, currentLevel, 0)), width);
            if (safe.isPresent()) {
                return safe;
            }
        }

        return Optional.absent();
    }

    private Optional<Location> checkAroundLocation(Location location, final int radius) {
        // Let's check the center of the 'circle' first...
        Vector3i blockPos = new Vector3i(location.getBlockPosition());
        if (isSafeLocation((World) location.getExtent(), blockPos)) {
            return Optional.of(new Location(location.getExtent(), blockPos));
        }

        // Now we're going to search in expanding concentric circles...
        for (int currentRadius = 0; currentRadius <= radius; currentRadius++) {
            Optional<Vector3i> safePosition = checkAroundSpecificDiameter(location, currentRadius);
            if (safePosition.isPresent()) {
                // If a safe area was found: Return the checkLoc, it is the safe location.
                return Optional.of(new Location(location.getExtent(), safePosition.get()));
            }
        }

        return Optional.absent();
    }

    private Optional<Vector3i> checkAroundSpecificDiameter(Location checkLoc, final int radius) {
        World world = (World) checkLoc.getExtent();
        Vector3i blockPos = checkLoc.getBlockPosition();
        // Check out at the radius provided.
        blockPos = blockPos.add(radius, 0, 0);
        if (isSafeLocation(world, blockPos)) {
            return Optional.of(blockPos);
        }

        // Move up to the first corner..
        for (int i = 0; i < radius; i++) {
            blockPos = blockPos.add(radius, 0, 0);
            if (isSafeLocation(world, blockPos)) {
                return Optional.of(blockPos);
            }
        }

        // Move to the second corner..
        for (int i = 0; i < radius * 2; i++) {
            blockPos = blockPos.add(radius, 0, 0);
            if (isSafeLocation(world, blockPos)) {
                return Optional.of(blockPos);
            }
        }

        // Move to the third corner..
        for (int i = 0; i < radius * 2; i++) {
            blockPos = blockPos.add(radius, 0, 0);
            if (isSafeLocation(world, blockPos)) {
                return Optional.of(blockPos);
            }
        }

        // Move to the last corner..
        for (int i = 0; i < radius * 2; i++) {
            blockPos = blockPos.add(radius, 0, 0);
            if (isSafeLocation(world, blockPos)) {
                return Optional.of(blockPos);
            }
        }

        // Move back to just before the starting point.
        for (int i = 0; i < radius - 1; i++) {
            blockPos = blockPos.add(radius, 0, 0);
            if (isSafeLocation(world, blockPos)) {
                return Optional.of(blockPos);
            }
        }
        return Optional.absent();
    }

    public boolean isSafeLocation(World world, Vector3i blockPos) {
        BlockType type = world.getBlockType(blockPos);
        Vector3i up = new Vector3i(blockPos.add(Vector3i.UP));
        Vector3i down = new Vector3i(blockPos.sub(0, 1, 0));

        if (!isBlockSafe(world, blockPos) || !isBlockSafe(world, up) || !isBlockSafe(world, down)) {
            return false;
        }

        // TODO
        if (world.getBlock(down).getType() == BlockTypes.AIR) {
            SpongeMod.instance.getLogger().debug("Air detected below " + blockPos); 
            final boolean blocksBelowSafe = areBlocksBelowSafe(world, down);
            SpongeMod.instance.getLogger().debug("Has 2 blocks of water below " + blocksBelowSafe);
            return blocksBelowSafe; 
        }
        return true;
    }

    protected boolean isBlockSafe(World world, Vector3i blockPos) {
        BlockType block = world.getBlockType(blockPos);
        if (block.isSolidCube()) {
            return false;
        }
        if (blockPos.getY() < 0) {
            SpongeMod.instance.getLogger().warn("Location " + blockPos + " is below the world.");
            return false;
        }

        if (blockPos.getY() >= world.getDimension().getHeight()) {
            SpongeMod.instance.getLogger().warn("Location " + blockPos + " is above the world.");
            return false;
        }

        BlockType type = world.getBlockType(blockPos);
        if (type == BlockTypes.LAVA) {
            SpongeMod.instance.getLogger().warn("Lava detected at " + blockPos);
            return false;
        }
        if (type == BlockTypes.FIRE) {
            SpongeMod.instance.getLogger().warn("Fire detected at " + blockPos);
            return false;
        }
        return true;
    }

    protected boolean areBlocksBelowSafe(World world, Vector3i blockPos) {

        Vector3i blockBelowPos = new Vector3i(blockPos).sub(0, 1, 0);
        Vector3i blockBelowPos2 = new Vector3i(blockPos).sub(0, 2, 0);
        Vector3i blockBelowPos3 = new Vector3i(blockPos).sub(0, 3, 0);
        BlockType blockBelow = world.getBlockType(blockBelowPos);
        BlockType blockBelow2 = world.getBlockType(blockBelowPos2);
        BlockType blockBelow3 = world.getBlockType(blockBelowPos3);
        System.out.println("areBlocksBelowSafe, UP: " + blockBelow.getId() + ", CENTER: " + blockBelow2.getId() + ", DOWN: " + blockBelow3.getId());
        if (blockBelow == BlockTypes.AIR && blockBelow2 == BlockTypes.AIR && blockBelow3 == BlockTypes.AIR) {
            System.out.println("DETECTED FALL BELOW!!! AVOIDING SPAWN");
            return false; // prevent fall damage
        }

        if ((blockBelow == BlockTypes.AIR && (blockBelow2 == BlockTypes.LAVA || blockBelow2 == BlockTypes.FLOWING_LAVA)) ||
                blockBelow == BlockTypes.AIR && blockBelow2 == BlockTypes.AIR && (blockBelow3 == BlockTypes.LAVA || blockBelow3 == BlockTypes.FLOWING_LAVA)) {
            System.out.println("DETECTED LAVA BELOW!!! AVOIDING SPAWN");
            return false; // prevent death;
        }

        return true;
    }
}

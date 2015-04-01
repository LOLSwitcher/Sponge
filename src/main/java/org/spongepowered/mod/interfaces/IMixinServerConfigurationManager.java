package org.spongepowered.mod.interfaces;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.world.Location;

public interface IMixinServerConfigurationManager {

    EntityPlayerMP respawnPlayer(EntityPlayerMP playerIn, int targetDimension, boolean conqueredEnd, Location location);
}

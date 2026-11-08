package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;

@Mixin(ServerChunkManager.class)
public interface IMixinServerChunkManager
{
    @Accessor("ticketManager")
    ChunkTicketManager minihud_getTicketManager();
}

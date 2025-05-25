package fi.dy.masa.minihud.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.minihud.event.RenderHandler;
import net.minecraft.client.render.debug.NeighborUpdateDebugRenderer;

@Mixin(NeighborUpdateDebugRenderer.class)
public abstract class MixinNeighborUpdateDebugRenderer
{
    @Inject(method = "render", at = @At("HEAD"))
    public void fixDebugRendererState(CallbackInfo ci)
    {
        RenderHandler.fixDebugRendererState();
    }

    // very bad hack fix
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    public VertexConsumer dontGetBuffer(VertexConsumerProvider instance, RenderLayer renderLayer) {
        // simply calling vertexConsumers.getBuffer() causes rendering glitches?
        return null;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/render/VertexConsumer;DDDDDDFFFF)V"))
    public void fixDrawBox(VertexConsumer vertexConsumer, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m) {
        RenderSystem.disableTexture();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(d, e, f, g, h, i, new Color4f(j, k, l, m), buffer);

        tessellator.draw();
        RenderSystem.enableTexture();
    }
}

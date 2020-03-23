package ca.stellardrift.permissionsex.fabric.mixin.lifecycle;

import ca.stellardrift.permissionsex.fabric.ComponentChatMessagePacket;
import ca.stellardrift.permissionsex.fabric.TextAdapter;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.network.packet.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.PacketByteBuf;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatMessageS2CPacket.class)
public class MixinChatMessageS2CPacket implements ComponentChatMessagePacket {
    @Shadow
    private Text message;

    private Component component;

    @Nullable
    @Override
    public Component getComponent() {
        Component comp = this.component;
        if (comp == null) {
            return this.component = TextAdapter.toComponent(this.message);
        } else {
            return comp;
        }
    }

    @Override
    public void setComponent(@Nullable Component component) {
        this.component = component;
        this.message = null;
    }

    @Inject(method = "getMessage", at = @At("HEAD"))
    public void returnConvertedComponent(CallbackInfoReturnable<Text> ci) {
        if (this.message == null && this.component != null) {
            this.message = TextAdapter.toMcText(this.component);
        }
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PacketByteBuf;writeText(Lnet/minecraft/text/Text;)Lnet/minecraft/util/PacketByteBuf;"))
    public PacketByteBuf writeText(PacketByteBuf buf, Text message) {
        if (this.component != null) {
            return buf.writeString(GsonComponentSerializer.INSTANCE.serialize(this.component));
        } else {
            return buf.writeText(message);
        }
    }
}

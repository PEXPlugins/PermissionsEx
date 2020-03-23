@file:JvmName("TextAdapter")
package ca.stellardrift.permissionsex.fabric

import net.kyori.text.Component
import net.kyori.text.serializer.gson.GsonComponentSerializer
import net.minecraft.client.network.packet.ChatMessageS2CPacket
import net.minecraft.network.MessageType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

interface ComponentChatMessagePacket {
    var component: Component?
}

fun createS2CChatPacket(component: Component, type: MessageType = MessageType.SYSTEM): ChatMessageS2CPacket {
    val packet = ChatMessageS2CPacket(null, type)
    (packet as ComponentChatMessagePacket).component = component
    return packet
}

@JvmName("sendPlayerMessage")
@JvmOverloads
fun ServerPlayerEntity.sendMessage(text: Component, type: MessageType = MessageType.SYSTEM) {
   this.networkHandler.sendPacket(createS2CChatPacket(text, type))
}

fun Component.toMcText(): Text {
    return Text.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(this))!!
}

fun Text.toComponent(): Component {
    return GsonComponentSerializer.INSTANCE.deserialize(Text.Serializer.toJson(this))
}


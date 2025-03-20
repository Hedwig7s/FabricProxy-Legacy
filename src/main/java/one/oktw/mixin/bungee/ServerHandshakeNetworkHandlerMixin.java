package one.oktw.mixin.bungee;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.text.Text;
import one.oktw.Util;
import one.oktw.interfaces.BungeeClientConnection;
import one.oktw.mixin.ClientConnectionAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ServerHandshakeNetworkHandler.class)
public class ServerHandshakeNetworkHandlerMixin {

    @Unique
    private static final Gson gson = new Gson();

    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onHandshake(Lnet/minecraft/network/packet/c2s/handshake/HandshakeC2SPacket;)V", at = @At(value = "HEAD"), cancellable = true)
    private void onProcessHandshakeStart(HandshakeC2SPacket packet, CallbackInfo ci) {
        if (packet.intendedState() == ConnectionIntent.LOGIN) {
            String[] split = packet.address().split("\00");
            if (split.length == 3 || split.length == 4) {
                // override/insert forwarded IP into connection:
                ((ClientConnectionAccessor) connection).setAddress(
                        new InetSocketAddress(split[1], ((InetSocketAddress) connection.getAddress()).getPort())
                );


                // extract forwarded profile information and save them:
                ((BungeeClientConnection) connection).setSpoofedUUID(Util.fromString(split[2]));

                if (split.length == 4) {
                    ((BungeeClientConnection) connection).setSpoofedProfile(gson.fromJson(split[3], Property[].class));
                }
            } else {
                // no extra information found in the address, disconnecting player:
                Text disconnectMessage = Text.of(
                        "Bypassing proxy not allowed! If you wish to use IP forwarding, " +
                                "please enable it in your BungeeCord config as well!");
                // connection.send(new LoginDisconnectS2CPacket(disconnectMessage)); Broken, unsure how to fix
                connection.disconnect(disconnectMessage);
                ci.cancel();
            }
        }
    }
}

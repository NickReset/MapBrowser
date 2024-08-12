package social.nickrest.mapbrowser;

import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.Marker;
import dev.cerus.maps.api.event.PlayerClickScreenEvent;
import dev.cerus.maps.util.Vec2;
import dev.cerus.maps.util.Vec2D;
import io.papermc.paper.event.player.AsyncChatEvent;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.map.MapCursor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class RemoteScreen extends WebSocketClient implements Listener {

    private static final String host = "ws://localhost:9999";
    private final MapScreen screen;
    private Marker cursor;

    public RemoteScreen(MapScreen screen) {
        super(URI.create(host));
        this.screen = screen;
        connect();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        MapBrowser.getInstance().getLogger().log(Level.INFO, "Connected to websocket server.");
    }

    @Override
    public void onMessage(String s) {

    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes.array()));
            image = MapBrowser.getInstance().resizeImage(image, screen.getWidth() * 128, screen.getHeight() * 128);
            screen.getGraphics().drawImage(image, 0, 0);

            if (cursor != null) {
                screen.removeMarker(cursor);
                screen.addMarker(cursor);
            }
            screen.sendMaps(true);
        } catch (IOException e) {
            MapBrowser.getInstance().getLogger().log(Level.SEVERE, "Unable to parse image received from websocket server.");
            MapBrowser.getInstance().getLogger().log(Level.SEVERE, e.toString());
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        if (cursor != null) {
            screen.removeMarker(cursor);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        screen.spawnFrames(event.getPlayer());
        screen.sendMaps(true, event.getPlayer());
    }

    @Override
    public void onError(Exception e) {
        MapBrowser.getInstance().getLogger().log(Level.SEVERE, e.toString());
    }

    @EventHandler(ignoreCancelled = true) @SuppressWarnings("unchecked")
    public void onClickScreen(PlayerClickScreenEvent e) {
        Vec2 pos = e.getClickPos();
        Vec2D browserPos = new Vec2D((double) pos.x / (screen.getWidth() * 128) * 1280, (double) pos.y / (screen.getHeight() * 128) * 1024);
        JSONObject data = new JSONObject();
        data.put("x", browserPos.x);
        data.put("y", browserPos.y);

        if (e.isLeftClick()) {
            data.put("type", "click");
            data.put("button", "left");
        } else if (e.isRightClick()) {
            data.put("type", "move");
        }

        send(data.toJSONString());

        Marker marker = new Marker(pos.x * 2, pos.y * 2, (byte) 14, MapCursor.Type.BANNER_WHITE, true);
        if (cursor != null) {
            screen.removeMarker(cursor);
        }
        cursor = marker;
    }

    @EventHandler @SuppressWarnings("unchecked")
    public void onChat(AsyncChatEvent e) {
        String text = ((TextComponent) e.message()).content();
        JSONObject data = new JSONObject();
        data.put("type", "type");
        data.put("text", text);

        send(data.toJSONString());
    }
}

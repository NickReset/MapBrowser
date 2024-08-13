package social.nickrest.mapbrowser;

import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.Marker;
import dev.cerus.maps.api.event.PlayerClickScreenEvent;
import dev.cerus.maps.util.Vec2;
import dev.cerus.maps.util.Vec2D;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.map.MapCursor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class RemoteScreen extends WebSocketClient implements Listener, CommandExecutor {

    private static final String host = "ws://localhost:9999";
    private final MapScreen screen;
    private Marker cursor;
    private static final MapBrowser plugin = MapBrowser.getInstance();

    public RemoteScreen(MapScreen screen) {
        super(URI.create(host));
        this.screen = screen;
        connect();

        plugin.getCommand("forward").setExecutor(this);
        plugin.getCommand("back").setExecutor(this);
        plugin.getCommand("enter").setExecutor(this);
        plugin.getCommand("space").setExecutor(this);
        plugin.getCommand("backspace").setExecutor(this);
        plugin.getCommand("goto").setExecutor(this);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        plugin.getLogger().log(Level.INFO, "Connected to websocket server.");
    }

    @Override
    public void onMessage(String s) {

    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes.array()));
            image = plugin.resizeImage(image, screen.getWidth() * 128, screen.getHeight() * 128);
            screen.getGraphics().drawImage(image, 0, 0);

            if (cursor != null) {
                screen.removeMarker(cursor);
                screen.addMarker(cursor);
            }
            screen.sendMaps(true);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to parse image received from websocket server.");
            plugin.getLogger().log(Level.SEVERE, e.toString());
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
        plugin.getLogger().log(Level.SEVERE, e.toString());
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

    @EventHandler @SuppressWarnings("unchecked")
    public void onScroll(PlayerItemHeldEvent e) {
        int scrollDirection;
        int previousSlot = e.getPreviousSlot();
        int newSlot = e.getNewSlot();

        if ((previousSlot == 0 && newSlot == 8) || (newSlot == previousSlot - 1)) {
            scrollDirection = -1;
        } else if ((previousSlot == 8 && newSlot == 0) || (newSlot == previousSlot + 1)) {
            scrollDirection = 1;
        } else return;

        JSONObject data = new JSONObject();
        data.put("type", "scroll");
        data.put("direction", scrollDirection);

        send(data.toJSONString());
    }

    @Override @SuppressWarnings("unchecked")
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        JSONObject data = new JSONObject();
        data.put("type", "action");

        switch (command.getName()) {
            case "goto":
                try {
                    URL url = new URL(strings[0]);
                    data.put("url", url.toString());
                } catch (MalformedURLException e) {
                    commandSender.sendMessage(e.getMessage());
                    return false;
                }
            case "forward":
            case "back":
            case "space":
            case "enter":
            case "backspace":
                data.put("action", command.getName());
                break;
            default:
                return true;
        }

        send(data.toJSONString());
        commandSender.sendPlainMessage("Executing command: " + command.getName() + "...");
        return true;
    }
}

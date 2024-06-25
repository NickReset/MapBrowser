package social.nickrest.mapbrowser;

import com.github.kklisura.cdt.launch.ChromeArguments;
import com.github.kklisura.cdt.launch.ChromeLauncher;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.event.PlayerClickScreenEvent;
import dev.cerus.maps.api.graphics.ColorCache;
import dev.cerus.maps.api.graphics.MapGraphics;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import dev.cerus.maps.util.Vec2;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

@Getter
public final class MapBrowser extends JavaPlugin implements Listener {

    private MapScreen screen;
    private ChromeService service;

    @Override
    public void onEnable() {
        screen = MapScreenRegistry.getScreens().stream().findFirst().orElse(null);

        if(screen == null) {
            getLogger().severe("THIS IS NOT THE METHOD 😡");
            return;
        }



        // resolve testimage.png
        if (!getDataFolder().toPath().resolve("testimage.png").toFile().exists()) {
            saveResource("testimage.png", false);
        }

        MapGraphics<?, ?> graphics = screen.getGraphics();
        graphics.fillComplete(ColorCache.rgbToMap(255, 255, 255));
        screen.spawnFrames(Bukkit.getOnlinePlayers().toArray(new Player[0]));
        screen.sendMaps(true);

        // browser
        new Thread(() -> {
            try(ChromeLauncher launcher = new ChromeLauncher()) {
                getLogger().info("Launching Chrome...");

                service = launcher.launch(
                        Path.of("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge_proxy.exe"),
                        ChromeArguments.defaults(true).build()
                );

                getLogger().info("Chrome Version: " + service.getVersion().getProtocolVersion());
                getLogger().info("V8 Version: " + service.getVersion().getV8Version());
            } catch (Exception e) {
                throw new RuntimeException("THE METHOD!!!! ??????????", e);
            }
        }).start();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.getTabs().forEach(service::closeTab);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(Component.text(e.getPlayer().getName() + " joined the game!"));

        try {
            BufferedImage image = ImageIO.read(getDataFolder().toPath().resolve("testimage.png").toFile());
            image = resizeImage(image, screen.getWidth() * 128, screen.getHeight() * 128);
            screen.getGraphics().drawImage(image,0, 0);
        } catch (IOException err) {
            throw new RuntimeException("THE METHOD!!!! 😭😭😭😭😭", err);
        }

        screen.spawnFrames(e.getPlayer());
        screen.sendMaps(true, e.getPlayer());
        Bukkit.broadcast(Component.text(String.format("Screen: x: %s, y: %s", screen.getWidth() * 128, screen.getHeight() * 128)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onClickScreen(PlayerClickScreenEvent e) {
        Vec2 pos = e.getClickPos();

        Bukkit.broadcast(Component.text(String.format("Click at x: %s, y: %s, right: %s", pos.x, pos.y, !e.isLeftClick())));
    }

    public BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);

        return resizedImage;
    }

    public static MapBrowser getInstance() {
        return getPlugin(MapBrowser.class);
    }
}

package net.runelite.client.plugins.mywebsocket;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.myplugin.PythonConnection;
import net.runelite.client.plugins.myplugin.StateDataConfig;
import net.runelite.client.plugins.mywebsocket.WebsocketPanel;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import lombok.AccessLevel;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import com.google.inject.Provides;

@Slf4j
@PluginDescriptor(
        name = "My Websocket"
)
public class MyWebsocketPlugin extends Plugin {
    @Inject
    protected Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private WebsocketConfig config;
    private WebsocketPanel panel;
    @Inject
    private ClientThread clientThread;
    private MyPythonConnection ws;

    @Provides
    WebsocketConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(WebsocketConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        if (this.clientThread != null) {
            clientThread.invoke(() ->
            {
                Widget settingsInit = client.getWidget(WidgetInfo.SETTINGS_INIT);
                if (settingsInit != null) {
                    client.createScriptEvent(settingsInit.getOnLoadListener())
                            .setSource(settingsInit)
                            .run();
                }
            });
        }

        log.info("Example started!");

        ws = new MyPythonConnection(new URI("ws://localhost:8765"), new Draft_6455(), this);
        ws.connect();
    }


}
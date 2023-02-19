package net.runelite.client.plugins.myplugin;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
		name = "State Data"
)
public class StateDataPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private StateDataConfig config;

	private StateDataPanel panel;

	private NavigationButton navButton;
	private String data;
	private WorldPoint lastTickLocation;
	private int lastHitPoints;
	private int runEnergy;
	private int lastPrayerPoints;
	private java.util.List<WorldPoint> worldPointsList;
	private Tile[][][] scenetiles;

	private WebSocketClient ws;

	@Getter
	private final Set<GameObject> treeObjects = new HashSet<>();

	@Provides
	StateDataConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StateDataConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		panel = injector.getInstance(StateDataPanel.class);
		panel.init(config);
		log.info("Example started!");

		ws = new PythonConnection(new URI("ws://localhost:8765"), new Draft_6455());
		ws.connect();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "notes_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("State Data")
				.icon(icon)
				.priority(7)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		treeObjects.clear();
		panel = null;
		clientToolbar.removeNavigation(navButton);
		ws.close();
	}

	@Subscribe
	public void onGameTick(GameTick t) throws IOException, URISyntaxException {
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			lastTickLocation = null;
		}
		else {

			JSONObject obj = new JSONObject();
			JSONObject env = new JSONObject();
			lastTickLocation = client.getLocalPlayer().getWorldLocation();
			lastHitPoints = client.getRealSkillLevel(Skill.HITPOINTS);
			lastPrayerPoints = client.getRealSkillLevel(Skill.PRAYER);
			runEnergy = client.getEnergy();

			worldPointsList = client.getLocalPlayer().getWorldArea().toWorldPointList();

			scenetiles = client.getScene().getTiles();

			obj.put("location", "[" + lastTickLocation.getX() + ", " + lastTickLocation.getY() + "]");
			//obj.put("items", client.getItemContainers().toString());
			//obj.put("camera", client.getCameraPitch() + ", " + client.getCameraYaw() + ", " + client.get3dZoom());
			//obj.put("goblins", client.getCachedNPCs()[NpcID.GOBLIN.getIndex()]);
			for (int i = 0; i < client.getNpcs().toArray().length; i++)
			{
				if (client.getNpcs().get(i).getId() == NpcID.GOBLIN)
				{
					int x = client.getNpcs().get(i).getWorldLocation().getX();
					int y = client.getNpcs().get(i).getWorldLocation().getY();
					obj.put("goblin", "[" + x + ", " + y + "]");
				}
			}

			obj.put("energy", runEnergy);
			obj.put("health", lastHitPoints);
			//obj.put("Prayerpoints", lastPrayerPoints);

			String message = obj.toString();
			ws.send(message);

		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.gameStateData(), null);
		}
	}

	void updateData(String data, int hp, int pp, int run)
	{
		String jsonData = "{'World Location':'" + data + "','Hitpoints':" + hp + ",'Prayer Points':" + pp + ", 'Run Energy':" + run + "}";
		panel.setData(jsonData);
		Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(
						new StringSelection(jsonData),
						null
				);
		//SwingUtilities.invokeLater(() -> panel.setData(data));
	}


}

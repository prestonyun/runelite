package net.runelite.client.plugins.myplugin;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.json.JSONObject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
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
	private int[] validmovements;

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

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "combaticon.png");

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
			validmovements = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
			lastTickLocation = client.getLocalPlayer().getWorldLocation();
			lastHitPoints = client.getRealSkillLevel(Skill.HITPOINTS);
			lastPrayerPoints = client.getRealSkillLevel(Skill.PRAYER);
			runEnergy = client.getEnergy();

			WorldPoint north = new WorldPoint(lastTickLocation.getX(), lastTickLocation.getY() + 2, lastTickLocation.getPlane());
			WorldPoint northeast = new WorldPoint(lastTickLocation.getX() + 2, lastTickLocation.getY() + 2, lastTickLocation.getPlane());
			WorldPoint east = new WorldPoint(lastTickLocation.getX() + 2, lastTickLocation.getY(), lastTickLocation.getPlane());
			WorldPoint southeast = new WorldPoint(lastTickLocation.getX() + 2, lastTickLocation.getY() -2, lastTickLocation.getPlane());
			WorldPoint south = new WorldPoint(lastTickLocation.getX(), lastTickLocation.getY() - 2, lastTickLocation.getPlane());
			WorldPoint southwest = new WorldPoint(lastTickLocation.getX() - 2, lastTickLocation.getY() - 2, lastTickLocation.getPlane());
			WorldPoint west = new WorldPoint(lastTickLocation.getX() - 2, lastTickLocation.getY(), lastTickLocation.getPlane());
			WorldPoint northwest = new WorldPoint(lastTickLocation.getX() - 2, lastTickLocation.getY() + 2, lastTickLocation.getPlane());

			if (lastTickLocation.toWorldArea().canTravelInDirection(client, 0, 1) && lastTickLocation.toWorldArea().contains(north))
				validmovements[0] = 1;
			else
				validmovements[0] = 0;

			obj.put("valid_movements", validmovements[0]);
			obj.put("location", "[" + lastTickLocation.getX() + ", " + lastTickLocation.getY() + "]");

			obj.put("energy", runEnergy);
			obj.put("health", lastHitPoints);
			obj.put("Prayerpoints", lastPrayerPoints);

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

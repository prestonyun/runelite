package net.runelite.client.plugins.myplugin;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import net.runelite.api.events.ItemContainerChanged;
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
import net.runelite.client.game.walking.Reachable;

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
	private WorldPoint lastTickLocation;
	private ItemContainer previousInventory;


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
		previousInventory = client.getItemContainer(InventoryID.INVENTORY);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		treeObjects.clear();
		panel = null;
		clientToolbar.removeNavigation(navButton);
		ws.close();
		previousInventory = null;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
			previousInventory = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick t) throws IOException, URISyntaxException {
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			lastTickLocation = null;
		}
		else {

			JSONObject obj = new JSONObject();
			lastTickLocation = client.getLocalPlayer().getWorldLocation();

			obj.put("location", "[" + lastTickLocation.getX() + ", " + lastTickLocation.getY() + "]");

			obj.put("health", client.getRealSkillLevel(Skill.HITPOINTS));
			obj.put("Prayerpoints", client.getRealSkillLevel(Skill.PRAYER));
			obj.put("energy", client.getEnergy());
			obj.put("valid_movements", getValidMovementLocationsAsString(client, lastTickLocation, 10));
			obj.put("inventory", getInventoryAsString());

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

	public String getInventoryAsString() {
		ItemContainer inventory = client.getItemContainer(net.runelite.api.InventoryID.INVENTORY);
		if (inventory != null) {
			Item[] items = inventory.getItems();
			String inventoryString = Arrays.stream(items)
					.map(item -> String.valueOf(item != null ? item.getId() : -1))
					.collect(Collectors.joining(", ", "[", "]"));
			return inventoryString;
		} else {
			// Return inventory with placeholders
			String inventoryString = IntStream.range(0, 28)
					.mapToObj(i -> String.valueOf(-1))
					.collect(Collectors.joining(", ", "[", "]"));
			return inventoryString;
		}
	}


	public String getValidMovementLocationsAsString(Client client, WorldPoint startPoint, int maxDistance) {
		List<WorldPoint> validLocations = new ArrayList<>();
		Deque<WorldPoint> queue = new ArrayDeque<>();
		Set<WorldPoint> visited = new HashSet<>();

		queue.add(startPoint);
		visited.add(startPoint);

		while (!queue.isEmpty()) {
			WorldPoint current = queue.removeFirst();
			if (current.distanceTo(startPoint) > maxDistance) {
				continue;
			}

			if (Reachable.isWalkable(client, current)) {
				validLocations.add(current);
			}

			for (WorldPoint neighbor : Reachable.getNeighbours(client, current, null)) {
				if (visited.add(neighbor)) {
					queue.addLast(neighbor);
				}
			}
		}

		StringBuilder sb = new StringBuilder("[");
		for (WorldPoint location : validLocations) {
			sb.append(location.getX()).append(",").append(location.getY()).append(",");
		}
		for (int i = validLocations.size(); i < 100; i++) {
			sb.append("-1,-1,");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]");

		return sb.toString();
	}

}

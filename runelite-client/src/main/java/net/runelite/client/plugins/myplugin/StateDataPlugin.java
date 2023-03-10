package net.runelite.client.plugins.myplugin;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.runelite.api.events.ClientTick;
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

	//private WebsocketClient ws;
	private PythonConnection ws;

	private Properties props;

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
		System.out.println(System.getProperty("user.dir"));
		panel = injector.getInstance(StateDataPanel.class);
		panel.init(config);
		log.info("Example started!");

		props = new Properties();
		try (InputStream input = new FileInputStream("config.properties"))
		{
			props.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String serverUri = props.getProperty("serverUri");
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] {new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

			}

			@Override
			public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[]{};
			}
		}}, null);

		SSLSocketFactory factory = sslContext.getSocketFactory();
		ws = new PythonConnection(new URI(serverUri));
		//ws = new PythonConnection(new URI("wss://localhost:8765"), new Draft_6455());
		ws.setSocket(factory.createSocket());
		ws.connect();


		//ws = new PythonConnection(new URI("ws://localhost:8765"), new Draft_6455());
		//ws = new PythonConnection(new URI("wss://prestonyun-automatic-potato-ppx5v7x74ghr757-8765.preview.app.github.dev/"));
		//ws.connect();

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

/*	@Subscribe
	public void onClientTick(ClientTick t) {

		int gameTick = client.getGameCycle();
		int clientTick = client.getTickCount();


		if (ws.isConnected())
		{
			StringBuilder sb = new StringBuilder("[");
			sb.append(String.valueOf(gameTick)).append(',').append(String.valueOf(clientTick)).append(']');
			JSONObject obj = new JSONObject();
			obj.put("gametick", sb.toString());
			ws.send(obj.toString());
		}
	}*/

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

			obj.put("hitpoints", client.getRealSkillLevel(Skill.HITPOINTS));
			obj.put("prayerpoints", client.getRealSkillLevel(Skill.PRAYER));
			obj.put("energy", client.getEnergy());
			obj.put("valid_movements", getValidMovementLocationsAsString(client, lastTickLocation, 10));
			obj.put("inventory", getInventoryAsString());

			ws.send(obj.toString());

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
			String[] fixedSizeItems = new String[28];
			Arrays.fill(fixedSizeItems, "-1");
			for (int i = 0; i < items.length && i < fixedSizeItems.length; i++) {
				if (items[i] != null) {
					fixedSizeItems[i] = String.valueOf(items[i].getId());
				}
			}
			String inventoryString = Arrays.stream(fixedSizeItems)
					.map(id -> String.format("%2s", id))
					.collect(Collectors.joining(", ", "[", "]"));
			return inventoryString;
		} else {
			String inventoryString = IntStream.range(0, 28)
					.mapToObj(i -> String.valueOf(-1))
					.collect(Collectors.joining(", ", "[", "]"));
			return inventoryString;
		}
	}



	public String getValidMovementLocationsAsString(Client client, WorldPoint startPoint, int maxDistance) {
		WorldPoint[] validLocations = new WorldPoint[410];
		int count = 0;
		Deque<WorldPoint> queue = new ArrayDeque<>();
		Set<WorldPoint> visited = new HashSet<>();

		queue.add(startPoint);
		visited.add(startPoint);

		while (!queue.isEmpty() && count < 410) {
			WorldPoint current = queue.removeFirst();
			if (current.distanceTo(startPoint) > maxDistance) {
				continue;
			}

			if (Reachable.isWalkable(client, current)) {
				validLocations[count] = current;
				count++;
			}

			for (WorldPoint neighbor : Reachable.getNeighbours(client, current, null)) {
				if (visited.add(neighbor)) {
					queue.addLast(neighbor);
				}
			}
		}
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < validLocations.length; i++) {
			WorldPoint location = validLocations[i];
			if (location != null) {
				sb.append("[").append(location.getX()).append(",").append(location.getY()).append("]").append(",");
			} else {
				sb.append("[-1,-1],");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]");

		return sb.toString();
	}


}

package net.runelite.client.plugins.myplugin;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class PythonConnection extends WebSocketClient {

    private WebSocket socket;

    private boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    public PythonConnection(URI serverUri, Draft draft) {
        super(serverUri, draft);
        this.socket = this.getConnection();
    }

    public PythonConnection(URI serverUri) throws URISyntaxException {
        super(serverUri);
        isConnected = false;
    }

    public void createSocket() throws URISyntaxException {
        this.socket = new PythonConnection(new URI("ws://localhost:8765"), new Draft_6455());

    }

    public void closeSocket() {
        this.socket.close(1000, null);
    }

    private void sendMessage(JSONObject message) {
        this.socket.send(message.toString());
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        isConnected = true;
        System.out.println("opened connection");
    }


    @Override
    public void onMessage(String s) {
        JSONObject obj = new JSONObject(s);
        System.out.println(obj.toString());
        if (obj.has("type")) {
            JSONObject data, payloadObject;
            String event = obj.get("type").toString();
            switch (event) {
                case "greeting":
                    payloadObject = new JSONObject();
                    payloadObject.put("greeting", "Hello back!");
                    sendMessage(payloadObject);
            }
        }

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        isConnected = false;
        System.out.println("closed connection");
    }

    @Override
    public void onError(Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
    }
}
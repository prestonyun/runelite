package net.runelite.client.plugins.myplugin;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class PythonConnection extends WebSocketClient {

    private boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    public PythonConnection(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public PythonConnection(URI serverUri) throws URISyntaxException {
        super(serverUri);
        isConnected = false;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        isConnected = true;
        System.out.println("opened connection");
    }

    @Override
    public void onMessage(String s) {
        JSONObject obj = new JSONObject(s);
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
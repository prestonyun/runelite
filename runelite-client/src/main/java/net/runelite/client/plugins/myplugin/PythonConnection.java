package net.runelite.client.plugins.myplugin;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

public class PythonConnection extends WebSocketClient {

    public PythonConnection(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public PythonConnection(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("opened connection");
    }

    @Override
    public void onMessage(String s) {
        JSONObject obj = new JSONObject(s);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("closed connection");
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
}
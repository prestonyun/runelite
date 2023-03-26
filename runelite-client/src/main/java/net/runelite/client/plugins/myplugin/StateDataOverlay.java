package net.runelite.client.plugins.myplugin;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;

import javax.inject.Inject;
import java.awt.*;

public class StateDataOverlay extends Overlay {

    private final Client client;

    @Inject
    private StateDataOverlay(Client client)
    {
        this.client = client;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getSelectedSceneTile() != null)
        {
            getLocation(graphics, client.getSelectedSceneTile().getLocalLocation());
        }
        return null;
    }

    private void getLocation(Graphics2D graphics, LocalPoint dest)
    {
        if (dest == null)
        {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null)
        {
            return;
        }

    }

}

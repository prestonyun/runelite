package net.runelite.api;

import net.runelite.api.DecorativeObject;
import net.runelite.api.GroundObject;
import net.runelite.api.WallObject;

public interface CustomScene {
    void removeWallObject(WallObject paramWallObject);

    void removeWallObject(int paramInt1, int paramInt2, int paramInt3);

    void removeDecorativeObject(DecorativeObject paramDecorativeObject);

    void removeDecorativeObject(int paramInt1, int paramInt2, int paramInt3);

    void removeGroundObject(GroundObject paramGroundObject);

    void removeGroundObject(int paramInt1, int paramInt2, int paramInt3);
}

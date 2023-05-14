package net.runelite.client.plugins.mystatussocket;

import com.google.gson.Gson;
import net.runelite.client.plugins.mystatussocket.data.death.DeathBuilder;
import net.runelite.client.plugins.mystatussocket.data.hitsplat.HitsplatBuilder;
import net.runelite.client.plugins.mystatussocket.data.player.PlayerDataBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import okhttp3.*;

import java.io.IOException;

import static net.runelite.client.plugins.mystatussocket.MyStatusSocketEndpoints.LOG_ENDPOINT;

@Slf4j
@AllArgsConstructor
public class MyStatusSocketClient
{

    private Client client;
    private ItemManager itemManager;
    private MyStatusSocketConfig config;
    private OkHttpClient okClient;

    public void sendHitsplat(int damage, String targetName)
    {
        HitsplatBuilder builder = new HitsplatBuilder(client);
        builder.setDamage(damage);
        builder.setTargetName(targetName != null ? targetName : "");
        post(builder.build());
    }

    public void sendInventoryChangeLog()
    {
        PlayerDataBuilder builder = new PlayerDataBuilder(client, itemManager);
        post(builder.build());
    }

    public void sendCombatLog(String targetName, boolean isAttacking)
    {
        PlayerDataBuilder builder = new PlayerDataBuilder(client, itemManager, targetName, isAttacking);
        post(builder.build());
    }

    public void sendDeath(String targetName)
    {
        DeathBuilder builder = new DeathBuilder(client);
        builder.setTargetName(targetName != null ? targetName : "");
        post(builder.build());
    }

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);

        // automatically include a "/" at the end of the initial endpoint URL if it wasn't included
        String endpoint = config.endpoint().endsWith("/") ? config.endpoint() : config.endpoint() + "/";

        HttpUrl url = HttpUrl.parse(endpoint + LOG_ENDPOINT);
        MediaType mt = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mt, json);

        Request request = new Request.Builder().url(url).post(body).build();
        okClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.warn("Failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                log.info("Code: {} - Response: {}", response.code(), response.body().string());
                response.close();
            }
        });
    }
}
package com.jouriroosjen.hardcoreSMPPlugin.utils;

import github.scarsz.discordsrv.dependencies.json.JSONObject;
import github.scarsz.discordsrv.dependencies.okhttp3.OkHttpClient;
import github.scarsz.discordsrv.dependencies.okhttp3.Request;
import github.scarsz.discordsrv.dependencies.okhttp3.Response;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Utility class for all things regarding the player avatar
 *
 * @author Jouri Roosjen
 * @version 1.0.0.0
 */
public class PlayerAvatarUtil {
    private static final OkHttpClient HTTP = new OkHttpClient();

    /**
     * Get the avatar url of the given player.
     *
     * @param player    The player
     * @param imageSize The size of the image
     * @return A url to the player's avatar
     */
    public static String getPlayerAvatarUrl(Player player, int imageSize) {
        boolean isFloodgatePlayer = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());

        // Return Minotar url if the player is not on Bedrock
        if (!isFloodgatePlayer) return "https://minotar.net/helm/" + player.getName() + "/" + imageSize + ".png";

        // Create request to fetch bedrock player texture
        String bedrockXuid = FloodgateApi.getInstance().getPlayer(player.getUniqueId()).getXuid();

        String requestUrl = "https://api.geysermc.org/v2/skin/" + bedrockXuid;
        Request request = new Request.Builder().url(requestUrl).build();

        try (Response response = HTTP.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                String texture = jsonResponse.optString("texture_id", null);

                // Return bedrock player head based on texture
                if (texture != null) return "https://mc-heads.net/avatar/" + texture + "/" + imageSize + ".png";
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch floodgate avatar for " + player.getName());
            e.printStackTrace();
        }

        // Return steve head as fallback
        return "https://minotar.net/helm/Steve/" + imageSize + ".png";
    }
}

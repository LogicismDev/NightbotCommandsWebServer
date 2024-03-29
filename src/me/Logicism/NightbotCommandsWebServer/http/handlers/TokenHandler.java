package me.Logicism.NightbotCommandsWebServer.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.Logicism.NightbotCommandsWebServer.NightbotCommandsWebServer;
import me.Logicism.NightbotCommandsWebServer.http.BrowserClient;
import me.Logicism.NightbotCommandsWebServer.http.BrowserData;
import me.Logicism.NightbotCommandsWebServer.http.oauth.TokenHandle;
import me.Logicism.NightbotCommandsWebServer.util.FileUtils;
import me.Logicism.NightbotCommandsWebServer.util.HTTPUtils;
import me.Logicism.NightbotCommandsWebServer.util.TextUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TokenHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            NightbotCommandsWebServer.INSTANCE.getLogger().info(exchange.getRequestMethod() + " - " +
                    (exchange.getRequestHeaders().containsKey("X-Forwarded-For") ?
                            exchange.getRequestHeaders().get("X-Forwarded-For").get(0) :
                            exchange.getRemoteAddress().getAddress()) + " - " +
                    (exchange.getRequestHeaders().containsKey("User-Agent") ?
                            exchange.getRequestHeaders().get("User-Agent").get(0) : "Unknown User-Agent")
                    + " - /token");
            Map<String, String> queryMap = TextUtils.queryToMap(exchange.getRequestURI().getQuery());

            if (queryMap.containsKey("error")) {
                HTTPUtils.throwError(exchange, queryMap.get("error_description").replace("+", " "));
            } else {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", exchange.getRequestHeaders().get("User-Agent").get(0));

                BrowserData bd = BrowserClient.executePOSTRequest(new URL("https://id.twitch.tv/oauth2/token"),
                        "client_id=" + NightbotCommandsWebServer.INSTANCE.getConfig().getClientID() +
                                "&client_secret=" + NightbotCommandsWebServer.INSTANCE.getConfig().getClientSecret() +
                                "&code=" + queryMap.get("code") + "&grant_type=authorization_code" +
                                "&redirect_uri=" + NightbotCommandsWebServer.INSTANCE.getConfig().getCallbackURI(), headers);

                if (bd.getResponseCode() == 200) {
                    JSONObject tokenObject = new JSONObject(BrowserClient.requestToString(bd.getResponse()));

                    headers.put("Authorization", "Bearer " + tokenObject.getString("access_token"));
                    headers.put("Client-Id", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID());

                    bd = BrowserClient.executeGETRequest(new URL("https://api.twitch.tv/helix/users"), headers);

                    JSONObject userObject = new JSONObject(BrowserClient.requestToString(bd.getResponse()))
                            .getJSONArray("data").getJSONObject(0);

                    if (NightbotCommandsWebServer.INSTANCE.containsUserTokenHandle(userObject.getString("id"),
                            tokenObject.getJSONArray("scope").getString(0))) {
                        TokenHandle handle = NightbotCommandsWebServer.INSTANCE.getTokenHandle(userObject
                                .getString("id"), tokenObject.getJSONArray("scope")
                                .getString(0));

                        handle.setAccessToken(tokenObject.getString("access_token"));
                        handle.setRefreshToken(tokenObject.getString("refresh_token"));
                        handle.setTimeToExpire(tokenObject.getLong("expires_in"));
                        handle.setExpired(false);

                        if (NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().containsKey(handle)) {
                            NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().get(handle)
                                    .cancel(true);

                            NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().replace(handle, NightbotCommandsWebServer
                                    .INSTANCE.getExecutor().schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            handle.setExpired(true);
                                        }
                                    }, handle.getTimeToExpire(), TimeUnit.SECONDS));
                        } else {
                            NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().put(handle, NightbotCommandsWebServer
                                    .INSTANCE.getExecutor().schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            handle.setExpired(true);
                                        }
                                    }, handle.getTimeToExpire(), TimeUnit.SECONDS));
                        }
                    } else {
                        TokenHandle handle = new TokenHandle(userObject.getString("id"),
                                tokenObject.getString("access_token"), tokenObject.getString("refresh_token"),
                                Collections.singletonList(tokenObject.getJSONArray("scope").getString(0)),
                                tokenObject.getLong("expires_in"));

                        NightbotCommandsWebServer.INSTANCE.getTokenHandleList().add(handle);
                        NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().put(handle, NightbotCommandsWebServer
                                .INSTANCE.getExecutor().schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        handle.setExpired(true);
                                    }
                                }, handle.getTimeToExpire(), TimeUnit.SECONDS));


                        File tokenHandleDatabase = new File("tokenHandles.dat");

                        try {
                            NightbotCommandsWebServer.INSTANCE.writeTokenHandleList(tokenHandleDatabase);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    String response = FileUtils.fileToString(HTTPUtils.getFile("/callback.html"))
                            .replace("{moderatorname}", userObject.getString("display_name"))
                            .replace("{moderatorid}", userObject.getString("id"));

                    HTTPUtils.throwSuccessHTML(exchange, response);
                } else {
                    HTTPUtils.throwError(exchange, "Cannot grab access token! Please reauthenticate to Twitch again!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

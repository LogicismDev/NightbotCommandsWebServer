package me.Logicism.NightbotCommandsWebServer.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.Logicism.NightbotCommandsWebServer.NightbotCommandsWebServer;
import me.Logicism.NightbotCommandsWebServer.http.BrowserClient;
import me.Logicism.NightbotCommandsWebServer.http.BrowserData;
import me.Logicism.NightbotCommandsWebServer.http.oauth.TokenHandle;
import me.Logicism.NightbotCommandsWebServer.util.HTTPUtils;
import me.Logicism.NightbotCommandsWebServer.util.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommandsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        NightbotCommandsWebServer.INSTANCE.getLogger().info(exchange.getRequestMethod() + " - " +
                (exchange.getRequestHeaders().containsKey("X-Forwarded-For") ?
                        exchange.getRequestHeaders().get("X-Forwarded-For").get(0) :
                        exchange.getRemoteAddress().getAddress()) + " - " +
                (exchange.getRequestHeaders().containsKey("User-Agent") ?
                        exchange.getRequestHeaders().get("User-Agent").get(0) : "Unknown User-Agent")
                + " - /webhook");
        Map<String, String> queryMap = TextUtils.queryToMap(exchange.getRequestURI().getQuery());

        if (queryMap.containsKey("moderator") && queryMap.containsKey("channel")) {
            if (exchange.getRequestURI().toString().equals("/commands/followage")) {
                TokenHandle tokenHandle = NightbotCommandsWebServer.INSTANCE
                        .getTokenHandle(queryMap.get("moderator"), "moderator:read:followers");
                if (tokenHandle != null) {
                    if (tokenHandle.isExpired()) {
                        NightbotCommandsWebServer.INSTANCE
                                .refreshHandle(exchange.getRequestHeaders().get("User-Agent").get(0), tokenHandle);
                    }

                    Map<String, String> headers = new HashMap<>();

                    headers.put("User-Agent", exchange.getRequestHeaders().get("User-Agent").get(0));
                    headers.put("Authorization", "Bearer " + tokenHandle.getAccessToken());
                    headers.put("Client-Id", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID());

                    BrowserData bd = BrowserClient.executeGETRequest(new URL(
                            "https://api.twitch.tv/helix/users?login=" + queryMap.get("channel") + "&login=" +
                                    queryMap.get("username")), headers);

                    JSONArray userArray =
                            new JSONObject(BrowserClient.requestToString(bd.getResponse())).getJSONArray("data");

                    if (bd.getResponseCode() == 200) {
                        String response;

                        bd = BrowserClient.executeGETRequest(new URL(
                                "https://api.twitch.tv/helix/channels/followers?broadcaster_id="
                                        + userArray.getJSONObject(0).getString("id") + "&user_id="
                                        + userArray.getJSONObject(1).getString("id")), headers);

                        userArray =
                                new JSONObject(BrowserClient.requestToString(bd.getResponse()))
                                        .getJSONArray("data");

                        if (userArray.length() != 0) {
                            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                            format.withZone(ZoneId.of("UTC"));

                            LocalDate date = LocalDate.parse(userArray.getJSONObject(0).getString("followed_at"));
                            LocalDate nowDate = LocalDate.now();

                            Period period = Period.between(date, nowDate).normalized();

                            response = "<html><body>@" + userArray.getJSONObject(0)
                                    .getString("user_name") + " has been following for " + period.getYears() +
                                    (period.getYears() == 1 ? "year" : "years") + " and " + period.getMonths() +
                                    (period.getMonths() == 1 ? "month" : "months") + ", for a total of "
                                    + period.getDays() + (period.getDays() == 1 ? " Day" : " Days") + "</body></html>";
                        } else {
                            response = "<html><body>@" + userArray.getJSONObject(0) + " is not following" +
                                    "the channel</body></html>";
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "<html><body>Cannot find that channel or username!</body></html>";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "<html><body>Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!</body></html>";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().equals("/commands/quote")) {
                TokenHandle tokenHandle = NightbotCommandsWebServer.INSTANCE
                        .getTokenHandle(queryMap.get("moderator"), "moderator:read:followers");
                if (tokenHandle != null) {
                    if (tokenHandle.isExpired()) {
                        NightbotCommandsWebServer.INSTANCE
                                .refreshHandle(exchange.getRequestHeaders().get("User-Agent").get(0), tokenHandle);
                    }

                    Map<String, String> headers = new HashMap<>();

                    headers.put("User-Agent", exchange.getRequestHeaders().get("User-Agent").get(0));
                    headers.put("Authorization", "Bearer " + tokenHandle.getAccessToken());
                    headers.put("Client-Id", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID());

                    BrowserData bd = BrowserClient.executeGETRequest(new URL(
                            "https://api.twitch.tv/helix/users?login=" + queryMap.get("channel")), headers);

                    JSONArray userArray =
                            new JSONObject(BrowserClient.requestToString(bd.getResponse())).getJSONArray("data");

                    if (bd.getResponseCode() == 200) {
                        String response;

                        bd = BrowserClient.executeGETRequest(new URL(
                                "https://api.twitch.tv/helix/channels/followers?broadcaster_id="
                                        + userArray.getJSONObject(0).getString("id")), headers);

                        userArray =
                                new JSONObject(BrowserClient.requestToString(bd.getResponse()))
                                        .getJSONArray("data");

                        if (NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                .containsKey(userArray.getJSONObject(0).getString("id"))) {
                            try {
                                List<String> quotes = NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                        .get(userArray.getJSONObject(0).getString("id"));

                                if (quotes.size() > Integer.parseInt(queryMap.get("index")) - 1 &&
                                        Integer.parseInt(queryMap.get("index")) - 1 > -1) {
                                    response = "<html><body>" + quotes.get(Integer.parseInt(queryMap.get("index")))
                                            + "</body></html>";
                                } else {
                                    response = "<html><body>No quote found under that number!</body></html>";
                                }
                            } catch (NumberFormatException e) {
                                response = "<html><body>That is an invalid number!</body></html>";
                            }
                        } else {
                            response = "<html><body>Channel does not have any quotes!</body></html>";
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "<html><body>Cannot find that channel or username!</body></html>";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "<html><body>Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!</body></html>";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().equals("/commands/addquote")) {
                TokenHandle tokenHandle = NightbotCommandsWebServer.INSTANCE
                        .getTokenHandle(queryMap.get("moderator"), "moderator:read:followers");
                if (tokenHandle != null) {
                    if (tokenHandle.isExpired()) {
                        NightbotCommandsWebServer.INSTANCE
                                .refreshHandle(exchange.getRequestHeaders().get("User-Agent").get(0), tokenHandle);
                    }

                    Map<String, String> headers = new HashMap<>();

                    headers.put("User-Agent", exchange.getRequestHeaders().get("User-Agent").get(0));
                    headers.put("Authorization", "Bearer " + tokenHandle.getAccessToken());
                    headers.put("Client-Id", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID());

                    BrowserData bd = BrowserClient.executeGETRequest(new URL(
                            "https://api.twitch.tv/helix/users?login=" + queryMap.get("channel")), headers);

                    JSONArray userArray =
                            new JSONObject(BrowserClient.requestToString(bd.getResponse())).getJSONArray("data");

                    if (bd.getResponseCode() == 200) {
                        String response;

                        bd = BrowserClient.executeGETRequest(new URL(
                                "https://api.twitch.tv/helix/channels/followers?broadcaster_id="
                                        + userArray.getJSONObject(0).getString("id")), headers);

                        userArray =
                                new JSONObject(BrowserClient.requestToString(bd.getResponse()))
                                        .getJSONArray("data");

                        if (!NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                .containsKey(userArray.getJSONObject(0).getString("id"))) {
                            NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                    .put(userArray.getJSONObject(0).getString("id"), new ArrayList<>());
                        }

                        NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                .get(userArray.getJSONObject(0).getString("id"))
                                .add(URLDecoder.decode(queryMap.get("text"), "UTF-8"));

                        File quotesMapDatabase = new File("quotesMap.dat");
                        NightbotCommandsWebServer.INSTANCE.writeQuotesMap(quotesMapDatabase);

                        response = "<html><body>Added Quote: " +
                                URLDecoder.decode(queryMap.get("text"), "UTF-8") + "</body></html>";

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "<html><body>Cannot find that channel or username!</body></html>";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "<html><body>Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!</body></html>";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().equals("/commands/delquote")) {
                TokenHandle tokenHandle = NightbotCommandsWebServer.INSTANCE
                        .getTokenHandle(queryMap.get("moderator"), "moderator:read:followers");
                if (tokenHandle != null) {
                    if (tokenHandle.isExpired()) {
                        NightbotCommandsWebServer.INSTANCE
                                .refreshHandle(exchange.getRequestHeaders().get("User-Agent").get(0), tokenHandle);
                    }

                    Map<String, String> headers = new HashMap<>();

                    headers.put("User-Agent", exchange.getRequestHeaders().get("User-Agent").get(0));
                    headers.put("Authorization", "Bearer " + tokenHandle.getAccessToken());
                    headers.put("Client-Id", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID());

                    BrowserData bd = BrowserClient.executeGETRequest(new URL(
                            "https://api.twitch.tv/helix/users?login=" + queryMap.get("channel")), headers);

                    JSONArray userArray =
                            new JSONObject(BrowserClient.requestToString(bd.getResponse())).getJSONArray("data");

                    if (bd.getResponseCode() == 200) {
                        String response;

                        bd = BrowserClient.executeGETRequest(new URL(
                                "https://api.twitch.tv/helix/channels/followers?broadcaster_id="
                                        + userArray.getJSONObject(0).getString("id")), headers);

                        userArray =
                                new JSONObject(BrowserClient.requestToString(bd.getResponse()))
                                        .getJSONArray("data");

                        if (!NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                .containsKey(userArray.getJSONObject(0).getString("id"))) {
                            NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                    .put(userArray.getJSONObject(0).getString("id"), new ArrayList<>());
                        }

                        if (NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                .get(userArray.getJSONObject(0).getString("id")).isEmpty()) {
                            response = "<html><body>Channel does not have any quotes!</body></html>";
                        } else {
                            if (NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                    .get(userArray.getJSONObject(0).getString("id")).size()
                                    > Integer.parseInt(queryMap.get("index")) - 1) {
                                String quote = NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                        .get(userArray.getJSONObject(0).getString("id"))
                                        .get(Integer.parseInt(queryMap.get("index")) - 1);

                                NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                        .get(userArray.getJSONObject(0).getString("id"))
                                        .remove(Integer.parseInt(queryMap.get("index")) - 1);

                                File quotesMapDatabase = new File("quotesMap.dat");
                                NightbotCommandsWebServer.INSTANCE.writeQuotesMap(quotesMapDatabase);

                                response = "<html><body>Removed Quote: " + quote + "</body></html>";
                            } else {
                                response = "<html><body>Quote number does not exist</body></html>";
                            }
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "<html><body>Cannot find that channel or username!</body></html>";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "<html><body>Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!</body></html>";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().equals("/commands/quotes")) {
                TokenHandle tokenHandle = NightbotCommandsWebServer.INSTANCE
                        .getTokenHandle(queryMap.get("moderator"), "moderator:read:followers");
                if (tokenHandle != null) {
                    if (tokenHandle.isExpired()) {
                        NightbotCommandsWebServer.INSTANCE
                                .refreshHandle(exchange.getRequestHeaders().get("User-Agent").get(0), tokenHandle);
                    }

                    Map<String, String> headers = new HashMap<>();

                    headers.put("User-Agent", exchange.getRequestHeaders().get("User-Agent").get(0));
                    headers.put("Authorization", "Bearer " + tokenHandle.getAccessToken());
                    headers.put("Client-Id", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID());

                    BrowserData bd = BrowserClient.executeGETRequest(new URL(
                            "https://api.twitch.tv/helix/users?login=" + queryMap.get("channel")), headers);

                    JSONArray userArray =
                            new JSONObject(BrowserClient.requestToString(bd.getResponse())).getJSONArray("data");

                    if (bd.getResponseCode() == 200) {
                        String response;

                        bd = BrowserClient.executeGETRequest(new URL(
                                "https://api.twitch.tv/helix/channels/followers?broadcaster_id="
                                        + userArray.getJSONObject(0).getString("id")), headers);

                        userArray =
                                new JSONObject(BrowserClient.requestToString(bd.getResponse()))
                                        .getJSONArray("data");

                        if (NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                .containsKey(userArray.getJSONObject(0).getString("id"))) {
                            List<String> quotes = NightbotCommandsWebServer.INSTANCE.getQuotesMap()
                                    .get(userArray.getJSONObject(0).getString("id"));
                            List<String> formattedQuotes = new ArrayList<>();
                            int i = 1;
                            for (String s : quotes) {
                                formattedQuotes.add("#" + i + " - " + s);
                            }
                            
                            response = "<html><body>" + StringUtils.join(formattedQuotes, ", ") + "</body></html>";
                        } else {
                            response = "<html><body>Channel does not have any quotes!</body></html>";
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "<html><body>Cannot find that channel or username!</body></html>";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "<html><body>Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!</body></html>";

                    HTTPUtils.throwError(exchange, response);
                }
            } else {
                String response = "<html><body>Command is unidentified!</body></html>";

                HTTPUtils.throwError(exchange, response);
            }
        } else {
            String response = "<html><body>Query requires 'channel' and 'moderator'</body></html>";

            HTTPUtils.throwError(exchange, response);
        }
    }

}

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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            if (exchange.getRequestURI().toString().startsWith("/commands/followage")) {
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
                            if (userArray.getJSONObject(0).getString("user_login").equalsIgnoreCase(queryMap.get("username"))) {
                                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).withZone(ZoneId.of("UTC"));

                                try {
                                    LocalDateTime date = LocalDateTime.parse(userArray.getJSONObject(0).getString("followed_at"), format);
                                    LocalDateTime nowDate = LocalDateTime.now();

                                    Period period = Period.between(date.toLocalDate(), nowDate.toLocalDate()).normalized();

                                    response = "@" + userArray.getJSONObject(0)
                                            .getString("user_name") + " has been following for " +
                                            (period.getYears() == 1 ? "1 year, " : (period.getYears() > 0 ? period.getYears() + " years,  " : ""))  +
                                            (period.getMonths() == 1 ? "1 month, " : (period.getMonths() > 0 ?  period.getMonths() + " months, " : "")) +
                                            (period.getDays() == 1 ? " and 1 day" : period.getDays() > 0 ? " and " + period.getDays() + " days" : "") ;
                                } catch (Exception e) {
                                    response = "Exception Occurred!";
                                    e.printStackTrace();
                                }
                            } else {
                                response = "Unable to grab follower information, please try again!";
                            }
                        } else {
                            response = "@" + queryMap.get("username") + " is not following the channel";
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "Cannot find that channel or username! Please retry your query or check your spelling!";

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    }
                } else {
                    String response = "Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!";

                    HTTPUtils.throwSuccessHTML(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().startsWith("/commands/quote")) {
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

                                if (queryMap.containsKey("index")) {
                                    if (quotes.size() > Integer.parseInt(queryMap.get("index")) - 1 &&
                                            Integer.parseInt(queryMap.get("index")) - 1 > -1) {
                                        response = quotes.get(Integer.parseInt(queryMap.get("index")))
                                                ;
                                    } else {
                                        response = "No quote found under that number!";
                                    }
                                } else {
                                    response = quotes.get(new Random().nextInt(quotes.size()))
                                            ;
                                }
                            } catch (NumberFormatException e) {
                                response = "That is an invalid number!";
                            }
                        } else {
                            response = "Channel does not have any quotes!";
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "Cannot find that channel or username!";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().startsWith("/commands/addquote")) {
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

                        response = "Added Quote: " +
                                URLDecoder.decode(queryMap.get("text"), "UTF-8") ;

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "Cannot find that channel or username!";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().startsWith("/commands/delquote")) {
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
                            response = "Channel does not have any quotes!";
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

                                response = "Removed Quote: " + quote ;
                            } else {
                                response = "Quote number does not exist";
                            }
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "Cannot find that channel or username!";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!";

                    HTTPUtils.throwError(exchange, response);
                }
            } else if (exchange.getRequestURI().toString().startsWith("/commands/quotes")) {
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
                            
                            response = StringUtils.join(formattedQuotes, ", ") ;
                        } else {
                            response = "Channel does not have any quotes!";
                        }

                        HTTPUtils.throwSuccessHTML(exchange, response);
                    } else {
                        String response = "Cannot find that channel or username!";

                        HTTPUtils.throwError(exchange, response);
                    }
                } else {
                    String response = "Moderator is unauthenticated! Head to https://nightbot.logicism.tv/ to Authenticate!";

                    HTTPUtils.throwError(exchange, response);
                }
            } else {
                String response = "Command is unidentified!";

                HTTPUtils.throwError(exchange, response);
            }
        } else {
            String response = "Query requires 'channel' and 'moderator'";

            HTTPUtils.throwError(exchange, response);
        }
    }

}

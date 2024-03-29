package me.Logicism.NightbotCommandsWebServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.net.httpserver.HttpServer;
import me.Logicism.NightbotCommandsWebServer.http.handlers.CommandsHandler;
import me.Logicism.NightbotCommandsWebServer.http.handlers.StaticPageHandler;
import me.Logicism.NightbotCommandsWebServer.http.handlers.TokenHandler;
import me.Logicism.NightbotCommandsWebServer.http.oauth.TokenHandle;
import me.Logicism.NightbotCommandsWebServer.logging.TwitchOverlayLogger;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class NightbotCommandsWebServer {

    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime()
            .availableProcessors());

    private static OverlayConfig config;
    private static TwitchOverlayLogger logger;

    private static List<TokenHandle> tokenHandleList;
    private static Map<TokenHandle, ScheduledFuture> tokenHandleExpirations = new HashMap<>();

    private static Map<String, List<String>> quotesMap;

    private static HttpServer server;

    public static NightbotCommandsWebServer INSTANCE;

    public static void main(String[] args) {
        INSTANCE = new NightbotCommandsWebServer();

        try {
            server = HttpServer.create(new InetSocketAddress(config.getHttpServerIP(), config.getHttpServerPort()),
                    0);

            server.createContext("/", new StaticPageHandler());
            server.createContext("/callback", new TokenHandler());
            server.createContext("/commands", new CommandsHandler());

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            server.setExecutor(executor);

            server.start();

            System.out.println("HTTP is listening on " + config.getHttpServerIP() + ":" + config.getHttpServerPort());

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.stop(0);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NightbotCommandsWebServer() {
        File tokenHandleDatabase = new File("tokenHandles.dat");
        File quotesMapDatabase = new File("quotesMap.dat");

        if (!tokenHandleDatabase.exists()) {
            tokenHandleList = new ArrayList<>();

            try {
                writeTokenHandleList(tokenHandleDatabase);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                readTokenHandleList(tokenHandleDatabase);

                for (TokenHandle tokenHandle : tokenHandleList) {
                    tokenHandle.setExpired(true);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (!quotesMapDatabase.exists()) {
            quotesMap = new HashMap<>();

            try {
                writeQuotesMap(quotesMapDatabase);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                readQuotesMap(quotesMapDatabase);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            config = mapper.readValue(new File("config.yml"), OverlayConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger = new TwitchOverlayLogger(NightbotCommandsWebServer.class);
    }

    public List<TokenHandle> getTokenHandleList() {
        return tokenHandleList;
    }

    public boolean containsUserTokenHandle(String userId, String scope) {
        return getTokenHandle(userId, scope) != null;
    }

    public TokenHandle getTokenHandle(String userId, String scope) {
        for (TokenHandle handle : tokenHandleList) {
            if (handle.getUserId().equals(userId) && handle.getScopes().contains(scope)) {
                return handle;
            }
        }

        return null;
    }

    public void refreshHandle(String userAgent, TokenHandle handle) throws IOException {
        handle.refreshToken(userAgent);

        if (NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().containsKey(handle)) {
            NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().get(handle)
                    .cancel(true);

            NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations().replace(handle,
                    NightbotCommandsWebServer.INSTANCE.getExecutor()
                            .schedule(new Runnable() {
                                @Override
                                public void run() {
                                    handle.setExpired(true);
                                }
                            }, handle.getTimeToExpire(), TimeUnit.SECONDS));
        } else {
            NightbotCommandsWebServer.INSTANCE.getTokenHandleExpirations()
                    .put(handle, NightbotCommandsWebServer.INSTANCE.getExecutor()
                            .schedule(new Runnable() {
                                @Override
                                public void run() {
                                    handle.setExpired(true);
                                }
                            }, handle.getTimeToExpire(), TimeUnit.SECONDS));
        }
    }

    public void readTokenHandleList(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream databaseInputStream = new ObjectInputStream(new FileInputStream(file));

        tokenHandleList = (List<TokenHandle>) databaseInputStream.readObject();
    }

    public void writeTokenHandleList(File file) throws IOException {
        ObjectOutputStream databaseOutputStream = new ObjectOutputStream(new FileOutputStream(file));

        databaseOutputStream.writeObject(tokenHandleList);
    }

    public void readQuotesMap(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream databaseInputStream = new ObjectInputStream(new FileInputStream(file));

        quotesMap = (Map<String, List<String>>) databaseInputStream.readObject();
    }

    public void writeQuotesMap(File file) throws IOException {
        ObjectOutputStream databaseOutputStream = new ObjectOutputStream(new FileOutputStream(file));

        databaseOutputStream.writeObject(quotesMap);
    }

    public OverlayConfig getConfig() {
        return config;
    }

    public Map<TokenHandle, ScheduledFuture> getTokenHandleExpirations() {
        return tokenHandleExpirations;
    }

    public TwitchOverlayLogger getLogger() {
        return logger;
    }

    public ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }

    public Map<String, List<String>> getQuotesMap() {
        return quotesMap;
    }
}

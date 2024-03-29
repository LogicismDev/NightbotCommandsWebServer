package me.Logicism.NightbotCommandsWebServer;

public class OverlayConfig {

    private String httpServerIP;

    private int httpServerPort;

    private String clientID;

    private String clientSecret;

    private String callbackURI;

    private String overlayBaseURL;

    public String getHttpServerIP() {
        return httpServerIP;
    }

    public int getHttpServerPort() {
        return httpServerPort;
    }

    public String getClientID() {
        return clientID;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getCallbackURI() {
        return callbackURI;
    }

    public String getOverlayBaseURL() {
        return overlayBaseURL;
    }

}

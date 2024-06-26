package me.Logicism.NightbotCommandsWebServer.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.Logicism.NightbotCommandsWebServer.NightbotCommandsWebServer;
import me.Logicism.NightbotCommandsWebServer.util.FileUtils;
import me.Logicism.NightbotCommandsWebServer.util.HTTPUtils;

import java.io.IOException;

public class StaticPageHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        NightbotCommandsWebServer.INSTANCE.getLogger().info(exchange.getRequestMethod() + " - " +
                (exchange.getRequestHeaders().containsKey("X-Forwarded-For") ?
                        exchange.getRequestHeaders().get("X-Forwarded-For").get(0) :
                        exchange.getRemoteAddress().getAddress()) + " - " +
                (exchange.getRequestHeaders().containsKey("User-Agent") ?
                        exchange.getRequestHeaders().get("User-Agent").get(0) : "Unknown User-Agent")
                + " - /webhook");

        if (exchange.getRequestURI().toString().equals("/")) {
            String response = FileUtils.fileToString(HTTPUtils.getFile("/index.html"))
                    .replace("{client_id}", NightbotCommandsWebServer.INSTANCE.getConfig().getClientID())
                    .replace("{redirect_uri}", NightbotCommandsWebServer.INSTANCE.getConfig().getCallbackURI());

            HTTPUtils.throwSuccessHTML(exchange, response);
        } else if (exchange.getRequestURI().toString().equals("/privacy")) {
            HTTPUtils.throwSuccessPage(exchange, HTTPUtils.getFile("/privacy.html"));
        } else if (exchange.getRequestURI().toString().equals("/terms")) {
            HTTPUtils.throwSuccessPage(exchange, HTTPUtils.getFile("/terms.html"));
        } else if (HTTPUtils.containsPage(exchange.getRequestURI().toString())) {
            HTTPUtils.throwSuccessPage(exchange, HTTPUtils.getFile(exchange.getRequestURI().toString()));
        } else if (HTTPUtils.containsFile(exchange.getRequestURI().toString())) {
            HTTPUtils.throwSuccessFile(exchange, HTTPUtils.getFile(exchange.getRequestURI().toString()));
        } else {
            HTTPUtils.throw404(exchange);
        }
    }


}

package io.github.colinzhu.webconsole;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static io.github.colinzhu.webconsole.SysOutToEventBus.EVENT_CONSOLE_MSG_CREATED;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class WebVerticle extends AbstractVerticle {
    private boolean isTaskRunning = false;
    private final HttpServerOptions options;

    static final String EVENT_START_PARAMS_RECEIVED = "start_params_received";

    @Override
    public void start() {
        startWebServer()
                .onSuccess(webServer -> log.info("Web console server started, url: {}", getServerUrl(options)))
                .onFailure(err -> log.error("Failed to start the web console server", err));
    }

    private Future<HttpServer> startWebServer() {
        HttpServer server = vertx.createHttpServer(options);
        server.webSocketHandler(this::onWebSocketConnected);

        Router router = Router.router(vertx);
        router.route().handler(StaticHandler.create("web"));
        server.requestHandler(router);

        return server.listen();
    }

    private void onWebSocketConnected(ServerWebSocket webSocket) {
        webSocket.writeTextMessage("Welcome to the web console!");
        webSocket.writeTextMessage("Is the task running? " + isTaskRunning);
        vertx.eventBus().consumer(EVENT_CONSOLE_MSG_CREATED, message -> {
            webSocket.writeTextMessage((String) message.body()); // redirect the message to websocket (web)
        });
        webSocket.textMessageHandler(this::onMessageReceived);
    }

    private void onMessageReceived(String socketMsg) {
        log.info("Message received: {}", socketMsg);
        if (socketMsg.startsWith("startParams=")) {
            if (isTaskRunning) {
                log.info("One task is still running, cannot trigger to run now.");
                return;
            }
            isTaskRunning = true;
            vertx.eventBus().request(EVENT_START_PARAMS_RECEIVED, socketMsg)
                    .onSuccess(msg -> isTaskRunning = false);
        }
    }

    private static String getServerUrl(HttpServerOptions options) {
        String protocol = options.isSsl() ? "https://" : "http://";
        String host = options.getHost().equals("0.0.0.0") ? "127.0.0.1" : options.getHost();
        return protocol + host + ":" + options.getPort();
    }
}

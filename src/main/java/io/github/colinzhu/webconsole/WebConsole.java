package io.github.colinzhu.webconsole;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class WebConsole extends AbstractVerticle {
    private final Consumer<String[]> task;
    private boolean isTaskRunning = false;

    private static WebConsole instance;

    /**
     * Start the web console server with given port number without SSL
     * @param task the task to be executed
     * @param port web server port number
     */
    public static synchronized void start(Consumer<String[]> task, int port) {
        start(task, new HttpServerOptions().setPort(port));
    }
    /**
     * Start the web console server
     * @param task the task to be executed
     * @param options <a href="https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html">HttpServerOptions</a> to start web server
     */
    public static synchronized void start(Consumer<String[]> task, HttpServerOptions options) {
        if (instance == null) { // only deploy once
            VertxOptions vertxOptions = new VertxOptions().setMaxWorkerExecuteTime(TimeUnit.MINUTES.toNanos(Long.MAX_VALUE)); //to prevent vert.x warning message
            instance = new WebConsole(task);
            Vertx.vertx(vertxOptions).deployVerticle(instance) // deploy the WebConsole verticle
                    .compose(deployed -> instance.startWebServer(options)).onSuccess(webServer -> { // start the web server
                        log.info("Web console server started, url:" + WebConsole.getServerUrl(options));
                        instance.redirectStdOutToWeb();
                    }).onFailure(err -> log.error("Failed to start the web console server", err));
        }
    }

    private Future<HttpServer> startWebServer(HttpServerOptions options) {
        HttpServer server = vertx.createHttpServer(options);
        server.webSocketHandler(this::onWebSocketConnected);

        Router router = Router.router(vertx);
        router.route().handler(StaticHandler.create("web"));
        server.requestHandler(router);

        return server.listen();
    }

    private void redirectStdOutToWeb() {
        OutputStream webConsoleOutputStream = new OutputStream() {
            private final OutputStream oriOutStream = System.out;
            private final StringBuilder sb = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    vertx.eventBus().publish("console.log", sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append((char) b);
                }
                try {
                    oriOutStream.write(b); //keep the original console output
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        System.setOut(new PrintStream(webConsoleOutputStream));
    }

    private void onWebSocketConnected(ServerWebSocket webSocket) {
        webSocket.writeTextMessage("Welcome to the web console!");
        webSocket.writeTextMessage("Is the task running? " + isTaskRunning);
        vertx.eventBus().consumer("console.log", message -> {
            webSocket.writeTextMessage((String) message.body()); // redirect the message to websocket (web)
        });
        webSocket.textMessageHandler(this::onMessageReceived);
    }

    private void onMessageReceived(String msg) {
        log.info("Message received: {}", msg);
        if (msg.startsWith("startParams=")) {
            if (isTaskRunning) {
                log.info("One task is still running, cannot trigger to run now.");
                return;
            }
            isTaskRunning = true;
            String[] params = msg.replace("startParams=", "").split(" ");
            vertx.executeBlocking(promise -> {
                try {
                    task.accept(params);
                } catch (Exception e) {
                    promise.fail(e);
                }
                promise.complete();
            }).onSuccess(v -> {
                log.info("executeBlock success");
                isTaskRunning = false;
            }).onFailure(err -> {
                log.error("executeBlock error", err);
                isTaskRunning = false;
            });
        }
    }

    private static String getServerUrl(HttpServerOptions options) {
        String protocol = options.isSsl() ? "https://" : "http://";
        String host = options.getHost().equals("0.0.0.0") ? "127.0.0.1" : options.getHost();
        return protocol + host + ":" + options.getPort();
    }
}
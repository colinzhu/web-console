package colinzhu.webconsole;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
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

    private final int port;

    public static synchronized void start(Consumer<String[]> task, int port) {
        if (instance == null) { // only deploy once
            VertxOptions vertxOptions = new VertxOptions().setMaxWorkerExecuteTime(TimeUnit.MINUTES.toNanos(100L));
            instance = new WebConsole(task, port);
            Vertx.vertx(vertxOptions).deployVerticle(instance);
        }
    }

    @Override
    public void start() {
        startWebServer().onSuccess(httpServer -> {
            log.info("server started at " + port);
            redirectStdOutToWeb();
        }).onFailure(err -> log.error("failed to start server", err));
    }

    private Future<HttpServer> startWebServer() {
        HttpServer server = vertx.createHttpServer();
        server.webSocketHandler(this::onWebSocketConnected);

        Router router = Router.router(vertx);
        router.route().handler(StaticHandler.create("webroot"));
        server.requestHandler(router);

        return server.listen(port);
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
}
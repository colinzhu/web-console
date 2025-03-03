package io.github.colinzhu.webconsole;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class WebConsole {
    private final Vertx vertx;
    private final Consumer<String[]> task;

    private static WebConsole instance;

    /**
     * Start the web console server with preTask, given port number
     *
     * @param preTask the task to be auto executed before the main task
     * @param task    the main task to be executed
     * @param port    web server port number
     */
    public static synchronized void start(Runnable preTask, Consumer<String[]> task, int port) {
        start(preTask, task, new HttpServerOptions().setPort(port));
    }

    /**
     * Start the web console server with given port number
     *
     * @param task the main task to be executed
     * @param port web server port number
     */
    public static synchronized void start(Consumer<String[]> task, int port) {
        start(null, task, new HttpServerOptions().setPort(port));
    }

    /**
     * Start the web console server
     *
     * @param preTask the task to be auto executed before the main task
     * @param task    the main task to be executed
     * @param options <a href="https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html">HttpServerOptions</a> to start web server
     */
    public static synchronized void start(Runnable preTask, Consumer<String[]> task, HttpServerOptions options) {
        if (instance == null) {
            VertxOptions vertxOptions = new VertxOptions().setMaxWorkerExecuteTime(TimeUnit.MINUTES.toNanos(Long.MAX_VALUE)); //to prevent vert.x warning message
            Vertx vertx = Vertx.vertx(vertxOptions);

            SysOutToEventBus.setup(vertx);
            instance = new WebConsole(vertx, task);
            vertx.eventBus().consumer(WebVerticle.EVENT_START_PARAMS_RECEIVED, instance::execute);
            vertx.deployVerticle(new WebVerticle(options));

            if (preTask != null) {
                preTask.run();
            }
        }
    }

    private void execute(Message<Object> msg) {
        String body = (String) msg.body();
        String[] params = body.replace("startParams=", "").split(" ");
        vertx.executeBlocking(promise -> {
                    try {
                        task.accept(params);
                    } catch (Exception e) {
                        promise.fail(e);
                    }
                    promise.complete();
                })
                .onSuccess(v -> {
                    log.info("executeBlock success");
                    msg.reply("done");
                })
                .onFailure(err -> {
                    log.error("executeBlock error", err);
                    msg.fail(1, err.getMessage());
                });
    }
}
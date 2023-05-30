package io.github.colinzhu.webconsole;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;

public class Example {
    public static void main(String[] args) {
        // Example to start web console without SSL
        // WebConsole.start(ExampleTask::main, 8080);

        // Example to start web console without SSL
        WebConsole.start(() -> System.out.println("this is preTask hello"), ExampleTask::main, 8080);

        // Example to start web console with SSL
//        HttpServerOptions options = new HttpServerOptions()
//                .setPort(8080)
//                .setSsl(true)
//                .setHost("example.xyz")
//                .setKeyCertOptions(new PemKeyCertOptions().setCertPath("chain.pem").setKeyPath("key.pem"));
//        WebConsole.start(null, ExampleTask::main, options);
    }
}

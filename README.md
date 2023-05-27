# Web Console

Web Console is a super simple java tool that allows you to trigger a task (running on a server) from a web browser and see the output in real time. It uses Vert.x framework to create a web server and a websocket handler, and redirects the standard output to the web console. 

## How to use

1. Add the dependency `<groupId>io.github.colinzhu</groupId><artifactId>web-console</artifactId><version>0.1.0</version>` into your project
2. Invoke `WebConsole.start(...)` with your task (implements Consumer<String[]>) and port number
3. Run your app, which will start a web server e.g. `http://localhost:8082` 
4. Open a web browser and navigate to `http://localhost:8082`. You should see a web console with a welcome message and a prompt to enter the parameters for the task.
5. Enter the parameters for the task separated by spaces and click "Start". The task will start running and you will see the output in the web console.

## Example

Suppose you have a task that prints “Hello, world!” n times, where n is a parameter. You can create a class that implements Consumer<String[]> and pass it to the WebConsole.start() method. For example:

```java
package colinzhu.webconsole;

import java.util.function.Consumer;

public class HelloWorldTask {

    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]); // get the parameter from args
        for (int i = 0; i < n; i++) {
            System.out.println("Hello, world!"); // print to standard output
        }
    }
}
```

Then, in your App(main) class, you can start the web console with this task:

```java
import io.github.colinzhu.webconsole.WebConsole;

public class App {
    public static void main(String[] args) {
        WebConsole.start(HelloWorldTask::main, 8082); // start the web console with HelloWorldTask and port 8082
    }
}
```

Now, if you go to http://localhost:8082 and enter 5 as the parameter, you will see this output:

```text
Welcome to the web console!
Is the task running? false
5
Hello, world!
Hello, world!
Hello, world!
Hello, world!
Hello, world!

```

## Example with SSL
To start the web console with SSL:

```java
import io.github.colinzhu.webconsole.WebConsole;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;

public class App {
    public static void main(String[] args) {
        // Example to start web console with SSL e.g. https://example.xyz:8082
        HttpServerOptions options = new HttpServerOptions()
                .setPort(8082)
                .setSsl(true)
                .setHost("example.xyz")
                .setKeyCertOptions(new PemKeyCertOptions().setCertPath("chain.pem").setKeyPath("private_key.pem"));
        WebConsole.start(HelloWorldTask::main, options);
    }
}
```

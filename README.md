# Web Console

Web Console is a super simple java tool that allows you to: 
1. from a web browser, trigger a task to run  
2. and from the web browser, see the output in real time

## Usage with example
Suppose you have a task that prints “Hello, world!” n times, where n is a parameter. You want to trigger it to run from a web browser and see the output in real time.

```java
public class HelloWorld {
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]); // get the parameter from args
        for (int i = 0; i < n; i++) {
            System.out.println("Hello, world!"); // print to standard output
        }
    }
}
```
Step 1: Add the dependency into your project:
   ```xml
   <dependency>
       <groupId>io.github.colinzhu</groupId>
       <artifactId>web-console</artifactId>
       <version>0.1.2</version>
   </dependency>
   ```
Step 2: write a method which wraps the original task using `WebConsole.start(...)` like below.

```java
import io.github.colinzhu.webconsole.WebConsole;

public class App {
    public static void main(String[] args) {
        WebConsole.start(HelloWorld::main, 8082); // start the web console with HelloWorldTask and port 8082
    }
}
```
Step 3: run your App.main(), and go to http://localhost:8082 and enter 5 as the parameter and click the "Start" button, you will see this output:

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

## Customize the UI page
Web Console comes with a default UI page

You can customize UI page by creating your own index.html and put it into classpath as `/web/index.html`, for example create a file `resources/web/index.html` in your project

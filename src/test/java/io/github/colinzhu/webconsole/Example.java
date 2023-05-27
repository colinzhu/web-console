package io.github.colinzhu.webconsole;

public class Example {
    public static void main(String[] args) {
        WebConsole.start(ExampleTask::main, 8082);
    }
}

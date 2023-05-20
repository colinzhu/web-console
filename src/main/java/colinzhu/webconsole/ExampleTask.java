package colinzhu.webconsole;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class ExampleTask {

    public static void main(String[] args) {
        log.info("Example task started");
        int n = args != null && args.length > 0 && Pattern.matches("\\d+", args[0]) ? Integer.parseInt(args[0]) : 30;
        for (int i = 0; i < n; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("running " + System.currentTimeMillis());
        }
        log.info("Example task completed");
    }
}
package colinzhu.webconsole;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExampleTask {

    public static void main(String[] args) {
        log.info("Example task started");
        for (int i = 0; i < 30; i++) {
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
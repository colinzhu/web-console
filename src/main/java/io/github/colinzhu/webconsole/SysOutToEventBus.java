package io.github.colinzhu.webconsole;

import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
class SysOutToEventBus {
    private final Vertx vertx;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private static SysOutToEventBus instance;

    static final String EVENT_CONSOLE_MSG_CREATED = "console_messages_created";

    static synchronized void setup(Vertx vertx) {
        if (instance == null) {
            instance = new SysOutToEventBus(vertx);
            instance.sysOutToMessageQueue();
            instance.messageQueueToEventBus();
        }
    }

    private void messageQueueToEventBus() {
        vertx.setPeriodic(500, id -> {
            List<String> batch = new ArrayList<>();
            messageQueue.drainTo(batch);
            if (!batch.isEmpty()) {
                vertx.eventBus().publish(EVENT_CONSOLE_MSG_CREATED, String.join("\n", batch));
            }
        });
    }

    private void sysOutToMessageQueue() {
        OutputStream messageQueueOutputStream = new OutputStream() {
            private final BufferedOutputStream oriOutStream = new BufferedOutputStream(System.out);
            private final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    // Convert the byte array into a string and queue the message
                    boolean offered = messageQueue.offer(byteArrayStream.toString());
                    if (!offered) {
                        System.err.println("Message queue is full. Dropping message.");
                    }
                    byteArrayStream.reset();  // Clear the buffer
                } else {
                    byteArrayStream.write(b);  // Buffer the log message
                }
                // Write to the original System.out in a buffered manner
                oriOutStream.write(b);
            }

            @Override
            public void flush() throws IOException {
                oriOutStream.flush();  // Ensure the buffered output is flushed
            }
        };

        // Redirect System.out to our custom PrintStream
        System.setOut(new PrintStream(messageQueueOutputStream, true));
    }
}

package org.mineUGC.storage.yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class YamlWatcher implements AutoCloseable {
    private final Path directory;
    private final Consumer<File> onChange;
    private final Logger logger;
    private WatchService watchService;
    private ExecutorService executor;
    private volatile boolean running;

    public YamlWatcher(Path directory, Consumer<File> onChange, Logger logger) {
        this.directory = directory;
        this.onChange = onChange;
        this.logger = logger;

        File dir = directory.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        running = true;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mineugc-yaml-watcher");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::poll);
    }

    private void poll() {
        while (running) {
            try {
                WatchKey key = watchService.poll(1000, TimeUnit.MILLISECONDS);
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path filename = (Path) event.context();
                    if (filename.toString().endsWith(".yml") || filename.toString().endsWith(".yaml")) {
                        File changed = directory.resolve(filename).toFile();
                        logger.info("Detected change: " + changed.getName());
                        onChange.accept(changed);
                    }
                }
                if (!key.reset()) {
                    logger.warning("Watch key is no longer valid, stopping watcher");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warning("YamlWatcher error: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
    }
}

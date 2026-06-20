package copilot.overlay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AdviceReader {

    private static final long POLL_INTERVAL_MS = 500;
    private static final String ENV_VAR = "COPILOT_ADVICE_PATH";
    private static final String DEFAULT_FILENAME = "output/advice.txt";

    private final Path adviceFilePath;
    private final ScheduledExecutorService scheduler;
    private final Consumer<AdviceData> onAdviceChanged;
    private String lastReadContent;

    public AdviceReader(Consumer<AdviceData> onAdviceChanged) {
        this.onAdviceChanged = onAdviceChanged;
        this.adviceFilePath = resolvePath();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AdviceReader");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::poll, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void poll() {
        try {
            if (!Files.exists(adviceFilePath)) {
                return;
            }
            String content = new String(Files.readAllBytes(adviceFilePath), "UTF-8");
            if (!content.equals(lastReadContent)) {
                lastReadContent = content;
                AdviceData data = parseAdvice(content);
                data.timestamp = System.currentTimeMillis();
                onAdviceChanged.accept(data);
            }
        } catch (IOException ignored) {
        }
    }

    private AdviceData parseAdvice(String content) {
        AdviceData data = new AdviceData();
        data.rawText = content;

        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith("推荐")) {
                data.recommendation = extractValue(line);
            } else if (line.startsWith("理由")) {
                data.reason = extractValue(line);
            } else if (line.startsWith("风险")) {
                data.risk = extractValue(line);
            } else if (line.startsWith("吐槽")) {
                data.comment = extractValue(line);
            }
        }
        return data;
    }

    private String extractValue(String line) {
        int idx = line.indexOf('：');
        if (idx == -1) idx = line.indexOf(':');
        if (idx >= 0 && idx + 1 < line.length()) {
            return line.substring(idx + 1).trim();
        }
        return "";
    }

    private Path resolvePath() {
        String envPath = System.getenv(ENV_VAR);
        if (envPath != null && !envPath.isEmpty()) {
            return Paths.get(envPath);
        }
        Path workingDir = Paths.get("").toAbsolutePath();
        return workingDir.resolve(DEFAULT_FILENAME);
    }

    public static class AdviceData {
        public String rawText = "";
        public String recommendation = "";
        public String reason = "";
        public String risk = "";
        public String comment = "";
        public long timestamp;
    }
}

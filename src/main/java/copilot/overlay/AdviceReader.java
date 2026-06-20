package copilot.overlay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class AdviceReader {

    private static final long POLL_INTERVAL_MS = 500;
    private static final String ENV_VAR = "COPILOT_ADVICE_PATH";
    private static final String DEFAULT_JSON_FILENAME = "output/overlay.json";
    private static final String DEFAULT_TXT_FILENAME = "output/advice.txt";

    private final Path jsonFilePath;
    private final Path txtFilePath;
    private final ScheduledExecutorService scheduler;
    private final Consumer<AdviceData> onAdviceChanged;
    private final OverlayConfig config;
    private String lastJsonContent;
    private String lastTxtContent;

    public AdviceReader(Consumer<AdviceData> onAdviceChanged, OverlayConfig config) {
        this.onAdviceChanged = onAdviceChanged;
        this.config = config;
        Path[] paths = resolvePaths();
        this.jsonFilePath = paths[0];
        this.txtFilePath = paths[1];
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
            if (Files.exists(jsonFilePath)) {
                String content = new String(Files.readAllBytes(jsonFilePath), "UTF-8");
                if (!content.equals(lastJsonContent)) {
                    lastJsonContent = content;
                    AdviceData data = parseJson(content);
                    if (data != null) {
                        data.stale = isJsonStale(data);
                        onAdviceChanged.accept(data);
                        return;
                    }
                    data = parseAdvice(content);
                    data.timestamp = System.currentTimeMillis();
                    data.fromJson = false;
                    data.status = "ok";
                    data.stale = isFileStale(jsonFilePath);
                    onAdviceChanged.accept(data);
                }
                return;
            }

            if (Files.exists(txtFilePath)) {
                String content = new String(Files.readAllBytes(txtFilePath), "UTF-8");
                if (!content.equals(lastTxtContent)) {
                    lastTxtContent = content;
                    AdviceData data = parseAdvice(content);
                    data.timestamp = System.currentTimeMillis();
                    data.fromJson = false;
                    data.status = "ok";
                    data.stale = isFileStale(txtFilePath);
                    onAdviceChanged.accept(data);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private AdviceData parseJson(String content) {
        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(content);

            AdviceData data = new AdviceData();
            data.rawText = content;
            data.fromJson = true;
            data.status = root.getString("status", "error");
            data.overlayVisibility = root.getBoolean("overlay_visibility", false);
            data.timestamp = root.getLong("timestamp_ms", 0);
            data.scenario = root.getString("scenario", "");
            data.inCombat = root.getBoolean("in_combat", false);
            data.stateHash = root.getString("state_hash", "");

            if (root.has("screen_type")) {
                JsonValue st = root.get("screen_type");
                if (!st.isNull()) data.screenType = st.asString();
            }
            if (root.has("floor")) {
                JsonValue f = root.get("floor");
                if (!f.isNull()) data.floor = f.asInt();
            }
            if (root.has("character")) {
                JsonValue c = root.get("character");
                if (!c.isNull()) data.character = c.asString();
            }

            JsonValue advice = root.get("advice");
            if (advice != null) {
                data.recommendation = advice.getString("recommendation", "");
                data.reason = advice.getString("reason", "");
                data.risk = advice.getString("risk", "");
                data.comment = advice.getString("commentary", "");
            }

            return data;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isJsonStale(AdviceData data) {
        if (data.overlayVisibility && data.timestamp > 0) {
            long age = System.currentTimeMillis() - data.timestamp;
            return age > config.maxDataAgeMs;
        }
        return false;
    }

    private boolean isFileStale(Path path) {
        try {
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis();
            return age > config.maxDataAgeMs;
        } catch (IOException e) {
            return false;
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

    private Path[] resolvePaths() {
        String envPath = System.getenv(ENV_VAR);
        if (envPath != null && !envPath.isEmpty()) {
            Path p = Paths.get(envPath);
            return new Path[] { p, p };
        }
        Path workingDir = Paths.get("").toAbsolutePath();
        return new Path[] {
            workingDir.resolve(DEFAULT_JSON_FILENAME),
            workingDir.resolve(DEFAULT_TXT_FILENAME)
        };
    }

    public static class AdviceData {
        public String rawText = "";
        public String recommendation = "";
        public String reason = "";
        public String risk = "";
        public String comment = "";
        public long timestamp;

        public String status = "error";
        public boolean overlayVisibility = false;
        public String screenType = null;
        public String scenario = "";
        public boolean inCombat = false;
        public String stateHash = "";
        public Integer floor = null;
        public String character = null;
        public boolean fromJson = false;
        public boolean stale = false;
    }
}

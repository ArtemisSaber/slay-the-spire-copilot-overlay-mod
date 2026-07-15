package copilot.overlay;

import basemod.BaseMod;
import basemod.interfaces.PostUpdateSubscriber;
import basemod.interfaces.RenderSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

@SpireInitializer
public class CopilotOverlayMod implements RenderSubscriber, PostUpdateSubscriber {

    private static CopilotOverlayMod instance;
    private AdviceOverlay overlay;
    private AdviceReader reader;
    private OverlayConfig config;
    private final AtomicLong revisionCounter = new AtomicLong(1);

    public static void initialize() {
        instance = new CopilotOverlayMod();
        BaseMod.subscribe(instance);
    }

    public CopilotOverlayMod() {
        config = new OverlayConfig();
        overlay = new AdviceOverlay(config);
        overlay.setAutoplayWriter(this::writeAutoplayControl);
        reader = new AdviceReader(overlay::setAdvice, config);
        reader.start();
    }

    @Override
    public void receiveRender(SpriteBatch sb) {
        overlay.render(sb);
    }

    @Override
    public void receivePostUpdate() {
        if (Gdx.input.isKeyJustPressed(config.hotkeyPrimary)) {
            config.visible = !config.visible;
        }
        overlay.checkButtonClick();
    }

    private void writeAutoplayControl(String mode) {
        Path controlPath = reader.getOutputDir().resolve("autoplay-control.json");
        long rev = revisionCounter.getAndIncrement();
        String json = "{" +
                "\"schema_version\":1," +
                "\"revision\":" + rev + "," +
                "\"mode\":\"" + mode + "\"," +
                "\"allow_combat\":true," +
                "\"allow_rest\":true," +
                "\"allow_events\":true," +
                "\"allow_shop\":true," +
                "\"allow_map\":true," +
                "\"allow_card_rewards\":true," +
                "\"allow_potion_decision\":true," +
                "\"allow_grid_select\":true" +
                "}";
        try {
            Path tmpPath = controlPath.resolveSibling(controlPath.getFileName() + ".tmp");
            Files.write(tmpPath, json.getBytes("UTF-8"));
            Files.move(tmpPath, controlPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
        }
    }
}

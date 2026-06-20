package copilot.overlay;

import basemod.BaseMod;
import basemod.interfaces.RenderSubscriber;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

@SpireInitializer
public class CopilotOverlayMod implements RenderSubscriber {

    private static CopilotOverlayMod instance;
    private AdviceOverlay overlay;
    private AdviceReader reader;

    public static void initialize() {
        instance = new CopilotOverlayMod();
        BaseMod.subscribe(instance);
    }

    public CopilotOverlayMod() {
        OverlayConfig config = new OverlayConfig();
        overlay = new AdviceOverlay(config);
        reader = new AdviceReader(overlay::setAdvice, config);
        reader.start();
    }

    @Override
    public void receiveRender(SpriteBatch sb) {
        overlay.render(sb);
    }
}

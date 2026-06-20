package copilot.overlay;

public class OverlayConfig {

    public float positionX = 20.0f;
    public float positionY = 205.0f;
    public boolean visible = true;
    public long maxDataAgeMs = 60_000;
    public float fadeDurationMs = 2_000;
    public int hotkeyPrimary = com.badlogic.gdx.Input.Keys.F2;
    public int hotkeyModifier = com.badlogic.gdx.Input.Keys.CONTROL_LEFT;
    public boolean hideDuringCombat = true;
}

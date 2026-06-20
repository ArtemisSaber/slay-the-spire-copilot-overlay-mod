package copilot.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.core.Settings.GameLanguage;
import com.megacrit.cardcrawl.helpers.FontHelper;

public class AdviceOverlay {

    private static final float BOX_WIDTH = 360.0f;
    private static final float BOX_PADDING = 14.0f;
    private static final float FIELD_GAP = 6.0f;
    private static final float CONTENT_INDENT = 10.0f;
    private static final Color BG_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.1f);
    private static final Color BORDER_COLOR = new Color(0.3f, 0.3f, 0.3f, 0.9f);
    private static final Color LABEL_COLOR = Color.CYAN;
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ERROR_COLOR = new Color(0.9f, 0.4f, 0.4f, 1.0f);
    private static final float FADE_IN_MS = 400f;
    private static final float CONTENT_DIP_MS = 200f;
    private static final float CONTENT_DIP_MIN = 0.7f;
    private static final float PULSE_CYCLE_MS = 1500f;
    private static final float PULSE_MIN = 0.4f;
    private static final float ELLIPSIS_CYCLE_MS = 500f;

    private final OverlayConfig config;
    private volatile AdviceReader.AdviceData currentAdvice = new AdviceReader.AdviceData();
    private ShapeRenderer shapeRenderer;
    private boolean fadingIn;
    private long fadeInStartTime;
    private float fadeInDurationMs;
    private boolean fadingOut;
    private long fadeOutStartTime;
    private float alphaFactor;
    private String lastContentHash = "";

    public AdviceOverlay(OverlayConfig config) {
        this.config = config;
    }

    public void setAdvice(AdviceReader.AdviceData advice) {
        this.currentAdvice = advice;
    }

    public void render(SpriteBatch sb) {
        if (!config.visible) return;

        computeAlphaFactor();
        if (alphaFactor <= 0) return;

        float x = Settings.WIDTH - BOX_WIDTH - config.positionX;
        float y = Settings.HEIGHT - config.positionY;

        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }

        float height = measureHeight();
        float boxTop = y;
        float boxBottom = y - height;

        sb.end();

        shapeRenderer.setProjectionMatrix(sb.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(fade(BG_COLOR));
        shapeRenderer.rect(x, boxBottom, BOX_WIDTH, height);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(fade(BORDER_COLOR));
        shapeRenderer.rect(x, boxBottom, BOX_WIDTH, height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        sb.begin();

        BitmapFont bodyFont = FontHelper.tipBodyFont;
        float lineSpacing = bodyFont.getLineHeight();
        float contentMaxWidth = BOX_WIDTH - BOX_PADDING * 2 - CONTENT_INDENT;

        float textX = x + BOX_PADDING;
        float textY = boxTop - BOX_PADDING - lineSpacing;

        if (currentAdvice == null || "loading".equals(currentAdvice.status)) {
            float pulse = getPulseAlpha();
            Color c = fade(Color.GRAY);
            c.a *= pulse;
            FontHelper.renderFontLeftTopAligned(sb, bodyFont,
                    getAnimatedLoadingText(), textX, boxTop - BOX_PADDING, c);
            return;
        }

        if (currentAdvice.recommendation.isEmpty()
                && currentAdvice.reason.isEmpty()
                && currentAdvice.risk.isEmpty()
                && currentAdvice.comment.isEmpty()) {
            return;
        }

        boolean isError = "error".equals(currentAdvice.status);
        Color labelCol = isError ? fade(ERROR_COLOR) : fade(LABEL_COLOR);
        Color textCol = isError ? fade(ERROR_COLOR) : fade(TEXT_COLOR);

        textY = drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                getLabelRecommendation(), currentAdvice.recommendation, labelCol, textCol, textX, textY);
        textY = drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                getLabelReason(), currentAdvice.reason, labelCol, textCol, textX, textY);
        textY = drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                getLabelRisk(), currentAdvice.risk, labelCol, textCol, textX, textY);
        if (currentAdvice.comment != null && !currentAdvice.comment.isEmpty()) {
            drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                    getLabelComment(), currentAdvice.comment, labelCol, textCol, textX, textY);
        }
    }

    private float measureHeight() {
        BitmapFont bodyFont = FontHelper.tipBodyFont;
        float lineSpacing = bodyFont.getLineHeight();
        float contentMaxWidth = BOX_WIDTH - BOX_PADDING * 2 - CONTENT_INDENT;

        float h = BOX_PADDING * 2 + lineSpacing;
        AdviceReader.AdviceData adv = currentAdvice;
        if (adv == null || "loading".equals(adv.status)) return BOX_PADDING * 2 + lineSpacing;
        if (adv.recommendation.isEmpty() && adv.reason.isEmpty()
                && adv.risk.isEmpty() && adv.comment.isEmpty()) return BOX_PADDING * 2 + lineSpacing;
        h += fieldHeight(bodyFont, lineSpacing, contentMaxWidth, adv.recommendation);
        h += fieldHeight(bodyFont, lineSpacing, contentMaxWidth, adv.reason);
        h += fieldHeight(bodyFont, lineSpacing, contentMaxWidth, adv.risk);
        h += fieldHeight(bodyFont, lineSpacing, contentMaxWidth, adv.comment);
        return Math.max(h, 140.0f);
    }

    private float fieldHeight(BitmapFont font, float lineSpacing, float maxWidth, String value) {
        if (value == null || value.isEmpty()) return 0;
        float contentHeight = lineSpacing
                - FontHelper.getSmartHeight(font, value, maxWidth, lineSpacing);
        return lineSpacing + contentHeight + FIELD_GAP;
    }

    private float drawField(SpriteBatch sb, BitmapFont font, float lineSpacing, float maxWidth,
                            String label, String value, Color labelColor, Color textColor,
                            float x, float y) {
        if (value == null || value.isEmpty()) return y;
        FontHelper.renderFontLeftTopAligned(sb, font, label + ":", x, y, labelColor);
        y -= lineSpacing;
        FontHelper.renderSmartText(sb, font, value, x + CONTENT_INDENT, y, maxWidth, lineSpacing, textColor);
        float contentHeight = lineSpacing
                - FontHelper.getSmartHeight(font, value, maxWidth, lineSpacing);
        y -= contentHeight + FIELD_GAP;
        return y;
    }

    private void computeAlphaFactor() {
        if (!config.visible) {
            resetFade();
            return;
        }

        AdviceReader.AdviceData adv = currentAdvice;
        if (adv == null || adv.stale) {
            resetFade();
            return;
        }

        String contentHash = adv.status + "|" + adv.recommendation + "|"
                + adv.reason + "|" + adv.risk + "|" + adv.comment;
        boolean contentChanged = !contentHash.equals(lastContentHash);
        lastContentHash = contentHash;

        boolean shouldShow = adv.overlayVisibility || "loading".equals(adv.status);
        long now = System.currentTimeMillis();

        if (!shouldShow) {
            if (!fadingOut) {
                fadingOut = true;
                fadeOutStartTime = now;
            }
            fadingIn = false;
            long elapsed = now - fadeOutStartTime;
            if (elapsed >= config.fadeDurationMs) {
                alphaFactor = 0;
            } else {
                alphaFactor = 1.0f - (float) elapsed / config.fadeDurationMs;
            }
            return;
        }

        fadingOut = false;

        boolean isFullFadeIn = fadingIn && fadeInDurationMs == FADE_IN_MS;

        if (!fadingIn && alphaFactor < 0.1f) {
            fadingIn = true;
            fadeInStartTime = now;
            fadeInDurationMs = FADE_IN_MS;
        } else if (contentChanged && !"loading".equals(adv.status) && !isFullFadeIn) {
            fadingIn = true;
            fadeInStartTime = now;
            fadeInDurationMs = CONTENT_DIP_MS;
        }

        if (fadingIn) {
            long elapsed = now - fadeInStartTime;
            if (elapsed >= fadeInDurationMs) {
                fadingIn = false;
                alphaFactor = 1.0f;
            } else if (fadeInDurationMs == CONTENT_DIP_MS) {
                float t = (float) elapsed / CONTENT_DIP_MS;
                if (t < 0.5f) {
                    alphaFactor = 1.0f - (1.0f - CONTENT_DIP_MIN) * (t / 0.5f);
                } else {
                    alphaFactor = CONTENT_DIP_MIN + (1.0f - CONTENT_DIP_MIN) * ((t - 0.5f) / 0.5f);
                }
            } else {
                alphaFactor = (float) elapsed / FADE_IN_MS;
            }
            return;
        }

        alphaFactor = 1.0f;
    }

    private void resetFade() {
        fadingIn = false;
        fadingOut = false;
        alphaFactor = 0;
    }

    private Color fade(Color base) {
        Color c = base.cpy();
        c.a *= alphaFactor;
        return c;
    }

    private static boolean isChinese() {
        GameLanguage lang = Settings.language;
        return lang == GameLanguage.ZHS || lang == GameLanguage.ZHT;
    }

    private static String getAnimatedLoadingText() {
        String base = isChinese() ? "少女祈祷中" : "Waiting for advice";
        int dots = (int) ((System.currentTimeMillis() / (long) ELLIPSIS_CYCLE_MS) % 4);
        StringBuilder sb = new StringBuilder(base);
        for (int i = 0; i < dots; i++) {
            sb.append('.');
        }
        return sb.toString();
    }

    private static float getPulseAlpha() {
        long now = System.currentTimeMillis();
        double phase = (now % (long) PULSE_CYCLE_MS) / PULSE_CYCLE_MS * Math.PI * 2;
        float raw = (float) ((Math.sin(phase) + 1.0) / 2.0);
        return PULSE_MIN + (1.0f - PULSE_MIN) * raw;
    }

    private static String getLabelRecommendation() {
        return isChinese() ? "推荐" : "Recommendation";
    }

    private static String getLabelReason() {
        return isChinese() ? "理由" : "Reason";
    }

    private static String getLabelRisk() {
        return isChinese() ? "风险" : "Risk";
    }

    private static String getLabelComment() {
        return isChinese() ? "吐槽" : "Comment";
    }
}

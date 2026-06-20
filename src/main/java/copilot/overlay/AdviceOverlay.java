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

    private final OverlayConfig config;
    private volatile AdviceReader.AdviceData currentAdvice = new AdviceReader.AdviceData();
    private ShapeRenderer shapeRenderer;

    public AdviceOverlay(OverlayConfig config) {
        this.config = config;
    }

    public void setAdvice(AdviceReader.AdviceData advice) {
        this.currentAdvice = advice;
    }

    public void render(SpriteBatch sb) {
        if (!config.visible) return;

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
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(x, boxBottom, BOX_WIDTH, height);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER_COLOR);
        shapeRenderer.rect(x, boxBottom, BOX_WIDTH, height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        sb.begin();

        BitmapFont bodyFont = FontHelper.tipBodyFont;
        float lineSpacing = bodyFont.getLineHeight();
        float contentMaxWidth = BOX_WIDTH - BOX_PADDING * 2 - CONTENT_INDENT;

        float textX = x + BOX_PADDING;
        float textY = boxTop - BOX_PADDING - lineSpacing;

        if (currentAdvice == null || currentAdvice.rawText.isEmpty()) {
            FontHelper.renderFontLeftTopAligned(sb, bodyFont,
                    getWaitingText(), textX, textY, Color.GRAY);
            return;
        }

        textY = drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                getLabelRecommendation(), currentAdvice.recommendation, textX, textY);
        textY = drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                getLabelReason(), currentAdvice.reason, textX, textY);
        textY = drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                getLabelRisk(), currentAdvice.risk, textX, textY);
        if (currentAdvice.comment != null && !currentAdvice.comment.isEmpty()) {
            drawField(sb, bodyFont, lineSpacing, contentMaxWidth,
                    getLabelComment(), currentAdvice.comment, textX, textY);
        }
    }

    private float measureHeight() {
        BitmapFont bodyFont = FontHelper.tipBodyFont;
        float lineSpacing = bodyFont.getLineHeight();
        float contentMaxWidth = BOX_WIDTH - BOX_PADDING * 2 - CONTENT_INDENT;

        float h = BOX_PADDING * 2 + lineSpacing;
        AdviceReader.AdviceData adv = currentAdvice;
        if (adv == null || adv.rawText.isEmpty()) return BOX_PADDING * 2 + lineSpacing;
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
                            String label, String value, float x, float y) {
        if (value == null || value.isEmpty()) return y;
        FontHelper.renderFontLeftTopAligned(sb, font, label + ":", x, y, LABEL_COLOR);
        y -= lineSpacing;
        FontHelper.renderSmartText(sb, font, value, x + CONTENT_INDENT, y, maxWidth, lineSpacing, TEXT_COLOR);
        float contentHeight = lineSpacing
                - FontHelper.getSmartHeight(font, value, maxWidth, lineSpacing);
        y -= contentHeight + FIELD_GAP;
        return y;
    }

    private static boolean isChinese() {
        GameLanguage lang = Settings.language;
        return lang == GameLanguage.ZHS || lang == GameLanguage.ZHT;
    }

    private static String getWaitingText() {
        return isChinese() ? "等待建议..." : "Waiting for advice...";
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

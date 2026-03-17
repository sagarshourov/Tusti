package com.sagarroy.tusti;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.*;

public class GameView extends View {

    // ── Density-independent sizing (set in constructor) ────────────
    private float dp;                        // 1dp in pixels
    private int   TUBE_W, TUBE_H, CELL_H;
    private int   CORNER_R, TUBE_GAP, TOP_PAD, ROW_GAP, LIFT_AMT;

    private long  lastTapTime  = 0;
    private int   lastTapIndex = -1;

    // ── Deep premium palette ───────────────────────────────────────
    // Background gradient stops
    private static final int BG_TOP    = 0xFF0A0E1A;
    private static final int BG_BOT    = 0xFF0D1F35;
    // Card / panel
    private static final int CARD_BG   = 0xFF111E35;
    private static final int CARD_BDR  = 0xFF1E3560;
    // Tube glass
    private static final int GLASS_BDR = 0xCCE8F0FF;  // frosted rim
    private static final int GLASS_HI  = 0x44FFFFFF;  // inner highlight strip
    private static final int GLASS_SHD = 0xFF080D1A;  // inner shadow strip
    // Selected glow
    private static final int SEL_GLOW  = 0xFFFFD700;
    // Text
    private static final int TXT_HI    = 0xFFFFFFFF;
    private static final int TXT_MID   = 0xFFB8C8E0;
    private static final int TXT_DIM   = 0xFF6B85A8;
    // Buttons
    private static final int BTN_PRI   = 0xFF3A7BD5;   // primary blue
    private static final int BTN_SUC   = 0xFF27AE60;   // success green
    private static final int BTN_WARN  = 0xFFE67E22;   // warning orange
    private static final int BTN_DARK  = 0xFF1A2A40;   // muted dark

    // ── Rich water colours (more vivid, hand-tuned) ───────────────
    // These override GameState.WATER_COLORS for rendering only
    private static final int[] RENDER_COLORS = {
            0xFFFF3B30,  // vivid red
            0xFF007AFF,  // iOS blue
            0xFFFFCC00,  // gold yellow
            0xFF34C759,  // fresh green
            0xFFAF52DE,  // purple
            0xFF5AC8FA,  // sky cyan
            0xFFFF9500,  // warm orange
            0xFFFF2D55,  // hot pink
            0xFF8B4513,  // rich brown
            0xFF00C7BE,  // teal
    };
    // Lighter tint for top-of-layer shine
    private static final int[] RENDER_TINTS = {
            0xFFFF6B61, 0xFF4D9FFF, 0xFFFFD84D, 0xFF6ED98B,
            0xFFCC88F0, 0xFF8CDBFF, 0xFFFFB84D, 0xFFFF6D87,
            0xFFA0693D, 0xFF4DDDD8,
    };

    // ── Paints ────────────────────────────────────────────────────
    private final Paint bgPaint    = new Paint();
    private final Paint fillP      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokeP    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textP      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint btnP       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint btnTxtP    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowP      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowP    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayP   = new Paint();
    private final Paint particleP  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Radial glow for selected tube
    private RadialGradient selGrad;
    private int selGradTubeIdx = -2;

    // ── Screen & Game ─────────────────────────────────────────────
    private static final int S_MENU = 0, S_GAME = 1, S_WON = 2, S_RULES = 3;
    private int screen = S_MENU;
    private final GameState state = new GameState();
    private int selected = -1;
    private boolean animating = false;

    // ── Tube layout ───────────────────────────────────────────────
    private RectF[] tubeRects;
    private float[] tubeCx, tubeCy;

    // ── Pour animation ────────────────────────────────────────────
    private int   pFrom = -1, pTo = -1, pColor = 0, pCells = 0;
    private int   pColorIdx = 0;
    private float animX, animY, animAngle, pourProgress;
    private AnimatorSet masterAnim;

    // ── Particles (pour drips) ────────────────────────────────────
    private static final int MAX_PARTICLES = 20;
    private final float[] prtX   = new float[MAX_PARTICLES];
    private final float[] prtY   = new float[MAX_PARTICLES];
    private final float[] prtVx  = new float[MAX_PARTICLES];
    private final float[] prtVy  = new float[MAX_PARTICLES];
    private final float[] prtR   = new float[MAX_PARTICLES];
    private final float[] prtA   = new float[MAX_PARTICLES];   // alpha 0..1
    private int prtCount = 0;
    private ValueAnimator particleAnim;

    // ── Win / Confetti ────────────────────────────────────────────
    private float winScale = 0f;
    private float confT    = 0f;
    private ValueAnimator winAnim, confAnim;
    private static final int CONF_N = 40;
    private final float[] cfX = new float[CONF_N], cfY = new float[CONF_N];
    private final float[] cfVx = new float[CONF_N], cfVy = new float[CONF_N];
    private final float[] cfRot = new float[CONF_N], cfW = new float[CONF_N], cfH = new float[CONF_N];
    private final int[]   cfC  = new int[CONF_N];

    // ── Button rects ──────────────────────────────────────────────
    private RectF btnRestart, btnHome, btnNext, btnMenuFromGame;
    private RectF[] menuLevelBtns;
    private RectF btnRulesMenu, btnBackRules;

    // ── Menu scroll ───────────────────────────────────────────────
    private float menuScrollY, menuTouchY0, menuScrollY0;

    // ── Background gradient ───────────────────────────────────────
    private LinearGradient bgGrad;
    private int bgGradH = 0;

    // ═════════════════════════════════════════════════════════════
    public GameView(Context ctx) {
        super(ctx);
        dp = ctx.getResources().getDisplayMetrics().density;
        TUBE_W   = dp(52); TUBE_H   = dp(180);
        CELL_H   = dp(38); CORNER_R = dp(24);
        TUBE_GAP = dp(14); TOP_PAD  = dp(130);
        ROW_GAP  = dp(50); LIFT_AMT = dp(88);
        setupPaints();
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        initConfetti();
    }

    private int dp(float v) { return Math.round(v * dp); }

    // ── Paint setup ───────────────────────────────────────────────
    private void setupPaints() {
        bgPaint.setStyle(Paint.Style.FILL);
        fillP.setStyle(Paint.Style.FILL);
        strokeP.setStyle(Paint.Style.STROKE);
        strokeP.setStrokeWidth(dp * 2.2f);
        strokeP.setStrokeCap(Paint.Cap.ROUND);

        textP.setTextAlign(Paint.Align.CENTER);
        textP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textP.setColor(TXT_HI);

        btnP.setStyle(Paint.Style.FILL);

        btnTxtP.setTextAlign(Paint.Align.CENTER);
        btnTxtP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btnTxtP.setColor(TXT_HI);

        glowP.setStyle(Paint.Style.FILL);
        glowP.setMaskFilter(new BlurMaskFilter(dp * 12, BlurMaskFilter.Blur.NORMAL));

        shadowP.setStyle(Paint.Style.FILL);
        shadowP.setMaskFilter(new BlurMaskFilter(dp * 8, BlurMaskFilter.Blur.NORMAL));

        particleP.setStyle(Paint.Style.FILL);
    }

    // ── Confetti init ─────────────────────────────────────────────
    private void initConfetti() {
        int[] cols = {0xFFFF3B30,0xFF007AFF,0xFFFFCC00,0xFF34C759,
                0xFFAF52DE,0xFF5AC8FA,0xFFFF9500,0xFFFF2D55};
        for (int i = 0; i < CONF_N; i++) {
            cfX[i]   = (float) Math.random();
            cfY[i]   = -(float)(Math.random() * 1.2f);
            cfVx[i]  = (float)(Math.random() - 0.5) * 0.003f;
            cfVy[i]  = 0.005f + (float)(Math.random() * 0.008f);
            cfRot[i] = (float)(Math.random() * 360);
            cfW[i]   = dp(6) + dp((float)(Math.random() * 6));
            cfH[i]   = dp(4) + dp((float)(Math.random() * 4));
            cfC[i]   = cols[i % cols.length];
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  onSizeChanged
    // ═════════════════════════════════════════════════════════════
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        bgGrad  = new LinearGradient(0, 0, 0, h, BG_TOP, BG_BOT, Shader.TileMode.CLAMP);
        bgGradH = h;
        buildMenuButtons(w, h);
    }

    private void buildMenuButtons(int w, int h) {
        float bw = w * 0.84f, bh = dp(68), lx = (w - bw) / 2f;
        float startY = h * 0.20f, gap = bh + dp(14);
        menuLevelBtns = new RectF[GameState.TOTAL_LEVELS];
        for (int i = 0; i < GameState.TOTAL_LEVELS; i++) {
            float top = startY + i * gap;
            menuLevelBtns[i] = new RectF(lx, top, lx + bw, top + bh);
        }
        float ry = startY + GameState.TOTAL_LEVELS * gap + dp(12);
        btnRulesMenu = new RectF(lx, ry, lx + bw, ry + bh);
    }

    private void buildTubeLayout() {
        int W = getWidth(), H = getHeight(), n = state.getTubeCount();
        tubeRects = new RectF[n]; tubeCx = new float[n]; tubeCy = new float[n];
        int perRow = (n <= 5) ? n : (n <= 8) ? 4 : 5;
        int rows = (int) Math.ceil((double) n / perRow);
        for (int i = 0; i < n; i++) {
            int row = i / perRow, col = i % perRow;
            int rc = (row == rows-1 && n % perRow != 0) ? n % perRow : perRow;
            float rowW = rc * (TUBE_W + TUBE_GAP) - TUBE_GAP;
            float x = (W - rowW) / 2f + col * (TUBE_W + TUBE_GAP);
            float y = TOP_PAD + row * (TUBE_H + ROW_GAP);
            tubeRects[i] = new RectF(x, y, x + TUBE_W, y + TUBE_H);
            tubeCx[i] = x + TUBE_W / 2f; tubeCy[i] = y;
        }
        float by = H - dp(74), bw2 = dp(100), bh2 = dp(46);
        btnRestart     = new RectF(dp(10), by, dp(10)+bw2, by+bh2);
        btnMenuFromGame= new RectF((W-bw2)/2f, by, (W+bw2)/2f, by+bh2);
        btnHome        = new RectF(W-dp(10)-bw2, by, W-dp(10), by+bh2);
    }

    // ═════════════════════════════════════════════════════════════
    //  onDraw
    // ═════════════════════════════════════════════════════════════
    @Override
    protected void onDraw(Canvas canvas) {
        // Background gradient
        bgPaint.setShader(bgGrad);
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        int w = getWidth(), h = getHeight();
        switch (screen) {
            case S_MENU:  drawMenu(canvas, w, h);  break;
            case S_GAME:  drawGame(canvas, w, h);  break;
            case S_WON:   drawWon(canvas, w, h);   break;
            case S_RULES: drawRules(canvas, w, h); break;
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  MENU
    // ═════════════════════════════════════════════════════════════
    private void drawMenu(Canvas canvas, int w, int h) {
        // Top logo card
        fillP.setColor(CARD_BG);
        RectF topCard = new RectF(0, 0, w, h * 0.175f);
        canvas.drawRect(topCard, fillP);
        // Accent line
        fillP.setColor(BTN_PRI);
        canvas.drawRect(0, topCard.bottom - dp(2), w, topCard.bottom, fillP);

        // Title
        textP.setTextSize(dp(30)); textP.setColor(TXT_HI);
        textP.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        canvas.drawText("TUSTI ROY", w / 2f, h * 0.085f, textP);
        textP.setTextSize(dp(13)); textP.setColor(TXT_DIM);
        textP.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        canvas.drawText("SELECT A LEVEL TO BEGIN", w / 2f, h * 0.145f, textP);

        // Level list
        canvas.save();
        canvas.clipRect(0, h * 0.18f, w, h);
        canvas.translate(0, menuScrollY);

        for (int i = 0; i < GameState.TOTAL_LEVELS; i++) {
            RectF r = menuLevelBtns[i];
            int lvl = i + 1;
            drawMenuLevelCard(canvas, r, lvl, w);
        }

        // How to Play button
        drawButton(canvas, btnRulesMenu, BTN_DARK, "HOW TO PLAY", dp(15));
        // Bottom border on rules button
        strokeP.setColor(CARD_BDR); strokeP.setStrokeWidth(dp(1));
        canvas.drawRoundRect(btnRulesMenu, dp(14), dp(14), strokeP);

        canvas.restore();
    }

    private void drawMenuLevelCard(Canvas canvas, RectF r, int lvl, int w) {
        int diffColor = GameState.difficultyColor(lvl);

        // Card shadow
        shadowP.setColor(0x33000000);
        canvas.drawRoundRect(r.left + dp(2), r.top + dp(4),
                r.right + dp(2), r.bottom + dp(4), dp(14), dp(14), shadowP);

        // Card background
        fillP.setColor(CARD_BG);
        canvas.drawRoundRect(r, dp(14), dp(14), fillP);

        // Left accent bar
        fillP.setColor(diffColor);
        canvas.drawRoundRect(r.left, r.top, r.left + dp(6), r.bottom, dp(3), dp(3), fillP);

        // Subtle border
        strokeP.setColor(CARD_BDR); strokeP.setStrokeWidth(dp(1));
        canvas.drawRoundRect(r, dp(14), dp(14), strokeP);

        // Level number circle
        float cx = r.left + dp(38), cy = r.centerY(), cr = dp(20);
        fillP.setColor(diffColor & 0x30FFFFFF | diffColor & 0xFF000000);
        // lighter tint circle
        fillP.setColor((diffColor & 0x00FFFFFF) | 0x22000000);
        fillP.setColor(0x22FFFFFF);
        Paint tmp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tmp.setStyle(Paint.Style.FILL);
        tmp.setColor(diffColor);
        canvas.drawCircle(cx, cy, cr, tmp);
        textP.setTextSize(dp(15)); textP.setColor(TXT_HI);
        textP.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        canvas.drawText(String.valueOf(lvl), cx, cy - (textP.descent() + textP.ascent()) / 2f, textP);

        // Level title
        textP.setTextSize(dp(16)); textP.setColor(TXT_HI);
        textP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textP.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Level " + lvl, r.left + dp(68), r.centerY() - dp(6), textP);

        // Difficulty label
        textP.setTextSize(dp(11)); textP.setColor(diffColor);
        textP.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        canvas.drawText(GameState.difficultyLabel(lvl), r.left + dp(68), r.centerY() + dp(10), textP);

        // Mini colour dots preview
        int numColors = lvl <= 5 ? 3 : lvl <= 10 ? 4 : lvl <= 15 ? 5 : lvl <= 20 ? 6 : 8;
        float dotR = dp(5), dotX = r.right - dp(14);
        for (int k = 0; k < Math.min(numColors, 6); k++) {
            tmp.setColor(RENDER_COLORS[(numColors - 1 - k) % RENDER_COLORS.length]);
            canvas.drawCircle(dotX - k * (dotR * 2.2f), r.centerY(), dotR, tmp);
        }

        textP.setTextAlign(Paint.Align.CENTER);
    }

    // ═════════════════════════════════════════════════════════════
    //  GAME
    // ═════════════════════════════════════════════════════════════
    private void drawGame(Canvas canvas, int w, int h) {
        if (tubeRects == null) buildTubeLayout();

        drawGameHeader(canvas, w);

        int n = state.getTubeCount();
        int[][] tubes = state.getTubes();

        // Draw all static tubes
        for (int i = 0; i < n; i++) {
            if (animating && i == pFrom) continue;
            boolean sel = (i == selected);
            float dy = sel ? -dp(18) : 0;

            // Draw selected glow
            if (sel) drawTubeGlow(canvas, i);

            drawTubeAt(canvas, tubeRects[i].left, tubeRects[i].top + dy,
                    tubes[i], sel, 0f,
                    (animating && i == pTo) ? pourProgress : -1f, i);
        }

        // Animated tube
        if (animating && pFrom >= 0) {
            drawAnimTube(canvas, tubes[pFrom]);
            drawParticles(canvas);
        }

        drawGameFooter(canvas, w, h);
    }

    private void drawGameHeader(Canvas canvas, int w) {
        // Header card
        fillP.setColor(CARD_BG);
        canvas.drawRect(0, 0, w, TOP_PAD - dp(8), fillP);
        fillP.setColor(BTN_PRI);
        canvas.drawRect(0, TOP_PAD - dp(10), w, TOP_PAD - dp(8), fillP);

        int lvl = state.currentLevel;
        // Difficulty pill
        int diffC = GameState.difficultyColor(lvl);
        RectF pill = new RectF(w/2f - dp(50), dp(12), w/2f + dp(50), dp(30));
        fillP.setColor(diffC);
        canvas.drawRoundRect(pill, dp(9), dp(9), fillP);
        textP.setTextSize(dp(10)); textP.setColor(TXT_HI);
        textP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        canvas.drawText(GameState.difficultyLabel(lvl), w/2f, dp(24), textP);

        // Level number
        textP.setTextSize(dp(26));
        textP.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        textP.setColor(TXT_HI);
        canvas.drawText("Level " + lvl, w/2f, dp(68), textP);

        // Moves counter
        textP.setTextSize(dp(12));
        textP.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textP.setColor(TXT_DIM);
        canvas.drawText("MOVES  " + state.moves, w/2f, dp(92), textP);
    }

    private void drawTubeGlow(Canvas canvas, int idx) {
        float cx = tubeCx[idx];
        float cy = tubeRects[idx].centerY() - dp(18);
        float r  = TUBE_W * 1.4f;
        glowP.setShader(new RadialGradient(cx, cy, r,
                new int[]{0x55FFD700, 0x00FFD700}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, glowP);
        glowP.setShader(null);
    }

    /**
     * Draws a tube with glassy look:
     *  - Drop shadow
     *  - Water layers with colour gradient
     *  - Glass overlay: left highlight strip + right shadow strip + top rim
     *  - Border stroke
     */
    private void drawTubeAt(Canvas canvas, float left, float top,
                            int[] cells, boolean sel, float rotDeg,
                            float destFill, int tubeIdx) {
        float right = left + TUBE_W, bottom = top + TUBE_H;
        float cx2 = left + TUBE_W / 2f;

        canvas.save();
        if (rotDeg != 0f) canvas.rotate(rotDeg, cx2, bottom);

        // Drop shadow
        shadowP.setColor(sel ? 0x66FFD700 : 0x55000000);
        shadowP.setMaskFilter(new BlurMaskFilter(dp * (sel ? 14 : 8), BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(left + dp(4), top + dp(8), right + dp(4), bottom + dp(6),
                CORNER_R, CORNER_R, shadowP);

        // Clip to tube shape
        Path clip = new Path();
        clip.addRoundRect(left, top, right, bottom, CORNER_R, CORNER_R, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        // Tube background (glass dark interior)
        fillP.setColor(0xFF08111E);
        canvas.drawRect(left, top, right, bottom, fillP);

        // Draw water layers bottom→top
        for (int j = 0; j < GameState.TUBE_CAPACITY; j++) {
            if (cells[j] == 0) continue;
            int cIdx = colorIndex(cells[j]);
            float ct = bottom - (j + 1) * CELL_H - dp(4);
            float cb = bottom - j * CELL_H - dp(4);

            // Base water fill
            fillP.setColor(RENDER_COLORS[cIdx]);
            canvas.drawRect(left, ct, right, cb, fillP);

            // Top layer: glossy highlight gradient
            if (j == topIndexOf(cells)) {
                LinearGradient wg = new LinearGradient(left, ct, right, ct,
                        new int[]{0x55FFFFFF, RENDER_TINTS[cIdx], RENDER_COLORS[cIdx]},
                        new float[]{0f, 0.3f, 1f}, Shader.TileMode.CLAMP);
                fillP.setShader(wg);
                canvas.drawRect(left, ct, right, ct + dp(6), fillP);
                fillP.setShader(null);
            }
        }

        // Destination fill during pour
        if (destFill >= 0f && tubeIdx == pTo) {
            int freeSlots = state.freeSpace(pTo);
            float riseH   = destFill * pCells * CELL_H;
            float fb = bottom - (GameState.TUBE_CAPACITY - freeSlots) * CELL_H - dp(4);
            int cIdx = colorIndex(pColor);
            fillP.setColor(RENDER_COLORS[cIdx]);
            canvas.drawRect(left, fb - riseH, right, fb, fillP);
            // Ripple at top of rising water
            fillP.setColor(0x66FFFFFF);
            canvas.drawRect(left, fb - riseH, right, fb - riseH + dp(3), fillP);
        }

        // Glass left highlight strip
        LinearGradient hiGrad = new LinearGradient(left, 0, left + TUBE_W * 0.4f, 0,
                new int[]{0x33FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
        fillP.setShader(hiGrad);
        canvas.drawRect(left, top, left + TUBE_W * 0.4f, bottom, fillP);
        fillP.setShader(null);

        // Glass right shadow strip
        LinearGradient shGrad = new LinearGradient(right - TUBE_W * 0.25f, 0, right, 0,
                new int[]{0x00000000, 0x22000000}, null, Shader.TileMode.CLAMP);
        fillP.setShader(shGrad);
        canvas.drawRect(right - TUBE_W * 0.25f, top, right, bottom, fillP);
        fillP.setShader(null);

        canvas.restore(); // end clip

        // Glass border
        strokeP.setColor(sel ? SEL_GLOW : GLASS_BDR);
        strokeP.setStrokeWidth(sel ? dp(3) : dp(2));
        canvas.drawRoundRect(left, top, right, bottom, CORNER_R, CORNER_R, strokeP);

        // Inner rim highlight at very top
        fillP.setColor(0x55FFFFFF);
        canvas.drawRoundRect(left + dp(4), top + dp(4), right - dp(4), top + dp(12),
                dp(8), dp(8), fillP);

        canvas.restore();
    }

    private void drawAnimTube(Canvas canvas, int[] cells) {
        float left = animX - TUBE_W / 2f;
        drawTubeAt(canvas, left, animY, cells, false, animAngle, -1f, -1);

        // Stream when pouring
        if (Math.abs(animAngle) >= 70f && pourProgress > 0f) {
            float pivY = animY + TUBE_H;
            double rad = Math.toRadians(animAngle);
            float mouthX = animX + (float)(-Math.sin(rad) * TUBE_H);
            float mouthY = pivY  + (float)(-Math.cos(rad) * TUBE_H);
            float destCx = tubeCx[pTo];
            float destTy = tubeRects[pTo].top + dp(10); // water flow

            int cIdx = colorIndex(pColor);
            int baseCol = RENDER_COLORS[cIdx];

            // Stream arc
            int steps = 16;
            for (int k = 0; k <= steps; k++) {
                float t  = k / (float) steps;
                float sx = mouthX + (destCx - mouthX) * t;
                float sy = mouthY + (destTy  - mouthY) * t
                        + (float)(Math.sin(Math.PI * t) * -dp(20));
                float r  = dp(5) * (1f - t * 0.45f) * pourProgress;
                int alpha = (int)(255 * (1f - t * 0.3f) * pourProgress);
                particleP.setColor((baseCol & 0x00FFFFFF) | (alpha << 24));
                canvas.drawCircle(sx, sy, r, particleP);
            }

            // Spawn particles each frame
            spawnParticles(destCx, destTy, baseCol);
        }
    }

    // ── Particle system ───────────────────────────────────────────
    private void spawnParticles(float x, float y, int color) {
        if (prtCount >= MAX_PARTICLES) return;
        for (int k = 0; k < 2 && prtCount < MAX_PARTICLES; k++) {
            prtX[prtCount]  = x + (float)(Math.random() - 0.5) * dp(20);
            prtY[prtCount]  = y;
            prtVx[prtCount] = (float)(Math.random() - 0.5) * dp(3);
            prtVy[prtCount] = -(float)(Math.random() * dp(4));
            prtR[prtCount]  = dp(3) + (float)(Math.random() * dp(3));
            prtA[prtCount]  = 1f;
            prtCount++;
        }
    }

    private void drawParticles(Canvas canvas) {
        for (int i = 0; i < prtCount; i++) {
            prtVy[i] += dp(0.4f);   // gravity
            prtX[i]  += prtVx[i];
            prtY[i]  += prtVy[i];
            prtA[i]  -= 0.04f;
            int col = pColor;
            int alpha = Math.max(0, (int)(prtA[i] * 255));
            particleP.setColor((col & 0x00FFFFFF) | (alpha << 24));
            canvas.drawCircle(prtX[i], prtY[i], prtR[i] * prtA[i], particleP);
        }
        // Compact dead particles
        int alive = 0;
        for (int i = 0; i < prtCount; i++) {
            if (prtA[i] > 0.05f) {
                prtX[alive] = prtX[i]; prtY[alive] = prtY[i];
                prtVx[alive]= prtVx[i]; prtVy[alive]= prtVy[i];
                prtR[alive] = prtR[i]; prtA[alive]  = prtA[i];
                alive++;
            }
        }
        prtCount = alive;
    }

    private void drawGameFooter(Canvas canvas, int w, int h) {
        if (btnRestart == null) return;
        // Frosted bottom bar
        fillP.setColor(0xCC080D1A);
        canvas.drawRect(0, h - dp(86), w, h, fillP);
        fillP.setColor(CARD_BDR);
        canvas.drawRect(0, h - dp(86), w, h - dp(84), fillP);

        drawButton(canvas, btnRestart,      BTN_SUC,  "RESTART", dp(12));
        drawButton(canvas, btnMenuFromGame, BTN_DARK, "MENU",    dp(12));
        drawButton(canvas, btnHome,         BTN_PRI,  "HOME",    dp(12));
    }

    // ═════════════════════════════════════════════════════════════
    //  WIN SCREEN
    // ═════════════════════════════════════════════════════════════
    private void drawWon(Canvas canvas, int w, int h) {
        // Dimmed game board
        if (tubeRects != null) {
            int[][] tubes = state.getTubes();
            for (int i = 0; i < state.getTubeCount(); i++)
                drawTubeAt(canvas, tubeRects[i].left, tubeRects[i].top, tubes[i], false, 0, -1, -1);
        }
        overlayP.setColor(0xCC050A14);
        canvas.drawRect(0, 0, w, h, overlayP);

        // Confetti rectangles
        for (int i = 0; i < CONF_N; i++) {
            float fx = (cfX[i] + cfVx[i] * confT) * w;
            float fy = (cfY[i] + cfVy[i] * confT) * h;
            fy = ((fy % h) + h) % h;
            canvas.save();
            canvas.rotate(cfRot[i] + confT * 2, fx, fy);
            fillP.setColor(cfC[i]);
            canvas.drawRect(fx - cfW[i]/2, fy - cfH[i]/2, fx + cfW[i]/2, fy + cfH[i]/2, fillP);
            canvas.restore();
        }

        // Main card
        float cw = w * 0.86f, ch = h * 0.46f;
        float cl = (w - cw) / 2f, ct = (h - ch) * 0.44f;

        // Card shadow
        shadowP.setColor(0x88000000);
        shadowP.setMaskFilter(new BlurMaskFilter(dp(20), BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(cl, ct + dp(10), cl+cw, ct+ch+dp(10), dp(24), dp(24), shadowP);

        fillP.setColor(CARD_BG);
        canvas.drawRoundRect(cl, ct, cl+cw, ct+ch, dp(24), dp(24), fillP);

        // Top gradient bar
        int diffC = GameState.difficultyColor(state.currentLevel);
        LinearGradient topBar = new LinearGradient(cl, ct, cl+cw, ct,
                diffC, adjustAlpha(diffC, 0.6f), Shader.TileMode.CLAMP);
        fillP.setShader(topBar);
        canvas.drawRoundRect(cl, ct, cl+cw, ct+dp(6), dp(4), dp(4), fillP);
        fillP.setShader(null);

        // Subtle card border
        strokeP.setColor(CARD_BDR); strokeP.setStrokeWidth(dp(1));
        canvas.drawRoundRect(cl, ct, cl+cw, ct+ch, dp(24), dp(24), strokeP);

        // Trophy emoji substitute — gold star circles
        float starCx = w / 2f, starCy = ct + ch * 0.22f;
        for (int k = -1; k <= 1; k++) {
            float s = (k == 0) ? dp(24) : dp(17);
            fillP.setColor(k == 0 ? 0xFFFFCC00 : 0xFFFFAA00);
            canvas.drawCircle(starCx + k * dp(34), starCy + (k == 0 ? 0 : dp(6)), s, fillP);
            fillP.setColor(0x44FFFFFF);
            canvas.drawCircle(starCx + k * dp(34) - s*0.3f,
                    starCy + (k==0?0:dp(6)) - s*0.3f, s*0.25f, fillP);
        }

        // YOU WIN
        textP.setTextSize(dp(34) * winScale);
        textP.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        textP.setColor(TXT_HI);
        canvas.drawText("YOU WIN! \uD83C\uDF89", w / 2f, ct + ch * 0.50f, textP);

        // Sub text
        textP.setTextSize(dp(13));
        textP.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textP.setColor(TXT_MID);
        canvas.drawText("Level " + state.currentLevel + "  ·  " + state.moves + " moves", w/2f, ct+ch*0.62f, textP);

        // Buttons
        float bw3 = cw * 0.27f, bh3 = dp(46);
        float gap3 = (cw - 3*bw3) / 4f;
        float by3  = ct + ch - dp(62);
        btnRestart = new RectF(cl+gap3,             by3, cl+gap3+bw3,             by3+bh3);
        btnNext    = new RectF(cl+2*gap3+bw3,       by3, cl+2*gap3+2*bw3,         by3+bh3);
        btnHome    = new RectF(cl+3*gap3+2*bw3,     by3, cl+3*gap3+3*bw3,         by3+bh3);

        drawButton(canvas, btnRestart, BTN_DARK, "RETRY",  dp(12));
        drawButton(canvas, btnNext,    BTN_WARN, "NEXT \u25B6", dp(12));
        drawButton(canvas, btnHome,    BTN_PRI,  "MENU",   dp(12));
    }

    // ═════════════════════════════════════════════════════════════
    //  RULES
    // ═════════════════════════════════════════════════════════════
    private void drawRules(Canvas canvas, int w, int h) {
        float m = dp(20);
        // Card
        shadowP.setColor(0x66000000);
        shadowP.setMaskFilter(new BlurMaskFilter(dp(16), BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(m, m, w-m, h-m, dp(20), dp(20), shadowP);
        fillP.setColor(CARD_BG);
        canvas.drawRoundRect(m, m, w-m, h-m, dp(20), dp(20), fillP);
        fillP.setColor(BTN_PRI);
        canvas.drawRoundRect(m, m, w-m, m+dp(5), dp(3), dp(3), fillP);
        strokeP.setColor(CARD_BDR); strokeP.setStrokeWidth(dp(1));
        canvas.drawRoundRect(m, m, w-m, h-m, dp(20), dp(20), strokeP);

        textP.setTextSize(dp(22));
        textP.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        textP.setColor(TXT_HI);
        canvas.drawText("HOW TO PLAY", w/2f, m + dp(52), textP);

        String[][] rules = {
                {"\u2460", "Tap a tube to select it."},
                {"\u2461", "Tap another tube to pour."},
                {"\u2462", "Pour only if destination is empty or top color matches."},
                {"\u2463", "Fill each tube with one color to win."},
                {"\u2464", "Complete levels to unlock harder puzzles!"},
        };
        float ty = m + dp(90);
        for (String[] rule : rules) {
            // Icon circle
            fillP.setColor(BTN_PRI & 0x33FFFFFF | 0x33000000);
            fillP.setColor(0x22007AFF);
            canvas.drawCircle(m + dp(32), ty + dp(2), dp(14), fillP);
            textP.setTextSize(dp(14)); textP.setColor(BTN_PRI);
            textP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            canvas.drawText(rule[0], m + dp(32), ty + dp(2) - (textP.descent()+textP.ascent())/2f, textP);

            // Rule text (left-aligned)
            Paint rp = new Paint(Paint.ANTI_ALIAS_FLAG);
            rp.setColor(TXT_MID); rp.setTextSize(dp(14));
            rp.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            canvas.drawText(rule[1], m + dp(56), ty + dp(2) - (rp.descent()+rp.ascent())/2f, rp);

            ty += dp(44);
        }

        float bw2 = dp(140), bh2 = dp(48);
        btnBackRules = new RectF((w-bw2)/2f, h-m-bh2-dp(10), (w+bw2)/2f, h-m-dp(10));
        drawButton(canvas, btnBackRules, BTN_PRI, "\u2190  BACK", dp(14));
    }

    // ═════════════════════════════════════════════════════════════
    //  Button helper
    // ═════════════════════════════════════════════════════════════
    private void drawButton(Canvas canvas, RectF r, int color, String label, float radius) {
        if (r == null) return;
        // Shadow
        shadowP.setColor(0x44000000);
        shadowP.setMaskFilter(new BlurMaskFilter(dp(6), BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(r.left+dp(2), r.top+dp(3), r.right+dp(2), r.bottom+dp(3), radius, radius, shadowP);

        // Fill
        btnP.setColor(color);
        canvas.drawRoundRect(r, radius, radius, btnP);

        // Top highlight
        LinearGradient hi = new LinearGradient(r.left, r.top, r.left, r.top + r.height()*0.5f,
                0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        btnP.setShader(hi);
        canvas.drawRoundRect(r, radius, radius, btnP);
        btnP.setShader(null);

        // Text
        btnTxtP.setTextSize(dp(12));
        btnTxtP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        canvas.drawText(label, r.centerX(), r.centerY() - (btnTxtP.descent()+btnTxtP.ascent())/2f, btnTxtP);
    }

    // ═════════════════════════════════════════════════════════════
    //  Touch handling
    // ═════════════════════════════════════════════════════════════
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float tx = e.getX(), ty = e.getY();
        switch (screen) {
            case S_MENU:  handleMenuTouch(e, tx, ty); break;
            case S_GAME:  if (e.getAction()==MotionEvent.ACTION_UP) handleGameTouch(tx,ty); break;
            case S_WON:   if (e.getAction()==MotionEvent.ACTION_UP) handleWonTouch(tx,ty);  break;
            case S_RULES: if (e.getAction()==MotionEvent.ACTION_UP) handleRulesClick(tx,ty);break;
        }
        return true;
    }

    private void handleMenuTouch(MotionEvent e, float tx, float ty) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: menuTouchY0 = ty; menuScrollY0 = menuScrollY; break;
            case MotionEvent.ACTION_MOVE:
                menuScrollY = menuScrollY0 + (ty - menuTouchY0);
                menuScrollY = Math.min(menuScrollY, 0);
                invalidate(); break;
            case MotionEvent.ACTION_UP:
                float sty = ty - menuScrollY;
                for (int i = 0; i < GameState.TOTAL_LEVELS; i++) {
                    if (menuLevelBtns[i] != null && menuLevelBtns[i].contains(tx, sty)) {
                        long now = System.currentTimeMillis();
                        if (lastTapIndex == i && (now - lastTapTime) < 400) {
                            // Double tap — enter level
                            startLevel(i + 1);
                            lastTapIndex = -1;
                        } else {
                            // First tap — just record it
                            lastTapTime  = now;
                            lastTapIndex = i;
                            invalidate(); // optional: highlight the selected row
                        }
                        return;
                    }
                }
                if (btnRulesMenu != null && btnRulesMenu.contains(tx, sty)) { screen = S_RULES; invalidate(); }
                break;
        }
    }

    private void startLevel(int lvl) {
        state.initLevel(lvl); tubeRects = null; selected = -1;
        prtCount = 0; screen = S_GAME; menuScrollY = 0; invalidate();
    }

    private void handleGameTouch(float tx, float ty) {
        if (animating) return;
        if (btnRestart      != null && btnRestart.contains(tx,ty))      { doRestart(); return; }
        if (btnMenuFromGame != null && btnMenuFromGame.contains(tx,ty)) { goMenu();    return; }
        if (btnHome         != null && btnHome.contains(tx,ty))         { goMenu();    return; }
        int hit = hitTest(tx, ty);
        if (hit == -1) { selected = -1; invalidate(); return; }
        if (selected == -1) {
            if (state.topIndex(hit) != -1) { selected = hit; invalidate(); }
        } else {
            if (hit == selected) { selected = -1; invalidate(); return; }
            if (state.canTransfer(selected, hit)) startPourAnim(selected, hit);
            else { selected = (state.topIndex(hit)!=-1)?hit:-1; invalidate(); }
        }
    }

    private void handleWonTouch(float tx, float ty) {
        if (btnRestart!=null && btnRestart.contains(tx,ty)) { doRestart(); return; }
        if (btnNext!=null    && btnNext.contains(tx,ty))    { doNextLevel(); return; }
        if (btnHome!=null    && btnHome.contains(tx,ty))    { goMenu(); }
    }

    private void handleRulesClick(float tx, float ty) {
        if (btnBackRules!=null && btnBackRules.contains(tx,ty)) { screen = S_MENU; invalidate(); }
    }

    private int hitTest(float tx, float ty) {
        if (tubeRects==null) return -1;
        for (int i = 0; i < tubeRects.length; i++) {
            RectF r = tubeRects[i];
            if (tx>=r.left-dp(10) && tx<=r.right+dp(10) && ty>=r.top-dp(36) && ty<=r.bottom+dp(8))
                return i;
        }
        return -1;
    }

    // ═════════════════════════════════════════════════════════════
    //  5-Phase Pour Animation
    // ═════════════════════════════════════════════════════════════
    private void startPourAnim(int from, int to) {
        if (tubeRects==null) return;
        pFrom=from; pTo=to; pColor=state.topColor(from);
        pColorIdx = colorIndex(pColor);
        pCells=Math.min(state.topRun(from), state.freeSpace(to));
        selected=-1; animating=true; pourProgress=0f; prtCount=0;

        float startX=tubeCx[from], startY=tubeRects[from].top;
        float liftY=startY-LIFT_AMT;
        float hoverX=tubeCx[to]+dp(-140), hoverY=tubeRects[to].top-LIFT_AMT+dp(-160);
       // float tiltDir=(hoverX>=startX-dp(10))?1f:-1f, tiltAngle=tiltDir*108f;
        float tiltDir=1f, tiltAngle=tiltDir*108f;
        animX=startX; animY=startY; animAngle=0f;

        ValueAnimator p0=anim(300, new DecelerateInterpolator(), t->{
            animX=startX; animY=startY+(liftY-startY)*t; animAngle=0f; invalidate();});
        ValueAnimator p1=anim(360, new AccelerateDecelerateInterpolator(), t->{
            animX=startX+(hoverX-startX)*t; animY=liftY+(hoverY-liftY)*t; animAngle=0f; invalidate();});
        ValueAnimator p2=anim(200, new DecelerateInterpolator(), t->{
            animX=hoverX; animY=hoverY; animAngle=tiltAngle*t; invalidate();});
        ValueAnimator p3=anim(360, new LinearInterpolator(), t->{
            pourProgress=t; animX=hoverX; animY=hoverY; animAngle=tiltAngle; invalidate();});
        p3.addListener(new AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(Animator a){
                state.transfer(pFrom,pTo); pourProgress=0f; prtCount=0;}});
        ValueAnimator p4a=anim(150, new AccelerateInterpolator(), t->{
            animX=hoverX; animY=hoverY; animAngle=tiltAngle*(1f-t); invalidate();});
        ValueAnimator p4b=anim(260, new DecelerateInterpolator(), t->{
            animX=hoverX+(startX-hoverX)*t; animY=hoverY+(startY-hoverY)*t; animAngle=0f; invalidate();});
        p4b.addListener(new AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(Animator a){
                animating=false; pFrom=-1; pTo=-1;
                if(state.won){screen=S_WON; startWinAnim();}
                invalidate();}});

        masterAnim=new AnimatorSet();
        masterAnim.playSequentially(p0,p1,p2,p3,p4a,p4b);
        masterAnim.start();
    }

    private ValueAnimator anim(int ms, TimeInterpolator interp, FloatConsumer update) {
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(ms); va.setInterpolator(interp);
        va.addUpdateListener(a -> update.accept((float)a.getAnimatedValue()));
        return va;
    }

    @FunctionalInterface
    interface FloatConsumer { void accept(float v); }

    // ═════════════════════════════════════════════════════════════
    //  Win animation
    // ═════════════════════════════════════════════════════════════
    private void startWinAnim() {
        winScale=0f; confT=0f;
        winAnim=ValueAnimator.ofFloat(0f,1f); winAnim.setDuration(600);
        winAnim.setInterpolator(new OvershootInterpolator(2f));
        winAnim.addUpdateListener(a->{winScale=(float)a.getAnimatedValue(); invalidate();});
        winAnim.start();
        confAnim=ValueAnimator.ofFloat(0f,300f); confAnim.setDuration(5000);
        confAnim.setInterpolator(new LinearInterpolator());
        confAnim.addUpdateListener(a->{confT=(float)a.getAnimatedValue(); invalidate();});
        confAnim.start();
    }

    // ═════════════════════════════════════════════════════════════
    //  Actions
    // ═════════════════════════════════════════════════════════════
    private void doRestart() {
        cancelAnims(); state.restart();
        selected=-1; animating=false; pFrom=-1; pTo=-1;
        winScale=0f; pourProgress=0f; prtCount=0; tubeRects=null;
        screen=S_GAME; invalidate();
    }

    private void doNextLevel() {
        cancelAnims(); state.nextLevel();
        selected=-1; animating=false; pFrom=-1; pTo=-1;
        winScale=0f; pourProgress=0f; prtCount=0; tubeRects=null;
        screen=S_GAME; invalidate();
    }

    private void goMenu() {
        cancelAnims(); selected=-1; animating=false; pFrom=-1; pTo=-1;
        prtCount=0; screen=S_MENU; invalidate();
    }

    private void cancelAnims() {
        if(masterAnim!=null) masterAnim.cancel();
        if(winAnim!=null)    winAnim.cancel();
        if(confAnim!=null)   confAnim.cancel();
    }

    public void handleBackPress() {
        if(screen==S_GAME||screen==S_WON) goMenu();
        else if(screen==S_RULES){screen=S_MENU; invalidate();}
    }

    // ═════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════
    private int topIndexOf(int[] arr) {
        for(int j=GameState.TUBE_CAPACITY-1;j>=0;j--) if(arr[j]!=0) return j;
        return -1;
    }

    /** Map a GameState water color back to its index in WATER_COLORS */
    private int colorIndex(int color) {
        for(int i=0;i<GameState.WATER_COLORS.length;i++)
            if(GameState.WATER_COLORS[i]==color) return i;
        return 0;
    }

    private int adjustAlpha(int color, float factor) {
        int a = Math.round(Color.alpha(color) * factor);
        return (color & 0x00FFFFFF) | (a << 24);
    }
}

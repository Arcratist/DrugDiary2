package brettdansmith.drugdiary.ui.avatar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AvatarGlyphDrawable extends Drawable {
    @NonNull private final String iconId;
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AvatarGlyphDrawable(@NonNull String iconId) {
        this.iconId = iconId;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setColor(0xFFFFFFFF);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xFFFFFFFF);
    }

    public void setMonotoneColor(int color) {
        strokePaint.setColor(color);
        fillPaint.setColor(color);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float unit = Math.min(b.width(), b.height()) / 100f;
        float stroke = Math.max(unit * 6f, 2f);
        strokePaint.setStrokeWidth(stroke);

        switch (iconId) {
            case "male_head":
                drawMaleHead(canvas, b);
                break;
            case "female_head":
                drawFemaleHead(canvas, b);
                break;
            case "eye":
                drawEye(canvas, b);
                break;
            case "portal":
                drawPortal(canvas, b);
                break;
            case "spiral":
                drawSpiral(canvas, b);
                break;
            case "fingerprint":
                drawFingerprint(canvas, b);
                break;
            case "snake":
                drawSnake(canvas, b);
                break;
            case "butterfly":
                drawButterfly(canvas, b);
                break;
            case "owl":
                drawOwl(canvas, b);
                break;
            case "wolf":
                drawWolf(canvas, b);
                break;
            case "deer":
                drawDeer(canvas, b);
                break;
            case "raven":
                drawRaven(canvas, b);
                break;
            case "fox":
                drawFox(canvas, b);
                break;
            case "tree":
                drawTree(canvas, b);
                break;
            case "mushroom":
                drawMushroom(canvas, b);
                break;
            case "mountain":
                drawMountain(canvas, b);
                break;
            case "moon":
                drawMoon(canvas, b);
                break;
            case "sun":
                drawSun(canvas, b);
                break;
            case "star":
                drawStar(canvas, b);
                break;
            case "planet":
                drawPlanet(canvas, b);
                break;
            case "cloud":
                drawCloud(canvas, b);
                break;
            case "wave":
                drawWave(canvas, b);
                break;
            case "flame":
                drawFlame(canvas, b);
                break;
            case "crystal":
                drawCrystal(canvas, b);
                break;
            case "lotus":
                drawLotus(canvas, b);
                break;
            case "sprout":
                drawSprout(canvas, b);
                break;
            case "lantern":
                drawLantern(canvas, b);
                break;
            case "compass":
                drawCompass(canvas, b);
                break;
            case "anchor":
                drawAnchor(canvas, b);
                break;
            default:
                drawStar(canvas, b);
                break;
        }
    }

    private void drawMaleHead(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 33), r(b, 16), strokePaint);
        c.drawRoundRect(x(b, 28), y(b, 52), x(b, 72), y(b, 82), r(b, 10), r(b, 10), strokePaint);
    }

    private void drawFemaleHead(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 31), r(b, 14), strokePaint);
        Path hair = new Path();
        hair.moveTo(x(b, 30), y(b, 36));
        hair.quadTo(x(b, 50), y(b, 68), x(b, 70), y(b, 36));
        hair.moveTo(x(b, 38), y(b, 45));
        hair.lineTo(x(b, 36), y(b, 74));
        hair.moveTo(x(b, 62), y(b, 45));
        hair.lineTo(x(b, 64), y(b, 74));
        c.drawPath(hair, strokePaint);
        c.drawArc(x(b, 36), y(b, 54), x(b, 64), y(b, 82), 10, 160, false, strokePaint);
    }

    private void drawEye(Canvas c, Rect b) {
        Path eye = new Path();
        eye.moveTo(x(b, 16), y(b, 50));
        eye.quadTo(x(b, 50), y(b, 20), x(b, 84), y(b, 50));
        eye.quadTo(x(b, 50), y(b, 80), x(b, 16), y(b, 50));
        c.drawPath(eye, strokePaint);
        c.drawCircle(x(b, 50), y(b, 50), r(b, 10), fillPaint);
    }

    private void drawPortal(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 50), r(b, 32), strokePaint);
        c.drawCircle(x(b, 50), y(b, 50), r(b, 22), strokePaint);
        c.drawCircle(x(b, 50), y(b, 50), r(b, 12), strokePaint);
    }

    private void drawSpiral(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 50), y(b, 50));
        for (int i = 0; i < 18; i++) {
            float t = i / 18f;
            double ang = t * Math.PI * 5.5;
            float rad = r(b, 3 + (30 * t));
            float px = x(b, 50) + (float) (Math.cos(ang) * rad);
            float py = y(b, 50) + (float) (Math.sin(ang) * rad);
            p.lineTo(px, py);
        }
        c.drawPath(p, strokePaint);
    }

    private void drawFingerprint(Canvas c, Rect b) {
        c.drawArc(x(b, 24), y(b, 20), x(b, 76), y(b, 84), 210, 120, false, strokePaint);
        c.drawArc(x(b, 30), y(b, 28), x(b, 70), y(b, 82), 210, 150, false, strokePaint);
        c.drawArc(x(b, 36), y(b, 35), x(b, 64), y(b, 78), 215, 175, false, strokePaint);
        c.drawArc(x(b, 42), y(b, 43), x(b, 58), y(b, 72), 220, 190, false, strokePaint);
        c.drawLine(x(b, 40), y(b, 78), x(b, 34), y(b, 90), strokePaint);
        c.drawLine(x(b, 60), y(b, 78), x(b, 66), y(b, 90), strokePaint);
    }

    private void drawSnake(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 22), y(b, 68));
        p.cubicTo(x(b, 24), y(b, 24), x(b, 76), y(b, 84), x(b, 78), y(b, 34));
        c.drawPath(p, strokePaint);
        c.drawCircle(x(b, 77), y(b, 31), r(b, 6), fillPaint);
    }

    private void drawButterfly(Canvas c, Rect b) {
        c.drawOval(x(b, 20), y(b, 30), x(b, 45), y(b, 58), strokePaint);
        c.drawOval(x(b, 20), y(b, 44), x(b, 45), y(b, 74), strokePaint);
        c.drawOval(x(b, 55), y(b, 30), x(b, 80), y(b, 58), strokePaint);
        c.drawOval(x(b, 55), y(b, 44), x(b, 80), y(b, 74), strokePaint);
        c.drawLine(x(b, 50), y(b, 28), x(b, 50), y(b, 76), strokePaint);
    }

    private void drawOwl(Canvas c, Rect b) {
        c.drawRoundRect(x(b, 28), y(b, 26), x(b, 72), y(b, 80), r(b, 18), r(b, 18), strokePaint);
        c.drawCircle(x(b, 42), y(b, 48), r(b, 8), strokePaint);
        c.drawCircle(x(b, 58), y(b, 48), r(b, 8), strokePaint);
        c.drawCircle(x(b, 42), y(b, 48), r(b, 3), fillPaint);
        c.drawCircle(x(b, 58), y(b, 48), r(b, 3), fillPaint);
        Path beak = new Path();
        beak.moveTo(x(b, 50), y(b, 54));
        beak.lineTo(x(b, 46), y(b, 62));
        beak.lineTo(x(b, 54), y(b, 62));
        beak.close();
        c.drawPath(beak, fillPaint);
    }

    private void drawWolf(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 22), y(b, 72));
        p.lineTo(x(b, 30), y(b, 30));
        p.lineTo(x(b, 44), y(b, 45));
        p.lineTo(x(b, 50), y(b, 24));
        p.lineTo(x(b, 56), y(b, 45));
        p.lineTo(x(b, 70), y(b, 30));
        p.lineTo(x(b, 78), y(b, 72));
        p.close();
        c.drawPath(p, strokePaint);
        c.drawCircle(x(b, 42), y(b, 55), r(b, 2.5f), fillPaint);
        c.drawCircle(x(b, 58), y(b, 55), r(b, 2.5f), fillPaint);
    }

    private void drawDeer(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 56), r(b, 15), strokePaint);
        c.drawLine(x(b, 43), y(b, 42), x(b, 34), y(b, 26), strokePaint);
        c.drawLine(x(b, 34), y(b, 26), x(b, 24), y(b, 30), strokePaint);
        c.drawLine(x(b, 34), y(b, 26), x(b, 30), y(b, 18), strokePaint);
        c.drawLine(x(b, 57), y(b, 42), x(b, 66), y(b, 26), strokePaint);
        c.drawLine(x(b, 66), y(b, 26), x(b, 76), y(b, 30), strokePaint);
        c.drawLine(x(b, 66), y(b, 26), x(b, 70), y(b, 18), strokePaint);
    }

    private void drawRaven(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 18), y(b, 62));
        p.quadTo(x(b, 38), y(b, 32), x(b, 64), y(b, 40));
        p.lineTo(x(b, 84), y(b, 30));
        p.lineTo(x(b, 68), y(b, 50));
        p.quadTo(x(b, 44), y(b, 72), x(b, 18), y(b, 62));
        c.drawPath(p, strokePaint);
        c.drawCircle(x(b, 56), y(b, 46), r(b, 2), fillPaint);
    }

    private void drawFox(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 50), y(b, 22));
        p.lineTo(x(b, 28), y(b, 42));
        p.lineTo(x(b, 36), y(b, 76));
        p.lineTo(x(b, 50), y(b, 64));
        p.lineTo(x(b, 64), y(b, 76));
        p.lineTo(x(b, 72), y(b, 42));
        p.close();
        c.drawPath(p, strokePaint);
        c.drawCircle(x(b, 43), y(b, 50), r(b, 2), fillPaint);
        c.drawCircle(x(b, 57), y(b, 50), r(b, 2), fillPaint);
    }

    private void drawTree(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 38), r(b, 18), strokePaint);
        c.drawCircle(x(b, 35), y(b, 48), r(b, 14), strokePaint);
        c.drawCircle(x(b, 65), y(b, 48), r(b, 14), strokePaint);
        c.drawRoundRect(x(b, 45), y(b, 54), x(b, 55), y(b, 82), r(b, 3), r(b, 3), strokePaint);
    }

    private void drawMushroom(Canvas c, Rect b) {
        Path cap = new Path();
        cap.moveTo(x(b, 20), y(b, 54));
        cap.quadTo(x(b, 50), y(b, 20), x(b, 80), y(b, 54));
        cap.close();
        c.drawPath(cap, strokePaint);
        c.drawRoundRect(x(b, 40), y(b, 52), x(b, 60), y(b, 82), r(b, 8), r(b, 8), strokePaint);
        c.drawCircle(x(b, 40), y(b, 44), r(b, 3), fillPaint);
        c.drawCircle(x(b, 58), y(b, 40), r(b, 3), fillPaint);
    }

    private void drawMountain(Canvas c, Rect b) {
        Path p1 = new Path();
        p1.moveTo(x(b, 16), y(b, 78));
        p1.lineTo(x(b, 40), y(b, 34));
        p1.lineTo(x(b, 56), y(b, 78));
        Path p2 = new Path();
        p2.moveTo(x(b, 40), y(b, 78));
        p2.lineTo(x(b, 62), y(b, 46));
        p2.lineTo(x(b, 84), y(b, 78));
        c.drawPath(p1, strokePaint);
        c.drawPath(p2, strokePaint);
    }

    private void drawMoon(Canvas c, Rect b) {
        c.drawCircle(x(b, 54), y(b, 50), r(b, 24), strokePaint);
        c.drawCircle(x(b, 62), y(b, 45), r(b, 20), fillPaint);
    }

    private void drawSun(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 50), r(b, 16), strokePaint);
        for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4f);
            float sx = x(b, 50) + (float) (Math.cos(angle) * r(b, 24));
            float sy = y(b, 50) + (float) (Math.sin(angle) * r(b, 24));
            float ex = x(b, 50) + (float) (Math.cos(angle) * r(b, 34));
            float ey = y(b, 50) + (float) (Math.sin(angle) * r(b, 34));
            c.drawLine(sx, sy, ex, ey, strokePaint);
        }
    }

    private void drawStar(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 50), y(b, 20));
        p.lineTo(x(b, 58), y(b, 42));
        p.lineTo(x(b, 82), y(b, 42));
        p.lineTo(x(b, 62), y(b, 56));
        p.lineTo(x(b, 70), y(b, 80));
        p.lineTo(x(b, 50), y(b, 65));
        p.lineTo(x(b, 30), y(b, 80));
        p.lineTo(x(b, 38), y(b, 56));
        p.lineTo(x(b, 18), y(b, 42));
        p.lineTo(x(b, 42), y(b, 42));
        p.close();
        c.drawPath(p, strokePaint);
    }

    private void drawPlanet(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 50), r(b, 14), strokePaint);
        Path ring = new Path();
        ring.moveTo(x(b, 18), y(b, 56));
        ring.quadTo(x(b, 50), y(b, 36), x(b, 82), y(b, 56));
        ring.quadTo(x(b, 50), y(b, 66), x(b, 18), y(b, 56));
        c.drawPath(ring, strokePaint);
    }

    private void drawCloud(Canvas c, Rect b) {
        c.drawCircle(x(b, 38), y(b, 50), r(b, 12), strokePaint);
        c.drawCircle(x(b, 52), y(b, 42), r(b, 14), strokePaint);
        c.drawCircle(x(b, 66), y(b, 50), r(b, 12), strokePaint);
        c.drawLine(x(b, 28), y(b, 62), x(b, 74), y(b, 62), strokePaint);
    }

    private void drawWave(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 16), y(b, 46));
        p.cubicTo(x(b, 26), y(b, 34), x(b, 34), y(b, 58), x(b, 44), y(b, 46));
        p.cubicTo(x(b, 54), y(b, 34), x(b, 62), y(b, 58), x(b, 72), y(b, 46));
        p.cubicTo(x(b, 78), y(b, 40), x(b, 82), y(b, 48), x(b, 84), y(b, 46));
        c.drawPath(p, strokePaint);
        Path p2 = new Path();
        p2.moveTo(x(b, 16), y(b, 62));
        p2.cubicTo(x(b, 26), y(b, 50), x(b, 34), y(b, 74), x(b, 44), y(b, 62));
        p2.cubicTo(x(b, 54), y(b, 50), x(b, 62), y(b, 74), x(b, 72), y(b, 62));
        p2.cubicTo(x(b, 78), y(b, 56), x(b, 82), y(b, 64), x(b, 84), y(b, 62));
        c.drawPath(p2, strokePaint);
    }

    private void drawFlame(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 50), y(b, 18));
        p.quadTo(x(b, 66), y(b, 36), x(b, 62), y(b, 56));
        p.quadTo(x(b, 59), y(b, 78), x(b, 50), y(b, 84));
        p.quadTo(x(b, 41), y(b, 78), x(b, 38), y(b, 58));
        p.quadTo(x(b, 35), y(b, 44), x(b, 44), y(b, 30));
        p.close();
        c.drawPath(p, strokePaint);
        c.drawCircle(x(b, 50), y(b, 58), r(b, 6), strokePaint);
    }

    private void drawCrystal(Canvas c, Rect b) {
        Path p = new Path();
        p.moveTo(x(b, 50), y(b, 16));
        p.lineTo(x(b, 28), y(b, 40));
        p.lineTo(x(b, 36), y(b, 78));
        p.lineTo(x(b, 64), y(b, 78));
        p.lineTo(x(b, 72), y(b, 40));
        p.close();
        c.drawPath(p, strokePaint);
        c.drawLine(x(b, 50), y(b, 16), x(b, 50), y(b, 78), strokePaint);
    }

    private void drawLotus(Canvas c, Rect b) {
        Path center = new Path();
        center.moveTo(x(b, 50), y(b, 32));
        center.quadTo(x(b, 60), y(b, 50), x(b, 50), y(b, 72));
        center.quadTo(x(b, 40), y(b, 50), x(b, 50), y(b, 32));
        c.drawPath(center, strokePaint);
        Path left = new Path();
        left.moveTo(x(b, 36), y(b, 40));
        left.quadTo(x(b, 44), y(b, 54), x(b, 36), y(b, 70));
        left.quadTo(x(b, 22), y(b, 56), x(b, 36), y(b, 40));
        c.drawPath(left, strokePaint);
        Path right = new Path();
        right.moveTo(x(b, 64), y(b, 40));
        right.quadTo(x(b, 56), y(b, 54), x(b, 64), y(b, 70));
        right.quadTo(x(b, 78), y(b, 56), x(b, 64), y(b, 40));
        c.drawPath(right, strokePaint);
    }

    private void drawSprout(Canvas c, Rect b) {
        c.drawLine(x(b, 50), y(b, 70), x(b, 50), y(b, 36), strokePaint);
        Path left = new Path();
        left.moveTo(x(b, 50), y(b, 46));
        left.quadTo(x(b, 36), y(b, 38), x(b, 30), y(b, 24));
        left.quadTo(x(b, 44), y(b, 24), x(b, 50), y(b, 36));
        c.drawPath(left, strokePaint);
        Path right = new Path();
        right.moveTo(x(b, 50), y(b, 46));
        right.quadTo(x(b, 64), y(b, 38), x(b, 70), y(b, 24));
        right.quadTo(x(b, 56), y(b, 24), x(b, 50), y(b, 36));
        c.drawPath(right, strokePaint);
    }

    private void drawLantern(Canvas c, Rect b) {
        c.drawLine(x(b, 50), y(b, 20), x(b, 50), y(b, 30), strokePaint);
        c.drawRoundRect(x(b, 34), y(b, 30), x(b, 66), y(b, 72), r(b, 10), r(b, 10), strokePaint);
        c.drawLine(x(b, 40), y(b, 72), x(b, 60), y(b, 72), strokePaint);
        c.drawLine(x(b, 44), y(b, 76), x(b, 56), y(b, 76), strokePaint);
    }

    private void drawCompass(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 50), r(b, 28), strokePaint);
        Path p = new Path();
        p.moveTo(x(b, 50), y(b, 26));
        p.lineTo(x(b, 58), y(b, 58));
        p.lineTo(x(b, 50), y(b, 74));
        p.lineTo(x(b, 42), y(b, 42));
        p.close();
        c.drawPath(p, strokePaint);
    }

    private void drawAnchor(Canvas c, Rect b) {
        c.drawCircle(x(b, 50), y(b, 26), r(b, 7), strokePaint);
        c.drawLine(x(b, 50), y(b, 33), x(b, 50), y(b, 66), strokePaint);
        c.drawArc(x(b, 30), y(b, 52), x(b, 70), y(b, 82), 180, 180, false, strokePaint);
        c.drawLine(x(b, 38), y(b, 66), x(b, 28), y(b, 56), strokePaint);
        c.drawLine(x(b, 62), y(b, 66), x(b, 72), y(b, 56), strokePaint);
        c.drawLine(x(b, 42), y(b, 40), x(b, 58), y(b, 40), strokePaint);
    }

    private float x(Rect b, float percent) {
        return b.left + (b.width() * (percent / 100f));
    }

    private float y(Rect b, float percent) {
        return b.top + (b.height() * (percent / 100f));
    }

    private float r(Rect b, float percent) {
        return (Math.min(b.width(), b.height()) * (percent / 100f));
    }

    @Override
    public void setAlpha(int alpha) {
        strokePaint.setAlpha(alpha);
        fillPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        strokePaint.setColorFilter(colorFilter);
        fillPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

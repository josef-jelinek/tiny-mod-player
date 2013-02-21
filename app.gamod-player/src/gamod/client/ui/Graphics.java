package gamod.client.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;

public final class Graphics {
  public interface ButtonMaker {
    Drawable normal();

    Drawable active();

    Drawable disabled();
  }

  private enum State {
    normal, active, disabled
  };

  public static final class PlayButtonMaker implements ButtonMaker {
    public Drawable normal() {
      return new PlayButton(State.normal);
    }

    public Drawable active() {
      return new PlayButton(State.active);
    }

    public Drawable disabled() {
      return new PlayButton(State.disabled);
    }
  }

  public static final class PauseButtonMaker implements ButtonMaker {
    public Drawable normal() {
      return new PauseButton(State.normal);
    }

    public Drawable active() {
      return new PauseButton(State.active);
    }

    public Drawable disabled() {
      return new PauseButton(State.disabled);
    }
  }

  public static final class StopButtonMaker implements ButtonMaker {
    public Drawable normal() {
      return new StopButton(State.normal);
    }

    public Drawable active() {
      return new StopButton(State.active);
    }

    public Drawable disabled() {
      return new StopButton(State.disabled);
    }
  }

  private static abstract class Button extends Drawable {
    protected final Paint bgPaint;
    protected final Paint fgPaint;

    public Button(State state) {
      bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      bgPaint.setColor(state == State.active ? 0xFFDDDDDD : 0xFFFFFFFF);
      bgPaint.setStyle(Paint.Style.FILL);
      fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      fgPaint.setColor(state == State.disabled ? 0x80000000 : 0xFF000000);
      bgPaint.setStyle(Paint.Style.FILL);
    }

    protected void drawBg(Canvas canvas, Rect bounds) {
      float radius = bounds.width() / 2f;
      float cx = bounds.exactCenterX();
      float cy = bounds.exactCenterY();
      canvas.drawCircle(cx, cy, radius, bgPaint);
      canvas.drawCircle(cx, cy, radius, fgPaint);
      canvas.drawCircle(cx, cy, radius * 0.92f, bgPaint);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.UNKNOWN;
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter filter) {}
  }

  private static final class PlayButton extends Button {
    public PlayButton(State state) {
      super(state);
    }

    @Override
    public void draw(Canvas canvas) {
      Rect bounds = getBounds();
      drawBg(canvas, bounds);
      float cx = bounds.exactCenterX();
      float cy = bounds.exactCenterY();
      float rr = bounds.width() / 8f;
      Path path = new Path();
      path.moveTo(cx + 1.4f * rr, cy);
      path.lineTo(cx - 0.8f * rr, cy + 1.2f * rr);
      path.lineTo(cx - 0.8f * rr, cy - 1.2f * rr);
      path.close();
      canvas.drawPath(path, fgPaint);
    }
  }

  private static final class PauseButton extends Button {
    public PauseButton(State state) {
      super(state);
    }

    @Override
    public void draw(Canvas canvas) {
      Rect bounds = getBounds();
      drawBg(canvas, bounds);
      float cx = bounds.exactCenterX();
      float cy = bounds.exactCenterY();
      float rr = bounds.width() / 8f;
      canvas.drawRect(cx - rr, cy - rr, cx - rr / 4, cy + rr, fgPaint);
      canvas.drawRect(cx + rr / 4, cy - rr, cx + rr, cy + rr, fgPaint);
    }
  }

  private static final class StopButton extends Button {
    public StopButton(State state) {
      super(state);
    }

    @Override
    public void draw(Canvas canvas) {
      Rect bounds = getBounds();
      drawBg(canvas, bounds);
      float cx = bounds.exactCenterX();
      float cy = bounds.exactCenterY();
      float rr = (bounds.width() / 2f) / 4;
      canvas.drawRect(cx - rr, cy - rr, cx + rr, cy + rr, fgPaint);
    }
  }
}

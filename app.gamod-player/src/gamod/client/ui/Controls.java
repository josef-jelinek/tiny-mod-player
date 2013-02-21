package gamod.client.ui;

import gamod.client.Start;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.view.View;
import android.widget.*;

public final class Controls implements Presentation {
  private final Start activity;
  private final Button playButton;
  private final Button pauseButton;
  private final Button stopButton;
  private final TextView infoText;

  public Controls(Start start, Button play, Button pause, Button stop, TextView info) {
    activity = start;
    playButton = play;
    pauseButton = pause;
    stopButton = stop;
    infoText = info;
    setButtonGraphics();
    setButtonCallbacks();
    showButtonsForStop();
  }

  private void setButtonGraphics() {
    setBackground(playButton, new Graphics.PlayButtonMaker());
    setBackground(pauseButton, new Graphics.PauseButtonMaker());
    setBackground(stopButton, new Graphics.StopButtonMaker());
  }

  private void setBackground(Button button, Graphics.ButtonMaker buttonMaker) {
    StateListDrawable background = new StateListDrawable();
    int[] pressed = { android.R.attr.state_pressed };
    int[] focused = { android.R.attr.state_focused };
    int[] disabled = { -android.R.attr.state_enabled };
    background.addState(pressed, buttonMaker.active());
    background.addState(focused, buttonMaker.active());
    background.addState(disabled, buttonMaker.disabled());
    background.addState(StateSet.WILD_CARD, buttonMaker.normal());
    button.setBackgroundDrawable(background);
  }

  private void setButtonCallbacks() {
    playButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        activity.playClicked();
      }
    });
    pauseButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        activity.pauseClicked();
      }
    });
    stopButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        activity.stopClicked();
      }
    });
  }

  public void showButtonsForPlay() {
    hideButton(playButton);
    showButton(pauseButton);
    stopButton.setEnabled(true);
  }

  public void showButtonsForPause() {
    showButton(playButton);
    hideButton(pauseButton);
    stopButton.setEnabled(true);
  }

  public void showButtonsForStop() {
    showButton(playButton);
    hideButton(pauseButton);
    stopButton.setEnabled(false);
  }

  private void showButton(final Button button) {
    button.setEnabled(true);
    button.setVisibility(View.VISIBLE);
  }

  private void hideButton(final Button button) {
    button.setEnabled(false);
    button.setVisibility(View.GONE);
  }

  public void showInfo(final String s) {
    infoText.setText(s);
  }
}

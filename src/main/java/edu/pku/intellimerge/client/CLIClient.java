package edu.pku.intellimerge.client;

import java.text.DecimalFormat;

public class CLIClient {

  private long minimum = 0;

  private long maximum = 100;

  private long barLen = 100;

  private char showChar = '=';

  private DecimalFormat formater = new DecimalFormat("#.##%");

  public CLIClient() {}

  /**
   * Show progress and precent in standard output
   *
   * @param minimum
   * @param maximum
   * @param barLen
   */
  public CLIClient(long minimum, long maximum, long barLen) {
    this(minimum, maximum, barLen, '=');
  }

  /**
   * Show progress and precent in standard output
   *
   * @param minimum
   * @param maximum
   * @param barLen
   * @param showChar
   */
  public CLIClient(long minimum, long maximum, long barLen, char showChar) {
    this.minimum = minimum;
    this.maximum = maximum;
    this.barLen = barLen;
    this.showChar = showChar;
  }

  public static void main(String[] args) throws InterruptedException {
    CLIClient cpb = new CLIClient(0, 100, 30, '#');
    for (int i = 1; i <= 100; i++) {
      cpb.show(i);
      Thread.sleep(100);
    }
  }

  /**
   * Show the progress bar
   *
   * @param value current progress (start <= current <= end)
   */
  public void show(long value) {
    if (value < minimum || value > maximum) {
      return;
    }

    reset();
    minimum = value;
    float rate = (float) (minimum * 1.0 / maximum);
    long len = (long) (rate * barLen);
    draw(len, rate);
    if (minimum == maximum) {
      afterComplete();
    }
  }

  private void draw(long len, float rate) {
    System.out.print("Progress: ");
    for (int i = 0; i < len; i++) {
      System.out.print(showChar);
    }
    System.out.print(' ');
    System.out.print(format(rate));
  }

  private void reset() {
    // move the cursor to begin
    System.out.print('\r');
  }

  private void afterComplete() {
    System.out.print('\n');
  }

  private String format(float num) {
    return formater.format(num);
  }
}

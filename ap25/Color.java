package ap25;

import java.util.Map;

/**
 * 石の色や状態を表す列挙型。
 * BLACK: 黒石
 * WHITE: 白石
 * NONE: 空きマス
 * BLOCK: 壁やブロック
 */
public enum Color {
  BLACK(1),   // 黒石
  WHITE(-1),  // 白石
  NONE(0),    // 空きマス
  BLOCK(3);   // 壁やブロック

  // 各Colorに対応する記号を保持するマップ
  static Map<Color, String> SYMBOLS =
      Map.of(BLACK, "o", WHITE, "x", NONE, " ", BLOCK, "#");

  private int value; // 色を整数値で表現

  /**
   * コンストラクタ
   * @param value 色を表す整数値
   */
  private Color(int value) {
    this.value = value;
  }

  /**
   * 色の整数値を取得
   * @return 色の値
   */
  public int getValue() {
    return this.value;
  }

  /**
   * 色を反転（黒⇔白、その他はそのまま）
   * @return 反転した色
   */
  public Color flipped() {
    switch (this) {
    case BLACK: return WHITE;
    case WHITE: return BLACK;
    default: return this;
    }
  }

  /**
   * 色に対応する記号を返す
   * @return 記号文字列
   */
  public String toString() {
    return SYMBOLS.get(this);
  }

  /**
   * 文字列からColorを取得
   * @param str 記号文字列
   * @return 対応するColor（該当しない場合はNONE）
   */
  public Color parse(String str) {
    return Map.of("o", BLACK, "x" , WHITE).getOrDefault(str, NONE);
  }
}

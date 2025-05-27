package ap25;

import static ap25.Board.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ゲームの手（着手）を表すクラス。
 * 位置や色、特殊な手（パス・タイムアウト・反則など）を管理する。
 */
public class Move {
  public final static int PASS = -1;     // パスを表す定数
  final static int TIMEOUT = -10;        // タイムアウトを表す定数
  final static int ILLEGAL = -20;        // 反則を表す定数
  final static int ERROR = -30;          // エラーを表す定数

  int index;     // 盤面上の位置（0～LENGTH-1、特殊値あり）
  Color color;   // 着手した色

  /**
   * 指定位置・色のMoveを生成
   */
  public static Move of(int index, Color color) {
    return new Move(index, color);
  }

  /**
   * 文字列（例: "a1"）と色からMoveを生成
   */
  public static Move of(String pos, Color color) {
    return new Move(parseIndex(pos), color);
  }

  /**
   * パスのMoveを生成
   */
  public static Move ofPass(Color color) {
    return new Move(PASS, color);
  }

  /**
   * タイムアウトのMoveを生成
   */
  public static Move ofTimeout(Color color) {
    return new Move(TIMEOUT, color);
  }

  /**
   * 反則のMoveを生成
   */
  public static Move ofIllegal(Color color) {
    return new Move(ILLEGAL, color);
  }

  /**
   * エラーのMoveを生成
   */
  public static Move ofError(Color color) {
    return new Move(ERROR, color);
  }

  /**
   * コンストラクタ
   * @param index 盤面上の位置
   * @param color 着手した色
   */
  public Move(int index, Color color) {
    this.index = index;
    this.color = color;
  }

  /** インデックスを取得 */
  public int getIndex() { return this.index; }
  /** 行番号を取得 */
  public int getRow() { return this.index / SIZE; }
  /** 列番号を取得 */
  public int getCol() { return this.index % SIZE; }
  /** 色を取得 */
  public Color getColor() { return this.color; }
  /** ハッシュ値を取得 */
  public int hashCode() { return Objects.hash(this.index, this.color); }

  /**
   * オブジェクト同士の等価性判定
   */
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Move other = (Move) obj;
    return this.index == other.index && this.color == other.color;
  }

  /** 色がNONEかどうか */
  public boolean isNone() { return this.color == Color.NONE; }

  /** 合法手かどうか */
  public boolean isLegal() { return this.index >= PASS; }
  /** パスかどうか */
  public boolean isPass() { return this.index == PASS; }

  /** 反則・タイムアウト・エラーかどうか */
  public boolean isFoul() { return this.index < PASS; }
  public boolean isTimeout() { return this.index == TIMEOUT; }
  public boolean isIllega() { return this.index == ILLEGAL; }
  public boolean isError() { return this.index == ERROR; }

  /**
   * 色を反転したMoveを返す
   */
  public Move flipped() {
    return new Move(this.index, this.color.flipped());
  }

  /**
   * 指定した色で新しいMoveを返す
   */
  public Move colored(Color color) {
    return new Move(this.index, color);
  }

  /**
   * 指定した座標が盤面内か判定
   */
  public static boolean isValid(int col, int row) {
    return 0 <= col && col < SIZE && 0 <= row && row < SIZE;
  }

  /**
   * 8方向のオフセットを返す
   */
  static int[][] offsets(int dist) {
    return new int[][] {
      { -dist, 0 }, { -dist, dist }, { 0, dist }, { dist, dist },
      { dist, 0 }, { dist, -dist }, { 0, -dist }, { -dist, -dist } };
  }

  /**
   * 指定位置の隣接マスのインデックスリストを返す
   */
  public static List<Integer> adjacent(int k) {
    var ps = new ArrayList<Integer>();
    int col0 = k % SIZE, row0 = k / SIZE;

    for (var o : offsets(1)) {
      int col = col0 + o[0], row = row0 + o[1];
      if (Move.isValid(col, row)) ps.add(index(col, row));
    }

    return ps;
  }

  /**
   * 指定位置から指定方向の直線上のインデックスリストを返す
   */
  public static List<Integer> line(int k, int dir) {
    var line = new ArrayList<Integer>();
    int col0 = k % SIZE, row0 = k / SIZE;

    for (int dist = 1; dist < SIZE; dist++) {
      var o = offsets(dist)[dir];
      int col = col0 + o[0], row = row0 + o[1];
      if (Move.isValid(col, row) == false)
        break;
      line.add(index(col, row));
    }

    return line;
  }

  /**
   * 列・行からインデックスを計算
   */
  public static int index(int col, int row) {
    return SIZE * row + col;
  }

  /**
   * インスタンスのインデックスを文字列化
   */
  public String toString() {
    return toIndexString(this.index);
  }

  /**
   * 文字列（例: "a1"）からインデックスを計算
   */
  public static int parseIndex(String pos) {
    return SIZE * (pos.charAt(1) - '1') + pos.charAt(0) - 'a';
  }

  /**
   * インデックスを文字列（例: "a1"）に変換
   */
  public static String toIndexString(int index) {
    if (index == PASS) return "..";
    if (index == TIMEOUT) return "@";
    return toColString(index % SIZE) + toRowString(index / SIZE);
  }

  /**
   * 列番号を文字列（a～）に変換
   */
  public static String toColString(int col) {
    return Character.toString('a' + col);
  }

  /**
   * 行番号を文字列（1～）に変換
   */
  public static String toRowString(int row) {
    return Character.toString('1' + row);
  }

  /**
   * インデックスリストを文字列リストに変換
   */
  public static List<String> toStringList(List<Integer> moves) {
    return moves.stream().map(k -> toIndexString(k)).toList();
  }
}

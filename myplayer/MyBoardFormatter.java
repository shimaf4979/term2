package myplayer;

import ap25.*;
import static ap25.Board.*;
import static ap25.Color.*;
import java.util.List;
import java.util.Map;

/**
 * MyBoardの状態を人間が読みやすい形式で表示するクラス
 * ボードの可視化と合法手の表示を担当
 * ゲームの進行状況を分かりやすく表示するためのフォーマッター
 */
public class MyBoardFormatter {
  /**
   * ボードを文字列形式で表示
   * 行列形式でボードを描画し、石の配置、合法手、最後の手などを視覚的に表示
   * 
   * 表示例:
   * abcdef
   * 1| | c4
   * 2| .xo. | o: [c2, e2]
   * 3| xXo |
   * 4| oX |
   * 5| .ox. |
   * 6| |
   * 
   * 記号の意味:
   * - o: 黒石
   * - x: 白石
   * - .: 現在のターンの合法手
   * - 大文字: 最後に打たれた石
   * - 右側: 最後の手と現在ターンの合法手リスト
   * 
   * @param board 表示するボード
   * @return フォーマットされた文字列
   */
  public static String format(MyBoard board) {
    var turn = board.getTurn();
    var move = board.getMove();
    var blacks = board.findNoPassLegalIndexes(BLACK);
    var whites = board.findNoPassLegalIndexes(WHITE);
    var legals = Map.of(BLACK, blacks, WHITE, whites);

    var buf = new StringBuilder("  ");
    for (int k = 0; k < SIZE; k++)
      buf.append(Move.toColString(k));
    buf.append("\n");

    for (int k = 0; k < SIZE * SIZE; k++) {
      int col = k % SIZE;
      int row = k / SIZE;

      if (col == 0)
        buf.append((row + 1) + "|");

      if (board.get(k) == NONE) {
        boolean legal = false;
        var b = blacks.contains(k);
        var w = whites.contains(k);
        if (turn == BLACK && b)
          legal = true;
        if (turn == WHITE && w)
          legal = true;
        buf.append(legal ? '.' : ' ');
      } else {
        var s = board.get(k).toString();
        if (move != null && k == move.getIndex())
          s = s.toUpperCase();
        buf.append(s);
      }

      if (col == SIZE - 1) {
        buf.append("| ");
        if (row == 0 && move != null) {
          buf.append(move);
        } else if (row == 1) {
          buf.append(turn + ": " + toString(legals.get(turn)));
        }
        buf.append("\n");
      }
    }

    buf.setLength(buf.length() - 1);
    return buf.toString();
  }

  /**
   * 手のリストを文字列リストに変換
   * 位置インデックスのリストを人間が読みやすい座標文字列のリストに変換
   * 例: [14, 15, 16] -> ["c3", "d3", "e3"]
   * 
   * @param moves 手のリスト（位置インデックス）
   * @return 文字列表現のリスト
   */
  static List<String> toString(List<Integer> moves) {
    return moves.stream().map(k -> Move.toIndexString(k)).toList();
  }
}
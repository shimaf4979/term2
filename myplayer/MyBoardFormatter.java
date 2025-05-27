package myplayer;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.List;
import java.util.Map;

import ap25.*;

/**
 * 盤面を見やすい文字列形式に整形するユーティリティクラス。
 */
public class MyBoardFormatter {
  /**
   * 盤面情報を文字列として整形して返す
   * @param board 表示対象の盤面
   * @return 整形済みの盤面文字列
   */
  public static String format(MyBoard board) {
    var turn = board.getTurn(); // 現在の手番
    var move = board.getMove(); // 直前の手
    var blacks = board.findNoPassLegalIndexes(BLACK); // 黒の合法手
    var whites = board.findNoPassLegalIndexes(WHITE); // 白の合法手
    var legals = Map.of(BLACK, blacks, WHITE, whites); // 合法手マップ

    var buf = new StringBuilder("  ");
    // 列ラベル（a～fなど）を追加
    for (int k = 0; k < SIZE; k++) buf.append(Move.toColString(k));
    buf.append("\n");

    // 各マスごとに盤面を出力
    for (int k = 0; k < SIZE * SIZE; k++) {
      int col = k % SIZE;
      int row = k / SIZE;

      if (col == 0) buf.append((row + 1) + "|");

      if (board.get(k) == NONE) {
        // 空きマスの場合、合法手なら'.'、それ以外は空白
        boolean legal = false;
        var b = blacks.contains(k);
        var w = whites.contains(k);
        if (turn == BLACK && b) legal = true;
        if (turn == WHITE && w) legal = true;
        buf.append(legal ? '.' : ' ');
      } else {
        // 石がある場合は記号を表示（直前の手は大文字）
        var s = board.get(k).toString();
        if (move != null && k == move.getIndex()) s = s.toUpperCase();
        buf.append(s);
      }

      if (col == SIZE - 1) {
        buf.append("| ");
        // 1行目に直前の手、2行目に合法手リストを表示
        if (row == 0 && move != null) {
          buf.append(move);
        } else if (row == 1) {
          buf.append(turn + ": " + toString(legals.get(turn)));
        }
        buf.append("\n");
      }
    }

    // 最後の改行を削除
    buf.setLength(buf.length() - 1);
    return buf.toString();
  }

  /**
   * インデックスリストを文字列リストに変換
   * @param moves インデックスリスト
   * @return 文字列リスト
   */
  static List<String> toString(List<Integer> moves) {
    return moves.stream().map(k -> Move.toIndexString(k)).toList();
  }
}

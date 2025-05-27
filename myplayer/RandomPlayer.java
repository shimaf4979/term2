package myplayer;

import ap25.*;
import java.util.Random;

/**
 * ランダムに合法手を選ぶプレイヤークラス。
 */
public class RandomPlayer extends Player {
  Random rand = new Random(); // 乱数生成器

  /**
   * コンストラクタ
   * @param color プレイヤーの色
   */
  public RandomPlayer(Color color) {
    super("R", color);
  }

  /**
   * 合法手の中からランダムに1手選んで返す
   * @param board 現在の盤面
   * @return 選択した手
   */
  public Move think(Board board) {
    var moves = board.findLegalMoves(getColor());
    var i = this.rand.nextInt(moves.size());
    return moves.get(i);
  }
}

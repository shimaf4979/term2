package myplayer;

import ap25.*;
import java.util.Random;

/**
 * ランダムに合法手を選択するプレイヤー
 * テスト用やベンチマーク用として使用される
 * 戦略を持たずに純粋にランダムで手を選ぶため、AIの性能測定の基準として活用
 */
public class RandomPlayer extends Player {
  /**
   * 乱数生成器
   * 合法手の中からランダムに選択するために使用
   */
  Random rand = new Random();

  /**
   * コンストラクタ
   * プレイヤー名を"R"に設定し、指定された色で初期化
   * 
   * @param color プレイヤーの色（BLACK or WHITE）
   */
  public RandomPlayer(Color color) {
    super("R", color);
  }

  /**
   * ランダムに手を選択
   * 合法手のリストを取得し、その中からランダムに1つを選択
   * パスも含めて純粋にランダム選択を行う
   * 
   * @param board 現在のボード状態
   * @return ランダムに選択された合法手
   */
  public Move think(Board board) {
    var moves = board.findLegalMoves(getColor());
    var i = this.rand.nextInt(moves.size());
    return moves.get(i);
  }
}
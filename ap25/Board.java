package ap25;

import java.util.List;

/**
 * ボードの状態や操作を定義するインターフェース。
 * ゲーム盤面に関する基本的な操作を規定する。
 */
public interface Board {
  int SIZE = 6;                // 盤面の一辺の長さ
  int LENGTH = SIZE * SIZE;    // 盤面の全マス数

  /**
   * 指定位置の色を取得する
   * @param k 位置（0～LENGTH-1）
   * @return 指定位置の色
   */
  Color get(int k);

  /**
   * 現在の手（直前の手）を取得する
   * @return 直前の手
   */
  Move getMove();

  /**
   * 現在の手番の色を取得する
   * @return 手番の色
   */
  Color getTurn();

  /**
   * 指定した色の石の数を数える
   * @param color 色
   * @return 石の数
   */
  int count(Color color);

  /**
   * ゲームが終了しているか判定する
   * @return 終了ならtrue
   */
  boolean isEnd();

  /**
   * 勝者の色を返す（引き分けの場合はNONE）
   * @return 勝者の色
   */
  Color winner();

  /**
   * 指定した色の反則を記録する
   * @param color 反則した色
   */
  void foul(Color color);

  /**
   * 現在のスコアを返す
   * @return スコア
   */
  int score();

  /**
   * 指定した色の合法手を全て返す
   * @param color 手番の色
   * @return 合法手のリスト
   */
  List<Move> findLegalMoves(Color color);

  /**
   * 指定した手を打った後の新しい盤面を返す
   * @param move 着手
   * @return 新しい盤面
   */
  Board placed(Move move);

  /**
   * 盤面を反転した新しい盤面を返す
   * @return 反転盤面
   */
  Board flipped();

  /**
   * 盤面のディープコピーを返す
   * @return 複製された盤面
   */
  Board clone();
}

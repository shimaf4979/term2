package myplayer;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ap25.*;

/**
 * ボード評価を行うクラス
 * 各マスに戦略的重要性に基づく重み付けを行い、ボード全体の評価値を計算
 * 角や辺は高く評価し、角に隣接するマスは低く評価する
 */
class MyEval {
  /**
   * 各マスの重み行列（6x6）
   * 角は最高評価(10点)、辺も高評価(10点)、通常マスは標準(1点)
   * 角に隣接するマス（角を相手に取られるリスク）は低評価(-5点)
   */
  static float[][] M = {
      { 10, 10, 10, 10, 10, 10 },
      { 10, -5, 1, 1, -5, 10 },
      { 10, 1, 1, 1, 1, 10 },
      { 10, 1, 1, 1, 1, 10 },
      { 10, -5, 1, 1, -5, 10 },
      { 10, 10, 10, 10, 10, 10 },
  };

  /**
   * ボードの評価値を計算
   * ゲーム終了時は実際のスコアに大きな重みを付けて返す
   * 通常時は各マスの重み×石の価値の合計を計算
   * 
   * @param board 評価するボード
   * @return 評価値（正の値は黒有利、負の値は白有利）
   */
  public float value(Board board) {
    if (board.isEnd())
      return 1000000 * board.score();

    return (float) IntStream.range(0, LENGTH)
        .mapToDouble(k -> score(board, k))
        .reduce(Double::sum).orElse(0);
  }

  /**
   * 指定した位置の評価値を計算
   * その位置の重み×石の価値（黒=1, 白=-1, 空=0）
   * 
   * @param board 評価するボード
   * @param k     評価する位置
   * @return その位置の評価値
   */
  float score(Board board, int k) {
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

/**
 * ミニマックス法とアルファベータ枝刈りを使用するAIプレイヤー
 * 位置評価とゲーム木探索により最適手を選択する
 * デフォルトで3手先まで読んで最適解を探索
 */
public class MyPlayer extends ap25.Player {
  /**
   * プレイヤーの識別名
   */
  static final String MY_NAME = "MY24";

  /**
   * ボード評価を行うオブジェクト
   */
  MyEval eval;

  /**
   * 探索の深さ制限（何手先まで読むか）
   */
  int depthLimit;

  /**
   * 現在選択されている手
   * 探索中に最適手として更新される
   */
  Move move;

  /**
   * 内部で管理するボード状態
   */
  MyBoard board;

  /**
   * デフォルトコンストラクタ
   * 名前、評価関数、探索深度をデフォルト値で初期化
   * 
   * @param color プレイヤーの色（BLACK or WHITE）
   */
  public MyPlayer(Color color) {
    this(MY_NAME, color, new MyEval(), 3);
  }

  /**
   * フルパラメータコンストラクタ
   * 
   * @param name       プレイヤー名
   * @param color      プレイヤーの色
   * @param eval       使用する評価関数
   * @param depthLimit 探索深度の上限
   */
  public MyPlayer(String name, Color color, MyEval eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new MyBoard();
  }

  /**
   * 探索深度指定コンストラクタ
   * 
   * @param name       プレイヤー名
   * @param color      プレイヤーの色
   * @param depthLimit 探索深度の上限
   */
  public MyPlayer(String name, Color color, int depthLimit) {
    this(name, color, new MyEval(), depthLimit);
  }

  /**
   * 外部からボード状態を設定
   * ゲーム進行に合わせて内部ボードを更新
   * 
   * @param board 設定するボード状態
   */
  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      this.board.set(i, board.get(i));
    }
  }

  /**
   * このプレイヤーが黒かどうかを判定
   * 
   * @return 黒の場合true
   */
  boolean isBlack() {
    return getColor() == BLACK;
  }

  /**
   * 次の手を考える（メイン思考ルーチン）
   * パスしかできない場合はパスを返し、そうでなければミニマックス探索を実行
   * 
   * @param board 現在のボード状態
   * @return 選択した手
   */
  public Move think(Board board) {
    this.board = this.board.placed(board.getMove());

    if (this.board.findNoPassLegalIndexes(getColor()).size() == 0) {
      this.move = Move.ofPass(getColor());
    } else {
      var newBoard = isBlack() ? this.board.clone() : this.board.flipped();
      this.move = null;

      maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

      this.move = this.move.colored(getColor());
    }

    this.board = this.board.placed(this.move);
    return this.move;
  }

  /**
   * 最大化ノードの探索（自分のターン）
   * 黒番の手を探索し、最も評価値の高い手を選択
   * アルファベータ枝刈りを使用して効率化
   * 
   * @param board 現在のボード状態
   * @param alpha アルファ値（これまでの最大値）
   * @param beta  ベータ値（これまでの最小値）
   * @param depth 現在の探索深度
   * @return 最大評価値
   */
  float maxSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth))
      return this.eval.value(board);

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves);

    if (depth == 0)
      this.move = moves.get(0);

    for (var move : moves) {
      var newBoard = board.placed(move);
      float v = minSearch(newBoard, alpha, beta, depth + 1);

      if (v > alpha) {
        alpha = v;
        if (depth == 0)
          this.move = move;
      }

      if (alpha >= beta)
        break;
    }

    return alpha;
  }

  /**
   * 最小化ノードの探索（相手のターン）
   * 白番の手を探索し、最も評価値の低い手を選択
   * アルファベータ枝刈りを使用して効率化
   * 
   * @param board 現在のボード状態
   * @param alpha アルファ値（これまでの最大値）
   * @param beta  ベータ値（これまでの最小値）
   * @param depth 現在の探索深度
   * @return 最小評価値
   */
  float minSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth))
      return this.eval.value(board);

    var moves = board.findLegalMoves(WHITE);
    moves = order(moves);

    for (var move : moves) {
      var newBoard = board.placed(move);
      float v = maxSearch(newBoard, alpha, beta, depth + 1);
      beta = Math.min(beta, v);
      if (alpha >= beta)
        break;
    }

    return beta;
  }

  /**
   * 探索終了条件の判定
   * ゲーム終了または深度制限に達した場合に探索を終了
   * 
   * @param board 現在のボード状態
   * @param depth 現在の探索深度
   * @return 探索を終了すべき場合true
   */
  boolean isTerminal(Board board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  /**
   * 手の順序を決定する
   * 現在はランダムシャッフルを使用（将来的には改善可能）
   * 
   * @param moves 並び替える手のリスト
   * @return 並び替えられた手のリスト
   */
  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}
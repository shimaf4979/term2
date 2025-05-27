package myplayer;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ap25.*;

/**
 * 評価関数クラス。盤面の価値を計算する。
 */
class MyEval {
  // 盤面の各マスの重み
  static float[][] M = {
      { 10,  10, 10, 10,  10,  10},
      { 10,  -5,  1,  1,  -5,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,   1,  1,  1,   1,  10},
      { 10,  -5,  1,  1,  -5,  10},
      { 10,  10, 10, 10,  10,  10},
  };

  /**
   * 盤面全体の評価値を返す
   */
  public float value(Board board) {
    if (board.isEnd()) return 1000000 * board.score();

    return (float) IntStream.range(0, LENGTH)
      .mapToDouble(k -> score(board, k))
      .reduce(Double::sum).orElse(0);
  }

  /**
   * 指定マスの評価値を返す
   */
  float score(Board board, int k) {
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

/**
 * αβ法による探索型プレイヤー
 */
public class MyPlayer extends ap25.Player {
  static final String MY_NAME = "MY24"; // プレイヤー名
  MyEval eval;        // 評価関数
  int depthLimit;     // 探索の深さ制限
  Move move;          // 選択した手
  MyBoard board;      // 内部で保持する盤面

  /**
   * コンストラクタ（デフォルト）
   */
  public MyPlayer(Color color) {
    this(MY_NAME, color, new MyEval(), 2);
  }

  /**
   * コンストラクタ（詳細指定）
   */
  public MyPlayer(String name, Color color, MyEval eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new MyBoard();
  }

  /**
   * コンストラクタ（評価関数省略）
   */
  public MyPlayer(String name, Color color, int depthLimit) {
    this(name, color, new MyEval(), depthLimit);
  }

  /**
   * 盤面をセットする
   */
  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      this.board.set(i, board.get(i));
    }
  }

  /** 黒番かどうか */
  boolean isBlack() { return getColor() == BLACK; }

  /**
   * 着手を決定する
   */
  public Move think(Board board) {
    this.board = this.board.placed(board.getMove());

    // 合法手がなければパス
    if (this.board.findNoPassLegalIndexes(getColor()).size() == 0) {
      this.move = Move.ofPass(getColor());
    } else {
      // 黒番ならそのまま、白番なら盤面を反転して探索
      var newBoard = isBlack() ? this.board.clone() : this.board.flipped();
      this.move = null;

      maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

      this.move = this.move.colored(getColor());
    }

    this.board = this.board.placed(this.move);
    return this.move;
  }

  /**
   * αβ法のmaxノード探索
   */
  float maxSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves);

    if (depth == 0)
      this.move = moves.get(0);

    for (var move: moves) {
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
   * αβ法のminノード探索
   */
  float minSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(WHITE);
    moves = order(moves);

    for (var move: moves) {
      var newBoard = board.placed(move);
      float v = maxSearch(newBoard, alpha, beta, depth + 1);
      beta = Math.min(beta, v);
      if (alpha >= beta) break;
    }

    return beta;
  }

  /**
   * 探索打ち切り条件
   */
  boolean isTerminal(Board board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  /**
   * 手の順序をランダム化
   */
  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}

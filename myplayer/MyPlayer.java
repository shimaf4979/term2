package myplayer;

import static ap25.Board.*;
import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ap25.*;

class MyEval {
  // 改善案1: コーナー周辺の評価値を調整
  static float[][] M = {
      { 100, -50,  25,  25, -50, 100 }, // コーナーは高評価、隣接マスは低評価
      { -50, -25,   1,   1, -25, -50 },
      {  25,   1,   1,   1,   1,  25 },
      {  25,   1,   1,   1,   1,  25 },
      { -50, -25,   1,   1, -25, -50 },
      { 100, -50,  25,  25, -50, 100 },
  };

  // 角からの安定石数をカウント（簡易版）
  int stableStones(Board board, int colorValue) {
    int stable = 0;
    int[][] corners = { {0,0}, {0,5}, {5,0}, {5,5} };
    for (var c : corners) {
      int dx = c[0] == 0 ? 1 : -1;
      int dy = c[1] == 0 ? 1 : -1;
      int x = c[0], y = c[1];
      // 角が自分の石なら端まで連続している石をカウント
      if (board.get(x * SIZE + y).getValue() == colorValue) {
        int i = x;
        while (i >= 0 && i < SIZE && board.get(i * SIZE + y).getValue() == colorValue) {
          int jj = y;
          while (jj >= 0 && jj < SIZE && board.get(i * SIZE + jj).getValue() == colorValue) {
            stable++;
            jj += dy;
          }
          i += dx;
        }
      }
    }
    return stable;
  }

  public float value(Board board) {
    if (board.isEnd()) return 1000000 * board.score(); // ゲーム終了時は石数で評価

    // 改善案2: モビリティを評価に追加
    float mobilityScore = board.findLegalMoves(BLACK).size() - board.findLegalMoves(WHITE).size();

    int piecesCount = IntStream.range(0, LENGTH)
        .map(i -> board.get(i).getValue() != 0 ? 1 : 0).sum();

    // 安定石の評価を追加
    int myStable = stableStones(board, 1);
    int oppStable = stableStones(board, -1);
    float stableScore = (myStable - oppStable) * 10; // 重みは10

    // パリティ（偶奇性）を終盤で加味
    float parityScore = 0;
    if (piecesCount > 40) {
      int empty = LENGTH - piecesCount;
      if (empty % 2 == 0) parityScore = 5; // 偶数残り有利
      else parityScore = -5;
    }

    if (piecesCount > 24) { // 終盤の局面
        return board.score() * 100 + stableScore + parityScore;
    }

    return (float) IntStream.range(0, LENGTH)
        .mapToDouble(k -> score(board, k))
        .reduce(Double::sum).orElse(0) + mobilityScore * 5 + stableScore + parityScore;
  }

  float score(Board board, int k) {
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

public class MyPlayer extends ap25.Player {
  static final String MY_NAME = "8128";
  MyEval eval;
  int depthLimit;
  Move move;
  MyBoard board;

  public MyPlayer(Color color) {
    this(MY_NAME, color, new MyEval(), 3);
  }

  public MyPlayer(String name, Color color, MyEval eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new MyBoard();
  }

  public MyPlayer(String name, Color color, int depthLimit) {
    this(name, color, new MyEval(), depthLimit);
  }

  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      this.board.set(i, board.get(i));
    }
  }

  boolean isBlack() { return getColor() == BLACK; }

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

  float maxSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(BLACK);
    moves = order(moves, board, BLACK); // 改善案4: Moveの並び順を評価値でソート

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

  float minSearch(Board board, float alpha, float beta, int depth) {
    if (isTerminal(board, depth)) return this.eval.value(board);

    var moves = board.findLegalMoves(WHITE);
    moves = order(moves, board, WHITE); // 改善案4: Moveの並び順を評価値でソート

    for (var move: moves) {
      var newBoard = board.placed(move);
      float v = maxSearch(newBoard, alpha, beta, depth + 1);
      beta = Math.min(beta, v);
      if (alpha >= beta) break;
    }

    return beta;
  }

  boolean isTerminal(Board board, int depth) {
    // 改善案3: 終盤では探索深度を動的に変更
    int piecesCount = IntStream.range(0, LENGTH)
        .map(i -> board.get(i).getValue() != 0 ? 1 : 0).sum();

    int dynamicDepthLimit = piecesCount > 24 ? this.depthLimit + 2 : this.depthLimit; // 終盤で探索を深くする
    return board.isEnd() || depth > dynamicDepthLimit;
  }

  List<Move> order(List<Move> moves, Board board, Color color) {
    // 改善案4: Moveの並び順を評価値でソート
    return moves.stream()
        .sorted((move1, move2) -> {
            Board board1 = board.placed(move1);
            Board board2 = board.placed(move2);
            float value1 = this.eval.value(board1);
            float value2 = this.eval.value(board2);
            return Float.compare(value2, value1); // 大きい評価値が先になるように
        }).toList();
  }
}
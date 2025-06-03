package p25x01;

import ap25.*;
import static ap25.Board.*;
import static ap25.Color.*;
import java.util.*;

class MyEval {
  static float[][] M = {
      { 100,  -5, 10, 10,  -5,  100},
      { -5,  -25,  1,  1,  -25,  -5},
      { 10,   1,  5,  5,   1,  10},
      { 10,   1,  5,  5,   1,  10},
      { -5,  -25,  1,  1,  -25,  -5},
      { 100,  -5, 10, 10,  -5,  100},
  };

  public float value(Board board) {
    if (board.isEnd()) return 1000000 * board.score();
    float sum = 0;
    for (int k = 0; k < LENGTH; k++) {
      sum += M[k / SIZE][k % SIZE] * board.get(k).getValue();
    }
    // モビリティ（自分の合法手数－相手の合法手数）を加味
    int myMoves = board.findLegalMoves(BLACK).size();
    int oppMoves = board.findLegalMoves(WHITE).size();
    sum += 5 * (myMoves - oppMoves); // 重みは調整可
    return sum;
  }
}

public class MyPlayerrui extends ap25.Player {
  static final String MY_NAME = "MY24";
  MyEval eval;
  int depthLimit;
  Move move;
  OurBoard board;
  static final int ENDGAME_FULL_SEARCH = 7; // 残り7手から完全読み
  Map<Long, Float> transTable; // 盤面ハッシュ→評価値キャッシュ

  public MyPlayerrui(Color color) {
    this(MY_NAME, color, new MyEval(), 4);
  }

  public MyPlayerrui(String name, Color color, MyEval eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new OurBoard();
    this.transTable = new HashMap<>();
  }

  public MyPlayerrui(String name, Color color, int depthLimit) {
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

    // 残り手数を計算
    int empty = 0;
    for (int i = 0; i < LENGTH; i++) {
      if (this.board.get(i) == ap25.Color.NONE) empty++;
    }
    // 残り7手以下なら完全探索、それ以外はdepthLimit
    int searchLimit = (empty <= ENDGAME_FULL_SEARCH) ? empty : this.depthLimit;

    // パス処理強化
    var legalIndexes = this.board.findNoPassLegalIndexes(getColor());
    if (legalIndexes.isEmpty()) {
      this.move = Move.ofPass(getColor());
    } else {
      Board searchBoard = isBlack() ? this.board : this.board.flipped();
      this.move = null;
      transTable.clear(); // 探索ごとにキャッシュをクリア
      maxSearch(searchBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0, searchLimit);
      this.move = this.move.colored(getColor());
    }

    this.board = this.board.placed(this.move);
    return this.move;
  }

  float maxSearch(Board board, float alpha, float beta, int depth, int searchLimit) {
    long hash = board.hashCode();
    if (transTable.containsKey(hash)) return transTable.get(hash);

    if (board.isEnd() || depth >= searchLimit) {
      float v = this.eval.value(board);
      transTable.put(hash, v);
      return v;
    }

    var moves = board.findLegalMoves(BLACK);
    if (moves.isEmpty()) {
      float v = minSearch(board, alpha, beta, depth + 1, searchLimit);
      transTable.put(hash, v);
      return v;
    }

    List<MoveScore> moveScores = new ArrayList<>();
    for (var move : moves) {
      var newBoard = board.placed(move);
      float score = eval.value(newBoard);
      moveScores.add(new MoveScore(move, score, newBoard));
    }
    moveScores.sort((a, b) -> Float.compare(b.score, a.score));

    if (depth == 0)
      this.move = moveScores.get(0).move;

    float best = alpha;
    for (var ms : moveScores) {
      float v = minSearch(ms.board, best, beta, depth + 1, searchLimit);

      if (v > best) {
        best = v;
        if (depth == 0)
          this.move = ms.move;
      }

      if (best >= beta)
        break;
    }
    transTable.put(hash, best);
    return best;
  }

  float minSearch(Board board, float alpha, float beta, int depth, int searchLimit) {
    long hash = board.hashCode() ^ 0x7FFFFFFFL; // 白番用にハッシュをずらす
    if (transTable.containsKey(hash)) return transTable.get(hash);

    if (board.isEnd() || depth >= searchLimit) {
      float v = this.eval.value(board);
      transTable.put(hash, v);
      return v;
    }

    var moves = board.findLegalMoves(WHITE);
    if (moves.isEmpty()) {
      float v = maxSearch(board, alpha, beta, depth + 1, searchLimit);
      transTable.put(hash, v);
      return v;
    }

    List<MoveScore> moveScores = new ArrayList<>();
    for (var move : moves) {
      var newBoard = board.placed(move);
      float score = eval.value(newBoard);
      moveScores.add(new MoveScore(move, score, newBoard));
    }
    moveScores.sort((a, b) -> Float.compare(a.score, b.score));

    float best = beta;
    for (var ms : moveScores) {
      float v = maxSearch(ms.board, alpha, best, depth + 1, searchLimit);
      if (v < best) {
        best = v;
      }
      if (alpha >= best) break;
    }
    transTable.put(hash, best);
    return best;
  }

  // Moveと評価値と新しい盤面をまとめるクラス
  static class MoveScore {
    Move move;
    float score;
    Board board;
    MoveScore(Move move, float score, Board board) {
      this.move = move;
      this.score = score;
      this.board = board;
    }
  }
}

package p25x01;

import static ap25.Board.LENGTH;
import static ap25.Board.SIZE;
import static ap25.Color.BLACK;
import static ap25.Color.WHITE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import ap25.Board;
import ap25.Color;
import ap25.Move;

class MyEval {
  static float[][] M = {
      { 10, 10, 10, 10, 10, 10 },
      { 10, -5, 1, 1, -5, 10 },
      { 10, 1, 1, 1, 1, 10 },
      { 10, 1, 1, 1, 1, 10 },
      { 10, -5, 1, 1, -5, 10 },
      { 10, 10, 10, 10, 10, 10 },
  };

  public float value(Board board) {
    if (board.isEnd())
      return 1000000 * board.score();

    return (float) IntStream.range(0, LENGTH)
        .mapToDouble(k -> score(board, k))
        .reduce(Double::sum).orElse(0);
  }

  float score(Board board, int k) {
    return M[k / SIZE][k % SIZE] * board.get(k).getValue();
  }
}

public class OurPlayer extends ap25.Player {
  static final String MY_NAME = "2400";
  MyEval eval;
  int depthLimit;
  Move move;
  OurBoard board;

  public OurPlayer(Color color) {
    this(MY_NAME, color, new MyEval(), 4);
  }

  public OurPlayer(String name, Color color, MyEval eval, int depthLimit) {
    super(name, color);
    this.eval = eval;
    this.depthLimit = depthLimit;
    this.board = new OurBoard();
  }

  public OurPlayer(String name, Color color, int depthLimit) {
    this(name, color, new MyEval(), depthLimit);
  }

  public void setBoard(Board board) {
    for (var i = 0; i < LENGTH; i++) {
      this.board.set(i, board.get(i));
    }
  }

  boolean isBlack() {
    return getColor() == BLACK;
  }

  public Move think(Board board) {
    this.board = this.board.placed(board.getMove());

    if (this.board.findNoPassLegalIndexes(getColor()).isEmpty()) {
      this.move = Move.ofPass(getColor());
    } else {
      var newBoard = isBlack() ? this.board.clone() : this.board.flipped();
      this.move = null;

      var legals = this.board.findNoPassLegalIndexes(getColor());

      maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);

      this.move = this.move.colored(getColor());

      if (legals.contains(this.move.getIndex()) == false) {
        System.out.println("**************");
        System.out.println(legals);
        System.out.println(this.move);
        System.out.println(this.move.getIndex());
        System.out.println(this.board);
        System.out.println(newBoard);
        System.exit(0);
        maxSearch(newBoard, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0);
      }
    }

    this.board = this.board.placed(this.move);
    return this.move;
  }

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

  boolean isTerminal(Board board, int depth) {
    return board.isEnd() || depth > this.depthLimit;
  }

  List<Move> order(List<Move> moves) {
    var shuffled = new ArrayList<Move>(moves);
    Collections.shuffle(shuffled);
    return shuffled;
  }
}

package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;
import java.util.stream.*;

/**
 * ゲーム進行を管理するクラス。
 * プレイヤー同士の対戦を制御し、勝敗や経過を出力する。
 */
public class MyGame {
  public static void main(String args[]) {
    // プレイヤーと盤面を生成し、ゲームを開始
    var player1 = new myplayer.MyPlayer(BLACK);
    var player2 = new myplayer.RandomPlayer(WHITE);
    var board = new MyBoard();
    var game = new MyGame(board, player1, player2);
    game.play();
  }

  static final float TIME_LIMIT_SECONDS = 60; // 持ち時間（秒）

  Board board;                        // 現在の盤面
  Player black;                       // 黒プレイヤー
  Player white;                       // 白プレイヤー
  Map<Color, Player> players;         // 色→プレイヤーのマップ
  List<Move> moves = new ArrayList<>(); // 着手履歴
  Map<Color, Float> times = new HashMap<>(Map.of(BLACK, 0f, WHITE, 0f)); // 各プレイヤーの消費時間

  /**
   * コンストラクタ
   * @param board 初期盤面
   * @param black 黒プレイヤー
   * @param white 白プレイヤー
   */
  public MyGame(Board board, Player black, Player white) {
    this.board = board.clone();
    this.black = black;
    this.white = white;
    this.players = Map.of(BLACK, black, WHITE, white);
  }

  /**
   * ゲームを進行し、結果を出力する
   */
  public void play() {
    // 各プレイヤーに盤面をセット
    this.players.values().forEach(p -> p.setBoard(this.board.clone()));

    while (this.board.isEnd() == false) {
      var turn = this.board.getTurn();
      var player = this.players.get(turn);

      Error error = null;
      long t0 = System.currentTimeMillis();
      Move move;

      // プレイヤーに手を考えさせる
      try {
        move = player.think(board.clone()).colored(turn);
      } catch (Error e) {
        error = e;
        move = Move.ofError(turn);
      }

      // 時間計測
      long t1 = System.currentTimeMillis();
      final var t = (float) Math.max(t1 - t0, 1) / 1000.f;
      this.times.compute(turn, (k, v) -> v + t);

      // 合法性・タイムアウト等のチェック
      move = check(turn, move, error);
      moves.add(move);

      // 盤面更新
      if (move.isLegal()) {
        board = board.placed(move);
      } else {
        board.foul(turn);
        break;
      }

      System.out.println(board);
    }

    printResult(board, moves);
  }

  /**
   * 手の合法性やタイムアウト等を判定
   * @param turn 手番
   * @param move プレイヤーの手
   * @param error エラー情報
   * @return 許容される手
   */
  Move check(Color turn, Move move, Error error) {
    if (move.isError()) {
      System.err.printf("error: %s %s", turn, error);
      System.err.println(board);
      return move;
    }

    if (this.times.get(turn) > TIME_LIMIT_SECONDS) {
      System.err.printf("timeout: %s %.2f", turn, this.times.get(turn));
      System.err.println(board);
      return Move.ofTimeout(turn);
    }

    var legals = board.findLegalMoves(turn);
    if (move == null || legals.contains(move) == false) {
      System.err.printf("illegal move: %s %s", turn, move);
      System.err.println(board);
      return Move.ofIllegal(turn);
    }

    return move;
  }

  /**
   * 勝者プレイヤーを取得
   * @param board 盤面
   * @return 勝者プレイヤー
   */
  public Player getWinner(Board board) {
    return this.players.get(board.winner());
  }

  /**
   * ゲーム結果を出力
   * @param board 最終盤面
   * @param moves 着手履歴
   */
  public void printResult(Board board, List<Move> moves) {
    var result = String.format("%5s%-9s", "", "draw");
    var score = Math.abs(board.score());
    if (score > 0)
      result = String.format("%-4s won by %-2d", getWinner(board), score);

    var s = toString() + " -> " + result + "\t| " + toString(moves);
    System.out.println(s);
  }

  /**
   * プレイヤー情報を文字列化
   */
  public String toString() {
    return String.format("%4s vs %4s", this.black, this.white);
  }

  /**
   * 着手履歴を文字列化
   * @param moves 着手リスト
   * @return 文字列
   */
  public static String toString(List<Move> moves) {
    return moves.stream().map(x -> x.toString()).collect(Collectors.joining());
  }

  /**
   * 着手履歴を取得
   */
  public List<Move> getMoves() {
    return this.moves;
  }
}

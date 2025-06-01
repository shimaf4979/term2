package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;
import java.util.stream.*;

/**
 * オセロゲームの進行を管理するメインクラス
 * プレイヤー間のターン制御、時間制限管理、結果表示を担当
 * ゲームルールの適用とプレイヤーの手の検証も行う
 */
public class MyGame {
  /**
   * メインメソッド
   * デフォルトでMyPlayer（黒）とRandomPlayer（白）でゲームを実行
   * 
   * @param args コマンドライン引数（使用しない）
   */
  public static void main(String args[]) {
    var player1 = new myplayer.MyPlayer(BLACK);
    var player2 = new myplayer.RandomPlayer(WHITE);
    var board = new MyBoard();
    var game = new MyGame(board, player1, player2);
    game.play();
  }

  /**
   * 1プレイヤーあたりの制限時間（秒）
   * この時間を超過するとタイムアウト負けとなる
   */
  static final float TIME_LIMIT_SECONDS = 60;

  /**
   * ゲームで使用するボード
   */
  Board board;

  /**
   * 黒プレイヤー
   */
  Player black;

  /**
   * 白プレイヤー
   */
  Player white;

  /**
   * プレイヤーのマップ（色 -> プレイヤー）
   * 現在のターンから対応するプレイヤーを取得するために使用
   */
  Map<Color, Player> players;

  /**
   * 打たれた手の履歴
   * ゲーム終了後の表示やデバッグに使用
   */
  List<Move> moves = new ArrayList<>();

  /**
   * 各プレイヤーの使用時間（秒）
   * タイムアウト判定に使用
   */
  Map<Color, Float> times = new HashMap<>(Map.of(BLACK, 0f, WHITE, 0f));

  /**
   * コンストラクタ
   * ゲームに必要な要素を初期化し、プレイヤーマップを作成
   * 
   * @param board ゲームで使用するボード
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
   * ゲームをプレイ（メインループ）
   * ターン制でプレイヤーに手を考えさせ、合法性チェック後にボードを更新
   * ゲーム終了まで継続し、最後に結果を表示
   */
  public void play() {
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

      // 使用時間を記録
      long t1 = System.currentTimeMillis();
      final var t = (float) Math.max(t1 - t0, 1) / 1000.f;
      this.times.compute(turn, (k, v) -> v + t);

      // 手の合法性などをチェック
      move = check(turn, move, error);
      moves.add(move);

      // ボードを更新
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
   * プレイヤーの手をチェック
   * エラー、タイムアウト、違法手をチェックし適切なMoveオブジェクトを返す
   * 
   * @param turn  現在のターンの色
   * @param move  プレイヤーが選択した手
   * @param error 思考中に発生したエラー
   * @return チェック済みの手
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
   * ゲームの勝者を取得
   * 
   * @param board 最終ボード状態
   * @return 勝利したプレイヤー
   */
  public Player getWinner(Board board) {
    return this.players.get(board.winner());
  }

  /**
   * ゲーム結果を表示
   * 勝者、スコア差、手の履歴を整形して出力
   * 
   * @param board 最終ボード状態
   * @param moves 打たれた手の履歴
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
   * ゲームの文字列表現を取得
   * プレイヤー名の対戦形式で表示
   * 
   * @return "黒プレイヤー vs 白プレイヤー" の形式
   */
  public String toString() {
    return String.format("%4s vs %4s", this.black, this.white);
  }

  /**
   * 手のリストを文字列に変換
   * 全ての手を連結した文字列を作成
   * 
   * @param moves 手のリスト
   * @return 連結された手の文字列
   */
  public static String toString(List<Move> moves) {
    return moves.stream().map(x -> x.toString()).collect(Collectors.joining());
  }
}
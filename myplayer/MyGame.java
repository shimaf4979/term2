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
    var player1 = new MyPlayer(BLACK);
    var player2 = new myplayer.RandomPlayer(WHITE);
    var board = new MyBoard();
    var game = new MyGame(board, player1, player2);
    game.play();
  }

  /**
   * 1プレイヤーあたりの制限時間（秒）
   * この時間を超過するとタイムアウト負けとなる
   */
  static final float TIME_LIMIT_SECONDS = 600;

  /**
   * ゲームで使用するボード
   */
  Board board;

  /**
   * 黒プレイヤー（先手）
   */
  Player black;

  /**
   * 白プレイヤー（後手）
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
   * @param black 黒プレイヤー（先手）
   * @param white 白プレイヤー（後手）
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

    // ゲーム開始情報の表示
    System.out.println("=== ゲーム開始 ===");
    System.out.printf("先手（黒・o）: %s\n", black.toString());
    System.out.printf("後手（白・x）: %s\n", white.toString());
    System.out.println();

    while (this.board.isEnd() == false) {
      var turn = this.board.getTurn();
      var player = this.players.get(turn);

      // 現在のターン表示（AIの場合）
      if (!(player instanceof HumanPlayer)) {
        String turnName = (turn == BLACK) ? "先手" : "後手";
        String stoneName = (turn == BLACK) ? "黒(o)" : "白(x)";
        System.out.printf("\n=== %s %s %sの番 ===\n",
            player.toString(), stoneName, turnName);
        System.out.println("思考中...");
      }

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

      // AIの手を表示
      if (!(player instanceof HumanPlayer) && move.isLegal()) {
        System.out.printf("%sが選択: %s (思考時間: %.2f秒)\n",
            player.toString(), move, t);
      }

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

      // AIの後は盤面を表示
      if (!(player instanceof HumanPlayer)) {
        System.out.println(board);
      }
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
      System.err.printf("エラー: %s %s\n", turn, error);
      System.err.println(board);
      return move;
    }

    if (this.times.get(turn) > TIME_LIMIT_SECONDS) {
      System.err.printf("タイムアウト: %s %.2f秒\n", turn, this.times.get(turn));
      System.err.println(board);
      return Move.ofTimeout(turn);
    }

    var legals = board.findLegalMoves(turn);
    if (move == null || legals.contains(move) == false) {
      System.err.printf("違法手: %s %s\n", turn, move);
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
    System.out.println("\n" + "=".repeat(60));
    System.out.println("                    ゲーム終了");
    System.out.println("=".repeat(60));

    var score = board.score();
    var blackCount = board.count(BLACK);
    var whiteCount = board.count(WHITE);

    System.out.printf("最終スコア: 黒(o)先手 %d - %d 白(x)後手\n", blackCount, whiteCount);
    System.out.printf("石差: %d\n", Math.abs(score));

    String result;
    if (score == 0) {
      result = "引き分け";
    } else {
      Player winner = getWinner(board);
      String winnerRole = (board.winner() == BLACK) ? "先手" : "後手";
      result = String.format("%s (%s) の勝利", winner, winnerRole);
    }

    System.out.println("結果: " + result);

    // 使用時間表示
    System.out.printf("\n使用時間:\n");
    System.out.printf("  %s (先手・黒): %.2f秒\n", black.toString(), times.get(BLACK));
    System.out.printf("  %s (後手・白): %.2f秒\n", white.toString(), times.get(WHITE));

    System.out.println("\n手順: " + toString(moves));
    System.out.println("=".repeat(60));
  }

  /**
   * ゲームの文字列表現を取得
   * プレイヤー名の対戦形式で表示
   * 
   * @return "黒プレイヤー(先手) vs 白プレイヤー(後手)" の形式
   */
  public String toString() {
    return String.format("%s(先手) vs %s(後手)", this.black, this.white);
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
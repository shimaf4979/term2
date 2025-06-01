package myplayer;

import static ap25.Color.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ap25.*;

/**
 * 6x6のオセロボードを表現し、ゲームの状態管理を行うクラス
 * Boardインターフェースを実装し、石の配置、合法手の判定、ゲーム終了判定などを提供
 * ボードの状態は36個の要素を持つColor配列で管理され、各要素はBLACK, WHITE, NONE, BLOCKのいずれか
 */
public class MyBoard implements Board, Cloneable {
  /**
   * ボードの状態を表す配列（36個の要素）
   * インデックス k = row * SIZE + col で位置を表現
   * 各要素はColor.BLACK, WHITE, NONE, BLOCKのいずれか
   */
  Color board[];

  /**
   * 最後に打たれた手を記録
   * 初期状態ではMove.ofPass(NONE)で初期化
   */
  Move move = Move.ofPass(NONE);

  /**
   * デフォルトコンストラクタ
   * 全てのマスをNONEで初期化し、その後init()で初期配置を設定
   */
  public MyBoard() {
    this.board = Stream.generate(() -> NONE).limit(LENGTH).toArray(Color[]::new);
    init();
  }

  /**
   * 内部コンストラクタ（配列とムーブを指定）
   * 
   * @param board コピー元のボード配列
   * @param move  最後の手
   */
  MyBoard(Color board[], Move move) {
    this.board = Arrays.copyOf(board, board.length);
    this.move = move;
  }

  /**
   * ボードのディープコピーを作成
   * 
   * @return 新しいMyBoardインスタンス
   */
  public MyBoard clone() {
    return new MyBoard(this.board, this.move);
  }

  /**
   * ボードを初期状態に設定
   * オセロの標準初期配置：c3, d4に黒石、d3, c4に白石を配置
   */
  void init() {
    set(Move.parseIndex("c3"), BLACK);
    set(Move.parseIndex("d4"), BLACK);
    set(Move.parseIndex("d3"), WHITE);
    set(Move.parseIndex("c4"), WHITE);
  }

  /**
   * 指定された位置の石の色を取得
   * 
   * @param k ボード上の位置（0-35）
   * @return その位置にある石の色
   */
  public Color get(int k) {
    return this.board[k];
  }

  /**
   * 最後に打たれた手を取得
   * 
   * @return 最後の手
   */
  public Move getMove() {
    return this.move;
  }

  /**
   * 現在のターンのプレイヤーの色を取得
   * 最後の手の相手の色が次のターン
   * 
   * @return 現在のターンの色
   */
  public Color getTurn() {
    return this.move.isNone() ? BLACK : this.move.getColor().flipped();
  }

  /**
   * 指定された位置に石を配置
   * 
   * @param k     ボード上の位置（0-35）
   * @param color 配置する石の色
   */
  public void set(int k, Color color) {
    this.board[k] = color;
  }

  /**
   * 他のMyBoardオブジェクトとの等価性を判定
   * 
   * @param otherObj 比較対象のオブジェクト
   * @return ボードの状態が同じ場合true
   */
  public boolean equals(Object otherObj) {
    if (otherObj instanceof MyBoard) {
      var other = (MyBoard) otherObj;
      return Arrays.equals(this.board, other.board);
    }
    return false;
  }

  /**
   * ボードの文字列表現を取得
   * MyBoardFormatterを使用してフォーマット
   * 
   * @return ボードの表示用文字列
   */
  public String toString() {
    return MyBoardFormatter.format(this);
  }

  /**
   * 指定された色の石の数を数える
   * 
   * @param color 数える石の色
   * @return その色の石の数
   */
  public int count(Color color) {
    return countAll().getOrDefault(color, 0L).intValue();
  }

  /**
   * ゲームが終了しているかを判定
   * 黒白両方のプレイヤーが打てる手がない場合にゲーム終了
   * 
   * @return 両プレイヤーとも打てる手がない場合true
   */
  public boolean isEnd() {
    var lbs = findNoPassLegalIndexes(BLACK);
    var lws = findNoPassLegalIndexes(WHITE);
    return lbs.size() == 0 && lws.size() == 0;
  }

  /**
   * ゲームの勝者を判定
   * スコアが正の場合は黒の勝ち、負の場合は白の勝ち、0の場合は引き分け
   * 
   * @return 勝者の色、引き分けの場合はNONE
   */
  public Color winner() {
    var v = score();
    if (isEnd() == false || v == 0)
      return NONE;
    return v > 0 ? BLACK : WHITE;
  }

  /**
   * 指定した色のプレイヤーに反則を適用
   * ボード全体を相手の色で埋める
   * 
   * @param color 反則したプレイヤーの色
   */
  public void foul(Color color) {
    var winner = color.flipped();
    IntStream.range(0, LENGTH).forEach(k -> this.board[k] = winner);
  }

  /**
   * 現在のスコアを計算
   * 黒石数 - 白石数で計算、完全勝利の場合は空きマス分もボーナス
   * 
   * @return スコア（正の値は黒有利、負の値は白有利）
   */
  public int score() {
    var cs = countAll();
    var bs = cs.getOrDefault(BLACK, 0L);
    var ws = cs.getOrDefault(WHITE, 0L);
    var ns = LENGTH - bs - ws;
    int score = (int) (bs - ws);

    if (bs == 0 || ws == 0)
      score += Integer.signum(score) * ns;

    return score;
  }

  /**
   * ボード上の全ての色の石を数える
   * 
   * @return 色ごとの石の数のマップ
   */
  Map<Color, Long> countAll() {
    return Arrays.stream(this.board).collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  /**
   * 指定した色の合法手をすべて見つける
   * パスも含む完全な合法手のリストを返す
   * 
   * @param color 手を探すプレイヤーの色
   * @return 合法手のリスト（パスも含む）
   */
  public List<Move> findLegalMoves(Color color) {
    return findLegalIndexes(color).stream()
        .map(k -> new Move(k, color)).toList();
  }

  /**
   * 指定した色の合法手の位置インデックスを取得
   * パスが必要な場合はPASSを追加
   * 
   * @param color 手を探すプレイヤーの色
   * @return 合法手の位置インデックスのリスト
   */
  List<Integer> findLegalIndexes(Color color) {
    var moves = findNoPassLegalIndexes(color);
    if (moves.size() == 0)
      moves.add(Move.PASS);
    return moves;
  }

  /**
   * 指定した色のパス以外の合法手を見つける
   * 実際に石を置ける位置のみを返す
   * 
   * @param color 手を探すプレイヤーの色
   * @return パス以外の合法手の位置インデックスのリスト
   */
  List<Integer> findNoPassLegalIndexes(Color color) {
    var moves = new ArrayList<Integer>();
    for (int k = 0; k < LENGTH; k++) {
      var c = this.board[k];
      if (c != NONE)
        continue;
      for (var line : lines(k)) {
        var outflanking = outflanked(line, color);
        if (outflanking.size() > 0)
          moves.add(k);
      }
    }
    return moves;
  }

  /**
   * 指定した位置から8方向の直線を取得
   * 
   * @param k 起点となる位置
   * @return 8方向の直線のリスト
   */
  List<List<Integer>> lines(int k) {
    var lines = new ArrayList<List<Integer>>();
    for (int dir = 0; dir < 8; dir++) {
      var line = Move.line(k, dir);
      lines.add(line);
    }
    return lines;
  }

  /**
   * 指定した方向の直線上で挟める石を取得
   * 相手の石が連続して並び、最後に自分の石がある場合に挟める
   * 
   * @param line  調べる直線上の位置のリスト
   * @param color 配置する石の色
   * @return 挟める石のリスト
   */
  List<Move> outflanked(List<Integer> line, Color color) {
    if (line.size() <= 1)
      return new ArrayList<Move>();
    var flippables = new ArrayList<Move>();
    for (int k : line) {
      var c = get(k);
      if (c == NONE || c == BLOCK)
        break;
      if (c == color)
        return flippables;
      flippables.add(new Move(k, color));
    }
    return new ArrayList<Move>();
  }

  /**
   * 指定した手を実行した後の新しいボード状態を返す
   * 元のボードは変更せず、新しいインスタンスを返す
   * 
   * @param move 実行する手
   * @return 手を実行した後の新しいボード
   */
  public MyBoard placed(Move move) {
    var b = clone();
    b.move = move;

    if (move.isPass() | move.isNone())
      return b;

    var k = move.getIndex();
    var color = move.getColor();
    var lines = b.lines(k);
    for (var line : lines) {
      for (var p : outflanked(line, color)) {
        b.board[p.getIndex()] = color;
      }
    }
    b.set(k, color);

    return b;
  }

  /**
   * ボードの色を反転した新しいボードを返す
   * 黒白を入れ替えた状態のボードを作成
   * 
   * @return 色が反転された新しいボード
   */
  public MyBoard flipped() {
    var b = clone();
    IntStream.range(0, LENGTH).forEach(k -> b.board[k] = b.board[k].flipped());
    b.move = this.move.flipped();
    return b;
  }
}
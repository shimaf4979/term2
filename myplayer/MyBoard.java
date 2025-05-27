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
 * オセロ盤面の実装クラス。
 * 盤面の状態管理や合法手判定、石の反転などのロジックを提供する。
 */
public class MyBoard implements Board, Cloneable {
  Color board[];                   // 盤面の状態を保持する配列
  Move move = Move.ofPass(NONE);   // 直前の手

  /**
   * デフォルトコンストラクタ（初期配置で盤面を生成）
   */
  public MyBoard() {
    this.board = Stream.generate(() -> NONE).limit(LENGTH).toArray(Color[]::new);
    init();
  }

  /**
   * コピーコンストラクタ
   * @param board 盤面配列
   * @param move 直前の手
   */
  MyBoard(Color board[], Move move) {
    this.board = Arrays.copyOf(board, board.length);
    this.move = move;
  }

  /**
   * 盤面のディープコピーを返す
   */
  public MyBoard clone() {
    return new MyBoard(this.board, this.move);
  }

  /**
   * 初期配置を設定
   */
  void init() {
    set(Move.parseIndex("c3"), BLACK);
    set(Move.parseIndex("d4"), BLACK);
    set(Move.parseIndex("d3"), WHITE);
    set(Move.parseIndex("c4"), WHITE);
  }

  /**
   * 指定位置の色を取得
   */
  public Color get(int k) { return this.board[k]; }

  /**
   * 直前の手を取得
   */
  public Move getMove() { return this.move; }

  /**
   * 現在の手番の色を取得
   */
  public Color getTurn() {
    return this.move.isNone() ? BLACK : this.move.getColor().flipped();
  }

  /**
   * 指定位置に色をセット
   */
  public void set(int k, Color color) {
    this.board[k] = color;
  }

  /**
   * 盤面同士の等価性判定
   */
  public boolean equals(Object otherObj) {
    if (otherObj instanceof MyBoard) {
      var other = (MyBoard) otherObj;
      return Arrays.equals(this.board, other.board);
    }
    return false;
  }

  /**
   * 盤面を文字列で出力
   */
  public String toString() {
    return MyBoardFormatter.format(this);
  }

  /**
   * 指定色の石の数をカウント
   */
  public int count(Color color) {
    return countAll().getOrDefault(color, 0L).intValue();
  }

  /**
   * ゲーム終了判定（両者合法手なし）
   */
  public boolean isEnd() {
    var lbs = findNoPassLegalIndexes(BLACK);
    var lws = findNoPassLegalIndexes(WHITE);
    return lbs.size() == 0 && lws.size() == 0;
  }

  /**
   * 勝者の色を返す（引き分けや未終了時はNONE）
   */
  public Color winner() {
    var v = score();
    if (isEnd() == false || v == 0 ) return NONE;
    return v > 0 ? BLACK : WHITE;
  }

  /**
   * 反則時の処理（反則した色以外で全マスを埋める）
   */
  public void foul(Color color) {
    var winner = color.flipped();
    IntStream.range(0, LENGTH).forEach(k -> this.board[k] = winner);
  }

  /**
   * スコア計算（黒-白、全滅時は残りマス分加算）
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
   * 全色の石の数をMapで返す
   */
  Map<Color, Long> countAll() {
    return Arrays.stream(this.board).collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  /**
   * 指定色の合法手リストを返す
   */
  public List<Move> findLegalMoves(Color color) {
    return findLegalIndexes(color).stream()
        .map(k -> new Move(k, color)).toList();
  }

  /**
   * 指定色の合法手インデックスリストを返す（パスも含む）
   */
  List<Integer> findLegalIndexes(Color color) {
    var moves = findNoPassLegalIndexes(color);
    if (moves.size() == 0) moves.add(Move.PASS);
    return moves;
  }

  /**
   * 指定色の合法手インデックスリスト（パス除く）
   */
  List<Integer> findNoPassLegalIndexes(Color color) {
    var moves = new ArrayList<Integer>();
    for (int k = 0; k < LENGTH; k++) {
      var c = this.board[k];
      if (c != NONE) continue;
      for (var line : lines(k)) {
        var outflanking = outflanked(line, color);
        if (outflanking.size() > 0) moves.add(k);
      }
    }
    return moves;
  }

  /**
   * 指定位置から8方向のラインリストを返す
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
   * 指定ラインでひっくり返せる石のリストを返す
   */
  List<Move> outflanked(List<Integer> line, Color color) {
    if (line.size() <= 1) return new ArrayList<Move>();
    var flippables = new ArrayList<Move>();
    for (int k: line) {
      var c = get(k);
      if (c == NONE || c == BLOCK) break;
      if (c == color) return flippables;
      flippables.add(new Move(k, color));
    }
    return new ArrayList<Move>();
  }

  /**
   * 指定手を打った後の新しい盤面を返す
   */
  public MyBoard placed(Move move) {
    var b = clone();
    b.move = move;

    if (move.isPass() | move.isNone())
      return b;

    var k = move.getIndex();
    var color = move.getColor();
    var lines = b.lines(k);
    for (var line: lines) {
      for (var p: outflanked(line, color)) {
        b.board[p.getIndex()] = color;
      }
    }
    b.set(k, color);

    return b;
  }

  /**
   * 盤面を反転した新しい盤面を返す
   */
  public MyBoard flipped() {
    var b = clone();
    IntStream.range(0, LENGTH).forEach(k -> b.board[k] = b.board[k].flipped());
    b.move = this.move.flipped();
    return b;
  }
}

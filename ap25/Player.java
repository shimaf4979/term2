package ap25;

/**
 * プレイヤーの抽象クラス。
 * プレイヤー名・色・盤面情報を保持し、思考メソッドを定義する。
 */
public abstract class Player {
  String name;    // プレイヤー名
  Color color;    // プレイヤーの色
  Board board;    // 現在の盤面

  /**
   * コンストラクタ
   * @param name プレイヤー名
   * @param color プレイヤーの色
   */
  public Player(String name, Color color) {
    this.name = name;
    this.color = color;
  }

  /**
   * 盤面を設定する
   * @param board 現在の盤面
   */
  public void setBoard(Board board) { this.board = board; }

  /**
   * プレイヤーの色を取得する
   * @return プレイヤーの色
   */
  public Color getColor() { return this.color; }

  /**
   * プレイヤー名を返す
   * @return プレイヤー名
   */
  public String toString() { return this.name; }

  /**
   * 着手を決定する（サブクラスで実装）
   * @param board 現在の盤面
   * @return 着手
   */
  public Move think(Board board) { return null; }
}

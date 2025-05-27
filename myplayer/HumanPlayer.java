package myplayer;

import ap25.*;
import java.util.List;
import java.util.Scanner;

/**
 * 標準入力から手を入力して対戦に参加する人間用プレイヤークラス。
 */
public class HumanPlayer extends Player {
  private final Scanner scanner = new Scanner(System.in);

  /**
   * コンストラクタ
   * @param color プレイヤーの色
   */
  public HumanPlayer(Color color) {
    super("Human", color);
  }

  /**
   * 標準入力から合法手を選択して返す
   * @param board 現在の盤面
   * @return 選択した手
   */
  @Override
  public Move think(Board board) {
    List<Move> legalMoves = board.findLegalMoves(getColor());
    System.out.println(board);
    System.out.println("あなた(" + getColor() + ")の番です。合法手: " + Move.toStringList(
      legalMoves.stream().map(Move::getIndex).toList()));
    while (true) {
      System.out.print("手を入力してください（例: a1, パスは .. ）: ");
      String input = scanner.nextLine().trim();
      // パスの場合
      if (input.equals("..")) {
        for (Move m : legalMoves) {
          if (m.isPass()) return m;
        }
        System.out.println("パスはできません。");
        continue;
      }
      // 合法手か判定
      for (Move m : legalMoves) {
        if (input.equals(Move.toIndexString(m.getIndex()))) {
          return m;
        }
      }
      System.out.println("不正な入力です。もう一度入力してください。");
    }
  }
}
package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;

/**
 * 指定回数だけ先手・後手を交互に入れ替えて対戦し、勝率を出力するクラス。
 */
public class MyGame2 {
  public static void main(String[] args) {
    int games = 100; // デフォルトの対戦回数
    if (args.length > 0) {
      try {
        games = Integer.parseInt(args[0]);
      } catch (Exception e) {
        System.err.println("Usage: java myplayer.MyGame2 [games]");
        System.exit(1);
      }
    }
    // プレイヤーを用意（ここではMyPlayerとRandomPlayerを例示）
    Player p1 = new MyPlayer(BLACK);
    Player p2 = new RandomPlayer(WHITE);

    int p1Win = 0, p2Win = 0, draw = 0;
    int p1Stones = 0, p2Stones = 0; // 平均枚数用
    for (int i = 0; i < games; i++) {
      Player black, white;
      if (i % 2 == 0) {
        black = p1;
        white = p2;
      } else {
        black = p2;
        white = p1;
      }

      Board board = new MyBoard();
      MyGame game = new MyGame(board, black, white);
      game.play();

      Color winner = game.board.winner();
      int blackStones = game.board.count(BLACK);
      int whiteStones = game.board.count(WHITE);

      // p1/p2の石数を記録
      if (black == p1) {
        p1Stones += blackStones;
        p2Stones += whiteStones;
      } else {
        p1Stones += whiteStones;
        p2Stones += blackStones;
      }

      if (winner == BLACK) {
        if (black == p1) p1Win++; else p2Win++;
      } else if (winner == WHITE) {
        if (white == p1) p1Win++; else p2Win++;
      } else {
        draw++;
      }
    }
    System.out.printf("総対局数: %d\n", games);
    System.out.printf("p1勝ち: %d (%.2f%%)\n", p1Win, 100.0 * p1Win / games);
    System.out.printf("p2勝ち: %d (%.2f%%)\n", p2Win, 100.0 * p2Win / games);
    System.out.printf("引き分け: %d (%.2f%%)\n", draw, 100.0 * draw / games);
    System.out.printf("p1平均石数: %.2f\n", (double)p1Stones / games);
    System.out.printf("p2平均石数: %.2f\n", (double)p2Stones / games);
  }
}
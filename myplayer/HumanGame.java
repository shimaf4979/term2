package myplayer;

import static ap25.Color.*;

public class HumanGame {
    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("             オセロゲーム");
        System.out.println("=".repeat(50));
        System.out.println("あなた vs MyPlayer");
        System.out.println();
        System.out.println("【操作方法】");
        System.out.println("・座標入力: a1, b3, f6 など");
        System.out.println("・番号選択: 表示される合法手の番号");
        System.out.println("・パス: .. または pass");
        System.out.println();
        System.out.println("【盤面の見方】");
        System.out.println("・o : 黒石（あなた）");
        System.out.println("・x : 白石（AI）");
        System.out.println("・. : 置ける位置");
        System.out.println("・  : 空の位置");
        System.out.println();

        // プレイヤーの色を選択
        var humanPlayer = new HumanPlayer(BLACK); // 人間は黒
        var aiPlayer = new MyPlayer(WHITE); // AIは白

        var board = new MyBoard();
        var game = new MyGame(board, humanPlayer, aiPlayer);

        System.out.println("ゲーム開始！");
        System.out.println("あなたは黒石(o)、先手です");

        game.play();
    }
}
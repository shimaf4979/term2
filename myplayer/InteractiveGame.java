package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.Scanner;

public class InteractiveGame {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=".repeat(60));
        System.out.println("                    6x6オセロゲーム");
        System.out.println("=".repeat(60));

        // ゲームモード選択
        System.out.println("【ゲームモード選択】");
        System.out.println("1. あなた vs AI (MyPlayer)");
        System.out.println("2. あなた vs ランダムAI");
        System.out.println("3. あなた vs あなた（二人プレイ）");
        System.out.print("ゲームモードを選択してください (1-3): ");

        int mode = getValidChoice(scanner, 1, 3);

        // 先手後手選択（二人プレイ以外）
        boolean humanFirst = true;
        String opponentName = "";

        if (mode != 3) {
            System.out.println("\n【先手・後手選択】");
            System.out.println("オセロでは黒石が先手、白石が後手です");
            System.out.println("1. あなたが先手（黒石）- AIが後手（白石）");
            System.out.println("2. AIが先手（黒石）- あなたが後手（白石）");
            System.out.print("選択してください (1-2): ");

            int turnChoice = getValidChoice(scanner, 1, 2);
            humanFirst = (turnChoice == 1);

            opponentName = (mode == 1) ? "MyPlayer" : "ランダムAI";
        }

        // プレイヤー作成
        Player player1, player2;
        String gameDescription = "";

        if (mode == 3) {
            // 二人プレイ
            player1 = new HumanPlayer(BLACK, "プレイヤー1");
            player2 = new HumanPlayer(WHITE, "プレイヤー2");
            gameDescription = "プレイヤー1(黒・先手) vs プレイヤー2(白・後手)";
        } else if (humanFirst) {
            // 人間が先手
            player1 = new HumanPlayer(BLACK, "あなた");
            player2 = createAIPlayer(mode, WHITE);
            gameDescription = String.format("あなた(黒・先手) vs %s(白・後手)", opponentName);
        } else {
            // AIが先手
            player1 = createAIPlayer(mode, BLACK);
            player2 = new HumanPlayer(WHITE, "あなた");
            gameDescription = String.format("%s(黒・先手) vs あなた(白・後手)", opponentName);
        }

        // ゲーム説明表示
        System.out.println("\n" + "=".repeat(60));
        System.out.println("【ゲーム設定完了】");
        System.out.println("対戦: " + gameDescription);
        System.out.println();
        System.out.println("【操作方法】");
        System.out.println("・座標入力: a1, b3, f6 など（列a-f, 行1-6）");
        System.out.println("・番号選択: 表示される合法手の番号を入力");
        System.out.println("・パス: .. または pass");
        System.out.println();
        System.out.println("【盤面の見方】");
        System.out.println("・o : 黒石（先手）");
        System.out.println("・x : 白石（後手）");
        System.out.println("・. : 現在のプレイヤーが置ける位置");
        System.out.println("・  : 空の位置");
        System.out.println("=".repeat(60));

        System.out.println("ゲーム開始！");
        if (mode != 3) {
            if (humanFirst) {
                System.out.println("あなたの先手です。黒石(o)を置いてください。");
            } else {
                System.out.println("AIの先手です。AIが考えています...");
            }
        } else {
            System.out.println("プレイヤー1の先手です。黒石(o)を置いてください。");
        }
        System.out.println();

        var board = new MyBoard();
        var game = new MyGame(board, player1, player2);
        game.play();

        scanner.close();
    }

    /**
     * 有効な選択肢を取得するヘルパーメソッド
     */
    private static int getValidChoice(Scanner scanner, int min, int max) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.printf("無効な選択です。%dから%dの間で選択してください: ", min, max);
            } catch (NumberFormatException e) {
                System.out.printf("数値を入力してください (%d-%d): ", min, max);
            }
        }
    }

    /**
     * AIプレイヤーを作成するヘルパーメソッド
     */
    private static Player createAIPlayer(int mode, Color color) {
        switch (mode) {
            case 1:
                return new MyPlayer(color);
            case 2:
                return new RandomPlayer(color);
            default:
                throw new IllegalArgumentException("Invalid AI mode: " + mode);
        }
    }
}
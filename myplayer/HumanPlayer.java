package myplayer;

import ap25.*;
import java.util.Scanner;

public class HumanPlayer extends Player {
    private Scanner scanner;
    private String displayName;

    public HumanPlayer(Color color) {
        this(color, "Human");
    }

    public HumanPlayer(Color color, String displayName) {
        super("Human", color);
        this.displayName = displayName;
        this.scanner = new Scanner(System.in);
    }

    public Move think(Board board) {
        var moves = board.findLegalMoves(getColor());

        // 現在の盤面を表示
        displayBoard(board);

        // 合法手がパスのみの場合
        if (moves.size() == 1 && moves.get(0).isPass()) {
            System.out.println("\n" + displayName + "：合法手がありません。パスします。");
            System.out.println("Enterキーを押してください...");
            scanner.nextLine();
            return Move.ofPass(getColor());
        }

        // 合法手を表示
        System.out.println("\n=== " + displayName + "の番です ===");
        String colorName = (getColor() == ap25.Color.BLACK) ? "黒石(o)・先手" : "白石(x)・後手";
        System.out.println("プレイヤー: " + displayName + " (" + colorName + ")");
        System.out.println("合法手: ");
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            if (move.isPass()) {
                System.out.println("  " + (i + 1) + ": パス (..)");
            } else {
                System.out.print("  " + (i + 1) + ": " + move.toString());
            }
        }
        System.out.println("");

        // ユーザーの入力を受け取る
        while (true) {
            System.out.print("手を選択してください (番号 または 座標例:a1): ");
            String input = scanner.nextLine().trim();

            try {
                // 番号で選択する場合
                if (input.matches("\\d+")) {
                    int choice = Integer.parseInt(input);
                    if (choice >= 1 && choice <= moves.size()) {
                        Move selectedMove = moves.get(choice - 1);
                        System.out.println(displayName + "の選択: " + selectedMove);
                        return selectedMove;
                    } else {
                        System.out.println("無効な番号です。1から" + moves.size() + "の間で選択してください。");
                        continue;
                    }
                }

                // 座標で選択する場合（例: a1, b3）
                if (input.length() == 2 &&
                        input.charAt(0) >= 'a' && input.charAt(0) <= 'f' &&
                        input.charAt(1) >= '1' && input.charAt(1) <= '6') {

                    Move attemptedMove = Move.of(input, getColor());

                    // 合法手かチェック
                    for (Move legalMove : moves) {
                        if (legalMove.equals(attemptedMove)) {
                            System.out.println(displayName + "の選択: " + attemptedMove);
                            return attemptedMove;
                        }
                    }
                    System.out.println("その位置には置けません。合法手から選択してください。");
                    continue;
                }

                // パスの場合
                if (input.equals("..") || input.equalsIgnoreCase("pass")) {
                    for (Move move : moves) {
                        if (move.isPass()) {
                            System.out.println(displayName + "の選択: パス");
                            return move;
                        }
                    }
                    System.out.println("パスはできません。");
                    continue;
                }

                System.out.println("無効な入力です。番号(1-" + moves.size() + ")または座標(a1-f6)で入力してください。");

            } catch (Exception e) {
                System.out.println("入力エラーです。もう一度入力してください。");
            }
        }
    }

    private void displayBoard(Board board) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("現在の盤面:");

        // 列のヘッダーを表示
        System.out.print("   ");
        for (char c = 'a'; c <= 'f'; c++) {
            System.out.print(" " + c + " ");
        }
        System.out.println();

        // 盤面を表示
        for (int row = 0; row < 6; row++) {
            System.out.print(" " + (row + 1) + " ");
            for (int col = 0; col < 6; col++) {
                int index = row * 6 + col;
                ap25.Color cellColor = board.get(index);
                String symbol;

                switch (cellColor) {
                    case BLACK:
                        symbol = " o ";
                        break;
                    case WHITE:
                        symbol = " x ";
                        break;
                    case NONE:
                        // 合法手の位置には . を表示
                        boolean isLegal = false;
                        var legalMoves = board.findLegalMoves(getColor());
                        for (var move : legalMoves) {
                            if (!move.isPass() && move.getIndex() == index) {
                                isLegal = true;
                                break;
                            }
                        }
                        symbol = isLegal ? " . " : "   ";
                        break;
                    default:
                        symbol = "   ";
                        break;
                }
                System.out.print(symbol);
            }
            System.out.println();
        }

        // 現在のスコア表示
        int blackCount = board.count(ap25.Color.BLACK);
        int whiteCount = board.count(ap25.Color.WHITE);
        System.out.println();
        System.out.println("スコア: 黒(o)先手 " + blackCount + " - " + whiteCount + " 白(x)後手");

        // 現在のターン表示
        Color currentTurn = board.getTurn();
        String turnInfo = (currentTurn == ap25.Color.BLACK) ? "黒(o)・先手" : "白(x)・後手";
        System.out.println("現在のターン: " + turnInfo);
        System.out.println("=".repeat(50));
    }
}
package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;

/**
 * 複数回のゲーム実行と統計情報の管理を行うクラス
 * プレイヤーの性能評価とベンチマークに使用
 * 大量のゲームを自動実行し、勝率や平均スコア差などの統計を提供
 */
public class GameRunner {
    /**
     * メインメソッド - コマンドライン引数でゲーム数を指定可能
     * デフォルトでは100回のゲームを実行
     * 
     * @param args コマンドライン引数（第1引数：ゲーム数）
     */
    public static void main(String[] args) {
        int numGames = 100; // デフォルトは100回

        // コマンドライン引数で回数を指定可能
        if (args.length > 0) {
            try {
                numGames = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("引数は数値で指定してください");
                return;
            }
        }

        runMultipleGames(numGames);
    }

    /**
     * 指定回数のゲームを実行し、統計を表示
     * 各ゲームでプレイヤーの色をランダムに決定し、公平性を保つ
     * 進捗表示と最終的な統計結果の表示を行う
     * 
     * @param numGames 実行するゲーム数
     */
    public static void runMultipleGames(int numGames) {
        Map<String, Integer> results = new HashMap<>();
        results.put("MY24", 0);
        results.put("Random", 0);
        results.put("Draw", 0);

        List<Integer> scores = new ArrayList<>();
        long totalTime = System.currentTimeMillis();

        System.out.println("=== " + numGames + "回のゲームを実行中... ===\n");

        for (int i = 1; i <= numGames; i++) {
            // プレイヤーの色をランダムに決定（公平性のため）
            Player player1, player2;
            if (Math.random() < 0.5) {
                player1 = new MyPlayer(BLACK);
                player2 = new RandomPlayer(WHITE);
            } else {
                player1 = new RandomPlayer(BLACK);
                player2 = new MyPlayer(WHITE);
            }

            var board = new MyBoard();
            var game = new SilentGame(board, player1, player2);
            var result = game.playQuiet();

            // 結果を記録
            String winner = result.winner;
            int score = result.score;

            results.put(winner, results.get(winner) + 1);
            scores.add(Math.abs(score));

            // 進捗表示
            if (i % 10 == 0 || i == numGames) {
                System.out.printf("進捗: %d/%d ゲーム完了\n", i, numGames);
            }
        }

        totalTime = System.currentTimeMillis() - totalTime;

        // 結果表示
        printResults(results, scores, numGames, totalTime);
    }

    /**
     * 統計結果を整形して表示
     * 勝利数、勝率、スコア統計、実行時間などを見やすく出力
     * 
     * @param results   勝利数の統計（プレイヤー名 -> 勝利数）
     * @param scores    スコア差のリスト
     * @param numGames  総ゲーム数
     * @param totalTime 実行時間（ミリ秒）
     */
    private static void printResults(Map<String, Integer> results, List<Integer> scores,
            int numGames, long totalTime) {
        System.out.println("\n=== 結果 ===");
        System.out.printf("総ゲーム数: %d\n", numGames);
        System.out.printf("実行時間: %.2f秒\n\n", totalTime / 1000.0);

        System.out.println("勝利数:");
        results.forEach((player, wins) -> {
            double percentage = (wins * 100.0) / numGames;
            System.out.printf("  %s: %d勝 (%.1f%%)\n", player, wins, percentage);
        });

        // 統計情報
        OptionalDouble avgScore = scores.stream().mapToInt(Integer::intValue).average();
        int maxScore = scores.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minScore = scores.stream().mapToInt(Integer::intValue).min().orElse(0);

        System.out.println("\nスコア統計:");
        System.out.printf("  平均差: %.1f\n", avgScore.orElse(0));
        System.out.printf("  最大差: %d\n", maxScore);
        System.out.printf("  最小差: %d\n", minScore);

        // MY24の勝率
        int myWins = results.get("MY24");
        double winRate = (myWins * 100.0) / numGames;
        System.out.printf("\nMY24の勝率: %.1f%%\n", winRate);
    }

    /**
     * ゲーム結果を格納するクラス
     * 1回のゲームの結果（勝者とスコア差）を保持
     */
    static class GameResult {
        /**
         * 勝者の名前（"MY24", "Random", "Draw"のいずれか）
         */
        String winner;

        /**
         * スコア差（絶対値）
         */
        int score;

        /**
         * コンストラクタ
         * 
         * @param winner 勝者名
         * @param score  スコア差
         */
        GameResult(String winner, int score) {
            this.winner = winner;
            this.score = score;
        }
    }

    /**
     * 静かに実行するゲームクラス（出力を抑制）
     * MyGameを継承し、ボード表示などの出力を行わずにゲームを実行
     * 大量のゲーム実行時に出力が邪魔にならないようにする
     */
    static class SilentGame extends MyGame {
        /**
         * コンストラクタ
         * 親クラスのコンストラクタを呼び出し
         * 
         * @param board ゲームボード
         * @param black 黒プレイヤー
         * @param white 白プレイヤー
         */
        public SilentGame(Board board, Player black, Player white) {
            super(board, black, white);
        }

        /**
         * 静かにゲームを実行
         * MyGame.play()と同様の処理を行うが、途中の出力を行わない
         * 最終的な結果のみをGameResultとして返す
         * 
         * @return ゲーム結果（勝者とスコア差）
         */
        public GameResult playQuiet() {
            this.players.values().forEach(p -> p.setBoard(this.board.clone()));

            while (this.board.isEnd() == false) {
                var turn = this.board.getTurn();
                var player = this.players.get(turn);

                Error error = null;
                long t0 = System.currentTimeMillis();
                Move move;

                try {
                    move = player.think(board.clone()).colored(turn);
                } catch (Error e) {
                    error = e;
                    move = Move.ofError(turn);
                }

                long t1 = System.currentTimeMillis();
                final var t = (float) Math.max(t1 - t0, 1) / 1000.f;
                this.times.compute(turn, (k, v) -> v + t);

                move = check(turn, move, error);
                moves.add(move);

                if (move.isLegal()) {
                    board = board.placed(move);
                } else {
                    board.foul(turn);
                    break;
                }
            }

            // 勝者を判定
            String winner;
            int score = board.score();

            if (score == 0) {
                winner = "Draw";
            } else {
                Color winnerColor = board.winner();
                Player winnerPlayer = this.players.get(winnerColor);
                if (winnerPlayer instanceof MyPlayer) {
                    winner = "MY24";
                } else {
                    winner = "Random";
                }
            }

            return new GameResult(winner, score);
        }
    }
}
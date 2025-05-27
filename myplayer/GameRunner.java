package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;

public class GameRunner {
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

    // ゲーム結果を格納するクラス
    static class GameResult {
        String winner;
        int score;

        GameResult(String winner, int score) {
            this.winner = winner;
            this.score = score;
        }
    }

    // 静かに実行するゲームクラス
    static class SilentGame extends MyGame {
        public SilentGame(Board board, Player black, Player white) {
            super(board, black, white);
        }

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
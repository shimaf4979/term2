package myplayer;

import ap25.*;
import static ap25.Color.*;
import java.util.*;

/**
 * 選択可能な対戦システム
 * 複数のAIプレイヤーから選択して対戦させることができる
 * プレイヤーの順番や対戦回数も設定可能
 */
public class TournamentRunner {
    /**
     * 利用可能なプレイヤータイプ
     */
    public enum PlayerType {
        MYPLAYER("MyPlayer", "最強AI - Ultimate 6x6"),
        MYPLAYER2("MyPlayer2", "中級AI - Alpha-Beta"),
        MYPLAYER3("MyPlayer3", "上級AI - NegaScout"),
        RANDOM("Random", "ランダムプレイヤー"),
        HUMAN("Human", "人間プレイヤー");

        private final String className;
        private final String description;

        PlayerType(String className, String description) {
            this.className = className;
            this.description = description;
        }

        public String getClassName() {
            return className;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * メインメソッド
     * コマンドライン引数で設定を指定可能
     * 
     * 使用例:
     * java TournamentRunner MyPlayer MyPlayer2 N=50
     * java TournamentRunner MyPlayer3 Random N=100 BlackFirst
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            showUsage();
            runInteractiveMode();
            return;
        }

        try {
            parseAndRun(args);
        } catch (Exception e) {
            System.err.println("エラー: " + e.getMessage());
            showUsage();
        }
    }

    /**
     * 使用方法の表示
     */
    private static void showUsage() {
        System.out.println("=".repeat(60));
        System.out.println("            オセロ対戦システム");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("【使用方法】");
        System.out.println("java TournamentRunner <Player1> <Player2> [オプション]");
        System.out.println();
        System.out.println("【利用可能なプレイヤー】");
        for (PlayerType type : PlayerType.values()) {
            System.out.printf("  %-10s : %s\n", type.name(), type.getDescription());
        }
        System.out.println();
        System.out.println("【オプション】");
        System.out.println("  N=<回数>     : 対戦回数（デフォルト: 10）");
        System.out.println("  BlackFirst   : Player1を常に黒にする");
        System.out.println("  WhiteFirst   : Player1を常に白にする");
        System.out.println("  Random       : ランダムに色を決定（デフォルト）");
        System.out.println();
        System.out.println("【使用例】");
        System.out.println("  java TournamentRunner MyPlayer MyPlayer2");
        System.out.println("  java TournamentRunner MyPlayer3 Random N=50");
        System.out.println("  java TournamentRunner MyPlayer MyPlayer2 N=100 BlackFirst");
        System.out.println();
    }

    /**
     * インタラクティブモード
     */
    private static void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("【インタラクティブモード】");

        // プレイヤー1選択
        PlayerType player1 = selectPlayer(scanner, "プレイヤー1");
        if (player1 == null)
            return;

        // プレイヤー2選択
        PlayerType player2 = selectPlayer(scanner, "プレイヤー2");
        if (player2 == null)
            return;

        // 対戦回数
        int numGames = selectGameCount(scanner);

        // 色の設定
        ColorSetting colorSetting = selectColorSetting(scanner);

        // 実行
        System.out.println();
        System.out.println("=== 対戦開始 ===");
        runTournament(player1, player2, numGames, colorSetting);

        scanner.close();
    }

    /**
     * コマンドライン引数の解析と実行
     */
    private static void parseAndRun(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("プレイヤーを2つ指定してください");
        }

        PlayerType player1 = parsePlayerType(args[0]);
        PlayerType player2 = parsePlayerType(args[1]);

        int numGames = 10;
        ColorSetting colorSetting = ColorSetting.RANDOM;

        // オプション解析
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("N=")) {
                numGames = Integer.parseInt(arg.substring(2));
            } else if (arg.equalsIgnoreCase("BlackFirst")) {
                colorSetting = ColorSetting.PLAYER1_BLACK;
            } else if (arg.equalsIgnoreCase("WhiteFirst")) {
                colorSetting = ColorSetting.PLAYER1_WHITE;
            } else if (arg.equalsIgnoreCase("Random")) {
                colorSetting = ColorSetting.RANDOM;
            } else {
                System.err.println("不明なオプション: " + arg);
            }
        }

        runTournament(player1, player2, numGames, colorSetting);
    }

    /**
     * プレイヤータイプの解析
     */
    private static PlayerType parsePlayerType(String name) {
        for (PlayerType type : PlayerType.values()) {
            if (type.name().equalsIgnoreCase(name) ||
                    type.getClassName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不明なプレイヤー: " + name);
    }

    /**
     * プレイヤー選択（インタラクティブ）
     */
    private static PlayerType selectPlayer(Scanner scanner, String prompt) {
        System.out.println();
        System.out.println("【" + prompt + "を選択】");
        PlayerType[] types = PlayerType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d: %s (%s)\n", i + 1, types[i].name(), types[i].getDescription());
        }

        while (true) {
            System.out.print(prompt + "を選択してください (1-" + types.length + "): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= types.length) {
                    return types[choice - 1];
                } else {
                    System.out.println("1から" + types.length + "の間で選択してください。");
                }
            } catch (NumberFormatException e) {
                System.out.println("数値を入力してください。");
            }
        }
    }

    /**
     * 対戦回数選択
     */
    private static int selectGameCount(Scanner scanner) {
        System.out.print("\n対戦回数を入力してください (デフォルト: 10): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return 10;
        }
        try {
            int count = Integer.parseInt(input);
            return Math.max(1, Math.min(10000, count));
        } catch (NumberFormatException e) {
            System.out.println("無効な入力です。デフォルトの10回で実行します。");
            return 10;
        }
    }

    /**
     * 色設定選択
     */
    private static ColorSetting selectColorSetting(Scanner scanner) {
        System.out.println("\n【色の設定】");
        System.out.println("  1: ランダム（各ゲームで色をランダム決定）");
        System.out.println("  2: プレイヤー1が常に黒");
        System.out.println("  3: プレイヤー1が常に白");

        while (true) {
            System.out.print("色の設定を選択してください (1-3, デフォルト: 1): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return ColorSetting.RANDOM;
            }
            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 1:
                        return ColorSetting.RANDOM;
                    case 2:
                        return ColorSetting.PLAYER1_BLACK;
                    case 3:
                        return ColorSetting.PLAYER1_WHITE;
                    default:
                        System.out.println("1から3の間で選択してください。");
                }
            } catch (NumberFormatException e) {
                System.out.println("数値を入力してください。");
            }
        }
    }

    /**
     * トーナメント実行
     */
    private static void runTournament(PlayerType player1Type, PlayerType player2Type,
            int numGames, ColorSetting colorSetting) {
        System.out.printf("=== %s vs %s (%d回戦) ===\n",
                player1Type.getDescription(), player2Type.getDescription(), numGames);
        System.out.println("色設定: " + colorSetting.getDescription());
        System.out.println();

        Map<String, Integer> results = new HashMap<>();
        results.put(player1Type.getClassName(), 0);
        results.put(player2Type.getClassName(), 0);
        results.put("Draw", 0);

        List<Integer> scores = new ArrayList<>();
        long totalTime = System.currentTimeMillis();

        for (int i = 1; i <= numGames; i++) {
            // 色の決定
            boolean player1IsBlack = determineColor(colorSetting, i);

            // プレイヤー作成
            Player black, white;
            if (player1IsBlack) {
                black = createPlayer(player1Type, BLACK);
                white = createPlayer(player2Type, WHITE);
            } else {
                black = createPlayer(player2Type, BLACK);
                white = createPlayer(player1Type, WHITE);
            }

            // ゲーム実行
            var board = new MyBoard();
            var game = new SilentGame(board, black, white);
            var result = game.playQuiet();

            // 結果記録
            String winner = determineWinner(result, player1Type, player2Type, player1IsBlack);
            results.put(winner, results.get(winner) + 1);
            scores.add(Math.abs(result.score));

            // 進捗表示
            if (i % Math.max(1, numGames / 10) == 0 || i == numGames) {
                System.out.printf("進捗: %d/%d ゲーム完了\n", i, numGames);
            }
        }

        totalTime = System.currentTimeMillis() - totalTime;
        printTournamentResults(player1Type, player2Type, results, scores, numGames, totalTime);
    }

    /**
     * 色の決定
     */
    private static boolean determineColor(ColorSetting setting, int gameNumber) {
        switch (setting) {
            case PLAYER1_BLACK:
                return true;
            case PLAYER1_WHITE:
                return false;
            case RANDOM:
                return Math.random() < 0.5;
            default:
                return true;
        }
    }

    /**
     * プレイヤー作成
     */
    private static Player createPlayer(PlayerType type, Color color) {
        switch (type) {
            case MYPLAYER:
                return new MyPlayer(color);
            case MYPLAYER2:
                return new MyPlayer2(color);
            case MYPLAYER3:
                return new MyPlayer3(color);
            case RANDOM:
                return new RandomPlayer(color);
            case HUMAN:
                return new HumanPlayer(color);
            default:
                throw new IllegalArgumentException("Unknown player type: " + type);
        }
    }

    /**
     * 勝者の決定
     */
    private static String determineWinner(GameResult result, PlayerType player1Type,
            PlayerType player2Type, boolean player1IsBlack) {
        if (result.score == 0) {
            return "Draw";
        }

        Color winnerColor = result.score > 0 ? BLACK : WHITE;
        boolean player1Won = (player1IsBlack && winnerColor == BLACK) ||
                (!player1IsBlack && winnerColor == WHITE);

        return player1Won ? player1Type.getClassName() : player2Type.getClassName();
    }

    /**
     * 結果表示
     */
    private static void printTournamentResults(PlayerType player1Type, PlayerType player2Type,
            Map<String, Integer> results, List<Integer> scores,
            int numGames, long totalTime) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    最終結果");
        System.out.println("=".repeat(60));

        System.out.printf("対戦: %s vs %s\n", player1Type.getDescription(), player2Type.getDescription());
        System.out.printf("総ゲーム数: %d\n", numGames);
        System.out.printf("実行時間: %.2f秒\n\n", totalTime / 1000.0);

        // 勝利数
        System.out.println("【勝利数】");
        int player1Wins = results.get(player1Type.getClassName());
        int player2Wins = results.get(player2Type.getClassName());
        int draws = results.get("Draw");

        System.out.printf("  %-20s: %3d勝 (%.1f%%)\n", player1Type.getClassName(),
                player1Wins, (player1Wins * 100.0) / numGames);
        System.out.printf("  %-20s: %3d勝 (%.1f%%)\n", player2Type.getClassName(),
                player2Wins, (player2Wins * 100.0) / numGames);
        System.out.printf("  %-20s: %3d戦 (%.1f%%)\n", "引き分け",
                draws, (draws * 100.0) / numGames);

        // 統計情報
        OptionalDouble avgScore = scores.stream().mapToInt(Integer::intValue).average();
        int maxScore = scores.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minScore = scores.stream().mapToInt(Integer::intValue).min().orElse(0);

        System.out.println("\n【スコア統計】");
        System.out.printf("  平均差: %.1f\n", avgScore.orElse(0));
        System.out.printf("  最大差: %d\n", maxScore);
        System.out.printf("  最小差: %d\n", minScore);

        // 勝者発表
        System.out.println("\n【結果】");
        if (player1Wins > player2Wins) {
            System.out.printf("🏆 %s の勝利！ (%d-%d)\n",
                    player1Type.getDescription(), player1Wins, player2Wins);
        } else if (player2Wins > player1Wins) {
            System.out.printf("🏆 %s の勝利！ (%d-%d)\n",
                    player2Type.getDescription(), player2Wins, player1Wins);
        } else {
            System.out.printf("🤝 引き分け！ (%d-%d)\n", player1Wins, player2Wins);
        }

        System.out.println("=".repeat(60));
    }

    /**
     * 色設定の種類
     */
    public enum ColorSetting {
        RANDOM("ランダム"),
        PLAYER1_BLACK("プレイヤー1が常に黒"),
        PLAYER1_WHITE("プレイヤー1が常に白");

        private final String description;

        ColorSetting(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * ゲーム結果
     */
    static class GameResult {
        final int score;

        GameResult(int score) {
            this.score = score;
        }
    }

    /**
     * 静かなゲーム実行（出力抑制版）
     */
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

            return new GameResult(board.score());
        }
    }
}
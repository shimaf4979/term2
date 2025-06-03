package p25x01;

import ap25.*;
import static ap25.Board.*;
import static ap25.Color.*;
import java.util.*;

/**
 * Principal Variation Search + 置換表 + 反復深化 オセロAI
 * 
 * 特徴:
 * - PVS (Principal Variation Search) による効率的な探索
 * - 置換表による局面評価の再利用
 * - 反復深化による時間制御
 * - 終盤完全読み
 * - 手順前進による枝刈り効果向上
 * - 異常値検出の強化
 */
public class MyPlayer extends ap25.Player {

    // 時間管理
    private static final long TOTAL_TIME_LIMIT_MS = 58000; // 58秒
    private long totalTimeUsed = 0;
    private int moveCount = 0;

    // 探索コンポーネント
    private final PVSEngine pvsEngine;
    private final TranspositionTable transTable;
    private final Evaluator evaluator;
    private final MoveOrderer moveOrderer;
    private final EndgameSolver endgameSolver;
    private final TimeManager timeManager;

    // 統計
    private long nodesSearched = 0;
    private int maxDepthReached = 0;

    // 前回のスコア（異常値検出用）
    private int previousScore = 0;
    private boolean hasPreviousScore = false;

    public MyPlayer(Color color) {
        super("PVS-AI", color);

        this.transTable = new TranspositionTable();
        this.evaluator = new Evaluator();
        this.moveOrderer = new MoveOrderer(evaluator);
        this.endgameSolver = new EndgameSolver(evaluator);
        this.timeManager = new TimeManager();
        this.pvsEngine = new PVSEngine(transTable, evaluator, moveOrderer);

        System.err.printf("[PVS-AI] 初期化完了 (%s)\n", color);
    }

    @Override
    public Move think(Board board) {
        long startTime = System.currentTimeMillis();
        moveCount++;

        long remainingTime = TOTAL_TIME_LIMIT_MS - totalTimeUsed;
        System.err.printf("[PVS-AI] 手%d | 残り時間: %.2fs\n",
                moveCount, remainingTime / 1000.0);

        try {
            OurBoard ourBoard = convertBoard(board);
            Color myColor = getColor();

            // 合法手チェック
            List<Move> legalMoves = ourBoard.findLegalMoves(myColor);
            if (legalMoves.isEmpty()) {
                return Move.ofPass(myColor);
            }
            if (legalMoves.size() == 1) {
                return legalMoves.get(0);
            }

            // 時間配分
            long allocatedTime = timeManager.allocateTime(ourBoard, remainingTime, moveCount);

            // 即座勝利チェック
            // Move winningMove = findWinningMove(ourBoard, myColor, legalMoves);
            // if (winningMove != null) {
            // System.err.printf("[PVS-AI] 即座勝利: %s\n", winningMove);
            // return winningMove;
            // }

            // 終盤完全読み（より保守的に）
            int emptyCount = countEmpty(ourBoard);
            if (emptyCount <= 6) {
                System.err.println("[PVS-AI] 終盤完全読み開始");
                Move perfectMove = endgameSolver.solve(ourBoard, myColor, Math.min(allocatedTime, 5000));
                if (perfectMove != null) {
                    return perfectMove;
                }
            }

            // メイン探索
            return performIterativeDeepening(ourBoard, myColor, allocatedTime);

        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            totalTimeUsed += elapsed;
            System.err.printf("[PVS-AI] 手%d完了: %.3fs | 合計: %.2fs | 深度: %d | ノード: %d\n",
                    moveCount, elapsed / 1000.0, totalTimeUsed / 1000.0,
                    maxDepthReached, nodesSearched);
        }
    }

    /**
     * 反復深化による探索（異常値検出強化版）
     */
    private Move performIterativeDeepening(OurBoard board, Color color, long timeLimit) {
        long deadline = System.currentTimeMillis() + timeLimit;

        Move bestMove = null;
        int bestScore = -10000;
        nodesSearched = 0;
        maxDepthReached = 0;

        // 前回のPVを保存
        List<Move> previousPV = new ArrayList<>();

        // 安全な結果（異常値が検出された時のフォールバック）
        Move safeMove = null;
        int safeScore = -10000;
        int safeDepth = 0;

        System.err.printf("[PVS-AI] 反復深化開始 (制限時間: %.2fs)\n", timeLimit / 1000.0);

        // 反復深化ループ
        for (int depth = 1; depth <= 20; depth++) {
            long iterationStart = System.currentTimeMillis();

            // 時間チェック
            if (iterationStart >= deadline - 200) {
                System.err.printf("[PVS-AI] 時間切れ (深度%d前)\n", depth);
                break;
            }

            try {
                PVSResult result = pvsEngine.searchRoot(board, color, depth,
                        previousPV, deadline - 100);

                if (result.move != null && result.pv != null) {
                    // 異常値チェック（強化版）
                    boolean isAbnormal = detectAbnormalScore(result.score, depth);

                    long iterationTime = System.currentTimeMillis() - iterationStart;
                    System.err.printf("[PVS-AI] 深度%d: %s (スコア=%d, 時間=%.3fs, PV=%s)%s\n",
                            depth, result.move, result.score, iterationTime / 1000.0,
                            formatPV(result.pv), isAbnormal ? " [異常値検出]" : "");

                    if (!isAbnormal) {
                        // 正常値の場合は採用
                        bestMove = result.move;
                        bestScore = result.score;
                        maxDepthReached = depth;
                        previousPV = new ArrayList<>(result.pv);

                        // 安全な結果として保存
                        safeMove = bestMove;
                        safeScore = bestScore;
                        safeDepth = depth;

                        // 前回スコア更新
                        previousScore = bestScore;
                        hasPreviousScore = true;

                        // 真の必勝/必敗が確定した場合は終了（保守的に）
                        if (isDefinitiveWin(result.score, depth)) {
                            System.err.println("[PVS-AI] 確定的勝負判定、探索終了");
                            break;
                        }
                    } else {
                        // 異常値の場合は警告して無視
                        System.err.printf("[PVS-AI] 警告: 深度%dで異常スコア (%d) を無視\n",
                                depth, result.score);

                        // 安全な結果がある場合はそれを維持
                        if (safeMove != null) {
                            bestMove = safeMove;
                            bestScore = safeScore;
                            maxDepthReached = safeDepth;
                        }
                    }
                } else {
                    System.err.printf("[PVS-AI] 深度%d: 時間切れ\n", depth);
                    break;
                }

            } catch (Exception e) {
                System.err.printf("[PVS-AI] 深度%d でエラー: %s\n", depth, e.getMessage());
                break;
            }
        }

        nodesSearched = pvsEngine.getNodesSearched();

        if (bestMove == null) {
            // フォールバック: 最初の合法手
            bestMove = board.findLegalMoves(color).get(0);
            System.err.println("[PVS-AI] フォールバック手を使用");
        }

        System.err.printf("[PVS-AI] 最終選択: %s (スコア=%d)\n", bestMove, bestScore);
        return bestMove;
    }

    /**
     * 異常スコア検出（強化版）
     */
    private boolean detectAbnormalScore(int score, int depth) {
        // 1. 基本的な異常値判定（ゲーム理論的に不可能なスコア）
        if (Math.abs(score) > 5000) {
            return true;
        }

        // 2. 深度による動的閾値
        int maxReasonableScore;
        if (depth <= 3) {
            maxReasonableScore = 300; // 浅い探索では大きな変動は異常
        } else if (depth <= 6) {
            maxReasonableScore = 800;
        } else if (depth <= 10) {
            maxReasonableScore = 1500;
        } else {
            maxReasonableScore = 2500; // 深い探索でも限界あり
        }

        if (Math.abs(score) > maxReasonableScore) {
            return true;
        }

        // 3. 前回スコアとの比較（急激な変化の検出）
        if (hasPreviousScore && depth >= 3) {
            int scoreDiff = Math.abs(score - previousScore);
            int maxChange;

            if (depth <= 5) {
                maxChange = 500; // 浅い深度では大きな変化は異常
            } else if (depth <= 8) {
                maxChange = 1000;
            } else {
                maxChange = 2000;
            }

            if (scoreDiff > maxChange) {
                return true;
            }
        }

        // 4. 序盤での極端なスコア
        if (moveCount <= 10 && Math.abs(score) > 1000) {
            return true;
        }

        return false;
    }

    /**
     * 確定的勝負判定（保守的）
     */
    private boolean isDefinitiveWin(int score, int depth) {
        // より保守的な判定：確実に勝負が決まっている場合のみ
        if (depth >= 8 && Math.abs(score) >= 3000) {
            return true;
        }
        if (depth >= 10 && Math.abs(score) >= 2000) {
            return true;
        }
        return false;
    }

    /**
     * 即座勝利手の検索
     */
    private Move findWinningMove(OurBoard board, Color color, List<Move> moves) {
        for (Move move : moves) {
            OurBoard after = board.placed(move);

            // 相手の石が0になる
            if (after.count(color.flipped()) == 0) {
                return move;
            }

            // 大きなスコア差
            int scoreDiff = after.count(color) - after.count(color.flipped());
            if (scoreDiff >= 20) {
                return move;
            }
        }
        return null;
    }

    /**
     * ボード変換
     */
    private OurBoard convertBoard(Board board) {
        OurBoard ourBoard = new OurBoard();
        for (int i = 0; i < LENGTH; i++) {
            ourBoard.set(i, board.get(i));
        }
        return ourBoard;
    }

    /**
     * 空きマス数カウント
     */
    private int countEmpty(OurBoard board) {
        int count = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == NONE)
                count++;
        }
        return count;
    }

    /**
     * PV(主要変化)の文字列化
     */
    private String formatPV(List<Move> pv) {
        if (pv == null || pv.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(pv.size(), 5); i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(pv.get(i).toString());
        }
        if (pv.size() > 5)
            sb.append("...");
        return sb.toString();
    }
}

/**
 * PVS (Principal Variation Search) エンジン
 */
class PVSEngine {
    private final TranspositionTable transTable;
    private final Evaluator evaluator;
    private final MoveOrderer moveOrderer;
    private long nodesSearched;

    public PVSEngine(TranspositionTable transTable, Evaluator evaluator, MoveOrderer moveOrderer) {
        this.transTable = transTable;
        this.evaluator = evaluator;
        this.moveOrderer = moveOrderer;
    }

    /**
     * ルート探索
     */
    public PVSResult searchRoot(OurBoard board, Color color, int depth,
            List<Move> previousPV, long deadline) {
        nodesSearched = 0;

        List<Move> moves = board.findLegalMoves(color);
        moves = moveOrderer.orderMoves(moves, board, color, previousPV);

        Move bestMove = null;
        int bestScore = -10000;
        List<Move> bestPV = new ArrayList<>();

        int alpha = -10000;
        int beta = 10000;

        for (int i = 0; i < moves.size(); i++) {
            if (System.currentTimeMillis() >= deadline)
                break;

            Move move = moves.get(i);
            OurBoard newBoard = board.placed(move);

            int score;
            List<Move> pv = new ArrayList<>();
            pv.add(move);

            if (i == 0) {
                // 最初の手は完全窓で探索
                PVSResult result = pvs(newBoard, color.flipped(), depth - 1,
                        -beta, -alpha, pv, deadline);
                score = -result.score;
                if (result.pv != null) {
                    pv.addAll(result.pv);
                }
            } else {
                // 2手目以降はnull window探索
                PVSResult result = pvs(newBoard, color.flipped(), depth - 1,
                        -alpha - 1, -alpha, new ArrayList<>(), deadline);
                score = -result.score;

                if (score > alpha && score < beta) {
                    // re-search with full window
                    pv = new ArrayList<>();
                    pv.add(move);
                    result = pvs(newBoard, color.flipped(), depth - 1,
                            -beta, -alpha, pv, deadline);
                    score = -result.score;
                    if (result.pv != null) {
                        pv.addAll(result.pv);
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                bestPV = pv;
            }

            alpha = Math.max(alpha, score);
            if (alpha >= beta)
                break; // カットオフ
        }

        return new PVSResult(bestMove, bestScore, bestPV);
    }

    /**
     * PVS探索
     */
    private PVSResult pvs(OurBoard board, Color color, int depth, int alpha, int beta,
            List<Move> pv, long deadline) {
        nodesSearched++;

        // 時間チェック
        if (System.currentTimeMillis() >= deadline) {
            return new PVSResult(null, evaluator.evaluate(board, color), null);
        }

        // 深度チェック
        if (depth <= 0) {
            return new PVSResult(null, evaluator.evaluate(board, color), null);
        }

        // 置換表チェック
        TTEntry entry = transTable.probe(board, depth, alpha, beta);
        if (entry != null) {
            return new PVSResult(entry.bestMove, entry.score, null);
        }

        List<Move> moves = board.findLegalMoves(color);

        // パス処理
        if (moves.isEmpty()) {
            List<Move> opponentMoves = board.findLegalMoves(color.flipped());
            if (opponentMoves.isEmpty()) {
                // ゲーム終了
                return new PVSResult(null, evaluator.evaluateTerminal(board, color), null);
            } else {
                // パス
                PVSResult result = pvs(board, color.flipped(), depth, -beta, -alpha,
                        new ArrayList<>(), deadline);
                return new PVSResult(Move.ofPass(color), -result.score, null);
            }
        }

        // 手順並び替え
        moves = moveOrderer.orderMoves(moves, board, color, pv);

        Move bestMove = null;
        int bestScore = -10000;
        List<Move> bestPV = null;
        int flag = TTEntry.UPPER_BOUND;

        for (int i = 0; i < moves.size(); i++) {
            if (System.currentTimeMillis() >= deadline)
                break;

            Move move = moves.get(i);
            OurBoard newBoard = board.placed(move);

            int score;
            List<Move> childPV = new ArrayList<>();

            if (i == 0) {
                // 最初の手は完全窓
                PVSResult result = pvs(newBoard, color.flipped(), depth - 1,
                        -beta, -alpha, childPV, deadline);
                score = -result.score;
                if (result.pv != null)
                    childPV.addAll(result.pv);
            } else {
                // null window探索
                PVSResult result = pvs(newBoard, color.flipped(), depth - 1,
                        -alpha - 1, -alpha, new ArrayList<>(), deadline);
                score = -result.score;

                if (score > alpha && score < beta) {
                    // re-search
                    childPV = new ArrayList<>();
                    result = pvs(newBoard, color.flipped(), depth - 1,
                            -beta, -alpha, childPV, deadline);
                    score = -result.score;
                    if (result.pv != null)
                        childPV.addAll(result.pv);
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                bestPV = childPV;
            }

            if (score >= beta) {
                // Beta cutoff
                flag = TTEntry.LOWER_BOUND;
                break;
            }

            if (score > alpha) {
                alpha = score;
                flag = TTEntry.EXACT;
            }
        }

        // 置換表に保存
        if (bestMove != null) {
            transTable.store(board, bestMove, bestScore, depth, flag);
        }

        return new PVSResult(bestMove, bestScore, bestPV);
    }

    public long getNodesSearched() {
        return nodesSearched;
    }
}

/**
 * PVS探索結果
 */
class PVSResult {
    final Move move;
    final int score;
    final List<Move> pv;

    public PVSResult(Move move, int score, List<Move> pv) {
        this.move = move;
        this.score = score;
        this.pv = pv;
    }
}

/**
 * 置換表
 */
class TranspositionTable {
    private static final int TABLE_SIZE = 1 << 18; // 256K entries
    private static final long TABLE_MASK = TABLE_SIZE - 1;
    private final TTEntry[] table;

    public TranspositionTable() {
        this.table = new TTEntry[TABLE_SIZE];
    }

    public TTEntry probe(OurBoard board, int depth, int alpha, int beta) {
        long hash = calculateHash(board);
        int index = (int) (hash & TABLE_MASK);
        TTEntry entry = table[index];

        if (entry != null && entry.hash == hash && entry.depth >= depth) {
            if (entry.flag == TTEntry.EXACT) {
                return entry;
            } else if (entry.flag == TTEntry.LOWER_BOUND && entry.score >= beta) {
                return entry;
            } else if (entry.flag == TTEntry.UPPER_BOUND && entry.score <= alpha) {
                return entry;
            }
        }

        return null;
    }

    public void store(OurBoard board, Move bestMove, int score, int depth, int flag) {
        long hash = calculateHash(board);
        int index = (int) (hash & TABLE_MASK);

        TTEntry existing = table[index];
        if (existing == null || existing.depth <= depth) {
            table[index] = new TTEntry(hash, bestMove, score, depth, flag);
        }
    }

    private long calculateHash(OurBoard board) {
        long hash = 0x9e3779b97f4a7c15L;
        for (int i = 0; i < LENGTH; i++) {
            Color c = board.get(i);
            int value = (c == BLACK ? 1 : (c == WHITE ? 2 : 0));
            hash ^= value * 0x9e3779b97f4a7c15L * (long) (i + 1);
            hash = Long.rotateLeft(hash, 7);
        }
        return hash;
    }
}

/**
 * 置換表エントリ
 */
class TTEntry {
    static final int EXACT = 0;
    static final int LOWER_BOUND = 1;
    static final int UPPER_BOUND = 2;

    final long hash;
    final Move bestMove;
    final int score;
    final int depth;
    final int flag;

    public TTEntry(long hash, Move bestMove, int score, int depth, int flag) {
        this.hash = hash;
        this.bestMove = bestMove;
        this.score = score;
        this.depth = depth;
        this.flag = flag;
    }
}

/**
 * 評価関数
 */
class Evaluator {
    // 改良版位置価値表（MyPlayer2に近づける）
    private static final int[][] POSITION_VALUES = {
            { 100, -10, 10, 10, -10, 100 },
            { -10, -20, 0, 0, -20, -10 },
            { 10, 0, 5, 5, 0, 10 },
            { 10, 0, 5, 5, 0, 10 },
            { -10, -20, 0, 0, -20, -10 },
            { 100, -10, 10, 10, -10, 100 }
    };

    public int evaluate(OurBoard board, Color color) {
        int score = 0;

        // 1. 位置価値（重み軽減）
        for (int i = 0; i < LENGTH; i++) {
            Color c = board.get(i);
            int row = i / SIZE;
            int col = i % SIZE;

            if (c == color) {
                score += POSITION_VALUES[row][col];
            } else if (c == color.flipped()) {
                score -= POSITION_VALUES[row][col];
            }
        }

        // 2. 機動性（重み軽減）
        int myMobility = board.findLegalMoves(color).size();
        int opMobility = board.findLegalMoves(color.flipped()).size();
        score += (myMobility - opMobility) * 15; // 30→15に削減

        // 3. 確定石（簡素化）
        score += evaluateStability(board, color) * 10; // 25→10に削減

        // 4. 石数差（終盤のみ、重み軽減）
        int emptyCount = countEmpty(board);
        if (emptyCount <= 12) {
            int stoneDiff = board.count(color) - board.count(color.flipped());
            int weight = Math.max(3, 15 - emptyCount); // 重み軽減
            score += stoneDiff * weight;
        }

        // 5. パス強制（重み軽減）
        if (myMobility == 1 && board.findLegalMoves(color).get(0).isPass()) {
            score -= 80; // 150→80に削減
        }
        if (opMobility == 1 && board.findLegalMoves(color.flipped()).get(0).isPass()) {
            score += 80; // 150→80に削減
        }

        return score;
    }

    public int evaluateTerminal(OurBoard board, Color color) {
        int stoneDiff = board.count(color) - board.count(color.flipped());
        if (stoneDiff > 0) {
            return 8000 + stoneDiff * 100;
        } else if (stoneDiff < 0) {
            return -8000 + stoneDiff * 100;
        } else {
            return 0;
        }
    }

    private int evaluateStability(OurBoard board, Color color) {
        int stability = 0;

        // コーナーの確定石
        int[] corners = { 0, SIZE - 1, SIZE * (SIZE - 1), SIZE * SIZE - 1 };
        for (int corner : corners) {
            if (board.get(corner) == color) {
                stability += 3;
            } else if (board.get(corner) == color.flipped()) {
                stability -= 3;
            }
        }

        return stability;
    }

    private int countEmpty(OurBoard board) {
        int count = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == NONE)
                count++;
        }
        return count;
    }
}

/**
 * 手順並び替え
 */
class MoveOrderer {
    private final Evaluator evaluator;

    public MoveOrderer(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public List<Move> orderMoves(List<Move> moves, OurBoard board, Color color, List<Move> pv) {
        List<ScoredMove> scoredMoves = new ArrayList<>();

        for (Move move : moves) {
            int score = 0;

            // PVムーブ最優先
            if (pv != null && !pv.isEmpty() && move.equals(pv.get(0))) {
                score += 10000;
            }

            // 基本位置価値
            int pos = move.getIndex();
            if (isCorner(pos)) {
                score += 2000;
            } else if (isXSquare(pos)) {
                score -= 500;
            } else if (isEdge(pos)) {
                score += 200;
            }

            // 取得石数
            OurBoard after = board.placed(move);
            int captured = board.count(color.flipped()) - after.count(color.flipped());
            score += captured * 50;

            // 機動性変化
            int myMobBefore = board.findLegalMoves(color).size();
            int opMobBefore = board.findLegalMoves(color.flipped()).size();
            int myMobAfter = after.findLegalMoves(color).size();
            int opMobAfter = after.findLegalMoves(color.flipped()).size();

            int mobilityChange = (myMobAfter - opMobAfter) - (myMobBefore - opMobBefore);
            score += mobilityChange * 30;

            scoredMoves.add(new ScoredMove(move, score));
        }

        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        return scoredMoves.stream().map(sm -> sm.move).toList();
    }

    private boolean isCorner(int pos) {
        return pos == 0 || pos == SIZE - 1 || pos == SIZE * (SIZE - 1) || pos == SIZE * SIZE - 1;
    }

    private boolean isEdge(int pos) {
        int row = pos / SIZE;
        int col = pos % SIZE;
        return row == 0 || row == SIZE - 1 || col == 0 || col == SIZE - 1;
    }

    private boolean isXSquare(int pos) {
        return pos == 1 || pos == SIZE - 2 || pos == SIZE || pos == 2 * SIZE - 1 ||
                pos == SIZE * (SIZE - 2) || pos == SIZE * (SIZE - 1) + 1 ||
                pos == SIZE * SIZE - SIZE - 1 || pos == SIZE * SIZE - 2;
    }
}

/**
 * スコア付き手
 */
class ScoredMove {
    final Move move;
    final int score;

    public ScoredMove(Move move, int score) {
        this.move = move;
        this.score = score;
    }
}

/**
 * 終盤完全読み（安全版）
 */
class EndgameSolver {
    private final Evaluator evaluator;
    private final Map<Long, Integer> cache = new HashMap<>();
    private static final int MAX_RECURSION_DEPTH = 8; // 再帰深度制限

    public EndgameSolver(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public Move solve(OurBoard board, Color color, long timeLimit) {
        long deadline = System.currentTimeMillis() + timeLimit;

        List<Move> moves = board.findLegalMoves(color);
        Move bestMove = moves.get(0);
        int bestScore = -10000;

        System.err.printf("[EndgameSolver] %d手を安全読み\n", moves.size());

        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline - 100)
                break;

            try {
                OurBoard newBoard = board.placed(move);
                int score = -solveRecursiveSafe(newBoard, color.flipped(), deadline - 50, 0);

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }

                System.err.printf("[EndgameSolver] %s: %d\n", move, score);
            } catch (Exception e) {
                // エラーが発生した場合は評価関数で代替
                System.err.printf("[EndgameSolver] %s: エラー発生、評価関数使用\n", move);
                OurBoard newBoard = board.placed(move);
                int score = evaluator.evaluate(newBoard, color);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }

        System.err.printf("[EndgameSolver] 最善手: %s (%d)\n", bestMove, bestScore);
        return bestMove;
    }

    private int solveRecursiveSafe(OurBoard board, Color color, long deadline, int depth) {
        // 安全装置: 深度制限
        if (depth >= MAX_RECURSION_DEPTH) {
            return evaluator.evaluate(board, color);
        }

        if (System.currentTimeMillis() >= deadline) {
            return evaluator.evaluate(board, color);
        }

        long hash = calculateHash(board, color);
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }

        List<Move> moves = board.findLegalMoves(color);

        if (moves.isEmpty()) {
            List<Move> opponentMoves = board.findLegalMoves(color.flipped());
            if (opponentMoves.isEmpty()) {
                // ゲーム終了
                int result = evaluator.evaluateTerminal(board, color);
                cache.put(hash, result);
                return result;
            } else {
                // パス
                int result = -solveRecursiveSafe(board, color.flipped(), deadline, depth + 1);
                cache.put(hash, result);
                return result;
            }
        }

        int bestScore = -10000;
        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline)
                break;

            OurBoard newBoard = board.placed(move);
            int score = -solveRecursiveSafe(newBoard, color.flipped(), deadline, depth + 1);
            bestScore = Math.max(bestScore, score);
        }

        cache.put(hash, bestScore);
        return bestScore;
    }

    private long calculateHash(OurBoard board, Color color) {
        long hash = 0;
        for (int i = 0; i < LENGTH; i++) {
            Color c = board.get(i);
            hash = hash * 3 + (c == BLACK ? 1 : (c == WHITE ? 2 : 0));
        }
        return hash * 2 + (color == BLACK ? 1 : 0);
    }
}

/**
 * 時間管理
 */
class TimeManager {
    public long allocateTime(OurBoard board, long remainingTime, int moveCount) {
        int emptyCount = countEmpty(board);

        // 基本時間配分
        long baseTime;
        if (emptyCount >= 28) {
            baseTime = Math.min(2000, remainingTime / 20);
        } else if (emptyCount >= 24) {
            baseTime = Math.min(3000, remainingTime / 15);
        } else if (emptyCount >= 20) {
            baseTime = Math.min(4000, remainingTime / 12);
        } else if (emptyCount >= 16) {
            baseTime = Math.min(6000, remainingTime / 10);
        } else if (emptyCount >= 12) {
            baseTime = Math.min(8000, remainingTime / 8);
        } else if (emptyCount >= 8) {
            baseTime = Math.min(12000, remainingTime / 6);
        } else {
            baseTime = Math.min(20000, remainingTime / 3);
        }

        // 調整
        double importance = calculateImportance(board, emptyCount, moveCount);
        long adjustedTime = (long) (baseTime * importance);

        // 制限
        long minTime = Math.min(500, remainingTime / 30);
        long maxTime = Math.min(remainingTime * 2 / 3, 25000);

        return Math.max(minTime, Math.min(maxTime, adjustedTime));
    }

    private double calculateImportance(OurBoard board, int emptyCount, int moveCount) {
        double importance = 1.0;

        // 序盤の重要局面
        if (moveCount <= 8) {
            importance *= 1.3;
        }

        // 合法手が少ない場合
        int moves = board.findLegalMoves(board.getTurn()).size();
        if (moves <= 3) {
            importance *= 1.5;
        }

        // コーナーが絡む場合
        boolean hasCorner = board.findLegalMoves(board.getTurn()).stream()
                .anyMatch(move -> isCorner(move.getIndex()));
        if (hasCorner) {
            importance *= 1.4;
        }

        // 終盤重視
        if (emptyCount <= 12) {
            importance *= 1.2;
        }
        if (emptyCount <= 8) {
            importance *= 1.5;
        }

        return Math.min(importance, 2.0);
    }

    private int countEmpty(OurBoard board) {
        int count = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == NONE)
                count++;
        }
        return count;
    }

    private boolean isCorner(int pos) {
        return pos == 0 || pos == SIZE - 1 || pos == SIZE * (SIZE - 1) || pos == SIZE * SIZE - 1;
    }
}
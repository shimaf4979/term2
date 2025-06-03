package myplayer;

import ap25.*;
import static ap25.Board.*;
import static ap25.Color.*;
import java.util.*;

/**
 * NegaScout探索をベースとした上級AI
 * NegaScout (Principal Variation Search) と改良された評価関数を使用
 */
public class MyPlayer3 extends Player {
    private static final long TIME_LIMIT_MS = 55000; // 55秒制限
    private long totalTimeUsed = 0;
    private int moveCount = 0;

    // 改良された位置価値テーブル
    private static final int[][] POSITION_VALUES = {
            { 120, -30, 20, 20, -30, 120 },
            { -30, -40, -3, -3, -40, -30 },
            { 20, -3, 5, 5, -3, 20 },
            { 20, -3, 5, 5, -3, 20 },
            { -30, -40, -3, -3, -40, -30 },
            { 120, -30, 20, 20, -30, 120 }
    };

    // 簡易置換表
    private final Map<Long, TTEntry> transTable = new HashMap<>();

    public MyPlayer3(Color color) {
        super("MP3", color);
    }

    @Override
    public Move think(Board board) {
        long startTime = System.currentTimeMillis();
        moveCount++;

        long remainingTime = TIME_LIMIT_MS - totalTimeUsed;

        try {
            MyBoard myBoard = convertBoard(board);
            List<Move> legalMoves = myBoard.findLegalMoves(getColor());

            if (legalMoves.isEmpty())
                return Move.ofPass(getColor());
            if (legalMoves.size() == 1)
                return legalMoves.get(0);

            // 時間配分の計算
            long allocatedTime = calculateTimeAllocation(myBoard, remainingTime);
            long deadline = startTime + allocatedTime;

            // 終盤では完全読み
            int emptyCount = countEmpty(myBoard);
            if (emptyCount <= 8) {
                return perfectSearch(myBoard, getColor(), deadline);
            }

            // NegaScout探索
            return negaScoutSearch(myBoard, getColor(), deadline);

        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            totalTimeUsed += elapsed;
            System.err.printf("[MP3] Move %d: %.3fs | Total: %.2fs\n",
                    moveCount, elapsed / 1000.0, totalTimeUsed / 1000.0);
        }
    }

    /**
     * 時間配分の計算
     */
    private long calculateTimeAllocation(MyBoard board, long remainingTime) {
        int emptySquares = countEmpty(board);

        if (emptySquares >= 28) {
            return Math.min(1800, remainingTime / 18);
        } else if (emptySquares >= 20) {
            return Math.min(3000, remainingTime / 12);
        } else if (emptySquares >= 12) {
            return Math.min(5000, remainingTime / 8);
        } else if (emptySquares >= 6) {
            return Math.min(10000, remainingTime / 4);
        } else {
            return Math.min(20000, remainingTime / 2);
        }
    }

    /**
     * NegaScout探索のメイン関数
     */
    private Move negaScoutSearch(MyBoard board, Color color, long deadline) {
        Move bestMove = null;
        int bestScore = -10000;
        int maxDepth = 0;

        // 置換表クリア（メモリ節約）
        if (transTable.size() > 10000) {
            transTable.clear();
        }

        // 反復深化
        for (int depth = 1; depth <= 14; depth++) {
            if (System.currentTimeMillis() >= deadline - 200)
                break;

            List<Move> moves = orderMovesAdvanced(board.findLegalMoves(color), board, color);
            Move depthBestMove = null;
            int depthBestScore = -10000;

            for (Move move : moves) {
                if (System.currentTimeMillis() >= deadline - 100)
                    break;

                MyBoard newBoard = board.placed(move);
                int score = -negaScout(newBoard, color.flipped(), depth - 1,
                        -10000, 10000, deadline);

                if (score > depthBestScore) {
                    depthBestScore = score;
                    depthBestMove = move;
                }
            }

            if (depthBestMove != null) {
                bestMove = depthBestMove;
                bestScore = depthBestScore;
                maxDepth = depth;

                if (depth >= 4) {
                    System.err.printf("[MP3] Depth %d: %s (score=%d)\n",
                            depth, bestMove, bestScore);
                }
            }
        }

        System.err.printf("[MP3] Final: %s (depth=%d, score=%d)\n",
                bestMove, maxDepth, bestScore);

        return bestMove != null ? bestMove : board.findLegalMoves(color).get(0);
    }

    /**
     * NegaScout探索の再帰関数
     */
    private int negaScout(MyBoard board, Color color, int depth, int alpha, int beta, long deadline) {
        if (depth <= 0 || System.currentTimeMillis() >= deadline) {
            return evaluateAdvanced(board, color);
        }

        // 置換表チェック
        long hash = calculateHash(board);
        TTEntry entry = transTable.get(hash);
        if (entry != null && entry.depth >= depth) {
            if (entry.flag == TTEntry.EXACT) {
                return entry.score;
            } else if (entry.flag == TTEntry.LOWER && entry.score >= beta) {
                return entry.score;
            } else if (entry.flag == TTEntry.UPPER && entry.score <= alpha) {
                return entry.score;
            }
        }

        List<Move> moves = board.findLegalMoves(color);

        if (moves.isEmpty()) {
            // パス
            if (board.findLegalMoves(color.flipped()).isEmpty()) {
                // ゲーム終了
                int result = evaluateTerminal(board, color);
                transTable.put(hash, new TTEntry(result, depth, TTEntry.EXACT));
                return result;
            }
            int result = -negaScout(board, color.flipped(), depth, -beta, -alpha, deadline);
            transTable.put(hash, new TTEntry(result, depth, TTEntry.EXACT));
            return result;
        }

        moves = orderMovesAdvanced(moves, board, color);
        boolean firstMove = true;
        int bestScore = -10000;

        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline)
                break;

            MyBoard newBoard = board.placed(move);
            int score;

            if (firstMove) {
                // 最初の手は通常のalpha-beta
                score = -negaScout(newBoard, color.flipped(), depth - 1,
                        -beta, -alpha, deadline);
                firstMove = false;
            } else {
                // NegaScout: まずnull windowで探索
                score = -negaScout(newBoard, color.flipped(), depth - 1,
                        -alpha - 1, -alpha, deadline);

                // alpha < score < betaなら再探索
                if (score > alpha && score < beta) {
                    score = -negaScout(newBoard, color.flipped(), depth - 1,
                            -beta, -alpha, deadline);
                }
            }

            bestScore = Math.max(bestScore, score);

            if (score >= beta) {
                // Beta cutoff
                transTable.put(hash, new TTEntry(score, depth, TTEntry.LOWER));
                return score;
            }

            alpha = Math.max(alpha, score);
        }

        // 置換表に保存
        int flag = (bestScore <= alpha) ? TTEntry.UPPER : TTEntry.EXACT;
        transTable.put(hash, new TTEntry(bestScore, depth, flag));

        return bestScore;
    }

    /**
     * 完全読み（終盤用）
     */
    private Move perfectSearch(MyBoard board, Color color, long deadline) {
        System.err.println("[MP3] Perfect search mode");

        List<Move> moves = board.findLegalMoves(color);
        Move bestMove = moves.get(0);
        int bestScore = -20000;

        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline - 100)
                break;

            MyBoard newBoard = board.placed(move);
            int score = -perfectMinimax(newBoard, color.flipped(), deadline);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            System.err.printf("[MP3] Perfect: %s = %d\n", move, score);
        }

        System.err.printf("[MP3] Perfect result: %s (score=%d)\n", bestMove, bestScore);
        return bestMove;
    }

    /**
     * 完全読み用のMinimax
     */
    private int perfectMinimax(MyBoard board, Color color, long deadline) {
        if (System.currentTimeMillis() >= deadline) {
            return evaluateAdvanced(board, color);
        }

        List<Move> moves = board.findLegalMoves(color);

        if (moves.isEmpty()) {
            if (board.findLegalMoves(color.flipped()).isEmpty()) {
                return evaluateTerminal(board, color);
            }
            return -perfectMinimax(board, color.flipped(), deadline);
        }

        int bestScore = -20000;
        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline)
                break;

            MyBoard newBoard = board.placed(move);
            int score = -perfectMinimax(newBoard, color.flipped(), deadline);
            bestScore = Math.max(bestScore, score);
        }

        return bestScore;
    }

    /**
     * 高度な手の並び替え
     */
    private List<Move> orderMovesAdvanced(List<Move> moves, MyBoard board, Color color) {
        List<ScoredMove> scoredMoves = new ArrayList<>();

        for (Move move : moves) {
            int score = 0;

            if (!move.isPass()) {
                int row = move.getRow();
                int col = move.getCol();

                // 基本位置価値
                score += POSITION_VALUES[row][col];

                // コーナー最優先
                if (isCorner(move.getIndex())) {
                    score += 1000;
                }

                // X位置（コーナー隣接）は避ける
                if (isXSquare(move.getIndex())) {
                    score -= 500;
                }

                MyBoard after = board.placed(move);

                // 取る石の数
                int captured = board.count(color.flipped()) - after.count(color.flipped());
                score += captured * 15;

                // 機動性
                int myMobility = after.findLegalMoves(color).size();
                int oppMobility = after.findLegalMoves(color.flipped()).size();
                score += (myMobility - oppMobility) * 8;

                // 安定石の数
                score += countStableDiscs(after, color) * 30;

                // 辺の制御
                score += evaluateEdgeControl(after, color) * 20;
            }

            scoredMoves.add(new ScoredMove(move, score));
        }

        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        return scoredMoves.stream().map(sm -> sm.move).toList();
    }

    /**
     * 高度な評価関数
     */
    private int evaluateAdvanced(MyBoard board, Color color) {
        int score = 0;

        // 位置価値
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

        // 機動性
        int myMoves = board.findLegalMoves(color).size();
        int opMoves = board.findLegalMoves(color.flipped()).size();
        score += (myMoves - opMoves) * 15;

        // 安定石
        score += (countStableDiscs(board, color) - countStableDiscs(board, color.flipped())) * 25;

        // 辺の制御
        score += evaluateEdgeControl(board, color) * 10;

        // 石差（終盤では重要）
        int emptyCount = countEmpty(board);
        if (emptyCount <= 12) {
            int stoneDiff = board.count(color) - board.count(color.flipped());
            score += stoneDiff * (15 - emptyCount);
        }

        return score;
    }

    /**
     * 安定石を数える（簡易版）
     */
    private int countStableDiscs(MyBoard board, Color color) {
        int stable = 0;

        // コーナーの石は安定
        int[] corners = { 0, SIZE - 1, SIZE * (SIZE - 1), SIZE * SIZE - 1 };
        for (int corner : corners) {
            if (board.get(corner) == color) {
                stable++;
            }
        }

        return stable;
    }

    /**
     * 辺の制御を評価
     */
    private int evaluateEdgeControl(MyBoard board, Color color) {
        int control = 0;

        // 各辺での石の数
        for (int i = 0; i < SIZE; i++) {
            // 上辺
            if (board.get(i) == color)
                control++;
            else if (board.get(i) == color.flipped())
                control--;

            // 下辺
            int bottom = SIZE * (SIZE - 1) + i;
            if (board.get(bottom) == color)
                control++;
            else if (board.get(bottom) == color.flipped())
                control--;

            // 左辺
            int left = i * SIZE;
            if (board.get(left) == color)
                control++;
            else if (board.get(left) == color.flipped())
                control--;

            // 右辺
            int right = i * SIZE + (SIZE - 1);
            if (board.get(right) == color)
                control++;
            else if (board.get(right) == color.flipped())
                control--;
        }

        return control;
    }

    /**
     * 終端評価
     */
    private int evaluateTerminal(MyBoard board, Color color) {
        int stoneDiff = board.count(color) - board.count(color.flipped());

        if (stoneDiff > 0) {
            return 8000 + stoneDiff * 150;
        } else if (stoneDiff < 0) {
            return -8000 + stoneDiff * 150;
        } else {
            return 0;
        }
    }

    /**
     * 位置判定ヘルパー
     */
    private boolean isCorner(int pos) {
        return pos == 0 || pos == SIZE - 1 || pos == SIZE * (SIZE - 1) || pos == SIZE * SIZE - 1;
    }

    private boolean isXSquare(int pos) {
        return (pos == 1 || pos == SIZE - 2 || pos == SIZE || pos == 2 * SIZE - 1 ||
                pos == SIZE * (SIZE - 2) || pos == SIZE * (SIZE - 1) + 1 ||
                pos == SIZE * SIZE - SIZE - 1 || pos == SIZE * SIZE - 2);
    }

    /**
     * ハッシュ値計算
     */
    private long calculateHash(MyBoard board) {
        long hash = 0;
        for (int i = 0; i < LENGTH; i++) {
            Color c = board.get(i);
            hash = hash * 3 + (c == BLACK ? 1 : (c == WHITE ? 2 : 0));
        }
        return hash;
    }

    /**
     * 空のマス数を数える
     */
    private int countEmpty(MyBoard board) {
        int count = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (board.get(i) == NONE)
                count++;
        }
        return count;
    }

    /**
     * Boardを MyBoardに変換
     */
    private MyBoard convertBoard(Board board) {
        MyBoard mb = new MyBoard();
        for (int i = 0; i < LENGTH; i++) {
            mb.set(i, board.get(i));
        }
        return mb;
    }

    /**
     * スコア付きの手
     */
    private static class ScoredMove {
        final Move move;
        final int score;

        ScoredMove(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }

    /**
     * 置換表エントリ
     */
    private static class TTEntry {
        static final int EXACT = 0;
        static final int LOWER = 1;
        static final int UPPER = 2;

        final int score;
        final int depth;
        final int flag;

        TTEntry(int score, int depth, int flag) {
            this.score = score;
            this.depth = depth;
            this.flag = flag;
        }
    }
}
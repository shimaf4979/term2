package myplayer;

import ap25.*;
import static ap25.Board.*;
import static ap25.Color.*;
import java.util.*;

/**
 * Alpha-Beta探索をベースとした中級AI
 * 基本的なAlpha-Beta pruningと簡単な評価関数を使用
 */
public class MyPlayer2 extends Player {
    private static final long TIME_LIMIT_MS = 55000; // 55秒制限
    private long totalTimeUsed = 0;
    private int moveCount = 0;

    // 基本的な位置価値テーブル
    private static final int[][] POSITION_VALUES = {
            { 100, -20, 10, 10, -20, 100 },
            { -20, -30, -5, -5, -30, -20 },
            { 10, -5, 2, 2, -5, 10 },
            { 10, -5, 2, 2, -5, 10 },
            { -20, -30, -5, -5, -30, -20 },
            { 100, -20, 10, 10, -20, 100 }
    };

    public MyPlayer2(Color color) {
        super("MP2", color);
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

            // Alpha-Beta探索
            return alphaBetaSearch(myBoard, getColor(), deadline);

        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            totalTimeUsed += elapsed;
            System.err.printf("[MP2] Move %d: %.3fs | Total: %.2fs\n",
                    moveCount, elapsed / 1000.0, totalTimeUsed / 1000.0);
        }
    }

    /**
     * 時間配分の計算
     */
    private long calculateTimeAllocation(MyBoard board, long remainingTime) {
        int emptySquares = countEmpty(board);

        if (emptySquares >= 28) {
            return Math.min(1500, remainingTime / 20);
        } else if (emptySquares >= 20) {
            return Math.min(2500, remainingTime / 15);
        } else if (emptySquares >= 12) {
            return Math.min(4000, remainingTime / 10);
        } else if (emptySquares >= 6) {
            return Math.min(8000, remainingTime / 5);
        } else {
            return Math.min(15000, remainingTime / 2);
        }
    }

    /**
     * Alpha-Beta探索のメイン関数
     */
    private Move alphaBetaSearch(MyBoard board, Color color, long deadline) {
        Move bestMove = null;
        int bestScore = -10000;
        int maxDepth = 0;

        // 反復深化
        for (int depth = 1; depth <= 12; depth++) {
            if (System.currentTimeMillis() >= deadline - 100)
                break;

            List<Move> moves = orderMoves(board.findLegalMoves(color), board, color);
            Move depthBestMove = null;
            int depthBestScore = -10000;

            for (Move move : moves) {
                if (System.currentTimeMillis() >= deadline - 50)
                    break;

                MyBoard newBoard = board.placed(move);
                int score = -alphaBeta(newBoard, color.flipped(), depth - 1,
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

                if (depth >= 3) {
                    System.err.printf("[MP2] Depth %d: %s (score=%d)\n",
                            depth, bestMove, bestScore);
                }
            }
        }

        System.err.printf("[MP2] Final: %s (depth=%d, score=%d)\n",
                bestMove, maxDepth, bestScore);

        return bestMove != null ? bestMove : board.findLegalMoves(color).get(0);
    }

    /**
     * Alpha-Beta探索の再帰関数
     */
    private int alphaBeta(MyBoard board, Color color, int depth, int alpha, int beta, long deadline) {
        if (depth <= 0 || System.currentTimeMillis() >= deadline) {
            return evaluate(board, color);
        }

        List<Move> moves = board.findLegalMoves(color);

        if (moves.isEmpty()) {
            // パス
            if (board.findLegalMoves(color.flipped()).isEmpty()) {
                // ゲーム終了
                return evaluateTerminal(board, color);
            }
            return -alphaBeta(board, color.flipped(), depth, -beta, -alpha, deadline);
        }

        moves = orderMoves(moves, board, color);

        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline)
                break;

            MyBoard newBoard = board.placed(move);
            int score = -alphaBeta(newBoard, color.flipped(), depth - 1,
                    -beta, -alpha, deadline);

            if (score >= beta) {
                return beta; // Beta cutoff
            }

            alpha = Math.max(alpha, score);
        }

        return alpha;
    }

    /**
     * 手の並び替え
     */
    private List<Move> orderMoves(List<Move> moves, MyBoard board, Color color) {
        List<ScoredMove> scoredMoves = new ArrayList<>();

        for (Move move : moves) {
            int score = 0;

            // 位置価値
            if (!move.isPass()) {
                int row = move.getRow();
                int col = move.getCol();
                score += POSITION_VALUES[row][col];

                // 取る石の数
                MyBoard after = board.placed(move);
                int captured = board.count(color.flipped()) - after.count(color.flipped());
                score += captured * 10;

                // 機動性
                int myMobility = after.findLegalMoves(color).size();
                int oppMobility = after.findLegalMoves(color.flipped()).size();
                score += (myMobility - oppMobility) * 5;
            }

            scoredMoves.add(new ScoredMove(move, score));
        }

        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        return scoredMoves.stream().map(sm -> sm.move).toList();
    }

    /**
     * 評価関数
     */
    private int evaluate(MyBoard board, Color color) {
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
        score += (myMoves - opMoves) * 10;

        // 石差（終盤では重要）
        int emptyCount = countEmpty(board);
        if (emptyCount <= 10) {
            int stoneDiff = board.count(color) - board.count(color.flipped());
            score += stoneDiff * (12 - emptyCount);
        }

        return score;
    }

    /**
     * 終端評価
     */
    private int evaluateTerminal(MyBoard board, Color color) {
        int stoneDiff = board.count(color) - board.count(color.flipped());

        if (stoneDiff > 0) {
            return 5000 + stoneDiff * 100;
        } else if (stoneDiff < 0) {
            return -5000 + stoneDiff * 100;
        } else {
            return 0;
        }
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
}
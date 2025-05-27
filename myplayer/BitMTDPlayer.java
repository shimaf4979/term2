package myplayer;

import ap25.*;
import java.util.*;

/**
 * 高速ビットボード＋MTD(f)＋置換表＋終盤ネガマックスの強化プレイヤー
 */
public class BitMTDPlayer extends ap25.Player {
    static final int ENDGAME_N = 11;
    static final int SIZE = 6;
    static final int LENGTH = SIZE * SIZE;

    BitBoard bitboard;
    static final double[] WEIGHTS = {
        100, -20, 10, 10, -20, 100,
        -20, -5, 1, 1, -5, -20,
        10, 1, 5, 5, 1, 10,
        10, 1, 5, 5, 1, 10,
        -20, -5, 1, 1, -5, -20,
        100, -20, 10, 10, -20, 100
    };

    Map<Long, Node> transTable = new HashMap<>();

    public BitMTDPlayer(Color color) {
        super("BitMTD", color);
        this.bitboard = new BitBoard();
    }

    @Override
    public Move think(Board board) {
        BitBoard b = new BitBoard();
        for (int i = 0; i < BitBoard.LENGTH; i++) {
            int v = board.get(i).getValue();
            if (v == 1) b.black |= 1L << i;
            if (v == -1) b.white |= 1L << i;
        }
        boolean isBlack = getColor() == ap25.Color.BLACK;
        int[] moves = b.getLegalMoveList(isBlack);
        if (moves.length == 0) {
            return Move.ofPass(getColor());
        }
        if (b.countEmpty() <= ENDGAME_N) {
            return negamaxRoot(b, isBlack);
        } else {
            return mtdfRoot(b, isBlack);
        }
    }

    int countEmpty(BitBoard b) {
        return LENGTH - Long.bitCount(b.black | b.white);
    }

    Move negamaxRoot(BitBoard b, boolean isBlack) {
        int[] moves = b.getLegalMoveList(isBlack);
        if (moves.length == 0) {
            return Move.ofPass(getColor());
        }
        int bestMove = moves[0];
        int bestScore = Integer.MIN_VALUE;
        for (int idx : moves) {
            BitBoard next = b.place(idx, isBlack);
            int score = -negamax(next, !isBlack);
            if (score > bestScore) {
                bestScore = score;
                bestMove = idx;
            }
        }
        return new Move(bestMove, getColor());
    }

    int negamax(BitBoard b, boolean isBlack) {
        int[] moves = b.getLegalMoveList(isBlack);
        if (moves.length == 0) {
            if (b.getLegalMoveList(!isBlack).length == 0) {
                return Long.bitCount(b.black) - Long.bitCount(b.white);
            }
            return -negamax(b, !isBlack);
        }
        int best = Integer.MIN_VALUE;
        for (int idx : moves) {
            BitBoard next = b.place(idx, isBlack);
            int score = -negamax(next, !isBlack);
            if (score > best) best = score;
        }
        return best;
    }

    Move mtdfRoot(BitBoard b, boolean isBlack) {
        int[] moves = b.getLegalMoveList(isBlack);
        if (moves.length == 0) {
            return Move.ofPass(getColor());
        }
        int bestMove = moves[0];
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int idx : moves) {
            BitBoard next = b.place(idx, isBlack);
            double score = -alphabeta(next, !isBlack, 5, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if (score > bestScore) {
                bestScore = score;
                bestMove = idx;
            }
        }
        return new Move(bestMove, getColor());
    }

    double alphabeta(BitBoard b, boolean isBlack, int depth, double alpha, double beta) {
        if (depth == 0 || b.countEmpty() == 0) {
            return evaluate(b, isBlack);
        }
        int[] moves = b.getLegalMoveList(isBlack);
        if (moves.length == 0) {
            if (b.getLegalMoveList(!isBlack).length == 0) {
                return Long.bitCount(b.black) - Long.bitCount(b.white);
            }
            return -alphabeta(b, !isBlack, depth, -beta, -alpha);
        }
        for (int idx : moves) {
            BitBoard next = b.place(idx, isBlack);
            double score = -alphabeta(next, !isBlack, depth - 1, -beta, -alpha);
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }
        return alpha;
    }

    double evaluate(BitBoard b, boolean isBlack) {
        double score = 0;
        for (int i = 0; i < LENGTH; i++) {
            int v = ((b.black >> i) & 1) != 0 ? 1 : ((b.white >> i) & 1) != 0 ? -1 : 0;
            score += WEIGHTS[i] * v;
        }
        return isBlack ? score : -score;
    }

    static class Node {
        double value;
        int depth;
        Node(double value, int depth) { this.value = value; this.depth = depth; }
    }
}
package myplayer;

import ap25.*;
import java.util.*;

public class SimplePlayer extends ap25.Player {
    static final int SIZE = 6;
    static final int LENGTH = SIZE * SIZE;
    static final double[] WEIGHTS = {
        100, -20, 10, 10, -20, 100,
        -20, -5, 1, 1, -5, -20,
        10, 1, 5, 5, 1, 10,
        10, 1, 5, 5, 1, 10,
        -20, -5, 1, 1, -5, -20,
        100, -20, 10, 10, -20, 100
    };

    public SimplePlayer(Color color) {
        super("Simple", color);
    }

    @Override
    public Move think(Board board) {
        List<Move> moves = board.findLegalMoves(getColor());
        if (moves.isEmpty()) return Move.ofPass(getColor());
        Move best = moves.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Move m : moves) {
            Board next = board.placed(m);
            double score = evaluate(next);
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best;
    }

    double evaluate(Board board) {
        double score = 0;
        for (int i = 0; i < LENGTH; i++) {
            int v = board.get(i).getValue();
            score += WEIGHTS[i] * v;
        }
        return getColor() == ap25.Color.BLACK ? score : -score;
    }
}
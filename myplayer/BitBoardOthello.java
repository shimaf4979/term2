package myplayer;

import java.util.*;

/**
 * 6x6オセロのビットボード実装＋探索エンジン
 */
public class BitBoardOthello {
    // 盤面は6x6=36マス。下位36bitのみ使用
    static final int SIZE = 6;
    static final int LENGTH = SIZE * SIZE;
    static final long MASK = (1L << LENGTH) - 1;

    // 盤面状態
    long black, white; // 黒石・白石の配置

    // Zobristハッシュ用
    static final long[][] ZOBRIST = new long[2][LENGTH];
    static {
        Random rnd = new Random(2024);
        for (int c = 0; c < 2; ++c)
            for (int i = 0; i < LENGTH; ++i)
                ZOBRIST[c][i] = rnd.nextLong();
    }

    // 置換表
    static class TTEntry {
        int depth;
        double value;
        int flag; // 0:exact, -1:upper, 1:lower
    }
    static final Map<Long, TTEntry> TT = new HashMap<>();

    // 評価関数重み（例：エッジ・隅・着手可能数など）
    static final double[] WEIGHTS = { 10, -5, 1, 1, -5, 10, /* ...36要素... */ };

    public BitBoardOthello(long black, long white) {
        this.black = black & MASK;
        this.white = white & MASK;
    }

    /** 初期盤面を返す */
    public static BitBoardOthello initial() {
        long b = 0, w = 0;
        b |= 1L << idx(2,2); b |= 1L << idx(3,3);
        w |= 1L << idx(2,3); w |= 1L << idx(3,2);
        return new BitBoardOthello(b, w);
    }

    /** (x,y)→ビットインデックス */
    static int idx(int x, int y) { return y * SIZE + x; }

    /** 手番の石配置を返す */
    long getBoard(boolean isBlack) { return isBlack ? black : white; }

    /** 空きマスのビットボード */
    long empty() { return ~(black | white) & MASK; }

    /** 合法手を返す（ビットボード） */
    long legalMoves(boolean isBlack) {
        // 8方向のルックアップで高速化可。ここでは簡易実装
        long me = getBoard(isBlack), opp = getBoard(!isBlack), moves = 0;
        int[] dx = {-1,0,1,-1,1,-1,0,1}, dy = {-1,-1,-1,0,0,1,1,1};
        for (int y = 0; y < SIZE; ++y) for (int x = 0; x < SIZE; ++x) {
            int k = idx(x,y);
            if (((empty() >> k) & 1) == 0) continue;
            for (int d = 0; d < 8; ++d) {
                int nx = x+dx[d], ny = y+dy[d], n = 0;
                while (0<=nx&&nx<SIZE && 0<=ny&&ny<SIZE && ((opp>>(idx(nx,ny)))&1)==1) {
                    nx += dx[d]; ny += dy[d]; n++;
                }
                if (n>0 && 0<=nx&&nx<SIZE && 0<=ny&&ny<SIZE && ((me>>(idx(nx,ny)))&1)==1) {
                    moves |= 1L<<k; break;
                }
            }
        }
        return moves;
    }

    /** 石を置いて新しい盤面を返す */
    public BitBoardOthello place(boolean isBlack, int pos) {
        long me = getBoard(isBlack), opp = getBoard(!isBlack);
        long flip = 0;
        int[] dx = {-1,0,1,-1,1,-1,0,1}, dy = {-1,-1,-1,0,0,1,1,1};
        int x = pos%SIZE, y = pos/SIZE;
        for (int d = 0; d < 8; ++d) {
            int nx = x+dx[d], ny = y+dy[d], n = 0;
            long mask = 0;
            while (0<=nx&&nx<SIZE && 0<=ny&&ny<SIZE && ((opp>>(idx(nx,ny)))&1)==1) {
                mask |= 1L<<idx(nx,ny);
                nx += dx[d]; ny += dy[d]; n++;
            }
            if (n>0 && 0<=nx&&nx<SIZE && 0<=ny&&ny<SIZE && ((me>>(idx(nx,ny)))&1)==1)
                flip |= mask;
        }
        if (isBlack)
            return new BitBoardOthello(me|flip|(1L<<pos), opp&~flip);
        else
            return new BitBoardOthello(opp&~flip, me|flip|(1L<<pos));
    }

    /** 石数カウント */
    int count(long bb) { return Long.bitCount(bb); }

    /** 評価関数（特徴量＋重み） */
    double evaluate(boolean isBlack) {
        double score = 0;
        for (int i = 0; i < LENGTH; ++i) {
            if (((black>>i)&1)==1) score += WEIGHTS[i];
            if (((white>>i)&1)==1) score -= WEIGHTS[i];
        }
        // 着手可能数なども加味可
        score += 0.1 * (Long.bitCount(legalMoves(isBlack)) - Long.bitCount(legalMoves(!isBlack)));
        return score;
    }

    /** Zobristハッシュ計算 */
    long zobrist() {
        long h = 0;
        for (int i = 0; i < LENGTH; ++i) {
            if (((black>>i)&1)==1) h ^= ZOBRIST[0][i];
            if (((white>>i)&1)==1) h ^= ZOBRIST[1][i];
        }
        return h;
    }

    /** 終盤ネガマックス（空き11以下） */
    double negamax(boolean isBlack, int depth, LinkedList<Integer> empties) {
        if (empties.isEmpty()) return count(black) - count(white);
        double best = -1000;
        long moves = legalMoves(isBlack);
        if (moves == 0) {
            if (legalMoves(!isBlack) == 0) // パスも不可＝終局
                return count(black) - count(white);
            return -negamax(!isBlack, depth, empties);
        }
        for (int pos : empties) {
            if (((moves>>pos)&1)==0) continue;
            empties.remove((Integer)pos);
            BitBoardOthello next = place(isBlack, pos);
            double v = -next.negamax(!isBlack, depth+1, empties);
            empties.addFirst(pos);
            if (v > best) best = v;
        }
        return best;
    }

    /** MTD(f)探索本体 */
    double mtdf(boolean isBlack, double f, int depth, int maxDepth) {
        double g = f, upper = 10000, lower = -10000;
        while (lower < upper) {
            double beta = (g == lower) ? g + 1 : g;
            g = search(isBlack, beta-1, beta, 0, maxDepth);
            if (g < beta) upper = g; else lower = g;
        }
        return g;
    }

    /** αβ探索＋置換表 */
    double search(boolean isBlack, double alpha, double beta, int depth, int maxDepth) {
        long hash = zobrist();
        TTEntry entry = TT.get(hash);
        if (entry != null && entry.depth >= maxDepth-depth) {
            if (entry.flag == 0) return entry.value;
            if (entry.flag == -1 && entry.value <= alpha) return entry.value;
            if (entry.flag == 1 && entry.value >= beta) return entry.value;
        }
        if (depth == maxDepth) return evaluate(isBlack);
        long moves = legalMoves(isBlack);
        if (moves == 0) {
            if (legalMoves(!isBlack) == 0)
                return count(black) - count(white);
            return -search(!isBlack, -beta, -alpha, depth+1, maxDepth);
        }
        double best = -10000;
        for (int i = 0; i < LENGTH; ++i) {
            if (((moves>>i)&1)==0) continue;
            BitBoardOthello next = place(isBlack, i);
            double v = -next.search(!isBlack, -beta, -alpha, depth+1, maxDepth);
            if (v > best) best = v;
            if (v > alpha) alpha = v;
            if (alpha >= beta) break;
        }
        TTEntry newEntry = new TTEntry();
        newEntry.depth = maxDepth-depth;
        newEntry.value = best;
        newEntry.flag = (best <= alpha) ? -1 : (best >= beta ? 1 : 0);
        TT.put(hash, newEntry);
        return best;
    }

    /** 盤面表示 */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < SIZE; ++x) {
                int k = idx(x,y);
                if (((black>>k)&1)==1) sb.append("●");
                else if (((white>>k)&1)==1) sb.append("○");
                else sb.append(".");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** テスト用main */
    public static void main(String[] args) {
        BitBoardOthello board = BitBoardOthello.initial();
        boolean turn = true; // true=黒, false=白
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println(board);
            long moves = board.legalMoves(turn);
            if (moves == 0 && board.legalMoves(!turn) == 0) break;
            if (moves == 0) { System.out.println("パス"); turn = !turn; continue; }
            System.out.print((turn?"黒":"白")+"の手 (例: a1=0, b1=1...): ");
            int pos = sc.nextInt();
            if (((moves>>pos)&1)==0) { System.out.println("不正な手"); continue; }
            board = board.place(turn, pos);
            turn = !turn;
        }
        System.out.println("終了\n"+board);
        System.out.println("黒: "+board.count(board.black)+" 白: "+board.count(board.white));
    }
}
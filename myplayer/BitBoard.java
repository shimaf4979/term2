package myplayer;

/**
 * 6x6オセロ専用のビットボード表現クラス
 * black, white: 各色の石の配置（下位36bitのみ使用）
 */
public class BitBoard implements Cloneable {
    public long black, white;

    public static final int SIZE = 6;
    public static final int LENGTH = SIZE * SIZE;

    public BitBoard() { this.black = 0; this.white = 0; }
    public BitBoard(long black, long white) { this.black = black; this.white = white; }

    /** 指定位置に石を置く */
    public void set(int idx, boolean isBlack) {
        long mask = 1L << idx;
        if (isBlack) black |= mask; else white |= mask;
    }

    /** 指定位置の色を取得: 1=黒, -1=白, 0=空 */
    public int get(int idx) {
        long mask = 1L << idx;
        if ((black & mask) != 0) return 1;
        if ((white & mask) != 0) return -1;
        return 0;
    }

    /** クローン */
    public BitBoard clone() { return new BitBoard(black, white); }

    /** 盤面の空きマス数を返す */
    public int countEmpty() {
        return LENGTH - Long.bitCount(black | white);
    }

    /** 合法手のビット列を返す（isBlack: 手番） */
    public long getLegalMoves(boolean isBlack) {
        long me = isBlack ? black : white;
        long opp = isBlack ? white : black;
        long empty = ~(black | white) & ((1L << LENGTH) - 1);
        long legal = 0;
        // 8方向
        int[] dir = {1, -1, 6, -6, 7, -7, 5, -5};
        for (int d = 0; d < dir.length; d++) {
            int offset = dir[d];
            long mask = shift(me, offset);
            long candidates = 0;
            for (int i = 0; i < 5; i++) {
                mask = shift(mask & opp, offset);
                candidates |= mask;
            }
            legal |= shift(candidates & opp, offset) & empty;
        }
        return legal;
    }

    /** 指定方向にビットをシフト（盤外考慮なし） */
    private long shift(long b, int offset) {
        if (offset > 0) return b << offset;
        if (offset < 0) return b >>> -offset;
        return b;
    }

    /** 合法手のリストを返す（インデックス配列） */
    public int[] getLegalMoveList(boolean isBlack) {
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
        for (int idx = 0; idx < LENGTH; idx++) {
            if (get(idx) != 0) continue;
            int x = idx % SIZE, y = idx / SIZE;
            boolean legal = false;
            // 8方向
            int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
            int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};
            for (int d = 0; d < 8; d++) {
                int nx = x + dx[d], ny = y + dy[d];
                boolean foundOpp = false;
                while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE) {
                    int ni = ny * SIZE + nx;
                    int v = get(ni);
                    if (v == 0) break;
                    if (v == (isBlack ? -1 : 1)) {
                        foundOpp = true;
                    } else if (v == (isBlack ? 1 : -1)) {
                        if (foundOpp) legal = true;
                        break;
                    } else {
                        break;
                    }
                    nx += dx[d];
                    ny += dy[d];
                }
                if (legal) break;
            }
            if (legal) list.add(idx);
        }
        return list.stream().mapToInt(i -> i).toArray();
    }

    /** 石を置いて反転処理を行う（返り値: 反転後のBitBoard） */
    public BitBoard place(int idx, boolean isBlack) {
        BitBoard next = this.clone();
        int x = idx % SIZE, y = idx / SIZE;
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};
        for (int d = 0; d < 8; d++) {
            int nx = x + dx[d], ny = y + dy[d];
            java.util.ArrayList<Integer> toFlip = new java.util.ArrayList<>();
            while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE) {
                int ni = ny * SIZE + nx;
                int v = get(ni);
                if (v == 0) break;
                if (v == (isBlack ? -1 : 1)) {
                    toFlip.add(ni);
                } else if (v == (isBlack ? 1 : -1)) {
                    for (int flipIdx : toFlip) {
                        next.set(flipIdx, isBlack);
                    }
                    break;
                } else {
                    break;
                }
                nx += dx[d];
                ny += dy[d];
            }
        }
        next.set(idx, isBlack);
        return next;
    }

    /** 指定位置に石を置いたときの合法手判定（境界考慮版） */
    public boolean isLegalMove(int idx, boolean isBlack) {
        if (get(idx) != 0) return false;
        int x = idx % SIZE, y = idx / SIZE;
        // 8方向
        int[] dir = {1, -1, SIZE, -SIZE, SIZE + 1, SIZE - 1, -SIZE + 1, -SIZE - 1};
        for (int d = 0; d < dir.length; d++) {
            int offset = dir[d];
            int nx = x, ny = y;
            boolean foundOpp = false;
            while (true) {
                nx += offset % SIZE;
                ny += offset / SIZE;
                if (nx < 0 || nx >= SIZE || ny < 0 || ny >= SIZE) break;
                int ni = ny * SIZE + nx;
                if (get(ni) == 0) break;
                if (get(ni) == (isBlack ? -1 : 1)) {
                    foundOpp = true;
                } else if (get(ni) == (isBlack ? 1 : -1)) {
                    if (foundOpp) {
                        return true;
                    }
                    break;
                } else {
                    break;
                }
            }
        }
        return false;
    }
}
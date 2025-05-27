package myplayer;
import java.util.Random;

/** Zobristハッシュ生成用ユーティリティ */
public class Zobrist {
    static final long[][] ZB = new long[2][36];
    static {
        Random r = new Random(20240527);
        for (int c = 0; c < 2; ++c)
            for (int i = 0; i < 36; ++i)
                ZB[c][i] = r.nextLong();
    }
    /** 盤面のハッシュ値を計算 */
    public static long hash(BitBoard b) {
        long h = 0;
        for (int i = 0; i < 36; ++i) {
            if (((b.black >> i) & 1) != 0) h ^= ZB[0][i];
            if (((b.white >> i) & 1) != 0) h ^= ZB[1][i];
        }
        return h;
    }
}
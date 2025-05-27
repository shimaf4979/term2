package myplayer;
import java.util.*;

/**
 * 簡易定跡データ構造（実際はバイナリファイルからロードする設計）
 */
public class OpeningBook {
    Map<Long, Integer> book = new HashMap<>();
    public OpeningBook() {
        // 例: book.put(盤面ハッシュ, 最善手インデックス);
    }
    public Integer getMove(BitBoard b) {
        return book.get(Zobrist.hash(b));
    }
}
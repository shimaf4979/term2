import ap25.*;
import ap25.league.*;
import java.util.function.*;
import static ap25.Color.*;

class Competition25 {
  final static long TIME_LIMIT_SECONDS = 60;

  public static void main(String args[]) {
    Function<Color, Player[]> builder = (Color color) -> {
      return new Player[] {
          new p25x01.OurPlayer(color),
          new p25x01.MyPlayer(color),
          new p25x01.MyPlayerrui(color),
      };
    };

    var league = new League(20, builder, TIME_LIMIT_SECONDS);
    league.run();
  }

  public static void singleGame(String args[]) {
    var player1 = new p25x01.OurPlayer(BLACK);
    var player2 = new p25x01.OurPlayer(WHITE);
    var board = new OfficialBoard();
    var game = new Game(board, player1, player2, TIME_LIMIT_SECONDS);
    game.play();
  }
}

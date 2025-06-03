# コンパイラとコンパイラオプションの設定
JAVAC = javac
JAVA = java
BIN_DIR = bin
JAVAC_OPTS = -cp $(BIN_DIR)

# ソースファイルのディレクトリ
SRC_DIRS = myplayer ap25

# メインクラス
MAIN_CLASS = myplayer.MyGame
RUNNER_CLASS = myplayer.GameRunner
HUMAN_CLASS = myplayer.HumanGame
INTERACTIVE_CLASS = myplayer.InteractiveGame
TOURNAMENT_CLASS = myplayer.TournamentRunner

# OSに応じたパス区切り文字の設定
ifeq ($(OS),Windows_NT)
    PATH_SEP = ;
else
    PATH_SEP = :
endif

# デフォルトターゲット
all:
	@echo "利用可能なコマンド:"
	@echo "  make compile     - ソースコードをコンパイル"
	@echo "  make run         - 1回のゲームを実行（MyPlayer vs Random）"
	@echo "  make interactive - インタラクティブゲーム（推奨）"
	@echo "                     ★先手後手、相手タイプを自由に選択可能"
	@echo "  make human       - 人間 vs AI のゲームを実行（黒固定）"
	@echo "  make test        - 100回のゲームを実行して統計を表示"
	@echo "  make test N=回数 - 指定回数のゲームを実行"
	@echo "  make clean       - コンパイル済みファイルを削除"
	@echo "  make tournament  - トーナメントモード（プレイヤー選択可能）"

# binディレクトリの作成
bin:
	mkdir -p $(BIN_DIR)

# コンパイル
compile: bin
	$(JAVAC) $(JAVAC_OPTS) -d $(BIN_DIR) myplayer/*.java ap25/*.java

# 1回実行（MyPlayer vs Random）
run: compile
	@echo "MyPlayer(先手・黒) vs RandomPlayer(後手・白) でゲームを開始します..."
	$(JAVA) $(JAVAC_OPTS) $(MAIN_CLASS)

# インタラクティブゲーム実行（推奨）
interactive: compile
	@echo "インタラクティブモードでゲームを開始します..."
	@echo "先手後手、相手タイプを自由に選択できます。"
	$(JAVA) $(JAVAC_OPTS) $(INTERACTIVE_CLASS)

# 人間 vs AI実行（黒固定）
human: compile
	@echo "人間(先手・黒) vs MyPlayer(後手・白) でゲームを開始します..."
	$(JAVA) $(JAVAC_OPTS) $(HUMAN_CLASS)

# 複数回実行（デフォルト100回）
test: compile
	@echo "MyPlayerの性能テストを開始します..."
	$(JAVA) $(JAVAC_OPTS) $(RUNNER_CLASS) $(if $(N),$(N),100)

# 特定回数実行の例
test10: compile
	@echo "10回のテストを実行します..."
	$(JAVA) $(JAVAC_OPTS) $(RUNNER_CLASS) 10

test1000: compile
	@echo "1000回のテストを実行します..."
	$(JAVA) $(JAVAC_OPTS) $(RUNNER_CLASS) 1000

# トーナメントモード実行
tournament: compile
	@echo "トーナメントモードを開始します..."
	@echo "プレイヤーを選択して対戦できます。"
	$(JAVA) $(JAVAC_OPTS) $(TOURNAMENT_CLASS)

# クリーンアップ
clean:
	rm -rf $(BIN_DIR)

# ヘルプ表示
help:
	@echo "=== 6x6オセロゲーム ==="
	@echo ""
	@echo "【推奨】make interactive"
	@echo "  - 先手後手を自由に選択"
	@echo "  - 相手（AI、ランダム、人間）を自由に選択"
	@echo "  - 最も柔軟な対戦モード"
	@echo ""
	@echo "【その他のコマンド】"
	@echo "  make human     - 人間先手 vs AI後手（固定）"
	@echo "  make run       - AI vs ランダム（観戦モード）"
	@echo "  make test      - AI性能テスト（100回）"
	@echo "  make clean     - ファイル削除"

.PHONY: all compile run human interactive test test10 test1000 clean bin help tournament
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

# OSに応じたパス区切り文字の設定
ifeq ($(OS),Windows_NT)
    PATH_SEP = ;
else
    PATH_SEP = :
endif

# デフォルトターゲット
all:
	@echo "利用可能なコマンド:"
	@echo "  make compile    - ソースコードをコンパイル"
	@echo "  make run        - 1回のゲームを実行"
	@echo "  make test       - 100回のゲームを実行して統計を表示"
	@echo "  make test N=回数 - 指定回数のゲームを実行"
	@echo "  make clean      - コンパイル済みファイルを削除"

# binディレクトリの作成
bin:
	mkdir -p $(BIN_DIR)

# コンパイル
compile: bin
	$(JAVAC) $(JAVAC_OPTS) -d $(BIN_DIR) myplayer/*.java ap25/*.java

# 1回実行
run: compile
	$(JAVA) $(JAVAC_OPTS) $(MAIN_CLASS)

# 複数回実行（デフォルト100回）
test: compile
	$(JAVA) $(JAVAC_OPTS) $(RUNNER_CLASS) $(if $(N),$(N),100)

# 特定回数実行の例
test10: compile
	$(JAVA) $(JAVAC_OPTS) $(RUNNER_CLASS) 10

test1000: compile
	$(JAVA) $(JAVAC_OPTS) $(RUNNER_CLASS) 1000

# クリーンアップ
clean:
	rm -rf $(BIN_DIR)

.PHONY: all compile run test test10 test1000 clean bin
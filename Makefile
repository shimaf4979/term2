# コンパイラとコンパイラオプションの設定
JAVAC = javac
JAVA = java
JAVAC_OPTS = -cp bin

# ソースファイルのディレクトリ
SRC_DIRS = myplayer ap25

# メインクラス
MAIN_CLASS = myplayer.MyGame

# デフォルトターゲット
all:
	@echo "利用可能なコマンド:"
	@echo "  make compile - ソースコードをコンパイル"
	@echo "  make run     - プログラムを実行"
	@echo "  make clean   - コンパイル済みファイルを削除"

# binディレクトリの作成
bin:
	mkdir -p bin

# コンパイル
compile: bin
	$(JAVAC) $(JAVAC_OPTS) -d bin myplayer/*.java ap25/*.java

# 実行
run: compile
	$(JAVA) $(JAVAC_OPTS) $(MAIN_CLASS)

# クリーンアップ
clean:
	rm -rf bin

.PHONY: all compile run clean bin
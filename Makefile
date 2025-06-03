# コンパイラとコンパイルオプションの設定
JAVAC = javac
JAVAC_FLAGS = -encoding UTF-8

# ソースファイルのディレクトリ
SRC_DIR = .
AP25_DIR = ap25
P25X00_DIR = p25x00

# クラスファイルの出力ディレクトリ
CLASS_DIR = classes

# メインクラス
MAIN_CLASS = Competition25

# デフォルトターゲット
all: compile

# コンパイル
compile:
	@mkdir -p $(CLASS_DIR)
	$(JAVAC) $(JAVAC_FLAGS) -d $(CLASS_DIR) $(SRC_DIR)/$(MAIN_CLASS).java

# リーグモードの実行
run-league: compile
	java -cp $(CLASS_DIR) $(MAIN_CLASS)

# シングルゲームモードの実行
run-single: compile
	java -cp $(CLASS_DIR) $(MAIN_CLASS) single

# クリーンアップ
clean:
	rm -rf $(CLASS_DIR)

.PHONY: all compile run-league run-single clean

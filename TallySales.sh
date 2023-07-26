#!/bin/bash

# メインクラス名を指定します
MAIN_CLASS="jp.co.local.TallySales"

# TallySales.jarファイルのパス
JAR_FILE="./TallySales.jar"

# JavaコマンドでTallySales.jarの中のメインクラスを実行します
java -cp "$JAR_FILE" "$MAIN_CLASS"sh
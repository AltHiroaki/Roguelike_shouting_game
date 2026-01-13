@echo off
chcp 65001
echo === ゲームクライアントを起動します ===
echo コンパイル中...
javac -encoding UTF-8 game/*.java
echo 起動中...
java game.ActionClient
pause
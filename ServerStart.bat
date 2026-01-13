@echo off
chcp 65001
echo === サーバーを起動します ===
echo コンパイル中...
javac -encoding UTF-8 MyServer2.java
echo 起動中...
java MyServer2
pause
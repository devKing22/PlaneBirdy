@echo off
cd /d "%~dp0"
javac -cp lib/jlayer-1.0.1.jar -d out src/*.java
java -cp out;lib/jlayer-1.0.1.jar FlappyPlane
pause

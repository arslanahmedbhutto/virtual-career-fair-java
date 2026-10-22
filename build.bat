@echo off
rem Compiles the app, runs the JUnit tests and packages a runnable jar (Windows).
rem Usage: build.bat   (then: java -jar dist\virtual-career-fair.jar)
setlocal
cd /d "%~dp0"
set TEST_CP=build\app;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\app build\test dist

echo Compiling application...
javac -d build\app src\*.java src\Model\*.java src\Controller\*.java src\View\*.java || exit /b 1

echo Compiling and running tests...
javac -cp "%TEST_CP%" -d build\test test\*.java || exit /b 1
java -cp "%TEST_CP%;build\test" org.junit.runner.JUnitCore Test.LobbyTest Test.MeetingSessionTest Test.ReservationTest Test.VCFSystemTest | findstr /b /c:"OK" /c:"FAILURES" /c:"Tests run"

echo Packaging dist\virtual-career-fair.jar...
jar --create --file dist\virtual-career-fair.jar --main-class Main -C build\app .
echo Done. Run it with: java -jar dist\virtual-career-fair.jar

#!/usr/bin/env bash
# Compiles the app, runs the JUnit tests and packages a runnable jar.
# Usage: ./build.sh            (then: java -jar dist/virtual-career-fair.jar)
set -euo pipefail
cd "$(dirname "$0")"

SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac
TEST_CP="build/app${SEP}lib/junit-4.13.2.jar${SEP}lib/hamcrest-core-1.3.jar"

rm -rf build dist
mkdir -p build/app build/test dist

echo "Compiling application..."
javac -d build/app $(find src -name "*.java")

echo "Compiling and running tests..."
javac -cp "$TEST_CP" -d build/test test/*.java
java -cp "${TEST_CP}${SEP}build/test" org.junit.runner.JUnitCore \
    Test.LobbyTest Test.MeetingSessionTest Test.ReservationTest Test.VCFSystemTest \
    | grep -E "^(OK|FAILURES|Tests run)" || true

echo "Packaging dist/virtual-career-fair.jar..."
jar --create --file dist/virtual-career-fair.jar --main-class Main -C build/app .
echo "Done. Run it with: java -jar dist/virtual-career-fair.jar"

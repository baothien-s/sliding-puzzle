# Sliding Puzzle (Java Swing)

```
game_docker/
  run.bat     Double-click to play
  build.bat   Rebuild SlidingPuzzle.jar from source
  SlidingPuzzle.jar  Prebuilt game
  src/game/   Main, Game, PuzzleBoard, PuzzlePiece, GamePanel
  test/game/  GameTests (no JUnit needed)
```

## Run on Windows
Double-click `run.bat` (needs Java 11+; a JRE is enough). It launches the prebuilt `SlidingPuzzle.jar`.
After editing the code, double-click `build.bat` (needs a JDK) to rebuild the jar.

## Run from the command line (JDK 11+)
```
javac -d out src/game/*.java
java -cp out game.Main
```
In an IDE: create a Java project, mark `src` as the source root, run `game.Main`.

## Test
```
javac -d out src/game/*.java test/game/*.java
java -cp out game.GameTests
```

## Controls
Click a tile next to the gap, or use the arrow keys (the tile slides in the arrow's direction).
Best scores are saved to `~/.sliding-puzzle-scores.properties`.

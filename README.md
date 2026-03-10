# ScaLaMatrixRain

![Matrix Digital Rain](https://github.com/user-attachments/assets/140317b8-0336-4402-9469-de003ee6ca6e)

[Scala](https://www.scala-lang.org/) [Lanterna](https://github.com/mabe02/lanterna) [Matrix Digital Rain](https://en.wikipedia.org/wiki/Matrix_digital_rain)

- [Lanterna](https://github.com/mabe02/lanterna) allows you to write easy semi-graphical user interfaces in a text-only environment, very similar to the C library curses but with more functionality.

## Run

```shell
./gradlew shadowJar && java -jar slmatrix/build/libs/slmatrix-all.jar
```

## Usage

```shell
Usage: slmatrix [options]

Help options:
  --usage            Print usage and exit
  -h, -help, --help  Print help message and exit

Other options:
  --scenes cursorBlink,trace,wakeUp,traceFail,rain  Which scenes to run
  --physics rain,warp,spiral                     Which physics to use
  --test-pattern                                 Display the test pattern during rain scene
```

## Native

### Setup GraalVM

Install [SDKMAN!](https://sdkman.io/)

```shell
sdk install java 23.0.1-graalce
```

### Compile

```shell
sdk use java 23.0.1-graalce
./gradlew nativeCompile --no-configuration-cache
```

## ToDo

### Options

Add flags for various options like character ranges, color, velocity vector, etc.

### Native

Compile for multiple targets and architectures like Linux, Windows, and MacOS.

Make a proper GitHub release with the compiled binaries.

Make it available via Homebrew and Scoop.

### GraalVM Recommendations

- G1GC: Use the G1 GC ('--gc=G1') for improved latency and throughput.
- PGO:  Use Profile-Guided Optimizations ('--pgo') for improved throughput.
- AWT:  Use the tracing agent to collect metadata for AWT.
- HEAP: Set max heap for improved and more predictable memory usage.
- CPU:  Enable more CPU features with '-march=native' for improved performance.

### Versioning

Add axion-release-plugin for versioning and releasing.

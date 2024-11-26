# ScaLaMatrixRain

[Scala](https://www.scala-lang.org/) [Lanterna](https://github.com/mabe02/lanterna) [Matrix Digital Rain](https://en.wikipedia.org/wiki/Matrix_digital_rain)

## Run

```shell
./gradlew shadowJar && java -jar app/build/libs/app-all.jar
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




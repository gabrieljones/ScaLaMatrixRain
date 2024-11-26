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

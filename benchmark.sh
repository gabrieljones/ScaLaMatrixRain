#!/bin/bash
./gradlew :slmatrix:test --tests "org.gabrieljones.scalarain.MainBenchmark.benchmarkRunLoop" | grep "FPS:"

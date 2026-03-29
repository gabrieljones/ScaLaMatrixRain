#!/bin/bash
./gradlew cleanTest :slmatrix:test --tests "org.gabrieljones.scalarain.MainBenchmark.benchmarkRunLoop" --no-build-cache
cat slmatrix/build/reports/tests/test/org.gabrieljones.scalarain.MainBenchmark/benchmarkRunLoop\(\).html | grep -i benchmark

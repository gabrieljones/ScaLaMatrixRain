#!/bin/bash
./gradlew clean test --tests org.gabrieljones.scalarain.MainBenchmark > /dev/null 2>&1
cat ./slmatrix/build/test-results/test/TEST-org.gabrieljones.scalarain.MainBenchmark.xml | grep -o '===== Benchmark Result.*====='

#!/bin/bash
./gradlew clean test --tests "org.gabrieljones.scalarain.MainBenchmark.benchmarkRunLoop" --rerun-tasks --info | grep "Benchmark Result"

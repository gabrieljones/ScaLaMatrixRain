for i in {1..3}; do
  ./gradlew :slmatrix:test --tests "org.gabrieljones.scalarain.MainBenchmark.benchmarkRunLoop" --rerun-tasks > /dev/null 2>&1
  cat slmatrix/build/reports/tests/test/org.gabrieljones.scalarain.MainBenchmark/benchmarkRunLoop\(\).html | grep -o 'Benchmark Result: .*'
done

## 2026-02-22 - [Performance Regression Mystery]
**Learning:** Every optimization attempt (Random optimization, Drops flattening, Hoisting invariants) has resulted in significant performance regression (40-50%).
**Insight:** The codebase seems extremely sensitive to changes. It is possible that the original baseline measurement was an outlier, or that any change to the bytecode structure is de-optimizing the HotSpot compilation (e.g. causing method to be too large for inlining, or changing loop unrolling decisions). Or maybe the "Baseline" was measured on a "hot" JVM and subsequent runs are "cold"? No, I am running a new Gradle daemon or new JVM for each test run? Gradle daemon persists.
**Action:** Revert to original state. measure baseline again multiple times to establish a stable baseline.

## 2026-02-22 - [Optimization Success: Random Bit Reuse]
**Learning:** Re-measuring the baseline with proper JIT warmup (1000 frames) revealed the true baseline was ~2570 FPS. The "Random Bit Reuse" optimization (using `nextLong` and consuming 7 bits at a time) achieved ~2699 FPS (+5%).
**Insight:** Initial benchmarks with short warmup were misleading. Proper benchmarking is crucial. The simple `nextLong()` bit buffering is indeed faster than `nextInt()` per pixel in Scala because it reduces method call overhead by factor of 9, outweighing the bit manipulation cost.
**Action:** Apply and submit "Random Bit Reuse".

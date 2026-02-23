## 2026-02-22 - [Optimization Success: Random Bit Reuse with Encapsulation]
**Learning:** Encapsulating the bit buffering logic into a `RandomBitBuffer` class with an `inline` method did not significantly degrade performance compared to the inline version (740 FPS vs 724 FPS in the short run test, assuming comparable conditions).
**Insight:** Scala 3's `inline` keyword effectively eliminates the method call overhead for small helper methods, allowing for cleaner code organization without sacrificing performance.
**Action:** Submit the encapsulated version.

## 2025-02-23 - Micro-optimizations in Tight Loops Regression
**Learning:** Attempted several micro-optimizations in `Main.scala` render loop including:
1. Unrolling the random number generation loop (regressed by ~10%).
2. Flattening `charCache` from `Array[Array]` to `Array` (regressed by ~20%).
3. Flattening `colorBuffer` and `charIndexBuffer` (no significant change).
4. Replacing manual bit-buffering (`next7Bits`) with `ThreadLocalRandom.nextInt()` (regressed by ~12%).
5. Padding character sets to power-of-2 to replace modulo with bitwise AND (regressed by ~20%).

**Insight:** The existing `next7Bits` manual buffering implementation is highly efficient for this specific use case (consuming small entropy chunks). Standard library calls or larger unrolled loops introduce overhead (instruction cache pressure or method call overhead) that outweighs the theoretical benefits. The JVM's JIT seems to optimize the existing compact loop structure very well. Increasing data structure size (padding) hurts cache locality more than the instruction savings help.

**Action:** Avoid replacing custom bit-buffering with standard library calls in extremely hot loops without verifying on the target JVM. Respect the existing "compact code" structure as it likely fits L1i cache better.

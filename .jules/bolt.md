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

## 2026-02-27 - Manual Loop Hoisting Wins
**Learning:** In the tight render loop of `Main.scala`, hoisting `frameContext.rows` (field access) and `sets.length` (extension method on opaque type) into local variables (`rows`, `setsLength`) improved performance by ~9% (2415 FPS -> 2633 FPS).
**Insight:** Even with JIT, repeated access to fields or extension methods on opaque types in critical loops can carry overhead (e.g., if the JIT fails to inline or hoist them due to complexity or call depth). Manual hoisting guarantees the values are in local registers.
**Failures:**
- Encapsulating LCG in `RandomBitBuffer` class (even with `inline`) caused regression.
- Flattening `charCache` (2D -> 1D array) caused regression (~800 FPS), possibly due to manual index arithmetic overhead vs JVM's optimized 2D array access.
- Inline 14-bit extraction from LCG seed caused regression, likely due to increased register pressure or instruction count in the hot path.
**Action:** Prefer simple manual hoisting of loop invariants. Be cautious with "clever" bitwise or structure flattening optimizations in tight loops where the JVM's default handling of 2D arrays and simple inlining is already highly optimized.

## 2026-03-01 - [Fast Bounded Random Generation]
**Learning:** `ThreadLocalRandom.current().nextInt(bound)` incurs significant overhead in hot render loops due to both thread-local lookups and the internal bounds-checking/modulo arithmetic. While bitwise masking works well for powers of 2 (e.g., `& 127`), mapping a uniform random number to an arbitrary bound (like the length of a character set) is much faster using Lemire's multiplication-shift method: `((next31Bits().toLong * bound.toLong) >>> 31).toInt`.
**Action:** When bounded random integers are required in critical paths, use an inline 31-bit LCG combined with the long multiplication-shift mapping technique instead of standard JDK `Random` methods.

## 2026-03-03 - [Optimization Success: Array Flattening for Cache Locality]
**Learning:** Flattening a 2D array (`Array[Array[Int]]`) representing a collection of small structs (e.g., drops with `x`, `y`, `vx`, `vy` components) into a 1D `Array[Int]` with a stride significantly improves performance in hot loops (e.g., from ~1300 FPS to ~2600 FPS in the benchmark).
**Insight:** This data-oriented design (DoD) optimization reduces memory fragmentation, eliminates object overhead per element, and maximizes CPU cache locality. The JVM's JIT compiler can aggressively optimize sequential access to primitive 1D arrays compared to dereferencing nested arrays.
**Action:** When managing collections of simple value-like data (structs) accessed sequentially in performance-critical loops, prefer a single flattened primitive array over arrays of objects or 2D arrays to eliminate object header overhead and improve memory access patterns.

## 2026-03-05 - [Optimization Success: Loop Hoisting sets.length]
**Learning:** Hoisting `sets.length` into `setsLength` and using it in `nextBounded(setsLength)` inside the drops loop reduces property access overhead in the tight render loop.
**Insight:** Even minor property accesses like `.length` on `sets` can have slight overhead when called in a loop over all drops, especially if `sets` is an opaque type.
**Action:** When a property is constant for a frame, hoist it outside the loop and use the local variable.

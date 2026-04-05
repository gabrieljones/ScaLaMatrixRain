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

## 2026-03-05 - [Optimization Success: Fast Path for Empty Matrix Cells]
**Learning:** In the `Main.scala` render loop, computing random numbers (`next7Bits()`) for every cell before checking if the cell is populated (`state >= 0`) wastes significant CPU cycles. Since the matrix is mostly empty, skipping RNG logic for empty cells increases FPS by ~15%.
**Action:** When iterating over a sparse matrix or grid, always check the state/population condition *before* executing expensive probability or transformation logic.

## 2026-03-05 - [Optimization Success: Avoid Modulo for Unit Velocity]
**Learning:** In the drop advancement loop of `Main.scala`, computing `frameCounter % vX == 0` when `vX` is frequently 1 or -1 incurs an unnecessary division operation. Adding a fast path for `vX == 1 || vX == -1` bypasses the modulo entirely, improving benchmark FPS from ~2805 to ~3072 (almost a 10% gain).
**Action:** In highly repetitive loops, explicitly check for and fast-path operations involving modulo 1 or -1, as they are logically trivial but computationally expensive if evaluated via ALU division.
## 2026-03-05 - [Optimization Success: Random Bit Reuse in Inner Loop]
**Learning:** In the tight inner loop of `Main.scala` (updating the fading/glitching drops), checking the fade probability, glitch probability, and selecting a new random character previously required up to three separate LCG updates (`next7Bits()`, `next7Bits()`, `nextBounded()`).
**Insight:** We can generate a single 31-bit random number (`next31Bits()`) and slice it into multiple parts: 7 bits for fade probability, 7 bits for glitch probability, and 17 bits to multiply with the sets length to act as a bounded random integer. This saves up to 2 LCG state updates per filled pixel.
**Action:** Extract chunks from a single random number generation in extremely hot loops if multiple small random values are needed.

## 2026-03-22 - [Optimization Success: Localizing Array Reads/Writes]
**Learning:** In a tight loop iterating over a flattened array representing structs (e.g. `dropsFlattened`), repeatedly reading and writing to the same array indices (`dropsFlattened(dI)` and `dropsFlattened(dI + 1)`) incurs redundant array bounds checks and memory accesses. Extracting the values into local variables (`pXN`, `pYN`), performing all updates on the local variables, writing back to the array exactly once, and then reusing the local variables for subsequent method calls (`updateChar`, `outOfBounds`) improved benchmark performance from ~2472 FPS to ~2695 FPS (+9%).
**Insight:** Even though array access is fast, the HotSpot JIT compiler cannot always prove that the array elements haven't been modified by other operations (e.g., aliasing or side effects of method calls like `updateChar`), forcing it to re-read the values from memory and perform bounds checks. Explicitly localizing the state to registers/stack variables guarantees the fastest access path.
**Action:** When updating elements of a primitive array within a hot loop, read the values into local variables once, perform all computations on the local variables, write the final result back to the array once, and pass the local variables to any subsequent methods instead of re-reading the array.

## 2026-03-05 - [Optimization Success: Flattening 2D State Arrays for Cache Locality]
**Learning:** Flattening 2D state arrays (`Array.fill(rows, cols)(-1)`) into 1D primitive arrays (`Array.fill(rows * cols)(-1)`) and iterating sequentially using a calculated index (`idx = rowOffset + fx`) avoids object dereferencing overhead and significantly improves CPU cache locality in hot render loops, yielding a ~5% increase in FPS (e.g., ~2684 FPS to ~2820 FPS).
**Insight:** The JVM's JIT compiler optimizes sequential access to large 1D primitive arrays much more effectively than accessing arrays of arrays, as 2D arrays in Java are fundamentally arrays of object references. Memory layout matters greatly in tight rendering loops.
**Action:** When working with grid-based state in critical paths, prefer a single flattened 1D primitive array over an `Array.ofDim` or `Array[Array]`. Iterate with a hoisted `rowOffset` to avoid repeated multiplication per element.

## 2026-03-24 - [Optimization Success: Loop flattening in grid iteration]
**Learning:** In the `Main.scala` render loop, iterating over a flattened 1D array representing a 2D grid (`colorBuffer`) using a single flat loop (`idx` from `0` to `rows * cols`) is slightly faster than using nested `fx` and `fy` loops with a hoisted `rowOffset`. The benchmark FPS increased from ~2574 to ~2645.
**Insight:** Simplifying control flow by using a single loop variable reduces the number of loop conditions and variable updates, which allows the JIT compiler to optimize the loop more effectively. Calculating `px = idx % cols` and `py = idx / cols` only when a populated cell is found (which is rare since the grid is mostly empty) shifts the cost out of the main iteration path.
**Action:** When iterating over a sparse grid represented as a 1D array, use a single loop with a flat index and calculate 2D coordinates only when necessary.

## 2026-03-24 - [Optimization Success: Skipping redundant character selection for stationary drops]
**Learning:** In the `Main.scala` drops advancement loop, drops with velocities > 1 are artificially slowed down using modulo logic (`frameCounter % v == 0`). In frames where a drop doesn't actually move, generating a new random character index (`charIndex`) via `nextBounded(setsLength)` causes unnecessary RNG overhead. By evaluating whether the drop's position has changed (`val moved = pXN != pXC || pYN != pYC`) and only invoking `nextBounded` if `moved` is true, benchmark FPS increased from ~2645 to ~2785.
**Insight:** Bypassing RNG calls by reusing existing state when logical state hasn't changed is a highly effective optimization in hot loops.
**Action:** When working with simulated physics or delayed updates in render loops, conditionally bypass random property generation if the entity hasn't logically updated its position/state.

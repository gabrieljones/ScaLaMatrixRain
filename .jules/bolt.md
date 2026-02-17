## 2026-02-16 - Optimize JVM 2D Array Access
**Learning:** In Scala (and JVM), 2D arrays are arrays of references to other arrays. Accessing `arr(x)(y)` when iterating row-by-row (`y` then `x`) causes cache thrashing because `arr(x)` is a different array object for each inner iteration step.
**Action:** Always align 2D array dimensions with the primary iteration order. If iterating by rows first (y then x), use `arr(y)(x)` (Row-Major) layout to keep the inner loop accessing contiguous memory in the same array object.

## 2026-10-24 - Hoist JVM 2D Array Lookups
**Learning:** Even with Row-Major layout, `arr(y)(x)` in a nested loop performs `arr(y)` lookup (loading the inner array reference) for every pixel.
**Action:** Hoist the inner array reference `val row = arr(y)` to the outer loop to reduce array pointer chasing and bounds checks by a factor of `width`.

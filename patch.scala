<<<<<<< SEARCH
    // Optimization: Use primitive arrays for color state (int) and character INDEX (int)
    // -1 represents empty/default state
    var colorBuffer = Array.fill(frameContext.rows, frameContext.cols)(-1)
    // Use Int to store index into 'sets', instead of Char
    var charIndexBuffer = Array.ofDim[Int](frameContext.rows, frameContext.cols)

    def updateChar(x: Int, y: Int, charIndex: Int, state: Int): Unit = {
      if (x >= 0 && x < frameContext.cols && y >= 0 && y < frameContext.rows) {
        val c = charCache(state)(charIndex)
        rainGraphics.setCharacter(x, y, c)

        colorBuffer(y)(x) = state
        if (state >= 0) {
           charIndexBuffer(y)(x) = charIndex
        }
      }
    }
=======
    // Optimization: Use primitive arrays for color state (int) and character INDEX (int)
    // Flattened 2D array to 1D array to improve cache locality and avoid multi-dimensional array lookup overhead
    // -1 represents empty/default state
    var colorBuffer = Array.fill(frameContext.rows * frameContext.cols)(-1)
    // Use Int to store index into 'sets', instead of Char
    var charIndexBuffer = new Array[Int](frameContext.rows * frameContext.cols)

    def updateChar(x: Int, y: Int, charIndex: Int, state: Int, rows: Int, cols: Int): Unit = {
      if (x >= 0 && x < cols && y >= 0 && y < rows) {
        val c = charCache(state)(charIndex)
        rainGraphics.setCharacter(x, y, c)

        val idx = y * cols + x
        colorBuffer(idx) = state
        if (state >= 0) {
           charIndexBuffer(idx) = charIndex
        }
      }
    }
>>>>>>> REPLACE

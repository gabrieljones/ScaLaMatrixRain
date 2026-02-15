package org.gabrieljones.scalarain

import com.googlecode.lanterna.terminal.Terminal

class FrameContext(terminal: Terminal, maxCodePointWidth: Int) {

  var cols: Int = 0
  var rows: Int = 0

  def h = rows
  def w = cols

  update(terminal)

  def update(terminal: Terminal): Unit = {
    val terminalSize = terminal.getTerminalSize
    cols = terminalSize.getColumns - maxCodePointWidth + 1 // prevent wrapping codepoints written to the right edge
    rows = terminalSize.getRows
  }

}

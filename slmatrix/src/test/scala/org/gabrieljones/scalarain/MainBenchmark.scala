package org.gabrieljones.scalarain

import org.junit.jupiter.api.Test
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal
import com.googlecode.lanterna.TerminalSize
import java.util.concurrent.TimeUnit

class MainBenchmark {

  @Test // Uncomment to run benchmark manually
  def benchmarkRunLoop(): Unit = {
    // Create a virtual terminal with a fixed size
    val terminal = new DefaultVirtualTerminal(new TerminalSize(120, 40))

    // Configure options to run only 'rain' scene for 500 frames
    // frameInterval = 0 means unthrottled execution
    val options = Options(
      scenes = Seq("rain"),
      maxFrames = 500,
      frameInterval = 0
    )

    val sets = Options.parseWeightedSets(options.unicodeChars)

    val colorContext = ColorContext.resolve(options.fadeColor)

    // Warmup
    val warmupOptions = options.copy(maxFrames = 10)
    Main.runLoop(warmupOptions, terminal, sets, colorContext)

    // Measure
    val start = System.nanoTime()
    Main.runLoop(options, terminal, sets, colorContext)
    val end = System.nanoTime()

    val durationMs = TimeUnit.NANOSECONDS.toMillis(end - start)
    val fps = if (durationMs > 0) (options.maxFrames * 1000.0) / durationMs else 0

    println(s"Benchmark Result: ${durationMs} ms for ${options.maxFrames} frames (~$fps FPS)")
  }
}

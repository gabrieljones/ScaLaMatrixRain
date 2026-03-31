package org.gabrieljones.scalarain

import org.junit.jupiter.api.Test
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal
import com.googlecode.lanterna.TerminalSize
import java.util.concurrent.TimeUnit

class MainBenchmark {

  @Test
  def benchmarkRunLoop(): Unit = {
    // Create a virtual terminal with a fixed size
    val terminal = new DefaultVirtualTerminal(new TerminalSize(120, 40))

    // Configure options to run only 'rain' scene for 500 frames
    // frameInterval = 0 means unthrottled execution
    val options = Options(
      scenes = Seq("rain"),
      maxFrames = 2000,
      frameInterval = 0
    )

    val sets = Options.parseWeightedSets(options.unicodeChars)

    val colorContext = ColorContext.resolve(options.fadeColor)

    // Warmup
    val warmupOptions = options.copy(maxFrames = 1000)
    Main.runLoop(warmupOptions, terminal, sets, colorContext)

    // Measure
    var start = System.nanoTime()
    Main.runLoop(options, terminal, sets, colorContext)
    var end = System.nanoTime()

    val durationMs1 = TimeUnit.NANOSECONDS.toMillis(end - start)
    val fps1 = if (durationMs1 > 0) (options.maxFrames * 1000.0) / durationMs1 else 0

    start = System.nanoTime()
    Main.runLoop(options, terminal, sets, colorContext)
    end = System.nanoTime()

    val durationMs2 = TimeUnit.NANOSECONDS.toMillis(end - start)
    val fps2 = if (durationMs2 > 0) (options.maxFrames * 1000.0) / durationMs2 else 0


    println(s"Benchmark Result 1: ${durationMs1} ms for ${options.maxFrames} frames (~$fps1 FPS)")
    println(s"Benchmark Result 2: ${durationMs2} ms for ${options.maxFrames} frames (~$fps2 FPS)")
  }
}

package com.wearwash.app.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class DeviceFailureArtifactsRule : TestWatcher() {
    override fun failed(error: Throwable, description: Description) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val outputDirectory = InstrumentationRegistry.getArguments()
            .getString("additionalTestOutputDir")
            ?.let(::File)
            ?: File(
                instrumentation.targetContext.getExternalFilesDir(null),
                "test-artifacts",
            )

        runCatching { outputDirectory.mkdirs() }
        val stem = "${description.className}-${description.methodName}"
            .replace(Regex("[^A-Za-z0-9._-]"), "_")

        runCatching {
            device.takeScreenshot(File(outputDirectory, "$stem.png"))
        }
        runCatching {
            File(outputDirectory, "$stem-logcat.txt").writeText(
                device.executeShellCommand("logcat -d -v threadtime -t 500"),
            )
        }
        runCatching {
            File(outputDirectory, "$stem-failure.txt").writeText(
                error.stackTraceToString(),
            )
        }
    }
}

package com.wearwash.app.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import android.view.WindowManager
import com.wearwash.app.MainActivity
import com.wearwash.app.R
import com.wearwash.app.WearWashApplication
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreCareCycleDeviceTest {
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var device: UiDevice
    private lateinit var app: WearWashApplication

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        app = instrumentation.targetContext.applicationContext as WearWashApplication
        val repository = app.appContainer.itemRepository
        runBlocking {
            repository.observeActiveItems().first().forEach { item ->
                repository.archiveItem(item.id, OffsetDateTime.now().toString())
            }
            repository.observeActiveItems().first { it.isEmpty() }
        }
        device = UiDevice.getInstance(instrumentation)
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        assertTrue(device.wait(Until.hasObject(By.text(text(R.string.add_item))), TIMEOUT))
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun registerUseBasketAndWashCompletesOnDevice() {
        val itemName = "Device test shirt"

        clickAction(R.string.add_item)
        assertTrue(device.wait(Until.hasObject(By.text(text(R.string.item_name))), TIMEOUT))
        val nameField = device.findObject(
            UiSelector().className("android.widget.EditText").instance(0),
        )
        nameField.click()
        nameField.setText(itemName)
        device.pressBack()
        val saveButton = By.text(text(R.string.save))
        assertTrue(device.wait(Until.hasObject(saveButton), TIMEOUT))
        clickAction(R.string.save)
        assertTrue(device.wait(Until.hasObject(By.text(itemName)), TIMEOUT))

        repeat(3) {
            clickAction(R.string.used_today)
            device.waitForIdle()
        }
        assertTrue(device.wait(Until.hasObject(By.text(text(R.string.needs_washing))), TIMEOUT))

        clickAction(R.string.laundry_basket_title)
        clickAction(R.string.add_to_basket)
        assertTrue(device.wait(Until.hasObject(By.text(itemName)), TIMEOUT))

        clickAction(R.string.wash_all)
        val confirmWash = By.text(text(R.string.mark_washed))
        assertTrue(device.wait(Until.hasObject(confirmWash), TIMEOUT))
        clickAction(R.string.mark_washed)

        assertTrue(device.wait(Until.hasObject(By.text(text(R.string.basket_empty))), TIMEOUT))
        assertTrue(device.wait(Until.hasObject(By.text(text(R.string.no_suggestions))), TIMEOUT))
    }

    private fun text(resourceId: Int): String = app.getString(resourceId)

    private fun clickAction(resourceId: Int) {
        val selector = By.text(text(resourceId))
        assertTrue(device.wait(Until.hasObject(selector), TIMEOUT))
        val target = device.findObjects(selector).firstNotNullOfOrNull { node ->
            generateSequence(node) { it.parent }.firstOrNull { it.isClickable }
        }
        checkNotNull(target) { "No clickable ancestor for '${text(resourceId)}'" }
        target.click()
    }

    private companion object {
        const val TIMEOUT = 8_000L
    }
}

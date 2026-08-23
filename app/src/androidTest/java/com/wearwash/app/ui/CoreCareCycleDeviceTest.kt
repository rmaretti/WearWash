package com.wearwash.app.ui

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.wearwash.app.MainActivity
import com.wearwash.app.R
import com.wearwash.app.WearWashApplication
import com.wearwash.app.data.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreCareCycleDeviceTest {
    private lateinit var device: UiDevice
    private lateinit var app: WearWashApplication
    private lateinit var repository: ItemRepository

    @get:Rule
    val failureArtifacts = DeviceFailureArtifactsRule()

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        app = instrumentation.targetContext.applicationContext as WearWashApplication
        repository = app.appContainer.itemRepository
        runBlocking {
            check(repository.observeActiveItems().first().isEmpty()) {
                "Device test requires isolated empty app data; refusing to mutate existing records"
            }
        }
        device = UiDevice.getInstance(instrumentation)
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        app.startActivity(
            Intent(app, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            ),
        )
        instrumentation.waitForIdleSync()
        assertTrue(findTargets(R.string.add_item).isNotEmpty())
    }

    @After
    fun tearDown() {
        if (::device.isInitialized) {
            device.pressHome()
        }
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
        assertTrue(device.wait(Until.gone(By.text(text(R.string.used_today))), TIMEOUT))

        val itemCheckbox = device.findObject(
            UiSelector().className("android.widget.CheckBox").instance(0),
        )
        assertTrue(itemCheckbox.waitForExists(TIMEOUT))
        itemCheckbox.click()
        assertTrue(findTargets(R.string.used_today).isNotEmpty())

        repeat(3) { index ->
            clickAction(R.string.used_today)
            waitForUsesSinceWash(index + 1)
            device.waitForIdle()
        }
        assertTrue(findTargets(R.string.needs_washing).isNotEmpty())

        clickAction(R.string.laundry_basket_title)
        val suggestedItemCheckbox = device.findObject(
            UiSelector().className("android.widget.CheckBox").instance(0),
        )
        assertTrue(suggestedItemCheckbox.waitForExists(TIMEOUT))
        suggestedItemCheckbox.click()
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

    private fun waitForUsesSinceWash(expected: Int) {
        runBlocking {
            withTimeout(TIMEOUT) {
                repository.observeActiveItems().first { items ->
                    items.singleOrNull()?.usesSinceWash == expected
                }
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun clickAction(resourceId: Int) {
        val target = findTargets(resourceId).firstNotNullOfOrNull { node ->
            generateSequence(node) { it.parent }.firstOrNull { it.isClickable }
        }
        checkNotNull(target) { "No clickable ancestor for '${text(resourceId)}'" }
        target.click()
    }

    private fun findTargets(resourceId: Int): List<androidx.test.uiautomator.UiObject2> {
        val label = text(resourceId)
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        do {
            val matches = device.findObjects(By.text(label)) + device.findObjects(By.desc(label))
            if (matches.isNotEmpty()) {
                return matches.distinctBy { it.hashCode() }
            }
            SystemClock.sleep(POLL_INTERVAL)
        } while (SystemClock.uptimeMillis() < deadline)
        return emptyList()
    }

    private companion object {
        const val TIMEOUT = 8_000L
        const val POLL_INTERVAL = 100L
    }
}

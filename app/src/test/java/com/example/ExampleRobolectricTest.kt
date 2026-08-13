package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.TransactionEntity
import com.example.ui.TransactionsTab
import com.example.ui.TrendsTab
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testTrendsTabRendering() {
    val cal = Calendar.getInstance()
    val mockTransactions = listOf(
      TransactionEntity(
        id = 1,
        amount = 1500.0,
        type = "EXPENSE",
        category = "Shopping",
        payeeOrSource = "Amazon",
        dateMillis = cal.timeInMillis,
        note = "bought goods"
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TrendsTab(transactions = mockTransactions)
      }
    }

    // Verify daily spends chart compiles and renders correctly
    composeTestRule.onNodeWithTag("daily_spend_line_chart").assertExists()
  }

  @Test
  fun testTransactionsTabWithEditRendering() {
    val cal = Calendar.getInstance()
    val mockTransactions = listOf(
      TransactionEntity(
        id = 1,
        amount = 1500.0,
        type = "EXPENSE",
        category = "Shopping",
        payeeOrSource = "Amazon",
        dateMillis = cal.timeInMillis,
        note = "bought goods"
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TransactionsTab(
          transactions = mockTransactions,
          onAddTransaction = { _, _, _, _, _, _ -> },
          onDeleteTransaction = {},
          onUpdateTransaction = {},
          onClearAllTransactions = {}
        )
      }
    }

    // Verify edit button is rendered
    composeTestRule.onNodeWithTag("edit_transaction_button").assertExists()
  }
}

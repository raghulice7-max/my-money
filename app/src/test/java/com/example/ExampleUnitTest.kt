package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testSmsParsing() {
    val sms1 = "Rs.31.00 spent on your SBI Credit Card ending with 0736 at METROPOLITANTRANSPORT on 25-05-26 via UPI (Ref No. 123655729464). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val sms2 = "Rs.100.00 spent on your SBI Credit Card ending with 0736 at AbdulSalam on 25-05-26 via UPI (Ref No. 123690202243). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val sms3 = "Rs.60.00 spent on your SBI Credit Card ending with 0736 at KamatchiK on 26-05-26 via UPI (Ref No. 123707137380). Trxn. not done by you? Report at https://sbicard.com/Dispute"

    val receiver = SmsReceiver()
    val parsed1 = receiver.parseSms(sms1)
    val parsed2 = receiver.parseSms(sms2)
    val parsed3 = receiver.parseSms(sms3)

    println("Parsed 1: payee=${parsed1?.payeeOrMerchant}, amount=${parsed1?.amount}, type=${parsed1?.type}")
    println("Parsed 2: payee=${parsed2?.payeeOrMerchant}, amount=${parsed2?.amount}, type=${parsed2?.type}")
    println("Parsed 3: payee=${parsed3?.payeeOrMerchant}, amount=${parsed3?.amount}, type=${parsed3?.type}")

    assertNotNull("Parsed1 should not be null", parsed1)
    assertEquals("METROPOLITANTRANSPORT", parsed1!!.payeeOrMerchant)

    assertNotNull("Parsed2 should not be null", parsed2)
    assertEquals("AbdulSalam", parsed2!!.payeeOrMerchant)

    assertNotNull("Parsed3 should not be null", parsed3)
    assertEquals("KamatchiK", parsed3!!.payeeOrMerchant)
  }
}



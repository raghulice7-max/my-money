package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testSmsParsing() {
    val sms1 = "Rs.31.00 spent on your SBI Credit Card ending with 0736 at METROPOLITANTRANSPORT on 25-05-26 via UPI (Ref No. 123655729464). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val sms2 = "Rs.100.00 spent on your SBI Credit Card ending with 0736 at AbdulSalam on 25-05-26 via UPI (Ref No. 123690202243). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val sms3 = "Rs.60.00 spent on your SBI Credit Card ending with 0736 at KamatchiK on 26-05-26 via UPI (Ref No. 123707137380). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    
    // User attached screenshot SMS messages
    val userSms1 = "Rs.20.00 spent on your SBI Credit Card ending with 0736 at CHENNAIYANC on 31-05-26 via UPI (Ref No. 123974706666). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val userSms2 = "Rs.98.00 spent on your SBI Credit Card ending with 0736 at APOLLOPHARMACY on 31-05-26 via UPI (Ref No. 124004769265). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val userSms3 = "Rs.10.00 spent on your SBI Credit Card ending with 0736 at GOPINATHM on 01-06-26 via UPI (Ref No. 12400856188). Trxn. not done by you? Report at https://sbicard.com/Dispute"

    // Newest user screenshot SMS messages from 02-06-26 and 03-06-26
    val userSmsJune2A = "Rs.34.00 spent on your SBI Credit Card ending with 0736 at MUHAMMEDMANSOORCK on 02-06-26 via UPI (Ref No. 124098935480). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val userSmsJune2B = "Rs.174.00 spent on your SBI Credit Card ending with 0736 at RMPMITTAIKADAI on 02-06-26 via UPI (Ref No. 124099777194). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val userSmsJune3A = "Rs.50.00 spent on your SBI Credit Card ending with 0736 at MUHAMMEDMANSOORCK on 03-06-26 via UPI (Ref No. 124152600957). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    val userSmsJune3B = "Rs.30.00 spent on your SBI Credit Card ending with 0736 at JKENTERPRISES on 03-06-26 via UPI (Ref No. 124152801201). Trxn. not done by you? Report at https://sbicard.com/Dispute"
    
    // Stylized font SMS that was previously not auto-detected
    val userSmsStylized = "Rs.20.00 \uD835\uDE00\uD835\uDDFD\uD835\uDDF2\uD835\uDDFB\uD835\uDE01 \uD835\uDDFC\uD835\uDDFB \uD835\uDE06\uD835\uDDFC\uD835\uDE02\uD835\uDE05 \uD835\uDDF0\uD835\uDDEE\uD835\uDE05\uD835\uDDF1 ending with 0736 at MUHAMMEDMANSOORCK on 13-06-26 via UPI (Ref No. 124691890021)."

    val receiver = SmsReceiver()
    val parsed1 = receiver.parseSms(sms1)
    val parsed2 = receiver.parseSms(sms2)
    val parsed3 = receiver.parseSms(sms3)
    
    val parsedUser1 = receiver.parseSms(userSms1)
    val parsedUser2 = receiver.parseSms(userSms2)
    val parsedUser3 = receiver.parseSms(userSms3)

    val parsedJune2A = receiver.parseSms(userSmsJune2A)
    val parsedJune2B = receiver.parseSms(userSmsJune2B)
    val parsedJune3A = receiver.parseSms(userSmsJune3A)
    val parsedJune3B = receiver.parseSms(userSmsJune3B)
    
    val parsedStylized = receiver.parseSms(userSmsStylized)

    println("Parsed 1: payee=${parsed1?.payeeOrMerchant}, amount=${parsed1?.amount}, type=${parsed1?.type}")
    println("Parsed 2: payee=${parsed2?.payeeOrMerchant}, amount=${parsed2?.amount}, type=${parsed2?.type}")
    println("Parsed 3: payee=${parsed3?.payeeOrMerchant}, amount=${parsed3?.amount}, type=${parsed3?.type}")
    
    println("Parsed User 1: payee=${parsedUser1?.payeeOrMerchant}, amount=${parsedUser1?.amount}, type=${parsedUser1?.type}")
    println("Parsed User 2: payee=${parsedUser2?.payeeOrMerchant}, amount=${parsedUser2?.amount}, type=${parsedUser2?.type}")
    println("Parsed User 3: payee=${parsedUser3?.payeeOrMerchant}, amount=${parsedUser3?.amount}, type=${parsedUser3?.type}")

    println("Parsed June 2A: payee=${parsedJune2A?.payeeOrMerchant}, amount=${parsedJune2A?.amount}, type=${parsedJune2A?.type}")
    println("Parsed June 2B: payee=${parsedJune2B?.payeeOrMerchant}, amount=${parsedJune2B?.amount}, type=${parsedJune2B?.type}")
    println("Parsed June 3A: payee=${parsedJune3A?.payeeOrMerchant}, amount=${parsedJune3A?.amount}, type=${parsedJune3A?.type}")
    println("Parsed June 3B: payee=${parsedJune3B?.payeeOrMerchant}, amount=${parsedJune3B?.amount}, type=${parsedJune3B?.type}")

    assertNotNull("Parsed1 should not be null", parsed1)
    assertEquals("METROPOLITANTRANSPORT", parsed1!!.payeeOrMerchant)

    assertNotNull("Parsed2 should not be null", parsed2)
    assertEquals("AbdulSalam", parsed2!!.payeeOrMerchant)

    assertNotNull("Parsed3 should not be null", parsed3)
    assertEquals("KamatchiK", parsed3!!.payeeOrMerchant)
    
    // Validate User SMS
    assertNotNull("ParsedUser1 should not be null", parsedUser1)
    assertEquals("CHENNAIYANC", parsedUser1!!.payeeOrMerchant)
    assertEquals(20.00, parsedUser1.amount, 0.0)
    assertEquals("EXPENSE", parsedUser1.type)

    assertNotNull("ParsedUser2 should not be null", parsedUser2)
    assertEquals("APOLLOPHARMACY", parsedUser2!!.payeeOrMerchant)
    assertEquals(98.00, parsedUser2.amount, 0.0)
    assertEquals("EXPENSE", parsedUser2.type)

    assertNotNull("ParsedUser3 should not be null", parsedUser3)
    assertEquals("GOPINATHM", parsedUser3!!.payeeOrMerchant)
    assertEquals(10.00, parsedUser3.amount, 0.0)
    assertEquals("EXPENSE", parsedUser3.type)

    // Validate Newest SMS from screenshot
    assertNotNull("parsedJune2A should not be null", parsedJune2A)
    assertEquals("MUHAMMEDMANSOORCK", parsedJune2A!!.payeeOrMerchant)
    assertEquals(34.0, parsedJune2A.amount, 0.0)

    assertNotNull("parsedJune2B should not be null", parsedJune2B)
    assertEquals("RMPMITTAIKADAI", parsedJune2B!!.payeeOrMerchant)
    assertEquals(174.0, parsedJune2B.amount, 0.0)

    assertNotNull("parsedJune3A should not be null", parsedJune3A)
    assertEquals("MUHAMMEDMANSOORCK", parsedJune3A!!.payeeOrMerchant)
    assertEquals(50.0, parsedJune3A.amount, 0.0)

    assertNotNull("parsedJune3B should not be null", parsedJune3B)
    assertEquals("JKENTERPRISES", parsedJune3B!!.payeeOrMerchant)
    assertEquals(30.0, parsedJune3B.amount, 0.0)

    assertNotNull("parsedStylized should not be null", parsedStylized)
    assertEquals("MUHAMMEDMANSOORCK", parsedStylized!!.payeeOrMerchant)
    assertEquals(20.0, parsedStylized.amount, 0.0)
    assertEquals("EXPENSE", parsedStylized.type)
  }

  @Test
  fun testSavingsAndInvestmentSmsParsing() {
    val smsInvestment = "Rs.5000.00 spent at Zerodha for Mutual Funds."
    val parsedInvestment = SmsReceiver().parseSms(smsInvestment)
    assertNotNull("parsed investment sms should not be null", parsedInvestment)
    assertEquals("Investment", parsedInvestment!!.guessedCategory)
    assertEquals(5000.0, parsedInvestment.amount, 0.0)

    val smsSavings = "Rs.10000.00 credited to Savings Account."
    val parsedSavings = SmsReceiver().parseSms(smsSavings)
    assertNotNull("parsed savings sms should not be null", parsedSavings)
    assertEquals("Savings", parsedSavings!!.guessedCategory)
    assertEquals(10000.0, parsedSavings.amount, 0.0)
  }
}



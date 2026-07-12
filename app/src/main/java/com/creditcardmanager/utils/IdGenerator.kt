package com.creditcardmanager.utils

import java.util.UUID

object IdGenerator {
    fun generate(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"
    fun generateBankId() = generate("bank")
    fun generateCardId() = generate("card")
    fun generateTagId() = generate("tag")
    fun generateTransactionId() = generate("txn")
    fun generateActivityId() = generate("act")
    fun generateReminderId() = generate("rem")
}

package com.xenonware.phone.util

import android.content.Context
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumberFormatter {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    fun formatForDisplay(rawNumber: String, context: Context): String {
        if (rawNumber.isBlank()) return rawNumber.trim()

        val cleaned = rawNumber.replace(Regex("[^+0-9]"), "").trim()

        if (cleaned.isEmpty()) return rawNumber.trim()

        val region = context.resources.configuration.locales.get(0).country ?: "DE"

        val parsedNumber = try {
            phoneUtil.parse(cleaned, region)
        } catch (_: Exception) {
            null
        }

        val isValid = parsedNumber != null && phoneUtil.isValidNumber(parsedNumber)

        return when {
            rawNumber.startsWith("+") -> {
                if (isValid) {
                    phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                } else {
                    addSpaces(cleaned)
                }
            }

            rawNumber.startsWith("0") -> {
                if (isValid) {
                    phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
                } else {
                    addSpaces(cleaned)
                }
            }

            else -> {
                addSpaces(cleaned)
            }
        }
    }

    private fun addSpaces(number: String): String {
        if (number.length <= 4) return number

        val prefixLen = when {
            number.startsWith("+") -> 3
            number.startsWith("0") -> 4
            else -> 0
        }

        val prefix = number.take(prefixLen)
        val rest = number.drop(prefixLen)

        return prefix + if (rest.isNotEmpty()) " " + rest.chunked(4).joinToString(" ") else ""
    }
}
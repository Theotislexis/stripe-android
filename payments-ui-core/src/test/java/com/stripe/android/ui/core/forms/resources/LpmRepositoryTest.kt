package com.stripe.android.ui.core.forms.resources

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.ui.core.elements.EmptyFormSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {
    private val lpmRepository = LpmRepository(
        ApplicationProvider.getApplicationContext<Application>().resources,
        object : IsFinancialConnectionsAvailable {
            override fun invoke(): Boolean {
                return true
            }
        }
    )

    @Test
    fun `Verify failing to read server schema reads from disk`() {
        lpmRepository.update(
            listOf("affirm"),
            """
          [
            {
                "type": "affirm",
                invalid schema
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("affirm")).isNotNull()
    }

    @Test
    fun `Verify field not found in schema is read from disk`() {
        lpmRepository.update(
            listOf("card", "afterpay_clearpay"),
            """
          [
            {
                "type": "afterpay_clearpay",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("afterpay_clearpay")).isNotNull()
        assertThat(lpmRepository.fromCode("card")).isNotNull()
    }

    @Test
    fun `Verify latest server spec`() {
        lpmRepository.update(
            listOf("bancontact", "sofort", "ideal", "sepa_debit", "p24", "eps", "giropay"),
            """
                [
                  {
                    "type": "an lpm",
                    "async": false,
                    "fields": [
                      {
                        "api_path": null,
                        "type": "afterpay_header"
                      }
                    ]
                  }
                ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("sofort")).isNotNull()
        assertThat(lpmRepository.fromCode("ideal")).isNotNull()
        assertThat(lpmRepository.fromCode("bancontact")).isNotNull()
        assertThat(lpmRepository.fromCode("p24")).isNotNull()
        assertThat(lpmRepository.fromCode("eps")).isNotNull()
        assertThat(lpmRepository.fromCode("giropay")).isNotNull()
    }

    @Test
    fun `Repository will contain LPMs in ordered and schema`() {
        lpmRepository.update(
            listOf("afterpay_clearpay"),
            """
          [
            {
                "type": "affirm",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
            },
            {
                "type": "afterpay_clearpay",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("afterpay_clearpay")).isNotNull()
        assertThat(lpmRepository.fromCode("affirm")).isNotNull()
    }

    @Test
    fun `Verify no fields in the default json are ignored the lpms package should be correct`() {
        lpmRepository.updateFromDisk()
        // If this test fails, check to make sure the spec's serializer is added to
        // FormItemSpecSerializer
        LpmRepository.exposedPaymentMethods.forEach { code ->
            if (!hasEmptyForm(code)) {
                assertThat(
                    lpmRepository.fromCode(code)!!.formSpec.items
                        .filter {
                            it is EmptyFormSpec && !hasEmptyForm(code)
                        }

                ).isEmpty()
            }
        }
    }

    private fun hasEmptyForm(code: String) =
        (code == "paypal" || code == "us_bank_account") &&
            lpmRepository.fromCode(code)!!.formSpec.items.size == 1 &&
            lpmRepository.fromCode(code)!!.formSpec.items.first() == EmptyFormSpec

    @Test
    fun `Verify the repository only shows card if in lpms json`() {
        assertThat(lpmRepository.fromCode("card")).isNull()
        lpmRepository.update(
            emptyList(),
            """
          [
            {
                "type": "affirm",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("card")).isNull()
    }

    // TODO(michelleb): Once we have the server implemented in production we can do filtering there instead
    // of in code here.
    @Test
    fun `Verify that unknown LPMs are not shown because not listed as exposed`() {
        lpmRepository.update(
            emptyList(),
            """
              [
                {
                    "type": "affirm",
                    "async": false,
                    "fields": [
                      {
                        "type": "affirm_header"
                      }
                    ]
                  },
                {
                    "type": "unknown_lpm",
                    "async": false,
                    "fields": [
                      {
                        "type": "affirm_header"
                      }
                    ]
                  }
             ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("unknown_lpm")).isNull()
    }

    @Test
    fun `Verify that payment methods hardcoded to delayed remain regardless of json`() {
        lpmRepository.updateFromDisk()
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)

        lpmRepository.update(
            emptyList(),
            """
              [
                {
                    "type": "sofort",
                    "async": false,
                    "fields": []
                  }
             ]
            """.trimIndent()
        )
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)
    }

    @Test
    fun `Verify that us_bank_account is supported when financial connections sdk available`() {
        lpmRepository.initialize(
            """
              [
                {
                  "type": "us_bank_account"
                }
              ]
            """.trimIndent().byteInputStream()
        )

        assertThat(lpmRepository.fromCode("us_bank_account")).isNotNull()
    }

    @Test
    fun `Verify that us_bank_account not supported when financial connections sdk not available`() {
        val lpmRepository = LpmRepository(
            ApplicationProvider.getApplicationContext<Application>().resources,
            object : IsFinancialConnectionsAvailable {
                override fun invoke(): Boolean {
                    return false
                }
            }
        )

        lpmRepository.initialize(
            """
              [
                {
                  "type": "us_bank_account"
                }
              ]
            """.trimIndent().byteInputStream()
        )

        assertThat(lpmRepository.fromCode("us_bank_account")).isNull()
    }
}

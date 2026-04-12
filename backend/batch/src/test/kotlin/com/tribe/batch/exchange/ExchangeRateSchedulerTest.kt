package com.tribe.batch.exchange

import com.ninjasquad.springmockk.MockkBean
import com.tribe.application.auth.EmailSender
import com.tribe.application.exchange.ExchangeRateGateway
import com.tribe.application.exchange.ExchangeRatePayload
import com.tribe.domain.exchange.CurrencyId
import com.tribe.domain.exchange.CurrencyRepository
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ExchangeRateSchedulerTest @Autowired constructor(
    private val exchangeRateScheduler: ExchangeRateScheduler,
    private val currencyRepository: CurrencyRepository,
) {
    @MockkBean
    private lateinit var emailSender: EmailSender

    @MockkBean
    private lateinit var exchangeRateGateway: ExchangeRateGateway

    @BeforeEach
    fun setup() {
        currencyRepository.deleteAll()
    }

    @Test
    @DisplayName("스케줄러 실행 시 환율 정보가 정확히 저장된다")
    fun updateCurrencySuccessAndSavesCorrectly() {
        every { exchangeRateGateway.findExchange(any(), any(), any()) } returns listOf(
            ExchangeRatePayload(1, "USD", "미국 달러", "1,350.50"),
            ExchangeRatePayload(1, "JPY(100)", "일본 옌", "925.33"),
            ExchangeRatePayload(1, "CNY", "중국 위안", "185.00"),
        )

        exchangeRateScheduler.updateCurrency()

        assertThat(currencyRepository.findAll()).hasSize(3)
        val today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).let {
            when (it.dayOfWeek) {
                java.time.DayOfWeek.SATURDAY -> it.minusDays(1)
                java.time.DayOfWeek.SUNDAY -> it.minusDays(2)
                else -> it
            }
        }

        val usd = currencyRepository.findById(CurrencyId("USD", today)).get()
        val jpy = currencyRepository.findById(CurrencyId("JPY", today)).get()
        val cny = currencyRepository.findById(CurrencyId("CNY", today)).get()

        assertThat(usd.exchangeRate).isEqualByComparingTo(BigDecimal("1350.5000"))
        assertThat(jpy.exchangeRate).isEqualByComparingTo(BigDecimal("9.2533"))
        assertThat(cny.exchangeRate).isEqualByComparingTo(BigDecimal("185.0000"))
    }
}

package com.usedmarket

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class UsedMarketApplicationTests {

	@Test
	fun contextLoads() {
		// Verifies the Spring application context starts successfully
		// with all Phase 1 configuration (JWT props, CORS props, DB connection to H2).
	}
}

package com.usedmarket.config

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {

	@GetMapping("/api/health")
	fun health(): Map<String, Any> = mapOf(
		"status" to "UP",
		"service" to "used-marketplace",
		"timestamp" to Instant.now().toString()
	)
}

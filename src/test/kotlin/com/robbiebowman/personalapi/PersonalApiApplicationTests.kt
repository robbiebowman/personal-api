package com.robbiebowman.personalapi

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations= ["classpath:application.properties"])
@MockBeans
class ApplicationTests {

	@Test
	fun contextLoads() {
	}

}

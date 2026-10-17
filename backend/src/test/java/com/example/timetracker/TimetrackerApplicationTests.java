package com.example.timetracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class TimetrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}

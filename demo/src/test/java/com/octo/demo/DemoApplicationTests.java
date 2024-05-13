package com.octo.demo;

import com.octo.cssb.HelloService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

	@Resource
	private HelloService helloService;

	@Test
	void contextLoads() {
		helloService.sayHello();
	}

}

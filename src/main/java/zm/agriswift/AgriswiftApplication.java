package zm.agriswift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgriswiftApplication {

	static void main(String[] args) {
		SpringApplication.run(AgriswiftApplication.class, args);
	}
}
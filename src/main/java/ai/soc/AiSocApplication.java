package ai.soc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiSocApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiSocApplication.class, args);

	}

}

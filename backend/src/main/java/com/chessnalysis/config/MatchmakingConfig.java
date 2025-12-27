package com.chessnalysis.config;

import com.chessnalysis.service.matchmaking.MatchmakingQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for matchmaking-related beans.
 */
@Configuration
public class MatchmakingConfig {

	/**
	 * Create the in-memory matchmaking queue as a singleton bean.
	 */
	@Bean
	public MatchmakingQueue matchmakingQueue() {
		return new MatchmakingQueue();
	}
}


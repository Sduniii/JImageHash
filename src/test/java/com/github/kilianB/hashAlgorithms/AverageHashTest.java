package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AverageHashTest {

	@Nested
	@DisplayName("Algorithm Id")
	class AlgorithmId {
		/**
		 * The algorithms id shall stay consistent throughout different instances of the
		 * jvm. While simple hashcodes do not guarantee this behavior hash codes created
		 * from strings and integers are by contract consistent.
		 */
		@Test
		@DisplayName("Consistent AlgorithmIds")
		public void consistency() {

			assertAll(() -> {
				assertEquals(-1105481375, new AverageHash(14).algorithmId());
			}, () -> {
				assertEquals(-1105480383, new AverageHash(25).algorithmId());
			});
		}
	}
	
	//Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase{

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new AverageHash(bitResolution);
		}
		
	}
	
}

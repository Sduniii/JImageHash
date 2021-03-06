package com.github.kilianB.hashAlgorithms;

import static com.github.kilianB.TestResources.ballon;
import static com.github.kilianB.TestResources.copyright;
import static com.github.kilianB.TestResources.highQuality;
import static com.github.kilianB.TestResources.lowQuality;
import static com.github.kilianB.TestResources.thumbnail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.hashAlgorithms.filter.Kernel;
import com.github.kilianB.matcher.Hash;

/**
 * Base test class applicable to all hashing algorithms
 * 
 * @author Kilian
 *
 */
public abstract class HashTestBase {

	@Nested
	class AlgorithmId {
		/**
		 * The algorithm ids shall not collide
		 */
		@Test
		@DisplayName("Unique AlgorithmsIds")
		public void uniquely() {

			int id0 = getInstance(15).algorithmId();
			int id1 = getInstance(40).algorithmId();
			int id2 = getInstance(60).algorithmId();

			assertAll(() -> {
				assertNotEquals(id0, id1);
			}, () -> {
				assertNotEquals(id0, id2);
			}, () -> {
				assertNotEquals(id1, id2);
			});
		}
	}

	@Nested
	class BitResolution {

		@Test
		void consistentKeyLength() {
			// To get comparable hashes the key length has to be consistent for all
			// resolution of images
			HashingAlgorithm d1 = getInstance(32);

			Hash ballonHash = d1.hash(ballon);
			Hash copyrightHash = d1.hash(copyright);
			Hash lowQualityHash = d1.hash(lowQuality);
			Hash highQualityHash = d1.hash(highQuality);
			Hash thumbnailHash = d1.hash(thumbnail);

			assertAll(() -> {
				assertEquals(ballonHash.getBitResolution(), copyrightHash.getBitResolution());
			}, () -> {
				assertEquals(ballonHash.getBitResolution(), lowQualityHash.getBitResolution());
			}, () -> {
				assertEquals(ballonHash.getBitResolution(), highQualityHash.getBitResolution());
			}, () -> {
				assertEquals(ballonHash.getBitResolution(), thumbnailHash.getBitResolution());
			});
		}

		/**
		 * The hash length of the algorithm is at least the supplied bits long
		 * 
		 * @param hasher
		 */
		@ParameterizedTest
		@MethodSource(value = "com.github.kilianB.hashAlgorithms.HashTestBase#bitResolutionBroad")
		void keyLengthMinimumBits(Integer bitResolution) {
			HashingAlgorithm hasher = getInstance(bitResolution + offsetBitResolution());
			assertTrue(hasher.hash(ballon).getBitResolution() >= hasher.bitResolution);
		}

		@Test
		void illegalConstructor() {
			assertThrows(IllegalArgumentException.class, () -> {
				getInstance(0);
			});
		}
	}

	@Nested
	@DisplayName("Serialization")
	class Serizalization {

		HashingAlgorithm originalAlgo;
		HashingAlgorithm deserializedAlgo;

		@BeforeEach
		void serializeAlgo() {
			originalAlgo = getInstance(32);

			File serFile = new File(originalAlgo.getClass().getName() + ".ser");

			// Write to file
			try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(serFile))) {
				os.writeObject(originalAlgo);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Read from file
			try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(serFile))) {
				deserializedAlgo = (HashingAlgorithm) is.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (serFile.exists()) {
					serFile.delete();
				}
			}
		}

		@Test
		void consistentId() {
			assertEquals(originalAlgo.algorithmId(), deserializedAlgo.algorithmId());
		}

		@Test
		void consistentHash() {
			// Algorithm id is checked in the method beforehand
			assertEquals(originalAlgo.hash(ballon).getHashValue(), deserializedAlgo.hash(ballon).getHashValue());
		}

		// TODO serialize algo with different filters

	}

	@Nested
	class HashTest {
		/**
		 * The hashes produced by the same algorithms shall return the same hash on
		 * successive calls
		 * 
		 * @param bitRes the bit resolution of the algorithm
		 */
		@ParameterizedTest
		@MethodSource(value = "com.github.kilianB.hashAlgorithms.HashTestBase#bitResolution")
		void consitent(Integer bitRes) {
			HashingAlgorithm h = getInstance(bitRes + offsetBitResolution());
			assertEquals(h.hash(ballon).getHashValue(), h.hash(ballon).getHashValue());
		}

		/**
		 * The hamming distance of the same image has to be 0
		 * 
		 * @param bitRes the bit resolution of the algorithm
		 */
		@ParameterizedTest
		@MethodSource(value = "com.github.kilianB.hashAlgorithms.HashTestBase#bitResolution")
		void equalImage(Integer bitRes) {
			HashingAlgorithm h = getInstance(bitRes + offsetBitResolution());
			assertEquals(0, h.hash(ballon).hammingDistance(h.hash(ballon)));
		}

		/**
		 * The hamming distance of similar images shall be lower than the distance of
		 * vastly different pictures
		 * 
		 * @param bitRes the bit resolution of the algorithm
		 */
		@ParameterizedTest
		@MethodSource(value = "com.github.kilianB.hashAlgorithms.HashTestBase#bitResolution")
		void unequalImage(Integer bitRes) {
			HashingAlgorithm h = getInstance(bitRes + offsetBitResolution());
			Hash lowQualityHash = h.hash(lowQuality);
			Hash highQualityHash = h.hash(highQuality);
			Hash ballonHash = h.hash(ballon);

			assertAll(() -> {
				assertTrue(
						lowQualityHash.hammingDistance(highQualityHash) < lowQualityHash.hammingDistance(ballonHash));
			}, () -> {
				assertTrue(
						highQualityHash.hammingDistance(lowQualityHash) < highQualityHash.hammingDistance(ballonHash));
			});
		}
	}

	@Nested
	class Filter {

		/**
		 * May not add filter after id has been calculated
		 */
		@Test
		void filterAddInvalid() {
			HashingAlgorithm hasher = getInstance(16);
			hasher.algorithmId();
			Kernel k = Kernel.identityFilter();
			assertThrows(IllegalStateException.class, () -> {
				hasher.addFilter(k);
			});
		}

		/**
		 * May not add filter after hashing operation
		 */
		@Test
		void filterAddInvalidHash() {
			HashingAlgorithm hasher = getInstance(16);
			hasher.hash(new BufferedImage(1, 1, 0x1));
			Kernel k = Kernel.identityFilter();
			assertThrows(IllegalStateException.class, () -> {
				hasher.addFilter(k);
			});
		}

		/**
		 * May not remove filter after id has been calculated
		 */
		@Test
		void filterRemoveInvalid() {
			HashingAlgorithm hasher = getInstance(16);
			hasher.algorithmId();
			Kernel k = Kernel.identityFilter();
			assertThrows(IllegalStateException.class, () -> {
				hasher.removeFilter(k);
			});
		}

		/**
		 * May not remove filter after hashing operation
		 */
		@Test
		void filterRemoveInvalidHash() {
			HashingAlgorithm hasher = getInstance(16);
			hasher.hash(new BufferedImage(1, 1, 0x1));
			Kernel k = Kernel.identityFilter();
			assertThrows(IllegalStateException.class, () -> {
				hasher.removeFilter(k);
			});
		}

		@Test
		void addFilterDistinctAlgorithmIds() {
			HashingAlgorithm hasher = getInstance(16);
			Kernel k = Kernel.identityFilter();
			hasher.addFilter(k);
			HashingAlgorithm hasher1 = getInstance(16);
			assertNotEquals(hasher.algorithmId(), hasher1.algorithmId());
		}

		@Test
		void addRemoveFilterSameIds() {
			HashingAlgorithm hasher = getInstance(16);
			Kernel k = Kernel.identityFilter();
			hasher.addFilter(k);
			HashingAlgorithm hasher1 = getInstance(16);
			assertNotEquals(hasher.algorithmId(), hasher1.algorithmId());
		}
	}

	static Stream<Integer> bitResolutionBroad() {
		Integer[] ints = new Integer[100];
		ArrayUtil.fillArray(ints, (index) -> {
			return index + 10;
		});
		return Stream.of(ints);
	}

	static Stream<Integer> bitResolution() {
		Integer[] ints = new Integer[10];
		ArrayUtil.fillArray(ints, (index) -> {
			return index + 10;
		});
		return Stream.of(ints);
	}

	/**
	 * Some hashing algorthms need a greater or lower key resolution. Allow
	 * individual test to override this.
	 * 
	 * @return
	 */
	protected int offsetBitResolution() {
		return 0;
	}

	protected abstract HashingAlgorithm getInstance(int bitResolution);

}

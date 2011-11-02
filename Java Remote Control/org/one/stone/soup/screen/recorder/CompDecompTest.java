package org.one.stone.soup.screen.recorder;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class CompDecompTest {

	// simple test data
	int[] testOldFrame1 =	{ 1, 1, 1, 2, 3, 2, 1, 3 };
	int[] testNewFrame1 =	{ 2, 1, 1, 1, 1, 1, 1, 3 };
	int[] expResult1 = 		{ 2, 0x20000006, 1, 0x10000001};
	
	int[] testOldFrame2 =	{1, 3, 2, 1, 3, 2, 2, 1};
	int[] testNewFrame2 =	{2, 2, 2, 1, 3, 2, 2, 2};
	int[] expResult2 = 		{0x20000003, 2, 0x10000004, 2};
	
	

	@Test
	public void testCompress() {
		
		System.out.println("Test 1: ");
		System.out.println("old           : " + Arrays.toString(testOldFrame1));
		System.out.println("new           : " + Arrays.toString(testNewFrame1));
		System.out.println("expected      : " + Arrays.toString(expResult1));
		
		String result = Arrays.toString(CompDecomp.compress(testNewFrame1, testOldFrame1));
		System.out.println("result        : " + result);
		assertTrue(result.equals(Arrays.toString(expResult1)));
		
		System.out.println("Test 2: ");
		System.out.println("old           : " + Arrays.toString(testOldFrame2));
		System.out.println("new           : " + Arrays.toString(testNewFrame2));
		System.out.println("expected      : " + Arrays.toString(expResult2));

		result = Arrays.toString(CompDecomp.compress(testNewFrame2, testOldFrame2));
		System.out.println("result        : " + result);
		assertTrue(result.equals(Arrays.toString(expResult2)));
	}

	@Test
	public void testDecompress() {
		System.out.println();
		// compress/de-compress tests with simple data
		int[] compArray1 = CompDecomp.compress(testNewFrame1, testOldFrame1);
		int[] decompArray1 = CompDecomp.decompress(testOldFrame1, compArray1);
		System.out.println("decompressed test1 : " + Arrays.toString(decompArray1));
		assertTrue(Arrays.toString(decompArray1).equals(Arrays.toString(testNewFrame1)));
		
		int[] compArray2 = CompDecomp.compress(testNewFrame2, testOldFrame2);
		int[] decompArray2 = CompDecomp.decompress(testOldFrame2, compArray2);
		System.out.println("decompressed  : " + Arrays.toString(decompArray2));
		assertTrue(Arrays.toString(decompArray2).equals(Arrays.toString(testNewFrame2)));

	}

}

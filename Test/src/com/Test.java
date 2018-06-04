package com;

public class Test {
	private static class XX {
		public static int getVal(int y) {
			return y * y * y;
		}
	}
		
	public static int hello(int a) {
		return a;
	}
		
	public static String hello(String s) {
		return s;
	}
	
	int run(int b) {
		return XX.getVal(2) + 2;
	}
	
	int get(int x) {
		if (x > y - 5) {
			return hello(2);
		}
			
		return run(-2);
	}
		
	int test1() {
		int z = Integer.MAX_VALUE;
		int j = 0, k = 0;
		for (int i = 0; i < z && 2 * j - 15 < 16 / k; ++k) {
			hello(i);
		}
		
		for (int i = 0; i < Integer.MAX_VALUE; ++i) {
			hello(i);
		}
		
		for (int i = 0; ; ++i) {
			if (i < 5) {
				break;
			}
		}
		
		for (int i = 0; i < 20; ++i) {
			for (int j = 0; j < 20; ++i) {
				for (int p = 2; p < 10; p++) {
					return get(i + j + p);
				}
			}
		}
	}
}

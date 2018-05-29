package com.dimaoq.fdf;

public class Test {
		private static final int MAX_FPS = 50;
	private static final int FPS_SIZE = MAX_FPS + 1;
	private static class XX {
		public static int getVal(int y) {
			return y * y * y;
		}
	}
		
		int hello(int a) {
			return a;
		}
			
		String hello(String s) {
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
			// HashSet<String> ss = new HashSet<>();
			// for (String s : ss) {
				// write(s);
			// }
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
					return 1;
				}
 			}
		}
}

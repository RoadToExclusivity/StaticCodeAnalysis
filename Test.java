package com.dimaoq.fdf;

public class Test {
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
			for (int i = 0; i < 6; ++i) {
				hello(i);
			}
			
			for (int i = 0; ; ++i) {
				if (i < 5) {
					break;
				}
			}
			
			for (int i = 0; i < 6 && 2 * j - 15 < 16 / x; ++k) {
				hello(i);
			}
			
			for (int i = 0; i < 20; ++i) {
				for (int j = 0; j < 20; ++i) {
					return 1;
				}
 			}
		}
}

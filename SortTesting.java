public class SortTesting {
	static final int[] BASE_ORDERED = {1,2,3,5,7,11,13,17,19,23};
	static final int[] SECONDARY = {0,1,2,3,4,5,6,7,8,9};
	static int[] base1 = {5,2,17,19,13,11,23,1,7,3};
	static int[] base2 = {3,7,1,23,11,13,19,17,2,5};
	static int[] base3 = {17,19,23,1,13,7,2,3,5,11};
	static int[] secondary = {0,1,2,3,4,5,6,7,8,9};
	public static void main(String args[]){
		System.out.println("TESTING: f()");
		System.out.format("base1:%s\n", arrStr(base1));
		System.out.format("secondary:%s\n", arrStr(secondary));
		System.out.format("f(base1, secondary)\n");
		f(base1, secondary);
		System.out.format("base1:%s\n", arrStr(base1));
		System.out.format("EXPECTED:%s\n", arrStr(BASE_ORDERED));
		System.out.format("secondary:%s\n", arrStr(secondary));

		// System.out.format("base2:%s\n", arrStr(base1));
		// System.out.format("base3:%s\n", arrStr(base1));
	}

	private static String arrStr(int[] arr){
		String s="";
		for (int i = 0; i < arr.length; ++i)
			s+=String.format("%s%s", arr[i], (i < arr.length-1 ? " ":""));
		return s;
	}

	private static void f(int[] base, int[] secondary){
		f(base, 0, secondary);
	}
	private static void f(int[] base, int i, int[] secondary){
		if (secondary != null && base.length != secondary.length){
			System.out.println("Arrays need to be same size!");
			return;
		}
		int min = i;
		if (i < base.length-1){
			for (int j = i+1; j < base.length; ++j)
				min = (base[j] < base[min]) ? j : min;
			swap(base, i, min);
			if (secondary!=null)
				swap(secondary, i, min);
				
			f(base, ++i, secondary);
		}
	}
    private static void swap(int[] arr, int i, int j){
  		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
  	}

}
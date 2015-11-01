package data.structure;

/**
 * Supports simple pairs of arbitrary items
 * @author benjaminposerow
 *
 * @param <T>
 * @param <U>
 */
public class Tuple2<T,U> {
	private T item1;
	private U item2;
	
	public Tuple2(T item1, U item2) {
		this.item1 = item1;
		this.item2 = item2;
	}
	
	public T _1() {
		return item1;
	}
	
	public U _2() {
		return item2;
	}
}

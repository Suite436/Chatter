package data.structure;

/**
 * Supports triples of arbitrary items
 * @author benjaminposerow
 *
 * @param <T>
 * @param <U>
 * @param <V>
 */
public class Tuple3<T, U, V> {
	private T item1;
	private U item2;
	private V item3;
	
	public Tuple3(T item1, U item2, V item3) {
		this.item1 = item1;
		this.item2 = item2;
	}
	
	public T _1() {
		return item1;
	}
	
	public U _2() {
		return item2;
	}
	
	public V _3() {
		return item3;
	}
}

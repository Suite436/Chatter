package util;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item1 == null) ? 0 : item1.hashCode());
		result = prime * result + ((item2 == null) ? 0 : item2.hashCode());
		result = prime * result + ((item3 == null) ? 0 : item3.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple3 other = (Tuple3) obj;
		if (item1 == null) {
			if (other.item1 != null)
				return false;
		} else if (!item1.equals(other.item1))
			return false;
		if (item2 == null) {
			if (other.item2 != null)
				return false;
		} else if (!item2.equals(other.item2))
			return false;
		if (item3 == null) {
			if (other.item3 != null)
				return false;
		} else if (!item3.equals(other.item3))
			return false;
		return true;
	}
}

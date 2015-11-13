package util;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item1 == null) ? 0 : item1.hashCode());
		result = prime * result + ((item2 == null) ? 0 : item2.hashCode());
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
		Tuple2 other = (Tuple2) obj;
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
		return true;
	}
}

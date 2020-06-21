package jcrystal.utils;

import java.util.Comparator;

public class NullComparator<T> implements Comparator<T>{

	Comparator<T> base;
	public NullComparator(Comparator<T> base) {
		this.base = base;
	}
	@Override
	public int compare(T o1, T o2) {
		if(o1 == null && o2 == null)
			return 0;
		if(o1 == null)return -1;
		if(o2 == null)return 1;
		return base.compare(o1, o2);
	}
}

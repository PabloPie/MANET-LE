package util;

public class Pair<A extends Comparable<A>,B extends Comparable<B>> implements Comparable<Pair<A,B>> {
	public final A _1;
	public final B _2;
	
	public Pair(A a, B b) {
		this._1 = a;
		this._2 = b;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Pair) {
			Pair<?, ?> p = (Pair<?, ?>)o;
			return this._1.equals(p._1) && this._2.equals(p._2);
		}
		return false;
	}

	@Override
	public int compareTo(Pair<A, B> o) {
		int cmp1 = this._1.compareTo(o._1);
		int cmp2 = this._2.compareTo(o._2);
		
		if(cmp1 == 0) {
			if(cmp2 == 0) return 0;
			return cmp2;
		}
		return cmp1;
	}
	
	@Override
	public String toString() {
		return "(" + this._1 + " ; " + this._2 + ")";
	}

}

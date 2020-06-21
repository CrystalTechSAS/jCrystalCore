package jcrystal.server.databases;

public enum ComparisonOperator {
	lessThan(com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN),
	lessThanEq(com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL),
	greaterThan(com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN),
	greaterThanEq(com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL),
	equal(com.google.appengine.api.datastore.Query.FilterOperator.EQUAL),
	in(com.google.appengine.api.datastore.Query.FilterOperator.IN);
	com.google.appengine.api.datastore.Query.FilterOperator datastoreEquivalent;
	
	private ComparisonOperator(com.google.appengine.api.datastore.Query.FilterOperator datastoreEquivalent) {
		this.datastoreEquivalent = datastoreEquivalent;
	}
	public static ComparisonOperator[] valuesSingle = {equal,lessThan,lessThanEq,greaterThan,greaterThanEq}; 
	public String firebaseMethod() {
		switch (this) {
		case equal:
			return "whereEqualTo";
		case lessThan:
			return "whereLessThan";
		case lessThanEq:
			return "whereLessThanOrEqualTo";
		case greaterThan:
			return "whereGreaterThan";
		case greaterThanEq:
			return "whereGreaterThanOrEqualTo";
		default:
			return name();
		}
	}
	public String getUserName() {
		switch (this) {
			case equal:
				return "is";
			default:
				return name();
		}
	}
	public boolean singleParam() {
		return this != in;
	}
}

package jcrystal.model.server.db;

import jcrystal.types.IJType;
import jcrystal.types.JIAnnotable;
import jcrystal.reflection.annotations.IndexType;

public interface IndexableField extends TypedField, JIAnnotable{
	
	default String name() {
		return fieldName();
	}
	public String fieldName();
	public String getDBName();
	public IndexType indexType();
	public IJType getIndexType();
}

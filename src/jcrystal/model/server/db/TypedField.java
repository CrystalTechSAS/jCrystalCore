package jcrystal.model.server.db;

import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;

public interface TypedField extends NamedField{
	public IJType type();
}

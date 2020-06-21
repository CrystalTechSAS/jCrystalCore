package jcrystal.lang.elements;

import java.util.Map;

import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.vars.IAccessor;

public class TypeChangeAccessor implements IAccessor{
	IAccessor base;
	IJType type;
	
	public TypeChangeAccessor(IJType type, IAccessor base) {
		this.base = base;
		this.type = type;
	}
	
	@Override
	public String name() {
		return base.name();
	}
	
	@Override
	public IJType type() {
		return type;
	}
	@Override
	public String read() {
		return base.read();
	}
	@Override
	public String write(String value) {
		return base.write(value);
	}
	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return base.getAnnotations();
	}
}

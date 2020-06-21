package jcrystal.lang.elements;

import jcrystal.types.IJType;
import jcrystal.types.vars.IAccessor;

public class CrystalConstantField implements IAccessor{
	IJType type;
	String name;
	String value;
	
	public CrystalConstantField(IJType type, String name, String value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}
	@Override
	public String write(String value) {
		throw new NullPointerException("Can`t write a constant field");
	}
	@Override
	public IJType type(){
		return type;
	}
	@Override
	public String read() {
		return value;
	}
	
	@Override
	public String name() {
		return name;
	}
}

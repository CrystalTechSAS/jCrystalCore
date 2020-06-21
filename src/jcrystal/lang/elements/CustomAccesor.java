package jcrystal.lang.elements;

import java.util.Map;

import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JVariable;
import jcrystal.types.vars.IAccessor;

public class CustomAccesor implements IAccessor{
	String name;
	String accesor;
	IJType tipo;
	JVariable f;
	
	public CustomAccesor(String name, String accesor, IJType tipo, JVariable f) {
		this.name = name;
		this.accesor = accesor;
		this.tipo = tipo;
		this.f = f;
	}
	
	@Override
	public IJType type(){
		return tipo;
	}
	
	@Override
	public String read() {
		return accesor;
	}
	@Override
	public String write(String value) {
		throw new NullPointerException("Not implemented yet");
	}
	@Override
	public String name() {
		return name;
	}
	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return f == null ? IAccessor.super.getAnnotations() : f.getAnnotations();
	}
}

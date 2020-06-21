package jcrystal.model.server.db;

import java.util.stream.Stream;

import jcrystal.utils.StringSeparator;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

public class FieldSeparator extends StringSeparator{
	
	public FieldSeparator() {
		super(", ");
	}
	public void add(EntityField campo){
		add(campo.fieldName());
	}
	public void addWithType(AbsICodeBlock code, EntityField campo){
		add(code.$(campo.type()) + " " + campo.fieldName());
	}
	public static FieldSeparator buildForNames(Stream<EntityField> e){
		FieldSeparator ret = new FieldSeparator();
		e.forEach(ret::add);
		return ret;
	}
}

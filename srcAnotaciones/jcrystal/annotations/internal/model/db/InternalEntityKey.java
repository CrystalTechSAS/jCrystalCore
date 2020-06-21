package jcrystal.annotations.internal.model.db;

import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JClass;

public class InternalEntityKey extends JAnnotation{
	
	private static final long serialVersionUID = 3915263509615710804L;
	
	private String simpleKeyName;
	
	private IJType parentEntity;
	
	public InternalEntityKey(String simpleKeyName, IJType parentEntity) {
		super(InternalEntityKey.class);
		this.simpleKeyName = simpleKeyName;
		this.parentEntity = parentEntity;
	}
	public String simpleKeyName() {
		return simpleKeyName;
	}
	public IJType parentEntity() {
		return parentEntity;
	}
}

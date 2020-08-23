package jcrystal.lang.elements;

import java.util.Map;

import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityField;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.vars.AbsJAccessor;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;

public abstract class AbsEntityFieldAccessor extends AbsJAccessor{

	final EntityField field;
	protected boolean isKey = false;
	protected String suffix;
	IJType type;
	public AbsEntityFieldAccessor(EntityField field) {
		this.field = field;
	}

	@Override
	public IJType type() {
		return type;
	}

	@Override
	public String name() {
		return field.fieldName();
	}

	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return field.getAnnotations();
	}
	
	public AbsEntityFieldAccessor changeToOriginalType() {
		this.type = field.type();
		this.suffix = null; 
		this.isKey = false;
		this.prefix = null;
		return this;
	}
	public AbsEntityFieldAccessor changeToKeys() {
		if(ContextType.SERVER.is()) {
			if(field.getTargetEntity() != null && field.getTargetEntity().key != null && field.getTargetEntity().key.isSimple())
				return changeToKeys("$RawKey");
			else
				return changeToKeys("$Key");
		}
		return changeToKeys("");
	}
	public AbsEntityFieldAccessor changeToKeys(String suffix) {
		this.type = field.type();
		this.suffix = suffix; 
		this.prefix = null;
		this.isKey = true;
		EntityClass target = field.getTargetEntity();
		if(!field.isClientSideOnly && target != null) {
			if(target.key != null) {
				if(field.type().isIterable())
					this.type = field.getTargetEntity().key.getSingleKeyType().createListType();
				else
					this.type = field.getTargetEntity().key.getSingleKeyType();	
			}else
				this.isKey = false;	
		}else
			this.isKey = false;
		
		return this;
	}
}

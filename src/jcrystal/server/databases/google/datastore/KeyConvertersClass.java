package jcrystal.server.databases.google.datastore;

import java.util.stream.Collectors;

import jcrystal.model.server.db.EntityField;
import jcrystal.server.databases.AbsEntityGenerator;
import jcrystal.server.databases.AbsKeyConverterClass;
import jcrystal.types.utils.GlobalTypes;

public class KeyConvertersClass extends AbsKeyConverterClass{
	public KeyConvertersClass(AbsEntityGenerator parent) {
		super(parent);
	}
	
	@Override
	public String keyType(EntityField f) {
		if(f.keyData == null || f.getTargetEntity() == null)
			return $($convert(f.type())) + " " + f.fieldName();
		else
			return $($convert(f.getTargetEntity().key.getSingleKeyType())) + " " + f.fieldName();
	}
	
	@Override
	public String entityToRawKey(EntityField f) {
		if(f.getTargetEntity() != null) {
			if(!f.getTargetEntity().key.isSimple())
				return f.fieldName()+".getRawKey()";
			else
				return $(f.getTargetEntity().clase)+".Key.createRawKey("+f.fieldName()+")";
		}
		return f.fieldName();
	}

	@Override
	public String keyToRawKey(EntityField f) {
		throw new NullPointerException();
	}
	@Override
	public String rawKeyType(EntityField f) {
		if(f.keyData == null || f.getTargetEntity() == null)
			return $($convert(f.type())) + " " + f.fieldName();
		else if(f.getTargetEntity() != null && f.getTargetEntity().key.isSimple())
			return $(f.getTargetEntity().key.getSingleKeyType()) + " " + f.fieldName();
		else
			return $(GlobalTypes.Google.DataStore.KEY) + " " + f.fieldName();
	}
	@Override
	public String entityType(EntityField f) {
		if(f.keyData == null || f.getTargetEntity() == null)
			return $($convert(f.type())) + " " + f.fieldName();
		else
			return f.getTargetEntity().name() + " " + f.fieldName();
	}
	
	public String getRawKeyExpresion(String entityName) {
		return getRawKeyExpresion(entityName, parent.entidad.key.size());
	}
	public String getRawKeyExpresion(String entityName, int size) {
		String key = null;
		for(int e = 0; e < size; e++) {
			EntityField f = parent.entidad.key.getLlaves().get(e);
			String prevKey = key != null ? key + ", ":"";
			String accessor = f.fieldName();
			if(f.isConstant)
				accessor = "\""+f.fieldName()+"\"";
			if(e == parent.entidad.key.getLlaves().size() - 1) {
				if(f.getTargetEntity() != null && f.getTargetEntity().key.isSimple()) {
					accessor = f.fieldName();
				}
				key = "com.google.appengine.api.datastore.KeyFactory.createKey(" + prevKey + entityName + ", " + accessor + ")";
			}else if(f.getTargetEntity() != null) {
				if(f.getTargetEntity().key.isSimple())
					key = $(f.getTargetEntity().clase)+".Key.createRawKey(" + accessor + ")";
				else
					key = accessor;
			}else
				key = "com.google.appengine.api.datastore.KeyFactory.createKey(" + prevKey + "\"" + f.dbName + "\", " + accessor + ")";
		}
		return key;
	}
	public String getEntityKeyToRaw(int size) {
		return parent.entidad.key.stream().limit(size).map(f->{
			if(f.getTargetEntity() != null) {
				if(!f.getTargetEntity().key.isSimple())
					return f.fieldName()+".getRawKey()";
				else
					return $(f.getTargetEntity().clase)+".Key.createRawKey("+f.fieldName()+")";
			}
			return f.fieldName();
		}).collect(Collectors.joining(", "));
	}
}

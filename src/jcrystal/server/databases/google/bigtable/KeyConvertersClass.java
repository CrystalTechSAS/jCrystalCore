package jcrystal.server.databases.google.bigtable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.server.databases.AbsEntityGenerator;
import jcrystal.utils.langAndPlats.JavaCode;

public class KeyConvertersClass extends JavaCode{
	AbsEntityGenerator parent;
	public KeyConvertersClass(AbsEntityGenerator parent) {
		this.parent = parent;
	}
	public String keyType(EntityField f) {
		if(f.keyData == null || f.getTargetEntity() == null)
			return $($convert(f.type())) + " " + f.fieldName();
		else
			return $($convert(f.getTargetEntity().key.getSingleKeyType())) + " " + f.fieldName();
	}
	public String keyType(List<EntityField> keys) {
		return keys.stream().map(this::keyType).collect(Collectors.joining(", "));
	}
	public String keyType(Stream<EntityField> keys) {
		return keys.map(this::keyType).collect(Collectors.joining(", "));
	}
	public String keyType() {
		return keyType(parent.entidad.key.size());
	}
	public String keyType(EntityClass target) {
		return keyType(target.key.stream());
	}
	public String keyType(int size) {
		return keyType(parent.entidad.key.getLlaves().stream().limit(size));
	}
	
	public String keyNames(List<EntityField> keys) {
		return keys.stream().map(k->k.fieldName()).collect(Collectors.joining(", "));
	}
}

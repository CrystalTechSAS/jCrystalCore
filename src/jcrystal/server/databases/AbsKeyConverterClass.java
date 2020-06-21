package jcrystal.server.databases;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.utils.langAndPlats.JavaCode;

public abstract class AbsKeyConverterClass extends JavaCode{
	protected AbsEntityGenerator parent;
	public AbsKeyConverterClass(AbsEntityGenerator parent) {
		this.parent = parent;
	}
	/**
	 * Returns a typed key as raw db key type 
	 * @param f
	 * @return
	 */
	public abstract String rawKeyType(EntityField f);
	/**
	 * Returns a typed key as Entity.Key
	 * @param f
	 * @return
	 */
	public abstract String keyType(EntityField f);
	/**
	 * Returns the way to get raw key type from f 
	 * @param f
	 * @return
	 */
	public abstract String entityToRawKey(EntityField f);
	/**
	 * Returns the way to get raw key type from key types 
	 * @param f
	 * @return
	 */
	public abstract String keyToRawKey(EntityField f);
	/**
	 * Returns a typed key as Entity
	 * @param f
	 * @return
	 */
	public abstract String entityType(EntityField key);
	
	public boolean isComplexKey(EntityField key) {
		return key.getTargetEntity() != null && !key.getTargetEntity().key.isSimple();
	}
	
	public final String keyType(Stream<EntityField> keys) {
		return keys.filter(k->!k.isConstant).map(this::keyType).collect(Collectors.joining(", "));
	}
	public final String keyType(List<EntityField> keys) {
		return keyType(keys.stream());
	}
	public final String keyType() {
		return keyType(parent.entidad.key.size());
	}
	public final String keyType(int size) {
		return keyType(parent.entidad.key.getLlaves().stream().limit(size));
	}
	public final String keyType(EntityClass target) {
		return keyType(target.key.stream());
	}
	
	public final String keyNames(Stream<EntityField> keys) {
		return keys.filter(k->!k.isConstant).map(k->k.fieldName()).collect(Collectors.joining(", "));
	}
	public final String keyNames(List<EntityField> keys) {
		return keyNames(keys.stream());
	}
	public final String keyNames() {
		return keyNames(parent.entidad.key.size());
	}
	public final String keyNames(int size) {
		return keyNames(parent.entidad.key.getLlaves().stream().limit(size));
	}
	
	public final String entityToRawKey(Stream<EntityField> keys) {
		return keys.filter(k->!k.isConstant).map(this::entityToRawKey).collect(Collectors.joining(", "));
	}
	public final String entityToRawKey(List<EntityField> keys) {
		return entityToRawKey(keys.stream());
	}
	public final String entityToRawKey() {
		return entityToRawKey(parent.entidad.key.size());
	}
	public final String entityToRawKey(int size) {
		return entityToRawKey(parent.entidad.key.getLlaves().stream().limit(size));
	}
	
	public final String keyToRawKey(Stream<EntityField> keys) {
		return keys.filter(k->!k.isConstant).map(this::keyToRawKey).collect(Collectors.joining(", "));
	}
	public final String keyToRawKey(List<EntityField> keys) {
		return keyToRawKey(keys.stream());
	}
	public final String keyToRawKey() {
		return keyToRawKey(parent.entidad.key.size());
	}
	public final String keyToRawKey(int size) {
		return keyToRawKey(parent.entidad.key.getLlaves().stream().limit(size));
	}
	
	public final String rawKeyType() {
		return rawKeyType(parent.entidad.key.size());
	}
	public final String rawKeyType(int size) {
		return parent.entidad.key.getLlaves().stream().limit(size).filter(k->!k.isConstant).map(this::rawKeyType).collect(Collectors.joining(", "));
	}
	
	public final String entityType(List<EntityField> keys) {
		return entityType(keys.stream());
	}
	public final String entityType(Stream<EntityField> keys) {
		return keys.filter(k->!k.isConstant).map(this::entityType).collect(Collectors.joining(", "));
	}
	public final String entityType() {
		return entityType(parent.entidad.key.getLlaves().stream());
	}
	
	public final boolean isComplexKey(Stream<EntityField> keys) {
		return keys.anyMatch(this::isComplexKey);
	}
	public final boolean isComplexKey(List<EntityField> keys) {
		return keys.stream().anyMatch(this::isComplexKey);
	}
	public final boolean isComplexKey() {
		return isComplexKey(parent.entidad.key.size());
	}
	public final boolean isComplexKey(int size) {
		return isComplexKey(parent.entidad.key.getLlaves().stream().limit(size));
	}
}

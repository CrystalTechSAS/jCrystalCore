package jcrystal.model.server.db;

import jcrystal.annotations.internal.model.db.AutogeneratedKey;
import jcrystal.configs.server.dbs.DBType;
import jcrystal.entity.types.Autogenerated;
import jcrystal.entity.types.ConstantId;
import jcrystal.entity.types.CreationTimestamp;
import jcrystal.entity.types.Email;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.ModificationTimestamp;
import jcrystal.entity.types.PersistentFile;
import jcrystal.entity.types.security.EmailAccount;
import jcrystal.entity.types.security.FacebookAccount;
import jcrystal.entity.types.security.FirebaseAccount;
import jcrystal.entity.types.security.GoogleAccount;
import jcrystal.entity.types.security.Password;
import jcrystal.entity.types.security.PhoneAccount;
import jcrystal.entity.types.security.UsernameAccount;
import jcrystal.json.JsonLevel;
import jcrystal.lang.elements.AbsEntityFieldAccessor;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.EntityKeyWrapper;
import jcrystal.reflection.annotations.EntityProperty;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.reflection.annotations.Selector;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.reflection.annotations.entities.RelMtoM;
import jcrystal.reflection.annotations.security.HashSalt;
import jcrystal.server.databases.DBUtils;
import jcrystal.utils.StringUtils;

import java.util.Map;

import com.google.appengine.api.datastore.Text;

/**
* Created by gasotelo on 2/8/17.
*/
public class EntityField implements Comparable<EntityField>, IndexableField{
	public final JVariable f;
	public final boolean isSelector, isstatic, isFinal, isAutoNow, isEntityProperty, isConstant;
	public IndexType indexType;
	public final boolean isAccountField;
	public final String dbName;
	public final HashSalt hashSalt;
	public boolean editable;
	public boolean hasDefaultValue;
	public Object defaultValue;
	public JsonLevel level;
	public EntityKey keyData;
	
	
	private EntityClass targetEntity;
	
	public EntityField(JVariable f, DBType mainDBType) {
		this.f = f;		
		this.keyData = f.getAnnotation(EntityKey.class);
		isSelector = f.isAnnotationPresent(Selector.class);
		hashSalt = f.getAnnotation(HashSalt.class);
		isFinal = f.isFinal() || f.type().is(CreationTimestamp.class);
		isstatic = f.isStatic();
		isConstant = f.type().is(ConstantId.class);
		
		isEntityProperty = f.isAnnotationPresent(EntityProperty.class);
		if(this.keyData != null && isEntityProperty)
			((EntityKeyWrapper)this.keyData).indexAsProperty(true);
		EntityProperty prop = f.getAnnotation(EntityProperty.class);
		isAccountField = f.type().is(FacebookAccount.class, GoogleAccount.class, PhoneAccount.class, 
				UsernameAccount.class, EmailAccount.class, FirebaseAccount.class);
		if(isConstant) {
			isAutoNow = false;
			indexType = IndexType.NONE;
			editable = false;
			level = JsonLevel.NONE;
			dbName = f.name();
		}
		else if(prop != null){
			isAutoNow = prop.autoNow() || f.type().is(CreationTimestamp.class, ModificationTimestamp.class);
			dbName = prop.name().isEmpty() ? f.name() : prop.name();
			if(isAccountField) {
				indexType = IndexType.UNIQUE;
				editable = false;
			}else {
				editable = prop.editable() && !isAutoNow && !f.type().is(Password.class);
				indexType = prop.index();
			}
			level = prop.json();
		}
		else {
			isAutoNow = false;
			dbName = f.name();
			indexType = this.keyData != null && this.keyData.indexAsProperty() ? IndexType.MULTIPLE : IndexType.NONE;
			if (f.isAnnotationPresent(RelMto1.class))
				level = f.getAnnotation(RelMto1.class).keyLevel();
			if (f.isAnnotationPresent(Rel1to1.class))
				level = f.getAnnotation(Rel1to1.class).keyLevel();
			if (f.isAnnotationPresent(RelMtoM.class))
				level = f.getAnnotation(RelMtoM.class).keyLevel();
			editable = false;
		}
		if(this.keyData != null) {
			level = JsonLevel.ID;
			editable = keyData.postable();
		}

		if(f.type().is(Password.class) || isAccountField) {
			level = JsonLevel.NONE;
			editable = false;
		}
		if(type().is(Autogenerated.class)){
			f.addAnnotation(new AutogeneratedKey());
			f.type = DBUtils.getGeneratedKeyType(mainDBType);
		}else if(type().is(Email.class, com.google.appengine.api.datastore.Email.class)) {
			f.addAnnotation(new JAnnotation(Email.class));
			f.type = GlobalTypes.STRING;
		}
		hasDefaultValue = f.staticDefaultValue != null;
		if(!type().name().startsWith("jcrystal.entity.types"))
			defaultValue = f.staticDefaultValue;
		else {
			if(type().is(PersistentFile.class)) {
				editable = false;
			}
		}
	}
	public boolean isPrimitive(){
		return f.type().isPrimitive();
	}
	public boolean isAccountField() {
		return isAccountField;
	}
	public boolean isText(){
		return f.type().is(Text.class, LongText.class);
	}
	public IJType type(){
		return f.type();
	}
	@Override
	public IndexType indexType() {
		return indexType;
	}
	@Override
	public int compareTo(EntityField o) {
		return fieldName().compareTo(o.fieldName());
	}
	@Override
	public String fieldName() {
		return f.name();
	}
	@Override
	public String getDBName() {
		return dbName;
	}
	
	//Métodos para creación del código del indice (de esta propiedad)
	@Override
	public IJType getIndexType() {
		if(targetEntity != null)
			return targetEntity.key.getSingleKeyType();
		return f.type();
	}
	public boolean isArray() {
		return f.type().isArray();
	}
	public boolean isEnum() {
		return f.type().isEnum();
	}
	public final boolean isPostable(JsonLevel targetLevel) {
		return editable && (keyData != null || level.level <= targetLevel.level);  
	}
	public final String getWebServiceName(EntityClass entity, JCrystalWebServiceParam param) {
		if(entity.key.getLlaves().size()==1)
			return "id"+StringUtils.capitalize(param.nombre);
		return fieldName()+"_"+param.nombre;
	}
	public EntityClass getTargetEntity() {
		return targetEntity;
	}
	public void setTargetEntity(EntityClass targetEntity) {
		this.targetEntity = targetEntity;
	}
	public boolean isAutogenerated() {
		return f.isJAnnotationPresent(AutogeneratedKey.class);
	}
	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return f.getAnnotations();
	}
	@Override
	public JAnnotation getJAnnotationWithAncestorCheck(String name) {
		throw new NullPointerException();
	}
	public AbsEntityFieldAccessor propertyAccessor() {
		return propertyAccessor.changeToOriginalType();
	}
	public AbsEntityFieldAccessor propertyKeyAccessor() {
		return propertyAccessor.changeToKeys();
	}
	public AbsEntityFieldAccessor propertyKeyAccessor(String suffix) {
		return propertyAccessor.changeToKeys(suffix);
	}
	public AbsEntityFieldAccessor fieldAccessor() {
		return fieldAccessor.changeToOriginalType();
	}
	public AbsEntityFieldAccessor fieldKeyAccessor() {
		return fieldAccessor.changeToKeys();
	}
	private final AbsEntityFieldAccessor propertyAccessor = new AbsEntityFieldAccessor(this) {
		@Override
		public String write(String value) {
			String ret = "";
			if(prefix != null)
				ret += prefix;
			ret += fieldName();
			if(isKey)
				ret += suffix;
			return ret + "(" + value + ")";
		}
		@Override
		public String read() {
			String ret = "";
			if(prefix != null)
				ret += prefix;
			ret += fieldName();
			if(isKey)
				ret += suffix;
			return ret + "()";
		}
	};
	private final AbsEntityFieldAccessor fieldAccessor = new AbsEntityFieldAccessor(this) {
		@Override
		public String write(String value) {
			if(prefix != null)
				return prefix + fieldName() + " = " + value;
			return fieldName()+" = " + value;
		}
		@Override
		public String read() {
			if(prefix != null)
				return prefix + fieldName();
			return fieldName();
		}
	};
}
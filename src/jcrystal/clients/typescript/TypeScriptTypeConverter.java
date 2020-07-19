package jcrystal.clients.typescript;


import java.util.stream.Collectors;

import com.google.appengine.api.datastore.Text;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.android.AndroidClient;
import jcrystal.clients.utils.JsonWrapper;
import jcrystal.entity.types.CreationTimestamp;
import jcrystal.entity.types.ModificationTimestamp;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.manager.utils.FileWrapperResponse;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.WrapStringJType;
import jcrystal.types.convertions.IImportConverter;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.StreamUtils;
import jcrystal.utils.context.ITypeConverter;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

public class TypeScriptTypeConverter implements ITypeConverter, IImportConverter{
	ClientContext context;
	public TypeScriptTypeConverter(ClientContext context) {
		this.context = context;
	}
	
	@Override
	public IJType convert(IJType type) {
		if(type instanceof WrapStringJType)
			return type;
		if(type.is(Text.class, String.class))
			return GlobalTypes.STRING;
		else if(type.isSubclassOf(FileWrapperResponse.class))
			return convert(type.getInnerTypes().get(0));
		else if(type.name().equals("com.google.appengine.api.datastore.GeoPt"))
			return GlobalTypes.ARRAY.DOUBLE;
		else if(type.isEnum()) {
			if(type.tryResolve() == null)
				return GlobalTypes.STRING;
			return GlobalTypes.LONG;
		}else if(type.isIterable()){
			final IJType tipoParamero = type.getInnerTypes().get(0);
			return convert(tipoParamero).createListType();
		}else
			return GlobalTypeConverter.INSTANCE.convert(type);
	}

	@Override
	public String getImportLocation(String pathToRoot, IJType type) {
		if(type.isEnum())
			return "import {"+type.getSimpleName()+"} from \"" + pathToRoot + "enums/"+type.getSimpleName()+"\";";
		else if(type instanceof JsonWrapper) {
			return "import {" +type.getSimpleName() + "} from \"" + pathToRoot + "entities/" + type.getSimpleName() + "\";";
		}else if (type.isAnnotationPresent(Post.class)) {
			JsonLevel level = type.getAnnotation(Post.class).level();
			String className = context.data.entidades.get(type).name() + level.baseName();
			return "import {"+className+"} from \"" + pathToRoot + "entities/" + className+"\";";
		}
		else if(type.isAnnotationPresent(jSerializable.class))
			return "import {" +type.getSimpleName() + "} from \"" + pathToRoot + "results/" + type.getSimpleName() + "\";";
		else if(type.is(CreationTimestamp.class, ModificationTimestamp.class))
			return "import {CrystalDateMilis} from \"" + pathToRoot + "dates/CrystalDateMilis\";";
		else if(type.isJAnnotationPresent(CrystalDate.class))
			return "import {" +type.getSimpleName() + "} from \"" + pathToRoot + "dates/" + type.getSimpleName() + "\";";
		else if(type.isAnnotationPresent(jEntity.class))
			return "import {" +type.getSimpleName() + "} from \"" + pathToRoot + "entities/" + type.getSimpleName() + "\";";
		return null;
	}
	@Override
	public String $toString(IJType type, AbsICodeBlock parent) {
		if(type.is(CreationTimestamp.class, ModificationTimestamp.class))
			return "CrystalDateMilis";
		else if(type.isJAnnotationPresent(InternalEntityKey.class))
			return type.getJAnnotation(InternalEntityKey.class).simpleKeyName();
		else if(type == GlobalTypes.jCrystal.VoidSuccessListener)
			return "() => void";
		else if(type == GlobalTypes.jCrystal.ErrorListener)
			return "(error : RequestError) => void";
		else if(GlobalTypes.jCrystal.isSuccessListener(type))
			return "(" + StreamUtils.mapWithIndex(type.getInnerTypes(), (i, t)->"p" + i + " : " + parent.$(t)).collect(Collectors.joining(", ")) + ") => void";
		else if (type.isAnnotationPresent(Post.class)) {
			JClass superClase = context.data.entidades.get(type).clase;
			return superClase.getSimpleName()+type.getSimpleName();
		}
		return null;
	}
}

package jcrystal.model.web;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.clients.utils.ClassJsonLevelProvider;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JIAnnotable;
import jcrystal.utils.langAndPlats.AbsICodeBlock;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Method;

public interface IWServiceEndpoint extends Comparable<IWServiceEndpoint>, JIAnnotable, ClassJsonLevelProvider{

	JCrystalWebServiceParam getTokenParam();
	void gatherRequiredTypes(TreeSet<IJType> repetidos);
	String name();
	String getPath(ClientGeneratorDescriptor<?> clientConfig);
	Method getPathType();
	
	List<JCrystalWebServiceParam> getParameters();
	ContentType[] contentType();
	JCrystalWebServiceManager getManager();
	IJType getReturnType();
	IInternalConfig exportClientConfig(ClientGeneratorDescriptor<?> clientConfig);
	
	boolean isAdminClient();
	boolean hasAuthTokenResponse();
	Map<String, AbsICodeBlock> getUsageExamples();
	
	default int compareTo(IWServiceEndpoint o) {
		int a = getManager().clase.name.compareTo(o.getManager().clase.name);
		if(a!=0)
			return a;
		a = name().compareTo(o.name());
		if(a!=0)
			return a;
		return getPathType().name().compareTo(o.getPathType().name());
	}
	public default JAnnotation getJAnnotationWithAncestorCheck(String name) {
		throw new UnsupportedOperationException();
	}
	void registerClientExample(String clientId, AbsICodeBlock example);
	default boolean has(ContentType type) {
		ContentType[] contentType = contentType();
		for(int e = 0; e < contentType.length; e++)
			if(contentType[e] == type)
				return true;
		return false;
	}
	default boolean isMultipart() {
		return has(ContentType.MultipartForm);
	}
	default boolean isNotMultipart() {
		ContentType[] contentType = contentType();
		for(int e = 0; e < contentType.length; e++)
			if(contentType[e] != ContentType.MultipartForm)
				return true;
		return false;
	}
}

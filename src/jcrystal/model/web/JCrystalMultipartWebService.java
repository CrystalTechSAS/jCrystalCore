package jcrystal.model.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.langAndPlats.AbsICodeBlock;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Method;

public class JCrystalMultipartWebService implements IWServiceEndpoint{
	JCrystalWebServiceManager manager;
	JCrystalWebServiceParam tokenParam;
	List<JCrystalWebServiceParam> parameters = new ArrayList<>();
	public Map<String, AbsICodeBlock> usageExamples = new TreeMap<>();
	public JCrystalMultipartWebService(ClientContext context, JCrystalWebServiceManager manager) {
		this.manager = manager;
		tokenParam = manager._metodos.stream().filter(f->f.tokenParam!=null).map(f->f.tokenParam).findFirst().orElse(null);
		
		if(!manager.clase.hasEmptyConstructor())
			manager.clase.constructors.get(0).params.stream().map(p->new JCrystalWebServiceParam(context, this, p, true)).forEach(parameters::add);
		manager.managedAttributes.stream().map(p->new JCrystalWebServiceParam(context, this, p, true)).forEach(parameters::add);
		
		tokenParam = parameters.stream().filter(f->f.securityToken).findFirst().orElse(tokenParam);
	}
	@Override
	public String name() {
		return manager.clase.getSimpleName();
	}
	@Override
	public boolean isMultipart() {
		return true;
	}
	@Override
	public JCrystalWebServiceParam getTokenParam() {
		return tokenParam;
	}
	@Override
	public void gatherRequiredTypes(TreeSet<IJType> repetidos) {
		manager._metodos.forEach(c->{
			c.gatherRequiredTypes(repetidos);
		});
	}
	@Override
	public String getPath(ClientGeneratorDescriptor<?> clientConfig) {
		return manager.getPath(clientConfig);
	}
	@Override
	public Method getPathType() {
		return Method.POST;
	}
	@Override
	public List<JCrystalWebServiceParam> getParameters() {
		return parameters;
	}
	public List<JCrystalWebService> getServices() {
		return manager._metodos;
	}
	@Override
	public ContentType[] contentType() {
		return new ContentType[] { ContentType.MultipartForm};
	}
	@Override
	public JCrystalWebServiceManager getManager() {
		return manager;
	}
	@Override
	public IJType getReturnType() {
		return GlobalTypes.VOID;
	}
	@Override
	public JsonLevel getJsonLevel(IJType clase) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public IInternalConfig exportClientConfig(ClientGeneratorDescriptor<?> clientConfig) {
		IInternalConfig config = clientConfig.configs.get(manager.exportClientConfigId(clientConfig));
		if(config!=null)
			return config;
		return clientConfig.configs.values().stream().findFirst().orElse(null);
	}
	@Override
	public boolean isAdminClient() {
		return false;
	}
	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return manager.clase.getAnnotations();
	}
	@Override
	public boolean hasAuthTokenResponse() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void registerClientExample(String clientId, AbsICodeBlock example) {
		usageExamples.put(clientId, example);
	}
	@Override
	public Map<String, AbsICodeBlock> getUsageExamples() {
		return usageExamples;
	}
}

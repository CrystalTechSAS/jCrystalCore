package jcrystal.model.web;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.ClientId;
import jcrystal.entity.types.LongText;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.TypedField;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JVariable;
import jcrystal.types.vars.IAccessor;
import jcrystal.security.SignInInfo;
import jcrystal.reflection.annotations.Def;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.ws.HeaderParam;
import jcrystal.server.FileUploadDescriptor;
import jcrystal.service.types.Authorization;
import jcrystal.utils.InternalException;
import jcrystal.utils.StringUtils;

public class JCrystalWebServiceParam implements TypedField, IAccessor {
	public final JVariable p;
	public final boolean required;
	public final boolean securityToken;
	
	public final String nombre;
	public final String valorDefecto;
	public HttpType tipoRuta = HttpType.GET;
	public final boolean classParam;
	
	public JCrystalWebServiceParam(ClientContext context, IWServiceEndpoint m, JVariable p, boolean classParam) {
		this.p = p;
		this.classParam = classParam;
		boolean pRequiered = false;
		
		{
			String pNombre = p.name();
			pRequiered |= pNombre.endsWith("_");
			while(pNombre.endsWith("_"))
				pNombre = pNombre.substring(0, pNombre.length()-1);
			this.nombre = pNombre;
		}
		if(p.isAnnotationPresent(Def.class))
			throw new NullPointerException("Def values not supported");
		else
			valorDefecto = null;
		
		securityToken = context.data.isSecurityToken(p.type());
		
		//TODO Lista de jsonify, de entidades
		if (p.type().is(HttpServletRequest.class,HttpServletResponse.class, SignInInfo.class, PrintWriter.class, StringBuilder.class) || p.type().is("EntityManager"))
			tipoRuta = HttpType.SERVER;
		else if(securityToken)
			tipoRuta = HttpType.SESSION;
		else if(p.isAnnotationPresent(HeaderParam.class) || p.type().is(Authorization.class, ClientId.class))
			tipoRuta = HttpType.HEADER;
		else if(m.name().contains("$"+nombre))
			tipoRuta = HttpType.PATH;
		else if(m.getManager().isMultipart)
			tipoRuta = HttpType.POST;
		else if (p.type().is("JSONObject") || p.type().is("JSONArray") || p.type().isAnyAnnotationPresent(Post.class, jSerializable.class) || p.type().isJAnnotationPresent(InternalEntityKey.class))
			tipoRuta = HttpType.POST;
		else if(context.data.entidades.contains(p.type) && !context.data.entidades.get(p.type).key.isSimple())
			tipoRuta = HttpType.POST;
		else if(p.type().is(FileUploadDescriptor.class))
			tipoRuta = HttpType.POST;
		else if (p.type().is(LongText.class))
			tipoRuta = HttpType.POST;
		else if(p.type().isArray()/* && p.type().getComponentType().isAnnotationPresent(JsonSerializable.class)*/)
			tipoRuta = HttpType.POST;
		else if(p.type().isIterable()){
			final IJType tipoParamero = p.type().getInnerTypes().get(0);
			if(tipoParamero.isPrimitiveObjectType() || tipoParamero.isAnyAnnotationPresent(jSerializable.class, Post.class, jEntity.class) || tipoParamero.isEnum())
				tipoRuta = HttpType.POST;
			else
				throw new NullPointerException("Invalid type " + tipoParamero );
		}
		else if(m.getPathType().isPostLike())
			tipoRuta = HttpType.POST;
		
		required = pRequiered;
	}
	/**
	 * El id del parametro utilizado para la comunicación entre los clientes y el ws
	 * @return
	 */
	public String getWsParamName() {
		if(p.type().isAnnotationPresent(jEntity.class))
			return "id"+StringUtils.capitalize(nombre);
		else return nombre;
	}
	@Override
	public String name() {
		return nombre;
	}
	@Override
	public String read() {
		return nombre;
	}
	@Override
	public IJType type() {
		return p.type();
	}
	@Override
	public Map<String, JAnnotation> getAnnotations() {
		return p.getAnnotations();
	}
	@Override
	public JAnnotation getJAnnotationWithAncestorCheck(String name) {
		return p.getJAnnotationWithAncestorCheck(name);
	}
	@Override
	public String write(String value) {
		throw new InternalException(500, "not implemented");
	}
	
}

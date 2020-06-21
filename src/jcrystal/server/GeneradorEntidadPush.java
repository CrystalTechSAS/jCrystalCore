package jcrystal.server;

import jcrystal.lang.elements.CrystalConstantField;
import jcrystal.main.data.ClientContext;
import jcrystal.types.JClass;
import jcrystal.types.JIVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.vars.IAccessor;
import jcrystal.types.JVariable;
import jcrystal.reflection.annotations.Push;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class GeneradorEntidadPush {
	private ClientContext context;
	public GeneradorEntidadPush(ClientContext context) {
		this.context = context;
	}
	public static void generarServletCode(ClientContext context, final JClass entidad, final AbsCodeBlock code){
		code.new B() {{
				$("case \"/api/push/" + entidad.getSimpleName() + "\":", ()->{
					$if("(!\"0.1.0.2\".equals(req.getRemoteAddr()) && com.google.appengine.api.utils.SystemProperty.environment.value() == com.google.appengine.api.utils.SystemProperty.Environment.Value.Production)", ()->{
						$("resp.setStatus(401);");
						$("return;");
					});
					$("byte[] buffer = new byte[1024*4];");
					$("try(java.io.InputStream is = req.getInputStream())", ()->{
						openConnection(context, this.P, ()->{
							$("for(int n; (n = is.read(buffer)) != -1; )", ()->{
								$("out.write(buffer, 0, n);");
							});
							$("out.flush();");
						});
					});
				});
			}
		};
		
	}
	public void generarEntidad(final JClass entidad)throws Exception{
		final List<JVariable> campos = entidad.attributes.stream().filter(p -> !p.isAnnotationPresent(Push.PushTitle.class) && !p.isAnnotationPresent(Push.PushBody.class)).collect(Collectors.toList());
		
		final JIVariable campoTitle = entidad.attributes.stream().filter( p -> p.isAnnotationPresent(Push.PushTitle.class)).findFirst().orElse(null);
		final JIVariable campoBody = entidad.attributes.stream().filter( p -> p.isAnnotationPresent(Push.PushBody.class)).findFirst().orElse(null);
		String tTitle = campoTitle == null && entidad.isAnnotationPresent(Push.PushTitle.class) ? entidad.getAnnotation(Push.PushTitle.class).value() : null;
		String tBody = campoTitle == null && entidad.isAnnotationPresent(Push.PushBody.class) ? entidad.getAnnotation(Push.PushBody.class).value() : null;
		
		final String title = tTitle != null && !tTitle.trim().isEmpty() ? tTitle : "Title";
		final String body = tBody != null && !tBody.trim().isEmpty() ? tBody : "Text";
		
		StringSeparator params = new StringSeparator(", ");
		entidad.attributes.stream().forEach(p -> params.add(p.type().getSimpleName() + " " + p.name()));
		new JavaCode() {{
			$("public " + entidad.getSimpleName() + "(" + params + ")", ()->{
				entidad.attributes.stream().forEach(p -> $( "this." + p.name() + " = " + p.name()+";"));
			});
			$M(Modifier.PUBLIC, "void", "sendAsync", $(P(GlobalTypes.STRING, "to")), ()->{
				$("com.google.appengine.api.taskqueue.Queue queue = com.google.appengine.api.taskqueue.QueueFactory.getQueue(\"pushs\");");
				$("queue.add(TaskOptions.Builder.withUrl(\"/api/push/" + entidad.getSimpleName() + "\").method(com.google.appengine.api.taskqueue.TaskOptions.Method.POST)");
				$(".payload(\"{\\\"to\\\":\" +");
				$("jcrystal.JSONUtils.jsonQuote(to) + ");
				$("\", \\\"notification\\\":{\\\"title\\\":\" + ");
				$("jcrystal.JSONUtils.jsonQuote(" + (campoTitle == null ? "\""+title+"\"" : campoTitle.name()) + ") + ");
				$("\",\\\"text\\\":\" + ");
				$("jcrystal.JSONUtils.jsonQuote(" + (campoBody == null ? "\""+body+"\"" : campoBody.name()) + ") + ");
				$("\",\\\"sound\\\": \\\"default\\\"\" +");
				$("\"},\\\"data\\\": \" + ");
				$("this.toJson() + ");
				$("\"}\"));");
			});
			$M(Modifier.PUBLIC, "int", "send", $(P(GlobalTypes.STRING, "to")), ()->{
				$("try", ()->{
					openConnection(context, this, ()->{
						$("Charset UTF8 = java.nio.charset.Charset.forName(\"UTF-8\");");
						$("out.write(\"{\\\"to\\\":\".getBytes(UTF8));");
						$("out.write(jcrystal.JSONUtils.jsonQuote(to).getBytes(UTF8));");
						$("out.write((\", \\\"notification\\\":{\\\"title\\\":\").getBytes(UTF8));");
						$("out.write(jcrystal.JSONUtils.jsonQuote(" + (campoTitle == null ? "\""+title+"\"" : campoTitle.name()) + ").getBytes(UTF8));");
						$("out.write(\",\\\"text\\\":\".getBytes(UTF8));");
						$("out.write(jcrystal.JSONUtils.jsonQuote(" + (campoBody == null ? "\""+body+"\"" : campoBody.name()) + ").getBytes(UTF8));");
						$("out.write(\",\\\"sound\\\": \\\"default\\\"\".getBytes(UTF8));");
						$("out.write(\"},\\\"data\\\": \".getBytes(UTF8));");
						$("out.write(this.toJson().getBytes(UTF8));");
						$("out.write(\"}\".getBytes(UTF8));");
						$("out.flush();");
					});
					$("return respGCM;");
				});
				$SingleCatch("Exception ex", "ex.printStackTrace();");
				$("return -1;");
			});
			context.output.addSection(entidad, "PUSH", this);
		}};
		List<IAccessor> camposJson = campos.stream().map(f->f.accessor()).collect(Collectors.toList());
		camposJson.add(0, new CrystalConstantField(GlobalTypes.STRING, "t", "\"" + entidad.getSimpleName() + "\""));
		JavaCode JSON = new JavaCode(1);
		context.utils.generadorToJson.generateJsonify(entidad, JSON, null, camposJson);
		context.output.addSection(entidad, "JSON", JSON);
	}
	private static void openConnection(ClientContext context, AbsCodeBlock parent, Runnable block){
		parent.new B(){{
				$("java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(\"https://fcm.googleapis.com/fcm/send\").openConnection();");
				$("connection.setRequestMethod(\"POST\");");
				$("connection.setConnectTimeout(30000);");
				$("connection.setRequestProperty(\"Accept\", \"application/json\");");
				$("connection.setRequestProperty(\"Authorization\", \"key=" + context.input.SERVER.INTEGRATION.FIREBASE.getGCMToken() + "\");");
				$("connection.setRequestProperty(\"Content-Type\", \"application/json\");");
				$("connection.setDoOutput(true);");
				$("connection.connect();");
				$("try(java.io.OutputStream out = connection.getOutputStream())", block);
				$("int respGCM = connection.getResponseCode();");
				$("connection.disconnect();");
		}};
	}
}

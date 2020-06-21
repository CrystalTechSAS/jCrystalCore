package jcrystal.server;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;

import jcrystal.configs.clients.ResourceType;
import jcrystal.configs.server.dbs.DBInstance;
import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.async.Cron;
import jcrystal.reflection.annotations.async.Queue;
import jcrystal.server.async.Async;
import jcrystal.server.web.servlet.GeneradorServlet;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class GenerarQueueCode {
	
	private ClientContext context;
	
	public GenerarQueueCode(ClientContext context) {
		this.context = context;
	}
	
	public void generar() throws Exception{
		if(!context.input.SERVER.isAppEngine)
			return;
		generarArchivoQueues();
		new JavaCode() {{
			$("package " + context.input.SERVER.WEB.getBasePackage()+".servlets;");
			$("import jcrystal.utils.InternalException;");
			$("import jcrystal.utils.ValidationException;");
			$("import javax.servlet.http.*;");
			$("import java.io.IOException;");
			$("import static jcrystal.utils.ServletUtils.*;");
			$("@javax.servlet.annotation.WebServlet(name = \"ServletjCrystalAsync\",urlPatterns = {\"/jcrystal/async\"})");
			$("public class ServletJCrystal extends HttpServlet", () -> {
				$("private static final long serialVersionUID = " + context.back.random.nextLong() + "L;");
				$("private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ServletJCrystal.class.getName());");
				$("public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException",() -> {
					$if("(!\"0.1.0.2\".equals(req.getRemoteAddr()) && com.google.appengine.api.utils.SystemProperty.environment.value() == com.google.appengine.api.utils.SystemProperty.Environment.Value.Production)", ()->{
						$("resp.setStatus(401);");
						$("return;");
					});
					$("final String path = req.getServletPath();");
					$("resp.setContentType(\"application/json\");");
					$("resp.setCharacterEncoding(\"UTF-8\");");
					$("try", () -> {
						$("switch(path)", () -> {
							$("case \"/jcrystal/async\" : ",()->{
								$("jcrystal.queue.Async.dequeue(req);");
								$("resp.setStatus(200);");
								$("resp.getWriter().print(\"{\\\"success\\\":1}\");");
								$("break;");
							});
							$("default: send404(resp);break;");
						});
					});
					GeneradorServlet.putCatchs(context, this);
				});
			});
			context.output.exportFile(this, context.input.SERVER.WEB.getBasePackage().replace(".", "/") + "/servlets/ServletJCrystal.java");
		}};
		DBInstance db = context.input.SERVER.DB.list.stream().filter(f->f.type==DBType.GOOGLE_DATASTORE).findAny().orElse(context.input.SERVER.DB.MAIN);
		new JavaCode() {{
			$("package jcrystal.server.async;");
			$("import jcrystal.PrintWriterUtils;");
			$("public class JQueue", () -> {
				context.data.queues.forEach((key,queueDesc)->{
					$("public static class "+key,()->{
						Queue queue = queueDesc.queue;
						queueDesc.tasks.forEach(m->{
							Async asyncAn = m.getAnnotation(Async.class);
							
							StringSeparator args = new StringSeparator(", ");
							if(asyncAn.namabled())
								args.add("String taskName");
							if(asyncAn.timeable())
								args.add("long waitTimeMillis");
							m.parametros.stream().map(p->{
								if(p.type().isAnnotationPresent(jEntity.class))
									return $(context.data.entidades.get(p.type()).key.getSingleKeyType()) + " " + p.nombre;
								return $(p.type()) + " " + p.nombre;
							}).forEach(args::add);
							$M(PUBLIC|STATIC, "void", "task"+StringUtils.capitalize(m.name()), args, ()->{
								$("com.google.appengine.api.taskqueue.Queue queue = com.google.appengine.api.taskqueue.QueueFactory.getQueue(\"" + queue.name() + "\");");
								$("com.google.appengine.api.taskqueue.TaskOptions $task = com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl(\"" + m.getPath(null) + "\").method(com.google.appengine.api.taskqueue.TaskOptions.Method."+m.tipoRuta.name()+");");
								if(asyncAn.namabled())
									$("$task=$task.taskName(taskName);");
								if(asyncAn.timeable())
									$("$task=$task.countdownMillis(waitTimeMillis);");
								m.parametros.stream().filter(f->f.tipoRuta.isGetLike()).forEach(p->{
									if (p.type().is(long.class))
										$("$task=$task.param(\"" + p.nombre + "\", Long.toString(" + p.nombre + "));");
									else if (p.type().is(int.class))
										$("$task=$task.param(\"" + p.nombre + "\", Integer.toString(" + p.nombre + "));");
									else if (p.type().is(String.class))
										$if(p.nombre +" != null",()->{
											$("$task=$task.param(\"" + p.nombre + "\", " + p.nombre + ");");
										});
									else if (p.type().is(double.class))
										$("$task=$task.param(\"" + p.nombre + "\", Double.toString(" + p.nombre + "));");
									else if (p.type().is(boolean.class))
										$("$task=$task.param(\"" + p.nombre + "\", Boolean.toString(" + p.nombre + "));");
									else if (p.type().is(Long.class))
										$if(p.nombre +" != null",()->{
											$("$task=$task.param(\"" + p.nombre + "\", Long.toString(" + p.nombre + "));");
										});
									//else throw new NullPointerException(p.p.type().name());
								});
								if(m.tipoRuta.isPostLike()) {
									$("java.io.ByteArrayOutputStream $body = new java.io.ByteArrayOutputStream();");
									$("try(java.io.PrintWriter _pw = new java.io.PrintWriter($body))",()->{
										context.utils.generadorToJson.fillBody(this, m.parametros.stream().filter(param -> param.tipoRuta.isPostLike()).map(context.utils.getEntityUtils()::changeToKeys).collect(Collectors.toList()));
									});
									$("$task.header(\"Content-Type\", \"application/json\");");
									
									$("$task.payload($body.toByteArray());");
								}
								$("queue.add(jcrystal.context.CrystalContext.get()."+db.getDBName()+"().getTxn(), $task);");
							});
						});
					});
				});
			});
			context.output.exportFile(this, "jcrystal/server/async/JQueue.java");
		}};
		new JavaCode() {{
			$("package jcrystal.server.async;");
			$("import java.lang.annotation.Retention;"); 
			$("import java.lang.annotation.RetentionPolicy;");
			$("@Retention(RetentionPolicy.RUNTIME)");
			$("public @interface Async", () -> {
				$("Q name();");
				$("boolean namabled() default false;");
				$("boolean timeable() default false;");
				$("public enum Q",()->{
					$(context.data.queues.keySet().stream().collect(Collectors.joining(", "))+";");
				});
			});
			context.output.exportFile(this, "jcrystal/server/async/Async.java");
		}};
	}
	private void generarArchivoQueues() throws Exception {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<queue-entries>");
			context.data.queues.forEach((key, q)->{
				pw.println("	<queue>");
				pw.println("		<name>"+q.queue.name()+"</name>");
				pw.println("		<rate>"+q.queue.rate()+"</rate>");
				pw.println("	</queue>");
			});
			pw.println("	<queue>");
			pw.println("		<name>jcrystal</name>");
			pw.println("		<rate>10/s</rate>");
			pw.println("	</queue>");
			pw.println("	<queue>");
			pw.println("		<name>firebase</name>");
			pw.println("		<rate>100/s</rate>");
			pw.println("	</queue>");
			pw.println("</queue-entries>");
		}
		context.output.send(ResourceType.WEB_INF, sw.toString(), "queue.xml");
		sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<cronentries>");
			for(JCrystalWebService ws : context.data.CRONS){
				pw.println("	<cron>");
				pw.println("		<url>" + ws.getPath(null) + "</url>");
				pw.println("		<schedule>"+ws.getAnnotation(Cron.class).value()+"</schedule>");
				pw.println("		<timezone>"+ws.getAnnotation(Cron.class).timeZone()+"</timezone>");
				pw.println("	</cron>");
			}
			
			pw.println("</cronentries>");
		}
		context.output.send(ResourceType.WEB_INF, sw.toString(), "cron.xml");
	}
}

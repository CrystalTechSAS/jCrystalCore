package jcrystal.server.databases;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jcrystal.configs.server.dbs.DBInstanceBigQuery;
import jcrystal.configs.server.dbs.DBInstanceFirestore;
import jcrystal.configs.server.dbs.DBInstanceRealtimeDB;
import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.utils.InternalException;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class ContextGenerator {
	private ClientContext context;
	public ContextGenerator(ClientContext context) {
		this.context = context;
	}
	public void generar()throws Exception{
		generateAnnotations();
		new JavaCode(){{
			$("package jcrystal.context;");
			$("public class CrystalContext", ()->{
				$("public static final ThreadLocal<CrystalContext> userThreadLocal = new ThreadLocal<>();");
				$("public static void set()",()->{
					$("userThreadLocal.set(new CrystalContext());");
				});
				$("public static void clear()",()->{
					$("userThreadLocal.remove();");
				});
				$("public static CrystalContext get()",()->{
					$("CrystalContext ret = userThreadLocal.get();");
					$if("ret == null",()->{
						$("userThreadLocal.set(ret = new CrystalContext());");
					});
					$("return ret;");
				});
				$("public static void initialize()",()->{
					context.data.databases.forEach((k,db)->{
						switch (db.type) {
							case GOOGLE_REALTIMEDB:
								DBInstanceRealtimeDB rt = (DBInstanceRealtimeDB)db;
								$if("com.google.appengine.api.utils.SystemProperty.environment.value() == com.google.appengine.api.utils.SystemProperty.Environment.Value.Development",()->{
									if(rt.devDB() != null) {
										String url = rt.devDB();
										if(url.endsWith("/"))
											url = url.substring(0, url.length()-1);
										$("jcrystal.context."+StringUtils.camelizar(db.type.name())+".initialize(\"" + url + "\");");
									}
								});
								$else(()->{
									String url = rt.prodDB();
									if(url.endsWith("/"))
										url = url.substring(0, url.length()-1);
									$("jcrystal.context."+StringUtils.camelizar(db.type.name())+".initialize(\"" + url + "\");");
								});
								break;
							default:
								break;
						}
					});
					
					
;				});
				class Info{
					String type;
					String name;
					String params;
					public Info(String type, String name, String params) {
						this.type = type;
						this.name = name;
						this.params = params;
					}
				}
				Consumer<Info> $getter = (data)->{
					$("private "+data.type+" "+data.name+";");
					$("public "+data.type+" "+data.name+"()",()->{
						$ifNull(data.name,()->{
							$(data.name+" = new "+data.type+"("+data.params+");");	
						});
						$("return "+data.name+";");
					});
				};
				context.data.databases.forEach((k,db)->{
					switch (db.type) {
						case GOOGLE_DATASTORE:
							$getter.accept(new Info("jcrystal.context.DataStoreContext", db.getDBName(), ""));
							break;
						case GOOGLE_BIG_QUERY:
							DBInstanceBigQuery bq = (DBInstanceBigQuery)db;
							$getter.accept(new Info("jcrystal.context."+StringUtils.camelizar(db.type.name()), db.getDBName(),"\""+bq.getDatasetId()+"\""));
							break;
						case GOOGLE_FIRESTORE:
							DBInstanceFirestore fr = (DBInstanceFirestore)db;
							$getter.accept(new Info("jcrystal.context."+StringUtils.camelizar(db.type.name()), db.getDBName(),"\""+fr.getProjectId()+"\""));
							break;
						case GOOGLE_REALTIMEDB:
							$getter.accept(new Info("jcrystal.context."+StringUtils.camelizar(db.type.name()), db.getDBName(), ""));
							break;
						default:
							throw new NullPointerException("Unsupported DB type " + db.type);
					}
				});
			});
			context.output.exportFile(this, "jcrystal/context/CrystalContext.java");
		}};
		getTypeStream().filter(f->f!=DBType.GOOGLE_DATASTORE).forEach(type->{
			switch (type) {
				case GOOGLE_BIG_QUERY:
					new jcrystal.server.databases.google.bigtable.ContextGenerator(context).generate();
					break;
				case GOOGLE_DATASTORE:
					new jcrystal.server.databases.google.datastore.ContextGenerator(context).generate();
					break;
				case GOOGLE_REALTIMEDB:
					new jcrystal.server.databases.google.firebase.realtimedb.ContextGenerator(context).generate();
					break;
				case GOOGLE_FIRESTORE:
					new jcrystal.server.databases.google.firebase.firestore.ContextGenerator(context).generate();
					break;
					
				default:
					throw new InternalException(500, "Unssuported DB type : " + type);
			}
		});
	}
	public void generateAnnotations() {
		if(context.input.SERVER.DB.list.stream().anyMatch(f->f.id != null)) {
			new JavaCode(){{
				$("package jcrystal.annotations.db;");
				$("import java.lang.annotation.Retention;");
				$("import java.lang.annotation.RetentionPolicy;");
				$("@Retention(RetentionPolicy.RUNTIME)");
				$("public @interface DBReference", ()->{
					$("public DB main() default DB.DEFAULT;");
					$("public DB[] dbs() default {DB.DEFAULT};");
					$("public enum DB",()->{
						$(Stream.concat(Stream.of("DEFAULT"), context.input.SERVER.DB.list.stream().map(f->f.id)).filter(f->f!=null).collect(Collectors.joining(", ")));
					});
				});
				context.output.exportFile(this, "jcrystal/annotations/db/DBReference.java");
			}};
		}
	}
	
	private Stream<DBType> getTypeStream(){
		return context.data.databases.values().stream().map(f->f.type).distinct();
	}
	
}

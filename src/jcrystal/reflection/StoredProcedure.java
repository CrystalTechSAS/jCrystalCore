package jcrystal.reflection;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jcrystal.utils.StringSeparator;
import jcrystal.utils.langAndPlats.AbsCodeBlock;

public class StoredProcedure{
	String nombre;
	String[] argumentos;
	List<StoredProcedureResult> resultados = new ArrayList<>();
	String retorno;
	
	private static String INTERROGANTES = "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,";
	
	public StoredProcedure(String encabezado, List<String> resultados)throws Exception{
		String[] partes = encabezado.split(":");
		encabezado = partes[0];
		if(partes.length > 1)
			retorno = partes[1];
		int posA = encabezado.indexOf("(");
		int posC = encabezado.indexOf(")", posA+1);
		this.nombre = encabezado.substring(0, posA);
		this.argumentos = encabezado.substring(posA+1, posC).split(",");
		for(String res : resultados)
		this.resultados.add(new StoredProcedureResult(this, res));
		
	}
	public void crearMetodo(AbsCodeBlock pw){
		pw.new B(){{
				if(retorno == null)
					$("public static void " + nombre+"(");
				else
					$("public static "+retorno+" " + nombre+"(");
				
				StringSeparator parametros = new StringSeparator(", ");
				StringSeparator valores = new StringSeparator(", ");
				for(int e = 0; e < argumentos.length; e++){
					parametros.add(argumentos[e].trim());
					valores.add(argumentos[e].trim().split(" ")[1]);
				}
				for(int e = 0; e < resultados.size(); e++){
					parametros.add(resultados.get(e).interfaceName + " procesador"+e);
					valores.add("procesador"+e);
				}
				$M(Modifier.PUBLIC|Modifier.STATIC, retorno, nombre, parametros, "throws SQLException", ()->{
					$("try(Connection con = openConnection())",()->{
						if(retorno == null)
							$(nombre+"(con, " + valores + ");");
						else $("return "+nombre+"(con, " + valores + ");");
					});
				});
				$M(Modifier.PUBLIC|Modifier.STATIC, retorno, nombre, parametros.addFirst("Connection con"), "throws SQLException", ()->{
					if(retorno != null && retorno.equals("String"))
						$("String retorno = \"\";");
					$("try(CallableStatement st = con.prepareCall(\"{CALL "+ nombre +"("+INTERROGANTES.substring(0, argumentos.length*2 - 1)+")}\"))",()->{
						for(int e = 0; e < argumentos.length; e++){
							String[] arg = argumentos[e].trim().split(" +");
							if(arg[0].equals("int"))
								$("st.setInt("+(e+1)+", "+arg[1]+");");
							else if(arg[0].equals("String"))
								$("st.setString("+(e+1)+", "+arg[1]+");");
							else if(arg[0].equals("long"))
								$("st.setLong("+(e+1)+", "+arg[1]+");");
						}
						$("st.execute();");
						for(int e = 0; e < resultados.size(); e++){
							resultados.get(e).crearResultset(this.P, e);
							if(e != resultados.size() - 1)
								$("st.getMoreResults();");
						}
					});
					if(retorno != null)
						$("return retorno;");
				});
		}};
	}
	public void crearInterfaces(TreeSet<String> procesadas, ArrayList<String> INTERFACES){
		for(StoredProcedureResult spr : resultados)
		spr.crearInterfaz(procesadas, INTERFACES);
	}
}

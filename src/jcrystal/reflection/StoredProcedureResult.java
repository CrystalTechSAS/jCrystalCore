package jcrystal.reflection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jcrystal.utils.StringSeparator;
import jcrystal.utils.langAndPlats.AbsCodeBlock;

public class StoredProcedureResult{
	private static TreeSet<String> USED_NAMES = new TreeSet<>();
	private static TreeMap<String, String> INTERFACES = new TreeMap<>();
	static{
		if(new File("sps.txt").exists()){
			try(BufferedReader br = new BufferedReader(new FileReader("sps.txt"))){
				for(String line; (line = br.readLine()) != null ; ){
					USED_NAMES.add(line.split(":")[0]);
					INTERFACES.put(line.split(":")[1], line.split(":")[0]);
					System.out.println("Cargado "+line.split(":")[1]+" "+line.split(":")[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private String getNextInterfaceName()throws Exception{
		String key = getKey();
		String val = INTERFACES.get(key);
		if(val == null){
			for(int e=0; val == null;e++)
			if(!USED_NAMES.contains("Procesador"+e)){
				USED_NAMES.add("Procesador"+e);
				val = "Procesador"+e;
				INTERFACES.put(key, val);
				try(PrintWriter pw = new PrintWriter(new FileOutputStream("sps.txt", true))){
					pw.println(val+":"+key);
				}
			}
		}
		return val;
	}
	
	public final String multiplicidad;
	public final String[] params;
	public final String interfaceName;
	public final StoredProcedure padre;
	public StoredProcedureResult(StoredProcedure padre, String desc)throws Exception{
		this.padre = padre;
		String[] r = desc.trim().split(":");
		multiplicidad = r[0];
		
		String[] pParams = r[1].split(",");
		if(multiplicidad.equals("*")){
			params = new String[pParams.length+1];
			System.arraycopy(pParams, 0, params, 1, pParams.length);
			params[0] = "boolean coma";
		}else
			params = pParams;
		for(int e = 0; e < params.length; e++)
		params[e] = params[e].trim();
		interfaceName = getNextInterfaceName();
	}
	public void crearResultset(AbsCodeBlock pw, int pos){
		pw.new B(){{
				$("try(ResultSet rs = st.getResultSet())",()->{
					if(multiplicidad.equals("*")){
						$("for(boolean coma = false;rs.next();coma = true)",()->{
							StringSeparator param = new StringSeparator(", ").add("coma");
							int[] indx = {1};
							Arrays.stream(params).skip(multiplicidad.equals("*")?1:0).map(f->f.trim().split(" ")[0]).forEach(f->{
								if(f.equals("String"))
									param.add("rs.getString("+(indx[0]++)+")");
								else if(f.equals("int"))
									param.add("rs.getInt("+(indx[0]++)+")");
								else if(f.equals("long"))
									param.add("rs.getLong("+(indx[0]++)+")");
								else
									throw new NullPointerException();
							});
							if(padre.retorno != null)
								$("retorno += procesador"+pos+".procesar(" + param + ");");
							else
								$("procesador"+pos+".procesar(" + param + ");");
						});
					}else{
						$("if(rs.next())",()->{
							StringSeparator param = new StringSeparator(", ");
							int[] indx = {1};
							Arrays.stream(params).map(f->f.trim().split(" ")[0]).forEach(f->{
								if(f.equals("String"))
									param.add("rs.getString("+(indx[0]++)+")");
								else if(f.equals("int"))
									param.add("rs.getInt("+(indx[0]++)+")");
								else if(f.equals("long"))
									param.add("rs.getLong("+(indx[0]++)+")");
								else
									throw new NullPointerException();
							});
							if(padre.retorno != null)
								$("retorno += procesador"+pos+".procesar(" + param + ");");
							else $("procesador"+pos+".procesar(" + param + ");");
						});
					}
				});
		}};
	}
	public String getKey(){
		String key = getParams();
		if(padre.retorno!= null)
			key = padre.retorno+","+key;
		return key;
	}
	public String getParams(){
		String key = "";
		for(String p : params){
			if(key.isEmpty())
				key = p;
			else key+=", "+p;
		}
		return key;
	}
	public void crearInterfaz(Set<String> procesadas, List<String> out){
		if(!procesadas.contains(interfaceName)){
			procesadas.add(interfaceName);
			out.add("\tpublic static interface "+interfaceName+"{");
			String signatura = padre.retorno != null?"\t\tpublic String procesar(":"\t\tpublic void procesar(";
			signatura += getParams();
			out.add(signatura + ");");
			out.add("\t}");
		}
	}
}

package jcrystal.utlis.generators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorTuplas {
	
	public static void main(String[] args) throws Exception{
		final int MAX = 10;
		IntStream.rangeClosed(2, MAX).forEach(level->{
			List<String> code = new JavaCode() {{
					$("package jcrystal.results;");
					$("public class Tupla"+level+"<"+createTypes(0,level)+">",()->{
						for(int i = 0; i < level; i++)
						$("public final "+(char)('A'+i)+" v"+i+";");
						$("public Tupla"+level+"("+createParams(0, level)+")",()->{
							for(int i = 0; i < level; i++)
							$("this.v"+i+" = v"+i+";");
						});
						IntStream.range(2, level-1).forEach(i->{
							IntStream.range(0, level - i).forEach(j->{
								$("public Tupla"+level+"("+fix(createParams(0, j)+",Tupla" + i + "<" + createTypes(j,j+i)+"> tupla,"+createParams(j+i, level))+")",()->{
									for(int x = 0; x < j; x++)
									$("this.v"+x+" = v"+x+";");
									for(int x = j; x < j+i; x++)
									$("this.v"+x+" = tupla.v"+(x-j)+";");
									for(int x = j+i; x < level; x++)
									$("this.v"+x+" = v"+x+";");
								});
							});
						});
						IntStream.rangeClosed(1, MAX-level).forEach(i->{
							if(i==1){
								$("public <"+createTypes(level, level+i)+"> Tupla"+(i+level)+"<"+createTypes(0,i+level)+"> append("+createTypes(level, level+i)+" tupla)",()->{
									$("return new Tupla"+(i+level)+"<>("+createValues(0, level)+",tupla);");
								});
							}else
								$("public <"+createTypes(level, level+i)+"> Tupla"+(i+level)+"<"+createTypes(0,i+level)+"> append(Tupla"+i+"<"+createTypes(level, level+i)+"> tupla)",()->{
								String aditionals = IntStream.range(level, level+i).mapToObj(f->"tupla.v"+(f-level)).collect(Collectors.joining(","));
								$("return new Tupla"+(i+level)+"<>("+createValues(0, level)+","+aditionals+");");
							});
						});
					});
			}}.getCode();
			try {
				Files.write(new File("/Users/gasotelo/Documents/workspace/jCrystalUtils/src/jcrystal/results/Tupla"+level+".java").toPath(), code);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	private static String fix(String param) {
		if(param.startsWith(","))param = param.substring(1);
			if(param.endsWith(","))param = param.substring(0, param.length()-1);
			return param;
	}
	private static String createParams(int s, int e) {
		return IntStream.range(s, e).mapToObj(d -> (char)('A'+d)+" v"+d).collect(Collectors.joining(","));
	}
	private static String createValues(int s, int e) {
		return IntStream.range(s, e).mapToObj(d -> "v"+d).collect(Collectors.joining(","));
	}
	private static String createTypes(int s, int e) {
		return IntStream.range(s, e).mapToObj(d -> ""+(char)('A'+d)).collect(Collectors.joining(","));
	}
}

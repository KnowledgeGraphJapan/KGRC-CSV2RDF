package jp.kgrc.challenge2019;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;


public class TSV2RDFforKGRC2019 {
    String dataFolder = "data/KGRC2019";
    File saveFile;

    String license="https://creativecommons.org/licenses/by/4.0/";
    String attributionName="SIG-SWO, JSAI";

    ObjData objdata;//小説ごとに分けるとき = new ObjData();


	public static void main(String[] arg){
		new TSV2RDFforKGRC2019();
	}



	public TSV2RDFforKGRC2019(){
		File dir = new File(dataFolder);


		//小説ごとにKG作成
		File[] files =  dir.listFiles();
		for(int i=0;i<files.length;i++) {
			if(files[i].getName().endsWith(".tsv")) {
				CSV2RDFforSWOWS2019(files[i]);
			}
		}

	}




	void CSV2RDFforSWOWS2019(File f){
		try {

			//#kozaki 2019/06/04 Object管理用
		    objdata = new ObjData();


			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(f),"UTF-8"));

			String line="";
            //baseFileName = "";
			String filename = f.getName().replaceAll(".tsv", "");
			saveFile = new File(dataFolder+"/"+filename+".ttl");

			FileOutputStream out;
			out = new FileOutputStream(saveFile);
			OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
			BufferedWriter bw = new BufferedWriter(ow);

			File logfile = new File(dataFolder+"/"+filename+"_log.txt");
			BufferedWriter bwlog = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logfile), "SJIS"));
			//bwlog.write("uri,obj_source,prop,obj,obj_lbl,eng_lbl,type_lbl\n");

			//ヘッダ書き込み
			String comment ="\"作成日（2019-08-26） \"@ja";
			bw.write(this.getRDFHeader(f.getName().replace(".tsv", ""), comment));

			//System.out.println(this.getRDFHeader(comment));
			//ヘッダ無視
			line = br.readLine();

			//boolean readyID=false;
			String baseIRI = "http://kgc.knowledge-graph.jp/data/";
			String base = baseIRI+filename+"/";//"SpeckledBand/";

			//Objectとして参照するリソースのbaseURI→小説毎用
			String Objbase = baseIRI+filename+"/";

			//述語として参照するリソースのbaseURI→小説間で共通
			String Propbase = baseIRI+"predicate/";


			String prop; //説明用の目的語をつなぐプロパティ
			//String uri_base ="";
			String id ="";
			String uri ="";
			String obj;
			String obj_source;
			String obj_lbl;
			String eng_lbl;
			String time;
			String type_lbl;
			StatData sdata=null;// = new StatData();


			//各CSVファイルの処理
			while (br.ready()) {
				// System.out.println(line);
	            line = br.readLine();
	            String[] info = line.split("\t");
	            //変数のクリア（URI以外）
	            prop = "";
	            obj ="";
				obj_source ="";
				obj_lbl ="";
				eng_lbl ="";
				time  = "";
				type_lbl ="";

	            if(info.length>0) {
	            	//IDが変わった＝次のシーンに移行した際の処理
		            if(!id.equals(info[0].trim())) {
		            	System.out.println(line);
		            	//bwlog.write(line);

		            	if(sdata!=null) { //前のIDの終了処理
		            		bw.write(sdata.getTTL());
		            		}

		            	//変数のクリア
		            	id=info[0];
		            	uri = base + id;

		            	if(id.length()==1) {uri = base + "00"+id;}
		            	else if(id.length()==2) {uri = base + "0"+id;}


		            	sdata = new StatData();
		                sdata.setUri(uri);
		            	sdata.setType("Situation");//クラス名は要検討　別案）Scene

		            	//「原文」の処理
		            	/*if(!"".equals(info[2].trim())) {
			            	sdata.addTriple("kgc:source  \""+info[2]+"\"@ja");
			            	//sdata.addTriple("kgc:source  \""+info[5].replaceAll("\"", "'")+"\"@en");
		            	}
		            	if(!"".equals(info[5].trim())) {
			            	//sdata.addTriple("kgc:source  \""+info[2]+"\"@ja");
			            	sdata.addTriple("kgc:source  \""+info[5].replaceAll("\"", "'")+"\"@en");
		            	}*/
		            }
	            }

	            //-----プロパティの判定処理---------
	            if(info.length>2) {
	        		if(info[1].indexOf("（")>0) {
	    				prop = info[1].substring(0,info[1].indexOf("（"));
	    			}
	    			else{
	    				prop = info[1] ;//Property欄
	            	}

	        		//プロパティの表記統一処理
	        		//行頭を「小文字」，スペースは「_」で置換
	        		if(prop.length()>1) {
	        			prop = prop.toLowerCase().substring(0, 1)+prop.substring(1);
	        			prop = prop.replaceAll(" ", "_").trim();
	        		}


	        		if(prop.equals("who")) {
	        			prop="subject";
	        		}
	        		else if(prop.indexOf("対象文")>=0) {
	        			prop="source";
	        		}
	        		else if(prop.indexOf("情報源")>=0){
	        			prop = "infoSource";
	        			String infotype = info[2].trim();
	        			if(infotype.indexOf("の考え")>=0) {
	        				sdata.setType("Thought");
	        			}
	        			else if(infotype.indexOf("の発言")>=0) {
	        				sdata.setType("Statement");
	        			}
	        			else if(infotype.indexOf("への発言")>=0) {
	        				sdata.setType("Talk");
	        			}
	        			else {
	        				bwlog.write("【未処理】"+prop+"\n");
	        			}
	        		}
//	        		else if(prop.indexOf("その他の修飾")>=0) {
//	        			prop="adjunct";
//	        		}
//	        		else if(prop.indexOf("関連する文")>=0){
//	        			prop=info[3].replaceAll(" ", "_").replaceAll("/", "_").trim();
//	        			//特定の文間の関係の種類を変換する処理
//	        			if(prop.equals("because")) {
//	        				prop="why";
//	        			}
//	        			//関係の種類が未記入の時
//	        			if(info[3].trim().equals("")) {
//	        				prop="ERROR-文の関係未定義";
//	        				}
//	        		}
	        	}

        		//-----Objectの情報取得処理---------
	            if(info.length>2) {
	            	if(!"".equals(info[2].trim()) ) {//「原文」欄
	        			//obj = base + info[2].trim();
	        			obj_lbl = info[2].trim();
	        			obj_source = obj_lbl;

	        			//英語記述　→　すべての要素に対応しているので，判定が必要
	        			if(info.length>3) {
	        				eng_lbl = info[3].trim();
	        			}
	              		if(eng_lbl.equals("")) {
	              			eng_lbl = "NO_ENG_LABEL";
	              		}

	              		if(prop.indexOf("when")>=0){
        					type_lbl = "AbstractTime";
	        			}

	              		if(info.length>5) {
	              			if(!info[5].startsWith("#")) {
	              				type_lbl = info[5].trim();
	        				}
	        				if(type_lbl.equals("Object")) {
	        					type_lbl = "PhysicalObject";
	        				}

	        			}


	              		if(prop.equals("source")) {//「原文」の処理
			            	if(!"".equals(obj_lbl)) {
				            	sdata.addTriple("kgc:source  \""+obj_lbl.replaceAll("\"", "'")+"\"@ja");
				            }
			            	if(!eng_lbl.equals("NO_ENG_LABEL")) {
				            	sdata.addTriple("kgc:source  \""+eng_lbl.replaceAll("\"", "'")+"\"@en");
			            	}
	        			}
	            		else if(prop.equals("infoSource")) {
	            			obj_lbl = "";//"〇〇の考え"などがラベルにならないように処理

		            		if(eng_lbl.startsWith("Speech to")) {//"〇〇の△△への発言"を処理
	            				String[] eng_lbls = eng_lbl.replaceAll("Speech to", "").split(" of ");
	            				String[] obj_lbls = {obj_lbl,obj_lbl};
	            				if(obj_lbl.indexOf(",")>0) {
	            					obj_lbls = obj_lbl.split(",");
	            				}
	            				//○○の処理
	            				obj = Objbase + sdata.checkIRI(eng_lbls[0].trim());//IRIの禁則処理
			            		objdata.addObjData(Objbase, obj_source, prop, obj, obj_lbls[0].trim(), eng_lbls[0].trim(), type_lbl);
			            		//sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
			            		sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
			            		bwlog.write(uri+","+obj_source+","+prop+","+obj+","+obj_lbl+","+obj_lbls[0]+","+type_lbl+"\n");

			            		//△△の処理
			            		prop = "infoReceiver";
	            				obj = Objbase + sdata.checkIRI(eng_lbls[1].trim());//IRIの禁則処理 eng_lbls[1].trim().replaceAll(" ", "_");//IRIの禁則処理が必要
			            		objdata.addObjData(Objbase, obj_source, prop, obj, obj_lbls[1].trim(), eng_lbl, type_lbl);
			            		sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
			            		bwlog.write(uri+","+obj_source+","+prop+","+obj+","+obj_lbl+","+obj_lbls[1]+","+type_lbl+"\n");

	            			}
		            		else {
		        				eng_lbl = eng_lbl.replaceAll("Idea of ", "").replaceAll("Remark by ", "").trim();
		            			obj = Objbase +  sdata.checkIRI(eng_lbl.trim());//IRIの禁則処理 eng_lbl.replaceAll(" ", "_");//IRIの禁則処理が必要
		            			if(!eng_lbl.equals("NO_ENG_LABEL")) {
			            			objdata.addObjData(Objbase, obj_source, prop, obj, obj_lbl, eng_lbl, type_lbl);
			            			sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
			            			//bwlog.write(uri+","+obj_source+","+prop+","+obj+","+obj_lbl+","+eng_lbl+","+type_lbl+"\n");
		            			}
		        			}

	            		}
	            		else if(isRelatedStat(obj_lbl)) {
	            			if(obj_lbl.contains("-")) {
	            				System.out.println("要処理："+obj_lbl);
	            				bwlog.write("要処理："+obj_lbl+"\n");
	            				String[] rt = obj_lbl.replaceAll(" ", "").trim().split(",");
	            				for(int i=0;i<rt.length;i++) {
	            					if(rt[i].contains("-")) {
	            						String rg = parseIndexRange(rt[i]);
	            						bwlog.write("==>rg："+rg+"\n");
	            						obj_lbl=obj_lbl.replace(rt[i], rg);
	            					}
	            				}
	            				bwlog.write("==> "+obj_lbl+"\n");
	            			}

            				bwlog.write("関連："+obj_lbl+"\n");
            				System.out.println("関連："+obj_lbl);
            				String[] rt = obj_lbl.trim().split(",");
            				for(int i=0;i<rt.length;i++) {
            					String num = rt[i].trim() ;
            					obj = base + num;
            					if(num.length()==1) {obj = base + "00"+num;}
        		            	else if(num.length()==2) {obj = base + "0"+num;}

            					sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
            				}
            			}
	            		else {//subject, hasPredicate, その他の処理
	            			String obj_base;
	            			if((prop.indexOf("hasPredicate")>=0)||(prop.indexOf("hasProperty")>=0)) {
	            				obj = Propbase + sdata.checkIRI(eng_lbl.trim());//IRIの禁則処理 eng_lbl.replaceAll(" ", "_");
	            				obj_base = Propbase;
	            			}
	            			else {
	            				obj = Objbase + sdata.checkIRI(eng_lbl.trim());//IRIの禁則処理 eng_lbl.replaceAll(" ", "_");//IRIの禁則処理が必要
	            				obj_base = Objbase;
	            			}
	            			if(!eng_lbl.equals("NO_ENG_LABEL")) {
		            			objdata.addObjData(obj_base, obj_source, prop, obj, obj_lbl, eng_lbl, type_lbl);
		            			//bwlog.write(uri+","+obj_source+","+prop+","+obj+","+obj_lbl+","+eng_lbl+","+type_lbl+"\n");
		            			
		            			//ANDの処理
		            			String objid = info[3].trim();
		            			if(objid.indexOf("AND")>0) {
		            				System.out.println("AND処理");
		            				String[] obj_ids = objid.split("AND");
		            				for(int i=0;i<obj_ids.length;i++) {
		            					obj = base + obj_ids[i].trim();
		            					sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
		            				}
		            			}
		            			else {
		            				obj = base+objid;
		            				sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
		            			}
	            			}
	            		}

	        		}
	            }

//
        		if(info.length>4) {
            		time = info[4].trim();
            		if(!"".equals(time) && !time.startsWith("#")) {
		            	//sdata.addTriple("kgc:source  \""+info[2]+"\"@ja");
            			String tdata = time;
            			if(!time.contains("T")) {
            				tdata += "T00:00:00"; //日付のみ
            			}
            			else if(time.length() == "1887-07-08T23".length()) {
            				tdata += ":00:00";
            			}
            			else if(time.length() == "1887-07-08T23:45".length()) {
            				tdata += ":00";
            			}

            			sdata.addTriple("kgc:time  \""+tdata+"\"^^xsd:dateTime");
	            	}

        		}






//
//	            		//TODO ?ANDなどの特殊処理
////	            		else {//Objの定義処理
////	            			if(eng_lbl.indexOf(" AND ")>0) {
////	            				String[] obj_lbls = eng_lbl.split(" AND ");
////	            				for(int i=0;i<obj_lbls.length;i++) {
////	            					obj = base + obj_lbls[i].trim().replaceAll(" ", "_");//IRIの禁則処理が必要
////			            			objdata.addObjData(base, obj_source, prop, obj, obj_lbl, obj_lbls[i].trim(), type_lbl);
////			            			sdata.addTriple("kgc:"+prop+"   <"+obj+ ">");
////			            			bwlog.write(uri+","+obj_source+","+prop+","+obj+","+obj_lbl+","+obj_lbls[i]+","+type_lbl+"\n");
////	            				}
////	            			}

			}
			if(sdata!=null) { //前のIDの終了処理【最終】
        		//bw.write("    rdfs:comment  "+comment+" .\n");
        		bw.write(sdata.getTTL());
        	}


			//Ｏｂｊｅｃｔとして参照されたリソースの定義を出力
			bw.write(objdata.getTTL());


			br.close();
			bw.close();
			bwlog.close();

			//System.out.println(f.getName()+":::RDFファイル（Turtle形式）が出力されました");
			//JOptionPane.showMessageDialog(null, "RDFファイル（Turtle形式）が出力されました");


			}
			catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e.toString());
			e.printStackTrace();
		}

		//他のRDFフォーマットに変換
		//changeRdfFormat();
	}

	//TODO アルファベットで枝番が付く場合があるので，要検討．仮にabcを判定対象に入れている
	boolean isRelatedStat(String num) {
	    String regex =  "[0-9,-abc]*";
	    //Pattern p = Pattern.compile(regex);
	    return Pattern.matches(regex,num.replaceAll(" " , "").trim());
	}

	String parseIndexRange(String rg) {
		int sep = rg.indexOf("-");
		int st = Integer.parseInt(rg.substring(0,sep));
		int end = Integer.parseInt(rg.substring(sep+1));
		System.out.println("st end =" + st +" => " +end);
		String rgst = "";
		for(int i=st;i<=end;i++) {
			rgst += i;
			if(i!=end) {
				rgst += ",";
			}
		}
		System.out.println("==>rgst " + rgst);
		return rgst;
	}

	//変換すべきデータがあるかのチェック
	boolean checkData(String[] info) {
		if(info.length<3){
			return false;
			}

		if(!"".equals(info[2].trim())){return true;}
		else if(!"".equals(info[3].trim())){return true;}
		else if(!"".equals(info[4].trim())){return true;}
		else if(!"".equals(info[5].trim())){return true;}

		return false;
		}

	String[] getCsvArray(BufferedReader br){

		ArrayList<String> list = getCSV(br);
		//System.out.print("getCsvArray:");
		String[] info = new String[list.size()];
		for(int i=0;i<list.size();i++){
			info[i]=list.get(i);
			//System.out.print(info[i]);
		}
		//System.out.print("\n");
		return info;
	}


	//CSVの処理用メソッド：現在未使用
	ArrayList<String> getCSV(BufferedReader br){
		ArrayList<String> list = new ArrayList<String>();
		String line="";
		String token="";
		int index=0;

		try {
			line = br.readLine();
			//System.out.println("***"+line);

			while (true) {
	            //"  "で囲まれたトークンの処理
	            if(line.startsWith("\"")){
	            	line = line.substring(1);
	            	//次の"まで探す
		            while(br.ready()){
	            		if(!line.contains("\"")){
	            			line += "\n"+br.readLine();
	            		}
	            		else{
	            			break;
	            		}
	            	}

		            index = line.indexOf("\"");
		            if(index>0){
		            	token = line.substring(0, index).trim();
		            	list.add(token);

		            	if(line.length()>index+1){
			            	line = line.substring(index+1);
//			            	index = line.indexOf(",");
//			            	if(index>=0){
//			            		line = line.substring(index+1);
//			            	}
		            	}
			            else{
			            	return list;
			            }
		            }

		            index = line.indexOf(",");
	            	if(index>=0){
	            		line = line.substring(index+1);
	            	}

	            }
		        else{

		            index = line.indexOf(",");
					if(index>0){
						token = line.substring(0, index).trim();
			            list.add(token);
			            line = line.substring(index+1);
					}
					else if(index == 0){
		            	if(line.length()>1){
		            		list.add("");
		            		line = line.substring(1);
		            	}
		            	else{
		            		return list;
		            	}
		            }
		            else{
		            	if(line.length()>=1){
		            		list.add(line.trim());
		            	}
		            	return list;
		            }
		        }
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.toString());
		}

		return list;
	}

	String getRDFHeader(String filename, String comment){
		String header =
				 "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
				+"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
			    +"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
			    +"@prefix cc:   <http://creativecommons.org/ns#> .\n"
				+"@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .\n"
//				+"@prefix rss:  <http://purl.org/rss/2.0/> .\n"
//				+"@prefix geo:  <http://www.w3.org/2003/01/geo/wgs84_pos#> .\n"
//				+"@prefix schema: <http://schema.org/> .\n"
//				+"@prefix ldo:  <http://data.lodosaka.jp/property#> .\n"
//				+"@prefix ic: <http://imi.ipa.go.jp/ns/core/rdf#> .\n"
				+"@prefix kgc: <http://kgc.knowledge-graph.jp/ontology/kgc.owl#> . \n"
//				+"@prefix kd: <http://kgc.knowledge-graph.jp/data/SpeckledBand/> . \n"

				+"<http://kgc.knowledge-graph.jp/data/"+filename+"/metadata>\n"
				+"    rdfs:comment  "+comment+" ;\n"
				+"	cc:attributionName \""+this.attributionName+"\" ;\n"
				+"	cc:license <"+this.license+"> .\n"
				;

		//Class, Property定義


	    header +="#################################################################\n";
	    header +="#    Annotation properties\n";
	    header +="#################################################################\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#source\n";
	    header +="kgc:source rdf:type owl:AnnotationProperty .\n";

	    header +="#################################################################\n";
	    header +="#    Object Properties\n";
	    header +="#################################################################\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#ActionOption\n";
	    header +="kgc:ActionOption rdf:type owl:ObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Not\n";
	    header +="kgc:Not rdf:type owl:ObjectProperty ;\n";
	    header +="         rdfs:subPropertyOf kgc:ActionOption .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#RelationBetweenScene\n";
	    header +="kgc:RelationBetweenScene rdf:type owl:ObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#SceneObjectProperty\n";
	    header +="kgc:SceneObjectProperty rdf:type owl:ObjectProperty ;\n";
	    header +="                         rdfs:subPropertyOf kgc:SceneProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#SceneProperty\n";
	    header +="kgc:SceneProperty rdf:type owl:ObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#TargetObjProperty\n";
	    header +="kgc:TargetObjProperty rdf:type owl:ObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#adjunct\n";
	    header +="kgc:adjunct rdf:type owl:ObjectProperty ;\n";
	    header +="             rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#around\n";
	    header +="kgc:around rdf:type owl:ObjectProperty ;\n";
	    header +="            rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#at_the_same_time\n";
	    header +="kgc:at_the_same_time rdf:type owl:ObjectProperty ;\n";
	    header +="                      rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#because\n";
	    header +="kgc:because rdf:type owl:ObjectProperty ;\n";
	    header +="             rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#can\n";
	    header +="kgc:can rdf:type owl:ObjectProperty ;\n";
	    header +="            rdfs:subPropertyOf kgc:ActionOption .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#canNot\n";
	    header +="kgc:canNot rdf:type owl:ObjectProperty ;\n";
	    header +="            rdfs:subPropertyOf kgc:ActionOption .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#from\n";
	    header +="kgc:from rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#hasPart\n";
	    header +="kgc:hasPart rdf:type owl:ObjectProperty ;\n";
	    header +="             rdfs:subPropertyOf kgc:TargetObjProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#hasPredicate\n";
	    header +="kgc:hasPredicate rdf:type owl:ObjectProperty ;\n";
	    header +="                  rdfs:subPropertyOf kgc:TargetObjProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#hasProperty\n";
	    header +="kgc:hasProperty rdf:type owl:ObjectProperty ;\n";
	    header +="                 rdfs:subPropertyOf kgc:TargetObjProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#how\n";
	    header +="kgc:how rdf:type owl:ObjectProperty ;\n";
	    header +="         rdfs:subPropertyOf kgc:SceneObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#if\n";
	    header +="kgc:if rdf:type owl:ObjectProperty ;\n";
	    header +="        rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#infoReceiver\n";
	    header +="kgc:infoReceiver rdf:type owl:ObjectProperty ;\n";
	    header +="                  rdfs:subPropertyOf kgc:SceneProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#infoSource\n";
	    header +="kgc:infoSource rdf:type owl:ObjectProperty ;\n";
	    header +="                rdfs:subPropertyOf kgc:SceneProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#left\n";
	    header +="kgc:left rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#middle\n";
	    header +="kgc:middle rdf:type owl:ObjectProperty ;\n";
	    header +="            rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#near\n";
	    header +="kgc:near rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#next_to\n";
	    header +="kgc:next_to rdf:type owl:ObjectProperty ;\n";
	    header +="             rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#ofPart\n";
	    header +="kgc:ofPart rdf:type owl:ObjectProperty ;\n";
	    header +="            rdfs:subPropertyOf kgc:TargetObjProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#ofWhole\n";
	    header +="kgc:ofWhole rdf:type owl:ObjectProperty ;\n";
	    header +="             rdfs:subPropertyOf kgc:TargetObjProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#on\n";
	    header +="kgc:on rdf:type owl:ObjectProperty ;\n";
	    header +="        rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#opposite\n";
	    header +="kgc:opposite rdf:type owl:ObjectProperty ;\n";
	    header +="              rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#orTarget\n";
	    header +="kgc:orTarget rdf:type owl:ObjectProperty ;\n";
	    header +="              rdfs:subPropertyOf kgc:TargetObjProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#otherwise\n";
	    header +="kgc:otherwise rdf:type owl:ObjectProperty ;\n";
	    header +="               rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#right\n";
	    header +="kgc:right rdf:type owl:ObjectProperty ;\n";
	    header +="           rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#subject\n";
	    header +="kgc:subject rdf:type owl:ObjectProperty ;\n";
	    header +="             rdfs:subPropertyOf kgc:SceneProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#then\n";
	    header +="kgc:then rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#therefore\n";
	    header +="kgc:therefore rdf:type owl:ObjectProperty ;\n";
	    header +="               rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#to\n";
	    header +="kgc:to rdf:type owl:ObjectProperty ;\n";
	    header +="        rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#what\n";
	    header +="kgc:what rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:SceneObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#when\n";
	    header +="kgc:when rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:RelationBetweenScene ,\n";
	    header +="                             kgc:SceneObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#when_during\n";
	    header +="kgc:when_during rdf:type owl:ObjectProperty ;\n";
	    header +="                 rdfs:subPropertyOf kgc:RelationBetweenScene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#where\n";
	    header +="kgc:where rdf:type owl:ObjectProperty ;\n";
	    header +="           rdfs:subPropertyOf kgc:LocationProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#whom\n";
	    header +="kgc:whom rdf:type owl:ObjectProperty ;\n";
	    header +="          rdfs:subPropertyOf kgc:SceneObjectProperty .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#why\n";
	    header +="kgc:why rdf:type owl:ObjectProperty ;\n";
	    header +="         rdfs:subPropertyOf kgc:SceneObjectProperty .\n";

	    header +="###   http://kgc.knowledge-graph.jp/ontology/kgcc.owl#LocationProperty\n";
	    header +="kgc:LocationProperty rdf:type owl:ObjectProperty ;\n";
	    header +="                      rdfs:subPropertyOf kgc:SceneObjectProperty .\n";

	    header +="#################################################################\n";
	    header +="#    Data properties\n";
	    header +="#################################################################\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#time\n";
	    header +="kgc:time rdf:type owl:DatatypeProperty ;\n";
	    header +="          rdfs:range xsd:dateTime .\n";

	    header +="#################################################################\n";
	    header +="#    Classes\n";
	    header +="#################################################################\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#AbstractTime\n";
	    header +="kgc:AbstractTime rdf:type owl:Class .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Action\n";
	    header +="kgc:Action rdf:type owl:Class .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Animal\n";
	    header +="kgc:Animal rdf:type owl:Class ;\n";
	    header +="            rdfs:subClassOf kgc:Object .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#CanAction\n";
	    header +="kgc:CanAction rdf:type owl:Class ;\n";
	    header +="               rdfs:subClassOf kgc:Action .\n";


	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#CanNotAction\n";
	    header +="kgc:CanNotAction rdf:type owl:Class ;\n";
	    header +="                  rdfs:subClassOf kgc:CanAction ;\n";
	    header +="                  rdfs:subClassOf kgc:NotAction .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#NotAction\n";
	    header +="kgc:NotAction rdf:type owl:Class ;\n";
	    header +="               rdfs:subClassOf kgc:Action .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#OFobj\n";
	    header +="kgc:OFobj rdf:type owl:Class ;\n";
	    header +="           rdfs:subClassOf kgc:Object .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#ORobj\n";
	    header +="kgc:ORobj rdf:type owl:Class ;\n";
	    header +="           rdfs:subClassOf kgc:Object .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Object\n";
	    header +="kgc:Object rdf:type owl:Class .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Person\n";
	    header +="kgc:Person rdf:type owl:Class ;\n";
	    header +="            rdfs:subClassOf kgc:Animal .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Place\n";
	    header +="kgc:Place rdf:type owl:Class ;\n";
	    header +="           rdfs:subClassOf kgc:Object .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Property\n";
	    header +="kgc:Property rdf:type owl:Class .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Scene\n";
	    header +="kgc:Scene rdf:type owl:Class .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Situation\n";
	    header +="kgc:Situation rdf:type owl:Class ;\n";
	    header +="               rdfs:subClassOf kgc:Scene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Statement\n";
	    header +="kgc:Statement rdf:type owl:Class ;\n";
	    header +="               rdfs:subClassOf kgc:Scene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Talk\n";
	    header +="kgc:Talk rdf:type owl:Class ;\n";
	    header +="          rdfs:subClassOf kgc:Scene .\n";

	    header +="###  http://kgc.knowledge-graph.jp/ontology/kgcc.owl#Thought\n";
	    header +="kgc:Thought rdf:type owl:Class ;\n";
	    header +="             rdfs:subClassOf kgc:Scene .\n";


		return header;
	}


	 static String getDate(String pubdate) {
	        try {
	            //String s = "Thu, 06 Aug 2009 08:21:24 +0900";
	            DateFormat input = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	            Date d = input.parse(pubdate);

	            //DateFormat output = new SimpleDateFormat("yyyy/MM/dd (E) HH:mm:ss z");
	            DateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            //System.out.println(output.format(d).replaceAll(" ", "T"));
	            return output.format(d).replaceAll(" ", "T")+"Z";
	        } catch (ParseException e) {
	            e.printStackTrace();
	        }
			return "";
	    }


	 /*
	  * Turtleを別のフォーマットに変換する用メソッド（調整中）
	  * 
	 void changeRdfFormat(){
		 Model model = ModelFactory.createDefaultModel() ;

			File dir = new File("data/KGtest");
			File[] files =  dir.listFiles();
			for(int i=0;i<files.length;i++) {
				if(files[i].getName().endsWith(".ttl")) {
					model.read(files[i].getAbsolutePath(), "TURTLE") ;
					try {
					FileOutputStream out;
					out = new FileOutputStream("data/KGtest/output/"+files[i].getName());

					model.write(out,  "TURTLE");

					out = new FileOutputStream("data/KGtest/output/"+files[i].getName().replaceAll(".ttl", ".rdf"));
					model.write(out,  "RDF/XML");

					out = new FileOutputStream("data/KGtest/output/"+files[i].getName().replaceAll(".ttl", ".nt"));
					model.write(out,  "N-TRIPLE");
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			//参照解決公開用分割処理

			try {
//				File saveFolder = new File("data/KGtest/lod");
//				saveFolder.mkdir();
				String savepath = "data/KGtest/lod/";//"C:/work/lod/";
				String savepath2 = "data/KGtest/ForFig/";

				//Property prop_type = model.getProperty("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");

				//System.out.println("TYPE="+prop_type.toString());

				ResIterator	it = model.listSubjects();
				while(it.hasNext()){
					Resource res = it.next();
					String data = res.getURI();
					System.out.println(data);

					StmtIterator sit = res.listProperties();

					Model model_tmp = ModelFactory.createDefaultModel() ;
					model_tmp.setNsPrefixes(model.getNsPrefixMap());

					model_tmp.add(sit);

					ArrayList<Statement> addSt = new ArrayList<Statement>();

					StmtIterator sit2 = model_tmp.listStatements();
					boolean addCheck = true;

					while(sit2.hasNext()){
						Statement st = sit2.nextStatement();
						//model_tmp.add(st);
						if(st.getObject().isResource()) {
							Resource res2 = model.getResource(st.getResource().toString());
							StmtIterator sit3 = res2.listProperties();

							ArrayList<Statement> addStTemp = new ArrayList<Statement>();

							while(sit3.hasNext()){
								Statement st_add = sit3.nextStatement();
								if(st_add.getObject().toString().contains("http://www.w3.org/2002/07/owl")) {

								}else if(st_add.getPredicate().toString().contains("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {


							}else if(st_add.getObject().toString().contains("Situation")) {
								addCheck = false;
							}
								else {
									addStTemp.add(st_add);

								}
							}

							if(addCheck) {
								addSt.addAll(addStTemp);
							}
						}
					}


					String sub = data.substring(data.lastIndexOf("/")+1);
					//System.out.println("===>>>"+sub);

					if(!"".equals(sub) && !sub.endsWith(".ttl")){
						File outf = new File(savepath+sub+".ttl");
						FileOutputStream out_tmp = new FileOutputStream(outf);
						OutputStreamWriter ow = new OutputStreamWriter(out_tmp, "UTF-8");
						BufferedWriter bw = new BufferedWriter(ow);
						model_tmp.write(bw,"TURTLE");
						bw.close();

						model_tmp.add(addSt);
						File outf2 = new File(savepath2+sub+".ttl");
						FileOutputStream out_tmp2 = new FileOutputStream(outf2);
						OutputStreamWriter ow2 = new OutputStreamWriter(out_tmp2, "UTF-8");
						BufferedWriter bw2 = new BufferedWriter(ow2);
						model_tmp.write(bw2,"TURTLE");
						bw2.close();
					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	 }
*/

}



/*　1文をトリプルに変換するためのデータ構造
 *  →　uri，ｔｙｐｅ，その他のトリプル　の3種類で管理
 *
 */

class StatData{
	String uri;
	String type;
	ArrayList<String> triple;

	StatData(){
		this.uri = "";
		this.type = "";
		this.triple = new ArrayList<String>();
	}

	void setUri(String u) {
		this.uri = u;
	}

	void setType(String t) {
		this.type = t;
	}

	// "prop    obj" の形式でP-Oを追加
	void addTriple(String tri) {
		this.triple.add(tri);
	}

//	void addTripleIRI(String prop, String obj) {
//		String tri = "kgc:"+prop+"   <"+this.checkIRI(obj)+ ">";
//		this.triple.add(tri);
//	}

//	void addTripleLiteral(String tri) {
//		String tri = "kgc:source  \""+eng_lbl.replaceAll("\"", "'")+"\"@en"
//		this.triple.add(tri);
//	}


	String getTTL() {
		if(this.uri.equals("")||triple.size()==0) {
			return "";
		}

		String ttl_data ="";

		ttl_data +="<"+this.checkIRI(uri)+">\n";
		if(!type.equals("")) {
			ttl_data +="    rdf:type    kgc:"+ this.type +" ;\n" ;
		}

		boolean other = false;

		Iterator<String> it =this.triple.iterator();
		while(it.hasNext()) {
			String p_o = it.next();
			if(other) {//他のラベルがあるときは改行処理
				ttl_data +=" ;\n";
			}
			ttl_data +="    " + p_o;
			other = true;
		}
		ttl_data +=" .\n";//終了処理

		//ttl_data +="###---\n";//終了処理

		return ttl_data;
	}

	//IRIの禁則文字を処理
	String checkIRI(String iri) {
		String iri_checked = iri.replaceAll(" ", "_")
				.replaceAll(";", "_")
				.replaceAll(":", "_")
				.replaceAll("!", "_")
				.replaceAll("\"", "_")
				.replaceAll("'", "_")
				.replaceAll("http_", "http:")
				.replaceAll("https_", "https:");
		return iri_checked;
	}

}

/*Objectとして参照するリソースの管理用データ構造
 *  Class と　Ｐｅｏｐｅｒｔｙ　共用でとりあえず作る？
 *
 *  URIとラベルの組を管理する
 */
class ObjData {
	HashMap<String, ArrayList<String>> map;
	ArrayList<String[]> ofList;

	ObjData(){
		map = new HashMap<String, ArrayList<String>>();
		ofList = new ArrayList<String[]>();
	}

	//URI,プロパティ，ラベルを指定してObjのデータを解析・追加する
	void addObjData(String baseOrg, String obj_source, String prop, String objOrg, String obj_lbl, String eng_lbl, String type) {
		String base = baseOrg;
		String obj = objOrg;
		//目的語のObjectが大文字で始まるとき，小説をまたいで統一IRIにする処理
		if(!baseOrg.endsWith("data")&&!baseOrg.endsWith("property/")) {
			if( Character.isUpperCase( eng_lbl.charAt( 0 ) ) ) {
				//System.out.println("baseOrg::"+baseOrg);
				int index = baseOrg.lastIndexOf("/",baseOrg.length()-2)+1;
				String filename = baseOrg.substring(index);
				//System.out.println("baseOrg::"+baseOrg+ " index="+index +" filename="+filename);
				base = baseOrg.substring(0,index);
				obj = obj.replace(filename, "");
				//System.out.println("===>base::"+base);
			}
		}
		if(prop.equals("hasPredicate")) {
			addObj(obj, "(type)Action");
			if(eng_lbl.startsWith("cannot")) {
				addObj(obj, "(type)CanNotAction");
				addObj(obj, "(cannot)"+base+eng_lbl.substring(6).toLowerCase().trim());
//				addObj(base+eng_lbl.substring(6).trim(), eng_lbl.substring(6).trim());
//				//addObj(obj, "(cannot)"+base+obj_lbl.substring(6));
//				addObj(base+eng_lbl.substring(6).trim(), obj_lbl/*.substring(6)*/);
				//接頭語なしのObjを定義
				String org_eng_lbl = eng_lbl.substring(6).trim();
				addObj(base+org_eng_lbl, "\""+org_eng_lbl+"\"@en");
				if(obj_lbl.startsWith("cannot")) {//日本語では「not]がついていないことがある
					addObj(base+org_eng_lbl, "\""+obj_lbl.substring(6)+"\"@ja");
				}
			}
			else if(eng_lbl.startsWith("can")) {
				addObj(obj, "(type)CanAction");
				addObj(obj, "(can)"+base+eng_lbl.substring(3).toLowerCase().trim());
				//接頭語なしのObjを定義
				String org_eng_lbl = eng_lbl.substring(3).trim();
				addObj(base+org_eng_lbl, "\""+org_eng_lbl+"\"@en");
				if(obj_lbl.startsWith("can")) {//日本語では「not]がついていないことがある
					addObj(base+org_eng_lbl, "\""+obj_lbl.substring(3)+"\"@ja");
				}
			}
			else if(eng_lbl.startsWith("not")) {
				addObj(obj, "(type)NotAction");
				addObj(obj, "(not)"+base+eng_lbl.substring(3).toLowerCase().trim());
				//接頭語なしのObjを定義
				String org_eng_lbl = eng_lbl.substring(3).trim();
				addObj(base+org_eng_lbl, "\""+org_eng_lbl+"\"@en");
				if(obj_lbl.startsWith("not")) {//日本語では「not]がついていないことがある
					addObj(base+org_eng_lbl, "\""+obj_lbl.substring(3)+"\"@ja");
				}
			}
		}
		else if(prop.equals("hasProperty")) {
			addObj(obj, "(type)Property");
		}
		else if(!type.equals("")) {
			addObj(obj, "(type)"+type);
		}
//		else {
//			addObj(obj, "(type)Object");//もっと細分化できるが保留　例）Who→Person
//		}

		//String id = obj.replace("http://kgc.knowledge-graph.jp/data/dataset1/", "");
		addObj(obj, "\""+obj_lbl+"\"@ja");
		addObj(obj, "\""+eng_lbl+"\"@en");


		//if(!obj_lbl.equals(obj_source)) {//原文がlabelと一致する際にもｓｏｕｒｃｅに出力
		//	addObj(obj, "(source)"+obj_source);
		//}

		//ORの処理　
		//　Objは「ORでつないだリテラル」をURIとし定義
		//　　→その定義のトリプルでORの対象となっているObjの組を示す
		if(eng_lbl.indexOf(" OR ")>0) {
			addObj(obj, "(type)ORobj");
			if(!type.trim().equals("")) {
				addObj(obj, "(type)"+type);
			}

			String[] orObj = eng_lbl.split(" OR ");
			for(int i=0;i<orObj.length;i++) {
				String orLbl = orObj[i].trim();
				String orUri = base+orLbl;
				addObj(obj, "(or)"+orUri);
				addObj(orUri, "\""+orLbl+"\"@en");
			}
			return;
		}

		//OFの処理
		int ofIndef = eng_lbl.indexOf(" of ");
		if(ofIndef > 0) {
			addObj(obj, "(type)OFobj");
			if(!type.trim().equals("")) {
				addObj(obj, "(type)"+type);
			}
			String[] ofData = { base,  obj_source,  prop,  obj,  obj_lbl,  eng_lbl,  type};
			this.ofList.add(ofData);
		}
	}

	void checkOfData() {
		ArrayList<String[]> ofListTemp = (ArrayList<String[]>) ofList.clone();
		Iterator<String[]> it = ofListTemp.iterator();
		while(it.hasNext()) {
			String[] ofData = it.next(); //{ base,  obj_source,  prop,  obj,  obj_lbl,  eng_lbl,  type};
			String eng_lbl = ofData[5];
			String base = ofData[0];
			String obj_source = ofData[1];
			String obj = ofData[3];
			int ofIndef = eng_lbl.indexOf(" of ");

			String[] ofObj = eng_lbl.split(" of ");
//			for(int i=0;i<ofObj.length;i++) {
//				String ofLbl = ofObj[i].trim();
//				String ofUri = base+ofLbl;
//				if(i==0) {
//					addObj(obj, "(part)"+ofUri);
//				}
//				else {
//					addObj(obj, "(whole)"+ofUri);
//				}
//				addObj(ofUri, "\""+ofLbl+"\"@en");
//			}
			if(ofObj.length>2) {
				String subOfLbl = eng_lbl.substring(ofIndef+" of ".length()).trim();
				String subOfUri = base+subOfLbl;
				addObj(obj, "(whole)"+subOfUri);
				this.addObjData(base, obj_source, "ofTargetSub", subOfUri, "", subOfLbl, "");
				System.out.println("[subOf]"+eng_lbl+"---"+subOfLbl);
			}
		}

		Iterator<String[]> it2 = ofList.iterator();
		while(it2.hasNext()) {
			String[] ofData = it2.next(); //{ base,  obj_source,  prop,  obj,  obj_lbl,  eng_lbl,  type};
			String eng_lbl = ofData[5];
			String base = ofData[0];
			String obj_source = ofData[1];
			String obj = ofData[3];
			int ofIndef = eng_lbl.indexOf(" of ");

			String[] ofObj = eng_lbl.split(" of ");
			for(int i=0;i<ofObj.length;i++) {
				String ofLbl = ofObj[i].trim();
				String ofUri = base+ofLbl;
				if(i==0) {
					addObj(obj, "(part)"+ofUri);
				}
				else {
					addObj(obj, "(whole)"+ofUri);
				}
				addObj(ofUri, "\""+ofLbl+"\"@en");
			}
			if(ofObj.length>2) {
				String subOfLbl = eng_lbl.substring(ofIndef+" of ".length());
				String subOfUri = base+subOfLbl;
				addObj(obj, "(whole)"+subOfUri);
//				this.addObjData(base, obj_source, "ofTargetSub", subOfUri, "", subOfLbl, "");
//				System.out.println("[subOf]"+eng_lbl+"---"+subOfLbl);
			}
		}

	}

	//ＵＲＩとラベルを指定し，ラベルの重複をチェックして追加
	void addObj(String uri, String lbl) {
		//「空」のラベルを追加しない
		if(lbl.startsWith("\"\"")) {
			return;
		}
		else if(lbl.endsWith("@ja")) {//日本語ラベルを追加しない
			return;
		}

		ArrayList<String> lbls = map.get(uri);
		if(lbls==null) {
			lbls = new ArrayList<String>();
			map.put(uri, lbls);
		}

		if(!lbls.contains(lbl)) {
			lbls.add(lbl);
		}
	}

	String getTTL() {
		 checkOfData();//ofObjの分解処理

		String ttl_data ="#Objectとして参照されているリソースの定義\n";
		Iterator<String> it = map.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			ttl_data +="<"+key.replaceAll(" ", "_")+">\n";//URIの禁則処理は，もう少し真面目に入れる必要ありそう
			boolean other = false;

			ArrayList<String> lbls = map.get(key);
			Iterator<String> it_lbl =lbls.iterator();
			boolean typeInfo = false;
			while(it_lbl.hasNext()) {
				String lbl = it_lbl.next();
				if(other) {//他のラベルがあるときは改行処理
					ttl_data +=";\n";
				}
				if(lbl.startsWith("(type)")){
					ttl_data +="    rdf:type    kgc:"+lbl.substring(6);
					typeInfo = true;
				}
//				else if(lbl.startsWith("(source)")){
//					ttl_data +="    kgc:source    \""+lbl.substring(8)+"\"@ja";
//				}
				else if(lbl.startsWith("(or)")){
					ttl_data +="    kgc:orTarget    <"+lbl.substring(4).replaceAll(" ", "_")+">";
				}
				else if(lbl.startsWith("(whole)")){
					ttl_data +="    kgc:ofWhole    <"+lbl.substring("(whole)".length()).replaceAll(" ", "_")+">";
				}
				else if(lbl.startsWith("(part)")){
					ttl_data +="    kgc:ofPart    <"+lbl.substring("(part)".length()).replaceAll(" ", "_")+">";
				}
				else if(lbl.startsWith("(cannot)")){
					ttl_data +="    kgc:canNot    <"+lbl.substring(8).replaceAll(" ", "_")+">";
				}
				else if(lbl.startsWith("(can)")){
					ttl_data +="    kgc:can    <"+lbl.substring(5).replaceAll(" ", "_")+">";
				}
				else if(lbl.startsWith("(not)")){
					ttl_data +="    kgc:Not    <"+lbl.substring(5).replaceAll(" ", "_")+">";
				}
				else {
				    ttl_data +="    rdfs:label     "+lbl;
				}
				other = true;
			}
			if(!typeInfo) {
				ttl_data +=";\n    rdf:type    kgc:Object";
			}
			ttl_data +=".\n";//修了処理
		}


		return ttl_data;
	}

}



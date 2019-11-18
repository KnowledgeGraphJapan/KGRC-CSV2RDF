package jp.kgrc.challenge2019;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

public class PreProcTSVforKG {

    HashMap<String, String[]> predList = new HashMap<String, String[]>();



	public static void main(String[] args) {
		new PreProcTSVforKG();

	}

	PreProcTSVforKG(){
		//述語統一用のリスト読み込み
		File predListFile = new File("data/KGRC2019/setting/PredList.tsv");
		loadPredList(predListFile);
		PreProcTsv();
	}


	void PreProcTsv() {
		File dir = new File("data/KGRC2019/org");


		//小説ごとにKG作成
		File[] files =  dir.listFiles();
		for(int i=0;i<files.length;i++) {
			if(files[i].getName().endsWith(".tsv")) {
				loadPreProcTsv(files[i]);
			}
		}

	}

	//
	void loadPreProcTsv(File f) {
		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(f),"UTF-8"));

			String filename = f.getName();


//			File saveFile = new File("data/KGRC2019/"+filename.replace(".tsv", "_new.tsv"));
			File saveFile = new File("data/KGRC2019/"+filename);

			FileOutputStream out;
			out = new FileOutputStream(saveFile);
			OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
			BufferedWriter bw = new BufferedWriter(ow);

			String line="";
			String[] data;


			//ヘッダ行の処理
			bw.write(br.readLine()+"\n");

			while(br.ready()) {
				line = br.readLine();
				data = line.split("\t");
				String org_data ="";

				System.out.println(data[0]);

				//述語統一処理
				if(data.length>=3) {
					String key = filename+"_"+data[0];
					String prop = data[1];
					if(prop.equals("hasPredicate") || prop.equals("hasProperty")) {
						String[] pred = predList.get(key);
						if(pred!=null) {
							if(!data[1].equals(pred[0]) || !data[3].equals(pred[1])) {
								org_data="#ORG:"+data[1]+":"+data[3];
								System.out.println(filename+"\t"+data[0]+"\t"+data[1]+"\t"+data[2]+" "+key+"==>"+pred[0]+":"+pred[1]);
							}
							data[1] = pred[0];//Property
							data[3] = pred[1];//述語
						}
					}
				}

				for(int i=0;i<data.length;i++) {
					bw.write(data[i]+"\t");
				}
				bw.write("\t"+org_data+"\n");

			}

			br.close();
			bw.close();

		}
		catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}

	//述語の統一用リストの読み込み
	void loadPredList(File f) {
		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(f),"UTF-8"));

			String line="";
			String[] data;

			//ヘッダ行は無視
			br.readLine();

			while(br.ready()) {
				line = br.readLine();
				data = line.split("\t");
				if(data.length>5) {
					String key = data[0]+"_"+data[1];
					String[] predData = new String[2];
					predData[0] = data[2];//Propery
					predData[1] = data[5];//述語
					if(!predData[1].trim().equals("")) {
						predList.put(key, predData);
						System.out.println(key+"="+predData[0]+":"+predData[1]);
					}
				}
			}

		}
		catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}


}

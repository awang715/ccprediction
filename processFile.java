	import java.io.BufferedReader;
	import java.io.BufferedWriter;
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import java.io.IOException;
	import java.io.InputStreamReader;
	import java.io.OutputStreamWriter;
	import java.nio.file.Files;
	import java.nio.file.Paths;
	import java.util.HashMap;
	import java.util.HashSet;

	public class processFile {
		static int count_file = 0;// number of total files under this directory
		static HashSet file_paths = new HashSet();// store file paths to be read
		static HashSet<String> address_book = new HashSet<String>();// haven't dealt
																	// with
																	// self-addressed
																	// emails
		// the list of all recipients sent by this user, right now it contains some
		// multiple email addresses by a single user
		static HashSet<String> sent_train = new HashSet<String>();// train set
		static HashSet<String> sent_test = new HashSet<String>();// the last two
																	// months emails
																	// to form the
																	// test set
	    static HashMap<String,String> file_to = new HashMap<String,String>();
	    static HashMap<String,String> file_cc = new HashMap<String,String>();
	    static HashMap<String,String> file_bcc = new HashMap<String,String>();
	    static BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
	    static HashMap<String,String> file_date = new HashMap<String,String>();
		public static void main(String[] args) throws IOException {

			Files.walk(Paths.get("jones-t")).forEach(
					filePath -> {
						count_file++;
						String valid_path = filePath.toString();
						if (!valid_path.equals("jones-t")
								&& !valid_path.equals("jones-t/sent")
								&& !valid_path.equals("jones-t/sent_items"))
							file_paths.add(valid_path);

					});
			//System.out.println(count_file);

			for (Object f : file_paths) {
				readFile((String) f);
			}
			//System.out.println(address_book.size());
			for (String file: sent_train){
			        extractBodyMessage(file);
			}
			log.flush();
			log.close();
		}

		private static void readFile(String f) throws IOException {
			FileInputStream fstream = new FileInputStream((String) f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String line = null;
			String to_lines = "";
			String cc_lines = "";
			String bcc_lines = "";
			boolean to_flag = true;
			boolean cc_flag = true;
			boolean bcc_flag = true;
			boolean date_flag = true;
			while ((line = br.readLine()) != null) {
				if (line.indexOf("Date:") >= 0 && date_flag) {
					String date = line.substring(line.indexOf(":") + 1);
					date = date.split(",")[1];
					String[] elts = date.split(" ");
					String month = elts[2];
					if (month.equals("Jan"))
						month = "01";
					if (month.equals("Feb"))
						month = "02";
					if (month.equals("Mar"))
						month = "03";
					if (month.equals("Apr"))
						month = "04";
					if (month.equals("May"))
						month = "05";
					if (month.equals("Jun"))
						month = "06";
					if (month.equals("Jul"))
						month = "07";
					if (month.equals("Aug"))
						month = "08";
					if (month.equals("Sep"))
						month = "09";
					if (month.equals("Oct"))
						month = "10";
					if (month.equals("Nov"))
						month = "11";
					if (month.equals("Dec"))
						month = "12";
					String zero = "";
					if (elts[1].length() == 1)
						zero = "0" + elts[1];
					else
						zero = elts[1];
					String format_date = elts[3] + month + zero;
					if (Integer.parseInt(format_date) >= 20011101){
						sent_test.add(f);
						file_date.put(f,format_date);
					}
					else{
						sent_train.add(f);
						file_date.put(f,format_date);
					}
					date_flag = false;
				}
				if (line.indexOf("From:") >= 0) {
					if (line.indexOf("tana.jones@enron.com") < 0)
						return;
				}
				if (line.indexOf("To:") >= 0 && to_flag) {

					to_lines += line;

					line = br.readLine();

					while (line.indexOf(":") < 0) {

						to_lines += line;
						line = br.readLine();
					}

					int index = to_lines.indexOf(":");
					to_lines = to_lines.substring(index + 1);
					String[] addr = to_lines.split(",");
					
					for (int i = 0; i < addr.length; i++){
						String address = addr[i].replaceAll(" ", "").replaceAll(
								"\t", "");
						address_book.add(address);
						if (!file_to.containsKey(f)) file_to.put(f,address);
						else file_to.put(f,file_to.get(f)+","+address);
						}
					to_flag = false;

				}
				if (line.indexOf("Cc:") >= 0 && cc_flag) {
					cc_lines += line;

					line = br.readLine();

					while (line.indexOf(":") < 0) {

						cc_lines += line;
						line = br.readLine();
					}
					int index = cc_lines.indexOf(":");
					cc_lines = cc_lines.substring(index + 1);
					String[] addr = cc_lines.split(",");
					for (int i = 0; i < addr.length; i++){
						String address = addr[i].replaceAll(" ", "").replaceAll(
								"\t", "");
						address_book.add(address);
						if (!file_cc.containsKey(f)) file_cc.put(f,address);
						else file_cc.put(f,file_cc.get(f)+","+address);
						
					}
					cc_flag = false;
				}
				if (line.indexOf("Bcc:") >= 0 && bcc_flag) {
					bcc_lines += line;

					line = br.readLine();

					while (line.indexOf(":") < 0) {

						bcc_lines += line;
						line = br.readLine();
					}
					int index = bcc_lines.indexOf(":");
					bcc_lines = bcc_lines.substring(index + 1);
					String[] addr = bcc_lines.split(",");
					for (int i = 0; i < addr.length; i++){
						String address = addr[i].replaceAll(" ", "").replaceAll(
								"\t", "");
						address_book.add(address);
						if (!file_bcc.containsKey(f)) file_bcc.put(f,address);
						else file_bcc.put(f,file_bcc.get(f)+","+address);
					}
					bcc_flag = false;
				}
			}
			br.close();
			fstream.close();
		}
		
		private static void extractBodyMessage(String file) throws IOException{
			FileInputStream fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			
			String line = null;
			boolean flag = false;
			String bodyMSG = "";
			while ((line = br.readLine()) != null) {
				if (line.length()==0) flag = true;
				if (flag){
					bodyMSG+=line;
				}
				
				
			}
		    //bodyMSG = bodyMSG.replaceAll("\\W"," ");
		    bodyMSG = bodyMSG.replaceAll("\t", "");
		    String cc= file_cc.get(file);
		    String bcc = file_bcc.get(file);
		    if (cc==null&&bcc==null)
		    log.write(file_date.get(file)+"\t"+file_to.get(file)+"\t"+bodyMSG+"\n");
		    else if (cc==null&&bcc!=null){
		    	log.write(file_date.get(file)+"\t"+file_to.get(file)+"!"+bcc+"\t"+bodyMSG+"\n");
		    }
		    else if (cc!=null&&bcc==null){
		    	log.write(file_date.get(file)+"\t"+file_to.get(file)+"!"+cc+"\t"+bodyMSG+"\n");
		    }
		    else log.write(file_date.get(file)+"\t"+file_to.get(file)+"!"+cc+","+bcc+"\t"+bodyMSG+"\n");
		}
	}

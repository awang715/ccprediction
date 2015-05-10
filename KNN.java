    import java.io.BufferedReader;
    import java.io.BufferedWriter;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.io.OutputStreamWriter;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.Iterator;
    import java.util.Vector;
/**
 * @Author Anqi Wang
 * Streaming Class for KNN/TFIDF
 *
 **/



public class KNN{
	
           static int num_of_docs = 0;
           static HashSet<String> stopwords_set = new HashSet<String>();
           static HashMap<String,Integer> words_doc = new HashMap<String,Integer>();
           static BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
           static void addStopwords() {
        	   String stopwords = "about, all, am, an, and, are, as, at, be, been, but, by, can, cannot, did,";
        	   stopwords+="do, does, doing, done, for, from, had, has, have,";
               stopwords+="having, if, in, is, it, its, of, on, that, the, they, these, this,";
               stopwords+="those, to, too, want, wants, was, what, which, will, with,would";
                     for (String word: stopwords.split(",")){
                              stopwords_set.add(word);
                     }
           }
           public static void main(String[] args) throws IOException{

           	     BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                // BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
                 String input;

                 while ((input = reader.readLine())!=null){
                        process(input);   
                 }
                 log.write("*"+"\t"+num_of_docs+"\n");
                 Iterator itr = words_doc.keySet().iterator();
                 while (itr.hasNext()){
                 	String word = (String) itr.next();
                 	log.write(word+"\t"+words_doc.get(word)+"\n");
                 }
                 log.flush();

           }
           
           static void process(String input) throws IOException{
                      num_of_docs++;
                      String[] elements = input.split("\t");
                     // String[] addrs = elements[1].split(",");
                      Vector<String> toks = tokenizeDoc(elements[2]);
                      HashMap<String,Integer> doc_word = new HashMap<String,Integer>();
                      String labels = "*,"+num_of_docs;
                     // System.out.println(elements[1]);
                      log.write(labels+"\t"+elements[1]+"\n");
                      Iterator itr = toks.iterator();
                      while (itr.hasNext()){
                      	      String word = (String)itr.next();
                      	      String word_doc = num_of_docs+ "," + word;
                      	      //#docs w occurs
                      	      if (!doc_word.containsKey(word_doc)){
                                    
                      	            if (!words_doc.containsKey(word))
                      	      	           words_doc.put(word,1);
                      	            else words_doc.put(word,words_doc.get(word)+1);
                      	       }
                              //update #times w occur in doc d
                      	      if (!doc_word.containsKey(word_doc))
                      	      	     doc_word.put(word_doc,1);
                      	      else doc_word.put(word_doc,doc_word.get(word_doc)+1);
                      }

                      Iterator itr2 = doc_word.keySet().iterator();
                      while (itr2.hasNext()){
                      	      String output = (String)itr2.next();
                             log.write(output+"\t"+doc_word.get(output)+"\n");
                      }
                      log.flush();
                      doc_word.clear();

                      

           }




		static Vector<String> tokenizeDoc(String cur_doc){
          cur_doc = cur_doc.replaceAll("(?m)^[ \t]*\r?\n", "");
          cur_doc = cur_doc.replaceAll("\\W"," ").toLowerCase();
          String[] words = cur_doc.split("\\s+");
          Vector<String> tokens = new Vector<String>();
          for (int i=0; i<words.length;i++){
            words[i] = words[i].replaceAll("\\W","");
               if (words[i].length()>0){
               	    if (!stopwords_set.contains(words[i])){
                                 tokens.add(words[i]);
                    
                             }
               }
          }
          return tokens;
         }


}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
/**
 * @Author Anqi Wang
 * TFIDF - the Centroid Method
 */
public class TFIDF{
  static HashSet<String> stopwords_set = new HashSet<String>();
  static int total_docs;
  static int num_of_docs;
  static int total_email;
  static int correct;
  static HashMap<String,Integer> df = new HashMap<String,Integer>();//#docs w occurs
  static HashMap<String,Integer> tf = new HashMap<String,Integer>();//diff times w occurs in doc d (d,w),#
  static HashMap<Integer,ArrayList<String>> doc_dict = new HashMap<Integer,ArrayList<String>>();//doc,list<words in doc>
  static HashMap<Integer,String> doc_labels = new HashMap<Integer,String>();//doc,labels
  static HashMap<String,ArrayList<Integer>> label_docs = new HashMap<String,ArrayList<Integer>>();//label,list<docs that has this label>
  static HashMap<String, Double[]> label_vector = new HashMap<String, Double[]>();
  static BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
  static ArrayList<String> vocabulary = new ArrayList<String>();
  public static void main(String[] args) throws IOException{

    //read from train
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    String input = reader.readLine();
    int prev = 0;

    prev = process(prev,input);

    while ((input = reader.readLine())!=null){
      prev = process(prev,input);  
    }

    centroid();

    FileInputStream fstream = new FileInputStream(args[0]);
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    String line;
    while ((line = br.readLine())!=null){//predict every test document
      predict(line);   
    }
    log.write("Correctness : "+correct/(double)total_email);
    log.flush();

  }

  static void centroid(){
    Iterator itr = doc_dict.keySet().iterator();

    HashMap<Integer,ArrayList<Double>> doc_vector = new HashMap<Integer,ArrayList<Double>>();//map to store doc, centroid_vector
    while (itr.hasNext()){//Iterate through each document
      double length = 0.0;
      HashMap<String,Double> vector_elts = new HashMap<String,Double>();//u(wi,d) each word in this document
      int doc_num = (int)itr.next();
      ArrayList<String> list = doc_dict.get(doc_num);//word list in this doc
      Iterator itr2 = list.iterator();
      while (itr2.hasNext()){
        String w = (String)itr2.next();
        Integer d = df.get(w);        
        int dF_W = d;
        String doc_d = doc_num+","+w;
        int tF_W = tf.get(doc_d);
        double iDF_W = total_docs/(double)dF_W;
        double u_WD = Math.log((double)tF_W+1)*Math.log(iDF_W);
        length+=u_WD*u_WD;
        vector_elts.put(w,u_WD);
      }
      length = Math.sqrt(length);
      ArrayList<Double> vector = new ArrayList<Double>();
      for (int j=0;j<vocabulary.size();j++){//fixed ordered vocabulary list
        String word = vocabulary.get(j);
        if (vector_elts.containsKey(word))
          vector.add(j,vector_elts.get(word)/length);
        else vector.add(j,0.0);

      }
      doc_vector.put(doc_num,vector);
    }
    //calculate centroid'vector for each label  
    Iterator itr3 = label_docs.keySet().iterator();
    while (itr3.hasNext()){
      String label = (String)itr3.next();
      ArrayList<Integer> doc_list = label_docs.get(label);
      Iterator itr4 = doc_list.iterator();
      Double[] cent = new Double[vocabulary.size()];
      while (itr4.hasNext()){
        int docid = (int)itr4.next();
        ArrayList<Double> v = doc_vector.get(docid);
        for (int i=0;i<v.size();i++){
          if (cent[i]==null) cent[i]=new Double(v.get(i));
          else
            cent[i]=cent[i]+v.get(i);
        }
      }
      label_vector.put(label,cent);
    }

  }
  static void predict(String line) throws IOException{
    num_of_docs++;
    String[] elements = line.split("\t");
    String[] addrs = elements[1].split(","); // actual recipients
    HashSet<String> check_list = new HashSet<String>(Arrays.asList(addrs));
    Vector<String> toks = tokenizeDoc(elements[2]);
    HashMap<String,Integer> doc_word = new HashMap<String,Integer>();//cout word freq in this doc
    Iterator itr = toks.iterator();
    while (itr.hasNext()){
      String word = (String)itr.next();
      //update #times w occur in doc d
      if (!doc_word.containsKey(word))
        doc_word.put(word,1);
      else doc_word.put(word,doc_word.get(word)+1);
    }

    TreeMap<String,Double> simi = similarity(doc_word,toks);

    int num_of_can = 0;
    ArrayList<Map.Entry<String, Double>> sortedS = new ArrayList<Entry<String, Double>>(simi.entrySet());
    Collections.sort(sortedS, new Comparator<Map.Entry<String, Double>>() {
      @Override
      public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    for (Map.Entry<String, Double> node : sortedS) {
      if (num_of_can<15){
        String id = node.getKey();
        double sc = node.getValue();
        num_of_can++;
        if (check_list.contains(id)) {
          correct++;
          break;
        }
      }
      else break;
    }
    total_email+=1;

  }

  static HashSet<String> processLabels(HashMap<Integer,Double> map){
    HashSet<String> can = new HashSet<String>();
    Iterator i = map.keySet().iterator();
    while (i.hasNext()){
      int id = (int)i.next();
      String labels = doc_labels.get(id);
      String[] addr = labels.split(",");
      for (int j=0;j<addr.length;j++)
        can.add(addr[j]);
    }
    return can;
  }

  static TreeMap<String,Double> similarity(HashMap<String,Integer> u,Vector<String> toks){
    TreeMap<String,Double> similar = new TreeMap<String,Double>();
    Iterator itr = label_vector.keySet().iterator();
    Iterator itr2 = toks.iterator();
    double testDoc_length=0.0;
    HashMap<String,Double> vector_elts = new HashMap<String,Double>();
    while (itr2.hasNext()){//iterate through test doc's words
      String w = (String)itr2.next();
      Integer d = df.get(w);
      int dF_W = 0;
      if (d!=null) {
        dF_W = d;
        int tF_W = u.get(w);
        double iDF_W = total_docs/(double)dF_W;
        double u_WD = Math.log((double)tF_W+1)*Math.log(iDF_W);
        testDoc_length+=u_WD*u_WD;
        vector_elts.put(w,u_WD); 
      }
    }
    testDoc_length = Math.sqrt(testDoc_length);
    ArrayList<Double> vector = new ArrayList<Double>();
    for (int j=0;j<vocabulary.size();j++){
      String word = vocabulary.get(j);
      if (vector_elts.containsKey(word))
        vector.add(j,vector_elts.get(word)/testDoc_length);
      else vector.add(j,0.0);

    }

    while (itr.hasNext()){//calculate each test message and label vector's similarity
      String recipient = (String)itr.next();
      Double[] cent_v = label_vector.get(recipient);
      double score = 0.0;
      for (int k=0;k<cent_v.length;k++){
        score+=vector.get(k)*cent_v[k];
      }
      similar.put(recipient,score);
    }//END OF ITERATE THROUGH TEST DOC

    return similar;
  }

  static int process(int prev,String input) throws IOException{

    String[] msg = input.split("\t");
    String[] check = msg[0].split(",");

    if (check.length==1&&check[0].equals("*"))
      total_docs = Integer.parseInt(msg[1]);
    else if (check.length==1) {
      df.put(msg[0],Integer.parseInt(msg[1]));
    }
    else if (check[0].equals("*")){
      doc_labels.put(Integer.parseInt(check[1]),msg[1]);
      String[] labels = msg[1].split(",");
      for (int v =0;v<labels.length;v++){
        if (label_docs.get(labels[v])==null){
          ArrayList docs_list = new ArrayList();
          docs_list.add(Integer.parseInt(check[1]));
          label_docs.put(labels[v],docs_list);

        }
        else {
          ArrayList old_list = label_docs.get(labels[v]);
          old_list.add(Integer.parseInt(check[1]));
          label_docs.put(labels[v],old_list);

        }
      }      

    }
    else {

      int doc_id = Integer.parseInt(check[0]);
      if (prev!=Integer.parseInt(check[0])) {
        prev = Integer.parseInt(check[0]);
        ArrayList<String> w = new ArrayList<String>();
        if (!vocabulary.contains(check[1])) vocabulary.add(check[1]);
        w.add(check[1]);
        doc_dict.put(prev,w);
        tf.put(msg[0],Integer.parseInt(msg[1]));//(d,w) #

      }

      else {
        ArrayList<String> w = doc_dict.get(Integer.parseInt(check[0]));
        if (!vocabulary.contains(check[1])) vocabulary.add(check[1]);
        w.add(check[1]);
        doc_dict.put(Integer.parseInt(check[0]),w);
        tf.put(msg[0],Integer.parseInt(msg[1]));
      }

    }
    return prev;



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

  static void addStopwords() {
    String stopwords = "about, all, am, an, and, are, as, at, be, been, but, by, can, cannot, did,";
    stopwords+="do, does, doing, done, for, from, had, has, have,";
    stopwords+="having, if, in, is, it, its, of, on, that, the, they, these, this,";
    stopwords+="those, to, too, want, wants, was, what, which, will, with,would";
    for (String word: stopwords.split(",")){
      stopwords_set.add(word);
    }
  } 
}

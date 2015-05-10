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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
/**
 * @Author Anqi Wang
 * Cal class for KNN
 **/
public class Calculate{
  static HashSet<String> stopwords_set = new HashSet<String>();
  static int total_docs;
  static int num_of_docs;
  static int total_email;
  static int correct;
  static int total_retreived;
  static int total_relevant;
  static int correct_p;
  static HashMap<String,Integer> df = new HashMap<String,Integer>();//#docs w occurs
  static HashMap<String,Integer> tf = new HashMap<String,Integer>();//diff times w occurs in doc d (d,w),#
  static HashMap<Integer,ArrayList<String>> doc_dict = new HashMap<Integer,ArrayList<String>>();//doc,list<words in doc>
  static HashMap<Integer,String> doc_labels = new HashMap<Integer,String>();//doc,labels
  static BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
  static HashMap<String,Integer> sent_freq = new HashMap<String,Integer>();
  static ArrayList<String> random_sent_recipient = new ArrayList<String>();
  static HashMap<String,Integer> cooccur = new HashMap<String,Integer>();
  static HashMap<Integer,String> cross_set = new HashMap<Integer,String>();
  static HashMap<Integer,Integer> recency_set = new HashMap<Integer,Integer>();
  static HashMap<String,Integer> recent_freq = new HashMap<String,Integer>();
  static int total_sent;
  static HashMap<String,Double> knnscore = new HashMap<String,Double>();
  static HashMap<Integer,Double[]> voted_vectors = new HashMap<Integer,Double[]>();
  static HashMap<Integer,Double> weights = new HashMap<Integer,Double>();
  static int total;
  public static void main(String[] args) throws IOException{

    //read from train
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    String input = reader.readLine();
    int prev = 0;
    prev = process(prev,input);
    while ((input = reader.readLine())!=null){
      prev = process(prev,input);  
    }
    
    FileInputStream fstream2 = new FileInputStream(args[0]);//cross validation, read sent_train
    BufferedReader br2 = new BufferedReader(new InputStreamReader(fstream2));
    String line2;
    int cross_num=1;
    while ((line2 = br2.readLine())!=null){
      cross_set.put(cross_num,line2);
      recency_set.put(cross_num,Integer.parseInt(line2.split("\t")[0]));
      cross_num++;
    }
    
    cross_validation();//knnscore feature
    build_recency_feature();//recency feature
    voted_perceptron();
    Iterator itr = weights.keySet().iterator();
    while (itr.hasNext()){
      int k = (int)itr.next();
      weights.put(k,weights.get(k)/(double)total);
    }
    
    
    FileInputStream fstream = new FileInputStream(args[1]);
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    String line;
    while ((line = br.readLine())!=null){//predict every test document
      predict(line);   
    }
    log.write("Correctness : "+correct/(double)total_email);
    log.write("Precision :"+correct_p/(double)total_retreived+"\n");
    log.write("Recall :"+correct_p/(double)total_relevant+"\n");
    log.flush();
    
  }

  //voted perceptron algorithm to rerank candidates
  static void voted_perceptron(){
    
    int k =0;
    
    HashSet<String> filter = new HashSet<String>();
   // Double[] v = new Double[4];
   // v[0]=0.0;v[1]=0.0;v[2]=0.0;v[3]=0.0;
    Double[] v = new Double[3];
    v[0]=0.0;v[1]=0.0;v[2]=0.0;
    double c = 0.0;
    int epoch = 20;//changeable
    total = 0;
    while (epoch>0){
    Iterator itr = cross_set.keySet().iterator();
    while (itr.hasNext()){//CC prediction
      int doc_num = (int)itr.next();
      String file = cross_set.get(doc_num);
      String[] ri = file.split("\t")[1].split("!");
      if (ri.length>1){
            String[] tos = ri[0].split(",");
            String[] ccs = ri[1].split(",");
            //train positive examples
            for (int i=0;i<ccs.length;i++){
              filter.add(ccs[i]);
              total++;
              double knn_score = knnscore.get(ccs[i]);
              int sent_frequency =0;
              if (sent_freq.containsKey(ccs[i]))
                 sent_frequency = sent_freq.get(ccs[i]);
              int recent_frequency = 0;
              if (recent_freq.containsKey(ccs[i])) recent_frequency=recent_freq.get(ccs[i]);
              double cooccurance = 0;
              for (int j=0;j<tos.length;j++){
                filter.add(tos[j]);
                String lookup = tos[j]+","+ccs[i];
                if (cooccur.containsKey(lookup)){
                  cooccurance+=cooccur.get(lookup);
                }
              }
              if (cooccurance!=0) cooccurance=cooccurance/(double)sent_frequency;
              double sum = 0.0;
              //sum=v[0]*knn_score+v[1]*sent_frequency+v[2]*recent_frequency+v[3]*cooccurance;
              sum = v[0]*knn_score+v[1]*sent_frequency+v[2]*recent_frequency;
              if (sum>0) c++;
              else {
               
                Double[] new_vector = Arrays.copyOf(v,v.length);
                voted_vectors.put(k,new_vector);
                weights.put(k,c);
                v[0]+=knn_score;
                v[1]+=sent_frequency;
                v[2]+=recent_frequency;
                //v[3]+=cooccurance;
                //v[1]+=sent_frequency;
                //v[2]+=cooccurance;
                c=1;
                k++;
              }
            }
            //train negative examples
           
            Random r = new Random();
            int count = (int) (random_sent_recipient.size() * 0.1);
            while (count>0){
                  String neg_rec = (String)random_sent_recipient.get(r.nextInt(random_sent_recipient.size()));
                  if (filter.contains(neg_rec)) continue;
                  else {total++;
                        double knn_score = knnscore.get(neg_rec);
                        int sent_frequency =0;
                        if (sent_freq.containsKey(neg_rec))
                                sent_frequency = sent_freq.get(neg_rec);
                        int recent_frequency = 0;
                        if (recent_freq.containsKey(neg_rec)) recent_frequency=recent_freq.get(neg_rec);
                        double cooccurance = 0;
                        for (int j=0;j<tos.length;j++){
                
                        String lookup = tos[j]+","+neg_rec;
                        if (cooccur.containsKey(lookup)){
                               cooccurance+=cooccur.get(lookup);
                            }
                        }
                        if (cooccurance!=0) cooccurance=cooccurance/(double)sent_frequency;
                        double sum = 0.0;
                        //sum=v[0]*knn_score+v[1]*sent_frequency+v[2]*recent_frequency+v[3]*cooccurance;
                        sum=v[0]*knn_score+v[1]*sent_frequency+v[2]*recent_frequency;
                        if (sum<0) c++;
                        else {
                               Double[] new_vector = Arrays.copyOf(v,v.length);
                               voted_vectors.put(k,new_vector);
                               weights.put(k,c);
                               v[0]-=knn_score;
                               v[1]-=sent_frequency;
                               v[2]-=recent_frequency;
                               //v[3]-=cooccurance;
                               //v[1]-=sent_frequency;
                               //v[2]-=cooccurance;
                               c=1;
                               k++;
                        }
                   }
                   count--;
            }
            }
      }
      epoch--;
}

  }

  static void build_recency_feature(){
    int recent_threshold = 100;//changeable
    int k = 1;
     ArrayList<Map.Entry<Integer, Integer>> sortedR = new ArrayList<Entry<Integer, Integer>>(recency_set.entrySet());
    Collections.sort(sortedR, new Comparator<Map.Entry<Integer, Integer>>() {
      @Override
      public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
  
    for (Map.Entry<Integer, Integer> node : sortedR) {
      if (k<recent_threshold){
        int cross_id = node.getKey();
       
        String[] recipients = cross_set.get(cross_id).split("\t")[1].split("!");
        for (int i=0;i<recipients.length;i++){
          String[] r = recipients[i].split(",");
           for (int j=0;j<r.length;j++){
               if (!recent_freq.containsKey(r[j])) recent_freq.put(r[j],1);
               else recent_freq.put(r[j],recent_freq.get(r[j])+1);
           }

        }
        k++;

      }
      else break;
    }

  }
  // cross validate training set to compute knn score for each recipient in address book
  static void cross_validation() throws IOException{
    int total_number = cross_set.size();
    int batch_size = total_number/10;
    int k = 0;
    while (k<11){
      int st_size = batch_size*k+1;
      int end_size = Math.min(st_size+batch_size,total_number);
      for (int i=st_size;i<end_size;i++){
        String toBePredict = cross_set.get(i);
        predict_cross(toBePredict);
      }
      k++;
    }

  }

  static void predict(String line) throws IOException{
    num_of_docs++;
    String[] elements = line.split("\t");
    String[] addrs = elements[1].split("!"); // actual recipients
    if (addrs.length==1) return;//only predict emails with valid cc/bcc true recipients
    total_email+=1;
    String[] tos = addrs[0].split(",");
    List<String> excludeList =  Arrays.asList(tos);
    
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
    TreeMap<Integer,Double> simi = similarity(doc_word,toks);

    int knn = 0;
    HashMap<Integer,Double> knn_map = new HashMap<Integer,Double>();
    ArrayList<Map.Entry<Integer, Double>> sortedS = new ArrayList<Entry<Integer, Double>>(simi.entrySet());
    Collections.sort(sortedS, new Comparator<Map.Entry<Integer, Double>>() {
      @Override
      public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    for (Map.Entry<Integer, Double> node : sortedS) {
      if (knn<30){
        int id = node.getKey();
        double sc = node.getValue();
        knn_map.put(id,sc);
        knn++;

      }
      else break;
    }
    
    HashSet<String> candidates = processLabels(knn_map);//a pool of candidate recipients
    TreeMap<String,Double> result = new TreeMap<String,Double>();
    Iterator itr2 = candidates.iterator();
   
    while (itr2.hasNext()){

      String addr = (String)itr2.next();//this candidate
      double can_score = 0.0;
      Iterator itr3 = knn_map.keySet().iterator();
      while (itr3.hasNext()){
        int docid = (int)itr3.next();
        String l = doc_labels.get(docid);
        if (l.indexOf(addr)>=0){//y(d,c) check if this doc has this candidate
          can_score+=knn_map.get(docid);
        }

      }
      result.put(addr,can_score);
    }
    
    HashMap<String,Double> rerank_map = new HashMap<String,Double>();
    ArrayList<Map.Entry<String, Double>> sortedR = new ArrayList<Entry<String, Double>>(result.entrySet());
    Collections.sort(sortedR, new Comparator<Map.Entry<String, Double>>() {
      @Override
      public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    int e = 0;
    for (Map.Entry<String, Double> node : sortedR) {
     
        String id = node.getKey();
      
        double sc = rerank(id,tos);
        rerank_map.put(id,sc);
         
     
    }
    ArrayList<Map.Entry<String, Double>> sorted_rerank = new ArrayList<Entry<String, Double>>(rerank_map.entrySet());
    Collections.sort(sorted_rerank, new Comparator<Map.Entry<String, Double>>() {
      @Override
      public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    boolean p_flag = false;
    for (Map.Entry<String, Double> node : sorted_rerank) {
        if (e<20&&e<sorted_rerank.size()){
        String top_candidate = node.getKey();
        if (line.indexOf(top_candidate)>-1){
          if (!p_flag) {
            correct++;
            p_flag = true;
        }
          correct_p++;
        }
        e++;
      }
      else break;
    }
    total_retreived += tos.length;
    if (addrs.length>1) total_retreived+=addrs[1].split(",").length;
    total_relevant+=Math.min(e,sorted_rerank.size());
    /*
                for (int w=0;w<addrs.length;w++){
                  if (c.contains(addrs[w])){
                    correct++;
                    break;}
                }
                total_email+=1;*/
  }
  static double rerank(String r,String[] tos){
    double knn_score = knnscore.get(r);
    int sent = 0;
    if (sent_freq.containsKey(r))
    sent = sent_freq.get(r);
    int recent = 0;
    if (recent_freq.containsKey(r)) 
    recent = recent_freq.get(r);
    double co = 0.0;
    for (int i=0;i<tos.length;i++){
      String lookup = tos[i]+","+r;
      if (cooccur.containsKey(lookup)){
          co+=cooccur.get(lookup);
        }
    }
    if (co!=0) co=co/(double)sent;
    Iterator itr = voted_vectors.keySet().iterator();
    double result = 0.0;
    while (itr.hasNext()){
      int k = (int)itr.next();
      double c = weights.get(k);
      Double[] v = voted_vectors.get(k);
      //double vx = v[0]*knn_score+v[1]*sent+v[2]*recent+v[3]*co;
      double vx = v[0]*knn_score+v[1]*sent+v[2]*recent;
      if (vx>0) result+=c;
      else result-=c;
      
    }
    return result;
    }
  
  static void predict_cross(String line) throws IOException{
    // num_of_docs++;
    String[] elements = line.split("\t");
    //String[] addrs = elements[1].split(","); // actual recipients
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

    TreeMap<Integer,Double> simi = similarity(doc_word,toks);

    int knn = 0;
    HashMap<Integer,Double> knn_map = new HashMap<Integer,Double>();
    ArrayList<Map.Entry<Integer, Double>> sortedS = new ArrayList<Entry<Integer, Double>>(simi.entrySet());
    Collections.sort(sortedS, new Comparator<Map.Entry<Integer, Double>>() {
      @Override
      public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    for (Map.Entry<Integer, Double> node : sortedS) {
      if (knn<30){
        int id = node.getKey();
        double sc = node.getValue();
        knn_map.put(id,sc);
        knn++;

      }
      else break;
    }

    HashSet<String> candidates = processLabels(knn_map);//a pool of candidate recipients
    Iterator itr2 = candidates.iterator();
    while (itr2.hasNext()){

      String addr = (String)itr2.next();//this candidate
      double can_score = 0.0;
      Iterator itr3 = knn_map.keySet().iterator();
      while (itr3.hasNext()){
        int docid = (int)itr3.next();
        String l = doc_labels.get(docid);
        if (l.indexOf(addr)>=0){//y(d,c) check if this doc has this candidate
          can_score+=knn_map.get(docid);
        }

      }
      knnscore.put(addr,can_score);
      //System.out.println("address : "+addr+" score: "+can_score);
    }
  }

  static HashSet<String> processLabels(HashMap<Integer,Double> map){
    HashSet<String> can = new HashSet<String>();
    Iterator i = map.keySet().iterator();
    while (i.hasNext()){
      int id = (int)i.next();
      String labels = doc_labels.get(id);
      String[] addr = labels.split("!");
      for (int j=0;j<addr.length;j++){
        String[] temp = addr[j].split(",");
        for (int k=0;k<temp.length;k++)
          can.add(temp[k]);
      }
    }
    return can;
  }

  static TreeMap<Integer,Double> similarity(HashMap<String,Integer> u,Vector<String> toks){
    TreeMap<Integer,Double> similar = new TreeMap<Integer,Double>();
    Iterator itr = doc_dict.keySet().iterator();

    while (itr.hasNext()){//calculate each doc in train's similarity
      int doc_num = (int)itr.next();
      //System.out.println("train doc num is "+doc_num);
      ArrayList<String> list = doc_dict.get(doc_num);
      Iterator itr2 = toks.iterator();
      //HashSet<String> common_words = new HashSet<String>();
      double numerator = 0.0; //cumulate u * v in common words
      double testDoc_length = 0.0;//||u(d)||
      double vDoc_length=0.0;//||v(d)||
      while (itr2.hasNext()){//iterate through test doc's words
        String w = (String)itr2.next();
        //System.out.println("test Doc Word: "+w);
        Integer d = df.get(w);
        int dF_W = 0;
        if (d!=null) {dF_W = d;
        //System.out.println("df_w "+dF_W);
        int tF_W = u.get(w);
        //System.out.println("tF_w "+tF_W);
        double iDF_W = total_docs/(double)dF_W;
        //System.out.println("iDF_W"+iDF_W);
        double u_WD = Math.log((double)tF_W+1)*Math.log(iDF_W);
        //System.out.println("u_WD "+u_WD);
        testDoc_length+=u_WD*u_WD;
        if (list.contains(w)) {//check if it's a common word
          String search = doc_num + "," + w;//construct doc,word
          //  System.out.println("common word : "+w);
          int tF_doc = tf.get(search);//this doc this word 's TF
          double v_WD = Math.log((double)tF_doc+1)*Math.log(iDF_W);
          //  System.out.println(v_WD);
          numerator+=v_WD * u_WD;//dot product same word's u
        }
        }

      }//END OF ITERATE THROUGH TEST DOC
      //System.out.println("numerator is "+numerator+" test doc length "+testDoc_length);

      Iterator itr3 = list.iterator();//iterate through this doc's all words
      while (itr3.hasNext()){//calculate ||v||
        String word = (String)itr3.next();
        String dw = doc_num + "," + word;
        Integer f = df.get(word);
        int df_v = 0;
        if (f!=null) df_v = f;
        int tf_v = tf.get(dw);
        double idf_v = total_docs/(double)df_v;
        double v = Math.log((double)tf_v+1)*Math.log(idf_v);
        vDoc_length+=v*v;
      }//END OF ITERATE THROUGH LIST
      //System.out.println("vDoc_length is "+vDoc_length);
      double sim = numerator/Math.sqrt(testDoc_length*vDoc_length);//calculate similarity
      //System.out.println("sim score is "+sim+" doc number is "+doc_num);
      similar.put(doc_num,sim);//put it into tree map

    }
    //System.out.println(similar.size());
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
      String[] labels = msg[1].split("!");
      //coccurrence
      if (labels.length>1){
        String[] to_labels = labels[0].split(",");
        String[] cc_labels = labels[1].split(",");
        for (int q=0;q<to_labels.length;q++){
          for (int p=0;p<cc_labels.length;p++){
            String to_cc = to_labels[q]+","+cc_labels[p];
            if (!cooccur.containsKey(to_cc)) cooccur.put(to_cc,1);
            else cooccur.put(to_cc,cooccur.get(to_cc)+1);
          }
        }
      }
      int docnum = Integer.parseInt(check[1]);
      for (int m=0;m<labels.length;m++){
        String[] tp = labels[m].split(",");
        for (int i=0;i<tp.length;i++){
          if (!sent_freq.containsKey(tp[i]))
            sent_freq.put(tp[i],1);
          else sent_freq.put(tp[i],sent_freq.get(tp[i])+1);
          if (!random_sent_recipient.contains(tp[i])) random_sent_recipient.add(tp[i]);
          total_sent++;
        }
      }
      doc_labels.put(docnum,msg[1]);

      //System.out.println(input);
    }
    else {
      //System.out.println(msg[0]);
      int doc_id = Integer.parseInt(check[0]);
      if (prev!=Integer.parseInt(check[0])) {
        prev = Integer.parseInt(check[0]);
        ArrayList<String> w = new ArrayList<String>();
        w.add(check[1]);
        doc_dict.put(prev,w);
        tf.put(msg[0],Integer.parseInt(msg[1]));//(d,w) #

      }

      else {
        ArrayList<String> w = doc_dict.get(Integer.parseInt(check[0]));
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

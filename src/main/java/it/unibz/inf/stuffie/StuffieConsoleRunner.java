package it.unibz.inf.stuffie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.*;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;


// for heideltime
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.unihd.dbs.heideltime.standalone.*;
import de.unihd.dbs.heideltime.standalone.exceptions.*;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;


public class StuffieConsoleRunner {
	
	//for database in neo4j
	static Driver driver;

	private static LinkedHashMap<String, String> commands = new LinkedHashMap<>();
	private static LinkedHashMap<String, String> shorthandCommands = new LinkedHashMap<>();
	private static LinkedHashMap<String, String> loweredKeyCommands = new LinkedHashMap<>();
	private static StringBuilder validModes = new StringBuilder();
	private static StringBuilder validModesAndVals = new StringBuilder();
	private static LinkedHashMap<String, String> validVals = new LinkedHashMap<>();

	private static void initCommands() throws IOException {
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get("resource/console_commands.txt"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] command = line.split("-");
				commands.put(command[0], command[1]);
				loweredKeyCommands.put(command[0].toLowerCase(), command[0]);
				if (command.length > 2)
					shorthandCommands.put(command[1], command[2]);
			}
		}

		for (Class<?> x : Mode.class.getClasses()) {
			validModesAndVals.append("\t\t" + x.getSimpleName() + "=[");
			validModes.append(x.getSimpleName() + ", ");
			StringBuilder vals = new StringBuilder();
			for (Object enumval : x.getEnumConstants()) {
				vals.append(enumval.toString()).append(", ");
				validModesAndVals.append(enumval.toString()).append("|");
			}
			vals.deleteCharAt(vals.length() - 1).deleteCharAt(vals.length() - 1).append(".");
			validVals.put(x.getSimpleName(), vals.toString());
			validModesAndVals.deleteCharAt(validModesAndVals.length() - 1).append("]\n");
		}
		validModes.deleteCharAt(validModes.length() - 1).deleteCharAt(validModes.length() - 1).append(".");
		validModesAndVals.deleteCharAt(validModesAndVals.length()-1);
	}

	private static Mode[] getCustomModes(String[] args) {
		Mode[] modes = new Mode[args.length];

		int i = 0;
		for (String arg : args) {
			Mode m = getValidMode(arg);
			if (m == null)
				System.exit(1);
			modes[i] = getValidMode(arg);
			i++;
		}

		return modes;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Mode getValidMode(String arg) {
		String[] mode = arg.split("=");

		if (mode.length != 2) {
			System.out.println("Invalid mode change command: " + arg + ".");
		}

		Class<Enum> cls;
		try {
			cls = (Class<Enum>) Class.forName("it.inf.unibz.stuffie.Mode$" + mode[0]);
			System.out.println("Succesfully changed " + mode[0] + " to " + mode[1]);
			return (Mode) Enum.valueOf(cls, mode[1]);
		} catch (ClassNotFoundException e) {
			System.out.println("Mode not found: " + mode[0] + ". The accepted modes are: " + validModes.toString() + "\n");
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to change mode: " + mode[0] + ". Value not found: " + mode[1]
					+ ". The valid values are: " + validVals.get(mode[0]) + "\n");
		}

		return null;
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {

		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
	    props.setProperty("ner.applyFineGrained", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
		initCommands();
		Mode[] modes = getCustomModes(args);
		Stuffie stuffie = new Stuffie(modes);

		HeidelTimeStandalone heidelTime = new HeidelTimeStandalone(Language.ENGLISH,DocumentType.COLLOQUIAL,OutputType.TIMEML,"C:\\Users\\Reen\\Desktop\\HT\\heideltime-standalone-2.2.1\\heideltime-standalone\\config.props",POSTagger.TREETAGGER, true);
		
		
		//String file = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\resource\\SentencesApril2018Try.csv";
		//String file = "C:\\Users\\Reen\\Desktop\\DBFiles\\DBNew\\Locations\\SentencesDecember2017V2.csv";
		
		File folder = new File("C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\DBTest");
		File[] listOfFiles = folder.listFiles();
		
		int neo4j_id = 1;
		for (int i = 0; i < listOfFiles.length; i++) {
		  if (listOfFiles[i].isFile()) {
		    System.out.println("File " + listOfFiles[i].getName());
		    
		    //neo4j
		    //String file = "C:\\Users\\Reen\\Desktop\\DBFiles\\DBNew\\Locations\\2016\\" + listOfFiles[i].getName().toString();
			
		    String orig_file = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\DBTest\\" + listOfFiles[i].getName().toString();
		    String writefile = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\DBTest\\Locations\\" + listOfFiles[i].getName().toString();
		    String location_file = getSentencesFromFile(orig_file,writefile);
		    
		    
		    // DATABASE CREDENTIALS
		    AccessDatabase("bolt://localhost:7687", "neo4j", "123456");
			
			neo4j_id = process_file(neo4j_id,location_file,args,modes,stuffie,heidelTime);
			
			//neo4j
			closeDatabase();
		  } 
		}
		
		
		
		//neo4j
		//AccessDatabase("bolt://localhost:7687", "neo4j", "123456");
		
		//int neo4j_id = 54656;
		//neo4j_id = process_file(neo4j_id,file,args,modes,stuffie,heidelTime);
		
		//neo4j
		//closeDatabase();
		
		/*Scanner reader = new Scanner(System.in);
		String text = "";
		while (!text.equals("q")) {
			System.out.println("Enter text to extract, or <h> for help: ");
			text = reader.nextLine();
			if(text.isEmpty()) {
				System.out.println("Empty line. Please try again.");
			}
			else if(text.charAt(0) == '<' && text.charAt(text.length() - 1) == '>') {
				text = text.substring(1, text.length() - 1);
				if (text.contains("=")) {
					System.out.println("*************************");
					Mode m = getValidMode(text);
					if (m != null)
						stuffie.setMode(m);
				} else {
					String textLower = text.toLowerCase();
					if (!loweredKeyCommands.containsKey(textLower) && !shorthandCommands.containsKey(textLower)) {
						System.out.println("Invalid command: " + text + ". Enter <h> to list all valid commands.\n");
					} else if (textLower.equals("help") || textLower.equals("h")) {
						for (String command : commands.keySet()) {
							if (shorthandCommands.containsKey(commands.get(command))) {
								System.out.println("\t<" + command + "> <" + commands.get(command) + ">\t"
										+ shorthandCommands.get(commands.get(command)) + "\n");
							} else {
								System.out.println("\t<" + command + ">\t" + commands.get(command) + "\n");
							}
						}
						System.out.println(validModesAndVals.toString() + "\n");
					} else if (textLower.equals("show modes") || textLower.equals("sm")) {
						System.out.println("Current active modes: " + stuffie.currentModesInString() + ".\n");
					}
				}
			} else {
				System.out.println(stuffie.parseRelation(text));
				//String a = stuffie.parseRelation(text);
				//process_triples(a,pipeline);
			}
		}*/
		
		System.out.print("Bye bye.");
		//reader.close();
	}
	

public static ArrayList<RelationInstance> run_stuffie(String[] args,String text, Mode[] modes, Stuffie stuffie) throws InstantiationException, IllegalAccessException,
	IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {


		ArrayList<RelationInstance> output_final = new ArrayList<RelationInstance>();
		
			if(text.isEmpty()) {
				System.out.println("Empty line. Please try again.");
			}
			else if(text.charAt(0) == '<' && text.charAt(text.length() - 1) == '>') {
				text = text.substring(1, text.length() - 1);
				if (text.contains("=")) {
					Mode m = getValidMode(text);
					if (m != null)
						stuffie.setMode(m);
				} else {
					String textLower = text.toLowerCase();
					if (!loweredKeyCommands.containsKey(textLower) && !shorthandCommands.containsKey(textLower)) {
						System.out.println("Invalid command: " + text + ". Enter <h> to list all valid commands.\n");
					} else if (textLower.equals("help") || textLower.equals("h")) {
						for (String command : commands.keySet()) {
							if (shorthandCommands.containsKey(commands.get(command))) {
								System.out.println("\t<" + command + "> <" + commands.get(command) + ">\t"
										+ shorthandCommands.get(commands.get(command)) + "\n");
							} else {
								System.out.println("\t<" + command + ">\t" + commands.get(command) + "\n");
							}
						}
						System.out.println(validModesAndVals.toString() + "\n");
					} else if (textLower.equals("show modes") || textLower.equals("sm")) {
						System.out.println("Current active modes: " + stuffie.currentModesInString() + ".\n");
					}
				}
			} else {
				output_final = stuffie.parseRelation(text);
				//System.out.println(output_final);
			}
			return output_final;
			
	}

    public static ArrayList<String> getTripleComponents(RelationInstance relIns){
		
    	ArrayList<String> tripleComps = new ArrayList<String>();
    	String NULL_CMPNT = "<_>";
    	
    	String id = relIns.getId().toString();
		RelationArgument sub = relIns.getSubject();
		RelationArgument ob = relIns.getObject();
		RelationVerb pred = relIns.getVerb();
		
		tripleComps.add(id);
		
		if(sub!=null) {
			tripleComps.add(sub.toString());
		}
		else {
			tripleComps.add(NULL_CMPNT);
		}
		
		tripleComps.add(pred.toString());
		
		if(ob!=null) {
			tripleComps.add(ob.toString());
		}
		else {
			tripleComps.add(NULL_CMPNT);
		}
    	
    	return tripleComps;
    	
    }
    
    public static String process_entity(String entity) {
    	
    	entity = entity.replace("#","");
    	entity = entity.replace(";","");
    	entity = entity.replace(">","");
    	entity = entity.trim();
    	return entity;
    }
    

	public static int process_file(int neo4j_id,String file,String[] args, Mode[] modes, Stuffie stuffie,HeidelTimeStandalone heideltime) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException   {
		
		//String dest = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\resource\\OutputforSentencesV1_try.txt";

		try {
		    	
		    	
		    	BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		    	CSVReader csvReader = new CSVReader(in); 
		    	String[] cell;
		    	
		    	//FileWriter fstream = new FileWriter(dest, true);
		    	//BufferedWriter out = new BufferedWriter(fstream);
		    	//int neo4j_id = 19976;
		    	
				while ((cell = csvReader.readNext()) != null) {
		            // use comma as separator
					

					//System.out.println(00000000);
		            System.out.println( cell[0] + "\n" + cell[1] + "\n" + cell[2].toString() + "**********");
		            
		            
		            
		            
		            String headline = cell[1];
		            String sentence = cell[2];
		            
		            
		            String date="";
		            date = processDates(cell[0],sentence,heideltime);
					
		            
					//get Locations and remove duplicates
		            //String locations = findLocations(sentence);
		            String locations = cell[3];
		            if(locations!="") {
		            String[] result_array = locations.split(";");
        			List<String> result_list = Arrays.asList(result_array);
        			List<String> newList = result_list.stream().distinct().collect(Collectors.toList());
        			locations = String.join(";", newList);
		            }
		            
		            
		            //System.out.println(locations);
					try {
						
						//get stuffie output
						ArrayList<RelationInstance> triples = run_stuffie(args,cell[2].toString(),modes, stuffie);
						
						//valid ids: id's of triples for which subjects and objects are not empty placeholders
						ArrayList<String> validIDs = new ArrayList<String>();
						HashMap<String,String> tripleids_dbids = new HashMap<String,String>(); 
						
						// create unique ID's map
						for(RelationInstance relIns: triples) {
							
							ArrayList<String> tripleComps = getTripleComponents(relIns);
							
							String id = tripleComps.get(0).toLowerCase();
							String subject = tripleComps.get(1).toLowerCase();
							String predicate = tripleComps.get(2).toLowerCase();
							String object = tripleComps.get(3).toLowerCase();
							
							//add id to hashmap
							tripleids_dbids.put(id,Integer.toString(neo4j_id));
							neo4j_id+=1;
							
							//System.out.println("Here are the facets" + " $$$" + subject + "$$$ " + object + "$$$ " + predicate);
							//System.out.println(relIns.getFacets());
						
						} // for(RelationInstance relIns: triples)
							
						for(RelationInstance relIns: triples) {
							
							ArrayList<String> tripleComps = getTripleComponents(relIns);
							
							String id = tripleComps.get(0).toLowerCase();
							String subject = tripleComps.get(1).toLowerCase();
							String predicate = tripleComps.get(2).toLowerCase();
							String object = tripleComps.get(3).toLowerCase();
							
							//find id from map and create triple node
							String event_dbid = tripleids_dbids.get(id);
							String event_name = relIns.toString().toLowerCase();
							addNode(event_dbid,event_name,"event");
							
							
							//get date
							/*date = date.replace( "\\", " " );
							String[] date_comps = date.split("-");
							String day = date_comps[0];
							String month = date_comps[1];
							String year = date_comps[2];
							String new_date = (year + month + day);
							//int final_date = Integer.parseInt(new_date);
							//System.out.println(new_date);*/
							
							addNode(event_dbid,date,"date");
							
							//add sentence as property
							addNode(event_dbid,sentence,"sentence");
							//addExtras(event_dbid,sentence,"sentence");
							
							//add headline as property
							addNode(event_dbid,headline,"headline");
							//addExtras(event_dbid,headline,"headline");

													
							//add Location Node
							addNode(event_dbid,locations,"location");
							//addExtras(event_dbid,locations,"location");
							
							//check for <ctx#> and #link in subject and object
							
							Pattern p = Pattern.compile("\\#(.*)");
							Matcher p_s = p.matcher(subject);
							Matcher  p_o = p.matcher(object);
							
					    	Pattern ctx = Pattern.compile("(\\<(ctx#.*)\\>)");
							Matcher ctx_s = ctx.matcher(subject);
							Matcher ctx_o = ctx.matcher(object);
							
							// #id and <ctx#id> dont occur together in the same part of triple
							
							
							//PROCESS SUBJECT
							//check for <ctx#id> in subject, else check for #id
							if(ctx_s.find()) {
								//System.out.println(ctx_s.group() + "***********");
								String sb = subject.replace(ctx_s.group(), "");
								sb = sb.replace("<", "");
								sb = sb.replace(">", ">");
								String sb_lemma = "";
								sb = sb.replace(";", "");
								sb = sb.trim();
								if(!sb.contentEquals("<_>")) {
									Lemmatizer doc = new Lemmatizer();
							    	sb_lemma = doc.lemmatize(subject);
								}
								//System.out.println(ctx_s.group() + "***********sub" + sb);
								addSVO(event_dbid,sb,"subject",sb_lemma);
							}
							else{
								if(p_s.find()) {
									
									//create node e' and linked: e=>e' with link as 'has subject'
									
									//System.out.println(p_s.group());
									
									String temp_link_id = p_s.group();
									String link_id = process_entity(temp_link_id);
									
									//System.out.println(link_id);
									
									//map for link_id's node id
									String db_linkID = tripleids_dbids.get(link_id);
									addSpRelation(event_dbid,db_linkID,"hasSubject");
									
								}
								else {
									//create subject node
									String sb_lemma = "";
									subject = subject.replace(";", "");
									subject = subject.trim();
									if(!subject.contentEquals("<_>")) {
										Lemmatizer doc = new Lemmatizer();
								    	sb_lemma = doc.lemmatize(subject);
									}
									addSVO(event_dbid,subject,"subject",sb_lemma);
								}
							}
							
							//PROCESS PREDICATE
							predicate = predicate.replace(";", "");
							predicate = predicate.trim();
							String pr_lemma = "";
							if(!predicate.contentEquals("<_>")) {
								Lemmatizer doc = new Lemmatizer();
						    	pr_lemma = doc.lemmatize(predicate);
							}
							addSVO(event_dbid,predicate,"predicate",pr_lemma);
							
							//PROCESS OBJECT
							if(ctx_o.find()) {
									
									String ob = object.replace(ctx_o.group(), "");
									ob = ob.replace("<", "");
									ob = ob.replace(">", ">");
									String ob_lemma = "";
									ob = ob.replace(";", "");
									ob = ob.trim();
									if(!ob.contentEquals("<_>")) {
										Lemmatizer doc = new Lemmatizer();
										ob_lemma = doc.lemmatize(ob);
									}
									System.out.println(ctx_o.group() + "***********,obb" + ob);
									addSVO(event_dbid,ob,"object",ob_lemma);
								
							}
							else {
								if(p_o.find()) {
									
									//create node e' and linked: e=>e' with link as 'has object'
									
									//System.out.println(p_o.group());
									
									String temp_link_id = p_o.group();
									String link_id = process_entity(temp_link_id);
									
									//System.out.println(link_id);
									
									String linkID = tripleids_dbids.get(link_id);
									addSpRelation(event_dbid,linkID,"hasObject");

								}
								else {
									//create Object node
									object = object.replace(";", "");
									object = object.trim();
									String ob_lemma = "";
									if(!object.contentEquals("<_>")) {
										Lemmatizer doc = new Lemmatizer();
								    	pr_lemma = doc.lemmatize(object);
									}
									addSVO(event_dbid,object,"object",ob_lemma);
								}
								
								
								
							}
							
							//PROCESS FACETS
							System.out.println(relIns.getFacets());
							for(RelationArgument value: relIns.getFacets()) {
								
								System.out.println(value);
								if(value!=null) {
								String[] data = (value.toString()).split(";");
								
								String connector = data[0].toLowerCase();
								connector = connector.replace(";", "");
								connector = connector.trim();
								
								String target = data[1].toLowerCase();
								target = target.replace(";", "");
								target = target.trim();
								
								process_facet(connector,target,event_dbid,tripleids_dbids);
								}
							}
							
							
													
						}
						
					// Stuffie output try-catch
					}catch(Exception e) {
					   e.printStackTrace();
					}
				}
		   
		       in.close();
		       System.out.println(neo4j_id);
		    // file read try-catch
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    
			return neo4j_id;
	}
	
	
	public static void process_facet(String connector, String target, String event_dbid, HashMap<String,String> tripleids_dbids) {
		
		Pattern mtch_links = Pattern.compile("\\#(.*)");
		Matcher  t_ml = mtch_links.matcher(target);
		
		Pattern mtch_ctx = Pattern.compile("(\\<(ctx#.*)\\>)");
		Matcher  t_ctx = mtch_ctx.matcher(target);
		
		if(t_ctx.find()) {
			//if target has a context, create context link as well as target node
			
			
			String targ = target.replace(t_ctx.group(), "");
			targ = targ.replace("<", "");
			targ = targ.replace(">", ">");
			String targ_lemma = "";
			targ = targ.replace(";", "");
			targ = targ.trim();
			if(!targ.contentEquals("<_>")) {
				Lemmatizer doc = new Lemmatizer();
				targ_lemma = doc.lemmatize(targ);
			}
			addFacet(event_dbid,connector,target,"facet",targ_lemma);
			System.out.println(t_ctx.group());
		}
		else {
			if(t_ml.find()) {
				
				// if target has link, create that event node and link with connector
		
				
				String temp_link_id = t_ml.group();
				String link_id = process_entity(temp_link_id);
				
				
				//System.out.println(link_id);
				
				String linkID = tripleids_dbids.get(link_id);
				addSpFacet(event_dbid,connector,linkID,"facet");

			}
			else {
				String locations =  findLocations(target);
				String target_lemma="";
				//create Facet node
				if(locations=="") {
					if(!target.contentEquals("<_>")) {
						Lemmatizer doc = new Lemmatizer();
						target_lemma = doc.lemmatize(target);
					}
				addFacet(event_dbid,connector,target,"facet",target_lemma);
				}
			}
		}
		
	
		
	}
	
	public static String processDates(String refdate,String text,HeidelTimeStandalone heidelTime) {
		String date = "";
		
		
		
		//String text = "I tried out many options yesterday, but failed. I can't plan this day after "
				//+ "tomorrow, so I have to do this today or tomorrow ";
		try {
		Date refDate= new SimpleDateFormat("dd-MM-yyyy").parse(refdate);
		String xmlDoc = heidelTime.process(text, refDate);
		
		System.out.println(xmlDoc);
		ArrayList<String> listDates = getDates(xmlDoc,refdate);
		//System.out.println(listDates);
		
		
		if(listDates.size()==1) {
			return listDates.get(0).toString();
		}
		else {
			return refdate;
		}
		}catch(Exception e) {
			e.printStackTrace();
			return refdate;
		}
		
	}
	
	public static ArrayList<String> getDates(String xmlDoc,String refdate){
		
		//process xmlDoc to get rid of meta tags
		String[] list = xmlDoc.split("\n");
		StringBuilder processedDoc = new StringBuilder();
		for(int i=2;i<list.length;i++) {
			processedDoc.append(list[i]);
		}
			
		System.out.println(processedDoc.toString());
		
		/* list of dates to be returned  */
		ArrayList<String> dates = new ArrayList<String>();
	    try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(processedDoc.toString())));
					
			NodeList nList = doc.getElementsByTagName("TimeML");
	
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);					
				System.out.println("\nCurrent Element :" + nNode.getNodeName());
				
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					NodeList x3List = eElement.getElementsByTagName("TIMEX3");
					
					for (int i = 0; i < x3List.getLength(); i++) {
						Node x3Node = x3List.item(i);								
						System.out.println("\nCurrent Element :" + x3Node.getNodeName());
							
						if (x3Node.getNodeType() == Node.ELEMENT_NODE) {
							Element x3Element = (Element) x3Node;							
							System.out.println("Name : " + x3Element.getTextContent());
							System.out.println("Date : " + x3Element.getAttribute("value"));
							
							/* convert date in string format to Date object
							 */
							 String str_date = x3Element.getAttribute("value");
							 SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-YYYY");
							 
							 String[] date_check = str_date.split("-");
							 if(date_check.length>2) {
								 Date date=new SimpleDateFormat("yyyy-MM-dd").parse(str_date);
								 String strDate= formatter.format(date);
								 dates.add(strDate);
							 }
							 else {
								 str_date = str_date.replace("W", "");
								 Date date=new SimpleDateFormat("yyyy-ww").parse(str_date);
								 String strDate= formatter.format(date);
								 dates.add(strDate);
							 }
						}
					}
				}
			}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	dates.add(refdate);
	    	return dates;		
	    }
	    return dates;
	  }
	
	 public static void AccessDatabase(String uri, String user, String password){
	        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
	 }
	 
	 public static void addSVO(String id, String name, String type,String lemma) {
		 try (Session session = driver.session())
	        {
	            // Wrapping Cypher in an explicit transaction provides atomicity
	            // and makes handling errors much easier.
	            try (Transaction tx = session.beginTransaction())
	            {
	                if(type.contentEquals("subject")) {
	                	tx.run("MERGE (n:EntityNode {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", name,"lemma_form",lemma));
	                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:EntityNode {name: {name}})" + "MERGE (a)-[r:HAS_SUBJECT]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
	                }
	                else if(type.contentEquals("object")) {
	                	tx.run("MERGE (n:EntityNode {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", name,"lemma_form",lemma));
	                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:EntityNode {name: {name}})" + "MERGE (a)-[r:HAS_OBJECT]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
	                }
	                else if(type.contentEquals("predicate")) {
	                	tx.run("MERGE (n:Predicate {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", name,"lemma_form",lemma));
	                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:Predicate {name: {name}})" + "MERGE (a)-[r:HAS_PREDICATE]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
	                }
	            	
	            	//tx.run("MERGE (b:Person {name: 'mynewnode'})");
	                tx.success();  // Mark this write as successful.
	            }
	            session.close();
	        }
	 }
	 
	 
	 private static void addNode(String id, String name, String type){
	        // Sessions are lightweight and disposable connection wrappers.
	        try (Session session = driver.session())
	        {
	            // Wrapping Cypher in an explicit transaction provides atomicity
	            // and makes handling errors much easier.
	            try (Transaction tx = session.beginTransaction())
	            {

	                if(type.contentEquals("event")) {
	                	tx.run("MERGE (n:Event {dbid: {x}})", parameters("x", id));
	                }
	                
	                else if(type.contentEquals("date")) {
	                	String date = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.date = {y}" + "RETURN n",parameters("x",id,"y",date));
	                	//tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.date = {y}" + "RETURN n",parameters("x",id,"y",date));
	                }
	                
	                else if(type.contentEquals("sentence")) {
	                	String sentence = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.sentence = {y}" + "RETURN n",parameters("x",id,"y",sentence));
	                }
	                
	                else if(type.contentEquals("headline")) {
	                	String headline = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.headline = {y}" + "RETURN n",parameters("x",id,"y",headline));
	                }
	                
	                else if(type.contentEquals("location")) {
	                	String location = name;
	                	tx.run("MERGE (n:Event {dbid: {x}})" + "SET n.location = {y}" + "RETURN n",parameters("x",id,"y",location));
	                }
	            	
	            	//tx.run("MERGE (b:Person {name: 'mynewnode'})");
	                tx.success();  // Mark this write as successful.
	            }
	            session.close();
	        }
	 }
	 
	  
	    
	 
	    public static void addFacet(String id, String connector, String target, String type, String lemma) {
	    	try (Session session = driver.session())
	        {
	    		try (Transaction tx = session.beginTransaction())
	            {
	    			if(type.contentEquals("facet")) {
	                	tx.run("MERGE (n:Facet {name: {x}})" + "SET n.lemma = {lemma_form}" + "RETURN n", parameters("x", target,"lemma_form",lemma));
	                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:Facet {name: {facet_name}})" + "MERGE (a)-[r:CONNECTING_CLAUSE {name:{rel_name}}]->(b)" + "RETURN a,b",parameters("myid",id,"facet_name",target,"rel_name",connector));
	                }
	    			tx.success(); 
	            }
	    		session.close();
	        }
	    }
	 
	    public static void addSpFacet(String nodeid1, String connector, String nodeid2, String type) {
	    	try (Session session = driver.session())
	        {
	    		try (Transaction tx = session.beginTransaction())
	            {
	    			if(type.contentEquals("facet")) {
	    				tx.run("MERGE (n:Event {dbid: {x}})", parameters("x", nodeid2));
	    				tx.run("MATCH (a:Event {dbid: {myid} }),(b:Event {dbid: {myid2}})" + "MERGE (a)-[r:CONNECTING_CLAUSE {name:{rel_name}}]->(b)" + "RETURN a,b",parameters("myid",nodeid1,"myid2",nodeid2,"rel_name",connector));	
	    			}
	    			tx.success(); 
	            }
	    		session.close();
	        }
	    }
	    
	 	public static void addSpRelation(String nodeId1, String nodeId2, String relation) {
	 		try (Session session = driver.session()){
	 			
	 			try (Transaction tx = session.beginTransaction())
	            {
	 				 tx.run("MERGE (n:Event {dbid: {x}})", parameters("x", nodeId2));
	 				 if(relation.contentEquals("hasSubject")) {
		                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:Event {dbid: {myid2}})" + "MERGE (a)-[r:HAS_SUBJECT]->(b)" + "RETURN a,b",parameters("myid",nodeId1,"myid2",nodeId2));
		             }
	 				if(relation.contentEquals("hasObject")) {
	                	tx.run("MATCH (a:Event {dbid: {myid} }),(b:Event {dbid: {myid2}})" + "MERGE (a)-[r:HAS_OBJECT]->(b)" + "RETURN a,b",parameters("myid",nodeId1,"myid2",nodeId2));
	 				}
	            }
	 		    
	 			session.close();
	        }
	 	}
	 	
	 	
	 	
	 	//locations function
	 	public static void iterateOver(HashMap<String, ArrayList<ArrayList<String>>> map,String writefile) {
			
			// headline is key, [date,sentence,location,1/0]
			
			Iterator it = map.entrySet().iterator();
		    while (it.hasNext()) {
		    	
		    	
		    	Map.Entry pair = (Map.Entry)it.next();
		    	String headline = pair.getKey().toString();
		    	
		    	ArrayList<ArrayList<String>> array_values = (ArrayList<ArrayList<String>>) pair.getValue();
		    	
		    	int count_index = 0;
		    	
		    	for(ArrayList<String> array: array_values) {
		    		//System.out.println(array.get(3));
		    		//if location does not exist
		    		if(array.get(3)!="1") {
		    			//System.out.println("Not found");
		    			int f_low = count_index + 1;
		    			int f_high = array_values.size();
		        		String f_result = checkForward(array_values,f_low,f_high);
		        		
		        		int b_low = 0;
		        		int b_high = count_index - 1;
		        		String b_result = checkBackward(array_values,b_low,b_high);
		        		
		        		
		        		if(f_result!="" && b_result!="") {
		        			String result = f_result + ";" + b_result;
		        			
		        			String[] result_array = result.split(";");
		        			List<String> result_list = Arrays.asList(result_array);
		        			List<String> newList = result_list.stream().distinct().collect(Collectors.toList());
		        			String loc = String.join(";", newList);
		        			
		        			array.set(2,loc);
		        		}
		        		else if(f_result=="" && b_result!="") {
		        			//System.out.println(b_result+ " "+ array.get(0));
		        			array.set(2, b_result);
		        		}
		        		else if(f_result!="" && b_result=="") {
		        			//System.out.println(f_result+ " "+ array.get(0));
		        			array.set(2, f_result);
		        		}
		        		else if(f_result=="" && b_result=="") {
		        			//System.out.println(b_result+"))))))))))"+f_result);
		        			//System.out.println("##########");
		        			//System.out.println(array + " " + array_values);
		        			
		        		}
		        	
		        	write_to_csv(array.get(0),headline,array.get(1),array.get(2),writefile);
		        	//System.out.println(array);	
		        	}
		    		else {
		    			write_to_csv(array.get(0),headline,array.get(1),array.get(2),writefile);
		    			//System.out.println(array.get(2) + "************"+array.get(0));
		    		}
		        	count_index+=1;
		    		//System.out.println("\n");
		        } 
		    
		    	it.remove();
		    }
		}
	 	
	 	public static String checkForward(ArrayList<ArrayList<String>> array, int low, int high) {
			String result = "";
			
			if(low<=high) {
				for (int i = low; i<high; i++ ) {
					if(array.get(i).get(3)=="1") {
						return array.get(i).get(2);
					}
				    //System.out.println(array.get(i));
				}
			}
			
			return result;
		}
		
		public static String checkBackward(ArrayList<ArrayList<String>> array, int low, int high) {
			String result = "";
			
			if(low<=high) {
				for (int i = high; i>=low; i-- ) {
					if(array.get(i).get(3)=="1") {
						return array.get(i).get(2);
					}
				    //System.out.println(array.get(i));
				}
			}
			
			return result; 
		}
		
		public static String findLocations(String sentence) {
			
			String output = "";
			Properties props = new Properties();
		    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
		    props.setProperty("ner.applyFineGrained", "false");
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		    CoreDocument doc = new CoreDocument(sentence);
		    pipeline.annotate(doc);
		    //System.out.println("---");
		    //System.out.println("entities found");
		    try {
		    for (CoreEntityMention em : doc.entityMentions()) {
		    	if(em.entityType().contentEquals("LOCATION")) {
		    		if(output.contentEquals("")) {
		    			output = em.text().toString();
		    		}
		    		else {
			    		output = output + ";" + em.text().toString();
		    		}
		    		//System.out.println("\tdetected entity: is a location"+em.text()+"\t"+em.entityType());
		    	}
		    }
		    }catch(Exception e){
		    	e.printStackTrace();
		    }
		    //System.out.println(output);
		    return output;
			
		}
	 	
	 
		public static void printMap(Map mp) {
		    Iterator it = mp.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey());
		        //System.out.println(pair.getKey() + " = " + pair.getValue());
		        ArrayList<ArrayList<String>> value = (ArrayList<ArrayList<String>>) pair.getValue();
		        for(ArrayList<String> array: value) {
		        	for(String item: array) {
		        		System.out.println(item + "*****");
		        	}
		        	System.out.println("\n");
		        }
		        System.out.println("Next Item");
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		}
		
		public static String getSentencesFromFile(String filename,String writefile) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException,IOException {
			
			HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<String, ArrayList<ArrayList<String>>>(); 
			
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
	    	CSVReader csvReader = new CSVReader(in); 
	    	String[] cell;
	    	
			while ((cell = csvReader.readNext()) != null) {
				
				String date = cell[0];
	            String headline = cell[1];
	            String sentence = cell[2];  
	            String locations = findLocations(sentence) ;
	            
	            ArrayList<String> instance = new ArrayList<String>();
	            instance.add(date);
	            instance.add(sentence);
	            instance.add(locations);
	            System.out.println(date+ " " +locations);
	            
	            //check locations null
	            if(locations.contentEquals("") || locations==null) {
	            	//System.out.println("yes");
	            	instance.add("0");
	            }
	            else {
	            	instance.add("1");
	            }
	            
	            
	            //check in map
	            if(map.containsKey(headline)) {
	            	ArrayList<ArrayList<String>> this_list = map.get(headline);
	            	this_list.add(instance);
	            	map.put(headline, this_list);
	            }
	            else {
	            	ArrayList<ArrayList<String>> new_list = new ArrayList<ArrayList<String>>();
	            	new_list.add(instance);
	            	map.put(headline,new_list);
	            }
	            
			}

			//printMap(map);
			iterateOver(map,writefile);
			
			in.close();
			csvReader.close();
			
			return writefile;
		}
		
		public static void write_to_csv(String date, String headline, String sentence, String location, String filename) {
			//String filename = "C:\\Users\\Reen\\Desktop\\DBFiles\\DBNew\\Locations\\SentencesFebruary2016V2.csv";
			File file = new File(filename); 
			try { 
		        FileWriter outputfile = new FileWriter(file,true); 
		  
		        CSVWriter writer = new CSVWriter(outputfile); 
		  
		        String[] data = {date,headline,sentence,location};
		        writer.writeNext(data);
		        /*String[] header = { "Name", "Class", "Marks" }; 
		        writer.writeNext(header); 
		  
		        String[] data1 = { "Aman", "10", "620" }; 
		        writer.writeNext(data1); 
		        String[] data2 = { "Suraj", "10", "630" }; 
		        writer.writeNext(data2);*/
		  
		        writer.close();
		        outputfile.close();
		    } 
		    catch (IOException e) { 
		        // TODO Auto-generated catch block 
		        e.printStackTrace(); 
		    } 
			
		}
		
	    public static void closeDatabase(){
	        // Closing a driver immediately shuts down all open connections.
	        driver.close();
	    }
	    
	   
	
}

//outside class body

/*public static void addExtras(String id, String name, String type) {

try (Session session = driver.session())
{
    // Wrapping Cypher in an explicit transaction provides atomicity
    // and makes handling errors much easier.
    try (Transaction tx = session.beginTransaction())
    {
        if(type.contentEquals("date")) {
        	Integer new_date = Integer.parseInt(name);
        	tx.run("MERGE (n:Date {date: {x}})", parameters("x", name));
        	tx.run("MATCH (a:Event {name: {myid} }),(b:Date {date: {name}})" + "MERGE (a)-[r:HAS_DATE]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
        }
        else if(type.contentEquals("headline")) {
        	tx.run("MERGE (n:Headline {name: {x}})", parameters("x", name));
        	tx.run("MATCH (a:Event {name: {myid} }),(b:Headline {name: {name}})" + "MERGE (a)-[r:HAS_HEADLINE]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
        }
        else if(type.contentEquals("sentence")) {
        	tx.run("MERGE (n:Sentence {name: {x}})", parameters("x", name));
        	tx.run("MATCH (a:Event {name: {myid} }),(b:Sentence {name: {name}})" + "MERGE (a)-[r:HAS_Sentence]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
        }
        else if(type.contentEquals("location")) {
        	tx.run("MERGE (n:Location {name: {x}})", parameters("x", name));
        	tx.run("MATCH (a:Event {name: {myid} }),(b:Location {name: {name}})" + "MERGE (a)-[r:HAS_Location]->(b)" + "RETURN a,b",parameters("myid",id,"name",name));
            
        }
        tx.success();
    }
    session.close();
}

}*/


//check for empty subject object triples
//if(subject!="<_>" && object!="<_>") {
	//validIDs.add(id);
	
//}


//System.out.println(cell);

/*for (String next : cell) { 
       System.out.print(next + "*******" + "\n"); 
}*/ 

// write to file
/*out.write("###############################################################################");
out.write("\n");
out.write(cell[0] + "||||" + cell[1]);
out.write("\n");
out.write("###############################################################################");
out.write("\n");
out.write("\n");
out.write("\n");
out.write("Extracted triples " + "\n" + a);
out.write("\n");
out.write("\n");
out.write("\n");
out.write("\n");*/

/*	public static void process_file(String file, String[] args, Mode[] modes, Stuffie stuffie) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException  {

String dest = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\resource\\OutputTriplesV2.txt";
String line = "";
String a = "None";
try {
	FileReader filereader = new FileReader(file);
	CSVReader csvReader = new CSVReader(filereader); 
	String[] cell;
	
	FileWriter fstream = new FileWriter(dest, true);
	BufferedWriter out = new BufferedWriter(fstream);
	
	 while ((cell = csvReader.readNext()) != null) {
        	
		 for (String next : cell) { 
             System.out.print(next + "*******" + "\n"); 
		 }
		 
		 try {
           a = run_stuffie(args,cell[0],modes, stuffie);
         }
         catch(Exception e){
           System.out.println(cell[0]);
         }
		 
		 
		 out.write("###############################################################################");
         out.write("\n");
         out.write(cell[0]);
         out.write("\n");
         out.write("###############################################################################");
         out.write("\n");
         out.write("\n");
			out.write("\n");
			 out.write("Extracted triples " + "\n" + a);
			out.write("\n");
			out.write("\n");
			out.write("\n");
			out.write("\n");
     }
	 filereader.close();
     out.close();
	
} catch (IOException e) {
    e.printStackTrace();
}


}*/	

/*public static void process_triples(String triples,StanfordCoreNLP pipeline) {

System.out.println(triples);
String[] reltriples = triples.split("\n\n");
String relation = "";

try {
	String dest = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\resource\\OutputEvents.csv";
	File file = new File(dest); 
	FileWriter fstream = new FileWriter(file,true);
	CSVWriter writer = new CSVWriter(fstream); 	

	for(String rel: reltriples) {
		
		ArrayList<String> relfacets = new ArrayList<String>();
		System.out.println("This is my triple " + rel);
		
		ArrayList<String> dates = new ArrayList<String>();
		ArrayList<String> locations = new ArrayList<String>();
		dates.clear();
		locations.clear();
	
		String[] relationfacets = rel.split("\n");
		relation = relationfacets[0];
		//String regex = "^\\d*\\.\\d+|\\d+\\.\\d*$";
		//Pattern pattern = java.util.regex.Pattern.compile(regex);
		//Matcher matcher = pattern.matcher(first_subject);
		//if(matcher.find()) {
			//first_subject = first_subject.replaceFirst(regex, "");
			//first_subject = first_subject.replace(":	", "");
			//possible_subjects.add(first_subject);
		//}
		
		
		if(relationfacets.length>1) {
			for(String r: relationfacets) {
				relfacets.add(r);
			}
		}
		//System.out.println(relation);
	
		//process facets for LOCATION and DATE
		for(String facet: relfacets) {
			facet = facet.replaceAll(";","");
			CoreDocument doc = new CoreDocument(facet);
			pipeline.annotate(doc);
			for (CoreEntityMention em : doc.entityMentions()) {
		    	if(em.entityType().contentEquals("LOCATION")) {
		    		//System.out.println("\tdetected entity: is a location "+em.text()+"\t"+em.entityType());
		    		locations.add(em.text());
		    	}
		    	if(em.entityType().contentEquals("DATE")) {
		    		//System.out.println("\tdetected entity: is a date "+em.text()+"\t"+em.entityType());
		    		dates.add(em.text());
		    	}
		    }		 
		}
		
		//write relation
		String[] rel_to_be_written = relation.split(";");
		List<String> list = new ArrayList(Arrays.asList(rel_to_be_written));
    	
	    System.out.println(relation);
		
		//write dates
		if(dates.size()!=0) {
			 String dates_to_bewritten = String.join(";", dates);
			 list.add(dates_to_bewritten);					 
		}
		else {
			list.add("");
		}

		//write locations	 
		if(locations.size()!=0) {
			String locations_to_bewritten = String.join(";", locations);
			list.add(locations_to_bewritten);	
		}
		else {
			list.add("");
		}
		Object[] to_be_written = list.toArray();
		String[] write_this = Arrays.copyOf(to_be_written,to_be_written.length, String[].class);
		//System.out.println("Heyyyyyyyyyyyy"+ write_this);
		writer.writeNext(write_this);
		dates.clear();
		locations.clear();
}
writer.close();
}catch(Exception e) {
	e.printStackTrace();
}

}*/



	/*public static void read_file(String file,String[] args, Mode[] modes, Stuffie stuffie) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
	String line = "";
    String cvsSplitBy = ",";
    String a = "None";
    String b = "None";
    String c = "None";
    String d = "None";
    String dest = "C:\\Users\\Reen\\Desktop\\stuffie-master\\stuffie-master\\resource\\OutputCo-referencedGraph.txt";

    try {
    	
    	//BufferedReader br = new BufferedReader(new FileReader(file));
    	
    	FileReader filereader = new FileReader(file);
    	CSVReader csvReader = new CSVReader(filereader); 
    	String[] cell;
    	
    	FileWriter fstream = new FileWriter(dest, true);
		BufferedWriter out = new BufferedWriter(fstream);

       // while ((line = br.readLine()) != null) {

		while ((cell = csvReader.readNext()) != null) {
            // use comma as separator
			//System.out.println(cell);
			
			 for (String next : cell) { 
	                System.out.print(next + "*******" + "\n"); 
	         } 
			
            //System.out.println( cell[3] + "\n\n" + cell[6] + "\n\n" + cell[9] + "\n\n" + cell[12]);
            try {
            a = run_stuffie(args,cell[3],modes, stuffie);
            }
            catch(Exception e){
            	System.out.println(cell[3]);
            }
            
            try {
                b = run_stuffie(args,cell[6],modes, stuffie);
                }
            catch(Exception e){
                	System.out.println(cell[6]);
                }
            
            try {
                c = run_stuffie(args,cell[9],modes, stuffie);
                }
            catch(Exception e){
                	System.out.println(cell[9]);
            }
            
            try {
                d = run_stuffie(args,cell[12],modes, stuffie);
                }
            catch(Exception e){
                	System.out.println(cell[12]);
            }

            //output to file
            out.write("###############################################################################");
            out.newLine();
            out.write("               " + cell[0] + "       " );
            out.newLine();
            out.write("###############################################################################");
			out.newLine();
			out.write("Question:  " + cell[1]);
			out.newLine();
			out.write("Option A: " + cell[2] );
			out.newLine();
			out.write("Hypothesis " + cell[3] );
			out.newLine();
			out.newLine();
			out.write("Extracted triples " + "\n" + a);
			
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			

			out.write("Option B: " + cell[5] );
			out.newLine();
			out.write("Hypothesis " + cell[6] );
			out.newLine();
			out.newLine();
			out.write("Extracted triples " + "\n" + b);
			
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			
			
			out.write("Option C: " + cell[8] );
			out.newLine();
			out.write("Hypothesis " + cell[9] );
			out.newLine();
			out.newLine();
			out.write("Extracted triples " + "\n" + c);
			
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			
			out.write("Option D: " + cell[11] );
			out.newLine();
			out.write("Hypothesis " + cell[12] );
			out.newLine();
			out.newLine();
			out.write("Extracted triples " + "\n" + c);
			
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			out.newLine();
			
			
        }
        
		filereader.close();
        out.close();

    } catch (IOException e) {
        e.printStackTrace();
    }
    
	return;
}*/
	
	


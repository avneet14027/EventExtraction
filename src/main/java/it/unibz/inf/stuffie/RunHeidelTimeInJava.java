package it.unibz.inf.stuffie;


import java.io.File;
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


public class RunHeidelTimeInJava {
	public static void main(String[] args) throws DocumentCreationTimeMissingException, ParseException {
		
		/* initialize HeidelTimeStandalone for date extraction */
		HeidelTimeStandalone heidelTime = new HeidelTimeStandalone(Language.ENGLISH,
                DocumentType.COLLOQUIAL,
                OutputType.TIMEML,
                "C:\\Users\\Reen\\Desktop\\HT\\heideltime-standalone-2.2.1\\heideltime-standalone\\config.props",
                POSTagger.TREETAGGER, true);
		String strDate = "21-04-2018";
		Date refDate=new SimpleDateFormat("dd-MM-yyyy").parse(strDate);
		//Date refDate = new Date();
		String text = "The government would write to Army to seek identification of The government personnel allegedly involved in firing on protesting mob in Handwara and Kupwara, where five people, including a woman were killed, and dozens others injured in troops action last week. But i wonder what is the date today?";
		String xmlDoc = heidelTime.process(text, refDate);
		
		System.out.println(xmlDoc);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-wwww");
	    System.out.println(dateFormat.format(new Date()));
		
		ArrayList<String> listDates = get_date(xmlDoc);
		System.out.println(listDates);
		
		File folder = new File("C:\\Users\\Reen\\Desktop\\DBFiles\\DBNew\\Locations\\2016");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
		  if (listOfFiles[i].isFile()) {
			  String file = "C:\\Users\\Reen\\Desktop\\DBFiles\\DBNew\\Locations\\2016\\" + listOfFiles[i].getName().toString();
		    System.out.println("File " + file);
		  } 
		}
		
		
	}
		
	public static ArrayList<String> get_date(String xmlDoc){
		
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
	    }
	    return dates;
	  }
}
		
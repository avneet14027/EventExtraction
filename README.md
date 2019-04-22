Process of Extracting events from News Articles(A demo on processing of data at different phase):

Download the project as zip file , unzip it and open the command prompt inside "/EventExtraction-new-b/Preprocess". 

Run following commands one by one.
For the demo purpose, we have a file named "/Preprocess/TestCoref2016/April2016/April 15,2016_1.txt" which contains a news article with the headline as first line and content(input to our program)

Step1. Coreference Resolution:
Run the following command:
>Python3 CoreferenceResolveDBV2.py  ./TestCoref2016/April2016/April 15,2016_1.txt  ./TestCoref2016/April2016/preprocess_coref.txt

the data in April 15,2016_1.txt has been preprocessed with coreference resolution and is stored in preprocess_coref.txt

Step2. Splitting the data obtained in previous step into sentences:
Run the following command:
>Python3 tokenize_sent.py ./TestCoref2016/April2016/preprocess_coref.txt ./TestCoref2016/April2016/input_to_stuffie.csv

input_to_stuffie.csv will be generated in which each row corresponds to a single sentence.

Step4. This csv will be input to the stuffie.






Step5. Creation of DB in Neo4j:






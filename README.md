Process of Extracting events from News Articles:
Download the project as zip file , unzip it and open the command prompt 
Run following commands one by one.
Step1. For the demo purpose, we have a file named "raw_data.txt" which contains a news article with the headline as first line and content(input to our program):

Step2. Coreference Resolution:
Run the following command:
>Python3 sPacy.py raw_data.txt preprocess_coref.txt

the data in raw_data.txt has been preprocessed with coreference resolution and is stored in preprocess.txt

Step3. Splitting the data into sentences:
Run the following command:
>Python3 split_data.py preprocess_coref.txt input_to_stuffie.csv

input_to_stuffie.csv will be generated in which each row corresponds to a single sentence.

Step4. This csv will be input to the stuffie.





Step5. 






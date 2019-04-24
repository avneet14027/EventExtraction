## Process of Extracting events from News Articles(A demo on processing of data at different phase):

Download the project as zip file , unzip it and open the command prompt inside "/EventExtraction-new-b/Preprocess". 

Run following commands one by one.
For the demo purpose, we have a file named "/Preprocess/TestCoref2016/April2016/April 15,2016_1.txt" which contains a news article with the headline as first line and content(input to our program)

Further, running this project would require the following to be set up beforehand. 
 - HeidelTime Standalone Version 2.2.1 
 - Neo4j Desktop Version 1.1.17
 - Spacy library Python.
 - Stanford CoreNLP
 - NeuralCoref 4.0 for coreference resolution. 

#### Step1. Coreference Resolution:
Run the following command:
>python3 CoreferenceResolveDBV2.py  ./TestCoref2016/April2016/April 15,2016_1.txt  ./TestCoref2016/April2016/preprocess_coref.txt

the data in April 15,2016_1.txt has been preprocessed with coreference resolution and is stored in preprocess_coref.txt

#### Step2. Splitting the data obtained in previous step into sentences:
Run the following command:
> python3 tokenize_sent.py ./TestCoref2016/April2016/preprocess_coref.txt ./TestCoref2016/April2016/input_to_stuffie.csv

input_to_stuffie.csv will be generated in which each row corresponds to a single sentence.

#### Step4. This csv will be input to the stuffie.
Place the csv file in DBTest folder. Update 'orig_file', 'writefile' , and 'folder' path in the main function in StuffieConsoleRunner.java file according to your workspace. Also update, HeidelTime config.props file path in main function of the same file.

#### Step5. Start Neo4j Database.
Create an empty graph database indtance  in Neo4j Desktop Version 1.1.17 . Start the database instace. Update the authorization credentials in StuffieConsoleRunner.java file in the main function, in AccessDatabase function call. 
Finally run the StuffieConsoleRunner.java file. This will start creating the database.

#### Step6. Querying the database
Run QueryDB.java file to test a few queries. Alternatively, run the queries using Neo4j browser.

Sample Outputs have been generated and added to the SampleOutputs folder for the query keywords "death, strike, protest, bomb meet, stone pelt, pellet". 

### Citations:
StuffIE: Semantic Tagging of Unlabeled Facets Using Fine-Grained Information Extraction. 

 @misc{lia_corrales_2015_15991,
        author       = {Lia Corrales},
        title        = {{dust: Calculate the intensity of dust scattering halos in the X-ray}},
        month        = mar,
        year         = 2015,
        doi          = {10.5281/zenodo.15991},
        version      = {1.0},
        publisher    = {Zenodo},
        url          = {https://doi.org/10.5281/zenodo.15991}
        }


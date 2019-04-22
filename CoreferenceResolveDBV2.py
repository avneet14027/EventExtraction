import spacy
import os
from os import listdir
from os.path import isfile, join
import io
import codecs
import os
import neuralcoref
import sys
nlp = spacy.load('en')

neuralcoref.add_to_pipe(nlp)

def process_dirs(mypath,temp_path):
    subdirs = [x[0] for x in os.walk(path)]
    subdirs = subdirs[1:]
    for mypath in subdirs:
        onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]
        print(mypath)
        for filename in onlyfiles:
            process_file(filename,mypath,temp_path)
    print(onlyfiles)
    print(subdirs)

def process_file(filename,mypath,temp_path):
    global nlp
    
    temp = filename.split("_")
    print(temp)
    date_str = temp[0]
    #print(date_str)
    date_obj = datetime.datetime.strptime(date_str, '%B %m, %Y')
    new_date = date_obj.date().strftime('%d-%m-%Y')
    new_filename = new_date + "_" + temp[1]
    
    month = mypath.split("\\")[-1]
    #write_path = "C:\\Users\\Reen\\Desktop\\OutCoref2016" + "\\" + month + "\\" + new_filename
    directory = temp_path + "\\" + month
    if not os.path.exists(directory):
        os.makedirs(directory)
    write_path = temp_path + "\\" + month + "\\" + new_filename
    file_read = mypath + "\\" + filename
    print(file_read)
    with open(file_read, 'r',encoding="utf8") as file:
        data = file.read()
        content = data.split("\n\t")
        #print(content)
        headline = content[0]
        body = content[1]
        #print(body)
        doc = nlp(body)
        coref_body = doc._.coref_resolved
        #print(coref_body)
        data_to_write = headline + "\n\n" + coref_body
        write_to_file(write_path,data_to_write)
    file.close()

def write_to_file(filename,content):
    with open(filename, 'w+') as the_file:
        the_file.write(content)
    the_file.close()

if __name__ == "__main__":
	test_core_path = sys.argv[1]
	out_core_path = sys.argv[2]
    process_dirs(test_core_path,out_core_path)




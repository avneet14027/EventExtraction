from nltk.tokenize.punkt import PunktSentenceTokenizer, PunktParameters
from os import listdir
from os.path import isfile, join
import re
import csv
import io
import sys

def split_sentences_V2(text):
	regex = "(?<!\w\.\w.)(?<![A-Z][a-z]\.)(?<=\.|\?)\s"
	split_text = re.compile(regex).split(text)
	for i in split_text:
		i.strip()
		#print(i)
	return(split_text)

def split_sentence(text):
	punkt_param = PunktParameters()
	abbreviation = ['dr', 'mr', 'ms', 's.a.s', 'n.n', 'rs.']
	punkt_param.abbrev_types = set(abbreviation)
	tokenizer = PunktSentenceTokenizer(punkt_param)
	split_text = tokenizer.tokenize(text)
	#for i in split_text:
		#print(i)
	return(split_text)

def read_files(mypath,csvfile_path):
	onlyfiles = [join(mypath, f) for f in listdir(mypath) if isfile(join(mypath, f))]
	sorted_files = sorted(onlyfiles)
	#print(sorted_files)
	for f in sorted_files:
		print(f)
		content = ''
		with open(f,'r') as filename:
			content = filename.read()
		date = ((f.split(mypath))[1]).replace('/','')
		date = date.split('_')[0]
		body = content.split('\n\n')
		text = body[1]
		headline = body[0]
		#print("*******************",date,"\n\n\n")
		V1 = split_sentence(text)
		#V2 = split_sentences_V2(text)
		#V1.sort()
		#V2.sort()
		#print(set(V1).intersection(set(V2)))
		write_to_CSV(date,headline,V1,csvfile_path)


def write_to_CSV(date,headline,sentences,csvfile_path):
	with io.open(csvfile_path, mode='a', encoding="utf8") as csv_file:
		writer = csv.writer(csv_file)
		for i in sentences:
			if i!="":
				i = i.strip()
				row = [date,headline,i]
				writer.writerow(row)
	csv_file.close()

if __name__ == "__main__":
	textfiles_path = sys.argv[1]
	csvfile_path = sys.argv[2]
	read_files(textfiles_path,csvfile_path)






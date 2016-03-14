#!/bin/bash

TYPE=$1
INPUT_PATH=$2
INPUT_PATH_RECORD_ALIGNMENTS=$3
OUTPUT_FILE=$4
OUTPUT_FILE_ALIGNMENTS=$5

/usr/bin/java -cp dist/Generator.jar:dist/lib/Helper.jar induction.utils.ExportExamplesToEdusFile ${TYPE} ${INPUT_PATH} ${INPUT_PATH_RECORD_ALIGNMENTS} ${OUTPUT_FILE} ${OUTPUT_FILE_ALIGNMENTS}

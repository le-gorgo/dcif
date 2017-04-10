#!/bin/bash

METHOD=(SOLAR-Inc Solar-Carc Solar)

METHOD2=(DICF-PB-Star DICF-PB-Star-FixedRoot-1 DICF-PB-Async DICF-PB-NewAsync)

METHOD4=(DICF-PB-Star DICF-PB-NewAsync DICF-PB-Star-FixedRoot-1 DICF-PB-Star-FixedRoot-2 DICF-PB-Token DICF-PB-Async DICF-PB-Token-FixedOrder-2-1-0 DICF-PB-Token-FixedOrder-2-1-0 DICF-PB-Token-FixedOrder-1-0-2 DICF-PB-Token-FixedOrder-0-2-1 DICF-PB-Token-FixedOrder-1-2-0 DICF-PB-Token-FixedOrder-2-0-1 DICF-PB-Token-FixedOrder-3-1-2 ) 
 
 

METHOD6=(DICF-PB-Async DICF-PB-NewAsync)

METHOD8=(DICF-PB-Async DICF-PB-NewAsync DICF-PB-Star DICF-PB-Star-FixedRoot-1 DICF-PB-Star-FixedRoot-2 DICF-PB-Star-FixedRoot-3 DICF-PB-Star-FixedRoot-4 DICF-PB-Star-FixedRoot-5 DICF-PB-Star-FixedRoot-6 DICF-PB-Star-FixedRoot-7 DICF-PB-Token DICF-PB-Async DICF-PB-Token-FixedOrder-7-6-5-4-3-2-1-0 DICF-PB-Token-FixedOrder-4-5-2-1-0-3-6-7 DICF-PB-Token-FixedOrder-1-6-7-0-4-5-3-2 DICF-PB-Token-FixedOrder-5-0-3-6-7-2-1-4 DICF-PB-Token-FixedOrder-6-1-5-4-2-7-3-0 DICF-PB-Token-FixedOrder-4-2-3-5-0-1 DICF-PB-Token-FixedOrder-3-5-4-0-1-2 ) 


fileN="RNG006-2.C1i2"

for met in ${METHOD[*]} 
do

		java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar -verbose  -method=$met -var=_all_ld-1-4 -t=600000 $fileN.sol $fileN.csv 
		echo "######################################"	&>> LOG_File.log	
done

for met in ${METHOD2[*]} 
do

		java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar -verbose  -method=$met -var=_all_ld-1-4 -dist=_kmet2 -t=600000 $fileN.sol $fileN.csv 
		echo "######################################"	&>> LOG_File.log	
done

for met in ${METHOD4[*]} 
do

		java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar   -method=$met -var=_all_ld-1-4 -dist=_kmet4 -t=600000 $fileN.sol $fileN.csv 
		echo "######################################"	&>> LOG_File.log
done


#for met in ${METHOD6[*]} 
#do

		#java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar  -method=$met -var=_all_ld-1--1 -dist=_kmet6 -t=600000 $fileN.sol $fileN.csv 
		#echo "######################################"	&>> LOG_File.log
#done


for met in ${METHOD8[*]} 
do

		java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar   -method=$met -var=_all_ld-1-4 -dist=_kmet8 -t=600000 $fileN.sol $fileN.csv 
		echo "######################################"	&>> LOG_File.log
done

echo "Dooone"

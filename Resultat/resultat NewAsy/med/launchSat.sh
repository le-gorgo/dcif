#!/bin/bash

METHOD=(SOLAR-Inc Solar-Carc)

METHOD4=(DICF-PB-Star-FixedRoot-2 DICF-PB-NewAsync  DICF-PB-Star-FixedRoot-3 DICF-PB-Token DICF-PB-Token-FixedOrder-3-2-1-0 DICF-PB-Token-FixedOrder-2-1-0-3 DICF-PB-Token-FixedOrder-1-0-3-2 DICF-PB-Token-FixedOrder-0-3-2-1 DICF-PB-Token-FixedOrder-1-2-3-0 DICF-PB-Token-FixedOrder-2-3-0-1 DICF-PB-Token-FixedOrder-3-0-1-2 ) #DICF-PB-Star DICF-PB-Star-FixedRoot-1
#
METHOD8=(DICF-PB-Star DICF-PB-NewAsync DICF-PB-Star-FixedRoot-1 DICF-PB-Star-FixedRoot-2 DICF-PB-Star-FixedRoot-3 DICF-PB-Star-FixedRoot-4 DICF-PB-Star-FixedRoot-5 DICF-PB-Star-FixedRoot-6 DICF-PB-Star-FixedRoot-7 DICF-PB-Token DICF-PB-Async DICF-PB-Token-FixedOrder-7-6-5-4-3-2-1-0 DICF-PB-Token-FixedOrder-4-5-2-1-0-2-2-7 DICF-PB-Token-FixedOrder-1-6-7-0-4-5-3-2 DICF-PB-Token-FixedOrder-5-0-2-2-7-2-1-4 DICF-PB-Token-FixedOrder-6-1-5-4-2-7-3-0 DICF-PB-Token-FixedOrder-4-2-3-5-0-1 DICF-PB-Token-FixedOrder-3-5-4-0-1-2 ) 

METHOD16=(DICF-PB-Star DICF-PB-NewAsync DICF-PB-Star-FixedRoot-1 DICF-PB-Star-FixedRoot-2 DICF-PB-Star-FixedRoot-3 DICF-PB-Star-FixedRoot-4 DICF-PB-Star-FixedRoot-5 DICF-PB-Star-FixedRoot-6 DICF-PB-Star-FixedRoot-7 DICF-PB-Star-FixedRoot-8 DICF-PB-Star-FixedRoot-9 DICF-PB-Star-FixedRoot-10 DICF-PB-Star-FixedRoot-11 DICF-PB-Star-FixedRoot-12 DICF-PB-Star-FixedRoot-13 DICF-PB-Star-FixedRoot-14 DICF-PB-Star-FixedRoot-15 DICF-PB-Token DICF-PB-Async) 

m='medium_TPnaiveshortdist_per5_seuil1'

#for met in ${METHOD[*]}
#do 	 
	#java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar -verbose -method=$met -var=_all_ld2-2 -t=600000 $m.sol $m.csv &>> logFile_$met.log
#done

for met in ${METHOD16[*]}
do 	 
	java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar -method=$met -var=_all_ld2-2 -dist=_kmet16 -t=600000 $m.sol $m.csv &>> logFile16_all_ld2-2_$met.log
done

for met in ${METHOD4[*]}
do 	 
	java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar -method=$met -var=_all_ld2-2 -dist=_kmet4 -t=600000 $m.sol $m.csv &>> logFile4_all_ld2-2_$met.log
done

for met in ${METHOD8[*]}
do 	 
	java -d64 -Xms512m -Xmx8g -jar CFLauncher.jar  -method=$met -var=_all_ld2-2 -dist=_kmet8 -t=600000 $m.sol $m.csv &>> logFile8_all_ld2-2_$met.log
done

echo "finish"

CC = gcc
ALLFILES = assocarr.o cmd_line.o discreterv.o \
	   matrix.o misc.o \
	   Random_Number.o str.o \
           genSamples.o extractFeatures.o readText.o
ALLHEADERS = assocarr.h cmd_line.h discreterv.h \
	     dym_arr.h matrix.h misc.h \
	     Random_Number.h str.h \
             genSamples.o extractFeatures.h readText.h
LDFLAGS = 
CCFLAGS = -Ilib -O
LIBRARY = -lm

#---------------------------------------------------------------

foo: extractFeatures.C readText.h $(ALLFILES) $(ALLHEADERS)
	$(CC) $(CCFLAGS) $(LDFLAGS) $(ALLFILES) -o foo $(LIBRARY)

Target: trainClassifier

extractFeatures.o: assocarr.h extractFeatures.C extractFeatures.h \
			matrix.h misc.h readText.h
	$(CC) $(CCFLAGS) -c extractFeatures.C

genSamples.o: genSamples.C genSamples.h extractFeatures.h \
		assocarr.h dym_arr.h matrix.h misc.h readText.h
	$(CC) $(CCFLAGS) -c genSamples.C

readText.o: assocarr.h readText.C readText.h dym_arr.h str.h misc.h
	$(CC) $(CCFLAGS) -c readText.C

trainClassifier: trainClassifier.C readText.h $(ALLFILES) $(ALLHEADERS) Makefile
	$(CC) $(CCFLAGS) $(LDFLAGS) trainClassifier.C $(ALLFILES) \
		-o trainClassifier $(LIBRARY)







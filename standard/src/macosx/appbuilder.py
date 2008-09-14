#!/usr/bin/env python
import sys, os, shutil, string

# Set up variables --- note, must be run from the root CVS directory!
shortname = sys.argv[1]
classname = sys.argv[2]
classpath = string.replace(sys.argv[3], os.getcwd(), '$JAVAROOT')
libpath = string.replace(sys.argv[4], os.getcwd(), '$JAVAROOT');
APP_PATH = shortname + '.app'
JAVAROOT = APP_PATH + '/Contents/Resources/Java'
PROPS = {}
for index in range(5, len(sys.argv), 2):
	PROPS[sys.argv[index]] = sys.argv[index+1]

# Create the app skeleton
if(os.access(APP_PATH, os.F_OK)):
	shutil.rmtree(APP_PATH)
os.makedirs(APP_PATH + '/Contents/MacOS')
os.makedirs(JAVAROOT)
f=open(APP_PATH + '/Contents/PkgInfo', 'w'); f.write('APPLHayS'); f.close()
shutil.copy2('/System/Library/Frameworks/JavaVM.framework/Resources/MacOS/JavaApplicationStub',
			 APP_PATH+'/Contents/MacOS/'+shortname)
#os.chmod(APP_PATH + '/Contents/MacOS/' + shortname, 555)

# Copy in the code files
os.makedirs(JAVAROOT + '/lib')
os.makedirs(JAVAROOT + '/src')
for x in ['lib/java', 'lib/macosx', 'src/adenine', 'src/resource', 'out', 'jython']:
	shutil.copytree(x, JAVAROOT + '/' + x)

# Create the plist file
f=open('src/macosx/Info.plist', 'r'); info = f.read(); f.close()
info = string.replace(info, 'REPLACE_SHORTNAME', shortname)
info = string.replace(info, 'REPLACE_CLASSNAME', classname)
info = string.replace(info, 'REPLACE_CLASSPATH', classpath)
info = string.replace(info, 'REPLACE_LIBPATH', libpath)
index = 1; 
for key, value in PROPS.items():
	info = string.replace(info, 'REPLACE_PROP'+str(index), key, 1)
	info = string.replace(info, 'REPLACE_VAL', value, 1)
	index = index + 1
f=open(APP_PATH + '/Contents/Info.plist', 'w'); f.write(info); f.close()

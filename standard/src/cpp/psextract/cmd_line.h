/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

#ifndef _CMD_LINE_H
#define _CMD_LINE_H
#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#define MAX_FLAG_SIZE 64

class cmd_line
  {
  private:
        int argc;
        char **argv;
	int *arg_used;
	char *cmd_line_string;

  public:
		int flag_position(const char* flag) const;
		int get_flag_pos(const char* flag) const {return flag_position(flag);}
		int exist_flag(const char *in_flag) const;
		float get_flag_arg(const char *in_flag, float dfault, int offset=0);
		double get_flag_arg(const char *in_flag, double dfault, int offset=0);
		long get_flag_arg(const char *in_flag, long dfault, int offset=0);
		const char *get_flag_arg(const char *in_flag, const char *dfault, int offset=0);
		const char *cmd_line_str(void) {return cmd_line_string;}
		void assert_all_parms_used(void) const;
		int get_flag_parm(const char *in_flag, int nterms, ...) const;

		cmd_line(int argc, char *argv[]);
		cmd_line::~cmd_line(void);
  };

/*
BUGS: 

1. get_flag_parm() doesn't work

*/

#endif		// _CMD_LINE_H

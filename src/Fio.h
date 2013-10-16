//
//  Fio.h
//
//  Created by Jan Sten Adamek && Vojtech Micka on 2.10.13.
//  Copyright (c) 2013 All rights reserved.
//

#ifndef __Fio__Fio__
#define __Fio__Fio__

#include <sys/types.h>

class Fio {
    enum { B_SIZE = 4096 }; // Buffer size

    int _fileDescriptor;
    char _buffer[B_SIZE];
    char _stopper;          // If the buffer is full, the NULL to stop reading goes here
    char * _ptr;            // Pointer to next byte in buffer to read
    ssize_t _gcount;        // Length of data in buffer
    bool _fileFinish;
    bool _finish;
    
public:
    Fio(int fileDescriptor);
    ~Fio();
    
    bool isFileFinish() {return _fileFinish;};
    bool isFinish() {return _finish;};
    
    char * nextWord();
    bool nextLine();
    bool nextLine(char *&line);
    
    long nextLong();
    int nextInt();
    
private:
    void _readToBuffer();
    long _parseLong(char *&ptr);
    bool _parseLine(char *&ptr);
};

#endif /* defined(__Fio__Fio__) */

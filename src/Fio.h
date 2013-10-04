//
//  Fio.h
//  HadgehogSP
//
//  Created by Vojtech Micka on 2.10.13.
//  Copyright (c) 2013 Aisacorp. All rights reserved.
//

#ifndef __Fio__Fio__
#define __Fio__Fio__

#include <iostream>

using namespace std;

#define B_SIZE 4096

class Fio {
    int _fileDescriptor;
    char _buffer[B_SIZE];
    char _stopper;
    char * _ptr;
    ssize_t _gcount;
    bool _fileFinish;
    bool _finish;
    
public:
    Fio(int fileDescriptor);
    ~Fio();
    
    long nextLong();
    int nextInt();
    
    bool isFinish() {return _finish;};
    
private:
    void _readToBuffer();
    long _parseLong(char *&ptr);
};

#endif /* defined(__Fio__Fio__) */

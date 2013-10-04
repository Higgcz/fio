//
//  Fio.h
//  HadgehogSP
//
//  Created by Vojtech Micka on 2.10.13.
//  Copyright (c) 2013 Aisacorp. All rights reserved.
//

#ifndef __Fio__Fio__
#define __Fio__Fio__

class Fio {
    static const size_t B_SIZE = 4096;

    int _fileDescriptor;
    char _buffer[B_SIZE];
    char _stopper;          // If the buffer is full, the NULL to stop reading goes here
    char * _ptr;
    ssize_t _gcount;
    bool _fileFinish;
    bool _finish;
    
public:
    Fio(int fileDescriptor);
    ~Fio();
    
    long nextLong();
    int nextInt();
    
    bool isFileFinish() {return _fileFinish;};
    bool isFinish() {return _finish;};
    
private:
    void _readToBuffer();
    long _parseLong(char *&ptr);
};

#endif /* defined(__Fio__Fio__) */

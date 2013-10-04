//
//  Fio.cpp
//  HadgehogSP
//
//  Created by Vojtech Micka on 2.10.13.
//  Copyright (c) 2013 Aisacorp. All rights reserved.
//

#include "Fio.h"
#include <assert.h>
#include <unistd.h>

Fio::Fio(int fileDescriptor):_fileDescriptor(fileDescriptor),_ptr(_buffer + B_SIZE),_gcount(0),_fileFinish(false),_finish(false) {
    _readToBuffer();
}

Fio::~Fio() {
}

void Fio::_readToBuffer() {
    assert(!_finish);
    
    ptrdiff_t left = _buffer + B_SIZE - _ptr;

    if (left > 0) {
        memcpy(_buffer, _ptr, left);
    }
    
    _gcount = read(_fileDescriptor, _buffer + left, B_SIZE - left) + left;
    _ptr = _buffer;
    
    _buffer[_gcount] = 0;
    assert(_buffer[0] != 0);
    
    if (_gcount < B_SIZE) {
        _fileFinish = true;
    }
}

long Fio::_parseLong(char *&ptr) {
    register long n(0);
    register char ch;
    
    do {
        ch = *ptr;
        if (ch == 0) return 0;
        ++ptr;
    } while (ch < '0' || ch > '9');
    
    do {
        n = (ch & 0x0f) + n * 10;
        ch = *ptr;
        ++ptr;
    } while (ch >= '0' && ch <= '9');
    
    return n;
}

int Fio::nextInt() {
    return (int) nextLong();
}

long Fio::nextLong() {

    char * tPtr = _ptr;
    long num = _parseLong(tPtr);
    
    if (tPtr >= _buffer + _gcount) {
        if (_fileFinish)
        {
            _finish = true;
            return 0;
        }
        _readToBuffer();
        num = _parseLong(tPtr);
    }
    
    _ptr = tPtr;
    
    return num;
}
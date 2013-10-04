Fast IO for C++
===

Library, what every students of CTU needs.

## Installation ##

Drag the **Fio** files into your project.

Currently Fio implements only reader with 4 kB buffer and simple ASCII number parser.

## Using Fio ##

First of all, you have to create instance of Fio class. You have to options:

1. Create instance on the stack, which is faster, but depends on your stack size:

```cpp
Fio fio(0); // 0 means standard input
```

2. Create instance on the heap, slower but memory is much more larger:

```cpp
Fio fio = new Fio(0);
```

### Methods ###

Fio currently implements only two methods <code>nextLong()</code> and <code>nextInt()</code>.

Both method returns next number available on the input.
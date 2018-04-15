# What is this and why should I be interested ?

This project was started out of pure boredom and the urge to learn the Kotlin programming language.

It tries to interpret the assembly language used at the KIT's ("Karlsruher Institut fuer Technologie") GBI/TGI courses,
to teach the low level concepts of how computer processors work.
This program is very much a WIP.

If you are a student at said institute, this project might prove to be useful in helping you understand the basic concepts of assembly languages, or to "debug" code that you wrote for an assignment.

# Usage

### Flags
|Flag|Effect|
|----|------|
|-d| starts the interpreter in a sort of "debug" mode in which you can stepp through the code line by line|
|-s| prints a "summary" at the end of execution (Memory view)|
#### Debug mode
|Input|Functionality|
|-----|-------------|
|r,reg,register,registers| prints the contents of the registers|
|d,dbg,debug| prints the contents of the entire memory|
|<Enter>| step (increase the IP by one)|

# Problems
- currently operates with 32bit address sizes instead of 24bit

# TODO
- make things prettier
- implement calls and returns
- write a small handbook / manual explaining the language
- to make usage easier a GUI would help

# Contributions
Contributions are very welcome 



Databend
========

Hello! Databend is a small project I've been working on in hopes of producing some nice [glitch art](http://www.glitch_art.reddit.com). It is mostly based on elements of randomness (as you can see from the code), but you can set certain constants if you want. I'll be adding more operations (and flags) as I go to make it a bit more extensive.

####Usage: <code>$databend "path/to/image.jpg" [operation] [parameters] [flags] </code>

===

##Operations:

###bshift
"Block Shift": This takes a random area of the image, shifts all pixels down <i>and</i> right a random amount, and repeats this a number of times.

####Parameters (required):
<b>iterations</b>: This tells bshift how many different blocks of the image to shift. 

####Flags (optional):
<b>-c</b>: When added, this will apply a random color mask to the affected area.

####Example: <code>$databend "arbitrary/path/img.bmp" bshift 15 -c</code> 

===

###lshift
"Line Shift": This is similar to bshift, only it takes blocks that are the length of the image, and of random height, and shift them only to the right. There is a frequency associated with this operation to determine the number of line shifts per operation. 

####Parameters (required):
none.

####Flags (optional):
<b>-h x</b>: When added, this will limit the height of each line shift area to <b>x</b>px

<b>-f x</b>: When added, this will set the frequency of each line shift operation to <b>x</b>%

<b>-c</b>: When added, this will apply a random color mask to the affected area.

####Example: <code>$databend "arbitrary/path/img.bmp" lshift -f 30 -c</code>
  



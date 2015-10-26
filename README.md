Databend
========

Hello! Databend is a small project I've been working on in hopes of producing some nice [glitch art](http://www.glitch_art.reddit.com). It is mostly based on elements of randomness (as you can see from the code), but you can set certain constants if you want. I'll be adding more operations (and flags) as I go to make it a bit more extensive.
####Example: 
***Original:***

![Original](https://raw.githubusercontent.com/cschlisner/Databend/master/butterfly1.jpg)


***Processed:***

![Processed](https://raw.githubusercontent.com/cschlisner/Databend/master/copy%20-%20butterfly1.jpg)

####Usage: <code>$databend "path/to/image.jpg" "path/to/output.jpg" [operation] [parameters] [flags] </code>

===

##Operations:

###bshift
"Block Shift": This takes a random area of the image, shifts all pixels down <i>and</i> right a random amount, and repeats this a number of times.

####Parameters (required):
<b>iterations</b>: This tells bshift how many different blocks of the image to shift. 

####Flags (optional):
<b>-c</b>: When added, this will apply a random color mask to the affected area.

####Example: <code>$databend "img.bmp" "out.bmp" bshift 15 -c</code> 

===

###lshift
"Line Shift": This is similar to bshift, only it takes blocks that are the length of the image, and of random height, and shifts them only to the right. There is a frequency associated with this operation to determine the number of line shifts per operation. 

####Parameters (required):
none.

####Flags (optional):
<b>-h x</b>: When added, this will limit the height of each line shift area to <b>x</b>px

<b>-f x</b>: When added, this will set the frequency of each line shift operation to <b>x</b>%

<b>-c</b>: When added, this will apply a random color mask to the affected area.

####Example: <code>$databend "img.bmp" "out.bmp" lshift -f 30 -c</code>
  
===

###psort
"Pixel Sort": This function sorts each pixel in each row based on the color data of each pixel. By default, it will sort based on the average color ((r+g+b) / 3) to the left, meaning that the lower average colors (darker) will be pushed to the left leaving the higher average colors (brighter) to the right. 

####Parameters (required):
none.

####Flags (optional):
<b>-d</b>: When added, this will change the sorting direction from left to right.

<b>-r</b>: When added, this will sort based on the red value of each pixel.

<b>-g</b>: When added, this will sort based on the green value of each pixel.

<b>-b</b>: When added, this will sort based on the blue value of each pixel.

#####<i>Note: any two of the color flags can be combined to sort on an average of the two, leaving out the third color from affecting the sort. All three flags enabled is equivalent to the default function.</i>

####Example: <code>$databend "img.bmp" "out.bmp" psort -d -b -g</code>


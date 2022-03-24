Databend
========

Hello! Databend is a small project I've been working on in hopes of producing some nice glitch art. An image can be modified with this program with a very general level of control by the user. So while the output of two identical executions will never be the same, a user can specify which qualities the two resulting images should share. 

The program is controlled by a series of operations and flags. The operations are performed on the image in the order they are specified in the command. 

This means that the command ```$ java ... bshift 5 esort``` will output something completely different than the command ```$ java ... esort bshift 5```, while still sharing the same visual elements. 

#### Example: 
***Original:***

![Original](https://raw.githubusercontent.com/cschlisner/Databend/master/Examples/Original.jpg)


***Processed:***

![Processed](https://raw.githubusercontent.com/cschlisner/Databend/master/Examples/Processed.jpg)

#### Usage: <code>$databend "path/to/image.jpg" "path/to/output.jpg" [operation] [parameters] [flags] </code>

===

## Operations:

### bshift
"Block Shift": This takes a random area of the image, shifts all pixels down <i>and</i> right a random amount, and repeats this a number of times.

#### Parameters (required):
<b>iterations</b>: This tells bshift how many different blocks of the image to shift. 
[Example: bshift 7](https://github.com/cschlisner/Databend/blob/master/Examples/bshift/7.jpg)

#### Flags (optional):
<b>-c</b>: When added, this will apply a random color mask to the affected area.
[Example: bshift 15 -c](https://github.com/cschlisner/Databend/blob/master/Examples/bshift/15-c.jpg)
#### Example: <code>$databend "img.bmp" "out.bmp" bshift 15 -c</code> 

===

### esort
"Edge Sort": This operation will detect edges in the image according to an optionally specified sensitivity variable and sort pixels preceding the edges by a certain (optionally specified) distance. A color mask may also be applied after the sorting.  

#### Parameters (required):
none.

#### Flags (optional):
<b>-l x</b>: When added, this will set the length of each sorted area to <b>x</b>% of the total image width - Default is 8%. 
[Example: esort -s 0.1 -l 10 -c](https://github.com/cschlisner/Databend/blob/master/Examples/esort/-s0.1-l10-c.jpg)

<b>-s x</b>: When added, this will set the specificity of the edge detection to <b>x</b> - Default is 1.1. Higher values will detect less edges, while lower values will detect more. This may need adjustment for darker images or images with vauge edges. 
[Example: esort -s 0.1](https://github.com/cschlisner/Databend/blob/master/Examples/esort/-s0.1.jpg)

<b>-c x</b>: When added, this will apply a random color mask with an error of <b>x</b>%, this allows limiting of the "random colors" applied to match the image colors. 
[Example: esort -s 0.1 -l 10 -c](https://github.com/cschlisner/Databend/blob/master/Examples/esort/-s0.1-l10-c.jpg)

#### Example: <code>$databend "img.bmp" "out.bmp" esort -l 10</code>
  
===

### psort
"Pixel Sort": This function sorts each pixel in each row based on the color data of each pixel. By default, it will sort based on the average color ((r+g+b) / 3) to the left, meaning that the lower average colors (darker) will be pushed to the left leaving the higher average colors (brighter) to the right. 

#### Parameters (required):
none.

#### Flags (optional):
<b>-d</b>: When added, this will change the sorting direction from left to right.
[Example: psort -b -r -d](https://github.com/cschlisner/Databend/blob/master/Examples/psort/-b-r-d.jpg)

<b>-r</b>: When added, this will sort based on the red value of each pixel.
[Example: psort -r](https://github.com/cschlisner/Databend/blob/master/Examples/psort/-r.jpg)

<b>-g</b>: When added, this will sort based on the green value of each pixel.
[Example: psort -g](https://github.com/cschlisner/Databend/blob/master/Examples/psort/-g.jpg)

<b>-b</b>: When added, this will sort based on the blue value of each pixel.
[Example: psort -b](https://github.com/cschlisner/Databend/blob/master/Examples/psort/-b.jpg)

##### <i>Note: any two of the color flags can be combined to sort on an average of the two, leaving out the third color from affecting the sort. All three flags enabled is equivalent to the default function.</i>

#### Example: <code>$databend "img.bmp" "out.bmp" psort -d -b -g</code>


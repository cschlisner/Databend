import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.Random;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

class databend {
	public static void main(String[] args){
		File file = new File(args[0]);
		try{

			System.out.println("Loading file... ");
			BufferedImage image = ImageIO.read(file);
			BufferedImage newImg = image;
			System.out.println("Working...");

			for (int i=2; i<args.length; ++i){
				if (args[i].equals("bshift")){
					if (i+1>=args.length){
						System.out.println("bshift needs an iteration parameter!");
						return;
					}
					boolean bcol = false;

					if (i+2<args.length)
						if (args[i+2].equals("-c")) bcol = true;
					
					System.out.format("blockshift n=%s col::%s | ", Integer.valueOf(args[i+1]), bcol);
					long t = System.currentTimeMillis();
					newImg = blockShifter(newImg, Integer.valueOf(args[i+1]), bcol);
					System.out.println((System.currentTimeMillis() - t) + "ms");
				}
				else if (args[i].equals("psort")){
					boolean r = false, g = false, b = false, d = false;
					for (int j=i+1; j<args.length; ++j){
						if (!args[j].contains("-"))
							break;
						else if (args[j].equals("-r")) r=true;
						else if (args[j].equals("-g")) g=true;
						else if (args[j].equals("-b")) b=true;
						else if (args[j].equals("-d")) d=true;
					}
					System.out.format("pixelsort red::%s green::%s blue::%s reversed::%s | ", r, g, b, d);
					long t = System.currentTimeMillis();
					newImg = pixelSorter(newImg,d,r,g,b);
					System.out.println((System.currentTimeMillis() - t) + "ms");
				}
				else if (args[i].equals("esort")){
					int trailLen = newImg.getWidth()/8, escol = -1; 
					Double spec = 1.1;
					for (int j=i+1; j<=i+6; j+=2){
						if (j+1<args.length){
							if (!args[j].contains("-"))
								break;
							if (args[j].equals("-c")) escol = Integer.valueOf(args[j+1]);
							else if (args[j].equals("-l")) trailLen = (int)((float)newImg.getWidth()*(Double.valueOf(args[j+1])/100.0f));
							else if (args[j].equals("-s")) spec = Double.valueOf(args[j+1]);
						}
					
					}
					System.out.format("edgesort col::%s len::%s spec::%s | ", escol, trailLen, spec);
					long t = System.currentTimeMillis();
					newImg = edgeSort(newImg, trailLen, spec, escol);
					System.out.println((System.currentTimeMillis() - t) + "ms");
				}
				else if (args[i].equals("osort")){
					Double spec = 1.1;
					int rand = -1;
					for (int j=i+1; j<=i+4; j+=2){
						if (j<args.length){
							if (!args[j].contains("-"))
								break;
							else if (args[j].equals("-s")) spec = Double.valueOf(args[j+1]);
							else if (args[j].equals("-r")) rand = Integer.valueOf(args[j+1]);
						}
					
					}
					System.out.format("objectsort spec::%s | ", spec);
					long t = System.currentTimeMillis();
					newImg = objectSort(newImg, spec, rand);
					System.out.println((System.currentTimeMillis() - t) + "ms");
				}
			}
			ImageIO.write(newImg, "jpg", new File(args[1]));	
			System.out.println("Done.");					  	   
		} catch (IOException e){
				System.out.println("Error. Check filename.");
				return;
		}
	}

	public static BufferedImage blockShifter(BufferedImage img, int n, boolean colored){
		int progInc = (n<10)?10/n:1;
		for (int i=0; i<n; ++i){
			img = pixelShift(img, -1,-1,-1,-1,-1,-1, colored);
		}
		return img;
	}

	public static BufferedImage pixelSorter(BufferedImage img, boolean d, boolean r, boolean g, boolean b){
		return pixelSorter(img, 0, 0, img.getWidth(), img.getHeight(), d, r, g, b);
	}

	public static BufferedImage pixelSorter(BufferedImage img, int startX, int startY, 
											int endX, int endY, boolean d, boolean r, boolean g, boolean b){
		// If no color flags were set, use them all
		if (!r&&!g&&!b){
			r = !r;
			g = !g;
			b = !b;
		}

		int width = endX-startX, height = endY-startY;
		int[] colordata = null;
		try {
			colordata = img.getRGB(startX, startY, width, height, null, 0, width); 
		} catch(Exception e){
			System.out.println(String.format("startX: %s | startY: %s | endX: %s | endY: %s", startX, startY, endX, endY));
		}
		int[] rowdata = new int[width];
		int[] averages = new int[width];
		//System.out.print("psort: [");
		for (int y = 0; y<height-1; ++y){
			// progress bar
			//System.out.print(((float)(((float)y/(float)height)/0.1) % 1 == 0)?".":"");

			// if direction is reversed fill in array from right, else fill in array from left
			for (int i=((d)?width-1:0); i!=((d)?0:width-1); i+=((d)?-1:1)){
				try {
					rowdata[((d)?(width-1)-i:i)] = colordata[y*width+(i)];
				} catch (Exception e){
					System.out.println("y = "+y+" i = "+i);
					e.printStackTrace();
				}
			}

			for (int i=0; i<width; ++i){
				Color c = new Color(rowdata[i]);
				// only use the average of the enabled colors
				averages[i] = (((r)?c.getRed():0)+((g)?c.getGreen():0)+((b)?c.getBlue():0))/(((r)?1:0)+((g)?1:0)+((b)?1:0));
			}

			// sorts the rowdata[] based on the values in the averages[]
			quickSort(averages, 0, averages.length-1, rowdata);
			// need to put the values back in the same way we took them
			for (int i=((d)?width-1:0); i!=((d)?0:width); i+=((d)?-1:1))
				 colordata[y*width+i] = rowdata[((d)?(width-1)-i:i)];
		}
		//System.out.println("]");
		img.setRGB(startX, startY, width, height, colordata, 0, width);
		return img;
	}

	public static BufferedImage objectSort(BufferedImage img, double specificity, int rand){
		int[][] edges = edgeDetector(img, specificity);
		
		// pairs of pixels across object boundaries
		// boundPair[Ycord] = {firstX, lastX}
		int[][] boundPairs = new int[img.getHeight()][2];

		for (int[] e : edges){
			// first X cord is unset
			if (boundPairs[e[1]][0] == 0 || e[0] < boundPairs[e[1]][0])
				boundPairs[e[1]][0] = e[0];
			//second X cord needs to be greatest
			else if (boundPairs[e[1]][1] == 0 || e[0] > boundPairs[e[1]][1])
				boundPairs[e[1]][1] = e[0];
		}
		Random r = new Random();
		for (int[] pair : boundPairs){
			if (rand > -1)
				pair[1] = r.nextInt(rand);
			if (pair[1] < pair[0]){
				int t = pair[0];
				pair[0] = pair[1];
				pair[1] = t;
				
			}

		}

		for (int i = 0; i < boundPairs.length; ++i){
			if (boundPairs[i][0] > 0 && (i - 2) > 0){
				img = pixelSorter(img, boundPairs[i][0], i-2, boundPairs[i][1], i, false, false, false, false);
			}
			
		}

		return img;
	}

	public static BufferedImage edgeSort(BufferedImage img, int trailLen, double specificity, int col){
		int[][] edges = edgeDetector(img, specificity);

		for (int[] e : edges){
			int startX = (e[0] - trailLen) > 0 ? e[0]-trailLen : 0;

			if (e[0] > 0 && (e[1] - 2) > 0){
				img = pixelSorter(img, startX, e[1]-2, e[0], e[1], false, false, false, false);
				if (col != -1)
					img = mimicColors(img, startX, e[1]-2, e[0], e[1], col);
			}
			
		}

		return img;
	}

	public static BufferedImage adjustColors(BufferedImage img, int startX, int startY, 
											int endX, int endY, int r, int g, int b, int a, int density, int similarity){
		
		Random rand = new Random();
		// if dimensions are not explicitly set, select random ones
		startY = (startY!=-1)?startY:rand.nextInt(img.getHeight()-10);
		endY = (endY!=-1)?endY:startY+rand.nextInt(img.getHeight()-startY);
		startX = (startX!=-1)?startX:rand.nextInt(img.getWidth()-10);
		endX = (endX!=-1)?endX:startX+rand.nextInt(img.getWidth()-startX);

		// if colors are not explicitly set, select random ones
		r = (r!=-1)?r:rand.nextInt(256);
		g = (g!=-1)?g:rand.nextInt(256);
		b = (b!=-1)?b:rand.nextInt(256);
		a = (a!=-1)?a:255;

		// density needs to be at least 2 for image to be visible
		density = (density!=-1)?density:rand.nextInt(5)+2;
		int width = endX-startX, height = endY-startY;
    	int[] colordata = img.getRGB(startX, startY, width, height, null, 0, width); 

    	
    	// set colors to match average image colors within range of similarity
    	int avgR=-1, avgG=-1, avgB=-1, avgA=-1;
    	if (similarity > 0 && colordata.length > 0){
    		avgR=1;
    		avgG=1;
    		avgB=1;
    		avgA=1;
    		for (int c : colordata){
    			Color col = new Color(c);
    			avgR += col.getRed();
    			avgG += col.getGreen();
    			avgB += col.getBlue();
    			avgA += col.getAlpha();
    		}
    		avgR /= colordata.length;
    		avgG /= colordata.length;
    		avgB /= colordata.length;
    		avgA /= colordata.length;
    	}

		int[] res = new int[colordata.length];
		for (int i=0; i<colordata.length; i+=density){
			// generates a new color for each pixel
			int nr = (r!=-1)?r:(similarity!=-1)?(rand.nextInt(similarity*2)-similarity+avgR):rand.nextInt(256);
			int ng = (g!=-1)?g:(similarity!=-1)?(rand.nextInt(similarity*2)-similarity+avgG):rand.nextInt(256);
			int nb = (b!=-1)?b:(similarity!=-1)?(rand.nextInt(similarity*2)-similarity+avgB):rand.nextInt(256);
			Color color = new Color(colordata[i]);
			Color newColor = new Color(
				(0<=(color.getRed()+nr)  &&(color.getRed()   +nr)<=255)? color.getRed()  +nr:(nr>0)?255:0, 
			    (0<=(color.getGreen()+ng)&&(color.getGreen() +ng)<=255)? color.getGreen()+ng:(ng>0)?255:0,
			    (0<=(color.getBlue()+nb) &&(color.getBlue()  +nb)<=255)? color.getBlue() +nb:(nb>0)?255:0,
			    0
				);
			// Too light of colors create a bad lighting effect
			if (color.getRed()+color.getGreen()+color.getBlue()>=550) newColor = newColor.darker();
			if (newColor.getRed() == newColor.getGreen() && newColor.getGreen() == newColor.getBlue())
				newColor = new Color(nr, ng, nb, 255);
			res[i] = newColor.getRGB();
		}
		img.setRGB(startX, startY, width, height, res, 0, width);
		return img;
	}


	// Get a random color (sans alpha), and darken or lighten it to match original average brightness

	public static BufferedImage mimicColors(BufferedImage img, int startX, int startY, 
											int endX, int endY, int similarity){
		Random rand = new Random();

		Color randColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));

    	int width = endX-startX, height = endY-startY;
    	int[] colordata = img.getRGB(startX, startY, width, height, null, 0, width); 
		
		for (int i=0; i<colordata.length; ++i){
			Color org = new Color(colordata[i]);
			Color mod = Color.BLACK;
			
			int r = (int)(getBrightness(org)*((float)randColor.getRed()/(float)getBrightness(randColor)));
			int g = (int)(getBrightness(org)*((float)randColor.getGreen()/(float)getBrightness(randColor)));
			int b = (int)(getBrightness(org)*((float)randColor.getBlue()/(float)getBrightness(randColor)));
			//System.out.println(r+" "+g+" "+b);

			r = (r > org.getRed() ? org.getRed() + similarity : org.getRed() - similarity);
			g = (g > org.getGreen() ? org.getGreen() + similarity : org.getGreen() - similarity);
			b = (b > org.getBlue() ? org.getBlue() + similarity : org.getBlue() - similarity);

			mod = new Color(r > 255 ? 255 : r < 0 ? 0 : r, g > 255 ? 255 : g < 0 ? 0 : g, b > 255 ? 255 : b < 0 ? 0 : b);

			int orgB = getBrightness(org);
			//System.out.println("org "+readCol(org)+" rand: "+readCol(randColor)+" mod: "+readCol(mod));

			colordata[i] = mod.getRGB();
		}

		img.setRGB(startX, startY, width, height, colordata, 0, width);
		return img;
	}

	private static int getBrightness(Color c){
		return c.getRed()+c.getGreen()+c.getBlue();
	}

	private static String readCol(Color c){
		return String.format("(%s,%s,%s)",c.getRed(),c.getGreen(),c.getBlue());
	}

	public static BufferedImage pixelShift(BufferedImage img, int startX, int startY, 
											int endX, int endY, int shiftx, int shifty, boolean colored){
		Random rand = new Random();
		// if dimensions of selected area are not explicitly set, select random ones
		startY = (startY!=-1)?startY:rand.nextInt(img.getHeight()-10);
		endY = (endY!=-1)?endY:startY+1+rand.nextInt(img.getHeight()-startY);
		startX = (startX!=-1)?startX:rand.nextInt(img.getWidth()-10);
		endX = (endX!=-1)?endX:startX+1+rand.nextInt(img.getWidth()-startX);
		shifty = (shifty!=-1)?shifty:rand.nextInt(endY-startY);
		shiftx = (shiftx!=-1)?shiftx:rand.nextInt(endX-startX);

		// make sure explicitly set dimensions are within image bounds
		startX = (startX>=0&&startX<img.getHeight())?startX:(startX>0)?img.getWidth()-1:0;
		startY = (startY>=0&&startY<img.getHeight())?startY:(startX>0)?img.getHeight()-1:0;
		endX = (endX>=0&&endX<img.getHeight())?endX:(endX>0)?img.getWidth()-1:0;
		endY = (endY>=0&&endY<img.getHeight())?endY:(endY>0)?img.getHeight()-1:0;
		int width = endX-startX, height = endY-startY;
		
		// particular pixel   = colordata[y*img.getWidth() + x]; 
    	int[] colordata = img.getRGB(startX, startY, width, height, null, 0, width);
		int[] res = new int[colordata.length];

		// run this with two indexers, one in the x direction and one in the y
		for (int x=0, y=0; x<width && y<height; x+=(x<width)?1:0, y+=(y<height)?1:0){
			int[] rowx = new int[width];
			int[] rowy = new int[height];			
			for (int i=0; i<rowx.length; ++i)
				rowx[i] = colordata[y*width + i];
			for (int i=0; i<rowy.length; ++i)
				rowy[i] = colordata[i*width + x];
			rowx = arrayShift(rowx, shiftx);
			rowy = arrayShift(rowy, shifty);
			for (int i=0; i<rowx.length; ++i)
				res[y*width + i] = rowx[i];
			for (int i=0; i<rowy.length; ++i)
				res[i*width + x] = rowy[i];
		}
		img.setRGB(startX, startY, width, height, res, 0, width); 
		if (colored) img = mimicColors(img, startX, startY, endX, endY, 15);
		return img;
	}

	public static int[][] edgeDetector(BufferedImage img, double specificity){
		int width = img.getWidth(), height = img.getHeight();
		int[] colordata = img.getRGB(0, 0, width, height, null, 0, width); 
		int[][] res = new int[colordata.length][2];
		int ei = 0;

		// holds R+B+G for each color
		int[] compoundColorData = new int[colordata.length];
		int i = 0;
		for (int cd : colordata){
			Color c = new Color(cd);
			compoundColorData[i++] = getBrightness(c);
		}

		double avgColorDiff = stdDeviation(compoundColorData);

		int threshold = (int)(avgColorDiff * specificity);

		//System.out.println("stdDev = "+avgColorDiff+"\nthreshold = "+threshold);

		// size of measurement grid - 
		// accuracy of 1 = 3x3 grid = (9-1) samples
		// accuracy of 2 = 5x5 grid = (25-1) samples
		int accuracy = 1;

		for (int y = 0; y < height; ++y){
			for (int x = 0; x < width; ++x){
				Color p1 = new Color(colordata[y * width + x]);

				Color[] samples = new Color[((accuracy*2+1)*(accuracy*2+1))-1];

				int sampleIndex = 0;
				for (int sy = 0; sy < accuracy*2+1; ++sy){
					for (int sx = 0; sx < accuracy*2+1; ++sx){
						if (sx == accuracy && sx == sy) // determining pixel
							continue;
						try {
							samples[sampleIndex] = new Color(colordata[((y-accuracy)+sy) * width + ((x-accuracy)+sx)]);
							++sampleIndex;
						} catch(Exception e){
							// the location of the sample pixel is out of image bounds, ignore it
							++sampleIndex;
						}
					}
				}

				for (Color s : samples){
					if (s != null){
						int savg = (getBrightness(s)+1)/3;
						int avg = (getBrightness(p1)+1)/3;
						
						if (x < width-1 && colCompare(p1, s, threshold)){
							res[ei][0] = x;
							res[ei++][1] = y; 
						}
					}
				}
			}
		}
		return res;
	}

	public static BufferedImage colorPixels(BufferedImage img, int[][] pix, Color col){
		int width = img.getWidth(), height = img.getHeight();

		int[] colordata = img.getRGB(0, 0, width, height, null, 0, width); 

		for (int[] edge : pix)
			colordata[edge[1] * width + edge[0]] = col.getRGB();

		img.setRGB(0, 0, width, height, colordata, 0, width);
		return img;
	}

	// compares two colors based on a threshold value and returns true if absolute values
	// of the red, green, and blue component differences are equal or above the threshold
	public static boolean colCompare(Color a, Color b, int threshold){
		return (Math.abs(a.getRed() - b.getRed()) +
				Math.abs(a.getGreen() - b.getGreen()) +
				Math.abs(a.getBlue() - b.getBlue()) >= threshold);
	}

	// shifts all the values in an array a certain amount.
	// If values overflow they go to the beginning
	public static int[] arrayShift(int[] arr, int shift){
		for (int j=0; j<shift; ++j){
			int tmp1 = arr[(arr.length-1)];
			for (int i=0; i<arr.length; ++i){
				int tmp2 = arr[i];
				arr[i] = tmp1;
				tmp1 = tmp2;
			}
		}
		return arr;
	}

	// if seconday array is passed, quick sorts both the base array and the secondary
	// array using the values in the base array.
	private static void quickSort(int[] base, int low, int high, int[] secondary) {
		if (secondary != null && base.length != secondary.length){
  			System.out.println("Arrays need to be same size!");
  			return;
		}
        int i = low;
        int j = high;
        int pivot = base[low+(high-low)/2];
        while (i <= j) {
            while (base[i] < pivot) i++;
            while (base[j] > pivot) j--;
            if (i <= j) {
                swap(base, i, j);
                if (secondary!=null) swap(secondary, i, j);
                i++;
                j--;
            }
        }
        if (low < j)
            quickSort(base, low, j, secondary);
        if (i < high)
            quickSort(base, i, high, secondary);
    }
    // I'll give you one guess
    private static void swap(int[] arr, int i, int j){
  		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
  	}

  	// Finds standard deviation of values in int array
  	private static double stdDeviation(int[] arr){
  		double sum=0, mean=0;
  		double[] diffs = new double[arr.length];

  		// find mean of arr
  		for (int val : arr)
  			sum += val;
  		mean = sum / arr.length;

  		// find square-differences
  		int i = 0;
  		for (int val : arr)
  			diffs[i++] = (val - mean)*(val - mean);

  		// find mean of square-differences
  		sum = 0;
  		for (double val : diffs)
  			sum += val;
  		mean = sum / diffs.length;
		
		// return square root of square differences
		return Math.sqrt(mean); 
  	}
}
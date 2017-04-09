import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.Random;
import java.util.Arrays;
import java.io.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Scanner; 


class databendAnimate {

	public static void main(String[] args){
		File file = new File(args[0]);
		try {
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

				else if (args[i].equals("animate")) {
					// argument count
					int ARGC = 2;

					if (i+1>=args.length){
						System.out.println("animate needs an mode parameter!");
						return;
					}
					
					String mode = args[i+1];
					int r = -1; // capture rate of processing: 
							  // r = 1  => every call to saveFrame() is aknowledged		 =>	100% frame ouput
							 //	 r = 23 => every 23rd call to saveFrame() is aknowledged => (100/23)% frame output
					for (int j=i+2; j<=i+(ARGC*2)+1; j+=2){
						if (j<args.length){
							if (!args[j].contains("-"))
								break;

							// -- ARGUMENTS --
							
							if (args[j].equals("--capture-rate"))
								if (j+1 < args.length)
									r = Integer.valueOf(args[j+1]);

							if (args[j].equals("--max-frames"))
								if (j+1 < args.length)
									VideoManager.MaxFrames = Integer.valueOf(args[j+1]);
								
							
						}
					
					}

					System.out.format("animate mode::%s capture-rate::%s max-frames::%s | \n", mode, r, VideoManager.MaxFrames);
					long t = System.currentTimeMillis();
					newImg = animate(newImg, mode, r, VideoManager.MaxFrames);
					System.out.println("Creating video ... ");
					VideoManager.createMp4FFMPEG(args[1].replace(".jpg", ""), 10);
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

	private static int ITERATION_STOP = -1; // preset to -1 since we need capture rate first
	public static BufferedImage animate(BufferedImage img, String mode, int r, int maxframes){
		boolean d = true;
		boolean rd = true;
		boolean g = true;
		boolean b = true;
		
		VideoManager.setFrameDir(String.format("Animation_%s", mode));
		VideoManager.setAnimImg(img);
		VideoManager.MaxFrames = maxframes;

		// use user-set capture rate if user supplied one otherwise we'll figure it out 
		// with the mode
		int captureRate = (r > 0) ? r : -1;

		int width = img.getWidth();
		int n = width; // for proper O(n) notation
		int height = img.getHeight();

		int frameTotal = width; // for O(n) complexity 

		System.out.println("\n===debug===\n(frameTotal)=(width)=(n)=(rowdata.length)="+frameTotal+"\n");


		int[] rowdata = new int[width];
		int[] averages = new int[width];
		
		System.out.println("===debug===\n(ITERATION_STOP)-(ITERATION_START)=(capture-rate)=(r)=(captureRate)="+captureRate+"\n"+
							"// Number of Iterations to perform of (mode)(<imagedata>) before saving 1 frame\n");
		int ITERATION_START = 0; 

		System.out.println("===debug===\nframeTotal="+frameTotal+"\n");
		int sortedRows = 0;

		int complexityBest = 0;

		String frame_dbg_info="";
		animationLoop:
		for (int FRAME_COUNT = 0; FRAME_COUNT < frameTotal && FRAME_COUNT < VideoManager.MaxFrames; ++FRAME_COUNT){
			// for each row of pixel in region (y-value)
			// System.out.println("===debug===\n");
			
			// progress bar
			printProgress(FRAME_COUNT, frameTotal, String.format(" (FRAME_COUNT)=%s | %s", FRAME_COUNT, frame_dbg_info));
			frame_dbg_info="";
			
			for (int y = 0; y<height-1; ++y){ // for per-row modification methods
				try {




					//****************************************************************************************
					// Methods used here need to: 
					//
					// - accept and use the parameter ITERATION_START to resume modification from any iteration
					// 		(use an overloaded signature to figure out required parameters from ITERATION_COUNT)
					//			(see quickSort example)
					// - exit immediately if (ITERATION_COUNT == ITERATION_STOP)
					// 		(store variables necessary for method continuation in globals before exit)
					//			(see quickSort example)
					// - throw an exception when the method is finished with processing the array (e.g. when it's sorted) 
					//
					// Before calling method, make sure to set the complexityBest variable and the captureRate
					//****************************************************************************************
					switch (mode.toLowerCase()){
						case "isort":
						case "insertion":
						case "insertionsort":
							complexityBest = n;							
							captureRate = captureRate > 0 ? captureRate : 10*(complexityBest / width);
							ITERATION_STOP = (ITERATION_STOP < 0) ? captureRate : ITERATION_STOP;
							//////////////

							// fill out rowdata[] and averages[] arrays
    						getRowDataAverages(rowdata, averages, width, y, d, rd, g, b);

							insertionSort(averages, ITERATION_START, rowdata);
    						
    						replaceRowData(rowdata, width, y, d);

							break;

						case "qsort":
						case "quick":
						case "quicksort":
							complexityBest = (int)(n*Math.log(n));
							captureRate = (int)(captureRate > 0 ? captureRate : 10*(complexityBest / width));
							ITERATION_STOP = (ITERATION_STOP < 0) ? captureRate : ITERATION_STOP;
							//////////////
							
							// fill out rowdata[] and averages[] arrays
    						getRowDataAverages(rowdata, averages, width, y, d, rd, g, b);
							
							quickSort(averages, ITERATION_START, rowdata);

    						replaceRowData(rowdata, width, y, d);

							break;
					}




				} catch (Exception e){
					frame_dbg_info+="                  ---Sorted Row----";
					if (++sortedRows == height)
						break animationLoop;
				}
			}
			// save a frame
			try {
				VideoManager.saveFrame();
			} catch (Exception e){
				System.out.format("Amount of frames has exceeded specified max (%s).\nContinue? (y/n) ", VideoManager.MaxFrames);
				Scanner scan = new Scanner(System.in);
				String s = scan.next();
				if (s.trim().equals("y"))
					VideoManager.MaxFrames = 0; // remove max amount ... 
				else break animationLoop;
			}

			// set the iteration parameter so the row sorting will continue on the exact iteration they left off (R)
			ITERATION_START = ITERATION_STOP;
			// add another R to complete the next set of iterations so the next run will cover R -> 2R
			ITERATION_STOP += captureRate;
		}
		System.out.println();
		img.setRGB(0, 0, width, height, VideoManager.getAnimFrameColorData(), 0, width);
		return img;
	}

	// Kick-ass progress bar Stolen from github.com/cschlisner/uniChess
	public static void printProgress(int prog, int total, String status){
        double percent = (double)prog/total;
        double percentFrom20 = 20 * percent;
        System.out.print("\rProcessing [");
        for (int i = 0; i < 20; ++i){
            if (i <= (int)percentFrom20)
                System.out.print("=");
            else System.out.print(" ");
        }
        System.out.print("] "+status);
    }

    private static void getRowDataAverages(int[] rowdata, int[] averages, int width, int y, boolean d, boolean rd, boolean g, boolean b){
    	// create copy array of row pixel data
		// if direction is reversed fill in array from right, else fill in array from left
		for (int i=((d)?width-1:0); i!=((d)?0:width-1); i+=((d)?-1:1)){
			try {
				rowdata[((d)?(width-1)-i:i)] = VideoManager.getAnimFrameColorData()[y*width+(i)];
			} catch (Exception e){
				System.out.println("y = "+y+" i = "+i);
				e.printStackTrace();
			}
		}
		// fill in an array of rgb average values
		for (int i=0; i<width; ++i){
			Color c = new Color(rowdata[i]);
			// only use the average of the enabled colors
			averages[i] = (((rd)?c.getRed():0)+((g)?c.getGreen():0)+((b)?c.getBlue():0))/(((rd)?1:0)+((g)?1:0)+((b)?1:0));
		}
    }

    private static void replaceRowData(int[] rowdata, int width, int y, boolean d){
    	// need to put the values back in the same way we took them
		for (int i=((d)?width-1:0); i!=((d)?0:width); i+=((d)?-1:1))
			 VideoManager.getAnimFrameColorData()[y*width+i] = rowdata[((d)?(width-1)-i:i)];
    }


	/**
	*	Sorts average pixel values in region in img
	*	row by row
	*/
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
			try {
				// sorts the rowdata[] based on the values in the averages[]
				quickSort(averages, 0, averages.length-1, rowdata);
			} catch(Exception e){
				// sorted
			}
			// need to put the values back in the same way we took them
			for (int i=((d)?width-1:0); i!=((d)?0:width); i+=((d)?-1:1))
				 colordata[y*width+i] = rowdata[((d)?(width-1)-i:i)];
		}
		//System.out.println("]");
		img.setRGB(startX, startY, width, height, colordata, 0, width);
		return img;
	}

	// find lowest val, bring to front
	// if seconday array is passed, quick sorts both the base array and the secondary
	// array using the values in the base array.
	// base = color averages
	// secondary = row data
	private static void insertionSort(int[] base, int[] secondary) throws Exception{
		insertionSort(base, 0, secondary);
	}

	private static void insertionSort(int[] base, int i, int[] secondary) throws Exception{
		if (i == ITERATION_STOP){
			// EXIT AFTER ITERATION_STOP ITERATIONS
			return;
		}
		if (secondary != null && base.length != secondary.length){
			System.out.println("Arrays need to be same size!");
			return;
		}
		int min = i;
		if (i < base.length-1){
			for (int j = i+1; j < base.length; ++j)
				min = (base[j] < base[min]) ? j : min;
			swap(base, i, min);
			if (secondary!=null)
				swap(secondary, i, min);
				
			insertionSort(base, ++i, secondary);
		}
		else throw new Exception("Sorted!");
	}


	static int[] qsLOW = null;
	static int[] qsHIGH = null;
	static int qsI = 0;
	private static void quickSort(int[] base, int i, int[] secondary){
		qsI = i;
		if (qsLOW == null){
			// n^2
			int complexityWorstQuickSort = base.length*base.length;

			qsLOW = new int[complexityWorstQuickSort]; // 
			qsHIGH = new int[complexityWorstQuickSort];
			qsHIGH[0] = base.length-1;
		}

		quickSort(base, qsLOW[qsI], qsHIGH[qsI], secondary);
	}

	// if seconday array is passed, quick sorts both the base array and the secondary
	// array using the values in the base array.
	private static void quickSort(int[] base, int low, int high, int[] secondary) {
		if (secondary != null && base.length != secondary.length){
  			System.out.println("Arrays need to be same size!");
  			return;
		}
		if (qsI == ITERATION_STOP){
			// save the parameters for continuation
			qsLOW[qsI] = low;
			qsHIGH[qsI] = high;
			return;
		}
		++qsI;

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

	private static void saveImage(BufferedImage img, boolean d, int startX, int startY, int width, int height, int y, int[] rowdata, int[] colordata, String path){
		// need to put the values back in the same way we took them
		for (int i=((d)?width-1:0); i!=((d)?0:width); i+=((d)?-1:1))
			 colordata[y*width+i] = rowdata[((d)?(width-1)-i:i)];

		// System.out.println("]");
		img.setRGB(startX, startY, width, height, colordata, 0, width);

		saveImage(img, path);
	}

	private static void saveImage(BufferedImage img, String path) {
		try {
			ImageIO.write(img, "jpg", new File(path));
		} catch (Exception e) {
			System.out.println("no");
		}
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

  	private static class VideoManager {
		public static int MaxFrames = 10000;
		public static int FrameCount = 0;

		public static String AnimFrameDir = "";
		public static String AnimFrame = null;
		private static int[] AnimFrameColorData = null;
		private static BufferedImage AnimImg = null;

		public static void setFrameDir(String dir){
			AnimFrameDir = dir;
			runCMD("mkdir "+AnimFrameDir);
		}

		public VideoManager(BufferedImage img, String outputFileDir, int maxFrames){
			setAnimImg(img);
			AnimFrameDir = outputFileDir;
			MaxFrames = maxFrames;
		}

		public static void saveFrame() throws Exception{

			if (MaxFrames > 0 && FrameCount == MaxFrames){
				throw new Exception("FrameCount exceeded MaxFrames, prompt user before continuing.");
			}


			AnimImg.setRGB(0, 0, AnimImg.getWidth(), AnimImg.getHeight(), AnimFrameColorData, 0, AnimImg.getWidth());

			try {
				ImageIO.write(AnimImg, "jpg", new File(String.format("%s/%s.jpg", AnimFrameDir, FrameCount++)));
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		public static void createMp4FFMPEG(String fileName, int framerate){
			try {
				runCMD(String.format("rm %s.mp4", fileName));
				runCMD(String.format("ffmpeg -framerate %s -i %s/%%d.jpg -c:v libx264 -r 20 -pix_fmt yuv420p %s.mp4", 
				framerate, AnimFrameDir, fileName)).waitFor();
			} catch (Exception e){
				System.out.println("Thread is no more");
			}
		}

		public static void setAnimImg(BufferedImage img){
			AnimImg = img;
			int imgWidth = img.getWidth();
			int imgHeight = img.getHeight();

			try {
				AnimFrameColorData = AnimImg.getRGB(0, 0, imgWidth, imgHeight, null, 0, imgWidth); 
			} catch(Exception e){
				System.out.println("Check parameters");
			}
		}

		public static BufferedImage getAnimImg(){
			return AnimImg;
		}

		public static int[] getAnimFrameColorData(){
			return AnimFrameColorData;
		}

		private static Process runCMD(String cmd){
	        Runtime rt = Runtime.getRuntime();
	        try {
	        	System.out.println("$"+cmd);
	            Process p = rt.exec(cmd);

				BufferedReader stdInput = new BufferedReader(new 
				     InputStreamReader(p.getInputStream()));

				BufferedReader stdError = new BufferedReader(new 
				     InputStreamReader(p.getErrorStream()));

				// read the output from the command
				String s = null;
				while ((s = stdInput.readLine()) != null) {
				    System.out.println(s);
				}

				// read any errors from the attempted command
				while ((s = stdError.readLine()) != null) {
				    System.out.println(s);
				}

	            return p;
	        } catch (Exception e){
	            e.printStackTrace();
	        }
	        return null;
	    }
	}
}


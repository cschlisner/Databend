import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.Random;
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
			System.out.println("done.");

			for (int i=2; i<args.length; ++i){
				if (args[i].equals("bshift")){
					if (i+1>=args.length){
						System.out.println("bshift needs an iteration parameter!");
						return;
					}
					boolean bcol = false;
					if (i+2<args.length){
						if (args[i+2].equals("-c")) bcol = true;
					}
					newImg = blockShifter(newImg, Integer.valueOf(args[i+1]), bcol);
				}
				else if (args[i].equals("lshift")){
					int lnmxhgt = -1, lnfreq = -1;
					boolean lncol = false;
					for (int j=i+1; j<=i+4; j+=2){
						if (j+1<args.length){
							if (args[j].equals("-h")) lnmxhgt = Integer.valueOf(args[j+1]);
							else if (args[j].equals("-f")) lnfreq = Integer.valueOf(args[j+1]);
						}
						if (j<args.length)
							if (args[j].equals("-c")) lncol = true;
					}
					newImg = lineShifter(newImg, lnmxhgt, lnfreq, lncol);
				}
				else if (args[i].equals("psort")){
					boolean r = false, g = false, b = false, d = false;
					for (int j=i+1; j<=i+2; ++j){
						if (j<args.length){
							if (args[j].equals("-r")) r=true;
							else if (args[j].equals("-g")) g=true;
							else if (args[j].equals("-b")) b=true;
							else if (args[j].equals("-d")) d=true;
						}
					}
					newImg = pixelSorter(newImg,d,r,g,b);
				}
			}
			ImageIO.write(newImg, "jpg", new File(args[1]));						  	   
		} catch (IOException e){
				System.out.println("Error. Check filename.");
				return;
		}
	}

	public static BufferedImage lineShifter(BufferedImage img, int maxHeight, int frequency, boolean colored){
		Random rand = new Random();
		int minHeight = img.getHeight()/128;
		maxHeight = (maxHeight!=-1)?maxHeight:rand.nextInt(img.getHeight()/18)+minHeight;
		frequency = (frequency!=-1)?frequency:rand.nextInt(50)+48; 
		int height=1;
		int n = img.getHeight();
		System.out.print("working: [");
		for (int i = 0; i<n; i+=height){
			if (i>=n) break;
			// progress bar
			System.out.print(((float)(((float)i/(float)n)/0.1) % 1 == 0)?".":"");
			height = rand.nextInt(maxHeight)+minHeight;
			// there's a [frequency]% chance of the following code executing
			if (height%(100/frequency)==0) 
				img = pixelShift(img, 0, i, img.getWidth(), i+height, -1, 0, colored);
		}
		System.out.print("]");
		return img;
	}

	public static BufferedImage blockShifter(BufferedImage img, int n, boolean colored){
		System.out.print("working: [");
		for (int i=0; i<n; ++i){
			// progress bar
			System.out.print(((float)(((float)i/(float)n)/0.1) % 1 == 0)?".":"");
			img = pixelShift(img, -1,-1,-1,-1,-1,-1, colored);
		}
		System.out.print("]");
		return img;
	}
	public static BufferedImage pixelSorter(BufferedImage img, boolean d, boolean r, boolean g, boolean b){
		// If no color flags were set, use them all
		if (!r&&!g&&!b){
			r = !r;
			g = !g;
			b = !b;
		}
		int width = img.getWidth(), height = img.getHeight();
		int[] colordata = img.getRGB(0, 0, width, height, null, 0, width); 
		int[] rowdata = new int[width];
		int[] averages = new int[width];
		System.out.print("working: [");
		for (int y = 0; y<height; ++y){
			// progress bar
			System.out.print(((float)(((float)y/(float)height)/0.1) % 1 == 0)?".":"");
			// if direction is reversed fill in array from right, else fill in array from left
			for (int i=((d)?width-1:0); i!=((d)?0:width); i+=((d)?-1:1))
				rowdata[((d)?(width-1)-i:i)] = colordata[y*width+i];
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
		System.out.print("]");
		img.setRGB(0, 0, width, height, colordata, 0, width);
		return img;
	}	

	public static BufferedImage adjustColors(BufferedImage img, int startX, int startY, 
											int endX, int endY, int r, int g, int b, int a, int density){
		
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
		a = (a!=-1)?a:rand.nextInt(256);
		// density needs to be at least 2 for image to be visible
		density = (density!=-1)?density:rand.nextInt(5)+2;
		int width = endX-startX, height = endY-startY;
    	int[] colordata = img.getRGB(startX, startY, width, height, null, 0, width); 
		int[] res = new int[colordata.length];
		for (int i=0; i<colordata.length; i+=density){
			Color color = new Color(colordata[i]);
			Color newColor = new Color(
				(0<=(color.getRed()+r)  &&(color.getRed()   +r)<=255)? color.getRed()  +r:(r>0)?255:0, 
			    (0<=(color.getGreen()+g)&&(color.getGreen() +g)<=255)? color.getGreen()+g:(g>0)?255:0,
			    (0<=(color.getBlue()+b) &&(color.getBlue()  +b)<=255)? color.getBlue() +b:(b>0)?255:0,
				(0<=(color.getAlpha()+a)&&(color.getAlpha() +a)<=255)? color.getAlpha() +a:(a>0)?255:0);
			// Too light of colors create a bad lighting effect
			if (color.getRed()+color.getGreen()+color.getBlue()>=550) newColor = newColor.darker();
			res[i] = newColor.getRGB();
		}
		img.setRGB(startX, startY, width, height, res, 0, width);
		return img;
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
		if (colored) img = adjustColors(img, startX, startY, endX, endY, -1, -1, -1, -1, 1);
		return img;
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
}
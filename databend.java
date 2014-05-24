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
			for (int i=1; i<args.length; ++i){
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
			}
			ImageIO.write(newImg, "jpg", new File("copy - "+file.toString()));						  	   
		} catch (IOException e){}
	}

	public static BufferedImage lineShifter(BufferedImage img, int maxHeight, int frequency, boolean colored){
		Random rand = new Random();
		int minHeight = img.getHeight()/128;
		maxHeight = (maxHeight!=-1)?maxHeight:rand.nextInt(img.getHeight()/18)+minHeight;
		frequency = (frequency!=-1)?frequency:rand.nextInt(50)+48; 
		int height=1;
		int n = img.getHeight();
		System.out.print("[");
		for (int i = 0; i<n; i+=height){
			if (i>=n) break;
			System.out.print(((float)(((float)i/(float)n)/0.1) % 1 == 0)?".":"");
			height = rand.nextInt(maxHeight)+minHeight;
			if (height%(100/frequency)==0) /// there's a [frequency]% chance of the following code executing
				img = pixelShift(img, 0, i, img.getWidth(), i+height, -1, 0, colored);
		}
		System.out.print("]");
		return img;
	}

	public static BufferedImage blockShifter(BufferedImage img, int n, boolean colored){
		System.out.print("[");
		for (int i=0; i<n; ++i){
			System.out.print(((float)(((float)i/(float)n)/0.1) % 1 == 0)?".":"");
			img = pixelShift(img, -1,-1,-1,-1,-1,-1, colored);
		}
		System.out.print("]");
		return img;
	}

	public static BufferedImage adjustColors(BufferedImage img, int startX, int startY, 
											int endX, int endY, int r, int g, int b, int a, int density){
		
		Random rand = new Random();
		startY = (startY!=-1)?startY:rand.nextInt(img.getHeight()-10);
		endY = (endY!=-1)?endY:startY+rand.nextInt(img.getHeight()-startY);
		startX = (startX!=-1)?startX:rand.nextInt(img.getWidth()-10);
		endX = (endX!=-1)?endX:startX+rand.nextInt(img.getWidth()-startX);
		r = (r!=-1)?r:rand.nextInt(256);
		g = (g!=-1)?g:rand.nextInt(256);
		b = (b!=-1)?b:rand.nextInt(256);
		a = (a!=-1)?a:rand.nextInt(256);
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
			if (color.getRed()+color.getGreen()+color.getBlue()>=550) newColor = newColor.darker();
			res[i] = newColor.getRGB();
		}
		img.setRGB(startX, startY, width, height, res, 0, width);
		return img;
	}

	public static BufferedImage pixelShift(BufferedImage img, int startX, int startY, 
											int endX, int endY, int shiftx, int shifty, boolean colored){
		Random rand = new Random();
		startY = (startY!=-1)?startY:rand.nextInt(img.getHeight()-10);
		endY = (endY!=-1)?endY:startY+1+rand.nextInt(img.getHeight()-startY);
		startX = (startX!=-1)?startX:rand.nextInt(img.getWidth()-10);
		endX = (endX!=-1)?endX:startX+1+rand.nextInt(img.getWidth()-startX);
		shifty = (shifty!=-1)?shifty:rand.nextInt(endY-startY);
		shiftx = (shiftx!=-1)?shiftx:rand.nextInt(endX-startX);
		// particular pixel   = colordata[y*img.getWidth() + x]; 
		startX = (startX>=0&&startX<img.getHeight())?startX:(startX>0)?img.getWidth()-1:0;
		startY = (startY>=0&&startY<img.getHeight())?startY:(startX>0)?img.getHeight()-1:0;
		endX = (endX>=0&&endX<img.getHeight())?endX:(endX>0)?img.getWidth()-1:0;
		endY = (endY>=0&&endY<img.getHeight())?endY:(endY>0)?img.getHeight()-1:0;
		int width = endX-startX, height = endY-startY;
		
    	int[] colordata = img.getRGB(startX, startY, width, height, null, 0, width);
		int[] res = new int[colordata.length];
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
}
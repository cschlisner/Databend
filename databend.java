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
			BufferedImage image = ImageIO.read(file);
			BufferedImage newImg = image;
			if (args[1].equals("shift")){
				if (args.length < 3)
					System.out.println("udongoofed");
				else {
					int n = Integer.valueOf(args[2]);
					System.out.print("[");
					for (int i=0; i<n; ++i){
						System.out.print(((float)(((float)i/(float)n)/0.1) % 1 == 0)?".":"");
						newImg = pixelShift(newImg, -1,-1,-1,-1,-1,-1, false);
					}
					System.out.print("]");
				}
			}
			ImageIO.write(newImg, "bmp", file);						  	   
		} catch (IOException e){}
	}

	public static BufferedImage adjustColors(BufferedImage img, int startx, int starty, 
											int endx, int endy, int r, int g, int b, int a, int density){
		
		Random rand = new Random();
		starty = (starty!=-1)?starty:rand.nextInt(img.getHeight()-10);
		endy = (endy!=-1)?endy:starty+rand.nextInt(img.getHeight()-starty);
		startx = (startx!=-1)?startx:rand.nextInt(img.getWidth()-10);
		endx = (endx!=-1)?endx:startx+rand.nextInt(img.getWidth()-startx);
		r = (r!=-1)?r:rand.nextInt(256);
		g = (g!=-1)?g:rand.nextInt(256);
		b = (b!=-1)?b:rand.nextInt(256);
		a = (a!=-1)?a:rand.nextInt(256);
		density = (density!=-1)?density:rand.nextInt(5)+2;
		int width = endx-startx, height = endy-starty;
    	int[] colordata = img.getRGB(startx, starty, width, height, null, 0, width); 
		int[] res = new int[colordata.length];
		for (int i=0; i<colordata.length; i+=density){
			Color color = new Color(colordata[i]);
			Color newColor = new Color(
				(0<=(color.getRed()+r)  &&(color.getRed()   +r)<=255)? color.getRed()  +r:(r>0)?255:0, 
			    (0<=(color.getGreen()+g)&&(color.getGreen() +g)<=255)? color.getGreen()+g:(g>0)?255:0,
			    (0<=(color.getBlue()+b) &&(color.getBlue()  +b)<=255)? color.getBlue() +b:(b>0)?255:0,
				(0<=(color.getAlpha()+a)&&(color.getAlpha() +a)<=255)? color.getAlpha() +a:(a>0)?255:0);
			res[i] = newColor.getRGB();
		}
		img.setRGB(startx, starty, width, height, res, 0, width);
		return img;
	}

	public static BufferedImage pixelShift(BufferedImage img, int startx, int starty, 
											int endx, int endy, int shiftx, int shifty, boolean colored){
		Random rand = new Random();
		starty = (starty!=-1)?starty:rand.nextInt(img.getHeight()-10);
		endy = (endy!=-1)?endy:starty+1+rand.nextInt(img.getHeight()-starty);
		startx = (startx!=-1)?startx:rand.nextInt(img.getWidth()-10);
		endx = (endx!=-1)?endx:startx+1+rand.nextInt(img.getWidth()-startx);
		shifty = (shifty!=-1)?shifty:rand.nextInt(endy-starty);
		shiftx = (shiftx!=-1)?shiftx:rand.nextInt(endx-startx);
		int width = endx-startx, height = endy-starty;
		// particular pixel   = colordata[y*img.getWidth() + x]; 
    	int[] colordata = img.getRGB(startx, starty, width, height, null, 0, width);
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
		img.setRGB(startx, starty, width, height, res, 0, width); 
		if (colored) img = adjustColors(img, startx, starty, endx, endy, -1, -1, -1, -1, 1);
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
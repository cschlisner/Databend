import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.io.IOException;
import javax.imageio.ImageIO;

class JPEGCorrupt {

	public static final byte SOIMARKER =  (byte)0xD8;
	public static final byte SOSMARKER =  (byte)0xDA;
	public static final byte EOIMARKER =  (byte)0xD9;
	public static final byte DHTMARKER =  (byte)0xC4;
	public static final byte DQTMARKER =  (byte)0xDB;
	public static final byte SOF0MARKER =  (byte)0xC0;
	public static final byte SOF2MARKER =  (byte)0xC2;

	public static void main(String[] args){
		try {
			
			if (args.length >= 5 && args[2].equals("-v")){
				MJPEG video = new MJPEG(args[0]);
				video.process(args[3], Integer.valueOf(args[4]), Float.valueOf(args[5]), 
					new EvalCallback(){
						@Override
						public void eval(byte val){
							System.out.println("Eval: "+val+" -> "+(val+1));
							val = (byte)((int)val + 1);
						}
					});
				video.save(args[1]);
			}
			// inline mode, format = input.jpg -i output.jpg <action> <action args>
			// actions:
			//	-e : edit | {-e <Section type> <Seciton index> <Byte index> <New Value>}
			//	-ex : exhaustive | 
			// 	-r : rand
			//	-print : print
			//	-re : repeat
			JPEG image = new JPEG(args[0]);
			JPEG savedImageState = new JPEG(image);				
			
			if (args.length > 2 && args[2].equals("-i")){
				//JPEG output = new JPEG(args[2]);
				String[] cmdarg = null;
				try{
					cmdarg = Arrays.copyOfRange(args, 4, args.length); 
					for (String c : cmdarg)
						System.out.print(c+" ");
					System.out.println();
				} catch (Exception e){}
				switch (args[3]){
					case "-e":
						edit(image, cmdarg);
						break;
					case "-ex":
						exhaust(image, cmdarg);
						break;
					case "-r":
						randLogical(image, cmdarg);
						break;
					case "-print":
						print(image, cmdarg);
						return;
				}
				image.save(args[1]);
				return;
			}

			Scanner scn = new Scanner(System.in);
			interfaceloop:
			for (;;){
				System.out.println("Editing "+args[0]+" | type ? for help");			
				System.out.print("#> ");

				// multiple commands at once with ; delimiter 
				String[] cmds = scn.nextLine().split(";");
				
				for (String cmd : cmds){
					String[] cmdtok = cmd.split(" ");
					String[] cmdarg = null;
					try {
						cmdarg = Arrays.copyOfRange(cmdtok, 1, cmdtok.length); 
						// System.out.print("args: ");
					 	// for (String s : cmdarg)
						// 	System.out.print(s+" ");
						// System.out.println();
					} catch (Exception e){}

					try {
						
						switch(cmdtok[0]){
							case "edit":
								savedImageState = new JPEG(image);
								edit(image, cmdarg);
								break;

							case "exhaustive":
								savedImageState = new JPEG(image);
								exhaust(image, cmdarg);
								break;
					
							case "rand":
								savedImageState = new JPEG(image);						
								randLogical(image, cmdarg);
								break;
					
							case "print":
								print(image, cmdarg);
								break;
					
							case "repeat":
								savedImageState = new JPEG(image);						
								repeat(image, cmdarg);
								break;
					
							case "save":
								image.save(args[1]);
								break;

							case "undo":
								image = new JPEG(savedImageState);
								break;
							case "exit":
							case "q":
								break interfaceloop;
							
							default:
								//printHelp();
								break;
						}
					} catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			image.save(args[1]);			
		} catch (Exception e){
			e.printStackTrace();
		}
	}	
	
	// OPTIONAL args = {SectionType (SOS, DHT, etc), section indices...}
	private static void print(JPEG image, String[] args) throws Exception{
		System.out.println(image);

		if (args != null && args.length > 1){
			// get section type to edit (SOS, DHT, etc)
			JPEG.Section[] sections = image.getSection(args[0]);
			if (sections == null)
				throw new Exception("[print]: need section type to print");
			
			// get indices of sections to edit
			List<Integer> sectionIndices = new ArrayList<>();
			for (int i=1; i<args.length; ++i)
				sectionIndices.add(Integer.valueOf(args[i]));
			if (sectionIndices.isEmpty())
				throw new Exception("[print]: need section indices to print");
				
			for (int i: sectionIndices){
				System.out.format("------- %s[%s] -------\n",args[0],i);
				System.out.println(sections[i]);
			}
		}
	}

	/**
	*	Iterates through sections and applies random logical operators to the bytes
	*
	*	@param args needs to specify section type, logical operators to use (all is default), and section indices
	*	
	*/
	// example: 
	// args = {DHT, -OR, -XOR, 0, 1}
	private static void randLogical(JPEG image, String[] args) throws Exception{
		if (args == null)
			throw new Exception("[randLogical]: need arguments");

		// get section type to edit (SOS, DHT, etc)
		JPEG.Section[] sections = image.getSection(args[0]);
		if (sections == null)
			throw new Exception("[randLogical]: need section type");
		
		// set boolean operations to use
		// AND, XOR, OR
		// default is all
		List<Integer> operators = new ArrayList<>();
		for (int i=1; i<args.length; ++i){
			if (args[i].contains("-")){
				switch (args[i]){
					case "-AND":
						operators.add(0);
						break;
					case "-XOR":
						operators.add(1);
						break;
					case "-OR":
						operators.add(2);					
						break;
				}
			}
		}
		if (operators.isEmpty())
			for (int i = 0; i < 3; ++i)
				operators.add(i);
		

		// get indices of sections to edit
		List<Integer> sectionIndices = new ArrayList<>();
		for (int i=1; i<args.length; ++i)
			if (!args[i].contains("-"))
				sectionIndices.add(Integer.valueOf(args[i]));

		for (int si : sectionIndices){
			JPEG.Section s = sections[si];

			for (int i = s.datastart+1; i < s.data.length-1; i += (new Random()).nextInt(s.data.length - i)){
				int mode = operators.get((new Random()).nextInt(operators.size()));
				System.out.format("%s[%s]: %s > ", si, i, s.data[i]);
				if (mode == 0)
					s.data[i] = (byte)(s.data[i] & image.bytes[3*i]);
				else if (mode == 1)
					s.data[i] = (byte)(s.data[i] ^ image.bytes[i]);
				else if (mode == 2)
					s.data[i] = (byte)(s.data[i] | image.bytes[i]);
				System.out.format("%s (%s)\n", s.data[i], (mode == 0 ? "&" : (mode == 1) ? "^" : (mode == 2) ? "|" : ""));

			}
		}
	}

	private static void edit(JPEG image, String[] args) throws Exception{
			System.out.println("===> Edit");
		// get section type to edit (SOS, DHT, etc)	
		JPEG.Section[] sections = image.getSection(args[0]);
		if (sections == null)
			throw new Exception("[edit]: need section type to edit");
		
		JPEG.Section s = sections[Integer.valueOf(args[1])];
		int k = Integer.valueOf(args[3]);
		s.data[Integer.valueOf(args[2])] = (byte)k;
	}

	// output a new image for each possible byte value for each byte in each section of 
	// a specified type. !!!!!!!!!!!!!!CAREFUL!!!!
	private static void exhaust(JPEG image, String[] args){
		String sectionType = args[0];
		int lim = (args.length > 0 ? Integer.valueOf(args[1]) : image.getSection(sectionType).length);
		boolean clean = args.length > 2 ? (args[2].equals("-c")) : false;
		File dir = new File(sectionType+"_exhaustive");    
		dir.mkdir();

		if (lim > image.getSection(sectionType).length)
			lim -= image.getSection(sectionType).length;

		for (int i = 0, n = 0; i < lim; ++i){
			JPEG.Section s = image.getSection(sectionType)[i];
			for (int j = 0; j < s.data.length; ++j){
				for (int v = 0; v < 255; ++v){
					byte[] data = null;
					if (clean)
						data = s.data;
					s.data[s.datastart+j] = (byte)v;
					image.save(sectionType+"_exhaustive/"+String.valueOf(++n));
					if (clean && data != null)
						s.data = data;
				}
			}
		}
	}

	private static void repeat(JPEG image, String[] args){
		// repeat blocks of image

	}

	private static void inject(JPEG image, String[] args){
		// inject data into image
	}

	abstract static class EvalCallback {
		public EvalCallback(){
		}
		public abstract void eval(byte val);
	}

	private static class MJPEG {
		public byte[] bytes;
		public ArrayList<JPEG> frames = new ArrayList<>();

		public MJPEG(String filename){
			Path file = Paths.get(filename);
			try {
				bytes = Files.readAllBytes(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// edit random bytes in section with a certain frequency and a EvalCallback function fn
		// f
		public void process(String section, int secIndex, float freq, EvalCallback fn){
			int frameStart = -1;
			int frameEnd = -1;
			for (int seek = 0 ; seek < bytes.length; ++seek){
				if (bytes[seek] == SOIMARKER){
					frameStart = seek;
				}
				if (frameStart > 0 && bytes[seek] == EOIMARKER){
					frameEnd = seek;
					
					JPEG frame = new JPEG(Arrays.copyOfRange(bytes, frameStart, frameEnd+1));

					try {
						String[] k = {""};
						print(frame, k);
					} catch(Exception e){}

					Random rand = new Random();
					for (int i = frame.SOS[secIndex].datastart; i < frame.SOS[secIndex].data.length; ++i){
						if (rand.nextFloat() <= freq){
							fn.eval(frame.SOS[secIndex].data[i]);
						}
					}

					frameStart = -1;
					frameEnd = -1;

				}
			}
		}

		public void save(String newFile){
			try {
				FileOutputStream stream = new FileOutputStream(newFile);
			    stream.write(bytes);
			    stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class JPEG {
		public byte[] bytes;
		public Section[] SOS, EOI, DHT, DQT, SOF2, SOF0, SOI;

		public JPEG(byte[] imgbytes){
			bytes = imgbytes;
			SOS = getAllSections(SOSMARKER);
			EOI = getAllSections(EOIMARKER);
			DHT = getAllSections(DHTMARKER);
			DQT = getAllSections(DQTMARKER);
			SOF2 = getAllSections(SOF2MARKER);
			SOF0 = getAllSections(SOF0MARKER);
			SOI = getAllSections(SOIMARKER);
		}

		public JPEG(JPEG other){
			this.bytes = Arrays.copyOfRange(other.bytes, 0, other.bytes.length);
			this.SOS = Arrays.copyOfRange(other.SOS, 0, other.SOS.length);
			this.EOI = Arrays.copyOfRange(other.EOI, 0, other.EOI.length);
			this.DHT = Arrays.copyOfRange(other.DHT, 0, other.DHT.length);
			this.DQT = Arrays.copyOfRange(other.DQT, 0, other.DQT.length);
			this.SOF2 = Arrays.copyOfRange(other.SOF2, 0, other.SOF2.length);
			this.SOF0 = Arrays.copyOfRange(other.SOF0, 0, other.SOF0.length);
			this.SOI = Arrays.copyOfRange(other.SOI, 0, other.SOI.length);
		}

		public JPEG(String filename){
			Path file = Paths.get(filename);
			try {
				bytes = Files.readAllBytes(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
			SOS = getAllSections(SOSMARKER);
			EOI = getAllSections(EOIMARKER);
			DHT = getAllSections(DHTMARKER);
			DQT = getAllSections(DQTMARKER);
			SOF2 = getAllSections(SOF2MARKER);
			SOF0 = getAllSections(SOF0MARKER);
			SOI = getAllSections(SOIMARKER);
		}

		public void save(String newFile){
			System.out.println("Saving: "+newFile);
			for (Section s : SOS)
				bytes = putBytesIntoArray(bytes, s.data, s.offset);
			for (Section s : DHT)
				bytes = putBytesIntoArray(bytes, s.data, s.offset);
			for (Section s : DQT)
				bytes = putBytesIntoArray(bytes, s.data, s.offset);
			for (Section s : SOF2)
				bytes = putBytesIntoArray(bytes, s.data, s.offset);
			for (Section s : SOF0)
				bytes = putBytesIntoArray(bytes, s.data, s.offset);

			try {
				FileOutputStream stream = new FileOutputStream(newFile);
			    stream.write(bytes);
			    stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private Section[] getSection(String type){
			switch(type){
				case "SOS":
					return SOS;
				case "DHT":
					return DHT;
				case "DQT":
					return DQT;
				case "S0F2":
					return SOF2;
				case "SOF0":
					return SOF0;
				default:
					return null;
			}
		}

		private Section[] getAllSections(byte marker){
			List<Section> sections = new ArrayList<>();
			int sectionIndex = -1;

			for (int i = 0; i<bytes.length; ++i){
				if (bytes[i] == (byte)0xFF){

					if (sectionIndex > 0){
						sections.add(new Section(marker, sectionIndex, Arrays.copyOfRange(bytes, sectionIndex+2, i)));
						sectionIndex = -1;
					}

					if (sectionIndex < 0 && bytes[i+1] == marker)
						sectionIndex = i;
				}
			}

			Section[] sectArr = new Section[sections.size()];
			int i = 0;
			for (Section s : sections) sectArr[i++] = s;
			return sectArr;
		}

		private class Section{
			
			int offset;
			int datastart;
			byte[] data;
			byte marker;
			public Section(byte marker, int offset, byte[] data){
				this.offset = offset + 2;
				this.data = data;
				this.marker = marker;
				
				if (marker == SOSMARKER)
					this.datastart = (int)data[1];
			}

			@Override
			public String toString(){
				StringBuilder sb = new StringBuilder();
				if (marker == DHTMARKER){
					// print labels for huffman definitions 0-16 http://stackoverflow.com/questions/662565/how-to-create-huffman-tree-from-ffc4-dht-header-in-jpeg-file
					System.out.println("_________________________Definitions________________________________| Symbols");
					System.out.println("1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16|");
				}
				int i = 0;
				for (byte b : data){
					++i;
					sb.append(b+((i<16)?" | ":" "));
				}
				sb.append("\n");
				return sb.toString();
			}
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("SOS: ");
			for (Section s : SOS)
				sb.append(s.offset+", ");
			sb.append("\nDHT: ");
			for (Section s : DHT)
				sb.append(s.offset+", ");
			sb.append("\nDQT: ");
			for (Section s : DQT)
				sb.append(s.offset+", ");
			sb.append("\nSOF2: ");
			for (Section s : SOF2)
				sb.append(s.offset+", ");
			sb.append("\nSOF0: ");
			for (Section s : SOF0)
				sb.append(s.offset+", ");
			return sb.toString();
		}
	}

	public static byte[] putBytesIntoArray(byte[] org, byte[] data, int startIndex){

		byte[] res = new byte[org.length];

		//System.out.println(String.format("Injecting array of size %s into array of size %s at position %s", data.length, org.length, startIndex));

		for (int i = 0, dataIndex = 0; i < res.length; ++i){
			if (i < startIndex || i >= startIndex+data.length){
				res[i] = org[i];
			}
			else res[i] = data[dataIndex++];
		}

		return res;
	}
}



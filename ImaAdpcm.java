
/**
 * Created by leo on 2017/6/26.
 */

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImaAdpcm {
    private static final byte[] stepIdxTable = {
        -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8
    };
    private static final short[] stepTable = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
            19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
            50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
            130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
            876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
            2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
            5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };
    private int block_length=256;
    private int block_length_decompression=block_length*4-14;
    public ImaAdpcm(){
    	
    }
    
    public byte[] convertAdpcmToPCM( InputStream input,int dataLengthSrc ) throws IOException {
      byte []tmp=new byte[0];
      byte []out=new byte[0];      
      
      byte[] buffer ;
      while( dataLengthSrc > 0 ) {
    	  buffer = new byte[ block_length_decompression ];
          int samples = dataLengthSrc > block_length ? block_length : dataLengthSrc;
          decode( input, buffer, samples );
          out=new byte[tmp.length+buffer.length];
          System.arraycopy(tmp,0,out,0,tmp.length);
          System.arraycopy(buffer,0,out,tmp.length,buffer.length);
          tmp=out.clone();

          dataLengthSrc -= samples;
      }
      return out;
  }

    public void decode( InputStream input, byte[] output, int count ) throws IOException {
        byte codes[]=new byte[count];
        int read_length=readFully( input, codes, 0, count );
        int inputIdx = 0, outputIdx = 0, outputEnd = output.length-1;


        byte codeSample1=codes[ inputIdx++ ];
        byte codeSample2=codes[ inputIdx++ ];
        byte codeIndex=codes[ inputIdx++ ];
        byte codeReserved=codes[ inputIdx++ ];

        output[outputIdx++]=codeSample1;
        output[outputIdx++]=codeSample2;
        int index=(codeIndex & 0xFF);
        int cur_sample=this.byteToShort(new byte[]{codeSample1,codeSample2});
        while( outputIdx < outputEnd && inputIdx<codes.length ) {
            byte codeB=codes[ inputIdx++ ];
            int Code1=((codeB >> 7) & 0x1)*8+((codeB >> 6) & 0x1)*4+((codeB >> 5) & 0x1)*2+((codeB >> 4) & 0x1)*1;
            int Code2=((codeB >> 3) & 0x1)*8+((codeB >> 2) & 0x1)*4+((codeB >> 1) & 0x1)*2+((codeB ) & 0x1)*1;
            
            int Code = Code2 & 0xFF;
            int fg=0;
            if ((Code & 8) != 0) fg=1 ;
            Code&=7;
            
            if (index<0) index=0;
            if (index>88) index=88;
            int diff = (int)((stepTable[index]*Code) /4.0+ stepTable[index] / 8.0);            
            if (fg==1) diff=-diff;
            cur_sample+=diff;
            
            if (cur_sample>32767) cur_sample=32767;
            else if (cur_sample<-32768) cur_sample= -32768;
            byte byteSample[]=ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) cur_sample).array();        
            
            output[outputIdx++]=byteSample[0];
            output[outputIdx++]=byteSample[1];

            index+=stepIdxTable[Code];
            if (index<0) index=0;
            if (index>88) index=88;



            Code = Code1 & 0xFF;
            fg=0;
            if ((Code & 8) != 0) fg=1 ;
            Code&=7;
            diff = (int)((stepTable[index]*Code) /4.0 + stepTable[index] / 8.0);
            if (fg==1) diff=-diff;
            cur_sample+=diff;
            if (cur_sample>32767) cur_sample=32767;
            else if (cur_sample<-32768) cur_sample= -32768;
            byteSample=ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) cur_sample).array();
            
            output[outputIdx++]=byteSample[0];
            output[outputIdx++]=byteSample[1];


            index+=stepIdxTable[Code];
            if (index<0) index=0;
            if (index>88) index=88;

        }
        
    }


    /* Read no less than count bytes from input into the output array. */
    public static int readFully( InputStream input, byte[] output, int offset, int count ) throws IOException {
        int ret_length=0;
        int end = offset + count;
        while( offset < end ) {
            int read = input.read( output, offset, end - offset );
//            if( read < 0 ) throw new java.io.EOFException();
            if( read < 0 ) break;
            offset += read;
            ret_length+=read;
        }
        return ret_length;
    }
    
    public short byteToShort(byte[] b){
        int MASK = 0xFF;
        short result = 0;
        result = (short) (b[0] & MASK);
        result = (short) (result + ((b[1] & MASK) << 8));
        return result;
    }

    
}

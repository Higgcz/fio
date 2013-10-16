/**
 * @author jan.cajthaml 2013
 * 
 * Port of "Fast IO for C++"
 * 
 * Based on Fio.h, Fio.cpp and idea of StringTokenizer implementation in Java (char* + lookup)
 * 
 */

import java.util.regex.*;
import java.io.*;
import java.nio.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.SwingUtilities;

public final class Fio implements Iterator<String>, Closeable
{
	
    // ---- [constants] ------

    private  static  final    int B_SIZE        =  4096;
    
    private  static  Pattern  WHITESPACE        =  Pattern.compile ( "\\p{javaWhitespace}+"                                                  );
    private  static  Pattern  FIND_ANY_PATTERN  =  Pattern.compile ( "(?s).*"                                                                );
    private  static  Pattern  BOOLEAN           =  Pattern.compile ( "true|false|TRUE|FALSE"                                                 );
    private  static  Pattern  DECIMAL           =  Pattern.compile ( "([\\+-]?\\d+(\\.\\d*)?|\\.\\d+)([eE][\\+-]?(\\d+(\\.\\d*)?|\\.\\d+))?" );
    private  static  Pattern  INTEGER           =  Pattern.compile ( "([\\+-]?\\d+)([eE][\\+-]?\\d+)?"                                       );
    private  static  Pattern  LINE              =  Pattern.compile ( ".*(\r\n|[\n\r\u2028\u2029\u0085])|.+$"                                 );

    // ---- [attrs] ------
    
    private  CharBuffer         buf        =  null;
    
    private  int                position   =  0;
    private  int                save       =  0;
    
    private  Matcher            matcher    =  null;
    private  Pattern            delimiter  =  null;
    private  InputStreamReader  source     =  null;
    
    private  boolean            input      =  false;
    private  boolean            skipped    =  false;
    private  boolean            valid      =  false;
    private  boolean            closed     =  false;

    // ---- [ctor] ------
    
    private Fio( InputStream source )
    {
        this . delimiter        =  WHITESPACE;
        this . source           =  new InputStreamReader( source );
        this . buf              =  CharBuffer . allocate( B_SIZE );
        
        this . buf.limit ( 0 );
        
        this . matcher          =  delimiter . matcher( buf );
        
        this . matcher . useTransparentBounds ( true  );
        this . matcher . useAnchoringBounds   ( false );
    }

    // ---- [api] ------

    public  boolean  hasNextBoolean ()  { return hasNext ( BOOLEAN );                         }
    public  boolean  hasNextByte    ()  { return hasNext ( INTEGER );                         }
    public  boolean  hasNextShort   ()  { return hasNext ( INTEGER );                         }
    public  boolean  hasNextInt     ()  { return hasNext ( INTEGER );                         }
    public  boolean  hasNextDecimal ()  { return hasNext ( DECIMAL );                         }
    
    public  boolean  nextBoolean    ()  { return Boolean . parseBoolean ( next( BOOLEAN ) );  }
    public  byte     nextByte       ()  { return Byte    . parseByte    ( next( INTEGER ) );  }	//FIXME better boudaries for short (use diff regex)
    public  short    nextShort      ()  { return Short   . parseShort   ( next( INTEGER ) );  }	//FIXME better boudaries for short (use diff regex)
    public  int      nextInt        ()  { return Integer . parseInt     ( next( INTEGER ) );  }
    public  float    nextDecimal    ()  { return Float   . parseFloat   ( next( DECIMAL ) );  }

    public boolean hasNext()
    {
        this . save  =  this . position;
        
        while( !closed )
        {
            if (has_token())
            {
                rollback();
                return true;
            }
            read();
        }

        boolean b = has_token();
        rollback();
        return b;
    }

    public String next()
    {
        while (true)
        {
            String token = find_token(null);
            if( token != null )
            {
            	this . valid    =  true;
                this . skipped  =  false;
                return token;
            }
            if( input ) read();
        }
    }

    public boolean hasNextLine()
    {
    	this . save    =  position;
        String result  =  scan ( LINE , 0 );
        
        if( result != null && valid )
        {
            String line = matcher . toMatchResult() . group( 1 );
            
            if( line != null )
                result = result.substring ( 0 , result.length() - line.length() );
        }
        
        rollback();
        
        return result != null;
    }

    public String nextLine()
    {
        String result = scan( LINE, 0 );
        
        if( result == null || !valid ) throw new NoSuchElementException("No line found");
        
        String line = matcher . toMatchResult() . group(1);
        
        if( line != null   )  result = result.substring(0, result.length() - line.length());
        if( result == null )  throw new NoSuchElementException();
        else				  return result;
    }

	// ---- [helpers] ------
    
    private final void rollback()
    {
        this . position	 =  save;
        this . save      =  -1;
        this . skipped   =  false;
    }

    private final void read()
    {
        if( buf.limit() == buf.capacity() ) expand();
        
        int p = buf.position();
        
        this . buf . position ( buf.limit()    );
        this . buf . limit    ( buf.capacity() );

        int n = 0;

        try                     { n = source.read( buf ); }
        catch (IOException ioe) { n = -1;                 }

        if( n == -1 ) input = false;
        if( n > 0   ) input = false;

        buf . limit    ( buf.position() );
        buf . position ( p              );
    }

    private final boolean expand()
    {
        int offset = save == -1 ? position : save;
        
        buf . position( offset );
        
        if (offset > 0)
        {
            buf . compact();
            
            if (save != -1) save -= offset;
            
            position -= offset;
            
            buf . flip();

            return true;
        }

        CharBuffer _buf = CharBuffer.allocate(buf.capacity() << 1);

        _buf . put  ( buf );
        _buf . flip (     );

        if (save != -1) save -= offset;

        this . position  -=  offset;
        this . buf       =   _buf;

        this . matcher . reset ( buf );
        
        return true;
    }

    private final boolean has_token()
    {
        this . valid = false;

        this . matcher . usePattern ( WHITESPACE             );
        this . matcher . region     ( position , buf.limit() );

        if( matcher.lookingAt()     )  position = matcher.end();
        if( position == buf.limit() )  return false;

        return true;
    }

    private final String find_token( Pattern pattern )
    {
        this . valid = false;

        this . matcher.usePattern( WHITESPACE );
        
        if( !skipped )
        {
        	this . matcher . region ( position , buf.limit() );
        	
            if( matcher.lookingAt() )
            {
                if( matcher.hitEnd() )
                {
                    input = true;
                    return null;
                }
                this . skipped  = true;
                this . position = matcher . end();
            }
        }

        if( position == buf.limit() )
        {
            input = true;
            return null;
        }

        matcher . region ( position, buf.limit() );
        
        boolean next = matcher.find();
        
        if( next && (matcher.end() == position) )  next = matcher.find();
        if( next )
        {
            if (matcher.requireEnd())
            {
                input = true;
                return null;
            }
            
            int end = matcher.start();
            
            if( pattern == null ) pattern = FIND_ANY_PATTERN;
            
            this . matcher . usePattern ( pattern       );
            this . matcher . region     ( position, end );
            
            if( this . matcher.matches() )
            {
                String s         =  matcher . group ();
                this . position  =  matcher . end   ();

                return s;
            }
            return null;
        }

        if( closed )
        {
            if( pattern == null ) pattern = FIND_ANY_PATTERN;

            this . matcher . usePattern ( pattern               );
            this . matcher . region     ( position, buf.limit() );

            if( matcher.matches() )
            {
                String s         =  matcher . group ();
                this . position  =  matcher . end   ();

                return s;
            }
            return null;
        }

        this . input = true;
        
        return null;
    }

    private final String find( Pattern pattern, int horizon )
    {
        this . valid = false;

        this . matcher . usePattern( pattern );
        
        int buf_limit     =  buf.limit();
        int line_limit    =  -1;
        int search_limit  =  buf_limit;
        
        if( horizon > 0 )
        {
            line_limit = position + horizon;
            
            if( line_limit < buf_limit ) search_limit = line_limit;
        }
        
        matcher . region( position, search_limit );
        
        if( matcher.find() )
        {
            if( matcher.hitEnd() && ((search_limit != line_limit) || ((search_limit == line_limit) && matcher.requireEnd())) )
            {
            	this . input = true;
            	return null;
            }
            
            this . position = matcher.end();
            
            return this . matcher.group();
        }

        if( closed ) return null;

        if( ( horizon == 0 ) || ( search_limit != line_limit ) ) this . input = true;
        
        return null;
    }


    public void close()
    {
        if( closed ) return;
        
        try                    { this . source . close(); }
        catch (IOException e)  {                          }
        
        this . source  =  null;
        this . closed  =  true;
    }

    public void remove()
    { throw new UnsupportedOperationException(); }

    private final boolean hasNext( Pattern pattern )
    {
    	save = position;
        
        while (true)
        {
            if (find_token(pattern) != null)
            {
                valid = true;
                rollback();
                return true;
            }
            if (input)	read();
            else
            {
            	rollback();
            	return false;
            }
        }
    }

    private final String next( Pattern pattern )
    {
        while( true )
        {
            String token = find_token(pattern);
            if( token != null )
            {
                valid	 = true;
                skipped  = false;
                return token;
            }
            if (input) read();
        }
    }
    
    private String scan( Pattern pattern, int horizon )
    {
        while( true )
        {
            String token = find(pattern, horizon);
            if( token != null )
            {
                valid = true;
                return token;
            }
            if( input ) read();
            else break;
        }
        return null;
    }
    
    // ---- [monkey test] ------
    
    public static void main(String ... args)
    {
    	Fio fio = new Fio(System.in);
    
    	SwingUtilities.invokeLater(new Runnable(){public void run(){
    		for(int i=0; i<10000; i++)System.out.println(i);
    	}});
    	
    	while(fio.hasNextShort()) System.out.println("RED: "+fio.nextShort());
    	
    	fio.close();
    }


}
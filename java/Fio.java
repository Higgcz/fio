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
        this . delimiter		=  WHITESPACE;
        this . source			=  new InputStreamReader( source );
        this . buf				=  CharBuffer . allocate( B_SIZE );
        
        this . buf.limit ( 0 );
        
        this . matcher			=  delimiter . matcher( buf );
        
        this . matcher . useTransparentBounds ( true  );
        this . matcher . useAnchoringBounds   ( false );
    }

	// ---- [api] ------

    public boolean hasNext()
    {
    	save = position;
        
        while (!closed)
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
            if (token != null)
            {
                valid = true;
                skipped = false;
                return token;
            }
            if (input) read();
        }
    }

    public void remove()
    { throw new UnsupportedOperationException(); }

    public boolean hasNext(Pattern pattern)
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

    public String next(Pattern pattern)
    {
        while (true)
        {
            String token = find_token(pattern);
            if (token != null)
            {
                valid	= true;
                skipped		= false;
                return token;
            }
            if (input) read();
        }
    }

    public boolean hasNextLine()
    {
    	save = position;

        String result = scan(LINE, 0);
        
        if (result != null && valid)
        {
            MatchResult mr = matcher.toMatchResult();
            String lineSep = mr.group(1);
            if (lineSep != null)
                result = result.substring(0, result.length() - lineSep.length());
        }
        
        rollback();
        
        return (result != null);
    }

    public String nextLine()
    {
        String result = scan(LINE, 0);
        
        if (result == null || !valid) throw new NoSuchElementException("No line found");
        
        String line = matcher.toMatchResult().group(1);
        
        if( line != null   )  result = result.substring(0, result.length() - line.length());
        if( result == null )  throw new NoSuchElementException();
        else				  return result;
    }

    private String scan(Pattern pattern, int horizon)
    {
        while (true)
        {
            String token = find(pattern, horizon);
            if (token != null)
            {
                valid = true;
                return token;
            }
            if( input ) read();
            else break;
        }
        return null;
    }

    public  boolean  hasNextBoolean ()  { return hasNext ( BOOLEAN );                       }
    public  boolean  hasNextByte    ()  { return hasNext ( INTEGER );                       }
    public  boolean	 hasNextShort   ()  { return hasNext ( INTEGER );                       }
    public  boolean	 hasNextInt     ()  { return hasNext ( INTEGER );                       }
    public  boolean	 hasNextDecimal ()  { return hasNext ( DECIMAL );                       }
    
    public  boolean	 nextBoolean    ()  { return Boolean . parseBoolean ( next(BOOLEAN) );  }
    public  byte     nextByte       ()  { return Byte    . parseByte    ( next(INTEGER) );  }
    public  short    nextShort      ()  { return Short   . parseShort   ( next(INTEGER) );  }
    public  int      nextInt        ()  { return Integer . parseInt     ( next(INTEGER) );  }
    public  float    nextDecimal    ()  { return Float   . parseFloat   ( next(DECIMAL) );  }
    
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
        try						{ n = source.read( buf ); }
        catch (IOException ioe)	{ n = -1;				  }

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

        this . position -= offset;
        this . buf      =  _buf;

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
        
    	this . matcher.usePattern(WHITESPACE);
        
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
        
        boolean foundNextDelim = matcher.find();
        
        if( foundNextDelim && (matcher.end() == position) )  foundNextDelim = matcher.find();
        if( foundNextDelim )
        {
            if (matcher.requireEnd())
            {
                input = true;
                return null;
            }
            
            int tokenEnd = matcher.start();
            
            if (pattern == null)
                pattern = FIND_ANY_PATTERN;
            
            this . matcher . usePattern ( pattern            );
            this . matcher . region     ( position, tokenEnd );
            
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
        
        int bufferLimit   =  buf.limit();
        int horizonLimit  =  -1;
        int searchLimit   =  bufferLimit;
        
        if( horizon > 0 )
        {
            horizonLimit = position + horizon;
            
            if (horizonLimit < bufferLimit) searchLimit = horizonLimit;
        }
        
        matcher . region( position, searchLimit );
        
        if( matcher.find() )
        {
            if (matcher.hitEnd() && ((searchLimit != horizonLimit) || ((searchLimit == horizonLimit) && matcher.requireEnd())))
            {
            	input = true;
            	return null;
            }
            
            position = matcher.end();
            
            return matcher.group();
        }

        if( closed ) return null;

        if( (horizon == 0) || (searchLimit != horizonLimit) ) this . input = true;
        
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

	// ---- [monkey test] ------
    
    public static void main(String ... args)
    {
    	Fio fio = new Fio(System.in);
    	
    	while(fio.hasNextShort()) System.out.println(fio.nextShort());
    	
    	fio.close();
    }


}
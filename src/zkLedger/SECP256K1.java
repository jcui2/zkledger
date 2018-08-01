package zkLedger;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

public class SECP256K1 {
    public static final ECCurve CURVE = ECNamedCurveTable.getParameterSpec("secp256k1").getCurve();
    public static MessageDigest SHA256;
    static {
        try {
            SHA256 = MessageDigest.getInstance("SHA-256");
        }catch(NoSuchAlgorithmException e) {
            SHA256 = null;
        }
    }
    
    public static final BigInteger P = CURVE.getOrder();
   
    
    /**
     * @param encoding a encoding of a decompressed point on elliptic curve Secp256k1 in small-endian byte order
     * @return the encoded point on elliptic curve Secp256k1 
     * credit to: http://javadox.com/org.bouncycastle/bcprov-jdk15on/1.51/org/bouncycastle/math/ec/ECCurve.java.html
     *            function:  protected ECPoint decompressPoint(int yTilde, BigInteger X1)
     */
    public static ECPoint pointDecompressFromString(byte[] encoding) {
        byte[] encodingFlipped = new byte[encoding.length];
        for (int i=0; i < encoding.length; i++) {
            encodingFlipped[i] = encoding[encoding.length-1-i];
        }

        int yTilde = encoding[0] % 2;
        BigInteger xCoord = new BigInteger(1, encodingFlipped).clearBit(0).shiftRight(1);   
        ECFieldElement x = CURVE.fromBigInteger(xCoord);
        ECFieldElement rhs = x.square().multiply(x).add(CURVE.getA().multiply(x)).add(CURVE.getB()); //x^3 + ax + b
        ECFieldElement y = rhs.sqrt();

        /*
         * If y is not a square, then we haven't got a point on the curve
         */
        if (y == null)
        {
            throw new IllegalArgumentException("Invalid point compression");
        }

        if (y.testBitZero() != (yTilde == 1))
        {
            // Use the other root
            y = y.negate();
        }
        return CURVE.createPoint(x.toBigInteger(), y.toBigInteger());

    }
    
    /**
     * Random big integer in (0, P);
     * @return
     */
    public static BigInteger getRandomBigInt() {
        BigInteger r;
        do{
            r = new BigInteger(P.bitLength(), new Random());
        } while (r.compareTo(P) >= 0 || r.equals(BigInteger.ZERO));
        
        return r;
    }
    
    
    public static BigInteger ecPointArrayToRandomness(ECPoint[] ecPointArray) {
        
            final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            try {
                for (ECPoint i: ecPointArray) {
                    byteArray.write(i.getEncoded(true));
                } 
            } catch(Exception e) {
                e.printStackTrace();
            }

       synchronized(SECP256K1.CURVE) {
            byte[] inputInByte = SECP256K1.SHA256.digest(byteArray.toByteArray());
            return new BigInteger(1, inputInByte);
        }
    }

}
